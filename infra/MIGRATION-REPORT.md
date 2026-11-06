# Informe de migración PostgreSQL de LifeHub

Fecha de validación: 2026-11-05/06 UTC.

## 1. Arquitectura final

```text
PostgreSQL Keycloak
└── keycloak

PostgreSQL LifeHub
├── identitydb   └── schema identity   └── identityuser
├── movementsdb └── schema movements  └── movementsuser
└── tagsdb      └── schema tags       └── tagsuser
```

LifeHub sigue ejecutándose como un único monolito modular. Keycloak mantiene
su instancia PostgreSQL separada y no comparte base, volumen ni usuario con
LifeHub.

## 2. Bases, contenedores y volúmenes

La instancia activa de LifeHub es `infra-postgres-lifehub-1`; contiene
`lifehub`, `identitydb`, `movementsdb` y `tagsdb`. La instancia activa de
Keycloak es `infra-postgres-keycloak-1` y contiene únicamente `keycloak`.

La aplicación activa es `infra-lifehub-1`. Keycloak es `infra-keycloak-1`.
Los contenedores y volúmenes antiguos de `user`, `movements` y `tags` están
conservados para rollback, junto con `infra_postgres_lifehub_data` y
`infra_kcdata`.

Espacio comprobado antes de la migración: aproximadamente 47 GB disponibles
en un disco de 73 GB.

## 3. Usuarios y permisos

Se crearon y validaron los roles no superusuario `identityuser`,
`movementsuser` y `tagsuser`. Cada uno tiene `CONNECT` únicamente sobre su
base, además de uso y creación en su esquema propietario. Los accesos
cruzados entre `identitydb`, `movementsdb` y `tagsdb` están denegados.

`lifehubuser` se conserva como usuario administrativo/bootstrap de la
instancia existente; la aplicación no lo usa como datasource de módulo.

## 4. Copia de datos y comparación

| Objeto | `lifehub` | Base nueva |
|---|---:|---:|
| `identity.users` | 2 | `identitydb.identity.users`: 2 |
| `identity.identity_flyway_schema_history` | 3 | `identitydb`: 3 |
| `movements.movements` | 472 | `movementsdb.movements`: 472 |
| `movements.movements_flyway_schema_history` | 3 | `movementsdb`: 3 |
| `tags.tags` | 34 | `tagsdb.tags`: 34 |
| `tags.tag_deletion_operations` | 0 | `tagsdb.tags`: 0 |
| `tags.tags_flyway_schema_history` | 3 | `tagsdb`: 3 |

Se compararon columnas, tipos, nulabilidad, valores por defecto, claves
primarias, índices, restricciones, secuencias, propietarios y filas. Las
secuencias conservaron sus valores: usuarios 2, movimientos 484, etiquetas
35; la secuencia de operaciones de borrado permanece sin valor generado.

No existen claves foráneas entre módulos en el esquema actual. La coordinación
de borrado de etiquetas se realiza mediante la operación interna protegida y
validación a nivel de aplicación.

## 5. Flyway y configuración

Cada módulo tiene datasource, usuario, EMF, transaction manager y Flyway
independientes:

- Identity: `identitydb`, schema `identity`, historial
  `identity_flyway_schema_history`.
- Movements: `movementsdb`, schema `movements`, historial
  `movements_flyway_schema_history`.
- Tags: `tagsdb`, schema `tags`, historial `tags_flyway_schema_history`.

Flyway arranca con `baselineOnMigrate=false`, `validateOnMigrate=true` y
`cleanDisabled=true`. Los historiales existentes validaron correctamente; no
se ejecutó `baseline` sobre datos migrados.

La variable existente `USER_DB_PASSWORD` se conserva por compatibilidad para
la contraseña bootstrap de LifeHub y `identityuser`; `MOVEMENTS_DB_PASSWORD`
y `TAGS_DB_PASSWORD` siguen siendo independientes. No se incluyen valores de
secretos en este informe.

## 6. Backups y checksums

Backup principal:

`/opt/app/backups/finanzapp-lifehub-pre-migration-20261105T230204Z/`

Incluye dumps de Keycloak, `lifehub`, cada esquema de LifeHub, globals y
snapshots de los volúmenes PostgreSQL relevantes. Los dumps tienen contenido,
`SHA256SUMS` valida correctamente y `pg_restore --list` devuelve TOC para cada
dump. También se hizo una restauración de verificación en un contenedor
efímero sin volumen persistente.

No se ejecutó `docker compose down -v`, no se borraron volúmenes ni se eliminó
`lifehub`.

## 7. Tests y validaciones funcionales

- Maven: 13 tests, 0 fallos, 0 errores, 0 omitidos; `BUILD SUCCESS`.
- Flyway y Hibernate: arranque correcto y esquemas validados.
- Healthcheck de LifeHub: HTTP 200, `{"status":"UP"}`.
- PostgreSQL LifeHub y PostgreSQL Keycloak: `healthy`.
- Keycloak: discovery local y público HTTP 200; base y volumen separados.
- Flujo funcional validado: login OIDC, creación/consulta de usuario,
  creación/consulta de etiquetas, creación/consulta de movimientos y borrado
  de etiquetas.
- Borrado de etiqueta: movimiento desacoplado, fila de etiqueta eliminada y
  operación marcada `COMPLETED`.

La prueba OIDC usó un usuario y cliente temporales, eliminados al finalizar.
La acción obligatoria `CONFIGURE_TOTP` se desactivó solo durante esa prueba y
se restauró a su estado original; los conteos de usuarios y clientes de
Keycloak quedaron sin cambios.

## 8. Logs y referencias antiguas

Los logs de arranque muestran los tres pools, tres EMF y tres Flyway activos.
Las trazas `NoResourceFoundException` observadas durante la inspección
procedían de sondas iniciales con rutas sin prefijo (`/users`, `/tags`,
`/movements`); las rutas públicas reales son `/user/**`, `/tag/**` y
`/movement/**`. No se detectaron errores de PostgreSQL asociados a la
migración.

La configuración activa de Spring y Compose ya no apunta a
`lifehub.identity`, `lifehub.movements` ni `lifehub.tags` como datasources.

## 9. Rollback

1. Detener solo `infra-lifehub-1` para cortar escrituras.
2. Restaurar la configuración anterior de Compose/Spring desde la copia de
   configuración validada.
3. Arrancar LifeHub apuntando a `lifehub` y sus esquemas originales.
4. Validar login, salud y conteos usando `lifehub.dump` si fuera necesario.
5. Mantener `identitydb`, `movementsdb`, `tagsdb`, `lifehub`, snapshots y
   backups. No eliminar nada sin confirmación explícita.

## 10. Elementos antiguos conservados

Se conservaron la base `lifehub`, sus esquemas y objetos, el volumen
`infra_postgres_lifehub_data`, los volúmenes PostgreSQL antiguos de módulos,
los contenedores detenidos asociados, el volumen `infra_kcdata` y todos los
backups fechados.

# Despliegue seguro

Estos pasos están pensados para actualizar una instalación existente sin borrar
los datos de PostgreSQL.

## Antes de actualizar

1. Haz una copia de seguridad de las tres bases de datos y de Keycloak. Como
   mínimo, conserva un `pg_dump` de `userdb`, `movementsdb` y `tagsdb`, además
   de un snapshot de la máquina virtual.
2. Conserva los volúmenes Docker existentes:
   `postgres_user_data`, `postgres_movements_data`, `postgres_tag_data` y
   `kcdata`. No uses `docker compose down -v`.
3. Crea `infra/.env` a partir de `.env.example`.

La variable `INTERNAL_SERVICE_TOKEN` debe ser un valor aleatorio largo. LifeHub
la utiliza para proteger la ruta interna de limpieza reintentable de etiquetas;
no es un token de usuario y nunca debe exponerse al frontend.

## Primera actualización de una instalación ya existente

Los contenedores PostgreSQL solo aplican `POSTGRES_PASSWORD` al inicializar un
volumen vacío. Por tanto, en una instalación existente debes poner inicialmente
en `infra/.env` exactamente las contraseñas que ya tienen los cuatro volúmenes,
consultándolas en el entorno de despliegue actual. No uses todavía contraseñas
nuevas para estos cuatro valores.

Para Keycloak, conserva también el usuario administrador que ya existe. La
variable `KEYCLOAK_ADMIN_PASSWORD` solo crea la credencial inicial cuando la
base de datos está vacía; no cambia la contraseña de un administrador existente.

Después de comprobar que la nueva versión funciona, las contraseñas pueden
rotarse mediante un procedimiento separado y controlado. No se deben cambiar
estas variables esperando que PostgreSQL modifique automáticamente los usuarios
de un volumen ya inicializado.

## Aplicación de la versión

Desde `infra/`:

```bash
docker compose --env-file .env config --quiet
docker compose --env-file .env up -d --build
docker compose ps
```

Las migraciones Flyway incluidas crean el historial mediante baseline, añaden
índices y convierten los importes existentes de `DOUBLE PRECISION` a `NUMERIC`
mediante `ALTER COLUMN ... USING`. Esta conversión es in-place: no elimina ni
recrea filas. Aun así, la copia de seguridad previa es obligatoria antes de
aplicar la migración de importes. En una base nueva, las tablas ya nacen con
`NUMERIC`. Hibernate está configurado con `ddl-auto: validate`, por lo que una
diferencia de esquema detiene el servicio en vez de alterar o borrar tablas
automáticamente.

Si Flyway o Hibernate detectan una diferencia, detén el despliegue y revisa el
esquema. No borres el volumen ni ejecutes migraciones destructivas para forzar
el arranque.

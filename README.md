# FinanzApp

FinanzApp es una aplicación personal de gestión financiera. Permite registrar
ingresos, gastos e inversiones, organizarlos mediante etiquetas y consultar
resúmenes, históricos, ahorros y patrimonio neto.

Este repositorio contiene el backend. El frontend está en el repositorio
hermano `finanzappv3-frontend`.

## Índice

- [Arquitectura](#arquitectura)
- [Requisitos](#requisitos)
- [Arranque local rápido](#arranque-local-rápido)
- [Arranque local manual](#arranque-local-manual)
- [Puertos y servicios](#puertos-y-servicios)
- [Configuración](#configuración)
- [Datos y bases de datos](#datos-y-bases-de-datos)
- [API principal](#api-principal)
- [Reglas de negocio](#reglas-de-negocio)
- [Seguridad](#seguridad)
- [Pruebas](#pruebas)
- [Despliegue remoto](#despliegue-remoto)
- [Migraciones](#migraciones)
- [Solución de problemas](#solución-de-problemas)

## Arquitectura

```text
Navegador
   │
   ▼
Next.js + NextAuth (frontend, 3000)
   │ BFF /api/*; tokens en servidor
   ▼
LifeHub / FinanzApp (8080)
   └── /user/*, /movement/*, /tag/*
                                      ├── identity   ──► PostgreSQL LifeHub / identitydb
                                      ├── movements  ──► PostgreSQL LifeHub / movementsdb
                                      └── tags       ──► PostgreSQL LifeHub / tagsdb

Keycloak (8081) ──► PostgreSQL keycloak
```

Componentes:

- **Frontend:** Next.js 16, React 19, TypeScript, NextAuth, Tailwind y
  Recharts.
- **Identidad:** Keycloak mediante OpenID Connect Authorization Code + PKCE.
- **Backend HTTP:** LifeHub, punto de entrada HTTP de la aplicación.
- **Backend:** `lifehub` como monolito modular Spring Boot. `identity`,
  `movements` y `tags` son módulos internos de LifeHub/FinanzApp.
- **Persistencia:** PostgreSQL 16 en dos instancias: una exclusiva para
  Keycloak (`keycloak`) y otra para LifeHub (`identitydb`, `movementsdb`,
  `tagsdb`). Cada módulo conserva su esquema interno y su usuario propietario.
- **Esquema:** Flyway; Hibernate utiliza `ddl-auto: validate` y no modifica
  automáticamente las tablas.

## Requisitos

- Docker Desktop con Docker Compose.
- Java 21.
- Node.js y npm.
- PowerShell en Windows.

Los servicios backend utilizan Maven Wrapper (`mvnw.cmd`), por lo que no hace
falta instalar Maven globalmente.

## Arranque local rápido

Desde PowerShell:

```powershell
cd C:\Users\alvub\Desktop\Desarrollo\finanzapp\finanzappv3\infra
Set-ExecutionPolicy -Scope Process Bypass
.\start-local.ps1
```

El script:

1. Crea `infra/.env.local` si no existe.
2. Levanta PostgreSQL y Keycloak en Docker.
3. Crea o actualiza el realm local `finanzapp`.
4. Crea el cliente de NextAuth y un usuario de desarrollo.
5. Compila y arranca el monolito modular `lifehub` con perfil `local`.
6. Crea `.env.local` del frontend si no existe.
7. Instala dependencias si todavía no existe `node_modules`.
8. Arranca Next.js.

La aplicación queda en:

```text
http://localhost:3000
```

Los logs se guardan en `infra/.local/logs/`.

Para detenerla sin borrar datos:

```powershell
cd C:\Users\alvub\Desktop\Desarrollo\finanzapp\finanzappv3\infra
.\stop-local.ps1
```

La parada utiliza `docker compose down`, nunca `docker compose down -v`.

Durante el desarrollo, el frontend se actualiza automáticamente mediante
Next.js. El lanzador también mantiene un observador del backend: al detectar
cambios en Java o en `src/main/resources`, recompila únicamente el servicio
afectado (`lifehub`). Normalmente no hay que relanzar nada;
solo esperar unos segundos a que termine la compilación.

Los logs del observador están en `infra/.local/logs/backend-watcher.out.log`.

La guía breve está en [infra/LOCAL.md](infra/LOCAL.md).

### Usuario local

El lanzador crea estas credenciales únicamente para desarrollo:

```text
Email:      local@finanzapp.test
Contraseña: localpass123!

Email	Contraseña
ana@finanzapp.test	Test1234!
carlos@finanzapp.test	Test1234!
lucia@finanzapp.test	Test1234!
```

No deben utilizarse en producción.

## Arranque local manual

### Infraestructura

Crear `infra/.env` desde `infra/.env.example`, configurar valores locales y
ejecutar:

```powershell
cd infra
docker compose --env-file .env -f docker-compose.local.yml up -d `
  postgres `
  keycloak
```

Después hay que configurar manualmente el realm `finanzapp`, el cliente de
NextAuth y un usuario en Keycloak.

### Backend

El modo recomendado es `infra\start-local.ps1`, que arranca LifeHub con las
mismas rutas externas de siempre. Para una ejecucion manual:

```powershell
.\mvnw.cmd -f .\pom.xml -pl lifehub -am package -DskipTests
java -jar .\lifehub\target\lifehub-0.0.1-SNAPSHOT.jar
```


### Frontend

Desde `lifehub-frontend`:

```powershell
Copy-Item .env.example .env.local
npm ci
npm run dev
```

## Puertos y servicios

| Componente | Puerto local | Expuesto directamente en producción |
|---|---:|---|
| Frontend Next.js | `3000` | Según el proxy inverso |
| LifeHub | `8080` | Sí, detrás del proxy |
| Keycloak | `8081` | Sí, bajo `/auth` y HTTPS |
| LifeHub (FinanzApp modular) | `8080` | Sí, detrás del proxy |
| PostgreSQL LifeHub | `5437` | No |
| PostgreSQL Keycloak | Interno Docker | No |

En producción, LifeHub y las bases de datos solo deben ser
accesibles dentro de la red Docker.

## Configuración

### Backend

- Perfil `local`: utiliza `localhost`, los puertos locales y el issuer local de
  Keycloak.
- Perfil `prod`: utiliza los nombres internos de Docker y el issuer público de
  Keycloak.

Variables de `infra/.env`:

| Variable | Uso |
|---|---|
| `KEYCLOAK_DB_PASSWORD` | Contraseña de la base de datos de Keycloak |
| `KEYCLOAK_ADMIN` | Administrador inicial de Keycloak |
| `KEYCLOAK_ADMIN_PASSWORD` | Contraseña inicial de Keycloak |
| `KEYCLOAK_HOSTNAME` | Host anunciado por Keycloak |
| `USER_DB_PASSWORD` | Contraseña existente de PostgreSQL LifeHub y de `identityuser` (nombre conservado por compatibilidad) |
| `MOVEMENTS_DB_PASSWORD` | Contraseña del usuario `movementsuser` |
| `TAGS_DB_PASSWORD` | Contraseña del usuario `tagsuser` |
| `INTERNAL_SERVICE_TOKEN` | Token privado para la ruta interna de etiquetas |

`infra/.env` y `infra/.env.local` no deben subirse al repositorio.

### Frontend

Variables de `lifehub-frontend/.env.local`:

```env
API_GATEWAY_URL=http://localhost:8080
AUTH_SECRET=secreto-largo-y-aleatorio
AUTH_KEYCLOAK_ISSUER=http://localhost:8081/auth/realms/finanzapp
AUTH_KEYCLOAK_ID=finanzapp-frontend
AUTH_KEYCLOAK_SECRET=secreto-del-cliente
NEXT_PUBLIC_KEYCLOAK_ISSUER=http://localhost:8081/auth/realms/finanzapp
```

En producción, `API_GATEWAY_URL` debe ser alcanzable desde el servidor de
Next.js, no necesariamente desde el navegador.

## Datos y bases de datos

El sistema utiliza PostgreSQL para la aplicación. H2 se utiliza únicamente en
tests concretos.

- `keycloak`: usuarios, clientes y configuración de Keycloak.
- `identitydb.identity`: perfiles y configuración de usuarios.
- `movementsdb.movements`: ingresos, gastos e inversiones.
- `tagsdb.tags`: etiquetas y operaciones reintentables de borrado.

En local, Docker mantiene `keycloak` en la instancia `postgres-keycloak` y
las bases de LifeHub en la instancia `postgres-lifehub`, ambas dentro del
proyecto Compose `finanzapp-local`. La base antigua `lifehub` y sus volúmenes
se conservan como respaldo de rollback.

Reglas de seguridad de datos:

- No utilizar `docker compose down -v` salvo que se quiera borrar el entorno.
- No borrar volúmenes para resolver errores de arranque.
- Antes de desplegar cambios de esquema en remoto, hacer backup de la base de
  datos de LifeHub y de la base de datos de Keycloak.
- En una instalación existente, las contraseñas de `.env` deben coincidir con
  las contraseñas reales de los usuarios PostgreSQL ya inicializados.

## API principal

Todas las rutas de LifeHub requieren JWT válido salvo los endpoints
de salud.

### Usuarios

Prefijo: `/user/users`

| Método | Ruta | Función |
|---|---|---|
| `GET` | `/isOnboarded` | Comprueba el onboarding |
| `POST` | `/` | Crea el perfil autenticado |
| `GET` | `/` | Obtiene el perfil autenticado |
| `PUT` | `/` | Actualiza el perfil autenticado |
| `GET` | `/idealPercentage` | Obtiene el porcentaje ideal |

### Movimientos

Prefijo: `/movement/movements`

Incluye consultas generales, por año, históricas y por etiqueta, además de
crear, consultar, modificar y borrar movimientos. También expone:

- `/savings`: métrica de ahorro.
- `/net-worth`: patrimonio neto.

### Etiquetas

Prefijo: `/tag/tags`

Incluye creación, listado, consulta, comprobación de existencia, borrado y
operaciones batch.

### Salud

LifeHub permite públicamente:

```text
/actuator/health
/actuator/info
```

## Reglas de negocio

### Patrimonio neto

La fórmula actual es:

```text
patrimonio inicial + beneficios - gastos
```

Los gastos incluyen gastos esenciales y discrecionales. Las inversiones no
incrementan ni reducen esta métrica. El patrimonio neto se
calcula mediante `/movement/movements/net-worth` y no reutiliza el endpoint de
ahorros.

### Borrado de etiquetas

El borrado entre `tags` y `movements` es reintentable:

1. El modulo de etiquetas marca la etiqueta como eliminada y registra una operacion pendiente.
2. Un procesador intenta desasociarla de los movimientos.
3. Si termina correctamente, se elimina la etiqueta y se completa la operación.
4. Si la limpieza interna no está disponible, se aplica reintento con espera
   creciente.

La comunicación interna requiere `INTERNAL_SERVICE_TOKEN` y no debe exponerse
al frontend.

## Seguridad

- Keycloak es el proveedor de identidad.
- NextAuth conserva los tokens sensibles en una sesión JWT HttpOnly del
  servidor.
- El frontend funciona como BFF y no entrega el access token al navegador.
- LifeHub valida JWT como OAuth2 Resource Server.
- La identidad se deriva del JWT, no de emails enviados por el cliente.
- Las rutas internas de borrado requieren el token privado de servicio.
- Los secretos se suministran mediante variables de entorno.
- Los contenedores backend se ejecutan con usuario no root.

## Pruebas

El backend se valida como un reactor Maven, que incluye el monolito modular y los modulos de FinanzApp:

```powershell
.\mvnw.cmd -f .\pom.xml test
```

Validaciones del frontend:

```powershell
cd ..\finanzappv3-frontend
npm ci
npm run lint
npm run build
npm audit --omit=dev --audit-level=high
```

GitHub Actions ejecuta los tests del reactor modular, ademas de las
validaciones de lint, build y auditoría del frontend.

## Despliegue remoto

La guía operativa completa está en [infra/DEPLOYMENT.md](infra/DEPLOYMENT.md).

Desde `infra/`:

```bash
docker compose --env-file .env config --quiet
docker compose --env-file .env up -d --build
docker compose ps
```

Antes de actualizar una instalación existente:

1. Hacer backup de Keycloak, de `lifehub` y de cada esquema de LifeHub.
2. Conservar los volúmenes Docker existentes.
3. Comprobar que `.env` contiene las contraseñas actuales.
4. Verificar que `INTERNAL_SERVICE_TOKEN` coincide con el configurado para LifeHub.
5. No ejecutar `down -v`.
6. Revisar los logs si Flyway o Hibernate detectan incompatibilidades.

El script `start-local.ps1` es exclusivamente para desarrollo local.

## Migraciones

Las migraciones Flyway están en `src/main/resources/db/migration` de cada
servicio.

Principios:

- Se ejecutan automáticamente al arrancar.
- Se utiliza `baseline-on-migrate` para instalaciones existentes.
- Hibernate valida el esquema, pero no lo modifica.
- La conversión de importes a `NUMERIC` se hace in-place.
- No deben añadirse migraciones con `DROP`, `TRUNCATE` ni borrados masivos.

Toda migración nueva debe probarse contra una copia de la base de datos y tener
un plan de recuperación mediante backup.

## Solución de problemas

### PowerShell bloquea el script

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

### Keycloak no arranca

```powershell
docker compose --project-name finanzapp-local logs keycloak
```

Comprobar también que el puerto `8081` no está ocupado.

### Un servicio no conecta con PostgreSQL

```powershell
docker compose --project-name finanzapp-local ps
```

Revisar los logs de `infra/.local/logs/` y comprobar que las contraseñas
coinciden con las usadas al inicializar los volúmenes.

### Error de issuer durante el login

En local debe utilizarse exactamente esta URL en Keycloak, backend y frontend:

```text
http://localhost:8081/auth/realms/finanzapp
```

### Reiniciar el entorno local desde cero

Este comando borra únicamente los datos locales del proyecto
`finanzapp-local`:

```powershell
docker compose --project-name finanzapp-local down -v
```

No utilizarlo en producción.

## Repositorios

Backend: `https://github.com/alvuberui/finanzappv3`

Frontend: `https://github.com/alvuberui/finanzappv3-frontend`

No subir `.env`, `.env.local`, logs, `target` ni `.next` al repositorio.

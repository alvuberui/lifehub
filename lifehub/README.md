# LifeHub backend modular

LifeHub es el unico backend de la aplicacion. FinanzApp es su primer modulo
funcional; los perfiles de usuario viven en `modules/identity` y los dominios
financieros en `modules/finance`.

## Estructura

```text
com.finanzapp.lifehub
`-- modules
    |-- core       # capacidades transversales futuras
    |-- identity   # identidad y perfil de usuario
    `-- finance    # dominios financieros
        |-- movements
        `-- tags
```

Las rutas publicas se mantienen sin cambios:

```text
/user/**      -> modulo Identity / users
/movement/**  -> modulo Finance / movements
/tag/**       -> modulo Finance / tags
```

## Persistencia

Para conservar el comportamiento y los datos actuales, se usan dos instancias
PostgreSQL: una exclusiva para Keycloak y otra para LifeHub. Dentro de la
instancia de LifeHub existen las bases `identitydb`, `movementsdb` y `tagsdb`.
Cada módulo usa su propia base, esquema y usuario propietario. La base antigua
`lifehub`, sus esquemas y los volúmenes anteriores se conservan temporalmente
como rollback y ya no reciben conexiones de la aplicación.

## Arranque local

Desde la raiz del backend:

```powershell
.\mvnw.cmd -f pom.xml -pl lifehub -am package
java -jar lifehub\target\lifehub-0.0.1-SNAPSHOT.jar
```

Con el perfil local, LifeHub escucha en `http://localhost:8080`. La instancia
PostgreSQL de LifeHub está disponible en `5437` y contiene `identitydb`,
`movementsdb` y `tagsdb`. Keycloak usa otra instancia PostgreSQL interna y está
en `8081`.

## Pruebas

```powershell
.\mvnw.cmd -f pom.xml test
```

Las pruebas usan tres bases H2 temporales y no tocan PostgreSQL.

## Docker y despliegue

El servicio `lifehub` es el unico backend de `infra/docker-compose.yml` y
expone directamente el puerto `8080`. La instancia PostgreSQL usa el volumen
`postgres_lifehub_data`; la base `lifehub` y los volúmenes anteriores se
conservan como respaldo de rollback. Las migraciones originales se mantienen
sin cambios para que Flyway valide sus checksums.

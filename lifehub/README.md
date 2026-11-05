# LifeHub backend modular

LifeHub es el unico backend de la aplicacion. FinanzApp es su primer modulo
funcional y contiene dentro de `modules/finance` toda la implementacion de
usuarios, movimientos y etiquetas.

## Estructura

```text
com.finanzapp.lifehub
`-- modules
    |-- core       # capacidades transversales futuras
    `-- finance    # FinanzApp completa
        |-- user
        |-- movements
        `-- tags
```

Las rutas publicas se mantienen sin cambios:

```text
/user/**      -> modulo Finance / users
/movement/**  -> modulo Finance / movements
/tag/**       -> modulo Finance / tags
```

## Persistencia

Para conservar el comportamiento y los datos actuales, se mantienen tres bases
PostgreSQL independientes: `userdb`, `movementsdb` y `tagsdb`. Cada una tiene su
propio `DataSource`, contexto JPA, gestor de transacciones y migraciones Flyway.
No se ejecuta ninguna migracion de datos ni se unifican las bases.

## Arranque local

Desde la raiz del backend:

```powershell
.\mvnw.cmd -f pom.xml -pl lifehub -am package
java -jar lifehub\target\lifehub-0.0.1-SNAPSHOT.jar
```

Con el perfil local, LifeHub escucha en `http://localhost:8080`. Las bases
PostgreSQL locales deben estar disponibles en `5435` (`userdb`), `5434`
(`movementsdb`) y `5436` (`tagsdb`), y Keycloak en `8081`.

## Pruebas

```powershell
.\mvnw.cmd -f pom.xml test
```

Las pruebas usan tres bases H2 temporales y no tocan PostgreSQL.

## Docker y despliegue

El servicio `lifehub` es el unico backend de `infra/docker-compose.yml` y
expone directamente el puerto `8080`. Los volumenes PostgreSQL existentes se
conservan y las migraciones originales se mantienen sin cambios para que Flyway
valide sus checksums.

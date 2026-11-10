# Desarrollo local rápido

Desde esta carpeta se puede arrancar toda la aplicación con un único comando:

```powershell
.\start-local.ps1
```

Desde la raiz del proyecto tambien puedes hacer doble clic en
`Iniciar-FinanzApp.bat`. Para detenerla, usa `Parar-FinanzApp.bat`.

El script crea, si no existen, la configuración local de Docker y del frontend,
levanta PostgreSQL y Keycloak, configura automáticamente el realm `finanzapp`,
crea un cliente de desarrollo y arranca el monolito modular `lifehub` y
Next.js. Los modulos `identity`, `movements` y `tags` se cargan dentro de LifeHub;
no se levantan como procesos separados.

La aplicación queda disponible en `http://localhost:3000`.

Usuario local:

- Email: `local@finanzapp.test`
- Contraseña: `localpass123!`

Al arrancar con `start-local.ps1` se cargan automáticamente datos de demostración
para este usuario: movimientos de los últimos meses, etiquetas y algunos eventos
extraordinarios para probar la previsión. La carga es idempotente y solo usa filas
marcadas con `[DEMO]`; no modifica datos de otros usuarios.

Para volver a cargarla manualmente:

```powershell
.\seed-local-demo.ps1
```

Para crear cuentas locales adicionales para probar conexiones y espacios compartidos:

```powershell
.\seed-local-accounts.ps1
```

Las cuentas de prueba creadas por ese script usan la contraseña `Test1234!` y
solo existen en el entorno local.

Para detenerla sin borrar datos:

```powershell
.\stop-local.ps1
```

Los datos se guardan en volúmenes Docker del proyecto `finanzapp-local`. La instancia
PostgreSQL de LifeHub contiene las bases `identitydb`, `movementsdb`, `tagsdb`,
`homedb` y `connectionsdb`; Keycloak mantiene su propia instancia PostgreSQL. Los adjuntos del módulo
Hogar se guardan en `lifehub/.local/home-attachments` y no se deben borrar al limpiar
la compilación.
El script de parada no utiliza `docker compose down -v`.

## Resumen mensual por email

El backend incluye un job diario que envía el resumen del mes anterior cuando
está activado. En local queda desactivado por defecto y en modo simulación para
no enviar emails accidentalmente.

En producción, configura en el `.env`:

```text
MONTHLY_REPORT_ENABLED=true
MONTHLY_REPORT_DRY_RUN=false
MONTHLY_REPORT_TEST_ENDPOINT_ENABLED=false
BREVO_FROM_EMAIL=notificaciones@tu-dominio.com
BREVO_FROM_NAME=LifeHub
BREVO_API_KEY=tu-api-key-de-brevo
```

La dirección remitente debe estar verificada en Brevo. El job se ejecuta a las
08:00 de `Europe/Madrid`, procesa el mes anterior y registra cada envío para no
duplicarlo. Puedes cambiar la hora con `MONTHLY_REPORT_CRON`.

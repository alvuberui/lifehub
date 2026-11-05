@echo off
setlocal

cd /d "%~dp0infra"
echo Iniciando FinanzApp local...
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\start-local.ps1"
if errorlevel 1 (
    echo.
    echo No se ha podido iniciar FinanzApp. Revisa el mensaje anterior.
    pause
    exit /b 1
)

echo.
echo FinanzApp se ha iniciado correctamente.
echo Frontend: http://localhost:3000
echo Backend:  http://localhost:8080
echo Keycloak: http://localhost:8081/auth
echo.
echo Esta ventana puede cerrarse. Los servicios continuaran activos.
pause

@echo off
setlocal

cd /d "%~dp0infra"
echo Deteniendo FinanzApp local...
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\stop-local.ps1"
if errorlevel 1 (
    echo.
    echo Se ha producido un error al detener FinanzApp.
    pause
    exit /b 1
)

echo.
echo FinanzApp se ha detenido. Los datos se han conservado.
pause

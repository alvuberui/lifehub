[CmdletBinding()]
param()

$ErrorActionPreference = "Continue"
$InfraRoot = $PSScriptRoot
$ProcessFile = Join-Path $InfraRoot ".local\processes.json"
$ComposeFile = Join-Path $InfraRoot "docker-compose.yml"
$EnvFile = Join-Path $InfraRoot ".env.local"

$processIds = @()
if (Test-Path $ProcessFile) {
    $processIds += @(Get-Content $ProcessFile -Raw | ConvertFrom-Json | ForEach-Object { $_.pid })
}

# También encuentra procesos antiguos si el lanzador anterior se cerró antes
# de actualizar processes.json. Solo coincide con las aplicaciones FinanzApp.
$processIds += @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object {
    ($_.Name -eq "java.exe" -and $_.CommandLine -match "lifehub-0\.0\.1-SNAPSHOT\.jar") -or
    ($_.Name -eq "cmd.exe" -and $_.CommandLine -match "mvnw\.cmd spring-boot:run") -or
    ($_.CommandLine -match "watch-local\.ps1") -or
    ($_.Name -eq "node.exe" -and $_.CommandLine -match "finanzappv3-frontend")
} | ForEach-Object { $_.ProcessId })

foreach ($processId in @($processIds | Select-Object -Unique)) {
    if ($processId) {
        & taskkill.exe /PID $processId /T /F | Out-Null
    }
}

Remove-Item $ProcessFile -Force -ErrorAction SilentlyContinue

if (Test-Path $EnvFile) {
    & docker compose --project-name finanzapp-local --env-file $EnvFile -f $ComposeFile down
}

Write-Host "FinanzApp local detenida. Los volúmenes de datos se han conservado."

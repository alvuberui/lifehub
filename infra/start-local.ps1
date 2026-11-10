[CmdletBinding()]
param(
    [switch]$SkipFrontend
)

$ErrorActionPreference = "Stop"

$InfraRoot = $PSScriptRoot
$BackendRoot = Split-Path -Parent $InfraRoot
$WorkspaceRoot = Split-Path -Parent $BackendRoot
$FrontendRoot = Join-Path $WorkspaceRoot "lifehub-frontend"
$ComposeFile = Join-Path $InfraRoot "docker-compose.local.yml"
$EnvFile = Join-Path $InfraRoot ".env.local"
$RuntimeRoot = Join-Path $InfraRoot ".local"
$LogRoot = Join-Path $RuntimeRoot "logs"
$ProcessFile = Join-Path $RuntimeRoot "processes.json"

New-Item -ItemType Directory -Force -Path $LogRoot | Out-Null

function Invoke-LocalCompose {
    param([Parameter(Mandatory)][string[]]$Arguments)

    & docker compose --project-name finanzapp-local --env-file $EnvFile -f $ComposeFile @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose ha fallado con código $LASTEXITCODE."
    }
}

function Get-EnvValue {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$Name
    )

    $line = Get-Content $Path | Where-Object {
        $_ -match "^$([regex]::Escape($Name))=(.*)$"
    } | Select-Object -First 1

    if ($null -eq $line) {
        return $null
    }

    return ($line -replace "^$([regex]::Escape($Name))=", "")
}

function Stop-PreviousLocalProcesses {
    if (-not (Test-Path $ProcessFile)) {
        return
    }

    try {
        $managedProcesses = Get-Content $ProcessFile -Raw | ConvertFrom-Json
    } catch {
        Write-Warning "No se pudo leer $ProcessFile; se continuará sin detener procesos anteriores."
        return
    }

    foreach ($managedProcess in @($managedProcesses)) {
        $processId = 0
        if (-not [int]::TryParse([string]$managedProcess.pid, [ref]$processId)) {
            continue
        }

        $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $processId" -ErrorAction SilentlyContinue
        if ($null -eq $processInfo) {
            continue
        }

        $commandLine = [string]$processInfo.CommandLine
        $isExpectedProcess = switch ([string]$managedProcess.name) {
            "lifehub" { $commandLine -like "*lifehub-0.0.1-SNAPSHOT.jar*" }
            "backend-watcher" { $commandLine -like "*watch-local.ps1*" }
            "frontend" { $commandLine -like "*lifehub-frontend*" -and ($commandLine -like "*npm*" -or $commandLine -like "*next*") }
            default { $false }
        }

        if ($isExpectedProcess) {
            Write-Host "Deteniendo proceso local anterior: $($managedProcess.name) (PID $processId)"
            & taskkill.exe /PID $processId /T /F | Out-Null
        }
    }
}

if (-not (Test-Path $EnvFile)) {
    @"
KEYCLOAK_DB_PASSWORD=keycloak-local-pass
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin-local-pass
KEYCLOAK_HOSTNAME=localhost
USER_DB_PASSWORD=authpass
MOVEMENTS_DB_PASSWORD=movementspass
TAGS_DB_PASSWORD=tagspass
HOME_DB_PASSWORD=homepass
CONNECTIONS_DB_PASSWORD=connectionspass
INTERNAL_SERVICE_TOKEN=local-internal-token
MONTHLY_REPORT_ENABLED=false
MONTHLY_REPORT_DRY_RUN=true
MONTHLY_REPORT_TEST_ENDPOINT_ENABLED=false
MONTHLY_REPORT_TIME_ZONE=Europe/Madrid
MONTHLY_REPORT_CRON=0 0 8 * * *
BREVO_FROM_EMAIL=
BREVO_FROM_NAME=LifeHub
BREVO_API_KEY=
"@ | Set-Content -Path $EnvFile -Encoding utf8
    Write-Host "Creado $EnvFile"
}

$KeycloakAdmin = Get-EnvValue -Path $EnvFile -Name "KEYCLOAK_ADMIN"
$KeycloakAdminPassword = Get-EnvValue -Path $EnvFile -Name "KEYCLOAK_ADMIN_PASSWORD"
$UserDbPassword = Get-EnvValue -Path $EnvFile -Name "USER_DB_PASSWORD"
$MovementsDbPassword = Get-EnvValue -Path $EnvFile -Name "MOVEMENTS_DB_PASSWORD"
$TagsDbPassword = Get-EnvValue -Path $EnvFile -Name "TAGS_DB_PASSWORD"
$HomeDbPassword = Get-EnvValue -Path $EnvFile -Name "HOME_DB_PASSWORD"
$ConnectionsDbPassword = Get-EnvValue -Path $EnvFile -Name "CONNECTIONS_DB_PASSWORD"
$InternalServiceToken = Get-EnvValue -Path $EnvFile -Name "INTERNAL_SERVICE_TOKEN"
$MonthlyReportEnabled = Get-EnvValue -Path $EnvFile -Name "MONTHLY_REPORT_ENABLED"
$MonthlyReportDryRun = Get-EnvValue -Path $EnvFile -Name "MONTHLY_REPORT_DRY_RUN"
$MonthlyReportTestEndpointEnabled = Get-EnvValue -Path $EnvFile -Name "MONTHLY_REPORT_TEST_ENDPOINT_ENABLED"
$MonthlyReportTimeZone = Get-EnvValue -Path $EnvFile -Name "MONTHLY_REPORT_TIME_ZONE"
$MonthlyReportCron = Get-EnvValue -Path $EnvFile -Name "MONTHLY_REPORT_CRON"
$BrevoFromEmail = Get-EnvValue -Path $EnvFile -Name "BREVO_FROM_EMAIL"
$BrevoFromName = Get-EnvValue -Path $EnvFile -Name "BREVO_FROM_NAME"
$BrevoApiKey = Get-EnvValue -Path $EnvFile -Name "BREVO_API_KEY"

if ([string]::IsNullOrWhiteSpace($HomeDbPassword)) {
    $HomeDbPassword = "home-local-pass"
}

if ([string]::IsNullOrWhiteSpace($ConnectionsDbPassword)) {
    $ConnectionsDbPassword = "connections-local-pass"
}

if ([string]::IsNullOrWhiteSpace($KeycloakAdminPassword)) {
    throw "KEYCLOAK_ADMIN_PASSWORD no está configurado en $EnvFile."
}

Stop-PreviousLocalProcesses

Write-Host "Levantando PostgreSQL y Keycloak..."
Invoke-LocalCompose @("up", "-d", "postgres-keycloak", "postgres-lifehub", "keycloak")

Write-Host "Esperando a que PostgreSQL de LifeHub esté listo..."
$postgresReady = $false
for ($attempt = 1; $attempt -le 30; $attempt++) {
    & docker compose --project-name finanzapp-local --env-file $EnvFile -f $ComposeFile exec -T postgres-lifehub pg_isready -U lifehubuser -d postgres 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        $postgresReady = $true
        break
    }
    Start-Sleep -Seconds 1
}
if (-not $postgresReady) {
    throw "PostgreSQL de LifeHub no ha respondido a tiempo."
}

Write-Host "Comprobando la base de datos de conexiones..."
$connectionsDatabaseExists = ((& docker compose --project-name finanzapp-local --env-file $EnvFile -f $ComposeFile exec -T postgres-lifehub psql -U lifehubuser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = 'connectionsdb';" 2>$null) -join "").Trim()
if ($connectionsDatabaseExists -ne "1") {
    $connectionsRoleExists = ((& docker compose --project-name finanzapp-local --env-file $EnvFile -f $ComposeFile exec -T postgres-lifehub psql -U lifehubuser -d postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname = 'connectionsuser';" 2>$null) -join "").Trim()
    if ($connectionsRoleExists -ne "1") {
        $escapedConnectionsPassword = $ConnectionsDbPassword.Replace("'", "''")
        & docker compose --project-name finanzapp-local --env-file $EnvFile -f $ComposeFile exec -T postgres-lifehub psql -v ON_ERROR_STOP=1 -U lifehubuser -d postgres -c "CREATE ROLE connectionsuser LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD '$escapedConnectionsPassword';" 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "No se pudo crear el usuario de connectionsdb."
        }
    }
    & docker compose --project-name finanzapp-local --env-file $EnvFile -f $ComposeFile exec -T postgres-lifehub psql -v ON_ERROR_STOP=1 -U lifehubuser -d postgres -c "CREATE DATABASE connectionsdb OWNER connectionsuser ENCODING 'UTF8' TEMPLATE template0;" 2>&1 | Out-Null
    & docker compose --project-name finanzapp-local --env-file $EnvFile -f $ComposeFile exec -T postgres-lifehub psql -v ON_ERROR_STOP=1 -U lifehubuser -d connectionsdb -c "REVOKE CONNECT ON DATABASE connectionsdb FROM PUBLIC; GRANT CONNECT ON DATABASE connectionsdb TO connectionsuser; REVOKE ALL ON SCHEMA public FROM PUBLIC; CREATE SCHEMA connections AUTHORIZATION connectionsuser; GRANT USAGE, CREATE ON SCHEMA connections TO connectionsuser;" 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo crear connectionsdb."
    }
}

$KeycloakBaseUrl = "http://localhost:8081/auth"
$Ready = $false
for ($attempt = 1; $attempt -le 60; $attempt++) {
    try {
        Invoke-WebRequest -Uri "$KeycloakBaseUrl/realms/master/.well-known/openid-configuration" -UseBasicParsing -TimeoutSec 3 | Out-Null
        $Ready = $true
        break
    } catch {
        Start-Sleep -Seconds 2
    }
}

if (-not $Ready) {
    throw "Keycloak no ha respondido en $KeycloakBaseUrl. Revisa: docker compose --project-name finanzapp-local logs keycloak"
}

Write-Host "Configurando realm, cliente y usuario local de Keycloak..."
$TokenResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "$KeycloakBaseUrl/realms/master/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        grant_type = "password"
        client_id = "admin-cli"
        username = $KeycloakAdmin
        password = $KeycloakAdminPassword
    }

$AdminHeaders = @{ Authorization = "Bearer $($TokenResponse.access_token)" }
$RealmName = "finanzapp"
$ClientId = "finanzapp-frontend"
$ClientSecret = "finanzapp-local-client-secret"
$LocalUserEmail = "local@finanzapp.test"
$LocalUserPassword = "localpass123!"

function Get-KeycloakResource {
    param([Parameter(Mandatory)][string]$Uri)

    try {
        return Invoke-RestMethod -Method Get -Uri $Uri -Headers $AdminHeaders
    } catch {
        return $null
    }
}

$Realm = Get-KeycloakResource "$KeycloakBaseUrl/admin/realms/$RealmName"
if ($null -eq $Realm) {
    Invoke-RestMethod `
        -Method Post `
        -Uri "$KeycloakBaseUrl/admin/realms" `
        -Headers $AdminHeaders `
        -ContentType "application/json" `
        -Body (@{
            realm = $RealmName
            enabled = $true
            sslRequired = "NONE"
            registrationAllowed = $false
        } | ConvertTo-Json -Depth 10) | Out-Null
}

$RealmConfiguration = @{
    realm = $RealmName
    enabled = $true
    sslRequired = "NONE"
    registrationAllowed = $true
    registrationEmailAsUsername = $true
    loginWithEmailAllowed = $true
    resetPasswordAllowed = $true
    verifyEmail = $false
}

Invoke-RestMethod `
    -Method Put `
    -Uri "$KeycloakBaseUrl/admin/realms/$RealmName" `
    -Headers $AdminHeaders `
    -ContentType "application/json" `
    -Body ($RealmConfiguration | ConvertTo-Json -Depth 10) | Out-Null

$Client = Get-KeycloakResource "$KeycloakBaseUrl/admin/realms/$RealmName/clients?clientId=$ClientId"
$ClientConfiguration = @{
    clientId = $ClientId
    name = "FinanzApp local"
    enabled = $true
    protocol = "openid-connect"
    publicClient = $false
    clientAuthenticatorType = "client-secret"
    secret = $ClientSecret
    standardFlowEnabled = $true
    directAccessGrantsEnabled = $false
    redirectUris = @(
        "http://localhost:3000/api/auth/callback/keycloak",
        "http://127.0.0.1:3000/api/auth/callback/keycloak"
    )
    webOrigins = @(
        "http://localhost:3000",
        "http://127.0.0.1:3000"
    )
    attributes = @{
        "pkce.code.challenge.method" = "S256"
        "post.logout.redirect.uris" = "http://localhost:3000/*"
    }
}

if ($null -eq $Client -or @($Client).Count -eq 0) {
    Invoke-RestMethod `
        -Method Post `
        -Uri "$KeycloakBaseUrl/admin/realms/$RealmName/clients" `
        -Headers $AdminHeaders `
        -ContentType "application/json" `
        -Body ($ClientConfiguration | ConvertTo-Json -Depth 10) | Out-Null
} else {
    $ClientUuid = @($Client)[0].id
    Invoke-RestMethod `
        -Method Put `
        -Uri "$KeycloakBaseUrl/admin/realms/$RealmName/clients/$ClientUuid" `
        -Headers $AdminHeaders `
        -ContentType "application/json" `
        -Body ($ClientConfiguration | ConvertTo-Json -Depth 10) | Out-Null
}

$ExistingUser = Get-KeycloakResource "$KeycloakBaseUrl/admin/realms/$RealmName/users?username=$LocalUserEmail"
$UserConfiguration = @{
    username = $LocalUserEmail
    email = $LocalUserEmail
    emailVerified = $true
    enabled = $true
    firstName = "Usuario"
    lastName = "Local"
    credentials = @(
        @{
            type = "password"
            value = $LocalUserPassword
            temporary = $false
        }
    )
}

if ($null -eq $ExistingUser -or @($ExistingUser).Count -eq 0) {
    Invoke-RestMethod `
        -Method Post `
        -Uri "$KeycloakBaseUrl/admin/realms/$RealmName/users" `
        -Headers $AdminHeaders `
        -ContentType "application/json" `
        -Body ($UserConfiguration | ConvertTo-Json -Depth 10) | Out-Null
}

function Start-LocalProcess {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$WorkingDirectory,
        [Parameter(Mandatory)][hashtable]$Environment,
        [Parameter(Mandatory)][string]$CommandLine
    )

    $stdout = Join-Path $LogRoot "$Name.out.log"
    $stderr = Join-Path $LogRoot "$Name.err.log"
    $oldEnvironment = @{}
    foreach ($entry in $Environment.GetEnumerator()) {
        $oldEnvironment[$entry.Key] = [Environment]::GetEnvironmentVariable($entry.Key, "Process")
        [Environment]::SetEnvironmentVariable($entry.Key, [string]$entry.Value, "Process")
    }

    try {
        $process = Start-Process `
            -FilePath "cmd.exe" `
            -ArgumentList @("/d", "/c", $CommandLine) `
            -WorkingDirectory $WorkingDirectory `
            -RedirectStandardOutput $stdout `
            -RedirectStandardError $stderr `
            -WindowStyle Hidden `
            -PassThru
    } finally {
        foreach ($entry in $oldEnvironment.GetEnumerator()) {
            [Environment]::SetEnvironmentVariable($entry.Key, $entry.Value, "Process")
        }
    }

    return [pscustomobject]@{ name = $Name; pid = $process.Id }
}

$processes = @()
$commonEnvironment = @{
    SPRING_PROFILES_ACTIVE = "local"
}

Write-Host "Compilando LifeHub y sus mÃ³dulos de FinanzApp..."
Push-Location $BackendRoot
try {
    & (Join-Path $BackendRoot "mvnw.cmd") -f (Join-Path $BackendRoot "pom.xml") -pl lifehub -am package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "La compilaciÃ³n de LifeHub ha fallado."
    }
    if ($LASTEXITCODE -ne 0) {
        throw "La compilaciÃ³n del API Gateway ha fallado."
    }
    if ($LASTEXITCODE -ne 0) {
        throw "La compilaciÃ³n de LifeHub ha fallado."
    }
} finally {
    Pop-Location
}

$processes += Start-LocalProcess `
    -Name "lifehub" `
    -WorkingDirectory (Join-Path $BackendRoot "lifehub") `
    -Environment (@{
        SPRING_PROFILES_ACTIVE = "local"
        USER_DB_PASSWORD = $UserDbPassword
        MOVEMENTS_DB_PASSWORD = $MovementsDbPassword
        TAGS_DB_PASSWORD = $TagsDbPassword
        HOME_DB_PASSWORD = $HomeDbPassword
        CONNECTIONS_DB_PASSWORD = $ConnectionsDbPassword
        INTERNAL_SERVICE_TOKEN = $InternalServiceToken
        LIFEHUB_INTERNAL_BASE_URL = "http://localhost:8080"
        MONTHLY_REPORT_ENABLED = $(if ([string]::IsNullOrWhiteSpace($MonthlyReportEnabled)) { "false" } else { $MonthlyReportEnabled })
        MONTHLY_REPORT_DRY_RUN = $(if ([string]::IsNullOrWhiteSpace($MonthlyReportDryRun)) { "true" } else { $MonthlyReportDryRun })
        MONTHLY_REPORT_TEST_ENDPOINT_ENABLED = $(if ([string]::IsNullOrWhiteSpace($MonthlyReportTestEndpointEnabled)) { "false" } else { $MonthlyReportTestEndpointEnabled })
        MONTHLY_REPORT_TIME_ZONE = $(if ([string]::IsNullOrWhiteSpace($MonthlyReportTimeZone)) { "Europe/Madrid" } else { $MonthlyReportTimeZone })
        MONTHLY_REPORT_CRON = $(if ([string]::IsNullOrWhiteSpace($MonthlyReportCron)) { "0 0 8 * * *" } else { $MonthlyReportCron })
        BREVO_FROM_EMAIL = $(if ($null -eq $BrevoFromEmail) { "" } else { $BrevoFromEmail })
        BREVO_FROM_NAME = $(if ([string]::IsNullOrWhiteSpace($BrevoFromName)) { "LifeHub" } else { $BrevoFromName })
        BREVO_API_KEY = $(if ($null -eq $BrevoApiKey) { "" } else { $BrevoApiKey })
    }) `
    -CommandLine ('java -jar "{0}"' -f (Join-Path $BackendRoot "lifehub\target\lifehub-0.0.1-SNAPSHOT.jar"))

try {
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $InfraRoot "seed-local-demo.ps1")
    if ($LASTEXITCODE -ne 0) {
        throw "La carga de datos de demostración ha fallado."
    }
} catch {
    Write-Warning "No se han podido cargar los datos de demostración: $($_.Exception.Message)"
}

$processes += Start-LocalProcess `
    -Name "backend-watcher" `
    -WorkingDirectory $InfraRoot `
    -Environment @{} `
    -CommandLine "powershell.exe -NoProfile -ExecutionPolicy Bypass -File `"$(Join-Path $InfraRoot 'watch-local.ps1')`""

if (-not $SkipFrontend) {
    $FrontendEnv = Join-Path $FrontendRoot ".env.local"
    if (-not (Test-Path $FrontendEnv)) {
        $randomBytes = New-Object byte[] 32
        $randomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        $randomGenerator.GetBytes($randomBytes)
        $randomGenerator.Dispose()
        $authSecret = [Convert]::ToBase64String($randomBytes)
        @"
API_GATEWAY_URL=http://localhost:8080
AUTH_SECRET=$authSecret
AUTH_KEYCLOAK_ISSUER=http://localhost:8081/auth/realms/finanzapp
AUTH_KEYCLOAK_ID=$ClientId
AUTH_KEYCLOAK_SECRET=$ClientSecret
NEXT_PUBLIC_KEYCLOAK_ISSUER=http://localhost:8081/auth/realms/finanzapp
"@ | Set-Content -Path $FrontendEnv -Encoding utf8
        Write-Host "Creado $FrontendEnv"
    }

    if (-not (Test-Path (Join-Path $FrontendRoot "node_modules"))) {
        Push-Location $FrontendRoot
        try {
            & npm.cmd ci
            if ($LASTEXITCODE -ne 0) { throw "npm ci ha fallado." }
        } finally {
            Pop-Location
        }
    }

    $processes += Start-LocalProcess `
        -Name "frontend" `
        -WorkingDirectory $FrontendRoot `
        -Environment @{} `
        -CommandLine 'npm.cmd run dev'
}

$processes | ConvertTo-Json | Set-Content -Path $ProcessFile -Encoding utf8

Write-Host ""
Write-Host "FinanzApp local arrancada."
Write-Host "Frontend: http://localhost:3000"
Write-Host "Usuario de prueba: $LocalUserEmail"
Write-Host "Contraseña de prueba: $LocalUserPassword"
Write-Host "Logs: $LogRoot"
Write-Host "Para parar sin borrar datos: .\stop-local.ps1"

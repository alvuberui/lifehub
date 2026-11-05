[CmdletBinding()]
param(
    [switch]$SkipFrontend
)

$ErrorActionPreference = "Stop"

$InfraRoot = $PSScriptRoot
$BackendRoot = Split-Path -Parent $InfraRoot
$WorkspaceRoot = Split-Path -Parent $BackendRoot
$FrontendRoot = Join-Path $WorkspaceRoot "finanzappv3-frontend"
$ComposeFile = Join-Path $InfraRoot "docker-compose.yml"
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

if (-not (Test-Path $EnvFile)) {
    @"
KEYCLOAK_DB_PASSWORD=keycloak-local-pass
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin-local-pass
KEYCLOAK_HOSTNAME=localhost
USER_DB_PASSWORD=authpass
MOVEMENTS_DB_PASSWORD=movementspass
TAGS_DB_PASSWORD=tagspass
INTERNAL_SERVICE_TOKEN=local-internal-token
"@ | Set-Content -Path $EnvFile -Encoding utf8
    Write-Host "Creado $EnvFile"
}

$KeycloakAdmin = Get-EnvValue -Path $EnvFile -Name "KEYCLOAK_ADMIN"
$KeycloakAdminPassword = Get-EnvValue -Path $EnvFile -Name "KEYCLOAK_ADMIN_PASSWORD"
$UserDbPassword = Get-EnvValue -Path $EnvFile -Name "USER_DB_PASSWORD"
$MovementsDbPassword = Get-EnvValue -Path $EnvFile -Name "MOVEMENTS_DB_PASSWORD"
$TagsDbPassword = Get-EnvValue -Path $EnvFile -Name "TAGS_DB_PASSWORD"
$InternalServiceToken = Get-EnvValue -Path $EnvFile -Name "INTERNAL_SERVICE_TOKEN"

if ([string]::IsNullOrWhiteSpace($KeycloakAdminPassword)) {
    throw "KEYCLOAK_ADMIN_PASSWORD no está configurado en $EnvFile."
}

Write-Host "Levantando PostgreSQL y Keycloak..."
Invoke-LocalCompose @("up", "-d", "postgres-keycloak", "postgres-user", "postgres-movements", "postgres-tags", "keycloak")

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
        INTERNAL_SERVICE_TOKEN = $InternalServiceToken
        LIFEHUB_INTERNAL_BASE_URL = "http://localhost:8080"
    }) `
    -CommandLine ('java -jar "{0}"' -f (Join-Path $BackendRoot "lifehub\target\lifehub-0.0.1-SNAPSHOT.jar"))

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

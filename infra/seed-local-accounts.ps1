[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$InfraRoot = $PSScriptRoot
$EnvFile = Join-Path $InfraRoot ".env.local"
$ComposeFile = Join-Path $InfraRoot "docker-compose.local.yml"
$ProjectName = "finanzapp-local"
$Realm = "finanzapp"
$KeycloakBaseUrl = "http://localhost:8081/auth"

if (-not (Test-Path $EnvFile)) {
    throw "No existe $EnvFile. Ejecuta primero start-local.ps1."
}

function Get-EnvValue {
    param([Parameter(Mandatory)][string]$Name)

    $line = Get-Content $EnvFile | Where-Object { $_ -match "^$([regex]::Escape($Name))=" } | Select-Object -First 1
    if ($null -eq $line) { return $null }
    return ($line -split "=", 2)[1].Trim().Trim('"')
}

$adminUser = Get-EnvValue "KEYCLOAK_ADMIN"
$adminPassword = Get-EnvValue "KEYCLOAK_ADMIN_PASSWORD"
if ([string]::IsNullOrWhiteSpace($adminUser) -or [string]::IsNullOrWhiteSpace($adminPassword)) {
    throw "Faltan KEYCLOAK_ADMIN o KEYCLOAK_ADMIN_PASSWORD en .env.local."
}

$accounts = @(
    [pscustomobject]@{ Email = "ana@finanzapp.test"; Name = "Ana"; Lastname = "García"; Wealth = "18000.00"; Income = 2600; Housing = 850; Food = 320; Transport = 110; Leisure = 130; Subscription = 35; Investment = 220 },
    [pscustomobject]@{ Email = "carlos@finanzapp.test"; Name = "Carlos"; Lastname = "Martín"; Wealth = "32000.00"; Income = 4100; Housing = 1250; Food = 450; Transport = 180; Leisure = 260; Subscription = 70; Investment = 500 },
    [pscustomobject]@{ Email = "lucia@finanzapp.test"; Name = "Lucía"; Lastname = "Santos"; Wealth = "8500.00"; Income = 1850; Housing = 700; Food = 240; Transport = 80; Leisure = 90; Subscription = 25; Investment = 120 }
)
$testPassword = "Test1234!"
$today = Get-Date

function Invoke-LifeHubSql {
    param(
        [Parameter(Mandatory)][string]$Database,
        [Parameter(Mandatory)][string]$Sql
    )

    $Sql | & docker compose --project-name $ProjectName --env-file $EnvFile -f $ComposeFile exec -T postgres-lifehub psql `
        --username lifehubuser --dbname $Database -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "No se pudo cargar la demo en $Database." }
}

function Invoke-LifeHubQuery {
    param(
        [Parameter(Mandatory)][string]$Database,
        [Parameter(Mandatory)][string]$Query
    )

    $result = & docker compose --project-name $ProjectName --env-file $EnvFile -f $ComposeFile exec -T postgres-lifehub psql `
        --username lifehubuser --dbname $Database -At -F "|" -c $Query
    if ($LASTEXITCODE -ne 0) { throw "No se pudo consultar $Database." }
    return @($result)
}

function Get-SeedDate {
    param([Parameter(Mandatory)][int]$MonthOffset, [Parameter(Mandatory)][int]$Day)

    $monthStart = Get-Date -Year $today.Year -Month $today.Month -Day 1
    $targetMonth = $monthStart.AddMonths($MonthOffset)
    $safeDay = [Math]::Min($Day, [DateTime]::DaysInMonth($targetMonth.Year, $targetMonth.Month))
    $candidate = Get-Date -Year $targetMonth.Year -Month $targetMonth.Month -Day $safeDay
    if ($candidate.Date -gt $today.Date) { return $today.Date.ToString("yyyy-MM-dd") }
    return $candidate.Date.ToString("yyyy-MM-dd")
}

Write-Host "Obteniendo acceso administrativo de Keycloak..."
$tokenResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "$KeycloakBaseUrl/realms/master/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{ grant_type = "password"; client_id = "admin-cli"; username = $adminUser; password = $adminPassword }

$headers = @{ Authorization = "Bearer $($tokenResponse.access_token)" }
$usersEndpoint = "$KeycloakBaseUrl/admin/realms/$Realm/users"

function Find-KeycloakUser {
    param(
        [Parameter(Mandatory)][string]$Email,
        [Parameter(Mandatory)][string]$Endpoint
    )

    $encoded = [Uri]::EscapeDataString($Email)
    $matches = Invoke-RestMethod -Method Get -Uri "${Endpoint}?email=$encoded&exact=true" -Headers $headers
    return @($matches) | Select-Object -First 1
}

foreach ($account in $accounts) {
    $existing = Find-KeycloakUser -Email $account.Email -Endpoint $usersEndpoint
    $configuration = @{
        username = $account.Email
        email = $account.Email
        emailVerified = $true
        enabled = $true
        firstName = $account.Name
        lastName = $account.Lastname
    }

    if ($null -eq $existing) {
        Invoke-RestMethod -Method Post -Uri $usersEndpoint -Headers $headers -ContentType "application/json" -Body ($configuration | ConvertTo-Json -Depth 10) | Out-Null
        $existing = Find-KeycloakUser -Email $account.Email -Endpoint $usersEndpoint
        Write-Host "Creada cuenta Keycloak: $($account.Email)"
    } else {
        Invoke-RestMethod -Method Put -Uri "$usersEndpoint/$($existing.id)" -Headers $headers -ContentType "application/json" -Body ($configuration | ConvertTo-Json -Depth 10) | Out-Null
        Write-Host "Actualizada cuenta Keycloak: $($account.Email)"
    }

    Invoke-RestMethod `
        -Method Put `
        -Uri "$usersEndpoint/$($existing.id)/reset-password" `
        -Headers $headers `
        -ContentType "application/json" `
        -Body (@{ type = "password"; value = $testPassword; temporary = $false } | ConvertTo-Json) | Out-Null
}

$identitySql = New-Object System.Text.StringBuilder
foreach ($account in $accounts) {
    [void]$identitySql.AppendLine(@"
INSERT INTO identity.users (
    username, name, lastname, birthdate, initial_wealth,
    monthly_saving_percentage, monthly_necessary_expenses_percentage,
    monthly_discretionary_expenses_percentage, monthly_investment_percentage
)
SELECT '$($account.Email)', '$($account.Name)', '$($account.Lastname)', '1990-01-01', $($account.Wealth), 20.00, 50.00, 20.00, 10.00
WHERE NOT EXISTS (SELECT 1 FROM identity.users WHERE username = '$($account.Email)');
"@)
}

$identitySql.ToString() | & docker compose --project-name $ProjectName --env-file $EnvFile -f $ComposeFile exec -T postgres-lifehub psql --username lifehubuser --dbname identitydb -v ON_ERROR_STOP=1
if ($LASTEXITCODE -ne 0) { throw "No se pudieron crear los perfiles de LifeHub." }

$tagDefinitions = @(
    @("Alimentacion", "#22C55E"),
    @("Vivienda", "#60A5FA"),
    @("Transporte", "#F59E0B"),
    @("Ocio", "#A78BFA"),
    @("Suscripciones", "#F472B6"),
    @("Inversion", "#14B8A6")
)

$tagSql = New-Object System.Text.StringBuilder
foreach ($account in $accounts) {
    foreach ($tag in $tagDefinitions) {
        [void]$tagSql.AppendLine("INSERT INTO tags.tags (name, color, user_email) SELECT '$($tag[0])', '$($tag[1])', '$($account.Email)' WHERE NOT EXISTS (SELECT 1 FROM tags.tags WHERE name = '$($tag[0])' AND user_email = '$($account.Email)');")
    }
}
Invoke-LifeHubSql -Database "tagsdb" -Sql $tagSql.ToString()

$tagIds = @{}
$tagRows = Invoke-LifeHubQuery -Database "tagsdb" -Query "SELECT user_email, name, id FROM tags.tags WHERE user_email IN ('ana@finanzapp.test', 'carlos@finanzapp.test', 'lucia@finanzapp.test');"
foreach ($row in $tagRows) {
    $parts = ([string]$row).Split('|', 3)
    if ($parts.Count -eq 3) { $tagIds["$($parts[0])|$($parts[1])"] = [long]$parts[2] }
}

$movementSql = New-Object System.Text.StringBuilder
foreach ($account in $accounts) {
    for ($monthOffset = -7; $monthOffset -le 0; $monthOffset++) {
        $suffix = $account.Name
        $incomeDate = Get-SeedDate $monthOffset 1
        $housingDate = Get-SeedDate $monthOffset 3
        $foodDate = Get-SeedDate $monthOffset 7
        $transportDate = Get-SeedDate $monthOffset 10
        $leisureDate = Get-SeedDate $monthOffset 15
        $subscriptionDate = Get-SeedDate $monthOffset 20
        $investmentDate = Get-SeedDate $monthOffset 25
        $housingTagId = $tagIds["$($account.Email)|Vivienda"]
        $foodTagId = $tagIds["$($account.Email)|Alimentacion"]
        $transportTagId = $tagIds["$($account.Email)|Transporte"]
        $leisureTagId = $tagIds["$($account.Email)|Ocio"]
        $subscriptionTagId = $tagIds["$($account.Email)|Suscripciones"]
        $investmentTagId = $tagIds["$($account.Email)|Inversion"]

        [void]$movementSql.AppendLine("INSERT INTO movements.movements (user_email, movement_type, amount, date, tag_id, description) SELECT '$($account.Email)', 'BENEFIT', $($account.Income), DATE '$incomeDate', $housingTagId, '[DEMO] $suffix - Nomina mensual' WHERE NOT EXISTS (SELECT 1 FROM movements.movements WHERE user_email = '$($account.Email)' AND date = DATE '$incomeDate' AND description = '[DEMO] $suffix - Nomina mensual');")
        [void]$movementSql.AppendLine("INSERT INTO movements.movements (user_email, movement_type, amount, date, tag_id, description) SELECT '$($account.Email)', 'ESSENTIAL_EXPENSE', $($account.Housing), DATE '$housingDate', $housingTagId, '[DEMO] $suffix - Vivienda' WHERE NOT EXISTS (SELECT 1 FROM movements.movements WHERE user_email = '$($account.Email)' AND date = DATE '$housingDate' AND description = '[DEMO] $suffix - Vivienda');")
        [void]$movementSql.AppendLine("INSERT INTO movements.movements (user_email, movement_type, amount, date, tag_id, description) SELECT '$($account.Email)', 'ESSENTIAL_EXPENSE', $($account.Food), DATE '$foodDate', $foodTagId, '[DEMO] $suffix - Alimentacion' WHERE NOT EXISTS (SELECT 1 FROM movements.movements WHERE user_email = '$($account.Email)' AND date = DATE '$foodDate' AND description = '[DEMO] $suffix - Alimentacion');")
        [void]$movementSql.AppendLine("INSERT INTO movements.movements (user_email, movement_type, amount, date, tag_id, description) SELECT '$($account.Email)', 'ESSENTIAL_EXPENSE', $($account.Transport), DATE '$transportDate', $transportTagId, '[DEMO] $suffix - Transporte' WHERE NOT EXISTS (SELECT 1 FROM movements.movements WHERE user_email = '$($account.Email)' AND date = DATE '$transportDate' AND description = '[DEMO] $suffix - Transporte');")
        [void]$movementSql.AppendLine("INSERT INTO movements.movements (user_email, movement_type, amount, date, tag_id, description) SELECT '$($account.Email)', 'DISCRETIONARY_EXPENSE', $($account.Leisure), DATE '$leisureDate', $leisureTagId, '[DEMO] $suffix - Ocio' WHERE NOT EXISTS (SELECT 1 FROM movements.movements WHERE user_email = '$($account.Email)' AND date = DATE '$leisureDate' AND description = '[DEMO] $suffix - Ocio');")
        [void]$movementSql.AppendLine("INSERT INTO movements.movements (user_email, movement_type, amount, date, tag_id, description) SELECT '$($account.Email)', 'DISCRETIONARY_EXPENSE', $($account.Subscription), DATE '$subscriptionDate', $subscriptionTagId, '[DEMO] $suffix - Suscripciones' WHERE NOT EXISTS (SELECT 1 FROM movements.movements WHERE user_email = '$($account.Email)' AND date = DATE '$subscriptionDate' AND description = '[DEMO] $suffix - Suscripciones');")
        [void]$movementSql.AppendLine("INSERT INTO movements.movements (user_email, movement_type, amount, date, tag_id, description) SELECT '$($account.Email)', 'INVESTMENT', $($account.Investment), DATE '$investmentDate', $investmentTagId, '[DEMO] $suffix - Inversion' WHERE NOT EXISTS (SELECT 1 FROM movements.movements WHERE user_email = '$($account.Email)' AND date = DATE '$investmentDate' AND description = '[DEMO] $suffix - Inversion');")
    }
}
Invoke-LifeHubSql -Database "movementsdb" -Sql $movementSql.ToString()

Write-Host ""
Write-Host "Cuentas locales listas:"
foreach ($account in $accounts) {
    Write-Host "  $($account.Email) / $testPassword"
}
Write-Host "Estas cuentas solo existen en Keycloak y PostgreSQL local."
Write-Host "Se han cargado 8 meses de movimientos y 6 etiquetas por cuenta."

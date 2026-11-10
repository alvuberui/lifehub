[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$InfraRoot = $PSScriptRoot
$ComposeFile = Join-Path $InfraRoot "docker-compose.local.yml"
$EnvFile = Join-Path $InfraRoot ".env.local"
$ProjectName = "finanzapp-local"
$LocalEmail = "local@finanzapp.test"

function Invoke-LifeHubSql {
    param(
        [Parameter(Mandatory)][string]$Database,
        [Parameter(Mandatory)][string]$Sql
    )

    $Sql | & docker compose --project-name $ProjectName --env-file $EnvFile -f $ComposeFile exec -T postgres-lifehub psql `
        --username lifehubuser --dbname $Database -v ON_ERROR_STOP=1

    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo cargar la demo en la base $Database."
    }
}

function Invoke-LifeHubQuery {
    param(
        [Parameter(Mandatory)][string]$Database,
        [Parameter(Mandatory)][string]$Query
    )

    $result = & docker compose --project-name $ProjectName --env-file $EnvFile -f $ComposeFile exec -T postgres-lifehub psql `
        --username lifehubuser --dbname $Database -At -F "|" -c $Query

    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo consultar la base $Database."
    }
    return @($result)
}

if (-not (Test-Path $EnvFile)) {
    throw "No existe $EnvFile. Ejecuta primero start-local.ps1."
}

$today = Get-Date
$monthNames = @(
    "Alimentacion",
    "Vivienda",
    "Transporte",
    "Ocio",
    "Suscripciones",
    "Salud",
    "Compras"
)
$monthColors = @("#22C55E", "#60A5FA", "#F59E0B", "#A78BFA", "#F472B6", "#14B8A6", "#FB7185")

Write-Host "Esperando a que LifeHub esté listo para cargar datos de demostración..."
$ready = $false
for ($attempt = 1; $attempt -le 60; $attempt++) {
    try {
        $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 3
        if ($health.status -eq "UP") {
            $ready = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 2
    }
}

if (-not $ready) {
    throw "LifeHub no respondió a tiempo; no se han cargado datos de demostración."
}

$identitySql = @"
INSERT INTO identity.users (
    username, name, lastname, birthdate, initial_wealth,
    monthly_saving_percentage, monthly_necessary_expenses_percentage,
    monthly_discretionary_expenses_percentage, monthly_investment_percentage
)
SELECT '$LocalEmail', 'Usuario', 'Local', '1990-01-01', 12500.00, 20.00, 50.00, 20.00, 10.00
WHERE NOT EXISTS (
    SELECT 1 FROM identity.users WHERE username = '$LocalEmail'
);
"@
Invoke-LifeHubSql -Database "identitydb" -Sql $identitySql

$tagSql = New-Object System.Text.StringBuilder
[void]$tagSql.AppendLine("UPDATE tags.tags SET name = 'Alimentacion' WHERE name = 'Alimentaci??n' AND user_email = '$LocalEmail';")
foreach ($index in 0..($monthNames.Count - 1)) {
    $tagName = $monthNames[$index].Replace("'", "''")
    $tagColor = $monthColors[$index]
    [void]$tagSql.AppendLine(@"
INSERT INTO tags.tags (name, color, user_email)
SELECT '$tagName', '$tagColor', '$LocalEmail'
WHERE NOT EXISTS (
    SELECT 1 FROM tags.tags WHERE name = '$tagName' AND user_email = '$LocalEmail'
);
"@)
}
Invoke-LifeHubSql -Database "tagsdb" -Sql $tagSql.ToString()

$tagIds = @{}
$tagRows = Invoke-LifeHubQuery -Database "tagsdb" -Query "SELECT id, name FROM tags.tags WHERE user_email = '$LocalEmail';"
foreach ($row in $tagRows) {
    $parts = ([string]$row).Split('|', 2)
    if ($parts.Count -eq 2) {
        $tagIds[$parts[1]] = [long]$parts[0]
    }
}

foreach ($tagName in $monthNames) {
    if (-not $tagIds.ContainsKey($tagName)) {
        throw "No se encontró la etiqueta de demo '$tagName'."
    }
}

function Get-DemoDate {
    param([Parameter(Mandatory)][int]$MonthOffset, [Parameter(Mandatory)][int]$Day)

    $monthStart = Get-Date -Year $today.Year -Month $today.Month -Day 1
    $targetMonth = $monthStart.AddMonths($MonthOffset)
    $lastDay = [DateTime]::DaysInMonth($targetMonth.Year, $targetMonth.Month)
    $safeDay = [Math]::Min($Day, $lastDay)
    $candidate = Get-Date -Year $targetMonth.Year -Month $targetMonth.Month -Day $safeDay
    if ($candidate.Date -gt $today.Date) {
        return $today.Date.ToString("yyyy-MM-dd")
    }
    return $candidate.Date.ToString("yyyy-MM-dd")
}

function Add-DemoMovement {
    param(
        [Parameter(Mandatory)][System.Text.StringBuilder]$Builder,
        [Parameter(Mandatory)][string]$Date,
        [Parameter(Mandatory)][string]$Type,
        [Parameter(Mandatory)][decimal]$Amount,
        [Parameter(Mandatory)][string]$Description,
        [Parameter(Mandatory)][long]$TagId
    )

    $safeDescription = $Description.Replace("'", "''")
    $amountSql = $Amount.ToString("0.00", [Globalization.CultureInfo]::InvariantCulture)
    [void]$Builder.AppendLine(@"
INSERT INTO movements.movements (user_email, movement_type, amount, date, tag_id, description)
SELECT '$LocalEmail', '$Type', $amountSql, DATE '$Date', $TagId, '$safeDescription'
WHERE NOT EXISTS (
    SELECT 1 FROM movements.movements
    WHERE user_email = '$LocalEmail' AND date = DATE '$Date'
      AND amount = $amountSql AND description = '$safeDescription'
);
"@)
}

$movementSql = New-Object System.Text.StringBuilder
for ($monthOffset = -11; $monthOffset -le 0; $monthOffset++) {
    $monthNumber = [Math]::Abs($monthOffset)
    $income = 2850 + (($monthNumber * 73) % 260)
    $groceries = 315 + (($monthNumber * 17) % 90)
    $transport = 95 + (($monthNumber * 11) % 55)
    $leisure = 80 + (($monthNumber * 13) % 110)
    $subscription = 42
    $investment = 220 + (($monthNumber * 19) % 100)

    Add-DemoMovement $movementSql (Get-DemoDate $monthOffset 1) "BENEFIT" $income "[DEMO] Nomina mensual" $tagIds["Vivienda"]
    Add-DemoMovement $movementSql (Get-DemoDate $monthOffset 2) "ESSENTIAL_EXPENSE" 920 "[DEMO] Alquiler / hipoteca" $tagIds["Vivienda"]
    Add-DemoMovement $movementSql (Get-DemoDate $monthOffset 5) "ESSENTIAL_EXPENSE" $groceries "[DEMO] Compra semanal" $tagIds["Alimentacion"]
    Add-DemoMovement $movementSql (Get-DemoDate $monthOffset 8) "ESSENTIAL_EXPENSE" $transport "[DEMO] Transporte" $tagIds["Transporte"]
    Add-DemoMovement $movementSql (Get-DemoDate $monthOffset 12) "DISCRETIONARY_EXPENSE" $leisure "[DEMO] Plan de ocio" $tagIds["Ocio"]
    Add-DemoMovement $movementSql (Get-DemoDate $monthOffset 18) "DISCRETIONARY_EXPENSE" $subscription "[DEMO] Suscripciones" $tagIds["Suscripciones"]
    Add-DemoMovement $movementSql (Get-DemoDate $monthOffset 23) "INVESTMENT" $investment "[DEMO] Aportacion inversion" $tagIds["Compras"]

    if (($monthOffset % 3) -eq 0) {
        Add-DemoMovement $movementSql (Get-DemoDate $monthOffset 15) "ESSENTIAL_EXPENSE" 68 "[DEMO] Farmacia y salud" $tagIds["Salud"]
    }
}
Invoke-LifeHubSql -Database "movementsdb" -Sql $movementSql.ToString()

$cleanupForecastSql = @"
DELETE FROM movements.forecast_events
WHERE user_email = '$LocalEmail'
  AND name IN ('Viaje de ejemplo', 'Boda de ejemplo');
"@
Invoke-LifeHubSql -Database "movementsdb" -Sql $cleanupForecastSql

Write-Host "Datos de demostración cargados para $LocalEmail (idempotente)."

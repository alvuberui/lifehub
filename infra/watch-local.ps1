[CmdletBinding()]
param()

$ErrorActionPreference = "Continue"
$InfraRoot = $PSScriptRoot
$BackendRoot = Split-Path -Parent $InfraRoot
$ProcessFile = Join-Path $InfraRoot ".local\processes.json"
$LogRoot = Join-Path $InfraRoot ".local\logs"
$EnvFile = Join-Path $InfraRoot ".env.local"
$MavenWrapper = Join-Path $BackendRoot "mvnw.cmd"

function Get-ServiceSnapshot {
    param([Parameter(Mandatory)][string]$ServiceRoot)

    $sourceRoots = @(
        (Join-Path $ServiceRoot "src\main\java"),
        (Join-Path $ServiceRoot "src\main\resources")
    )

    $files = foreach ($sourceRoot in $sourceRoots) {
        if (Test-Path $sourceRoot) {
            Get-ChildItem -Path $sourceRoot -File -Recurse
        }
    }

    return (($files | Sort-Object FullName | ForEach-Object {
        "$($_.FullName)|$($_.LastWriteTimeUtc.Ticks)|$($_.Length)"
    }) -join "`n")
}

function Get-EnvValue {
    param([Parameter(Mandatory)][string]$Name)

    if (-not (Test-Path $EnvFile)) {
        return $null
    }

    $line = Get-Content $EnvFile | Where-Object {
        $_ -match "^$([regex]::Escape($Name))=(.*)$"
    } | Select-Object -First 1

    if ($null -eq $line) {
        return $null
    }

    return ($line -replace "^$([regex]::Escape($Name))=", "")
}

function Stop-ManagedProcess {
    param([Parameter(Mandatory)][string]$Name)

    if (-not (Test-Path $ProcessFile)) {
        return
    }

    $processes = @(Get-Content $ProcessFile -Raw | ConvertFrom-Json)
    $target = $processes | Where-Object { $_.name -eq $Name } | Select-Object -First 1
    if ($null -ne $target -and $target.pid) {
        & taskkill.exe /PID $target.pid /T /F | Out-Null
    }
}

function Start-ManagedProcess {
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
        $startParameters = @{
            FilePath = "cmd.exe"
            ArgumentList = @("/d", "/c", $CommandLine)
            WorkingDirectory = $WorkingDirectory
            RedirectStandardOutput = $stdout
            RedirectStandardError = $stderr
            WindowStyle = "Hidden"
            PassThru = $true
        }
        $process = Start-Process @startParameters
    } finally {
        foreach ($entry in $oldEnvironment.GetEnumerator()) {
            [Environment]::SetEnvironmentVariable($entry.Key, $entry.Value, "Process")
        }
    }

    return $process.Id
}

function Restart-LifeHub {
    Push-Location $BackendRoot
    try {
        & $MavenWrapper -f (Join-Path $BackendRoot "pom.xml") -pl lifehub -am package -DskipTests
        if ($LASTEXITCODE -ne 0) {
            Write-Host "La compilacion de LifeHub ha fallado. Se reintentara en el siguiente cambio."
            return
        }
    } finally {
        Pop-Location
    }

    Stop-ManagedProcess -Name "lifehub"
    Start-Sleep -Milliseconds 500
    $newPid = Start-ManagedProcess `
        -Name "lifehub" `
        -WorkingDirectory (Join-Path $BackendRoot "lifehub") `
        -Environment (@{
            SPRING_PROFILES_ACTIVE = "local"
            USER_DB_PASSWORD = Get-EnvValue -Name "USER_DB_PASSWORD"
            MOVEMENTS_DB_PASSWORD = Get-EnvValue -Name "MOVEMENTS_DB_PASSWORD"
            TAGS_DB_PASSWORD = Get-EnvValue -Name "TAGS_DB_PASSWORD"
            INTERNAL_SERVICE_TOKEN = Get-EnvValue -Name "INTERNAL_SERVICE_TOKEN"
            LIFEHUB_INTERNAL_BASE_URL = "http://localhost:8080"
        }) `
        -CommandLine ('java -jar "{0}"' -f (Join-Path $BackendRoot "lifehub\target\lifehub-0.0.1-SNAPSHOT.jar"))

    $processes = @(Get-Content $ProcessFile -Raw | ConvertFrom-Json | Where-Object { $_.name -ne "lifehub" })
    $processes += [pscustomobject]@{ name = "lifehub"; pid = $newPid }
    $processes | ConvertTo-Json | Set-Content -Path $ProcessFile -Encoding utf8
    Write-Host "LifeHub recompilado y reiniciado."
}

$serviceRoot = Join-Path $BackendRoot "lifehub"
$lastSnapshot = Get-ServiceSnapshot $serviceRoot
Write-Host "Observador backend activo. Los cambios reinician LifeHub automaticamente."

while ($true) {
    Start-Sleep -Seconds 1
    $currentSnapshot = Get-ServiceSnapshot $serviceRoot
    if ($currentSnapshot -eq $lastSnapshot) {
        continue
    }

    Write-Host "Cambio detectado en LifeHub."
    Restart-LifeHub
    $lastSnapshot = $currentSnapshot
}

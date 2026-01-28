# Benchmark Gradle/test/compile startup speed improvements
# Compares: configuration cache, daemon cold start (no build file changes)

param(
    [switch]$Clean = $false,
    [int]$Iterations = 3
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..

Write-Host "`n=== Gradle Startup Benchmark ===" -ForegroundColor Cyan
Write-Host "Iterations: $Iterations`n" -ForegroundColor Gray

function Measure-GradleTask {
    param(
        [string]$Task,
        [string[]]$ExtraArgs = @(),
        [string]$Description
    )
    $times = @()
    for ($i = 1; $i -le $Iterations; $i++) {
        Write-Host "  Run ${i}/${Iterations}: $Description..." -NoNewline -ForegroundColor Gray
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        $argList = @($Task) + @($ExtraArgs)
        $prevErr = $ErrorActionPreference
        $ErrorActionPreference = "SilentlyContinue"
        & .\gradlew.bat $argList 2>$null | Out-Null
        $ErrorActionPreference = $prevErr
        $stopwatch.Stop()
        $elapsed = $stopwatch.ElapsedMilliseconds
        $times += $elapsed
        Write-Host " ${elapsed}ms" -ForegroundColor Green
        if ($LASTEXITCODE -ne 0) {
            Write-Host "    ERROR: Task failed (exit $LASTEXITCODE)" -ForegroundColor Red
            return $null
        }
    }
    $avg = [math]::Round(($times | Measure-Object -Average).Average, 0)
    return @{ Average = $avg; Min = ($times | Measure-Object -Minimum).Minimum; Max = ($times | Measure-Object -Maximum).Maximum }
}

try {
    if ($Clean) {
        Write-Host "Cleaning build and config cache..." -ForegroundColor Gray
        & .\gradlew.bat clean --no-configuration-cache 2>&1 | Out-Null
        Remove-Item -Path ".gradle\configuration-cache" -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host ""
    }

    # 1. Configuration cache impact
    Write-Host "1. Configuration Cache (compileJava)" -ForegroundColor Yellow
    Write-Host "   WITH configuration cache (subsequent runs):" -ForegroundColor Cyan
    $withCache = Measure-GradleTask -Task "compileJava" -ExtraArgs @("--configuration-cache") -Description "compileJava (config cache)"
    Write-Host "   WITHOUT configuration cache:" -ForegroundColor Cyan
    $withoutCache = Measure-GradleTask -Task "compileJava" -ExtraArgs @("--no-configuration-cache") -Description "compileJava (no cache)"
    if ($withCache -and $withoutCache) {
        $pct = [math]::Round((($withoutCache.Average - $withCache.Average) / $withoutCache.Average) * 100, 1)
        Write-Host "   Result: Config cache saves ~${pct}% (no cache: $($withoutCache.Average)ms -> with cache: $($withCache.Average)ms)`n" -ForegroundColor Green
    }

    # 2. Daemon cold start impact
    Write-Host "2. Daemon Cold Start (compileJava)" -ForegroundColor Yellow
    Write-Host "   Stopping daemon..." -ForegroundColor Gray
    $ErrorActionPreference = "SilentlyContinue"
    & .\gradlew.bat --stop 2>$null | Out-Null
    $ErrorActionPreference = "Stop"
    Start-Sleep -Seconds 2
    Write-Host "   WITHOUT daemon (cold JVM each run):" -ForegroundColor Cyan
    $noDaemon = Measure-GradleTask -Task "compileJava" -ExtraArgs @("--no-daemon", "--no-configuration-cache") -Description "compileJava (no daemon)"
    Write-Host "   WITH daemon (warm, after one run):" -ForegroundColor Cyan
    $ErrorActionPreference = "SilentlyContinue"
    & .\gradlew.bat compileJava --configuration-cache 2>$null | Out-Null
    $ErrorActionPreference = "Stop"
    $withDaemon = Measure-GradleTask -Task "compileJava" -ExtraArgs @("--configuration-cache") -Description "compileJava (daemon)"
    if ($noDaemon -and $withDaemon) {
        $pct = [math]::Round((($noDaemon.Average - $withDaemon.Average) / $noDaemon.Average) * 100, 1)
        Write-Host "   Result: Daemon saves ~${pct}% (no daemon: $($noDaemon.Average)ms -> daemon: $($withDaemon.Average)ms)`n" -ForegroundColor Green
    }

    Write-Host "=== Summary ===" -ForegroundColor Cyan
    Write-Host "See docs/setup/FAST_STARTUP_JVM_OPTIONS.md and docs/setup/BENCHMARK_GRADLE_STARTUP_RESULTS.md" -ForegroundColor Gray
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    exit 1
}

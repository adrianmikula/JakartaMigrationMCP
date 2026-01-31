# Benchmark: Gradle vs Mill startup / first-task performance
# Run from repo root: .\scripts\benchmark-build-startup.ps1
# For a full comparison (including successful compile/test), run on Linux or WSL where Mill build.sc compiles.

$ErrorActionPreference = "Continue"
$results = @()

Write-Host "`n=== Build tool startup benchmark ===" -ForegroundColor Cyan
Write-Host ""

# 1. Gradle: help (cold, no daemon) - measures Gradle JVM + config load
Write-Host "1. Gradle help (--no-daemon)..." -ForegroundColor Yellow
$gHelp = Measure-Command { & .\gradlew.bat help --no-daemon -q 2>&1 | Out-Null }
$results += [PSCustomObject]@{ Tool = "Gradle"; Task = "help (cold)"; Seconds = [math]::Round($gHelp.TotalSeconds, 2); Note = "exit may be non-zero" }
Write-Host "   $([math]::Round($gHelp.TotalSeconds, 2)) s" -ForegroundColor White

# 2. Gradle: help (warm, daemon) - if daemon already up
Write-Host "2. Gradle help (daemon)..." -ForegroundColor Yellow
$gHelpWarm = Measure-Command { & .\gradlew.bat help -q 2>&1 | Out-Null }
$results += [PSCustomObject]@{ Tool = "Gradle"; Task = "help (warm)"; Seconds = [math]::Round($gHelpWarm.TotalSeconds, 2); Note = "" }
Write-Host "   $([math]::Round($gHelpWarm.TotalSeconds, 2)) s" -ForegroundColor White

# 3. Mill: show version - measures Mill startup + build script load
Write-Host "3. Mill show version..." -ForegroundColor Yellow
$mVersion = Measure-Command { & .\mill.bat show version 2>&1 | Out-Null }
$results += [PSCustomObject]@{ Tool = "Mill"; Task = "show version"; Seconds = [math]::Round($mVersion.TotalSeconds, 2); Note = "" }
Write-Host "   $([math]::Round($mVersion.TotalSeconds, 2)) s" -ForegroundColor White

# 4. Mill: compile (on Windows often fails at build.sc compile; time still meaningful for startup)
Write-Host "4. Mill jakartaMigrationMcp.compile..." -ForegroundColor Yellow
$mCompile = Measure-Command { & .\mill.bat jakartaMigrationMcp.compile 2>&1 | Out-Null }
$results += [PSCustomObject]@{ Tool = "Mill"; Task = "compile"; Seconds = [math]::Round($mCompile.TotalSeconds, 2); Note = "may fail on Windows (ivy)" }
Write-Host "   $([math]::Round($mCompile.TotalSeconds, 2)) s" -ForegroundColor White

Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor Cyan
$results | Format-Table -AutoSize
Write-Host "For full compile/test comparison, run on Linux or WSL (Mill build.sc compiles there)." -ForegroundColor Gray

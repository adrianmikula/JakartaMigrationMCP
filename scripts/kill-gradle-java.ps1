# Stop Gradle daemons and optionally kill Java processes that may lock the filesystem on Windows.
# Run before rebuild/retest to avoid "file is in use" or daemon conflicts.
# Usage:
#   .\scripts\kill-gradle-java.ps1           # Stop Gradle daemons only (safe)
#   .\scripts\kill-gradle-java.ps1 -Force    # Also kill Java processes related to Gradle/this project

param(
    [switch]$Force  # If set, also kill Java processes that look like Gradle daemon, workers, or this project
)

# 1. Stop Gradle daemons gracefully (releases most file locks)
if (Test-Path "gradlew.bat") {
    Write-Host "Stopping Gradle daemons..." -ForegroundColor Cyan
    $null = & .\gradlew.bat --stop 2>&1  # Ignore stderr (e.g. Java restricted-method WARNING)
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Gradle daemons stopped." -ForegroundColor Green
    } else {
        Write-Host "  (No daemons running or wrapper failed; continuing.)" -ForegroundColor Gray
    }
} else {
    Write-Host "gradlew.bat not found; skipping Gradle --stop." -ForegroundColor Gray
}

if (-not $Force) {
    Write-Host "Done. Use -Force to also kill Java processes (Gradle workers, test JVMs, bootRun)." -ForegroundColor Gray
    exit 0
}

# 2. Kill Java processes that are Gradle-related or this project (avoid killing IDE/other Java apps)
Write-Host "Looking for Gradle/project Java processes to kill..." -ForegroundColor Cyan
$killed = 0
try {
    $javaProcesses = Get-CimInstance Win32_Process -Filter "name = 'java.exe'" -ErrorAction SilentlyContinue
    if (-not $javaProcesses) {
        Write-Host "  No java.exe processes found." -ForegroundColor Gray
        exit 0
    }
    foreach ($p in $javaProcesses) {
        $cmd = $p.CommandLine
        if (-not $cmd) { continue }
        # Match only Gradle daemon/workers or our app JAR (avoid killing IDE/other Java)
        $isGradle = $cmd -match 'GradleDaemon|GradleWorker|org\.gradle\.launcher|gradle.*daemon'
        $isOurJar = $cmd -match 'jakarta-migration-mcp[^\\]*\.jar|build\\libs\\jakarta'
        if ($isGradle -or $isOurJar) {
            Write-Host "  Killing PID $($p.ProcessId): $($cmd.Substring(0, [Math]::Min(80, $cmd.Length)))..." -ForegroundColor Yellow
            Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
            $killed++
        }
    }
    if ($killed -gt 0) {
        Write-Host "  Stopped $killed Java process(es)." -ForegroundColor Green
    } else {
        Write-Host "  No Gradle/project Java processes found (other Java processes left alone)." -ForegroundColor Gray
    }
} catch {
    Write-Host "  Could not enumerate/kill Java processes: $_" -ForegroundColor Red
    exit 1
}

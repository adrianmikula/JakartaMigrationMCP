# Fast Test Loop Script
# Optimized for snappy agentic AI feedback during development
param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("compile", "fast", "core", "pdf", "all")]
    [string]$Mode
)

$ErrorActionPreference = "Stop"

# Check if gradlew exists
if (-not (Test-Path ".\gradlew.bat")) {
    Write-Error "gradlew.bat not found in current directory"
    exit 1
}

# Common Gradle flags for fast execution
# Note: --no-daemon removed for <10s target - daemon provides faster startup
$gradleFlags = @("--configuration-cache", "--parallel")

Write-Host "🚀 Fast Test Loop - Mode: $Mode" -ForegroundColor Cyan

switch ($Mode) {
    "compile" {
        Write-Host "📦 Running compilation check..." -ForegroundColor Yellow
        & ".\gradlew.bat" ":premium-core-engine:compileJava" @gradleFlags
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Compilation successful - Ready for fast development!" -ForegroundColor Green
        }
    }

    "fast" {
        Write-Host "⚡ Running fast unit tests only..." -ForegroundColor Yellow
        & ".\gradlew.bat" ":premium-core-engine:fastTest" @gradleFlags
    }

    "core" {
        Write-Host "🔧 Running core functionality tests..." -ForegroundColor Yellow
        & ".\gradlew.bat" ":premium-core-engine:coreTest" @gradleFlags
    }

    "pdf" {
        Write-Host "📄 Running PDF generation tests only..." -ForegroundColor Yellow
        & ".\gradlew.bat" ":premium-core-engine:test" "--tests" "*PdfReportServiceImplTest*" @gradleFlags
    }

    "all" {
        Write-Host "🧪 Running all tests..." -ForegroundColor Yellow
        & ".\gradlew.bat" ":premium-core-engine:test" @gradleFlags
    }
}

exit $LASTEXITCODE

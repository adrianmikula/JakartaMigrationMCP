# Build and Release Script for Jakarta Migration MCP Server (Windows)
# This script builds the JAR, packages it, and prepares for release

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

Set-Location $ProjectRoot

Write-Host "🚀 Building Jakarta Migration MCP Server..." -ForegroundColor Green
Write-Host ""

# Get version from build.gradle.kts
$VersionLine = Select-String -Path "build.gradle.kts" -Pattern 'version\s*=\s*"' | Select-Object -First 1
$Version = if ($VersionLine) {
    if ($VersionLine.Line -match 'version\s*=\s*"([^"]+)"') {
        $matches[1]
    } else {
        "1.0.0-SNAPSHOT"
    }
} else {
    "1.0.0-SNAPSHOT"
}

$VersionClean = $Version -replace '-SNAPSHOT', ''

Write-Host "📦 Version: $Version" -ForegroundColor Cyan
Write-Host ""

# Clean previous builds
Write-Host "🧹 Cleaning previous builds..." -ForegroundColor Yellow
& .\gradlew.bat clean

# Build JAR
Write-Host "🔨 Building JAR..." -ForegroundColor Yellow
& .\gradlew.bat bootJar --no-daemon

# Find the built JAR
$JarFiles = Get-ChildItem -Path "build\libs" -Filter "*.jar" | Where-Object { $_.Name -notlike "*-plain.jar" }
$JarFile = $JarFiles | Select-Object -First 1

if (-not $JarFile) {
    Write-Host "❌ ERROR: JAR file not found in build\libs\" -ForegroundColor Red
    exit 1
}

Write-Host "✅ JAR built: $($JarFile.FullName)" -ForegroundColor Green
Write-Host ""

# Create release directory
$ReleaseDir = "release"
if (-not (Test-Path $ReleaseDir)) {
    New-Item -ItemType Directory -Path $ReleaseDir | Out-Null
}

# Copy JAR to release directory with standardized name
$ReleaseJar = Join-Path $ReleaseDir "jakarta-migration-mcp-${VersionClean}.jar"
Copy-Item $JarFile.FullName $ReleaseJar

Write-Host "📦 Release JAR: $ReleaseJar" -ForegroundColor Cyan
Write-Host ""

# Create checksums
Write-Host "🔐 Creating checksums..." -ForegroundColor Yellow
Set-Location $ReleaseDir
$JarName = "jakarta-migration-mcp-${VersionClean}.jar"

# SHA256
$Sha256 = (Get-FileHash -Path $JarName -Algorithm SHA256).Hash
"$Sha256  $JarName" | Out-File -FilePath "${JarName}.sha256" -Encoding ASCII

# MD5 (if available)
try {
    $Md5 = (Get-FileHash -Path $JarName -Algorithm MD5).Hash
    "$Md5  $JarName" | Out-File -FilePath "${JarName}.md5" -Encoding ASCII
} catch {
    # MD5 not available on all systems
}

Set-Location $ProjectRoot

# Prepare npm package
Write-Host "📦 Preparing npm package..." -ForegroundColor Yellow
npm pack --dry-run 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Note: npm pack requires package.json (already created)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "✅ Build complete!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Release artifacts:" -ForegroundColor Cyan
Write-Host "   - JAR: $ReleaseJar"
Write-Host "   - SHA256: $ReleaseDir\jakarta-migration-mcp-${VersionClean}.jar.sha256"
Write-Host ""
Write-Host "🚀 Next steps:" -ForegroundColor Yellow
Write-Host "   1. Test the JAR: java -jar $ReleaseJar"
Write-Host "   2. Create GitHub release: gh release create v${VersionClean} $ReleaseJar"
Write-Host "   3. Publish to npm: npm publish"
Write-Host ""


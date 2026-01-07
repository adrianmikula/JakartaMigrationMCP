# Build and Release Script for Jakarta Migration MCP Server (Windows)
# This script builds the JAR, packages it, and prepares for release

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

Set-Location $ProjectRoot

Write-Host "[BUILD] Building Jakarta Migration MCP Server..." -ForegroundColor Green
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

Write-Host "[VERSION] Version: $Version" -ForegroundColor Cyan
Write-Host ""

# Clean previous builds
Write-Host "[CLEAN] Cleaning previous builds..." -ForegroundColor Yellow
& .\gradlew.bat clean

# Build JAR
Write-Host "[BUILD] Building JAR..." -ForegroundColor Yellow
& .\gradlew.bat bootJar --no-daemon

# Find the built JAR
$JarFiles = Get-ChildItem -Path "build\libs" -Filter "*.jar" | Where-Object { $_.Name -notlike "*-plain.jar" }
$JarFile = $JarFiles | Select-Object -First 1

if (-not $JarFile) {
        Write-Host "[ERROR] JAR file not found in build\libs\" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] JAR built: $($JarFile.FullName)" -ForegroundColor Green
Write-Host ""

# Create release directory
$ReleaseDir = "release"
if (-not (Test-Path $ReleaseDir)) {
    New-Item -ItemType Directory -Path $ReleaseDir | Out-Null
}

# Copy JAR to release directory with standardized name
$ReleaseJar = Join-Path $ReleaseDir "jakarta-migration-mcp-${VersionClean}.jar"
Copy-Item $JarFile.FullName $ReleaseJar

Write-Host "[RELEASE] Release JAR: $ReleaseJar" -ForegroundColor Cyan
Write-Host ""

# Create checksums
Write-Host "[CHECKSUM] Creating checksums..." -ForegroundColor Yellow
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

# Update package.json version to match JAR version
Write-Host "[NPM] Updating package.json version..." -ForegroundColor Yellow
$PackageJsonPath = Join-Path $ProjectRoot "package.json"
if (Test-Path $PackageJsonPath) {
    $PackageJson = Get-Content $PackageJsonPath -Raw | ConvertFrom-Json
    $OldVersion = $PackageJson.version
    $PackageJson.version = $VersionClean
    $PackageJson | ConvertTo-Json -Depth 10 | Set-Content $PackageJsonPath -Encoding UTF8
    Write-Host "[OK] Updated package.json version from $OldVersion to $VersionClean" -ForegroundColor Green
    
    # Validate package.json files exist
    if ($PackageJson.files) {
        $MissingFiles = @()
        foreach ($file in $PackageJson.files) {
            $FilePath = Join-Path $ProjectRoot $file
            if (-not (Test-Path $FilePath)) {
                $MissingFiles += $file
            }
        }
        if ($MissingFiles.Count -gt 0) {
            Write-Host "[WARN] Some files in package.json 'files' array are missing:" -ForegroundColor Yellow
            foreach ($file in $MissingFiles) {
                Write-Host "   - $file" -ForegroundColor Yellow
            }
        }
    }
    
    # Check if repository URL is still placeholder
    if ($PackageJson.repository.url -like "*your-org*" -or $PackageJson.repository.url -like "*your-repo*") {
        Write-Host "[WARN] package.json repository URL is still a placeholder" -ForegroundColor Yellow
        Write-Host "   Update it with your actual GitHub repository URL before publishing" -ForegroundColor Yellow
    }
    
    # Check if main/bin files exist
    if ($PackageJson.main -and -not (Test-Path (Join-Path $ProjectRoot $PackageJson.main))) {
        Write-Host "[WARN] package.json 'main' file not found: $($PackageJson.main)" -ForegroundColor Yellow
    }
    if ($PackageJson.bin) {
        foreach ($binName in $PackageJson.bin.PSObject.Properties.Name) {
            $binPath = Join-Path $ProjectRoot $PackageJson.bin.$binName
            if (-not (Test-Path $binPath)) {
                Write-Host "[WARN] package.json 'bin' file not found: $($PackageJson.bin.$binName)" -ForegroundColor Yellow
            }
        }
    }
} else {
    Write-Host "[ERROR] package.json not found" -ForegroundColor Red
    exit 1
}

# Prepare npm package
Write-Host "[NPM] Preparing npm package..." -ForegroundColor Yellow
$NpmPackOutput = npm pack --dry-run 2>&1 | Out-String
$NpmPackExitCode = $LASTEXITCODE
if ($NpmPackExitCode -eq 0) {
    Write-Host "[OK] npm package ready" -ForegroundColor Green
} else {
    Write-Host "[WARN] npm pack check failed" -ForegroundColor Yellow
    Write-Host $NpmPackOutput
}

Write-Host ""
Write-Host "[OK] Build complete!" -ForegroundColor Green
Write-Host ""
Write-Host "[ARTIFACTS] Release artifacts:" -ForegroundColor Cyan
Write-Host "   - JAR: $ReleaseJar"
Write-Host "   - SHA256: $ReleaseDir\jakarta-migration-mcp-${VersionClean}.jar.sha256"
Write-Host "   - package.json version: $VersionClean"
Write-Host ""

# Ask if user wants to publish
Write-Host "[NEXT] Next steps:" -ForegroundColor Yellow
Write-Host "   1. Test the JAR: java -jar $ReleaseJar"
Write-Host "   2. Create GitHub release: gh release create v${VersionClean} $ReleaseJar"
Write-Host "   3. Publish to npm: npm publish"
Write-Host ""

$PublishNpm = Read-Host "Publish to npm now? (y/N)"
if ($PublishNpm -eq "y" -or $PublishNpm -eq "Y") {
    Write-Host ""
    Write-Host "[PUBLISH] Publishing to npm..." -ForegroundColor Yellow
    
    # Check if logged in
    $NpmWhoamiOutput = npm whoami 2>&1 | Out-String
    $NpmWhoamiExitCode = $LASTEXITCODE
    if ($NpmWhoamiExitCode -ne 0) {
        Write-Host "[ERROR] Not logged in to npm. Run: npm login" -ForegroundColor Red
        Write-Host "   Then run: npm publish" -ForegroundColor Yellow
        exit 1
    } else {
        $NpmUser = ($NpmWhoamiOutput -split "`n" | Where-Object { $_.Trim() -ne "" } | Select-Object -First 1).Trim()
        Write-Host "[OK] Logged in as: $NpmUser" -ForegroundColor Green
        
        # Check if version already exists
        Write-Host ""
        Write-Host "[NPM] Checking if version $VersionClean already exists..." -ForegroundColor Yellow
        $NpmViewOutput = npm view "@jakarta-migration/mcp-server@$VersionClean" version 2>&1 | Out-String
        $NpmViewExitCode = $LASTEXITCODE
        if ($NpmViewExitCode -eq 0 -and $NpmViewOutput.Trim() -eq $VersionClean) {
            Write-Host "[WARN] Version $VersionClean already exists on npm" -ForegroundColor Yellow
            $Overwrite = Read-Host "Publish anyway? (y/N)"
            if ($Overwrite -ne "y" -and $Overwrite -ne "Y") {
                Write-Host "[INFO] Skipping npm publish" -ForegroundColor Gray
                exit 0
            }
        } else {
            Write-Host "[OK] Version $VersionClean is new" -ForegroundColor Green
        }
        
        # Check if GitHub release exists (npm package downloads JAR from releases)
        Write-Host ""
        Write-Host "[INFO] Note: The npm package downloads the JAR from GitHub releases." -ForegroundColor Yellow
        Write-Host "   Make sure you create a GitHub release first:" -ForegroundColor Yellow
        Write-Host "   gh release create v${VersionClean} $ReleaseJar" -ForegroundColor Cyan
        
        $CreateRelease = Read-Host "Create GitHub release now? (y/N)"
        if ($CreateRelease -eq "y" -or $CreateRelease -eq "Y") {
            Write-Host ""
            Write-Host "[RELEASE] Creating GitHub release..." -ForegroundColor Yellow
            gh release create "v${VersionClean}" $ReleaseJar --title "v${VersionClean}" --notes "Jakarta Migration MCP Server v${VersionClean}"
            $GhReleaseExitCode = $LASTEXITCODE
            if ($GhReleaseExitCode -eq 0) {
                Write-Host "[OK] GitHub release created" -ForegroundColor Green
            } else {
                Write-Host "[WARN] GitHub release creation failed. Continue anyway? (y/N)" -ForegroundColor Yellow
                $Continue = Read-Host
                if ($Continue -ne "y" -and $Continue -ne "Y") {
                    Write-Host "[ERROR] Aborted. Create release manually, then run: npm publish" -ForegroundColor Red
                    exit 1
                }
            }
        }
        
        Write-Host ""
        Write-Host "[PUBLISH] Publishing to npm..." -ForegroundColor Yellow
        
        # Check if this is the first publish (scoped packages need --access public)
        $null = npm view "@jakarta-migration/mcp-server" 2>&1 | Out-String
        $IsFirstPublish = $LASTEXITCODE -ne 0
        
        if ($IsFirstPublish) {
            Write-Host "[INFO] First publish detected, using --access public" -ForegroundColor Yellow
            npm publish --access public
        } else {
            npm publish
        }
        
        $PublishExitCode = $LASTEXITCODE
        if ($PublishExitCode -eq 0) {
            Write-Host ""
            Write-Host "[OK] Published to npm successfully!" -ForegroundColor Green
            Write-Host "   Package: @jakarta-migration/mcp-server@$VersionClean" -ForegroundColor Cyan
            Write-Host "   Install: npm install -g @jakarta-migration/mcp-server" -ForegroundColor Cyan
            Write-Host ""
            Write-Host "[NEXT] Next steps:" -ForegroundColor Yellow
            Write-Host "   1. Test installation: npm install -g @jakarta-migration/mcp-server" -ForegroundColor Cyan
            Write-Host "   2. Update documentation with npm install instructions" -ForegroundColor Cyan
            Write-Host "   3. List on Glama.ai with npm package info" -ForegroundColor Cyan
        } else {
            Write-Host ""
            Write-Host "[ERROR] npm publish failed (exit code: $PublishExitCode)" -ForegroundColor Red
            Write-Host "   Check npm logs and try again" -ForegroundColor Yellow
            Write-Host "   Common issues:" -ForegroundColor Yellow
            Write-Host "     - Version already exists (use --force to overwrite)" -ForegroundColor Yellow
            Write-Host "     - Not logged in (run: npm login)" -ForegroundColor Yellow
            Write-Host "     - Missing permissions (check npm organization access)" -ForegroundColor Yellow
            exit 1
        }
    }
} else {
    Write-Host ""
    Write-Host "[INFO] To publish later:" -ForegroundColor Gray
    Write-Host "   1. Create GitHub release: gh release create v${VersionClean} $ReleaseJar" -ForegroundColor Gray
    Write-Host "   2. Publish to npm: npm publish" -ForegroundColor Gray
}

Write-Host ""


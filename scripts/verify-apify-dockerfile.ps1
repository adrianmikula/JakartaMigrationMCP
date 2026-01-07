# Verify Dockerfile meets Apify requirements
# This script checks the Dockerfile against Apify's requirements for Actor deployment

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Apify Dockerfile Verification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$errors = @()
$warnings = @()
$success = @()

# Read the Dockerfile
$dockerfilePath = "Dockerfile"
if (-not (Test-Path $dockerfilePath)) {
    Write-Host "ERROR: Dockerfile not found at $dockerfilePath" -ForegroundColor Red
    exit 1
}

$dockerfileContent = Get-Content $dockerfilePath -Raw

Write-Host "Checking Dockerfile requirements..." -ForegroundColor Yellow
Write-Host ""

# 1. Check for Apify base image
Write-Host "[1/10] Checking for Apify base image..." -ForegroundColor Yellow
if ($dockerfileContent -match "FROM apify/actor-node") {
    $success += "OK: Uses Apify base image (apify/actor-node)"
    Write-Host "  ✓ Uses Apify base image" -ForegroundColor Green
} else {
    $errors += "✗ Must use Apify base image (apify/actor-node)"
    Write-Host "  ✗ Missing Apify base image" -ForegroundColor Red
}

# 2. Check for multi-stage build
Write-Host "[2/10] Checking for multi-stage build..." -ForegroundColor Yellow
if ($dockerfileContent -match "AS builder" -or $dockerfileContent -match "FROM.*AS") {
    $success += "OK: Uses multi-stage build"
    Write-Host "  ✓ Multi-stage build detected" -ForegroundColor Green
} else {
    $warnings += "⚠ Consider using multi-stage build for smaller image size"
    Write-Host "  ⚠ No multi-stage build detected" -ForegroundColor Yellow
}

# 3. Check for non-root user
Write-Host "[3/10] Checking for non-root user..." -ForegroundColor Yellow
if ($dockerfileContent -match "USER (myuser|apify)" -and $dockerfileContent -match "USER root" -and $dockerfileContent -match "USER (myuser|apify)") {
    $success += "OK: Uses non-root user (myuser or apify)"
    Write-Host "  ✓ Non-root user configured" -ForegroundColor Green
} else {
    $errors += "✗ Must use non-root user (myuser or apify) and switch back from root"
    Write-Host "  ✗ Non-root user not properly configured" -ForegroundColor Red
}

# 4. Check working directory
Write-Host "[4/10] Checking working directory..." -ForegroundColor Yellow
if ($dockerfileContent -match "WORKDIR /home/(myuser|apify)") {
    $success += "OK: Uses Apify standard working directory (/home/myuser or /home/apify)"
    Write-Host "  ✓ Correct working directory" -ForegroundColor Green
} else {
    $warnings += "⚠ Working directory should be /home/myuser or /home/apify"
    Write-Host "  ⚠ Working directory may not match Apify standard" -ForegroundColor Yellow
}

# 5. Check for Java installation
Write-Host "[5/10] Checking for Java installation..." -ForegroundColor Yellow
if ($dockerfileContent -match "(apk add|apt-get install).*openjdk.*jre" -or $dockerfileContent -match "openjdk.*jre") {
    $success += "OK: Java JRE installation detected"
    Write-Host "  ✓ Java JRE installation found" -ForegroundColor Green
} else {
    $errors += "✗ Must install Java JRE for Spring Boot application"
    Write-Host "  ✗ Java JRE installation not found" -ForegroundColor Red
}

# 6. Check package manager consistency
Write-Host "[6/10] Checking package manager consistency..." -ForegroundColor Yellow
$usesApk = $dockerfileContent -match "apk add"
$usesApt = $dockerfileContent -match "apt-get install"
if ($usesApk -and -not $usesApt) {
    $warnings += "⚠ Using 'apk' (Alpine) - verify apify/actor-node:20 is Alpine-based"
    Write-Host "  ⚠ Using 'apk' package manager (Alpine)" -ForegroundColor Yellow
    Write-Host "     Note: Verify apify/actor-node:20 base image type" -ForegroundColor Yellow
} elseif ($usesApt -and -not $usesApk) {
    $success += "OK: Using 'apt-get' (Debian) - matches Debian-based Apify images"
    Write-Host "  ✓ Using 'apt-get' package manager (Debian)" -ForegroundColor Green
} elseif ($usesApk -and $usesApt) {
    $errors += "✗ Mixed package managers (apk and apt-get) - inconsistent"
    Write-Host "  ✗ Mixed package managers detected" -ForegroundColor Red
} else {
    $warnings += "⚠ No package manager commands found"
    Write-Host "  ⚠ No package manager commands detected" -ForegroundColor Yellow
}

# 7. Check for APIFY_CONTAINER_PORT usage
Write-Host "[7/10] Checking for APIFY_CONTAINER_PORT usage..." -ForegroundColor Yellow
if ($dockerfileContent -match "APIFY_CONTAINER_PORT") {
    $success += "OK: Uses APIFY_CONTAINER_PORT environment variable"
    Write-Host "  ✓ APIFY_CONTAINER_PORT configured" -ForegroundColor Green
} else {
    $errors += "✗ Must use APIFY_CONTAINER_PORT for port configuration"
    Write-Host "  ✗ APIFY_CONTAINER_PORT not found" -ForegroundColor Red
}

# 8. Check for EXPOSE directive
Write-Host "[8/10] Checking for EXPOSE directive..." -ForegroundColor Yellow
if ($dockerfileContent -match "EXPOSE") {
    $success += "OK: Port exposed via EXPOSE directive"
    Write-Host "  ✓ EXPOSE directive found" -ForegroundColor Green
} else {
    $warnings += "⚠ Consider adding EXPOSE directive for documentation"
    Write-Host "  ⚠ EXPOSE directive not found" -ForegroundColor Yellow
}

# 9. Check for health check
Write-Host "[9/10] Checking for health check..." -ForegroundColor Yellow
if ($dockerfileContent -match "HEALTHCHECK") {
    $success += "OK: Health check configured"
    Write-Host "  ✓ Health check found" -ForegroundColor Green
} else {
    $warnings += "⚠ Consider adding HEALTHCHECK for better monitoring"
    Write-Host "  ⚠ Health check not found" -ForegroundColor Yellow
}

# 10. Check for JVM memory settings
Write-Host "[10/10] Checking for JVM memory settings..." -ForegroundColor Yellow
if ($dockerfileContent -match "(-Xmx|-XX:MaxRAMPercentage|-XX:\+UseContainerSupport)") {
    $success += "OK: JVM memory settings configured for containers"
    Write-Host "  ✓ JVM memory settings found" -ForegroundColor Green
} else {
    $warnings += "⚠ Consider adding JVM memory flags (-Xmx, -XX:MaxRAMPercentage)"
    Write-Host "  ⚠ JVM memory settings not found" -ForegroundColor Yellow
}

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Verification Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($success.Count -gt 0) {
    Write-Host "OK: Passed Checks ($($success.Count)):" -ForegroundColor Green
    foreach ($item in $success) {
        Write-Host "  $item" -ForegroundColor Green
    }
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "⚠ Warnings ($($warnings.Count)):" -ForegroundColor Yellow
    foreach ($item in $warnings) {
        Write-Host "  $item" -ForegroundColor Yellow
    }
    Write-Host ""
}

if ($errors.Count -gt 0) {
    Write-Host "✗ Errors ($($errors.Count)):" -ForegroundColor Red
    foreach ($item in $errors) {
        Write-Host "  $item" -ForegroundColor Red
    }
    Write-Host ""
}

# Final verdict
Write-Host "========================================" -ForegroundColor Cyan
if ($errors.Count -eq 0) {
    Write-Host "RESULT: Dockerfile meets Apify requirements!" -ForegroundColor Green
    if ($warnings.Count -gt 0) {
        Write-Host "Note: Some warnings present - review recommended" -ForegroundColor Yellow
    }
    exit 0
} else {
    Write-Host "RESULT: Dockerfile has errors that must be fixed!" -ForegroundColor Red
    exit 1
}


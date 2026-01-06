# PowerShell script to test migration tools on example projects
# Unzips each project and runs migration analysis

$ErrorActionPreference = "Stop"

$examplesDir = Join-Path $PSScriptRoot "..\examples"
$examplesDir = Resolve-Path $examplesDir

Write-Host "Examples directory: $examplesDir" -ForegroundColor Cyan

# Get all ZIP files
$zipFiles = Get-ChildItem -Path $examplesDir -Filter "*.zip"

Write-Host "`nFound $($zipFiles.Count) ZIP files to process" -ForegroundColor Green

foreach ($zipFile in $zipFiles) {
    $projectName = [System.IO.Path]::GetFileNameWithoutExtension($zipFile.Name)
    $projectDir = Join-Path $examplesDir $projectName
    
    Write-Host "`n========================================" -ForegroundColor Yellow
    Write-Host "Processing: $projectName" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Yellow
    
    # Create project directory if it doesn't exist
    if (-not (Test-Path $projectDir)) {
        New-Item -ItemType Directory -Path $projectDir | Out-Null
        Write-Host "Created directory: $projectDir" -ForegroundColor Gray
    }
    
    # Check if already extracted
    $extracted = Get-ChildItem -Path $projectDir -Directory -ErrorAction SilentlyContinue
    if ($extracted.Count -eq 0) {
        Write-Host "Extracting $($zipFile.Name)..." -ForegroundColor Cyan
        try {
            Expand-Archive -Path $zipFile.FullName -DestinationPath $projectDir -Force
            Write-Host "Extracted successfully" -ForegroundColor Green
        } catch {
            Write-Host "Failed to extract: $_" -ForegroundColor Red
            continue
        }
    } else {
        Write-Host "Already extracted, skipping..." -ForegroundColor Gray
    }
    
    # Find the actual project root (might be nested)
    $projectRoot = $projectDir
    $nestedDirs = Get-ChildItem -Path $projectDir -Directory
    if ($nestedDirs.Count -eq 1) {
        $projectRoot = $nestedDirs[0].FullName
        Write-Host "Project root: $projectRoot" -ForegroundColor Gray
    }
    
    # Create log file
    $logFile = Join-Path $projectDir "migration-test-log.md"
    Write-Host "`nLog file: $logFile" -ForegroundColor Cyan
    
    # Initialize log
    $logContent = @"
# Migration Test Log: $projectName

**Test Date**: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
**Project Path**: $projectRoot

## Project Information

**Source ZIP**: $($zipFile.Name)
**Extracted To**: $projectDir

## Test Results

"@
    
    Set-Content -Path $logFile -Value $logContent
    
    Write-Host "`nLog file created" -ForegroundColor Green
}

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "Extraction complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "`nNext: Run Java tests to analyze each project" -ForegroundColor Cyan


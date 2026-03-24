#!/usr/bin/env pwsh

# Fast Test Loop Script for Quick Agent Feedback
# Usage: ./scripts/fast-test.ps1 [test-type]

param(
    [Parameter(Position=0)]
    [ValidateSet("compile", "fast", "core", "pdf", "all")]
    [string]$TestType = "fast"
)

$ErrorActionPreference = "Stop"

Write-Host "🚀 Fast Test Loop - Type: $TestType" -ForegroundColor Green

switch ($TestType) {
    "compile" {
        Write-Host "📝 Running compilation check..." -ForegroundColor Blue
        ./gradlew :premium-core-engine:compileJava --no-daemon --configuration-cache
        Write-Host "✅ Compilation successful!" -ForegroundColor Green
    }
    
    "fast" {
        Write-Host "⚡ Running fast tests only..." -ForegroundColor Blue
        ./gradlew :premium-core-engine:test --tests "*fast*" --parallel --max-worker-count=4 --no-daemon --configuration-cache
        Write-Host "✅ Fast tests completed!" -ForegroundColor Green
    }
    
    "core" {
        Write-Host "🔧 Running core functionality tests..." -ForegroundColor Blue
        ./gradlew :premium-core-engine:test --tests "*PdfReportServiceTest*" --tests "*RecipeServiceImplTest*" --tests "*ListRecipesTest*" --parallel --max-worker-count=4 --no-daemon --configuration-cache
        Write-Host "✅ Core tests completed!" -ForegroundColor Green
    }
    
    "pdf" {
        Write-Host "📄 Running PDF generation tests..." -ForegroundColor Blue
        ./gradlew :premium-core-engine:test --tests "*PdfReportServiceTest*" --parallel --no-daemon --configuration-cache
        Write-Host "✅ PDF tests completed!" -ForegroundColor Green
    }
    
    "all" {
        Write-Host "🔄 Running all tests (slower)..." -ForegroundColor Blue
        ./gradlew :premium-core-engine:test --parallel --max-worker-count=4 --no-daemon --configuration-cache
        Write-Host "✅ All tests completed!" -ForegroundColor Green
    }
}

Write-Host "🎯 Fast test loop completed in $((Get-Date) - $startTime).TotalSeconds seconds" -ForegroundColor Cyan

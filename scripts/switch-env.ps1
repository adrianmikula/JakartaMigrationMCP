# Environment Switch Script for Demo Environment
# Usage: .\switch-env.ps1 Demo [Production]

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("Demo", "Production")]
    $Environment
)

# Display header
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Jakarta Migration - Environment Switcher" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Load environment variables from .env file
$envFile = ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment from .env file..." -ForegroundColor Yellow
    
    # Source: .env file
    foreach ($line in Get-Content $envFile) {
        if ($line -match '^([^=]+)=(.*)$') {
            [Environment]::SetVariable($matches[1], $matches[2])
        }
    }
    
    Write-Host "Environment loaded successfully!" -ForegroundColor Green
} else {
    Write-Host "❌ .env file not found. Please create it from .env.example" -ForegroundColor Red
    Write-Host ""
    exit 1
}

Write-Host ""

if ($Environment -eq "Demo") {
    # Validate demo credentials are set
    $demoUsername = [Environment]::GetVariable("DEMO_USERNAME")
    $demoPassword = [Environment]::GetVariable("DEMO_PASSWORD")
    
    if (-not $demoUsername -or -not $demoPassword)) {
        Write-Host "❌ Demo credentials not found in .env file" -ForegroundColor Red
        Write-Host "Please set DEMO_USERNAME and DEMO_PASSWORD in .env" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Switching IntelliJ to Production environment..." -ForegroundColor Green
    
    # Update idea.properties for production environment
    $configPath = "$env:USERPROFILE\.IntelliJIdea2018.2\config\idea.properties"
    
    if (Test-Path $configPath) {
        # Backup existing config
        $backupPath = "$configPath.backup"
        Copy-Item $configPath $backupPath -Force
        
        # Set production configuration (remove demo settings)
        $ideaContent = Get-Content $configPath
        $ideaContent = $ideaContent -replace '# JetBrains Marketplace Demo Configuration', '# Production Configuration (default)' | Set-Content $configPath
        $ideaContent = $ideaContent -replace 'jb.service.configuration.url=https://active.jetprofile-mpdm.intellij.net/testservices/JetBrainsAccount.xml', '' | Set-Content $configPath
        $ideaContent = $ideaContent -replace '-Didea.plugins.host=https://master.demo.marketplace.intellij.net/', '' | Set-Content $configPath
        
        Write-Host "✅ Production environment configured!" -ForegroundColor Green
        Write-Host "🏭 IDE will use real JetBrains Account" -ForegroundColor Yellow
    } else {
        Write-Host "❌ Invalid environment. Use 'Demo' or 'Production'" -ForegroundColor Red
        exit 1
    }
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "🔄 Restart IntelliJ IDEA to apply changes" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Instructions for IntelliJ restart
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Close IntelliJ IDEA" -ForegroundColor Yellow
Write-Host "2. Run your plugin to test demo environment" -ForegroundColor Yellow
Write-Host "3. Switch back to demo when ready: .\switch-env.ps1 Demo" -ForegroundColor Yellow
Write-Host ""

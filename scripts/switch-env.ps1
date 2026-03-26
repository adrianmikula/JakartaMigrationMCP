# Environment Switch Script for JetBrains Marketplace
# Usage: .\scripts\switch-env.ps1 Demo [Production]

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("Demo", "Production")]
    $Environment
)

# Display header
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " JetBrains Marketplace - Environment Switcher" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Load environment variables from .env file
$envFile = ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment from .env file..." -ForegroundColor Yellow
    
    # Source: .env file
    $envVars = @{}
    foreach ($line in Get-Content $envFile) {
        if ($line -match '^([^=]+)=(.*)$') {
            $envVars[$matches[1]] = $matches[2]
        }
    }
    
    Write-Host "Environment loaded successfully!" -ForegroundColor Green
} else {
    Write-Host " .env file not found. Please create it from .env.example" -ForegroundColor Red
    Write-Host ""
    exit 1
}

Write-Host ""

if ($Environment -eq "Demo") {
    # Validate demo credentials are set
    $demoUsername = $envVars["DEMO_USERNAME"]
    $demoPassword = $envVars["DEMO_PASSWORD"]
    
    if (-not $envVars["DEMO_USERNAME"] -or -not $envVars["DEMO_PASSWORD"]) {
        Write-Host " Demo credentials not found in .env file" -ForegroundColor Red
        Write-Host "Please set DEMO_USERNAME and DEMO_PASSWORD in .env" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Switching IntelliJ to Demo environment..." -ForegroundColor Green
    
    # Update idea.properties for demo environment
    $configPath = "$env:USERPROFILE\.IntelliJIdea2018.2\config\idea.properties"
    
    if (Test-Path $configPath) {
        # Backup existing config
        $backupPath = "$configPath.backup"
        Copy-Item $configPath $backupPath -Force
        
        # Set demo configuration
        $demoServiceUrl = $envVars["DEMO_SERVER_URL"]
        
        $ideaContent = Get-Content $configPath
        $ideaContent = $ideaContent -replace '# Production Configuration \(default\)', '# JetBrains Marketplace Demo Configuration'
        $newConfigLine = "jb.service.configuration.url=$demoServiceUrl"
        $ideaContent = $ideaContent + "`n$newConfigLine`n"
        Set-Content $configPath $ideaContent
        
        # Update VM options
        $vmOptionsPath = "$env:USERPROFILE\.IntelliJIdea2018.2\idea64.vmoptions"
        if (Test-Path $vmOptionsPath) {
            $vmContent = Get-Content $vmOptionsPath
            if ($vmContent -notmatch '-Didea.plugins.host=https://master.demo.marketplace.intellij.net/') {
                $vmContent = $vmContent + "-Didea.plugins.host=https://master.demo.marketplace.intellij.net/"
                Set-Content $vmOptionsPath $vmContent
            }
        }
        
        Write-Host "✅ Demo environment configured!" -ForegroundColor Green
        Write-Host "🔗 IDE will connect to JetBrains Marketplace Demo" -ForegroundColor Yellow
        Write-Host "👤 Demo Username: $demoUsername" -ForegroundColor Cyan
    } else {
        Write-Host "⚠️ IntelliJ configuration file not found" -ForegroundColor Yellow
        Write-Host "📁 Expected path: $configPath" -ForegroundColor Yellow
        Write-Host "💡 Please run IntelliJ IDEA at least once to create the configuration" -ForegroundColor Yellow
        Write-Host "🔧 Or manually create the configuration directory and file" -ForegroundColor Yellow
    }
} elseif ($Environment -eq "Production") {
    Write-Host "Switching IntelliJ to Production environment..." -ForegroundColor Green
    
    # Update idea.properties for production environment
    $configPath = "$env:USERPROFILE\.IntelliJIdea2018.2\config\idea.properties"
    
    if (Test-Path $configPath) {
        # Backup existing config
        $backupPath = "$configPath.backup"
        Copy-Item $configPath $backupPath -Force
        
        # Set production configuration (remove demo settings)
        $ideaContent = Get-Content $configPath
        $ideaContent = $ideaContent -replace '# JetBrains Marketplace Demo Configuration', '# Production Configuration (default)'
        $ideaContent = $ideaContent -replace 'jb.service.configuration.url=https://active.jetprofile-mpdm.intellij.net/testservices/JetBrainsAccount.xml', ''
        $ideaContent = $ideaContent -replace '-Didea.plugins.host=https://master.demo.marketplace.intellij.net/', ''
        
        Write-Host " Production environment configured!" -ForegroundColor Green
        Write-Host " IDE will use real JetBrains Account" -ForegroundColor Yellow
    } else {
        Write-Host " Failed to read configuration file" -ForegroundColor Red
        exit 1
    }
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Restart IntelliJ IDEA to apply changes" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Instructions for IntelliJ restart
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Close IntelliJ IDEA" -ForegroundColor Yellow
Write-Host "2. Run your plugin to test demo environment" -ForegroundColor Yellow
Write-Host "3. Switch back to demo when ready: .\scripts\switch-env.ps1 Demo" -ForegroundColor Yellow
Write-Host ""

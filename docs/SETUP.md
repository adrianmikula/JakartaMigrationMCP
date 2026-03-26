# Development Environment Setup

This guide helps you switch between JetBrains Marketplace Demo environment and production environment for plugin testing.

## Environment Configuration

### `.env` File Setup
Create a `.env` file in your project root directory based on `.env.example`:

```bash
# Copy the example file
cp .env.example .env

# Edit with your actual demo credentials
# Windows: notepad .env
# macOS/Linux: nano .env
```

### Required Environment Variables
```properties
# JetBrains Marketplace Demo Environment Credentials
DEMO_USERNAME=your_actual_demo_username
DEMO_PASSWORD=your_actual_demo_password

# Demo Server Configuration  
DEMO_SERVER_URL=https://master.demo.marketplace.intellij.net/
DEMO_PLUGIN_HOST=https://master.demo.marketplace.intellij.net/

# Environment Settings
ENVIRONMENT=demo
LOG_LEVEL=INFO
```

## Environment Switching

### Option 1: Demo Environment (JetBrains Marketplace Demo)
For testing your plugin on JetBrains Marketplace Demo server:

#### 1.1 Configure `idea.properties`
Add to your `idea.properties` file:
```properties
# JetBrains Marketplace Demo Configuration
jb.service.configuration.url=https://active.jetprofile-mpdm.intellij.net/testservices/JetBrainsAccount.xml
```

#### 1.2 Configure VM Options
Add to your IntelliJ IDEA VM options:
- **Windows**: Edit `IntelliJ IDEA 2018.2.app.vmoptions`
- **macOS**: Edit `idea.vmoptions` 
- **Linux**: Edit `idea64.vmoptions`

Add this line:
```
-Didea.plugins.host=https://master.demo.marketplace.intellij.net/
```

#### 1.3 Demo Limitations
- Limited plugin functionality
- May have performance differences
- Not suitable for production development

### Option 2: Production Environment (Real JetBrains Account)
For normal development with real JetBrains Account:

#### 2.1 Configure `idea.properties`
Remove or comment out the demo configuration:
```properties
# Production Configuration (default)
# jb.service.configuration.url=https://active.jetprofile-mpdm.intellij.net/testservices/JetBrainsAccount.xml
```

#### 2.2 Configure VM Options
Remove the demo VM option:
```
# -Didea.plugins.host=https://master.demo.marketplace.intellij.net/
```

## Quick Switch Script

### PowerShell (Windows)
Create `switch-env.ps1`:
```powershell
param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("Demo", "Production")]
    $Environment
)

# Load environment variables from .env file
$envFile = ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment from .env file..." -ForegroundColor Yellow
    
    # Source the .env file
    foreach ($line in Get-Content $envFile) {
        if ($line -match '^([^=]+)=(.*)$') {
            [Environment]::SetVariable($matches[1], $matches[2])
        }
    }
    
    Write-Host "Environment loaded successfully!" -ForegroundColor Green
} else {
    Write-Host " .env file not found. Please create it from .env.example" -ForegroundColor Red
    exit 1
}

Write-Host "Switching IntelliJ to $Environment environment..." -ForegroundColor Green

$configPath = "$env:USERPROFILE\.IntelliJIdea2018.2\config\idea.properties"

if ($Environment -eq "Demo") {
    # Set up demo environment
    $demoUsername = [Environment]::GetVariable("DEMO_USERNAME")
    $demoPassword = [Environment]::GetVariable("DEMO_PASSWORD")
    $demoServerUrl = [Environment]::GetVariable("DEMO_SERVER_URL")
    $demoPluginHost = [Environment]::GetVariable("DEMO_PLUGIN_HOST")
    
    if (-not $demoUsername -or -not $demoPassword) {
        Write-Host " Demo credentials not found in .env file" -ForegroundColor Red
        Write-Host "Please set DEMO_USERNAME and DEMO_PASSWORD in .env" -ForegroundColor Red
        exit 1
    }
    
    # Update idea.properties
    (Get-Content $configPath) -replace '# Production Configuration \(default\)', '# JetBrains Marketplace Demo Configuration' | Set-Content $configPath
    Add-Content -Path $configPath -Value "`njb.service.configuration.url=https://active.jetprofile-mpdm.intellij.net/testservices/JetBrainsAccount.xml`n"
    
    # Update VM options
    $vmOptionsPath = "$env:USERPROFILE\.IntelliJIdea2018.2\idea64.vmoptions"
    $vmContent = Get-Content $vmOptionsPath
    if ($vmContent -notmatch '-Didea.plugins.host=https://master.demo.marketplace.intellij.net/') {
        $vmContent += "`n-Didea.plugins.host=https://master.demo.marketplace.intellij.net/`n"
        Set-Content $vmOptionsPath $vmContent
    }
    
    Write-Host " Demo environment configured!" -ForegroundColor Green
    Write-Host " IDE will connect to JetBrains Marketplace Demo" -ForegroundColor Yellow
    Write-Host " Demo Username: $demoUsername" -ForegroundColor Cyan
} elseif ($Environment -eq "Production") {
    # Set up production environment
    (Get-Content $configPath) -replace '# JetBrains Marketplace Demo Configuration', '# Production Configuration (default)' | Set-Content $configPath
    (Get-Content $configPath) -replace 'jb.service.configuration.url=https:\/\/active\.jetprofile-mpdm\.intellij\.net\/testservices\/JetBrainsAccount\.xml', '' | Set-Content $configPath
    
    # Update VM options
    $vmOptionsPath = "$env:USERPROFILE\.IntelliJIdea2018.2\idea64.vmoptions"
    $vmContent = Get-Content $vmOptionsPath
    $vmContent = $vmContent -replace '-Didea\.plugins\.host=https:\/\/master\.demo\.marketplace\.intellij\.net\//d', '' | Set-Content $vmOptionsPath
    
    Write-Host " Production environment configured!" -ForegroundColor Green
    Write-Host " IDE will use real JetBrains Account" -ForegroundColor Yellow
} else {
    Write-Host " Invalid environment. Use 'Demo' or 'Production'" -ForegroundColor Red
    exit 1
}

Write-Host " Restart IntelliJ IDEA to apply changes" -ForegroundColor Cyan
```

### Bash (macOS/Linux)
Create `switch-env.sh`:
```bash
#!/bin/bash

# Load environment variables from .env file
if [ -f ".env" ]; then
    echo "Loading environment from .env file..."
    
    # Source the .env file
    set -a
    while IFS='=' read -r line; do
        if [[ "$line" =~ ^([^=]+)=(.*)$ ]]; then
            export "${BASH_REMATCH[1]}=${BASH_REMATCH[2]}"
        fi
    done < .env
    
    echo "Environment loaded successfully!"
else
    echo " .env file not found. Please create it from .env.example" >&2
    exit 1
fi

ENVIRONMENT=$1

if [ "$ENVIRONMENT" = "Demo" ]; then
    # Set up demo environment
    if [ -z "$DEMO_USERNAME" ] || [ -z "$DEMO_PASSWORD" ]; then
        echo " Demo credentials not found in .env file" >&2
        echo "Please set DEMO_USERNAME and DEMO_PASSWORD in .env" >&2
        exit 1
    fi
    
    # Update idea.properties
    sed -i.bak 's/# Production Configuration (default)/# JetBrains Marketplace Demo Configuration/' "$HOME/.config/JetBrains/IdeaIC2018.2/idea.properties"
    echo "jb.service.configuration.url=https://active.jetprofile-mpdm.intellij.net/testservices/JetBrainsAccount.xml" >> "$HOME/.config/JetBrains/IdeaIC2018.2/idea.properties"
    
    # Update VM options
    VM_OPTIONS_FILE="$HOME/.config/JetBrains/IdeaIC2018.2/idea64.vmoptions"
    if ! grep -q "idea.plugins.host=https://master.demo.marketplace.intellij.net/" "$VM_OPTIONS_FILE"; then
        echo "-Didea.plugins.host=https://master.demo.marketplace.intellij.net/" >> "$VM_OPTIONS_FILE"
    fi
    
    echo " Demo environment configured!"
    echo " IDE will connect to JetBrains Marketplace Demo"
    echo " Demo Username: $DEMO_USERNAME"
elif [ "$ENVIRONMENT" = "Production" ]; then
    # Set up production environment
    sed -i.bak 's/# JetBrains Marketplace Demo Configuration/# Production Configuration (default)/' "$HOME/.config/JetBrains/IdeaIC2018.2/idea.properties"
    sed -i '/jb.service.configuration.url=https:\/\/active\.jetprofile-mpdm\.intellij\.net\/testservices\/JetBrainsAccount\.xml/d' "$HOME/.config/JetBrains/IdeaIC2018.2/idea.properties"
    
    # Update VM options
    VM_OPTIONS_FILE="$HOME/.config/JetBrains/IdeaIC2018.2/idea64.vmoptions"
    sed -i.bak '/-Didea\.plugins\.host=https:\/\/master\.demo\.marketplace\.intellij\.net\//d' "$VM_OPTIONS_FILE"
    
    echo " Production environment configured!"
    echo " IDE will use real JetBrains Account"
else
    echo " Invalid environment. Use 'Demo' or 'Production'" >&2
    exit 1
fi

echo " Restart IntelliJ IDEA to apply changes"
```

## Usage Instructions

### Initial Setup
```bash
# Step 1: Copy the example environment file
cp .env.example .env

# Step 2: Edit with your actual demo credentials
nano .env
```

### Environment Switching

#### Windows
```powershell
.\switch-env.ps1 Demo
.\switch-env.ps1 Production
```

#### macOS/Linux
```bash
chmod +x switch-env.sh
./switch-env.sh Demo
./switch-env.sh Production
```

## Environment Verification

After switching, verify your environment:

### Check IntelliJ Registry
1. **Help → About** → Look for "Plugin Host" URL
2. **Help → Edit Custom Properties** → Verify `idea.properties` settings
3. **VM Options** → Check that demo/prod URL is correctly set

### Test Plugin Loading
1. Start IntelliJ IDEA
2. Check if plugin loads correctly
3. Verify connectivity to expected environment

## Important Notes

 **Always restart IntelliJ IDEA** after switching environments
 **Remove demo configuration** before production deployments
 **Test thoroughly** in demo environment before switching to production
 **Security**: Never commit `.env` files with real credentials to version control

## Troubleshooting

### Plugin Not Loading
- Check `.env` file exists and is readable
- Verify required variables are set (`DEMO_USERNAME`, `DEMO_PASSWORD`)
- Check `idea.properties` file location and permissions
- Ensure IntelliJ is fully restarted

### Connection Issues
- Verify network connectivity to demo server
- Check firewall settings
- Try alternative IDE restart method

### VM Options Not Applied
- Verify VM options file path is correct
- Check file permissions
- Use IntelliJ's "Edit Custom VM Options" dialog as alternative

### Credential Issues
- Verify demo account credentials are correct
- Check for typos in `.env` file
- Ensure proper line endings in configuration files

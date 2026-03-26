# Development Environment Setup

This guide helps you switch between JetBrains Marketplace Demo environment and production environment for plugin testing.

## Environment Setup

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

Write-Host "Switching IntelliJ to $Environment environment..." -ForegroundColor Green

$configPath = "$env:USERPROFILE\.IntelliJIdea2018.2\config\idea.properties"

if ($Environment -eq "Demo") {
    # Set up demo environment
    (Get-Content $configPath) -replace '# Production Configuration \(default\)', '# JetBrains Marketplace Demo Configuration' | Set-Content $configPath
    Add-Content -Path $configPath -Value "`njb.service.configuration.url=https://active.jetprofile-mpdm.intellij.net/testservices/JetBrainsAccount.xml`n"
    
    # Update VM options
    $vmOptionsPath = "$env:USERPROFILE\.IntelliJIdea2018.2\idea64.vmoptions"
    $vmContent = Get-Content $vmOptionsPath
    if ($vmContent -notmatch '-Didea.plugins.host=https://master.demo.marketplace.intellij.net/') {
        $vmContent += "`n-Didea.plugins.host=https://master.demo.marketplace.intellij.net/`n"
        Set-Content $vmOptionsPath $vmContent
    }
    
    Write-Host "✅ Demo environment configured!" -ForegroundColor Green
    Write-Host "🔗 IDE will connect to JetBrains Marketplace Demo" -ForegroundColor Yellow
} elseif ($Environment -eq "Production") {
    # Set up production environment
    (Get-Content $configPath) -replace '# JetBrains Marketplace Demo Configuration', '# Production Configuration (default)' | Set-Content $configPath
    (Get-Content $configPath) -replace 'jb.service.configuration.url=https://active.jetprofile-mpdm.intellij.net/testservices/JetBrainsAccount.xml', '' | Set-Content $configPath
    
    # Update VM options
    $vmOptionsPath = "$env:USERPROFILE\.IntelliJIdea2018.2\idea64.vmoptions"
    $vmContent = Get-Content $vmOptionsPath
    $vmContent = $vmContent -replace '-Didea.plugins.host=https://master.demo.marketplace.intellij.net/', '' | Set-Content $vmOptionsPath
    
    Write-Host "✅ Production environment configured!" -ForegroundColor Green
    Write-Host "🏭 IDE will use real JetBrains Account" -ForegroundColor Yellow
} else {
    Write-Host "❌ Invalid environment. Use 'Demo' or 'Production'" -ForegroundColor Red
    exit 1
}

Write-Host "🔄 Restart IntelliJ IDEA to apply changes" -ForegroundColor Cyan
```

### Bash (macOS/Linux)
Create `switch-env.sh`:
```bash
#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 [Demo|Production]"
    exit 1
fi

ENVIRONMENT=$1

IDEA_CONFIG="$HOME/.config/JetBrains/IdeaIC2018.2/idea.properties"
VM_OPTIONS="$HOME/.config/JetBrains/IdeaIC2018.2/idea64.vmoptions"

echo "Switching IntelliJ to $ENVIRONMENT environment..."

if [ "$ENVIRONMENT" = "Demo" ]; then
    # Set up demo environment
    sed -i.bak 's/# Production Configuration (default)/# JetBrains Marketplace Demo Configuration/' "$IDEA_CONFIG"
    echo "jb.service.configuration.url=https://active.jetprofile-mpdm.intellij.net/testservices/JetBrainsAccount.xml" >> "$IDEA_CONFIG"
    
    # Update VM options
    if ! grep -q "idea.plugins.host=https://master.demo.marketplace.intellij.net/" "$VM_OPTIONS"; then
        echo "-Didea.plugins.host=https://master.demo.marketplace.intellij.net/" >> "$VM_OPTIONS"
    fi
    
    echo "✅ Demo environment configured!"
    echo "🔗 IDE will connect to JetBrains Marketplace Demo"
elif [ "$ENVIRONMENT" = "Production" ]; then
    # Set up production environment
    sed -i.bak 's/# JetBrains Marketplace Demo Configuration/# Production Configuration (default)/' "$IDEA_CONFIG"
    sed -i '/jb.service.configuration.url=https:\/\/active\.jetprofile-mpdm\.intellij\.net\/testservices\/JetBrainsAccount\.xml/d' "$IDEA_CONFIG"
    
    # Update VM options
    sed -i.bak '/-Didea\.plugins\.host=https:\/\/master\.demo\.marketplace\.intellij\.net\//d' "$VM_OPTIONS"
    
    echo "✅ Production environment configured!"
    echo "🏭 IDE will use real JetBrains Account"
else
    echo "❌ Invalid environment. Use 'Demo' or 'Production'"
    exit 1
fi

echo "🔄 Restart IntelliJ IDEA to apply changes"
```

## Usage Instructions

### Windows
```powershell
.\switch-env.ps1 Demo
.\switch-env.ps1 Production
```

### macOS/Linux
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

⚠️ **Always restart IntelliJ IDEA** after switching environments
🔄 **Remove demo configuration** before production deployments
📝 **Test thoroughly** in demo environment before switching to production

## Troubleshooting

### Plugin Not Loading
- Check `idea.properties` file location
- Verify URL syntax is correct
- Ensure IntelliJ is fully restarted

### Connection Issues
- Verify network connectivity to demo server
- Check firewall settings
- Try alternative IDE restart method

### VM Options Not Applied
- Verify VM options file path is correct
- Check file permissions
- Use IntelliJ's "Edit Custom VM Options" dialog as alternative

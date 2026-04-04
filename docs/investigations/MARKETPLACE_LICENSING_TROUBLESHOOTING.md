# JetBrains Marketplace Licensing Issue - Troubleshooting Guide

## 🚨 **Problem Summary**

**Error**: `Unknown product code provided PJAKARTAMIGRATI`

**Root Cause**: Plugin configuration mismatch between local code and JetBrains Marketplace setup.

---

## 🔍 **Diagnostic Steps**

### Step 1: Verify Local Configuration
```bash
# Check your plugin.xml
grep "productCode" premium-intellij-plugin/src/main/resources/META-INF/plugin.xml

# Should return:
<product-descriptor code="PJAKARTAMIGRATI" release-date="20250326" release-version="108"/>
```

### Step 2: Check Marketplace Configuration
Go to: https://plugins.jetbrains.com/developer/

Verify:
1. **Plugin ID**: `com.adrianmikula.jakarta-migration`
2. **Product Code**: `PJAKARTAMIGRATI` (must match exactly)
3. **Paid Plugin**: ✅ **ENABLED**
4. **Status**: **Published** (not draft)

### Step 3: Check Environment Alignment
```bash
# Are you using the right marketplace?
./gradlew :premium-intellij-plugin:runIdeDemo    # Demo marketplace
./gradlew :premium-intellij-plugin:runIdeProd     # Production marketplace
```

---

## 🛠️ **Immediate Solutions**

### Solution A: Fix Marketplace Listing (Recommended)

1. **Go to JetBrains Developer Dashboard**
2. **Navigate to your plugin**: "Jakarta Migration"
3. **Edit Plugin Details**:
   - Set **Product Code**: `PJAKARTAMIGRATI`
   - Enable **Paid Plugin**: ✅
   - Set **Price** (can be $0 for testing)
4. **Save Changes**
5. **Upload New Version** with same version number

### Solution B: Use Demo Marketplace (Testing)

1. **Build for demo**:
   ```bash
   ./gradlew :premium-intellij-plugin:buildDemoPlugin
   ```

2. **Run with demo**:
   ```bash
   ./gradlew :premium-intellij-plugin:runIdeDemo
   ```

3. **Install from Demo Marketplace**:
   - Configure IDE to use demo marketplace
   - Install plugin directly from there

### Solution C: Development Bypass (Immediate)

Use the built-in bypass I added:

```bash
# Option 1: Dev mode
./gradlew :premium-intellij-plugin:runIdeDev

# Option 2: Marketplace test override
./gradlew :premium-intellij-plugin:runIdeDemo

# Option 3: Manual override
./gradlew :premium-intellij-plugin:runIde -Djakarta.migration.marketplace.test=true
```

---

## 🔧 **Technical Details**

### What Happens Behind the Scenes

1. **Plugin Startup**:
   ```java
   LicensingFacade.getInstance().getConfirmationStamp("PJAKARTAMIGRATI")
   ```

2. **Marketplace Validation**:
   - JetBrains validates product code against their database
   - If not found → "Unknown product code" error
   - Plugin fails to initialize

3. **Fallback Behavior**:
   - `CheckLicense.isLicensed()` returns `null`
   - `SafeLicenseChecker` uses fallback behavior
   - Plugin loads but shows "Free" status

### Configuration Matrix

| Environment | Marketplace | Product Code | Result |
|-------------|-------------|-------------|---------|
| Production  | Production   | ✅ Works |
| Production  | Demo         | ❌ Error |
| Demo        | Production   | ❌ Error |
| Demo        | Demo         | ✅ Works |

---

## 🚀 **Testing Strategy**

### Phase 1: Local Testing
```bash
# Test with bypass (should always work)
./gradlew :premium-intellij-plugin:runIdeDev

# Verify premium features are enabled
# Check SupportComponent shows "Development Mode"
```

### Phase 2: Demo Marketplace Testing
```bash
# Build and test demo configuration
./gradlew :premium-intellij-plugin:buildDemoPlugin
./gradlew :premium-intellij-plugin:runIdeDemo

# Verify trial activation works
# Check licensing doesn't block IDE
```

### Phase 3: Production Marketplace Testing
```bash
# Only after marketplace is fixed
./gradlew :premium-intellij-plugin:buildProductionPlugin
./gradlew :premium-intellij-plugin:runIdeProd

# Verify actual licensing flow
```

---

## 📋 **Verification Checklist**

### Before Upload
- [ ] Product code in `plugin.xml` matches marketplace exactly
- [ ] Marketplace listing has "Paid Plugin" enabled
- [ ] Product code field is filled in marketplace
- [ ] Plugin version matches local version
- [ ] No draft status (must be published)

### After Upload
- [ ] Plugin appears in marketplace search
- [ ] Trial activation works correctly
- [ ] License validation succeeds
- [ ] No "Unknown product code" errors

### Bypass Testing
- [ ] Dev mode bypass works: `-Djakarta.migration.mode=dev`
- [ ] Marketplace test works: `-Djakarta.migration.marketplace.test=true`
- [ ] Demo environment works: `environment=demo`
- [ ] Safe mode works: `-Djakarta.migration.safe=true`

---

## 🆘 **Emergency Recovery**

If IDE gets stuck due to licensing:

### Option 1: Manual Plugin Removal
1. **Close IntelliJ completely**
2. **Delete plugin directory**:
   - Windows: `%APPDATA%\JetBrains\IntelliJIdea**\plugins\jakarta-migration*`
   - macOS: `~/Library/Application Support/JetBrains/IntelliJIdea*/*/plugins/jakarta-migration*`
   - Linux: `~/.local/share/JetBrains/IntelliJIdea*/*/plugins/jakarta-migration*`

### Option 2: Safe Mode Launch
```bash
# Launch with failsafe mode
idea64.exe -Djakarta.migration.safe=true
```

### Option 3: Configuration Reset
```bash
# Clear IntelliJ license cache
rm -rf ~/.config/JetBrains/*/options/other.xml
```

---

## 📞 **Support Channels**

### JetBrains Support
- **Marketplace Issues**: https://jb.gg/intellij-support
- **Plugin Development**: https://jb.gg/intellij-platform-dev

### Common Solutions
- **Product Code Issues**: Usually fixed by updating marketplace listing
- **Version Mismatches**: Upload exact same version
- **Environment Confusion**: Use demo marketplace for testing

---

## 🎯 **Prevention Checklist**

### For Future Releases
1. **Always test marketplace configuration before upload**
2. **Verify product code in both places matches**
3. **Test in demo marketplace first**
4. **Document marketplace environment requirements**
5. **Include bypass options for development**

### Development Best Practices
1. **Use dev mode for local development**
2. **Test marketplace flows before release**
3. **Keep version numbers synchronized**
4. **Test emergency recovery procedures**
5. **Document configuration requirements**

---

## 🔮 **Quick Fix Commands**

### Immediate Testing (Bypass)
```bash
# Dev mode (always works)
./gradlew :premium-intellij-plugin:runIdeDev

# Demo marketplace (if you have demo access)
./gradlew :premium-intellij-plugin:runIdeDemo

# Manual override (any environment)
./gradlew :premium-intellij-plugin:runIde -Djakarta.migration.marketplace.test=true
```

### Marketplace Fix
```bash
# Build for correct marketplace
./gradlew :premium-intellij-plugin:buildProductionPlugin

# Upload to production marketplace
# Then test with production marketplace
./gradlew :premium-intellij-plugin:runIdeProd
```

---

**This is a configuration issue, not a code bug.** The licensing system works correctly - it just needs the marketplace to be properly configured with the matching product code.

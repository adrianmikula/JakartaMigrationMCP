# JetBrains Marketplace Integration Plan

## Overview
Fix and complete the JetBrains Marketplace license verification and freemium implementation for the Jakarta Migration plugin.

**Plugin ID**: `30093`  
**Plugin URL**: https://plugins.jetbrains.com/plugin/30093-jakarta-migration

---

## Task List

### Phase 1: Fix Plugin ID References

#### 1.1 Update LicenseService.java
- **File**: `community-core-engine/src/main/java/adrianmikula/jakartamigration/config/LicenseService.java`
- **Line 21**: Change `PLUGIN_ID` from `"jakarta-migration"` to `"30093"`
- **Note**: The LicensingFacade uses the numeric plugin ID from the marketplace URL

```java
// Current (wrong)
private static final String PLUGIN_ID = "jakarta-migration";

// Fix to
private static final String PLUGIN_ID = "30093";
```

#### 1.2 Update MarketplaceLicenseService.java
- **File**: `community-core-engine/src/main/java/adrianmikula/jakartamigration/config/MarketplaceLicenseService.java`
- **Line 46**: Already has numeric ID `25558` - change to `"30093"`

```java
// Current
private static final String PLUGIN_ID = "25558"; // Jakarta Migration MCP plugin ID

// Fix to
private static final String PLUGIN_ID = "30093"; // From plugins.jetbrains.com/plugin/30093
```

#### 1.3 Update getMarketplaceUrl()
- **File**: `community-core-engine/src/main/java/adrianmikula/jakartamigration/config/FeatureFlagsProperties.java`
- **Line 131**: Fix the marketplace URL

```java
// Current (incomplete)
return "https://plugins.jetbrains.com/plugin/";

// Fix to
return "https://plugins.jetbrains.com/plugin/30093-jakarta-migration";
```

---

### Phase 2: Implement Real Marketplace API

#### 2.1 Update MarketplaceLicenseService.java
- **File**: `community-core-engine/src/main/java/adrianmikula/jakartamigration/config/MarketplaceLicenseService.java`
- **Lines 82-128**: Replace simulated validation with real API call

**JetBrains API Endpoint**: `https://plugins.jetbrains.com/api/license/validate`

```java
private LicenseValidationResult validateLicenseKey(String licenseKey) {
    try {
        // Real JetBrains Marketplace API call
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(MARKETPLACE_API_URL + "validate"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"pluginId\": \"" + PLUGIN_ID + "\", \"licenseKey\": \"" + licenseKey + "\"}"
            ))
            .timeout(TIMEOUT)
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            // Parse JSON response
            return parseMarketplaceResponse(response.body());
        } else if (response.statusCode() == 404) {
            return LicenseValidationResult.invalid("License not found");
        } else {
            return LicenseValidationResult.error("API error: " + response.statusCode());
        }
    } catch (Exception e) {
        log.warn("Failed to call Marketplace API, falling back to local validation: {}", e.getMessage());
        return validateLicenseKeyLocal(licenseKey);
    }
}

private LicenseValidationResult parseMarketplaceResponse(String json) {
    // Parse JSON using Jackson or similar
    // Return appropriate LicenseValidationResult based on response
}

private LicenseValidationResult validateLicenseKeyLocal(String licenseKey) {
    // Keep existing local validation for offline/dev mode
}
```

---

### Phase 3: Add Freemium Configuration

#### 3.1 Update plugin.xml
- **File**: `premium-intellij-plugin/src/main/resources/META-INF/plugin.xml`
- Already has vendor info, but could add `<category>` element

```xml
<vendor email="adrian.m@tuta.io" name="Adrian Mikula" url="https://github.com/adrianmikula/JakartaMigrationMCP">Adrian Mikula</vendor>

<!-- Add category for better discoverability -->
<category>Framework Integration</category>
```

---

### Phase 4: Add Trial Activation to IntelliJ Plugin

#### 4.1 Create Trial Activation UI
- Add a "Start Free Trial" button in the MigrationToolWindow
- Link to: `https://plugins.jetbrains.com/plugin/30093-jakarta-migration?subscription=true`

#### 4.2 Store Trial State
- Use IntelliJ's `PropertiesComponent` to persist trial end timestamp

---

### Phase 5: Add Tests

#### 5.1 Update Existing Tests
- **File**: `community-core-engine/src/test/java/adrianmikula/jakartamigration/config/LicenseServiceTest.java`
- Verify plugin ID `"30093"` is used

#### 5.2 Add MarketplaceLicenseService Tests
- Mock HTTP client responses
- Test valid, expired, and invalid license scenarios

---

## Implementation Order

1. Update `LicenseService.java` (PLUGIN_ID)
2. Update `MarketplaceLicenseService.java` (PLUGIN_ID + API)
3. Update `FeatureFlagsProperties.java` (marketplace URL)
4. Update `plugin.xml` (category)
5. Add trial activation UI (if time permits)
6. Run existing tests to verify no regressions
7. Add new tests for API integration

---

## References

- [JetBrains License Validation](https://plugins.jetbrains.com/docs/marketplace/add-marketplace-license-verification-calls-to-the-plugin-code.html)
- [Freemium Plugins](https://plugins.jetbrains.com/docs/marketplace/freemium.html)
- [Plugin Publishing](https://plugins.jetbrains.com/docs/marketplace/plugin-overview.html)

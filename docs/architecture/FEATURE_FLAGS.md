# Feature Flags System

The Jakarta Migration MCP Server uses a feature flags system to control access to premium features based on license tiers. This enables the monetization model described in the [monetization research](../research/monetisation.md).

## Overview

The feature flags system provides:
- **Tier-based access control** - Features are gated by license tier (COMMUNITY, PREMIUM, ENTERPRISE)
- **Flexible configuration** - Per-feature overrides for testing and gradual rollouts
- **License validation** - Integration with license key validation (placeholder for future implementation)
- **Developer-friendly** - Easy to check feature availability in code

## Architecture

### Components

1. **FeatureFlag** - Enumeration of all available features
2. **FeatureFlagsProperties** - Configuration properties class
3. **FeatureFlagsService** - Service for checking feature availability
4. **LicenseService** - License key validation (placeholder)

### Feature Tiers

| Feature | COMMUNITY | PREMIUM | ENTERPRISE |
|---------|-----------|---------|------------|
| Basic scanning | ✅ | ✅ | ✅ |
| Dependency analysis | ✅ | ✅ | ✅ |
| Migration planning | ✅ | ✅ | ✅ |
| Auto-fixes | ❌ | ✅ | ✅ |
| One-click refactor | ❌ | ✅ | ✅ |
| Binary fixes | ❌ | ✅ | ✅ |
| Advanced analysis | ❌ | ✅ | ✅ |
| Batch operations | ❌ | ✅ | ✅ |
| Custom recipes | ❌ | ✅ | ✅ |
| API access | ❌ | ✅ | ✅ |
| Export reports | ❌ | ✅ | ✅ |
| Priority support | ❌ | ❌ | ✅ |
| Cloud hosting | ❌ | ❌ | ✅ |

## Configuration

### application.yml

```yaml
jakarta:
  migration:
    feature-flags:
      enabled: true
      default-tier: COMMUNITY
      license-key: ${JAKARTA_MCP_LICENSE_KEY:}
      features:
        # Per-feature overrides (optional)
        auto-fixes: false
        cloud-hosting: false
```

### Environment Variables

- `JAKARTA_MCP_LICENSE_KEY` - License key for premium features
- `JAKARTA_MCP_PURCHASE_URL` - URL for upgrade/purchase page

## Usage

### Basic Feature Check

```java
@Service
@RequiredArgsConstructor
public class MyService {
    
    private final FeatureFlagsService featureFlags;
    
    public void performAction() {
        if (featureFlags.isEnabled(FeatureFlag.AUTO_FIXES)) {
            // Execute auto-fix logic
            autoFix();
        } else {
            // Return upgrade message
            throw new UpgradeRequiredException(
                featureFlags.getUpgradeMessage(FeatureFlag.AUTO_FIXES)
            );
        }
    }
}
```

### Require Feature (Throws Exception)

```java
public void performPremiumAction() {
    // Throws FeatureNotAvailableException if not enabled
    featureFlags.requireEnabled(FeatureFlag.ONE_CLICK_REFACTOR);
    
    // Proceed with premium feature
    executeOneClickRefactor();
}
```

### Check License Tier

```java
if (featureFlags.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)) {
    // User has premium or enterprise tier
    enablePremiumFeatures();
}
```

### Get All Enabled Features

```java
Set<FeatureFlag> enabledFeatures = featureFlags.getEnabledFeatures();
log.info("Enabled features: {}", enabledFeatures);
```

## Available Features

### AUTO_FIXES
- **Tier:** PREMIUM
- **Description:** Automatically fix detected Jakarta migration issues
- **Community Alternative:** Can only identify problems

### ONE_CLICK_REFACTOR
- **Tier:** PREMIUM
- **Description:** Execute complete Jakarta migration refactoring with a single command
- **Community Alternative:** Can only create migration plans

### BINARY_FIXES
- **Tier:** PREMIUM
- **Description:** Fix Jakarta migration issues in compiled binaries and JAR files
- **Community Alternative:** Can only analyze source code

### ADVANCED_ANALYSIS
- **Tier:** PREMIUM
- **Description:** Deep dependency analysis with transitive conflict detection
- **Community Alternative:** Basic dependency scanning

### BATCH_OPERATIONS
- **Tier:** PREMIUM
- **Description:** Process multiple projects in batch operations
- **Community Alternative:** Single project analysis only

### CUSTOM_RECIPES
- **Tier:** PREMIUM
- **Description:** Create and use custom Jakarta migration recipes
- **Community Alternative:** Standard recipes only

### API_ACCESS
- **Tier:** PREMIUM
- **Description:** Programmatic API access for CI/CD integrations
- **Community Alternative:** MCP interface only

### EXPORT_REPORTS
- **Tier:** PREMIUM
- **Description:** Export detailed migration reports in multiple formats
- **Community Alternative:** Basic JSON output only

### PRIORITY_SUPPORT
- **Tier:** ENTERPRISE
- **Description:** Priority support with SLA guarantees

### CLOUD_HOSTING
- **Tier:** ENTERPRISE
- **Description:** Managed cloud hosting with automatic scaling

## Development Mode

For development and testing, you can disable feature flags entirely:

```yaml
jakarta:
  migration:
    feature-flags:
      enabled: false  # All features available
```

Or enable specific features via overrides:

```yaml
jakarta:
  migration:
    feature-flags:
      enabled: true
      default-tier: COMMUNITY
      features:
        auto-fixes: true  # Override: enable even for community
```

## License Validation

The `LicenseService` currently implements placeholder validation:

- Keys starting with `PREMIUM-` → PREMIUM tier
- Keys starting with `ENTERPRISE-` → ENTERPRISE tier
- Everything else → Invalid

### Future Implementation

The license service should be extended to:

1. **Validate against license server** - Online validation for subscription licenses
2. **Check expiration dates** - Support time-limited licenses
3. **Verify signatures** - Cryptographic validation of license keys
4. **Support different license types**:
   - Trial licenses (time-limited)
   - Subscription licenses (recurring)
   - Perpetual licenses (one-time purchase)
   - Enterprise licenses (custom terms)

### Integration Points

- **Stripe** - For subscription validation
- **Apify** - For usage-based billing
- **Custom license server** - For enterprise customers

## Testing

Feature flags can be tested by:

1. **Unit tests** - Mock `FeatureFlagsService` or `LicenseService`
2. **Integration tests** - Configure different tiers via test properties
3. **Manual testing** - Set `JAKARTA_MCP_LICENSE_KEY` environment variable

### Example Test Configuration

```yaml
# src/test/resources/application-test.yml
jakarta:
  migration:
    feature-flags:
      enabled: true
      default-tier: PREMIUM  # Enable all features for tests
```

## Migration Path

When implementing monetization:

1. **Phase 1 (Current):** Feature flags infrastructure in place
2. **Phase 2:** Integrate license validation with Stripe/Apify
3. **Phase 3:** Add upgrade prompts in MCP tools
4. **Phase 4:** Implement premium features
5. **Phase 5:** Add usage tracking and billing

## Best Practices

1. **Always check feature flags** before executing premium features
2. **Provide clear upgrade messages** when features are unavailable
3. **Use `requireEnabled()`** for critical premium features
4. **Log feature flag checks** for debugging and analytics
5. **Test with different tiers** to ensure proper gating

## Related Documentation

- [Monetization Research](../research/monetisation.md)
- [MCP Tools Implementation](../mcp/MCP_TOOLS_IMPLEMENTATION.md)
- [Configuration Guide](../setup/MANUAL_SETUP.md)


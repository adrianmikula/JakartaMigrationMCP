# Freemium Model Setup

This document describes the freemium model implementation for the Jakarta Migration plugin.

## Overview

The Jakarta Migration plugin uses a freemium model with:
- **Community (Free)**: Basic analysis tools
- **Premium ($49/month or $399/year)**: Advanced features including auto-fixes

## Plugin ID

The plugin is published on JetBrains Marketplace with ID: **30093**

- **Marketplace URL**: https://plugins.jetbrains.com/plugin/30093-jakarta-migration
- **Plugin ID in code**: `30093` (numeric, used for LicensingFacade)

## License Tiers

### Community (Free)
Available to all users without purchase:
- `analyzeJakartaReadiness` - Project readiness analysis
- `detectBlockers` - Migration blocker detection  
- `recommendVersions` - Dependency version recommendations

### Premium
Requires JetBrains Marketplace subscription ($49/month or $399/year):
- `createMigrationPlan` - Phased migration planning
- `analyzeMigrationImpact` - Comprehensive impact analysis
- `verifyRuntime` - JAR runtime verification
- `applyAutoFixes` - Automatic code fixes
- `executeMigrationPlan` - Full automated migration
- Advanced analysis features
- Binary fixes

## Implementation

### Feature Flags

The feature flag system is defined in [`FeatureFlagsProperties.java`](../../community-core-engine/src/main/java/adrianmikula/jakartamigration/config/FeatureFlagsProperties.java):

```java
public static final double MONTHLY_PRICE_USD = 49.0;
public static final double YEARLY_PRICE_USD = 399.0;
public static final int FREE_TRIAL_DAYS = 7;
```

### License Service

License validation uses two approaches:

1. **IntelliJ LicensingFacade** - For IDE-installed plugins:
   ```java
   // Uses com.intellij.ide.licensing.LicensingFacade
   Boolean hasLicense = (Boolean) facadeClass
       .getMethod("isLicensed", String.class)
       .invoke(facade, "30093");
   ```

2. **Marketplace API** - For standalone validation:
   ```java
   // Calls https://plugins.jetbrains.com/api/license/validate
   MarketplaceLicenseService.validateLicense(licenseKey);
   ```

### Trial Support

Users can start a 7-day free trial:
- Trial activation stores timestamp in `FeatureFlagsProperties.trialEndTimestamp`
- After trial expires, user is reverted to Community tier
- Trial can be started from IntelliJ plugin UI

## IntelliJ Plugin Integration

The IntelliJ plugin UI shows:

1. **Upgrade Button**: Opens JetBrains Marketplace for purchase
2. **Trial Button**: Starts 7-day free trial
3. **Premium Badge**: Shows when premium is active

See [`MigrationToolWindow.java`](../../premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/MigrationToolWindow.java) for implementation.

## Testing

### Test Keys

For testing, the following keys are accepted:

| Key | Type | Duration |
|-----|------|----------|
| `TEST-*` | Development | 7 days |
| `DEV-*` | Development | 7 days |
| `PREMIUM` | Premium | 1 year |
| `EXPIRED` | Premium | Expired |

### Running Tests

```bash
# Run all license-related tests
./gradlew :community-core-engine:test --tests "adrianmikula.jakartamigration.config.*"

# Run MarketplaceLicenseService tests
./gradlew :community-core-engine:test --tests "MarketplaceLicenseServiceTest"
```

## Environment Variables

For development/testing:

| Variable | Description |
|----------|-------------|
| `jakarta.migration.license.tier` | Override license tier (COMMUNITY/PREMIUM) |
| `jakarta.migration.premium` | Set to "true" to enable premium features |

## See Also

- [JetBrains Marketplace License Validation](https://plugins.jetbrains.com/docs/marketplace/add-marketplace-license-verification-calls-to-the-plugin-code.html)
- [Freemium Plugins](https://plugins.jetbrains.com/docs/marketplace/freemium.html)
- [Feature Flags Documentation](../architecture/FEATURE_FLAGS.md)

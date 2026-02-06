# Feature Flags Setup Guide

This guide explains how to configure feature flags for the Jakarta Migration MCP Server. The server uses a single **default tier**; there is no license key or payment integration.

## Quick Start

### Default: All Features Enabled (ENTERPRISE)

The default configuration enables all features:

```yaml
# application.yml (default)
jakarta:
  migration:
    feature-flags:
      enabled: true
      default-tier: ENTERPRISE
      features: {}
```

No environment variables or license keys are required.

### Restrict to a Lower Tier (Testing)

To test tier-gated behavior, set a lower default tier:

```yaml
jakarta:
  migration:
    feature-flags:
      enabled: true
      default-tier: COMMUNITY  # or PREMIUM
      features: {}
```

### Per-Feature Overrides

You can enable specific features even when the default tier would not allow them:

```yaml
jakarta:
  migration:
    feature-flags:
      enabled: true
      default-tier: COMMUNITY
      features:
        auto-fixes: true  # Enable even for community tier
```

## Testing Feature Flags

### Test with ENTERPRISE (default)

```bash
java -jar jakarta-migration-mcp.jar \
  --spring.main.web-application-type=none \
  --spring.profiles.active=mcp-stdio
```

### Test with a Lower Tier

```bash
java -jar jakarta-migration-mcp.jar \
  --jakarta.migration.feature-flags.default-tier=COMMUNITY \
  --spring.main.web-application-type=none \
  --spring.profiles.active=mcp-stdio
```

## Available Features

See [Feature Flags Documentation](../architecture/FEATURE_FLAGS.md) for the full list of features and tiers.

## Related Documentation

- [Feature Flags Architecture](../architecture/FEATURE_FLAGS.md)
- [Environment Variables](ENVIRONMENT_VARIABLES.md)

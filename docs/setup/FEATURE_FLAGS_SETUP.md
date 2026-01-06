# Feature Flags Setup Guide

This guide explains how to configure and use feature flags for the Jakarta Migration MCP Server.

## Quick Start

### Enable All Features (Development)

```yaml
# application.yml
jakarta:
  migration:
    feature-flags:
      enabled: false  # Disables feature flag checks
```

### Community Tier (Default)

```yaml
jakarta:
  migration:
    feature-flags:
      enabled: true
      default-tier: COMMUNITY
      license-key: ""
```

### Premium Tier

```yaml
jakarta:
  migration:
    feature-flags:
      enabled: true
      default-tier: PREMIUM
      # Or use license key:
      license-key: ${JAKARTA_MCP_LICENSE_KEY:PREMIUM-test-key}
```

### Environment Variable Configuration

```bash
# Set license key
export JAKARTA_MCP_LICENSE_KEY="PREMIUM-your-key-here"

# Set purchase URL (for upgrade messages)
export JAKARTA_MCP_PURCHASE_URL="https://buy.stripe.com/your-link"
```

## Testing Feature Flags

### Test with Community Tier

```bash
# No license key = Community tier
java -jar bug-bounty-finder.jar \
  --spring.main.web-application-type=none \
  --spring.profiles.active=mcp
```

### Test with Premium Tier

```bash
# Set license key via environment variable
export JAKARTA_MCP_LICENSE_KEY="PREMIUM-test-key"
java -jar bug-bounty-finder.jar \
  --spring.main.web-application-type=none \
  --spring.profiles.active=mcp
```

### Test with Feature Override

```yaml
# application.yml
jakarta:
  migration:
    feature-flags:
      enabled: true
      default-tier: COMMUNITY
      features:
        auto-fixes: true  # Enable even for community tier
```

## Available Features

See [Feature Flags Documentation](../architecture/FEATURE_FLAGS.md) for complete list.

## Next Steps

1. **Configure feature flags** in `application.yml`
2. **Set license key** via environment variable or config
3. **Test feature availability** using the MCP tools
4. **Implement premium features** using `FeatureFlagsService`

## Related Documentation

- [Feature Flags Architecture](../architecture/FEATURE_FLAGS.md)
- [Monetization Research](../research/monetisation.md)


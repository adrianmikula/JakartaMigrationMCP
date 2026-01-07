# Stripe Integration Disabled - January 6, 2026

## Decision

Temporarily disabled Stripe integration to get MCP server working quickly.

## Changes Made

### 1. Disabled Stripe Validation

**File:** `src/main/resources/application.yml`

Changed:
```yaml
enabled: ${STRIPE_VALIDATION_ENABLED:true}
```

To:
```yaml
enabled: ${STRIPE_VALIDATION_ENABLED:false}
```

### 2. Fixed Configuration Binding

**File:** `src/main/resources/application.yml`

Changed:
```yaml
price-id-to-tier:
  # Example:
  # price_1234567890: PREMIUM
```

To:
```yaml
price-id-to-tier: {}
  # Example:
  # price_1234567890: PREMIUM
```

Same issue as `features` - empty YAML key with only comments was being bound as empty string instead of empty map.

## Impact

- Stripe license validation is disabled
- `StripeLicenseService` will still be created but will return `null` for all validations
- `LicenseService` will skip Stripe validation and use Apify or simple validation instead
- MCP server should start without Stripe configuration errors

## Re-enabling Stripe

To re-enable Stripe later:
1. Set `enabled: true` in `application.yml`
2. Configure `STRIPE_SECRET_KEY` environment variable
3. Set product IDs if needed
4. Configure price ID mappings if needed

**Status:** ✅ Disabled


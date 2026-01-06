# Stripe License Validation Setup

This guide explains how to configure and use Stripe for license validation in the Jakarta Migration MCP Server.

## Overview

Stripe is used for subscription-based license validation. The Jakarta Migration MCP Server can validate license keys (Stripe customer IDs, subscription IDs, or custom keys) via Stripe's API to determine user license tiers.

## How It Works

1. **User provides Stripe subscription/customer ID** as their license key
2. **Server validates subscription** against Stripe API
3. **Tier determined** based on subscription product/price
4. **Results cached** to reduce API calls

## Configuration

### Basic Setup

Add to `application.yml`:

```yaml
jakarta:
  migration:
    stripe:
      enabled: true
      api-url: https://api.stripe.com/v1
      secret-key: ${STRIPE_SECRET_KEY:}
      cache-ttl-seconds: 3600
      timeout-seconds: 5
      product-id-premium: prod_premium_id
      product-id-enterprise: prod_enterprise_id
      license-key-prefix: stripe_
```

### Environment Variables

```bash
# Your Stripe secret key (for making API requests)
export STRIPE_SECRET_KEY="sk_live_your_secret_key_here"

# Enable/disable Stripe validation
export STRIPE_VALIDATION_ENABLED="true"

# Stripe API URL (usually doesn't need to change)
export STRIPE_API_URL="https://api.stripe.com/v1"

# Cache TTL in seconds
export STRIPE_CACHE_TTL="3600"

# Request timeout in seconds
export STRIPE_TIMEOUT="5"

# Product IDs for tier identification
export STRIPE_PRODUCT_ID_PREMIUM="prod_premium_id"
export STRIPE_PRODUCT_ID_ENTERPRISE="prod_enterprise_id"

# License key prefix
export STRIPE_LICENSE_PREFIX="stripe_"

# Allow offline validation when API is unavailable
export STRIPE_ALLOW_OFFLINE="true"
```

## Getting Your Stripe API Keys

1. **Sign up** at [Stripe.com](https://stripe.com)
2. **Go to Developers** → **API keys**
3. **Get your Secret key** (starts with `sk_live_` for production or `sk_test_` for testing)
4. **Create Products** in Stripe Dashboard:
   - Create a "Premium" product
   - Create an "Enterprise" product
   - Note the Product IDs (starts with `prod_`)

## License Key Formats

The service supports multiple license key formats:

### 1. Subscription ID (Recommended)

```bash
export JAKARTA_MCP_LICENSE_KEY="sub_1234567890abcdef"
```

Direct subscription ID validation - fastest and most reliable.

### 2. Customer ID

```bash
export JAKARTA_MCP_LICENSE_KEY="cus_1234567890abcdef"
```

Validates by finding active subscriptions for the customer.

### 3. Custom Key with Prefix

```bash
export JAKARTA_MCP_LICENSE_KEY="stripe_sub_1234567890abcdef"
```

Custom format with `stripe_` prefix.

## Tier Determination

The service determines tier based on:

1. **Product ID matching**:
   - If subscription's product matches `product-id-premium` → PREMIUM
   - If subscription's product matches `product-id-enterprise` → ENTERPRISE

2. **Price ID mapping** (optional):
   - Configure `price-id-to-tier` mapping in `application.yml`
   - Maps specific price IDs to tiers

3. **Default behavior**:
   - Active subscription → PREMIUM tier
   - Trialing subscription → PREMIUM tier
   - Canceled/past_due → Invalid

## Setting Up Stripe Products

### Step 1: Create Products

1. Go to **Stripe Dashboard** → **Products**
2. Click **+ Add product**
3. Create:
   - **Premium Product**: Name it "Jakarta Migration MCP - Premium"
   - **Enterprise Product**: Name it "Jakarta Migration MCP - Enterprise"
4. Note the Product IDs (e.g., `prod_ABC123`)

### Step 2: Create Prices

1. For each product, create a price:
   - **Recurring** (monthly/yearly) or **One-time**
   - Set the amount
2. Note the Price IDs (e.g., `price_XYZ789`)

### Step 3: Configure in application.yml

```yaml
jakarta:
  migration:
    stripe:
      product-id-premium: prod_ABC123
      product-id-enterprise: prod_DEF456
      price-id-to-tier:
        price_XYZ789: PREMIUM
        price_UVW012: ENTERPRISE
```

## Testing

### Test with Test Keys

For testing without Stripe API:

```bash
# Premium test key
export JAKARTA_MCP_LICENSE_KEY="stripe_PREMIUM-test-key"

# Enterprise test key
export JAKARTA_MCP_LICENSE_KEY="stripe_ENTERPRISE-test-key"
```

### Test with Stripe Test Mode

1. Use Stripe test mode secret key (`sk_test_...`)
2. Create test subscriptions in Stripe Dashboard
3. Use test subscription IDs as license keys:

```bash
export STRIPE_SECRET_KEY="sk_test_your_test_key"
export JAKARTA_MCP_LICENSE_KEY="sub_test_1234567890"
```

### Disable Stripe Validation

For local development without Stripe:

```yaml
jakarta:
  migration:
    stripe:
      enabled: false
```

## Caching

License validation results are cached in memory:

- **Default TTL:** 3600 seconds (1 hour)
- **Configurable:** via `cache-ttl-seconds`
- **Cache cleared:** on server restart or via `clearCache()` method

## Error Handling

The service handles various error scenarios:

1. **Invalid subscription (404):** Returns null (invalid license)
2. **Network errors:** Falls back to offline validation (if enabled)
3. **API unavailable:** Falls back to simple validation (if enabled)
4. **Timeout:** Falls back to offline validation

## Monitoring

Check logs for validation activity:

```
DEBUG: Stripe license validated and cached: sub_... -> PREMIUM
WARN: Stripe license validation failed: Connection timeout
DEBUG: Falling back to simple validation
```

## Integration with Payment Links

As described in the monetization document, you can use Stripe Payment Links:

1. **Create Payment Link** in Stripe Dashboard
2. **Configure automatic delivery**:
   - After payment, Stripe emails the customer
   - Email contains subscription ID or customer ID
   - Customer uses this as their license key

### Example Flow

1. User clicks Stripe Payment Link: `https://buy.stripe.com/abc123`
2. User completes payment
3. Stripe sends email with subscription ID: `sub_1234567890`
4. User sets license key: `export JAKARTA_MCP_LICENSE_KEY="sub_1234567890"`
5. Server validates subscription and grants tier

## Troubleshooting

### Validation Always Returns Null

- Check that `STRIPE_SECRET_KEY` is set correctly
- Verify subscription ID format (starts with `sub_`)
- Check subscription status in Stripe Dashboard (must be `active` or `trialing`)
- Review logs for error messages

### Wrong Tier Assigned

- Verify Product IDs match your Stripe products
- Check `price-id-to-tier` mapping if using price-based validation
- Ensure subscription product matches configured product IDs

### Slow Validation

- Reduce `cache-ttl-seconds` if you need fresher results
- Check network latency to Stripe API
- Consider increasing `timeout-seconds` if needed

### API Errors

- Verify Stripe secret key is correct
- Check API key permissions in Stripe Dashboard
- Ensure you're using the correct API version
- Check Stripe API status page

## Best Practices

1. **Use Subscription IDs** instead of customer IDs for faster validation
2. **Set appropriate cache TTL** based on your needs
3. **Monitor subscription status** - handle cancellations gracefully
4. **Use test mode** for development and testing
5. **Keep secret keys secure** - never commit to version control

## Next Steps

1. **Set up webhooks** (future):
   - Listen for subscription events (canceled, renewed, etc.)
   - Update license status in real-time
   - Clear cache when subscriptions change

2. **Add usage tracking**:
   - Track feature usage per subscription
   - Implement usage limits
   - Report usage to Stripe

3. **Support multiple providers**:
   - Use both Stripe and Apify
   - Fallback between providers
   - Unified license management

## Related Documentation

- [Feature Flags Setup](FEATURE_FLAGS_SETUP.md)
- [Apify License Setup](APIFY_LICENSE_SETUP.md)
- [Monetization Research](../research/monetisation.md)
- [Stripe API Documentation](https://stripe.com/docs/api)


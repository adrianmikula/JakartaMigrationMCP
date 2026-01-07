# Stripe Payment Processor Setup

## Overview

Stripe is the **primary payment processor** for the Jakarta Migration MCP Server. All license validation and credit management is handled through Stripe subscriptions.

## Why Stripe?

- ✅ Industry-standard payment processing
- ✅ Secure subscription management
- ✅ Automatic billing and renewals
- ✅ Webhook support for real-time updates
- ✅ Comprehensive dashboard and analytics
- ✅ Support for multiple currencies and payment methods

## Setup Steps

### Step 1: Create Stripe Account

1. Go to [https://stripe.com](https://stripe.com)
2. Sign up for a free account
3. Complete account verification

### Step 2: Get API Keys

1. Navigate to **Developers** → **API keys** in Stripe Dashboard
2. Copy your **Secret key** (starts with `sk_test_` for test mode or `sk_live_` for production)
3. Keep this key secure - never commit it to git

**Test Mode vs Live Mode:**
- **Test Mode**: Use `sk_test_...` keys for development/testing
- **Live Mode**: Use `sk_live_...` keys for production

### Step 3: Create Products and Prices

1. Navigate to **Products** in Stripe Dashboard
2. Create products for each license tier:

#### Premium Tier Product
- **Name**: Jakarta Migration MCP - Premium
- **Description**: Premium tier with all features
- **Pricing**: Set your monthly/annual price
- **Copy the Product ID** (starts with `prod_...`)

#### Enterprise Tier Product
- **Name**: Jakarta Migration MCP - Enterprise
- **Description**: Enterprise tier with priority support
- **Pricing**: Set your monthly/annual price
- **Copy the Product ID** (starts with `prod_...`)

### Step 4: Configure Environment Variables

Set these in Railway (or your deployment platform):

```bash
# Stripe Configuration
STRIPE_VALIDATION_ENABLED=true
STRIPE_SECRET_KEY=sk_live_...  # or sk_test_... for testing
STRIPE_PRODUCT_ID_PREMIUM=prod_...
STRIPE_PRODUCT_ID_ENTERPRISE=prod_...
```

### Step 5: Set Up Webhooks (Optional but Recommended)

Webhooks allow real-time updates when subscriptions change:

1. Navigate to **Developers** → **Webhooks** in Stripe Dashboard
2. Click **Add endpoint**
3. Set endpoint URL: `https://your-app.railway.app/api/v1/stripe/webhook`
4. Select events to listen to:
   - `customer.subscription.created`
   - `customer.subscription.updated`
   - `customer.subscription.deleted`
   - `invoice.payment_succeeded`
   - `invoice.payment_failed`
5. Copy the **Signing secret** (starts with `whsec_...`)
6. Add to environment variables: `STRIPE_WEBHOOK_SECRET=whsec_...`

## License Key Formats

Stripe supports multiple license key formats:

### Format 1: Subscription ID (Recommended)
```
sub_1234567890abcdef
```
- Direct subscription ID from Stripe
- Most reliable validation method

### Format 2: Customer ID
```
cus_1234567890abcdef
```
- Customer ID from Stripe
- System finds active subscriptions for the customer

### Format 3: Custom Key with Prefix
```
stripe_sub_1234567890abcdef
```
- Custom format with `stripe_` prefix
- Extracts subscription ID and validates

## Testing

### Test Mode Setup

1. Use test mode API keys (`sk_test_...`)
2. Create test products in Stripe Dashboard
3. Create test subscriptions using Stripe test cards:
   - Success: `4242 4242 4242 4242`
   - Decline: `4000 0000 0000 0002`
4. Test license validation with test subscription IDs

### Test License Keys

For development/testing, you can use test keys:

```bash
# Premium tier test key
PREMIUM-test-key-123

# Enterprise tier test key
ENTERPRISE-test-key-123
```

These work without Stripe API calls when `STRIPE_VALIDATION_ENABLED=false` or when offline validation is allowed.

## Production Checklist

- [ ] Stripe account created and verified
- [ ] Live mode API keys obtained (`sk_live_...`)
- [ ] Products created for Premium and Enterprise tiers
- [ ] Product IDs configured in environment variables
- [ ] Webhook endpoint configured (optional)
- [ ] Webhook secret configured (if using webhooks)
- [ ] Test subscriptions created and validated
- [ ] License validation tested end-to-end

## Troubleshooting

### Problem: License validation always returns null

**Solutions**:
- Verify `STRIPE_SECRET_KEY` is set correctly
- Check that `STRIPE_VALIDATION_ENABLED=true`
- Verify product IDs match your Stripe products
- Check Stripe API logs in dashboard for errors
- Ensure subscription is active in Stripe

### Problem: Webhook not receiving events

**Solutions**:
- Verify webhook URL is accessible
- Check webhook secret matches
- Verify events are selected in Stripe dashboard
- Check application logs for webhook processing errors

### Problem: Test keys not working

**Solutions**:
- Ensure test keys start with `PREMIUM-` or `ENTERPRISE-`
- Check that offline validation is allowed: `STRIPE_ALLOW_OFFLINE=true`
- Verify Stripe validation is disabled for testing: `STRIPE_VALIDATION_ENABLED=false`

## Security Best Practices

1. **Never commit API keys to git** - Always use environment variables
2. **Use test keys for development** - Never use live keys in dev
3. **Rotate keys periodically** - Update keys every 90 days
4. **Use webhook signatures** - Always validate webhook requests
5. **Monitor API usage** - Set up alerts for unusual activity
6. **Use least privilege** - Only grant necessary permissions

## References

- [Stripe Documentation](https://stripe.com/docs)
- [Stripe API Reference](https://stripe.com/docs/api)
- [Stripe Webhooks Guide](https://stripe.com/docs/webhooks)
- [Stripe Testing Guide](https://stripe.com/docs/testing)

## Migration from Apify

If you were previously using Apify for license validation:

1. **Disable Apify**: Set `APIFY_VALIDATION_ENABLED=false` (default)
2. **Enable Stripe**: Set `STRIPE_VALIDATION_ENABLED=true` (default)
3. **Configure Stripe**: Follow setup steps above
4. **Migrate users**: Issue new Stripe-based license keys to existing users
5. **Test thoroughly**: Verify all license validations work correctly

Apify services are automatically disabled and will not be loaded unless explicitly enabled.


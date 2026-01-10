# Environment Variables Setup Guide

This guide explains how to configure environment variables for the Jakarta Migration MCP Server, both for local development and Apify deployment.

## Quick Start

1. **Copy the example file:**
   ```bash
   cp .env.example .env
   ```

2. **Fill in your API tokens:**
   - `APIFY_API_TOKEN` - Your Apify API token (for Apify-hosted deployment)
   - `STRIPE_SECRET_KEY` - Your Stripe secret key (for Stripe license validation)

3. **For local development:** The `.env` file is automatically loaded by Spring Boot

4. **For Apify deployment:** Set environment variables in Apify Actor settings

## Environment Variables Reference

### License & Feature Flags

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `JAKARTA_MCP_LICENSE_KEY` | User's license key (Stripe subscription ID, Apify token, or custom key) | No | Empty |
| `JAKARTA_MCP_PURCHASE_URL` | Stripe payment link for premium licenses | No | `https://buy.stripe.com/00w9AU4lv5sT7lddpM2kw00` |

### Apify Configuration

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `APIFY_API_TOKEN` | Your Apify API token for making validation requests | Yes (for Apify deployment) | Empty |
| `APIFY_VALIDATION_ENABLED` | Enable/disable Apify license validation | No | `true` |
| `APIFY_API_URL` | Apify API base URL | No | `https://api.apify.com/v2` |
| `APIFY_ACTOR_ID` | Apify Actor ID (optional) | No | Empty |
| `APIFY_CACHE_TTL` | Cache TTL in seconds | No | `3600` |
| `APIFY_TIMEOUT` | Request timeout in seconds | No | `5` |
| `APIFY_ALLOW_OFFLINE` | Allow offline validation | No | `true` |

**Getting Your Apify API Token:**
1. Go to [Apify Console → Integrations](https://console.apify.com/account#/integrations)
2. Copy your API token
3. Add to `.env` file or Apify Actor environment variables

### Stripe Configuration

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `STRIPE_SECRET_KEY` | Your Stripe secret key for API authentication | Yes (for Stripe validation) | Empty |
| `STRIPE_VALIDATION_ENABLED` | Enable/disable Stripe license validation | No | `false` |
| `STRIPE_API_URL` | Stripe API base URL | No | `https://api.stripe.com/v1` |
| `STRIPE_PRODUCT_ID_PREMIUM` | Stripe Product ID for Premium tier | No | Empty |
| `STRIPE_PRODUCT_ID_ENTERPRISE` | Stripe Product ID for Enterprise tier | No | Empty |
| `STRIPE_LICENSE_PREFIX` | License key prefix for Stripe keys | No | `stripe_` |
| `STRIPE_CACHE_TTL` | Cache TTL in seconds | No | `3600` |
| `STRIPE_TIMEOUT` | Request timeout in seconds | No | `5` |
| `STRIPE_ALLOW_OFFLINE` | Allow offline validation | No | `true` |
| `STRIPE_WEBHOOK_SECRET` | Webhook secret for Stripe webhooks | No | Empty |

**Getting Your Stripe Secret Key:**
1. Go to [Stripe Dashboard → API Keys](https://dashboard.stripe.com/apikeys)
2. Copy your secret key (starts with `sk_test_` for testing or `sk_live_` for production)
3. **⚠️ NEVER commit this to source control!**
4. Add to `.env` file (for local) or Apify Actor environment variables (for deployment)

### Apify Actor Environment Variables (Auto-Set)

These are automatically set by Apify when running as an Actor:

| Variable | Description | Set By |
|----------|-------------|--------|
| `ACTOR_ID` | Apify Actor ID | Apify Platform |
| `ACTOR_RUN_ID` | Apify Run ID | Apify Platform |
| `ACTOR_MAX_TOTAL_CHARGE_USD` | Optional spending limit | You (optional) |

### MCP Transport Configuration

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `MCP_TRANSPORT` | Transport mode: `stdio` (local) or `sse` (Apify/HTTP) | No | `stdio` |
| `MCP_SSE_PORT` | SSE port (only for SSE transport) | No | `8080` |
| `MCP_SSE_PATH` | SSE path (only for SSE transport) | No | `/mcp/sse` |

## Local Development Setup

### Step 1: Create .env File

```bash
# Copy the example file
cp .env.example .env

# Or create manually
touch .env
```

### Step 2: Add Required Variables

Edit `.env` file:

```bash
# For testing Apify license validation
APIFY_API_TOKEN=your_apify_api_token_here

# For testing Stripe license validation
STRIPE_SECRET_KEY=sk_test_your_stripe_secret_key_here
STRIPE_VALIDATION_ENABLED=true
STRIPE_PRODUCT_ID_PREMIUM=prod_your_premium_product_id
STRIPE_PRODUCT_ID_ENTERPRISE=prod_your_enterprise_product_id

# Test with a license key
JAKARTA_MCP_LICENSE_KEY=sub_your_stripe_subscription_id
```

### Step 3: Run the Application

Spring Boot automatically loads `.env` files. The application will use these environment variables.

```bash
# Run locally
./gradlew bootRun

# Or with JAR
java -jar build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar
```

## Apify Deployment Setup

### Step 1: Set Environment Variables in Apify

1. Go to [Apify Console → Actors → Your Actor → Settings → Environment Variables](https://console.apify.com/actors)
2. Add the following variables:

**Required:**
```
APIFY_API_TOKEN=your_apify_api_token_here
```

**Optional (for Stripe validation):**
```
STRIPE_SECRET_KEY=sk_live_your_stripe_secret_key_here
STRIPE_VALIDATION_ENABLED=true
STRIPE_PRODUCT_ID_PREMIUM=prod_your_premium_product_id
STRIPE_PRODUCT_ID_ENTERPRISE=prod_your_enterprise_product_id
```

**Optional (for spending limits):**
```
ACTOR_MAX_TOTAL_CHARGE_USD=10.00
```

### Step 2: Deploy Actor

The environment variables will be available to your Actor at runtime. They are automatically injected by Apify.

### Step 3: Verify

Check Actor logs to verify environment variables are loaded correctly.

## Security Best Practices

### ✅ DO:

- ✅ Store secrets in `.env` file (gitignored)
- ✅ Use environment variables in Apify Actor settings
- ✅ Use test keys (`sk_test_...`) for development
- ✅ Rotate API keys regularly
- ✅ Use different keys for development and production
- ✅ Set spending limits in Apify (`ACTOR_MAX_TOTAL_CHARGE_USD`)

### ❌ DON'T:

- ❌ Commit `.env` file to git (already in `.gitignore`)
- ❌ Commit API tokens in source code
- ❌ Share API tokens in documentation or issues
- ❌ Use production keys in development
- ❌ Hardcode secrets in configuration files

## Stripe API Key Security

The Stripe secret key is handled securely:

1. **Local Development:**
   - Stored in `.env` file (gitignored)
   - Loaded at runtime via Spring Boot's environment variable support
   - Never compiled into the JAR

2. **Apify Deployment:**
   - Set as environment variable in Apify Actor settings
   - Injected at runtime by Apify platform
   - Never stored in source code or Docker image

3. **User's Local Installation:**
   - Users can set `STRIPE_SECRET_KEY` environment variable
   - Or configure via `application.yml` (not recommended for secrets)
   - The key is only used at runtime, never compiled into code

## Troubleshooting

### Environment Variables Not Loading

**Problem:** Environment variables are not being read.

**Solutions:**
1. Check `.env` file exists and is in project root
2. Verify variable names match exactly (case-sensitive)
3. Restart the application after changing `.env`
4. For Apify: Check Actor environment variables in dashboard

### Stripe API Key Not Working

**Problem:** Stripe validation fails with authentication error.

**Solutions:**
1. Verify `STRIPE_SECRET_KEY` is set correctly
2. Check key format (should start with `sk_test_` or `sk_live_`)
3. Ensure key has correct permissions in Stripe dashboard
4. Check Stripe API logs for detailed error messages

### Apify API Token Not Working

**Problem:** Apify validation fails.

**Solutions:**
1. Verify `APIFY_API_TOKEN` is set correctly
2. Check token has not expired
3. Verify token has correct permissions
4. Check Apify API status page

## Related Documentation

- [Feature Flags Setup](FEATURE_FLAGS_SETUP.md)
- [Apify License Setup](APIFY_LICENSE_SETUP.md)
- [Stripe License Setup](../improvements/STRIPE_LICENSE_SETUP.md)
- [Apify Billing Events](APIFY_BILLING_EVENTS.md)


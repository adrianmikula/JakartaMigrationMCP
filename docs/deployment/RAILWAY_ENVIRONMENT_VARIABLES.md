# Railway Environment Variables

## Overview

This document lists all environment variables that need to be configured in the Railway dashboard for the Jakarta Migration MCP Server deployment.

**Note**: Environment variables are set in the Railway dashboard, not in `railway.json`. The `railway.json` file is for build and deploy configuration only.

## Required Environment Variables

### Core Configuration

| Variable | Value | Required | Description |
|----------|-------|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | `mcp-streamable-http` | ✅ **Critical** | Activates the streamable-http profile for web server mode |
| `PORT` | (Auto-set by Railway) | ✅ Auto-provided | Railway automatically provides this - don't set manually |

### License API Configuration

| Variable | Value | Required | Description |
|----------|-------|----------|-------------|
| `LICENSE_API_SERVER_API_KEY` | `your-secret-api-key-here` | ✅ **Required** | API key for authenticating requests to license endpoints. Generate with: `openssl rand -hex 32` |
| `LICENSE_API_ENABLED` | `true` | ❌ Optional | Enable/disable license API endpoints (default: `true`) |

### License Validation

#### Stripe License Validation (Primary Payment Processor)

| Variable | Value | Required | Description |
|----------|-------|----------|-------------|
| `STRIPE_VALIDATION_ENABLED` | `true` | ❌ Optional | Enable/disable Stripe license validation (default: `true`) |
| `STRIPE_SECRET_KEY` | `sk_live_...` or `sk_test_...` | ✅ **Required** | Stripe secret key for API authentication (get from Stripe Dashboard) |
| `STRIPE_API_URL` | `https://api.stripe.com/v1` | ❌ Optional | Stripe API base URL (default: `https://api.stripe.com/v1`) |
| `STRIPE_CACHE_TTL` | `3600` | ❌ Optional | Cache TTL for license validation in seconds (default: `3600`) |
| `STRIPE_TIMEOUT` | `5` | ❌ Optional | Request timeout in seconds (default: `5`) |
| `STRIPE_PRODUCT_ID_PREMIUM` | `prod_...` | ❌ Optional | Stripe Product ID for premium tier |
| `STRIPE_PRODUCT_ID_ENTERPRISE` | `prod_...` | ❌ Optional | Stripe Product ID for enterprise tier |
| `STRIPE_LICENSE_PREFIX` | `stripe_` | ❌ Optional | License key prefix for Stripe-based keys (default: `stripe_`) |
| `STRIPE_ALLOW_OFFLINE` | `true` | ❌ Optional | Allow offline validation when API is unavailable (default: `true`) |
| `STRIPE_WEBHOOK_SECRET` | `whsec_...` | ❌ Optional | Stripe webhook secret for validating webhooks |

#### Apify License Validation (Deprecated)

**Note**: Apify support is deprecated in favor of Stripe. Apify services are disabled by default and will not be loaded unless explicitly enabled.

| Variable | Value | Required | Description |
|----------|-------|----------|-------------|
| `APIFY_VALIDATION_ENABLED` | `false` | ❌ Optional | Enable/disable Apify license validation (default: `false`) |
| `APIFY_API_TOKEN` | `your-apify-token` | ❌ Optional | Your Apify API token for making validation requests (only if enabled) |
| `APIFY_API_URL` | `https://api.apify.com/v2` | ❌ Optional | Apify API base URL (default: `https://api.apify.com/v2`) |
| `APIFY_CACHE_TTL` | `3600` | ❌ Optional | Cache TTL for license validation in seconds (default: `3600`) |
| `APIFY_TIMEOUT` | `5` | ❌ Optional | Request timeout in seconds (default: `5`) |
| `APIFY_ACTOR_ID` | `your-actor-id` | ❌ Optional | Apify Actor ID/name for license validation |
| `APIFY_ALLOW_OFFLINE` | `true` | ❌ Optional | Allow offline validation when API is unavailable (default: `true`) |

### Feature Flags

| Variable | Value | Required | Description |
|----------|-------|----------|-------------|
| `JAKARTA_MCP_LICENSE_KEY` | `your-license-key` | ❌ Optional | License key for premium features (can be set per user) |

## Setting Environment Variables in Railway

### Step 1: Navigate to Variables Tab

1. Go to your Railway project dashboard
2. Select your service
3. Click on the **Variables** tab

### Step 2: Add Variables

1. Click **+ New Variable**
2. Enter the variable name (e.g., `LICENSE_API_SERVER_API_KEY`)
3. Enter the variable value
4. Click **Add**

### Step 3: Deploy

Railway will automatically redeploy when environment variables are added or changed.

## Generating API Keys

### Generate License API Server Key

```bash
# Using OpenSSL
openssl rand -hex 32

# Using PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))

# Using Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

### Example Generated Key

```
a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2
```

## Minimum Required Configuration

For basic deployment with license API enabled, you need at minimum:

```bash
SPRING_PROFILES_ACTIVE=mcp-streamable-http
LICENSE_API_SERVER_API_KEY=<generated-key>
STRIPE_SECRET_KEY=<your-stripe-secret-key>
```

**Note**: Stripe is the primary payment processor. You must configure Stripe to enable license validation.

## Security Best Practices

1. **Never commit API keys to git** - Always use Railway's environment variables
2. **Use strong, randomly generated keys** - Minimum 32 characters
3. **Rotate keys periodically** - Update keys every 90 days
4. **Use different keys for different environments** - Separate dev/staging/prod keys
5. **Restrict key access** - Only give access to team members who need it

## Verification

After setting environment variables, verify they're working:

1. **Check deployment logs** - Look for successful startup
2. **Test license API endpoint**:
   ```bash
   curl -H "Authorization: Bearer YOUR_API_KEY" \
     https://your-app.railway.app/api/v1/licenses/TEST-KEY/validate
   ```
3. **Check health endpoint**:
   ```bash
   curl https://your-app.railway.app/actuator/health
   ```

## Troubleshooting

### Problem: License API returns 401 Unauthorized

**Solution**: 
- Verify `LICENSE_API_SERVER_API_KEY` is set correctly
- Check that the Authorization header uses `Bearer {key}` format
- Ensure the key matches exactly (no extra spaces)

### Problem: License validation not working

**Solution**:
- Check that `APIFY_VALIDATION_ENABLED` or `STRIPE_VALIDATION_ENABLED` is set to `true`
- Verify API tokens/keys are correct
- Check deployment logs for validation errors

### Problem: App starts but endpoints not accessible

**Solution**:
- Verify `SPRING_PROFILES_ACTIVE=mcp-streamable-http` is set
- Check that Railway's `PORT` variable is being used (auto-provided)
- Review deployment logs for startup errors

## References

- [Railway Environment Variables Documentation](https://docs.railway.com/develop/variables)
- [Railway Config as Code](https://docs.railway.com/guides/config-as-code)
- [License API Implementation Guide](../strategy/LICENSE_API_IMPLEMENTATION.md)


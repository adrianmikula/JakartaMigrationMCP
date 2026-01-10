# Security Guide

This document outlines security best practices for the Jakarta Migration MCP Server.

## API Keys and Secrets

### Never Commit Secrets

**❌ NEVER commit the following to source control:**
- Apify API tokens
- Stripe secret keys
- License keys
- Any other API credentials

### Secure Storage

**✅ DO:**
- Store secrets in `.env` file (gitignored)
- Use environment variables in Apify Actor settings
- Use example files (`.example`) for documentation
- Rotate keys regularly

### Files That May Contain Secrets

The following files are gitignored and should not be committed:
- `.env` - Environment variables (contains secrets)
- `CURSOR_MCP_CONFIG_APIFY.json` - May contain API tokens
- `CURSOR_MCP_CONFIG_SSE.json` - May contain API tokens
- `CURSOR_MCP_CONFIG.json` - May contain API tokens

### Example Files

Example files (with `.example` extension) are safe to commit:
- `env.example` - Template for environment variables
- `CURSOR_MCP_CONFIG_APIFY.json.example` - Template for Cursor config
- `CURSOR_MCP_CONFIG.json.example` - Template for Cursor config

## Stripe API Key Security

The Stripe secret key is handled securely:

1. **Never compiled into code** - The key is only read at runtime
2. **Environment variable only** - Set via `STRIPE_SECRET_KEY` environment variable
3. **Not in source code** - Never hardcoded in Java files
4. **Not in JAR** - The key is not embedded in the compiled JAR

### For Local Users

Users running the MCP server locally can set the Stripe secret key via:
- `.env` file (recommended)
- Environment variable: `export STRIPE_SECRET_KEY=sk_test_...`
- System environment variables

### For Apify Deployment

Set `STRIPE_SECRET_KEY` as an environment variable in Apify Actor settings. The key is injected at runtime by Apify and never stored in the Docker image.

## Apify API Token Security

The Apify API token is used for:
- Validating user license keys
- Making API calls to Apify

### Storage

- **Local:** Store in `.env` file
- **Apify:** Set as environment variable in Actor settings
- **Never:** Hardcode in source code or config files

## Reporting Security Issues

If you discover a security vulnerability, please:
1. **DO NOT** create a public issue
2. Email security concerns to the maintainer
3. Include details about the vulnerability
4. Allow time for the issue to be addressed before public disclosure

## Best Practices

1. **Use test keys for development** - Use `sk_test_...` for Stripe, test tokens for Apify
2. **Rotate keys regularly** - Change API keys every 90 days
3. **Use different keys per environment** - Separate dev and production keys
4. **Monitor usage** - Check API logs for suspicious activity
5. **Set spending limits** - Use `ACTOR_MAX_TOTAL_CHARGE_USD` in Apify

## Related Documentation

- [Environment Variables Setup](docs/setup/ENVIRONMENT_VARIABLES.md)
- [Apify License Setup](docs/setup/APIFY_LICENSE_SETUP.md)
- [Stripe License Setup](docs/improvements/STRIPE_LICENSE_SETUP.md)


# NPM Installation Configuration Guide

This guide explains how to configure the Jakarta Migration MCP Server when installed via npm.

## Configuration File Location

The npm wrapper loads configuration from a JSON file in a standard user settings directory:

**Windows:**
```
%USERPROFILE%\.mcp-settings\jakarta-migration-license.json
```

**Linux/Mac:**
```
~/.mcp-settings/jakarta-migration-license.json
```

## Quick Setup

### Step 1: Create Settings Directory

**Windows (PowerShell):**
```powershell
New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.mcp-settings"
```

**Linux/Mac:**
```bash
mkdir -p ~/.mcp-settings
```

### Step 2: Create Configuration File

Copy the example file to the settings directory:

**Windows:**
```powershell
Copy-Item jakarta-migration-license.json.example "$env:USERPROFILE\.mcp-settings\jakarta-migration-license.json"
```

**Linux/Mac:**
```bash
cp jakarta-migration-license.json.example ~/.mcp-settings/jakarta-migration-license.json
```

### Step 3: Edit Configuration File

Open the file and fill in your values:

```json
{
  "environment": {
    "JAKARTA_MCP_LICENSE_KEY": "sub_your_stripe_subscription_id",
    "STRIPE_SECRET_KEY": "sk_test_your_stripe_secret_key",
    "STRIPE_VALIDATION_ENABLED": "false",
    "APIFY_API_TOKEN": "your_apify_api_token",
    "APIFY_VALIDATION_ENABLED": "true"
  }
}
```

## Configuration File Format

The configuration file is a JSON object with an `environment` section that maps to environment variables:

```json
{
  "environment": {
    "JAKARTA_MCP_LICENSE_KEY": "your_license_key",
    "STRIPE_SECRET_KEY": "sk_test_...",
    "STRIPE_VALIDATION_ENABLED": "false",
    "APIFY_API_TOKEN": "your_apify_token",
    "APIFY_VALIDATION_ENABLED": "true"
  }
}
```

### Available Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `JAKARTA_MCP_LICENSE_KEY` | Your license key (Stripe subscription ID, Apify token, or custom key) | No |
| `STRIPE_SECRET_KEY` | Stripe secret key for license validation | No (only if using Stripe) |
| `STRIPE_VALIDATION_ENABLED` | Enable Stripe license validation | No (default: false) |
| `APIFY_API_TOKEN` | Apify API token | No (only if using Apify) |
| `APIFY_VALIDATION_ENABLED` | Enable Apify license validation | No (default: true) |
| `JAKARTA_MCP_PURCHASE_URL` | Stripe payment link | No (has default) |

## Environment Variable Precedence

Environment variables are loaded in this order (later values override earlier ones):

1. **System environment variables** (highest priority)
2. **Configuration file** (`jakarta-migration-license.json`)
3. **Application defaults** (lowest priority)

This means:
- System environment variables always take precedence
- Configuration file is used as fallback
- Useful for corporate environments where system env vars may be restricted

## Corporate/Enterprise Setup

For corporate environments, IT administrators can:

1. **Create the settings directory** in a standard location
2. **Distribute the configuration file** via:
   - Group Policy (Windows)
   - Configuration management tools (Ansible, Puppet, etc.)
   - Manual distribution
3. **Set file permissions** to restrict access:
   ```bash
   chmod 600 ~/.mcp-settings/jakarta-migration-license.json
   ```

### Windows Group Policy Example

Create a startup script that copies the configuration file:

```powershell
# Copy-Item "\\server\share\jakarta-migration-license.json" "$env:USERPROFILE\.mcp-settings\jakarta-migration-license.json"
```

### Linux/Mac Deployment

Use configuration management:

```bash
# Ansible example
- name: Create MCP settings directory
  file:
    path: "{{ ansible_env.HOME }}/.mcp-settings"
    state: directory
    mode: '0700'

- name: Copy configuration file
  copy:
    src: jakarta-migration-license.json
    dest: "{{ ansible_env.HOME }}/.mcp-settings/jakarta-migration-license.json"
    mode: '0600'
```

## Security Best Practices

### File Permissions

**Linux/Mac:**
```bash
# Restrict access to owner only
chmod 600 ~/.mcp-settings/jakarta-migration-license.json
```

**Windows:**
- Right-click file → Properties → Security
- Remove access for all users except your account
- Or use `icacls` command:
  ```powershell
  icacls "$env:USERPROFILE\.mcp-settings\jakarta-migration-license.json" /inheritance:r /grant:r "$env:USERNAME:(R)"
  ```

### File Location

The configuration file is stored in:
- User's home directory (not shared)
- Hidden directory (`.mcp-settings`)
- Not in the npm package directory
- Not accessible to other users (with proper permissions)

### Secrets Management

**For Corporate Environments:**
- Store secrets in enterprise secret management (HashiCorp Vault, AWS Secrets Manager, etc.)
- Use scripts to populate the configuration file from secrets
- Rotate keys regularly
- Audit access to the configuration file

## Troubleshooting

### Configuration File Not Found

**Problem:** Configuration file is not being loaded.

**Solutions:**
1. Verify file exists: `ls ~/.mcp-settings/jakarta-migration-license.json` (Linux/Mac) or `dir %USERPROFILE%\.mcp-settings\jakarta-migration-license.json` (Windows)
2. Check file permissions
3. Verify JSON syntax is valid
4. Check npm wrapper logs for error messages

### Invalid JSON

**Problem:** Error message about invalid JSON.

**Solutions:**
1. Validate JSON syntax using a JSON validator
2. Check for trailing commas
3. Ensure all strings are properly quoted
4. Use the example file as a template

### Environment Variables Not Working

**Problem:** Environment variables from config file are not being used.

**Solutions:**
1. Check if system environment variables are overriding (they take precedence)
2. Verify the `environment` section exists in the JSON file
3. Restart the MCP client after changing the configuration file
4. Check npm wrapper logs for configuration loading messages

## Example Configuration Files

### Minimal Configuration (License Key Only)

```json
{
  "environment": {
    "JAKARTA_MCP_LICENSE_KEY": "sub_1234567890abcdef"
  }
}
```

### Full Configuration (Stripe + Apify)

```json
{
  "environment": {
    "JAKARTA_MCP_LICENSE_KEY": "sub_1234567890abcdef",
    "STRIPE_SECRET_KEY": "sk_test_51AbCdEf...",
    "STRIPE_VALIDATION_ENABLED": "true",
    "STRIPE_PRODUCT_ID_PREMIUM": "prod_ABC123",
    "STRIPE_PRODUCT_ID_ENTERPRISE": "prod_DEF456",
    "APIFY_API_TOKEN": "apify_api_xyz123...",
    "APIFY_VALIDATION_ENABLED": "true"
  }
}
```

## Related Documentation

- [Environment Variables Setup](ENVIRONMENT_VARIABLES.md)
- [Feature Flags Setup](FEATURE_FLAGS_SETUP.md)
- [Stripe License Setup](../improvements/STRIPE_LICENSE_SETUP.md)
- [Apify License Setup](APIFY_LICENSE_SETUP.md)


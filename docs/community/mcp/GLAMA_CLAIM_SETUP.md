# Glama MCP Server Claim Setup

## Overview

This document explains how to claim ownership of the Jakarta Migration MCP Server on [Glama](https://glama.ai), an MCP server directory and marketplace.

## What is glama.json?

The `glama.json` file in the project root allows you to claim ownership of your MCP server listing on Glama. This gives you control over:

- ✅ Update server name, description, and attributes
- ✅ Configure Docker image
- ✅ Access usage reports
- ✅ Receive review notifications

Reference: [What is glama.json?](https://glama.ai/blog/2025-07-08-what-is-glamajson)

## Current Configuration

**File**: `glama.json`

```json
{
  "$schema": "https://glama.ai/mcp/schemas/server.json",
  "maintainers": [
    "adrianmikula"
  ]
}
```

## Setup Steps

### 1. Verify GitHub Username

**Important**: The `maintainers` array must contain your **GitHub username** (not email or display name).

- Current value: `adrianmikula`
- If this is incorrect, update `glama.json` with your actual GitHub username
- You can find your username at: `https://github.com/YOUR_USERNAME`

### 2. Commit and Push to GitHub

```bash
git add glama.json
git commit -m "Add glama.json for MCP server ownership claim"
git push
```

### 3. Claim Ownership on Glama

1. Go to [Glama MCP Directory](https://glama.ai)
2. Find your Jakarta Migration MCP Server listing
3. Click **"Claim"** or **"Claim Ownership"**
4. Authenticate with GitHub (if repository is under personal account)
   - OR provide the repository URL if under an organization

### 4. Sync Changes

After adding or updating `glama.json`:
- Go through the **Claim** flow again on Glama
- This triggers Glama to pick up the latest changes

## Alternative Claim Method

If your GitHub repository is under your **personal account**:
- Simply authenticate with GitHub on Glama
- This automatically associates your account with the server
- **Note**: This doesn't work for organization repositories - you must use `glama.json`

## Troubleshooting

### Can't Claim Ownership?

Check the following:

1. ✅ **GitHub Username Correct?**
   - Ensure `maintainers` array contains your exact GitHub username
   - Check: `https://github.com/YOUR_USERNAME`

2. ✅ **JSON Syntax Valid?**
   - Validate JSON: `python -m json.tool glama.json`
   - Ensure no trailing commas or syntax errors

3. ✅ **File in Root Directory?**
   - `glama.json` must be in the repository root
   - Not in subdirectories like `docs/` or `src/`

4. ✅ **Committed and Pushed?**
   - File must be committed to the repository
   - Changes must be pushed to GitHub

5. ✅ **Repository Public?**
   - Glama needs access to read the file
   - Private repositories may not work

## Additional Configuration (Optional)

The `glama.json` file can include additional fields for server configuration:

```json
{
  "$schema": "https://glama.ai/mcp/schemas/server.json",
  "maintainers": ["adrianmikula"],
  "name": "Jakarta Migration MCP Server",
  "description": "Tools for analyzing and migrating Java applications from Java EE 8 to Jakarta EE 9+",
  "docker": {
    "image": "your-docker-image"
  }
}
```

Check the [Glama schema](https://glama.ai/mcp/schemas/server.json) for all available fields.

## Next Steps

1. ✅ `glama.json` file created
2. ⏳ Verify GitHub username is correct
3. ⏳ Commit and push to GitHub
4. ⏳ Claim ownership on Glama
5. ⏳ Update server listing with description, tags, etc.

## References

- [Glama Blog: What is glama.json?](https://glama.ai/blog/2025-07-08-what-is-glamajson)
- [Glama MCP Directory](https://glama.ai)
- [Glama Server Schema](https://glama.ai/mcp/schemas/server.json)


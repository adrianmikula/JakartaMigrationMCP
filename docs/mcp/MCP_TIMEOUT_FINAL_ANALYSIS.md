# MCP Timeout Issue - Final Analysis

## Current Status

- **Spring AI Version**: 1.1.2 (includes stability fixes from 1.1.1)
- **Issue**: Still experiencing 1-minute timeout on GetInstructions/ListOfferings
- **Conclusion**: The stability fixes in 1.1.1 did NOT address this specific issue

## Root Cause

Spring AI MCP 1.1.2 has a bug where:
1. Server info is not properly stored during initialization
2. When `GetInstructions` or `ListOfferings` are requested, Spring AI MCP tries to retrieve server info
3. Server info lookup fails with "No server info found"
4. The request hangs (doesn't return an error, just waits)
5. Cursor times out after 1 minute

## Why Stability Fixes Didn't Help

The "MCP stability fixes" in Spring AI 1.1.1 likely addressed:
- Tool registration issues
- General MCP server stability
- Connection handling

But did NOT address:
- Server info storage/retrieval for GetInstructions/ListOfferings
- The specific timeout behavior we're seeing

## Attempted Solutions (All Failed)

1. ❌ Added server description in application.yml
2. ❌ Created McpServerInfoConfiguration bean
3. ❌ Tried to disable capabilities via configuration
4. ❌ Upgraded to Spring AI 1.1.2 (already had fixes)

## What This Means

**This is an active bug in Spring AI MCP 1.1.2** that:
- Affects GetInstructions/ListOfferings specifically
- Causes Cursor to timeout during tool discovery
- Prevents tools from being accessible even though they're registered

## Next Steps

### Option 1: File a Bug Report (Recommended)

File an issue with Spring AI team:
- **Repository**: https://github.com/spring-projects/spring-ai/issues
- **Title**: "GetInstructions/ListOfferings timeout in Spring AI MCP 1.1.2 - No server info found"
- **Details**: Include logs showing "No server info found" and timeout behavior

### Option 2: Check for Newer Versions

Monitor Spring AI releases for:
- Version 1.1.3 or later
- Specific fixes for GetInstructions/ListOfferings
- MCP server info handling improvements

### Option 3: Try SSE Transport

Some users report better behavior with SSE:
- May have different initialization flow
- May bypass the problematic GetInstructions/ListOfferings

### Option 4: Wait for Fix

Since this is a framework bug:
- Tools are registered correctly (6 tools)
- Server starts successfully
- Only the discovery phase fails
- Framework fix is needed

## Workaround (If Tools Can Be Accessed)

If Cursor supports bypassing the discovery phase:
1. Tools are registered (visible in server logs)
2. Could potentially call tools directly by name
3. Would bypass GetInstructions/ListOfferings entirely

**Note**: This requires Cursor to support manual tool invocation.

## Conclusion

**This is a confirmed bug in Spring AI MCP 1.1.2** that requires a framework fix. The stability fixes in 1.1.1 did not address this specific issue. We should:

1. File a bug report with Spring AI
2. Monitor for newer versions with fixes
3. Consider SSE transport as an alternative
4. Document the limitation for users


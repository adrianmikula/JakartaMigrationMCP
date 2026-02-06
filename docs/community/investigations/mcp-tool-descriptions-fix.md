# MCP Tool Descriptions Fix

## Date: 2026-01-07

## Issue

Tool descriptions were being extracted correctly from `@McpTool` annotations, but the MCP protocol `required` array was missing from the `inputSchema`. This could cause MCP clients to not properly understand which parameters are required vs optional.

## Root Cause

The `getToolsFromClass` method in both `McpStreamableHttpController` and `McpSseController` was:
- ✅ Correctly extracting tool descriptions from `@McpTool` annotations
- ✅ Correctly extracting parameter descriptions from `@McpToolParam` annotations
- ❌ **NOT** building the `required` array in the `inputSchema`

## Fix Applied

Updated both controllers to:
1. Build a list of required parameter names while iterating through parameters
2. Add the `required` array to the `inputSchema` if there are any required parameters

### Changes Made

**Files Modified:**
- `src/main/java/adrianmikula/jakartamigration/mcp/McpStreamableHttpController.java`
- `src/main/java/adrianmikula/jakartamigration/mcp/McpSseController.java`

**Change Details:**
```java
// Before:
inputSchema.put("properties", properties);
toolMap.put("inputSchema", inputSchema);

// After:
List<String> required = new ArrayList<>();
// ... build required list while processing parameters ...
inputSchema.put("properties", properties);
if (!required.isEmpty()) {
    inputSchema.put("required", required);
}
toolMap.put("inputSchema", inputSchema);
```

## MCP Protocol Compliance

The MCP protocol specification requires that tool `inputSchema` objects include:
- ✅ `type: "object"` - Already present
- ✅ `properties: { ... }` - Already present
- ✅ `required: ["param1", "param2"]` - **NOW ADDED**

## Expected Behavior

After this fix, MCP clients should receive tool definitions like:

```json
{
  "name": "analyzeJakartaReadiness",
  "description": "Analyzes a Java project for Jakarta migration readiness...",
  "inputSchema": {
    "type": "object",
    "properties": {
      "projectPath": {
        "type": "string",
        "description": "Path to the project root directory"
      }
    },
    "required": ["projectPath"]
  }
}
```

## Verification

To verify the fix:
1. Rebuild and redeploy the Actor
2. Check MCP tools list response - should include `required` array
3. Verify MCP clients can properly identify required vs optional parameters

## Impact

- **Positive**: MCP clients will now correctly understand which parameters are required
- **No Breaking Changes**: This is an additive change that improves compliance with MCP spec
- **Backward Compatible**: MCP clients that don't check `required` will continue to work


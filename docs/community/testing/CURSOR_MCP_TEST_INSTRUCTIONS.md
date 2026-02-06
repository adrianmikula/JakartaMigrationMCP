# Testing MCP Tools in Cursor - Quick Guide

## Current Status

✅ **Project Built**: JAR file exists and is ready  
✅ **MCP Server Starts**: Server process runs successfully  
⚠️ **Tools Not Registered**: Tools don't have `@McpTool` annotations (not available in Spring AI 1.1.2)  

## Quick Test Steps

### 1. Verify Cursor MCP Configuration

The MCP server needs to be configured in Cursor's settings. Check if the configuration file exists:

**Windows Location**: 
```
%APPDATA%\Cursor\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json
```

**Or via Cursor Settings**:
- Open Cursor Settings (`Ctrl+,`)
- Navigate to **Features** → **MCP**
- Add the configuration from `CURSOR_MCP_CONFIG.json`

### 2. Restart Cursor

**Critical**: You must completely restart Cursor for MCP servers to initialize:
1. Close ALL Cursor windows
2. Wait a few seconds
3. Restart Cursor

### 3. Test Tool Discovery

Once Cursor restarts, try asking:

```
What Jakarta migration tools are available?
```

Or:

```
@jakarta-migration list tools
```

### 4. Test Individual Tools

If tools are visible, try:

```
Analyze Jakarta readiness for examples/demo-spring-javax-validation-example-master/demo-spring-javax-validation-example-master
```

```
Detect blockers for examples/javax-servlet-examples-master/javax-servlet-examples-master
```

```
Create a migration plan for examples/JavaxEmail-main/JavaxEmail-main
```

## Expected Results

### If Tools Are Visible ✅
- Cursor can list the 5 Jakarta migration tools
- Tools can be invoked via natural language
- Tools return JSON responses

### If Tools Are NOT Visible ❌
This means tools aren't registered with the MCP server. Possible reasons:
1. Tools don't have `@McpTool` annotations (current issue)
2. Annotation scanner not working
3. Tools need manual registration via `ToolCallbackProvider`

## Troubleshooting

### Check Cursor Logs
1. Open Cursor
2. Go to Help → Toggle Developer Tools
3. Check Console for MCP-related errors
4. Look for connection messages

### Verify Server Can Start
Run the test script:
```powershell
.\scripts\test-mcp-server-startup.ps1
```

### Check Configuration
Verify the JAR path in Cursor config matches:
```
E:/Source/JakartaMigrationMCP/build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar
```

## Next Steps if Tools Don't Appear

Since `@McpTool` annotations aren't available, we need to:

1. **Implement Manual Registration**: Create a `ToolCallbackProvider` bean to register tools
2. **Verify Spring AI MCP API**: Check what registration methods are available in 1.1.2
3. **Test Alternative Approaches**: Try different ways to expose tools

---

**Ready to test?** Restart Cursor and try asking about Jakarta migration tools!


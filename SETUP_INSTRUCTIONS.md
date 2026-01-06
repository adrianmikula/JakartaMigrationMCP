# Quick Setup: Install Jakarta Migration MCP in Cursor

## Step 1: Copy Configuration to Cursor

1. **Open Cursor Settings**
   - Press `Ctrl+,` (Windows) or `Cmd+,` (Mac)
   - Or: File → Preferences → Settings

2. **Navigate to MCP Settings**
   - Search for "MCP" in settings
   - Or go to: Features → MCP

3. **Add the Jakarta Migration MCP Server**

   Copy the configuration from `CURSOR_MCP_CONFIG.json` in this project, or use this:

   ```json
   {
     "mcpServers": {
       "jakarta-migration": {
         "command": "java",
         "args": [
           "-jar",
           "E:/Source/JakartaMigrationMCP/build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar",
           "--spring.main.web-application-type=none",
           "--spring.profiles.active=mcp-stdio",
           "--spring.ai.mcp.server.transport=stdio"
         ],
         "env": {}
       }
     }
   }
   ```

   **Important**: Replace `E:/Source/JakartaMigrationMCP` with your actual project path.

4. **Toggle the server ON** (if there's a toggle switch)

5. **Restart Cursor**
   - Close Cursor completely
   - Reopen Cursor
   - MCP servers only load on startup

## Step 2: Verify Installation

After restarting, the MCP server should be running. You can verify by asking:
- "What MCP tools are available?"
- Or try using one of the tools directly

## Step 3: Test the Tools

Once configured, you can test the tools on the example projects in the `examples/` directory.



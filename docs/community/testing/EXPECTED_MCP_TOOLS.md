# Expected MCP Tools

## Tools That Should Be Exposed

Based on `JakartaMigrationTools.java`, the following 6 tools should be publicly exposed:

### 1. analyzeJakartaReadiness
- **Name**: `analyzeJakartaReadiness` (exact case)
- **Description**: "Analyzes a Java project for Jakarta migration readiness. Returns a JSON report with readiness score, blockers, and recommendations."
- **Parameters**:
  - `projectPath` (String, required): Path to the project root directory

### 2. detectBlockers
- **Name**: `detectBlockers` (exact case)
- **Description**: "Detects blockers that prevent Jakarta migration. Returns a JSON list of blockers with types, reasons, and mitigation strategies."
- **Parameters**:
  - `projectPath` (String, required): Path to the project root directory

### 3. recommendVersions
- **Name**: `recommendVersions` (exact case)
- **Description**: "Recommends Jakarta-compatible versions for project dependencies. Returns a JSON list of version recommendations with migration paths and compatibility scores."
- **Parameters**:
  - `projectPath` (String, required): Path to the project root directory

### 4. createMigrationPlan
- **Name**: `createMigrationPlan` (exact case)
- **Description**: "Creates a comprehensive migration plan for Jakarta migration. Returns a JSON plan with phases, estimated duration, and risk assessment."
- **Parameters**:
  - `projectPath` (String, required): Path to the project root directory

### 5. analyzeMigrationImpact
- **Name**: `analyzeMigrationImpact` (exact case)
- **Description**: "Analyzes full migration impact combining dependency analysis and source code scanning. Returns a comprehensive summary with file counts, import counts, blockers, and estimated effort."
- **Parameters**:
  - `projectPath` (String, required): Path to the project root directory

### 6. verifyRuntime
- **Name**: `verifyRuntime` (exact case)
- **Description**: "Verifies runtime execution of a migrated Jakarta application. Returns a JSON result with execution status, errors, and metrics."
- **Parameters**:
  - `jarPath` (String, required): Path to the JAR file to execute
  - `timeoutSeconds` (Integer, optional): Optional timeout in seconds (default: 30)

## Verification Checklist

- [ ] All 6 tools appear in MCP tools list
- [ ] Tool names match exactly (case-sensitive)
- [ ] Tool descriptions are present
- [ ] Tool parameters are correctly defined
- [ ] Required vs optional parameters are marked correctly

## If Tools Are Missing

Check:
1. `.actor/actor.json` has `usesStandbyMode: true`
2. `.actor/actor.json` has `webServerMcpPath: "/mcp/sse"` or `/mcp`
3. Actor is running and in standby mode
4. `@McpTool` annotations are present in `JakartaMigrationTools.java`
5. MCP server started successfully (check logs)


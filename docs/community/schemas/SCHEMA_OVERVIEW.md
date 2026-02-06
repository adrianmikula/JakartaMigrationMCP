# Jakarta Migration MCP Server - Schema Overview

This document provides an overview of the JSON schemas for the Jakarta Migration MCP Server.

## Quick Reference

### Input Schemas

All tools accept input as JSON objects with the following structure:

| Tool | Required Fields | Optional Fields |
|------|----------------|-----------------|
| `analyzeJakartaReadiness` | `projectPath` | - |
| `detectBlockers` | `projectPath` | - |
| `recommendVersions` | `projectPath` | - |
| `createMigrationPlan` | `projectPath` | - |
| `verifyRuntime` | `jarPath` | `timeoutSeconds` |

### Output Schemas

All tools return JSON objects with a `status` field:

- **`success`** - Operation completed successfully
- **`error`** - An error occurred during execution
- **`upgrade_required`** - Premium feature requires license upgrade

## Input Schema Details

### Common Input Pattern

Most tools follow this pattern:

```json
{
  "projectPath": "/path/to/project"
}
```

**Field Descriptions:**
- `projectPath` (string, required): Absolute or relative path to project root directory
  - Must be a valid directory path
  - Supports both Unix (`/path/to/project`) and Windows (`C:\path\to\project`) formats

### Special Input: verifyRuntime

```json
{
  "jarPath": "/path/to/app.jar",
  "timeoutSeconds": 30
}
```

**Field Descriptions:**
- `jarPath` (string, required): Path to JAR file to execute
  - Must end with `.jar` extension
  - File must exist and be readable
- `timeoutSeconds` (integer, optional): Execution timeout in seconds
  - Default: 30
  - Range: 1-3600

## Output Schema Details

### Success Responses

Each tool has a specific success response structure:

1. **`analyzeJakartaReadiness`** - Returns readiness score and metrics
2. **`detectBlockers`** - Returns list of blockers with details
3. **`recommendVersions`** - Returns list of version recommendations
4. **`createMigrationPlan`** - Returns migration plan with phases
5. **`verifyRuntime`** - Returns verification status and metrics

### Error Response (All Tools)

```json
{
  "status": "error",
  "message": "Error description"
}
```

### Upgrade Required Response (Premium Tools)

```json
{
  "status": "upgrade_required",
  "message": "Feature requires PREMIUM license",
  "upgradeMessage": "Upgrade instructions",
  "requiredTier": "PREMIUM",
  "currentTier": "COMMUNITY"
}
```

## Schema Files

1. **`mcp-input-schemas.json`** - Complete input schemas with validation rules
2. **`mcp-output-schemas.json`** - Complete output schemas with all response types
3. **`example-requests-responses.json`** - Example request/response pairs

## Validation

### Using JSON Schema Validators

**JavaScript (Ajv):**
```javascript
const Ajv = require('ajv');
const ajv = new Ajv();
const schema = require('./mcp-input-schemas.json');

const validate = ajv.compile(schema.toolInputs.analyzeJakartaReadiness);
const valid = validate({ projectPath: '/path/to/project' });
```

**Python (jsonschema):**
```python
import jsonschema
import json

with open('mcp-input-schemas.json') as f:
    schema = json.load(f)

jsonschema.validate(
    {"projectPath": "/path/to/project"},
    schema['toolInputs']['analyzeJakartaReadiness']
)
```

**Java (everit-org/json-schema):**
```java
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

Schema schema = SchemaLoader.load(new JSONObject(schemaJson));
schema.validate(new JSONObject(inputJson));
```

## Type Generation

### TypeScript

```bash
npm install -g json-schema-to-typescript
json2ts -i mcp-input-schemas.json -o types.ts
```

### Java

Use tools like:
- [jsonschema2pojo](https://github.com/joelittlejohn/jsonschema2pojo)
- [quicktype](https://quicktype.io/)

## Integration Examples

### MCP Client Integration

```typescript
// TypeScript example
interface AnalyzeJakartaReadinessInput {
  projectPath: string;
}

interface ReadinessResponse {
  status: 'success' | 'error';
  readinessScore: number;
  readinessMessage: string;
  totalDependencies: number;
  blockers: number;
  recommendations: number;
  riskScore: number;
  riskFactors: string[];
}

async function analyzeReadiness(
  input: AnalyzeJakartaReadinessInput
): Promise<ReadinessResponse> {
  // Call MCP tool
  const response = await mcpClient.callTool('analyzeJakartaReadiness', input);
  return JSON.parse(response) as ReadinessResponse;
}
```

### API Gateway Integration

Use schemas to:
1. Validate incoming requests
2. Generate API documentation
3. Create mock responses for testing
4. Generate client SDKs

## Best Practices

1. **Always Validate Input** - Use schemas to validate before calling tools
2. **Handle All Response Types** - Check for `error` and `upgrade_required` status
3. **Type Safety** - Generate types from schemas for type-safe integration
4. **Documentation** - Use schemas to auto-generate API documentation
5. **Testing** - Use example requests/responses for integration tests

## Schema Versioning

- **Current Version**: Draft 7 (JSON Schema Draft 7)
- **Schema ID**: `https://jakarta-migration-mcp.com/schemas/`
- **Compatibility**: Backward compatible within major versions

## Support

For schema questions or issues:
- Review example requests/responses
- Check tool implementation in `JakartaMigrationTools.java`
- Validate against schemas using JSON Schema validators


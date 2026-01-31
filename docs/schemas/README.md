# Jakarta Migration MCP Server - JSON Schemas

This directory contains JSON Schema definitions for the Jakarta Migration MCP Server's input and output formats.

## Files

### MCP Client Schemas (JSON Schema Draft 7)

- **`mcp-input-schemas.json`** - Input parameter schemas for all MCP tools (for MCP client validation)
- **`mcp-output-schemas.json`** - Output response schemas for all MCP tools (for MCP client validation)
- **`example-requests-responses.json`** - Example request/response pairs

## Usage

These schemas can be used for:

1. **API Documentation** - Generate OpenAPI/Swagger documentation
2. **Validation** - Validate input/output in client applications
3. **Type Generation** - Generate TypeScript/Java types from schemas
4. **Testing** - Validate test data against schemas
5. **Integration** - Understand expected formats for MCP client integration

## Schema Structure

### Input Schemas

Each tool has a corresponding input schema definition:

```json
{
  "toolInputs": {
    "analyzeJakartaReadiness": {
      "type": "object",
      "required": ["projectPath"],
      "properties": {
        "projectPath": {
          "type": "string",
          "description": "Path to the project root directory"
        }
      }
    }
  }
}
```

### Output Schemas

Each tool has a corresponding output schema that may include:

- **Success Response** - Normal operation result
- **Error Response** - Error occurred during execution
- **Upgrade Required Response** - Feature requires a higher tier (optional; default tier is ENTERPRISE so all features are available)

## Tools

### Free Tools (Community Tier)

1. **`analyzeJakartaReadiness`** - Analyze project readiness
2. **`detectBlockers`** - Detect migration blockers
3. **`recommendVersions`** - Recommend Jakarta-compatible versions
4. **`check_env`** - Check environment variable status

### All Tools (Default Tier: ENTERPRISE)

All tools are available by default. Tier-based upgrade responses are optional and used only if a lower tier is configured.

1. **`createMigrationPlan`** - Create comprehensive migration plan
2. **`analyzeMigrationImpact`** - Full migration impact analysis
3. **`verifyRuntime`** - Verify runtime execution of migrated application

## Upgrade Required Response (Optional)

If a tool is gated by tier and the current tier is lower than required, the server may return:

```json
{
  "status": "upgrade_required",
  "message": "The 'createMigrationPlan' tool requires a PREMIUM tier...",
  "featureName": "One-click refactoring",
  "featureDescription": "Execute complete Jakarta migration refactoring...",
  "currentTier": "COMMUNITY",
  "requiredTier": "PREMIUM",
  "upgradeMessage": "Upgrade to PREMIUM to use this feature."
}
```

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
interface UpgradeRequiredResponse {
  status: 'upgrade_required';
  message: string;
  featureName: string;
  featureDescription: string;
  currentTier: 'COMMUNITY' | 'PREMIUM' | 'ENTERPRISE';
  requiredTier: 'PREMIUM' | 'ENTERPRISE';
  upgradeMessage: string;
}

function handleResponse(response: any) {
  if (response.status === 'upgrade_required') {
    const upgrade = response as UpgradeRequiredResponse;
    console.log(`Upgrade required: ${upgrade.featureName}`);
  }
}
```

## Documentation

- [Schema Overview](SCHEMA_OVERVIEW.md) - Complete schema documentation
- [Schema Update Summary](SCHEMA_UPDATE_SUMMARY.md) - Historical changes
- [Schema Comparison](SCHEMA_COMPARISON.md) - Schema comparison

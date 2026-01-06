# Jakarta Migration MCP Server - JSON Schemas

This directory contains JSON Schema definitions for the Jakarta Migration MCP Server's input and output formats.

## Files

- **`mcp-input-schemas.json`** - Input parameter schemas for all MCP tools
- **`mcp-output-schemas.json`** - Output response schemas for all MCP tools

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
- **Upgrade Required Response** - Premium feature requires license upgrade

## Tools

### Free Tools (Community Tier)

1. **`analyzeJakartaReadiness`** - Analyze project readiness
2. **`detectBlockers`** - Detect migration blockers
3. **`recommendVersions`** - Recommend compatible versions

### Premium Tools (Require License)

4. **`createMigrationPlan`** - Create migration plan
5. **`verifyRuntime`** - Verify runtime execution

## Example Usage

### Validate Input

```javascript
const Ajv = require('ajv');
const ajv = new Ajv();
const inputSchema = require('./mcp-input-schemas.json');

const validate = ajv.compile(inputSchema.toolInputs.analyzeJakartaReadiness);
const valid = validate({ projectPath: '/path/to/project' });

if (!valid) {
  console.error(validate.errors);
}
```

### Validate Output

```javascript
const outputSchema = require('./mcp-output-schemas.json');
const validate = ajv.compile(outputSchema.toolOutputs.analyzeJakartaReadiness);
const valid = validate(response);

if (!valid) {
  console.error(validate.errors);
}
```

## Schema Validation

All schemas follow JSON Schema Draft 7 specification:

- **Type**: `http://json-schema.org/draft-07/schema#`
- **Validation**: Full validation with required fields, types, and constraints
- **Examples**: Included in schema definitions

## Integration with MCP Clients

MCP clients can use these schemas to:

1. **Generate UI Forms** - Create input forms from schemas
2. **Validate Requests** - Validate tool calls before execution
3. **Type Safety** - Generate type definitions for type-safe clients
4. **Documentation** - Auto-generate API documentation

## References

- [JSON Schema Specification](https://json-schema.org/)
- [MCP Protocol Specification](https://modelcontextprotocol.io)
- [Spring AI MCP Server](https://docs.spring.io/spring-ai/reference/)


# Schema Comparison: MCP vs Apify

This document explains the difference between MCP client schemas and Apify Actor schemas.

## Two Different Schema Types

The Jakarta Migration MCP Server uses **two different schema formats** for different purposes:

1. **MCP Client Schemas** - Standard JSON Schema for MCP tool validation
2. **Apify Actor Schemas** - Apify-specific format for Actor UI and output display

## MCP Client Schemas

**Location**: `docs/schemas/`

**Purpose**: Validate MCP tool inputs/outputs in MCP clients

**Format**: JSON Schema Draft 7

**Files**:
- `mcp-input-schemas.json` - Tool input validation
- `mcp-output-schemas.json` - Tool output validation
- `example-requests-responses.json` - Examples

**Use Cases**:
- MCP client input validation
- Type generation (TypeScript, Java, Python)
- API documentation
- Integration testing

**Example**:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "definitions": {
    "analyzeJakartaReadinessInput": {
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

## Apify Actor Schemas

**Location**: `.actor/`

**Purpose**: Generate Apify Console UI and define output display

**Format**: Apify-specific schema format

**Files**:
- `input_schema.json` - Actor input UI generation
- `output_schema.json` - Output display configuration
- `actor.json` - Actor metadata

**Use Cases**:
- Auto-generate input forms in Apify Console
- Define output display in Apify Console
- Configure Actor deployment

**Example** (Input Schema):
```json
{
  "title": "Jakarta Migration MCP Server Input",
  "type": "object",
  "schemaVersion": 1,
  "properties": {
    "mcpTransport": {
      "title": "MCP Transport Mode",
      "type": "string",
      "enum": ["stdio", "sse"],
      "default": "sse"
    }
  }
}
```

**Example** (Output Schema):
```json
{
  "actorOutputSchemaVersion": 1,
  "title": "Jakarta Migration MCP Server Output",
  "properties": {
    "mcpEndpoint": {
      "type": "string",
      "title": "MCP Server Endpoint",
      "template": "{{run.containerUrl}}mcp/sse"
    }
  }
}
```

## Key Differences

| Aspect | MCP Client Schemas | Apify Actor Schemas |
|--------|-------------------|---------------------|
| **Format** | JSON Schema Draft 7 | Apify-specific format |
| **Purpose** | Validate tool I/O | Generate UI & display output |
| **Structure** | Tool-specific | Unified Actor config |
| **Input Focus** | Tool parameters | Actor configuration |
| **Output Focus** | Data structure | Storage locations & URLs |
| **Used By** | MCP clients | Apify Console |
| **Location** | `docs/schemas/` | `.actor/` |

## When to Use Which

### Use MCP Client Schemas When:
- ✅ Building MCP client integrations
- ✅ Validating tool inputs/outputs
- ✅ Generating TypeScript/Java types
- ✅ Writing integration tests
- ✅ Creating API documentation

### Use Apify Actor Schemas When:
- ✅ Deploying to Apify platform
- ✅ Configuring Actor in Apify Console
- ✅ Defining Actor input UI
- ✅ Configuring output display
- ✅ Setting up Actor metadata

## Both Are Needed

For a complete deployment:

1. **MCP Client Schemas** - Enable MCP clients to validate and use tools
2. **Apify Actor Schemas** - Enable Apify Console to provide UI and display output

They serve different purposes and complement each other.

## References

- **MCP Schemas**: See [README.md](README.md) and [SCHEMA_OVERVIEW.md](SCHEMA_OVERVIEW.md)
- **Apify Schemas**: See [APIFY_SCHEMAS.md](APIFY_SCHEMAS.md)
- **Apify Documentation**: 
  - [Input Schema](https://docs.apify.com/platform/actors/development/actor-definition/input-schema)
  - [Output Schema](https://docs.apify.com/platform/actors/development/actor-definition/output-schema)


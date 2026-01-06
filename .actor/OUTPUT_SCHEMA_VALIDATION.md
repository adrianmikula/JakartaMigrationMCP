# Apify Output Schema Validation

This document validates the output schema against Apify's official specification.

## Apify Output Schema Specification

According to [Apify's Output Schema Documentation](https://docs.apify.com/platform/actors/development/actor-definition/output-schema):

### Required Top-Level Fields

- ✅ `actorOutputSchemaVersion` (integer, required) - Must be `1`
- ✅ `title` (string, required) - Title of the schema
- ✅ `properties` (object, required) - Output property definitions

### Optional Top-Level Fields

- ✅ `description` (string, optional) - Description of the schema
- ❌ `type` - **NOT REQUIRED** in output schema (removed)

### Property Object Requirements

Each property in `properties` must have:

- ✅ `type` (string, required) - Must be `"string"`
- ✅ `title` (string, required) - Display title for the output
- ✅ `template` (string, required) - Template string with variables

### Optional Property Fields

- ✅ `description` (string, optional) - Description of the output

## Template Variables

Apify supports the following template variables:

- ✅ `{{run.containerUrl}}` - URL of the web server running inside the run
- ✅ `{{links.apiDefaultDatasetUrl}}` - API URL of default dataset
- ✅ `{{links.apiDefaultKeyValueStoreUrl}}` - API URL of default key-value store
- ✅ `{{links.publicRunUrl}}` - Public run URL in Apify Console

## Current Schema Validation

### Top-Level Structure

```json
{
  "actorOutputSchemaVersion": 1,  ✅ Required, correct value
  "title": "...",                  ✅ Required, present
  "description": "...",            ✅ Optional, present
  "properties": { ... }            ✅ Required, present
}
```

### Properties Validation

#### 1. `mcpEndpoint` ✅
- ✅ `type: "string"` - Correct
- ✅ `title` - Present
- ✅ `description` - Present
- ✅ `template: "{{run.containerUrl}}/mcp/sse"` - Valid template variable, correct path format

#### 2. `healthCheck` ✅
- ✅ `type: "string"` - Correct
- ✅ `title` - Present
- ✅ `description` - Present
- ✅ `template: "{{run.containerUrl}}/actuator/health"` - Valid template variable, correct path format

#### 3. `metrics` ✅
- ✅ `type: "string"` - Correct
- ✅ `title` - Present
- ✅ `description` - Present
- ✅ `template: "{{run.containerUrl}}/actuator/metrics"` - Valid template variable, correct path format

#### 4. `info` ✅
- ✅ `type: "string"` - Correct
- ✅ `title` - Present
- ✅ `description` - Present
- ✅ `template: "{{run.containerUrl}}/actuator/info"` - Valid template variable, correct path format

## Template Path Format

### Fixed Issues

1. **Path Format**: Changed from `{{run.containerUrl}}mcp/sse` to `{{run.containerUrl}}/mcp/sse`
   - `run.containerUrl` typically ends with a trailing slash
   - Added leading slash to paths for proper URL construction
   - Ensures: `http://container-url/mcp/sse` instead of `http://container-urlmcp/sse`

2. **Removed `type: "object"`**: 
   - Not required in Apify output schema specification
   - Only `actorOutputSchemaVersion`, `title`, and `properties` are required at top level

## Compliance Status

✅ **FULLY COMPLIANT** with Apify's Output Schema Specification v1

### Validation Checklist

- ✅ `actorOutputSchemaVersion: 1` - Correct version
- ✅ `title` - Present and descriptive
- ✅ `description` - Present (optional but recommended)
- ✅ `properties` - Present with valid structure
- ✅ All properties have `type: "string"`
- ✅ All properties have `title`
- ✅ All properties have `template` with valid variables
- ✅ Template paths use correct format with leading slashes
- ✅ Template variables use `{{variable}}` syntax
- ✅ No invalid fields at top level

## References

- [Apify Output Schema Documentation](https://docs.apify.com/platform/actors/development/actor-definition/output-schema)
- [Apify Template Variables](https://docs.apify.com/platform/actors/development/actor-definition/output-schema#available-template-variables)


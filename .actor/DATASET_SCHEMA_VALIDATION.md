# Apify Dataset Schema Validation

This document validates the dataset schema against Apify's official specification.

## Apify Dataset Schema Specification

According to [Apify's Dataset Schema Documentation](https://docs.apify.com/platform/actors/development/actor-definition/dataset-schema):

### Required Top-Level Fields

- ✅ `actorDatasetSchemaVersion` (integer, required) - Must be `1`
- ✅ `fields` (JSONSchema compatible object, required) - Schema of one dataset object
- ✅ `views` (DatasetView object, required) - Description of API and UI views

### DatasetView Object Requirements

Each view must have:

- ✅ `title` (string, required) - Visible in UI Output tab and API
- ✅ `transformation` (ViewTransformation object, required) - Data transformation definition
- ✅ `display` (ViewDisplay object, required) - Output tab UI visualization

### ViewTransformation Requirements

- ✅ `fields` (string[], required) - Fields to be presented in output

### ViewDisplay Requirements

- ✅ `component` (string, required) - Must be `"table"` (only available component)
- ✅ `properties` (Object, optional) - Display properties for fields

## Current Schema Validation

### Top-Level Structure

```json
{
  "actorDatasetSchemaVersion": 1,  ✅ Required, correct value
  "fields": { ... },                ✅ Required, present (empty schema)
  "views": { ... }                  ✅ Required, present
}
```

### Fields Schema

```json
{
  "type": "object",
  "properties": {}
}
```

✅ **Valid**: Empty object schema indicates no dataset fields (MCP server doesn't store data in datasets)

### Views Schema

#### `overview` View ✅

- ✅ `title: "No Dataset Output"` - Present
- ✅ `description` - Present (explains why no dataset)
- ✅ `transformation.fields: []` - Empty array (no fields to display)
- ✅ `display.component: "table"` - Required, correct value
- ✅ `display.properties: {}` - Optional, empty object

## Why This Schema?

This MCP server:
- ✅ Returns JSON responses directly via MCP protocol
- ✅ Does NOT use `Actor.pushData()` to store data
- ✅ Does NOT store results in Apify datasets
- ✅ Communicates via MCP protocol (stdio or SSE)

The dataset schema is minimal to:
1. Satisfy Apify's requirement for dataset schema
2. Clearly indicate no dataset output is used
3. Provide a friendly message in the Output tab

## Compliance Status

✅ **FULLY COMPLIANT** with Apify's Dataset Schema Specification

### Validation Checklist

- ✅ `actorDatasetSchemaVersion: 1` - Correct version
- ✅ `fields` - Present with valid JSON Schema (empty object)
- ✅ `views` - Present with at least one view
- ✅ View has `title` - Present
- ✅ View has `transformation` - Present with `fields` array
- ✅ View has `display` - Present with `component: "table"`
- ✅ All required fields present
- ✅ No invalid fields

## Alternative: Separate File

The dataset schema can also be defined in a separate file:

```json
// .actor/actor.json
{
  "storages": {
    "dataset": "./dataset_schema.json"
  }
}
```

We've chosen to include it inline in `actor.json` for simplicity.

## References

- [Apify Dataset Schema Documentation](https://docs.apify.com/platform/actors/development/actor-definition/dataset-schema)


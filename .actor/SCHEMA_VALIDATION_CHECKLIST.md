# Apify Schema Validation Checklist

This checklist verifies compliance with Apify's Actor schema specifications.

## Input Schema Validation

### Required Top-Level Fields
- ✅ `title` - Present
- ✅ `type: "object"` - Present
- ✅ `schemaVersion: 1` - Present
- ✅ `properties` - Present

### String Type Fields

According to Apify specification, string fields must either:
- Have an `enum` (becomes dropdown), OR
- Have an `editor` field (textfield, textarea, secret, etc.)

#### Field Validation:

1. **`mcpTransport`** ✅
   - Type: `string`
   - Has `enum`: ✅ ["stdio", "sse"]
   - Has `enumTitles`: ✅
   - Editor: Not required (has enum)

2. **`mcpSsePath`** ✅
   - Type: `string`
   - Has `enum`: ❌
   - Has `editor`: ✅ "textfield"
   - **FIXED**: Added `editor: "textfield"`

3. **`licenseKey`** ✅
   - Type: `string`
   - Has `enum`: ❌
   - Has `editor`: ✅ "textfield"

4. **`defaultTier`** ✅
   - Type: `string`
   - Has `enum`: ✅ ["COMMUNITY", "PREMIUM", "ENTERPRISE"]
   - Has `enumTitles`: ✅
   - Editor: Not required (has enum)

5. **`apifyApiToken`** ✅
   - Type: `string`
   - Has `enum`: ❌
   - Has `editor`: ✅ "secret"

6. **`stripeSecretKey`** ✅
   - Type: `string`
   - Has `enum`: ❌
   - Has `editor`: ✅ "secret"

### Integer Type Fields

1. **`mcpSsePort`** ✅
   - Type: `integer`
   - Has `minimum`: ✅ 1
   - Has `maximum`: ✅ 65535
   - Has `default`: ✅ 8080

### Number Type Fields

1. **`maxTotalChargeUsd`** ✅
   - Type: `number`
   - Has `minimum`: ✅ 0
   - Has `default`: ✅ 10.0

### Boolean Type Fields

1. **`apifyValidationEnabled`** ✅
   - Type: `boolean`
   - Has `default`: ✅ true

2. **`stripeValidationEnabled`** ✅
   - Type: `boolean`
   - Has `default`: ✅ true

## Output Schema Validation

### Required Top-Level Fields
- ✅ `actorOutputSchemaVersion: 1` - Present
- ✅ `title` - Present
- ✅ `type: "object"` - Present
- ✅ `properties` - Present

### Property Validation

All properties have:
- ✅ `type: "string"`
- ✅ `title`
- ✅ `description`
- ✅ `template` with valid template variables

## Actor Configuration Validation

### Required Fields
- ✅ `actorSpecification: 1` - Present
- ✅ `name` - Present
- ✅ `title` - Present
- ✅ `version` - Present
- ✅ `description` - Present

### Optional Fields
- ✅ `readme` - Present
- ✅ `input` - Present (references input_schema.json)
- ✅ `output` - Present (references output_schema.json)
- ✅ `storages` - Removed (not needed for MCP server)

## Apify Specification Compliance

### Input Schema v1
- ✅ All string fields without `enum` have `editor` field
- ✅ All string fields with `enum` have `enumTitles`
- ✅ All fields have `title` and `description`
- ✅ Section captions used for grouping
- ✅ Default values provided where appropriate

### Output Schema v1
- ✅ Uses `actorOutputSchemaVersion: 1`
- ✅ All properties have templates with valid variables
- ✅ Templates use `{{run.containerUrl}}` format

### Actor Specification v1
- ✅ Uses `actorSpecification: 1`
- ✅ No invalid storage definitions
- ✅ All references are valid file paths

## Status: ✅ VALIDATED

All schemas are now compliant with Apify's specifications.


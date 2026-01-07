# Configuration Binding Fix - January 6, 2026

## Problem

Application fails to start with:
```
Failed to bind properties under 'jakarta.migration.feature-flags.features' to java.util.Map<java.lang.String, java.lang.Boolean>:
    Property: jakarta.migration.feature-flags.features
    Value: ""
    Reason: No converter found capable of converting from type [java.lang.String] to type [java.util.Map<java.lang.String, java.lang.Boolean>]
```

## Root Cause

In `application.yml`, the `features:` key was defined with only commented-out examples:
```yaml
features:
  # Example: Enable auto-fixes for testing
  # auto-fixes: true
```

When a YAML key has no actual values (only comments), Spring Boot tries to bind it as an empty string `""` instead of an empty map `{}`. Since `FeatureFlagsProperties.features` is defined as `Map<String, Boolean>`, Spring can't convert an empty string to a Map.

## Solution

Set `features` to an explicit empty map `{}`:
```yaml
features: {}
```

This tells Spring Boot to bind it as an empty `Map<String, Boolean>` rather than trying to parse an empty string.

## Fix Applied

Updated `application.yml` to set `features: {}` instead of just `features:` with comments.

**Status:** ✅ Fixed


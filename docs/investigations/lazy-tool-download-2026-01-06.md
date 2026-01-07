# Lazy Tool Download Implementation - January 6, 2026

## Problem

The Apache Tomcat migration tool was being downloaded at startup when `RefactoringEngine` was created, causing:
- Startup delays
- Startup failures if download fails (HTTP 404/403)
- Unnecessary network calls when the tool might never be used

## Solution

Made the tool download **lazy/on-demand** - it only downloads when the tool is actually used.

## Changes Made

### 1. Lazy Initialization in Constructor

**Before:**
```java
public ApacheTomcatMigrationTool() {
    this.toolJarPath = findToolJar();  // Downloads immediately!
    this.timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
}
```

**After:**
```java
public ApacheTomcatMigrationTool() {
    this.toolJarPath = null;  // Will be resolved lazily when migrate() is called
    this.timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
}
```

### 2. Lazy Resolution in `migrate()` Method

**Before:**
- Tool path was already resolved in constructor

**After:**
```java
public MigrationResult migrate(Path sourcePath, Path destinationPath) throws IOException {
    // ... validation ...
    
    // Lazy initialization: find/download tool only when actually needed
    if (toolJarPath == null) {
        toolJarPath = findToolJar();
    }
    
    // ... rest of method ...
}
```

### 3. Cache Check Before Download

**Before:**
- Always tried to download immediately

**After:**
- First checks cache directory for existing tool
- Only downloads if not found in cache
- Downloads happen on-demand when `migrate()` is called

### 4. Helper Methods Added

Added `getCacheDirectory()` and `findCachedJar()` methods to check cache without downloading:
- `getCacheDirectory()` - Returns cache directory path if it exists (doesn't create it)
- `findCachedJar()` - Searches cache directory for existing JAR files

## Impact

### Benefits

1. ✅ **Faster Startup** - No download attempts at startup
2. ✅ **No Startup Failures** - Download failures don't prevent application from starting
3. ✅ **On-Demand Download** - Tool only downloads when actually needed
4. ✅ **Cache-First** - Checks cache before attempting download

### Behavior

- **At Startup**: `ApacheTomcatMigrationTool` is created, but no download happens
- **On First Use**: When `migrate()` is called, tool is found/downloaded
- **Subsequent Uses**: Uses cached tool (if available)

## Testing

The MCP server should now:
- Start without download attempts
- Only download tool when a migration is actually requested
- Handle download failures gracefully (returns null, migration fails with clear error)

**Status:** ✅ Implemented


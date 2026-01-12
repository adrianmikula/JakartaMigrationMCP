# Dual Package Refactor - Complete

## Summary

The dual package refactor has been completed. The codebase has been restructured to:

1. **Keep free code in the original folder structure** - Free components remain in `src/main/java/adrianmikula/jakartamigration/`
2. **Move premium code to subfolder** - Premium components are now in `jakarta-migration-mcp-premium/` subfolder

## Structure

### Main Project (Free Package)
```
src/main/java/adrianmikula/jakartamigration/
├── dependencyanalysis/     ✅ FREE - Dependency analysis
├── sourcecodescanning/      ✅ FREE - Source code scanning
└── mcp/
    └── JakartaMigrationTools.java  ✅ FREE - Only 4 free tools
```

**Build Output**: `jakarta-migration-mcp-free-{version}.jar`

### Premium Package (Subfolder)
```
jakarta-migration-mcp-premium/
├── src/main/java/adrianmikula/jakartamigration/
│   ├── dependencyanalysis/     ✅ Copied (needed by premium)
│   ├── sourcecodescanning/      ✅ Copied (needed by premium)
│   ├── coderefactoring/        ✅ PREMIUM - Code refactoring
│   ├── runtimeverification/    ✅ PREMIUM - Runtime verification
│   ├── config/                 ✅ PREMIUM - License validation
│   ├── api/                    ✅ PREMIUM - License API, Stripe webhooks
│   ├── storage/                 ✅ PREMIUM - License storage
│   └── mcp/
│       └── JakartaMigrationTools.java  ✅ PREMIUM - All tools
├── build.gradle.kts            ✅ Premium build config
├── settings.gradle.kts         ✅ Premium settings
├── package.json                ✅ Premium npm package
└── index.js                    ✅ Premium npm wrapper
```

**Build Output**: `jakarta-migration-mcp-premium-{version}.jar`

## Changes Made

### Main Project (`build.gradle.kts`)
- ✅ Excluded premium packages from source sets
- ✅ Removed OpenRewrite plugin and dependencies
- ✅ Removed ASM dependencies
- ✅ Updated bootJar to create `jakarta-migration-mcp-free-{version}.jar`
- ✅ Updated `JakartaMigrationTools.java` to only have 4 free tools

### Premium Package (`jakarta-migration-mcp-premium/`)
- ✅ Created complete premium package structure
- ✅ Copied all premium packages (coderefactoring, runtimeverification, config, api, storage)
- ✅ Copied free packages (dependencyanalysis, sourcecodescanning) - needed by premium
- ✅ Copied MCP infrastructure
- ✅ Created `build.gradle.kts` with all premium dependencies
- ✅ Created `package.json` and `index.js` for premium npm package
- ✅ Copied resources, scripts, and config files

### npm Packages
- ✅ Updated main `package.json` for free package
- ✅ Updated main `index.js` to download free JAR
- ✅ Created premium `package.json`
- ✅ Created premium `index.js` to download premium JAR

## Next Steps

1. **Test Free Package Build**:
   ```bash
   ./gradlew bootJar
   # Should create: build/libs/jakarta-migration-mcp-free-1.0.0-SNAPSHOT.jar
   ```

2. **Test Premium Package Build**:
   ```bash
   cd jakarta-migration-mcp-premium
   ./gradlew bootJar
   # Should create: build/libs/jakarta-migration-mcp-premium-1.0.0-SNAPSHOT.jar
   ```

3. **Move Premium to Separate Repo** (when ready):
   - The `jakarta-migration-mcp-premium/` folder can be checked into a new private repository
   - Update GitHub repo URLs in premium package.json and index.js

4. **Publish npm Packages**:
   - Free package: `@jakarta-migration/mcp-server` (open source)
   - Premium package: `@jakarta-migration/mcp-server-premium` (closed source)

## Notes

- The premium package includes all free components because premium tools depend on them
- The main project excludes premium packages to keep the free package lightweight
- Both packages can be built independently
- The premium folder structure is ready to be moved to a separate repository


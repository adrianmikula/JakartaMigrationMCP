# Release Guide - Jakarta Migration IntelliJ Plugin

This document provides step-by-step instructions for releasing new versions of the Jakarta Migration plugin to the JetBrains Marketplace.

## Release Types

### Minor Release (e.g., 1.0.15 → 1.0.16)
- Bug fixes and minor improvements
- New features that don't break compatibility
- **Does NOT change** `release-version` in plugin.xml
- **Does NOT change** monetization model, pricing, or plugin name

### Major Release (e.g., 1.x → 2.x)
- Breaking changes or major architectural updates
- **CAN change** `release-version` in plugin.xml
- **CAN change** monetization model, pricing, or plugin name
- Requires additional verification for breaking changes

---

## Pre-Release Checklist (Both Release Types)

### 1. Version Updates

#### Update `gradle.properties`
```properties
pluginVersion=1.0.16
```

#### Update `plugin.xml`
```xml
<!-- premium-intellij-plugin/src/main/resources/META-INF/plugin.xml -->
<version>1.0.16</version>
```

**Important**: For minor releases, `release-version` stays the same:
```xml
<!-- Keep unchanged for minor releases -->
<product-descriptor 
    code="PJAKARTAMIGRATI" 
    release-date="20260101" 
    release-version="10"
    optional="true"/>
```

For major releases, update `release-version` to match the new major version digits:
```xml
<!-- Example: For v2.0.0, release-version="20" -->
<product-descriptor 
    code="PJAKARTAMIGRATI" 
    release-date="20260101" 
    release-version="20"
    optional="true"/>
```

#### Update release-date (Major Releases Only)
Set to today's date in YYYYMMDD format:
```xml
release-date="20260527"
```

### 2. SQLite Database Schema Management

#### Minor Releases (Upgrade Scripts Required)

Minor releases **must** maintain backward compatibility and provide upgrade scripts:

1. Create new Liquibase changelog for schema changes in `premium-core-engine/src/main/resources/db/changelog/`
2. Name format: `YYYYMMDDHHMMSS__description.xml` (e.g., `20260527120000__add_new_column.xml`)
3. Ensure changelog follows sequential order - no gaps between versions
4. Test upgrade path from **all** previous minor versions in the same major line
5. Verify existing data is preserved during migration
6. **Never** drop tables or columns in minor releases (only add/alter)

Example upgrade changelog:
```xml
<changeSet id="1.0.16-add-column" author="developer">
    <addColumn tableName="scan_results">
        <column name="new_field" type="VARCHAR(255)"/>
    </addColumn>
</changeSet>
```

#### Major Releases (Complete Schema Rebuild)

Major releases **can** rebuild the SQLite schema completely:

1. No backward compatibility required - users start with fresh database
2. Redesign schema in `premium-core-engine/src/main/resources/db/changelog/db.changelog-master.xml`
3. Update all entity classes to match new schema
4. Document breaking changes in release notes
5. Test fresh install on clean system

**Note**: Major version bumps clear existing user data. Document this prominently.

**Known Issue**: SQLite does not support `Statement.RETURN_GENERATED_KEYS`. Use `SELECT last_insert_rowid()` instead. See `docs/troubleshooting/common_issues2.md` for details.

### 3. MCP Metadata and Tools Verification

Before releasing, update MCP (Model Context Protocol) metadata and tools to reflect new/changed functionality:

#### Update MCP Server Metadata

In `premium-intellij-plugin/build.gradle.kts`, verify the `generateMcpToolsJson` task reflects current capabilities:

```kotlin
tasks.register("generateMcpToolsJson") {
    // Verify server version matches plugin version
    // "version": "${project.version}"
    
    // Check that all tools are documented
    // - analyzeJakartaReadiness
    // - analyzeMigrationImpact
    // - detectBlockers
    // - recommendVersions
    // - applyOpenRewriteRefactoring
    // - [NEW tools for this release]
}
```

#### Verify MCP Tool Definitions

Check that each tool's `inputSchema` matches the actual implementation:

1. Review `premium-mcp-server/src/main/kotlin/.../tools/` for new/changed tools
2. Verify parameter names and types match between schema and code
3. Update descriptions if functionality changed
4. Add new tools to the JSON generation task
5. Test MCP server startup with updated definitions

#### Update AI Assistant Documentation

If new tools were added or existing ones modified:
1. Update tool descriptions in `generateMcpToolsJson` task
2. Verify AI Assistant can discover and use new capabilities
3. Test example prompts that exercise new functionality

### 4. Testing Requirements

#### Required Tests (Must Pass)
Run all unit and integration tests:

```bash
./gradlew :premium-intellij-plugin:test
./gradlew :community-core-engine:test
./gradlew :premium-core-engine:test
```

#### Marketplace Validation
```bash
./gradlew :premium-intellij-plugin:validateMarketplaceRequirements
```

#### Plugin Verifier (Recommended)
```bash
./gradlew :premium-intellij-plugin:buildPlugin :premium-intellij-plugin:runPluginVerifier
```

#### Optional Tests (Ask Before Skipping)
The following tests may be skipped with explicit confirmation:
- Heavy/slow performance tests
- Automated UI tests requiring full IntelliJ Platform environment

**Ask yourself**: "Are there any performance-critical changes that could affect large projects?" If yes, run performance tests.

#### Database Migration Tests (Required)
```bash
# For minor releases - test upgrade from previous version
./gradlew :premium-core-engine:test --tests "*DatabaseMigrationTest*"

# For major releases - test fresh database creation
./gradlew :premium-core-engine:test --tests "*DatabaseSchemaTest*"
```

#### Official IntelliJ Plugin Verification (Required)

Run the JetBrains Plugin Verifier on the bundled plugin ZIP before uploading:

```bash
# Build and verify the plugin
./gradlew :premium-intellij-plugin:buildPlugin :premium-intellij-plugin:runPluginVerifier
```

This validates:
- Binary compatibility with target IntelliJ versions
- Missing classes or methods
- Internal API usages
- Deprecated API usages
- Plugin descriptor issues

**Note**: Currently this is also available via the JetBrains Marketplace web UI after upload, but running it locally first catches issues earlier.

Reports are written to:
```
premium-intellij-plugin/build/reports/pluginVerifier/
```

Review the HTML/TXT report and resolve any compatibility problems before uploading.

### 4. Build Verification

```bash
./gradlew :premium-intellij-plugin:buildPlugin
```

Verify the generated ZIP:
1. Extract and check `META-INF/plugin.xml` contains correct version
2. **Critical**: Verify `until-build` attribute is NOT present in generated `plugin.xml`
3. Verify no IDE packages are bundled (see Known Issues below)

### 5. Documentation Updates

#### Update `plugin.xml` Changelog
Add new version entry to `<change-notes>`:
```xml
<h>1.0.16 - 2026-05-27</h>
<ul>
    <li>Fixed: ...</li>
    <li>Added: ...</li>
</ul>
```

#### Update Root `README.md`
Update the feature list and version references in the root-level README.

---

## Major Release Additional Steps

### 1. SQLite Schema Rebuild (Major Releases Only)

For major releases, rebuild the SQLite schema completely:

1. **Archive old changelogs**: Move `db.changelog-master.xml` contents to versioned archive
2. **Create fresh schema**: Design optimized schema without legacy baggage
3. **Update entities**: Modify all JPA/entity classes to match new schema
4. **Document breaking changes**: Users will lose existing scan history, refactor logs, etc.
5. **Test fresh installation**: Verify clean install works on new schema

**User Impact Warning**: Major version upgrades reset all local data. Document prominently:
- Scan history will be lost
- Refactor undo history will be cleared
- Settings may reset to defaults

### 2. MCP Tools Redesign (Major Releases Only)

Major releases can restructure MCP tools:

1. Review all tool implementations in `premium-mcp-server/`
2. Consolidate or split tools as needed
3. Update `generateMcpToolsJson` task with new tool structure
4. Verify AI Assistant integration with new tool signatures
5. Test all example prompts after tool changes

### 3. Monetization Model Review

Major releases can modify:
- Free vs Premium feature boundaries
- Pricing tiers
- Trial periods
- License enforcement

Review and update:
- `premium-intellij-plugin/src/main/java/.../license/CheckLicense.java`
- Feature flags in UI components
- `config/freemium.properties`

### 2. Breaking Changes Documentation

If the release contains breaking changes:
1. Document migration path for users
2. Update API documentation
3. Add warnings for deprecated features

### 4. IntelliJ Platform Compatibility

Review and update `since-build` if dropping support for older IntelliJ versions:
```xml
<idea-version since-build="242" />
```

**Note**: The `until-build` attribute should be **omitted entirely** for open-ended compatibility. See Known Issues below.

---

## Known Issues & Gotchas

### untilBuild Attribute Automatically Added

**Problem**: The IntelliJ Gradle plugin automatically adds `untilBuild="233.*"` if not explicitly configured.

**Solution**: In `premium-intellij-plugin/build.gradle.kts`, explicitly set empty string:
```kotlin
patchPluginXml {
    sinceBuild.set(providers.gradleProperty("intellij.sinceBuild").orElse("242"))
    untilBuild.set(providers.gradleProperty("intellij.untilBuild").orElse(""))
}
```

**Verification**: After building, extract the ZIP and verify the generated `META-INF/plugin.xml` contains:
```xml
<idea-version since-build="242" />
```
NOT:
```xml
<idea-version since-build="242" until-build="242.*" />
```

See `docs/troubleshooting/common_issues2.md` and `docs/premium/investigations/intellij-compatibility-range.md` for full details.

### Marketplace Compatibility Limits

The JetBrains Marketplace enforces a maximum `until-build` of `243.*`. Values greater than this are rejected. The solution is to omit the `until-build` attribute entirely (as shown above), giving open-ended compatibility with all future IntelliJ versions.

### IDE Package Bundling

Ensure `org.jetbrains.concurrency` and `org.jetbrains.util` packages are excluded from the JAR:
```kotlin
// In build.gradle.kts
tasks.named<org.jetbrains.intellij.tasks.PrepareSandboxTask>("prepareSandbox") {
    exclude { entry ->
        entry.name.contains("org/jetbrains/concurrency/") ||
        entry.name.contains("org/jetbrains/util/")
    }
}
```

---

## Manual Upload to JetBrains Marketplace

### 1. Build the Release

```bash
./gradlew :premium-intellij-plugin:clean :premium-intellij-plugin:buildPlugin
```

The plugin ZIP will be at:
```
premium-intellij-plugin/build/distributions/jakarta-migration-intellij-plugin-1.0.16.zip
```

### 2. Verify the Build

```bash
# Extract and check plugin.xml
unzip -p premium-intellij-plugin/build/distributions/jakarta-migration-intellij-plugin-1.0.16.zip META-INF/plugin.xml | head -30
```

Verify:
- Version number is correct
- No `until-build` attribute present
- `product-descriptor` is properly configured

### 3. Upload to Marketplace

1. Log in to [JetBrains Marketplace Vendor Portal](https://plugins.jetbrains.com/author/me)
2. Find "Jakarta Migration" in your plugins list
3. Click "Upload Update"
4. Select the built ZIP file
5. Review the compatibility range shown (should be open-ended, e.g., "242 — ")
6. Add release notes (copy from `plugin.xml` changelog)
7. Submit for review

### 4. Post-Upload Verification

1. Wait for JetBrains approval email (typically 1-2 business days)
2. Once approved, verify in the [Marketplace listing](https://plugins.jetbrains.com/plugin/26747-jakarta-migration)
3. Verify version appears correctly
4. Test installation in a clean IntelliJ instance

---

## Post-Release Steps

1. **Tag the Release**:
   ```bash
   git tag -a v1.0.16 -m "Release version 1.0.16"
   git push origin v1.0.16
   ```

2. **Update Version for Development**:
   Update `gradle.properties` and `plugin.xml` to next development version (e.g., `1.0.17-SNAPSHOT`)

3. **Update Documentation**:
   - Update `docs/RELEASING.md` version history section
   - Update any version-specific documentation

---

## Quick Reference

### Version Update Locations

| File | Field | Example |
|------|-------|---------|
| `gradle.properties` | `pluginVersion` | `1.0.16` |
| `plugin.xml` | `<version>` | `1.0.16` |
| `plugin.xml` | `release-version` | `10` (minor) / `20` (major) |
| `plugin.xml` | `release-date` | `20260527` |

### Build Commands

```bash
# Full test suite
./gradlew test

# Marketplace validation
./gradlew :premium-intellij-plugin:validateMarketplaceRequirements

# Build plugin
./gradlew :premium-intellij-plugin:buildPlugin

# Verify against multiple IntelliJ versions
./gradlew :premium-intellij-plugin:testIntelliJMatrix
```

---

*This document should be updated when new known issues are discovered or when the release process changes.*

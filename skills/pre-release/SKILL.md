# Pre-Release Skill

## Overview

This skill guides the pre-release verification for the Jakarta Migration IntelliJ plugin. It ensures that a release is not only built and tested, but also correctly documented: version numbers are consistent across all files, `CHANGELOG.md` and `plugin.xml` release notes are detailed and complete, and the generated plugin ZIP matches the intended version.

## When to Use This Skill

- A user says they want to cut a release, bump the version, or run the pre-release checklist.
- After a version bump, to verify release notes are present and meaningful.
- Before building the plugin ZIP for Marketplace upload.
- When reviewing whether `CHANGELOG.md`, `plugin.xml`, and `gradle.properties` are in sync.

## Pre-Release Checklist

### 1. Determine the release version

- Confirm the target version (e.g., `1.1.2`).
- Confirm whether it is a **patch**, **minor**, or **major** release.
- Patch/minor: keep the existing `release-version` in `plugin.xml`.
- Major: update `release-date` and set `release-version` to the new major/minor digits.

### 2. Update all version files

Update the version in every location:

- `gradle.properties` — `version=X.Y.Z`
- `premium-core-engine/src/main/resources/version.properties` — `version=X.Y.Z`
- `package.json` — `"version": "X.Y.Z"`
- `premium-intellij-plugin/src/main/resources/META-INF/plugin.xml` — `<version>X.Y.Z</version>`
- `build.gradle.kts` — the fallback `version=` default used by the `incrementVersion` task

Verify consistency:
```bash
grep -E "^version=|<version>" gradle.properties premium-core-engine/src/main/resources/version.properties premium-intellij-plugin/src/main/resources/META-INF/plugin.json 2>/dev/null || true
grep '"version"' package.json
```

### 3. Audit release notes

This step is the one most often missed. Do not allow a release to go out with only "Version bump to X.Y.Z" bullets.

#### 3.1 Identify actual changes

Use git to list the commits since the previous release:
```bash
git log <previous-release-tag-or-commit>..HEAD --oneline
```

Group the commits into **Added**, **Changed**, **Fixed**, and **Removed** categories.

#### 3.2 Update `CHANGELOG.md`

- Add a new top section `## [X.Y.Z] - YYYY-MM-DD`.
- Include real features and bugfixes derived from the commit history.
- Remove any placeholder "Version bump to X.Y.Z" entries.

#### 3.3 Update `plugin.xml` change notes

- In `premium-intellij-plugin/src/main/resources/META-INF/plugin.xml`, add a new `<h>X.Y.Z - YYYY-MM-DD</h>` block inside `<change-notes>` at the top.
- Use `<li><b>Added:</b> ...</li>`, `<li><b>Fixed:</b> ...</li>`, `<li><b>Changed:</b> ...</li>` markup.
- Ensure the same version is in `<version>X.Y.Z</version>`.

#### 3.4 Cross-check the two sources

- Every item in the `CHANGELOG.md` `X.Y.Z` section should also appear in the `plugin.xml` `<change-notes>`.
- Both must have the same release date.
- No placeholder-only release sections are allowed.

### 4. Run the build and test commands

Execute from the project root:

```bash
mise run test
mise run build-plugin-zip
```

- `mise run test` should pass.
- `mise run build-plugin-zip` should produce the ZIP at `premium-intellij-plugin/build/distributions/premium-intellij-plugin-X.Y.Z.zip`.

### 5. Verify the generated ZIP

Extract and inspect the built `plugin.xml`:

```bash
unzip -p premium-intellij-plugin/build/distributions/premium-intellij-plugin-X.Y.Z.zip \
  premium-intellij-plugin/lib/instrumented-premium-intellij-plugin-X.Y.Z.jar > /tmp/plugin.jar
unzip -p /tmp/plugin.jar META-INF/plugin.xml | grep -E "<version>|<idea-version|<product-descriptor"
```

Confirm:
- `<version>X.Y.Z</version>` is present.
- `<product-descriptor>` `release-version` is correct.
- `<idea-version since-build="..." />` does **not** have an `until-build` attribute.

### 6. Run Marketplace validation and plugin verifier (optional but recommended)

```bash
./gradlew :premium-intellij-plugin:validateMarketplaceRequirements --no-configuration-cache
mise run verify-plugin
```

If these fail due to known tooling/environment issues, capture the failure and document it. They must not fail because of a version or release-notes mismatch.

### 7. Final review

Before tagging or uploading:
- [ ] Version is identical in `gradle.properties`, `version.properties`, `package.json`, and `plugin.xml`.
- [ ] `CHANGELOG.md` has a detailed section for the new version.
- [ ] `plugin.xml` has detailed `<change-notes>` for the new version.
- [ ] The built ZIP contains the same version inside `META-INF/plugin.xml`.
- [ ] Tests passed and the plugin ZIP was produced.

## Common Gotchas

### Placeholder release notes

A release section like `- Version bump to 1.1.2` is not enough. Always replace it with real `Added`/`Changed`/`Fixed` bullets after reviewing the git log.

### `plugin.xml` version not updated

The `gradle.properties` version is the source of truth for the build, but the `<version>` tag in `plugin.xml` is what JetBrains Marketplace displays. If `plugin.xml` is not updated, the Marketplace upload will show the old version while the artifacts contain the new one.

### `plugin.xml` change notes missing the new version

A common failure is updating `<version>` but forgetting to add the matching `<h>X.Y.Z - ...</h>` block in `<change-notes>`. The release notes should start with the newest version at the top.

### Forgetting `package.json` or `version.properties`

These are consumed by the MCP server packaging and usage reporting. They must match too.

## Reference Files

- `docs/releases/README.md` — human-readable release process
- `gradle.properties` — root version source of truth
- `premium-core-engine/src/main/resources/version.properties` — runtime version for usage/error reporting
- `package.json` — MCP server package version
- `premium-intellij-plugin/src/main/resources/META-INF/plugin.xml` — JetBrains Marketplace descriptor
- `CHANGELOG.md` — public changelog

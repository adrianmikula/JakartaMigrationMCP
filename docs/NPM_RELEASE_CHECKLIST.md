# npm Production Release Checklist

Use this checklist before publishing to the npm registry.

## Pre-release

- [ ] **Version sync**: Update `version` in both `package.json` and `build.gradle.kts` (remove `-SNAPSHOT` for release). Tag and package version should match (e.g. `1.0.0`).
- [ ] **Changelog**: Document changes since last release (optional but recommended).
- [ ] **Tests**: Run `./gradlew test` and `npm test`; fix any failures.
- [ ] **Lint / quality**: Run `./gradlew check` (or your code-quality script) and fix issues.

## package.json

- [x] **name**: `@jakarta-migration/mcp-server` (scoped; first publish needs `npm publish --access public`).
- [x] **version**: Matches release (e.g. `1.0.0`).
- [x] **description**: Present and accurate.
- [x] **main** / **bin**: Point to `index.js`.
- [x] **license**: `BUSL-1.1` (matches root `LICENSE`).
- [x] **repository**: Correct GitHub URL with `.git` suffix.
- [x] **homepage** / **bugs**: Set for npm and support links.
- [x] **files**: Only what should be published (`index.js`, `scripts/`, `README.md`, `LICENSE`). No source, Gradle, or config.

## GitHub Release

- [ ] **Tag**: Create tag `v1.0.0` (match package version). Pushing the tag triggers the release workflow.
- [ ] **NPM_TOKEN**: In repo **Settings → Secrets and variables → Actions**, add `NPM_TOKEN` (npm auth token with publish access for the package).
- [ ] **Scope access**: If using `@jakarta-migration`, ensure the npm user/org has access and the package name is available (or use an unscoped name).

## After tagging

1. Workflow **Build and Release** runs: builds JAR, creates GitHub Release, publishes to npm.
2. Release job uploads `jakarta-migration-mcp-<version>.jar` to GitHub Releases (used by `index.js` for downloads).
3. **npm-publish** job runs `npm publish --access public` (requires `NPM_TOKEN`).

## Verify after publish

- [ ] **npm**: `npm view @jakarta-migration/mcp-server` shows the new version.
- [ ] **Install**: `npx -y @jakarta-migration/mcp-server --download-only` downloads the JAR from GitHub Releases and runs without errors.
- [ ] **README**: Installation and usage instructions work for the published version.

## Optional

- **npm pack**: Run `npm pack` locally to inspect the tarball contents before publishing.
- **Unpublish**: If you need to fix and re-publish, use `npm unpublish @jakarta-migration/mcp-server@1.0.0 --force` (restrictions apply; prefer publishing a new patch version).

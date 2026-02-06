# Packaging and Release Guide

This document describes how to build, package, and release the Jakarta Migration MCP Server.

## Overview

The Jakarta Migration MCP Server is distributed as:
1. **npm package** (`@jakarta-migration/mcp-server`) - Lightweight Node.js wrapper that downloads and runs the JAR
2. **GitHub Releases** - Direct JAR downloads for manual installation

## Build Process

### Prerequisites

- Java 21+
- Gradle (or use the included wrapper)
- Node.js 18+ (for npm packaging)
- Git (for releases)

### Building the JAR

```bash
# Clean and build
./gradlew clean bootJar

# The JAR will be in build/libs/
# Look for: jakarta-migration-mcp-1.0.0-SNAPSHOT.jar
```

### Building for Release

#### Linux/macOS

```bash
./scripts/build-release.sh
```

This script will:
- Clean previous builds
- Build the JAR
- Create a `release/` directory
- Copy the JAR with standardized naming
- Generate checksums (SHA256, MD5)

#### Windows

```powershell
.\scripts\build-release.ps1
```

Same functionality as the shell script, adapted for PowerShell.

## Release Process

### 1. Update Version

Update the version in:
- `build.gradle.kts` - `version = "1.0.0"`
- `package.json` - `"version": "1.0.0"`

### 2. Build Release Artifacts

```bash
# Linux/macOS
./scripts/build-release.sh

# Windows
.\scripts\build-release.ps1
```

### 3. Test the Release

```bash
# Test the JAR
java -jar release/jakarta-migration-mcp-1.0.0.jar \
  --spring.main.web-application-type=none \
  --spring.profiles.active=mcp

# Test npm package locally
npm pack
npm install -g jakarta-migration-mcp-server-1.0.0.tgz
jakarta-migration-mcp
```

### 4. Create GitHub Release

```bash
# Create a git tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# Or use GitHub CLI
gh release create v1.0.0 \
  release/jakarta-migration-mcp-1.0.0.jar \
  release/jakarta-migration-mcp-1.0.0.jar.sha256 \
  --title "Release v1.0.0" \
  --notes "Release notes here"
```

### 5. Publish to npm

```bash
# Ensure you're logged in
npm login

# Publish
npm publish --access public
```

**Note:** The GitHub Actions workflow can automate steps 4 and 5 when you push a tag.

## Automated Releases (GitHub Actions)

The `.github/workflows/release.yml` workflow automates:

1. **Build** - Builds the JAR when a tag is pushed
2. **Release** - Creates a GitHub release with the JAR and checksums
3. **npm Publish** - Publishes to npm registry

### Triggering a Release

```bash
# Create and push a tag
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

The workflow will:
- Build the JAR
- Create a GitHub release
- Publish to npm (if `NPM_TOKEN` secret is configured)

### Required Secrets

For npm publishing, add this secret to GitHub:
- `NPM_TOKEN` - npm authentication token with publish permissions

## npm Package Structure

```
@jakarta-migration/mcp-server/
├── index.js              # Main entry point (Node.js wrapper)
├── scripts/
│   └── postinstall.js   # Post-install verification
├── package.json         # npm package metadata
├── README.md            # Package README
└── LICENSE              # License file
```

### How It Works

1. User installs: `npm install -g @jakarta-migration/mcp-server`
2. `index.js` is installed as `jakarta-migration-mcp` command
3. When invoked, it:
   - Checks for Java installation
   - Downloads the JAR from GitHub releases (if not cached)
   - Runs the JAR with MCP-specific arguments
   - Communicates via stdio (MCP protocol)

### JAR Caching

The JAR is cached in:
- **Windows:** `%USERPROFILE%\AppData\jakarta-migration-mcp\`
- **Linux/macOS:** `~/.cache/jakarta-migration-mcp/`

This avoids re-downloading on every run.

## Version Management

### Semantic Versioning

Follow [Semantic Versioning](https://semver.org/):
- **MAJOR** - Breaking changes
- **MINOR** - New features, backward compatible
- **PATCH** - Bug fixes, backward compatible

### Version Synchronization

Keep versions synchronized across:
- `build.gradle.kts` - `version = "1.0.0"`
- `package.json` - `"version": "1.0.0"`
- Git tags - `v1.0.0`

## Testing Releases

### Local Testing

```bash
# Build
./gradlew bootJar

# Test JAR directly
java -jar build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar \
  --spring.main.web-application-type=none \
  --spring.profiles.active=mcp

# Test npm package
npm pack
npm install -g jakarta-migration-mcp-server-1.0.0.tgz
jakarta-migration-mcp
```

### Integration Testing

Test with actual MCP clients:

1. **Cursor:**
   - Install the package
   - Configure in Cursor settings
   - Test MCP tools

2. **Claude Code:**
   - Install the package
   - Configure in Claude Code settings
   - Test MCP tools

3. **Antigravity:**
   - Install the package
   - Configure in Antigravity settings
   - Test MCP tools

## Troubleshooting

### Build Fails

- Check Java version: `java -version` (should be 21+)
- Clean build: `./gradlew clean build`
- Check Gradle wrapper: `./gradlew --version`

### npm Publish Fails

- Verify you're logged in: `npm whoami`
- Check package name availability
- Ensure version is unique
- Check npm registry access

### GitHub Release Fails

- Verify GitHub token has release permissions
- Check tag format: `v1.0.0` (must start with `v`)
- Ensure JAR file exists in `release/` directory

### JAR Download Fails in npm Package

- Check GitHub release exists
- Verify repository name in `GITHUB_REPO` env var
- Check network connectivity
- Build locally and use local JAR path

## Best Practices

1. **Always test locally** before releasing
2. **Update changelog** with release notes
3. **Tag releases** with semantic versioning
4. **Verify checksums** before publishing
5. **Test with MCP clients** after release
6. **Monitor npm downloads** and GitHub releases

## Next Steps

After setting up packaging:

1. Configure GitHub Actions secrets (NPM_TOKEN)
2. Test the release process locally
3. Create your first release
4. Update documentation with installation instructions
5. Monitor and iterate based on user feedback


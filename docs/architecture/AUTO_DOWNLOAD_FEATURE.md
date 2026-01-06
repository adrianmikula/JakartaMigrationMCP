# Auto-Download Feature for Migration Tools

## Overview

The Jakarta Migration MCP automatically downloads the Apache Tomcat Jakarta EE Migration Tool on first use, eliminating the need for manual installation or environment variable configuration.

## How It Works

### Automatic Download Process

1. **First Use Detection**: When the Apache Tomcat migration tool is requested, the system checks if it's already cached
2. **Version Detection**: Queries GitHub API to get the latest release version
3. **Download**: Downloads the tool JAR from GitHub releases
4. **Caching**: Stores the downloaded JAR in a platform-specific cache directory
5. **Subsequent Uses**: Uses the cached JAR on subsequent runs

### Cache Locations

The tool is cached in platform-specific directories:

- **Windows**: `%USERPROFILE%\AppData\Local\jakarta-migration-tools\`
- **Linux/macOS**: `~/.cache/jakarta-migration-tools/`

### Download Source

- **Repository**: [apache/tomcat-jakartaee-migration](https://github.com/apache/tomcat-jakartaee-migration)
- **Source**: GitHub Releases API
- **Format**: `jakartaee-migration-{version}-shaded.jar`

## Implementation Details

### ToolDownloader Service

The `ToolDownloader` service handles:
- GitHub API integration for version detection
- HTTP download with progress tracking
- Cache directory management
- Error handling and fallbacks

### Key Features

1. **Version Detection**: Automatically detects latest version from GitHub API
2. **Resilient Download**: Handles redirects, timeouts, and network errors
3. **Progress Logging**: Logs download progress for large files
4. **Cache Management**: Reuses cached files to avoid re-downloading
5. **Fallback Support**: Falls back to manual installation paths if download fails

## Usage

### Automatic (Default)

```java
// No configuration needed - tool is auto-downloaded
RefactoringEngine engine = new RefactoringEngine(MigrationTool.APACHE_TOMCAT_MIGRATION);
```

The tool will:
1. Check cache directory
2. Download if not found
3. Use cached version on subsequent runs

### Manual Override (Optional)

If you prefer to use a manually installed tool:

```bash
export JAKARTA_MIGRATION_TOOL_PATH=/path/to/jakartaee-migration-1.0.0-shaded.jar
```

The system will use the specified path instead of auto-downloading.

## Error Handling

### Download Failures

If auto-download fails, the system:
1. Logs a warning with error details
2. Falls back to checking manual installation locations
3. Provides helpful error messages with manual download instructions

### Network Issues

- **Timeout**: 30 seconds connect, 60 seconds read
- **Retries**: Not implemented (can be added if needed)
- **Offline Mode**: Falls back to cached version if available

## Benefits

1. **Zero Configuration**: Users don't need to manually download or configure tools
2. **Always Up-to-Date**: Automatically uses latest version (unless cached)
3. **Fast Subsequent Runs**: Cached tool is reused, no re-downloading
4. **Platform Agnostic**: Works on Windows, Linux, and macOS
5. **Optional Override**: Still supports manual configuration if needed

## Security Considerations

1. **HTTPS Only**: Downloads only from HTTPS URLs (GitHub)
2. **Verified Source**: Only downloads from official Apache GitHub repository
3. **Cache Validation**: Checks file existence and size before use
4. **No Code Execution**: Downloaded JARs are only executed as separate processes

## Future Enhancements

1. **Version Pinning**: Allow users to pin specific tool versions
2. **Update Checking**: Periodically check for newer versions
3. **Checksum Verification**: Verify downloaded files with checksums
4. **Multiple Tool Support**: Extend to other migration tools
5. **Offline Mode**: Better support for offline environments

---

*Last Updated: 2026-01-27*
*Status: Implemented and Active*


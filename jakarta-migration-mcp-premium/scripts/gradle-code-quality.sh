#!/bin/bash
# Helper script for Gradle code quality checks
# Prefers mise-en-place when available, then gradlew, then direct gradle

# Check if mise is available
if command -v mise >/dev/null 2>&1; then
    # Use mise exec to run gradle (mise exec sets up the environment and tool versions)
    mise exec -- gradle codeQualityVerify --no-daemon
elif [ -f gradlew ]; then
    # Fall back to gradlew if mise is not available
    ./gradlew codeQualityVerify --no-daemon
else
    # Last resort: direct gradle (may not have correct version)
    echo "Warning: mise not found and gradlew not available. Using system gradle (version may be incorrect)." >&2
    gradle codeQualityVerify --no-daemon
fi


#!/bin/bash

# Build and Release Script for Jakarta Migration MCP Server
# This script builds the JAR, packages it, and prepares for release

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "🚀 Building Jakarta Migration MCP Server..."
echo ""

# Sync versions first
node "$SCRIPT_DIR/sync-versions.js" 2>/dev/null || true

# Get version from gradle.properties (source of truth)
VERSION=$(grep -oP '^version=\K[^#\s]+' "$PROJECT_ROOT/gradle.properties" | head -1 || echo "1.0.0-SNAPSHOT")
VERSION_CLEAN=$(echo "$VERSION" | sed 's/-SNAPSHOT//')

echo "📦 Version: $VERSION"
echo ""

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Build JAR
echo "🔨 Building JAR..."
./gradlew :community-mcp-server:bootJar --no-daemon

# Find the built JAR
JAR_FILE=$(find community-mcp-server/build/libs -name "*.jar" ! -name "*-plain.jar" | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "❌ ERROR: JAR file not found in community-mcp-server/build/libs/"
    exit 1
fi

echo "✅ JAR built: $JAR_FILE"
echo ""

# Create release directory
RELEASE_DIR="release"
mkdir -p "$RELEASE_DIR"

# Copy JAR to release directory with standardized name
RELEASE_JAR="$RELEASE_DIR/jakarta-migration-mcp-${VERSION_CLEAN}.jar"
cp "$JAR_FILE" "$RELEASE_JAR"

echo "📦 Release JAR: $RELEASE_JAR"
echo ""

# Create checksums
echo "🔐 Creating checksums..."
cd "$RELEASE_DIR"
sha256sum "jakarta-migration-mcp-${VERSION_CLEAN}.jar" > "jakarta-migration-mcp-${VERSION_CLEAN}.jar.sha256"
md5sum "jakarta-migration-mcp-${VERSION_CLEAN}.jar" > "jakarta-migration-mcp-${VERSION_CLEAN}.jar.md5" 2>/dev/null || md5 "jakarta-migration-mcp-${VERSION_CLEAN}.jar" > "jakarta-migration-mcp-${VERSION_CLEAN}.jar.md5" 2>/dev/null || true

cd "$PROJECT_ROOT"

# Prepare npm package
echo "📦 Preparing npm package..."
npm pack --dry-run 2>/dev/null || echo "Note: npm pack requires package.json (already created)"

echo ""
echo "✅ Build complete!"
echo ""
echo "📋 Release artifacts:"
echo "   - JAR: $RELEASE_JAR"
echo "   - SHA256: $RELEASE_DIR/jakarta-migration-mcp-${VERSION_CLEAN}.jar.sha256"
echo ""
echo "🚀 Next steps:"
echo "   1. Test the JAR: java -jar $RELEASE_JAR"
echo "   2. Create GitHub release: gh release create v${VERSION_CLEAN} $RELEASE_JAR"
echo "   3. Publish to npm: npm publish"
echo ""


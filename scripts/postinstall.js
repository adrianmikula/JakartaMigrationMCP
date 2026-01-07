#!/usr/bin/env node

/**
 * Post-install script for npm package
 * This script verifies the installation, optionally downloads the JAR, and provides helpful information
 */

const fs = require('fs');
const path = require('path');
const os = require('os');

const isWindows = process.platform === 'win32';

console.log('\n✅ Jakarta Migration MCP Server installed successfully!\n');

// Note: JAR will be downloaded automatically on first use
// To pre-download the JAR, run: npx @jakarta-migration/mcp-server --download-only

// Check for Java
const { execSync } = require('child_process');
let javaVersion = null;
try {
  const output = execSync('java -version', { encoding: 'utf8', stdio: 'pipe' });
  // Extract version from output
  const match = output.match(/version\s+"?(\d+)/);
  if (match) {
    javaVersion = parseInt(match[1]);
  }
} catch (e) {
  console.warn('⚠️  Warning: Java not found in PATH.');
  console.warn('   Please install Java 21+ from https://adoptium.net/');
  console.warn('   The MCP server requires Java to run.\n');
}

if (javaVersion) {
  if (javaVersion < 21) {
    console.warn(`⚠️  Warning: Java ${javaVersion} detected, but Java 21+ is recommended.`);
  } else {
    console.log(`✅ Java ${javaVersion} detected.`);
  }
}

console.log('\n📖 Next steps:');
console.log('   1. Configure the MCP server in your IDE:');
console.log('      - Cursor: Settings → Features → MCP');
console.log('      - Claude Code: Settings → MCP');
console.log('      - Antigravity: See documentation');
console.log('   2. Add this configuration:');
console.log('      {');
console.log('        "command": "npx",');
console.log('        "args": ["-y", "@jakarta-migration/mcp-server"]');
console.log('      }');
console.log('\n📚 Documentation: See README.md for detailed setup instructions.\n');


#!/usr/bin/env node

/**
 * Version sync check script
 * Ensures package.json version matches gradle.properties version
 * before publishing to npm.
 */

const fs = require('fs');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');
const packageJsonPath = path.join(rootDir, 'package.json');
const gradlePropertiesPath = path.join(rootDir, 'gradle.properties');

function getPackageJsonVersion() {
  const content = fs.readFileSync(packageJsonPath, 'utf8');
  const pkg = JSON.parse(content);
  return pkg.version;
}

function getGradlePropertiesVersion() {
  const content = fs.readFileSync(gradlePropertiesPath, 'utf8');
  // Find the first line that starts with version= (not a comment)
  for (const line of content.split('\n')) {
    const trimmed = line.trim();
    if (trimmed.startsWith('version=')) {
      return trimmed.split('=')[1].trim();
    }
  }
  throw new Error('version= not found in gradle.properties');
}

const npmVersion = getPackageJsonVersion();
const gradleVersion = getGradlePropertiesVersion();

if (npmVersion !== gradleVersion) {
  console.error(`❌ Version mismatch!`);
  console.error(`   package.json:  ${npmVersion}`);
  console.error(`   gradle.properties: ${gradleVersion}`);
  console.error(`\nRun: node scripts/sync-versions.js`);
  process.exit(1);
}

console.log(`✅ Versions in sync: ${npmVersion}`);
process.exit(0);

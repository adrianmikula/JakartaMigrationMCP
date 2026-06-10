#!/usr/bin/env node

/**
 * Version sync utility
 * Syncs package.json version to match gradle.properties version.
 * Run this after bumping the Gradle version.
 */

const fs = require('fs');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');
const packageJsonPath = path.join(rootDir, 'package.json');
const gradlePropertiesPath = path.join(rootDir, 'gradle.properties');

function getGradlePropertiesVersion() {
  const content = fs.readFileSync(gradlePropertiesPath, 'utf8');
  for (const line of content.split('\n')) {
    const trimmed = line.trim();
    if (trimmed.startsWith('version=')) {
      return trimmed.split('=')[1].trim();
    }
  }
  throw new Error('version= not found in gradle.properties');
}

function updatePackageJsonVersion(newVersion) {
  const content = fs.readFileSync(packageJsonPath, 'utf8');
  const pkg = JSON.parse(content);
  pkg.version = newVersion;
  fs.writeFileSync(packageJsonPath, JSON.stringify(pkg, null, 4) + '\n');
}

const gradleVersion = getGradlePropertiesVersion();
updatePackageJsonVersion(gradleVersion);
console.log(`✅ package.json updated to version ${gradleVersion}`);

#!/usr/bin/env node

/**
 * Test suite for configuration file loading functionality
 * 
 * Tests that the npm wrapper correctly loads environment variables
 * from the jakarta-migration-license.json configuration file.
 */

const fs = require('fs');
const path = require('path');
const os = require('os');
const assert = require('assert');

// Import the function to test
const { loadConfiguration } = require('./index.js');

// Test configuration
const testDir = path.join(os.tmpdir(), 'jakarta-migration-test');
const testConfigFile = path.join(testDir, 'jakarta-migration-license.json');

// Test results
let testsPassed = 0;
let testsFailed = 0;

function runTest(testName, testFn) {
  try {
    testFn();
    testsPassed++;
    console.log(`✅ ${testName}`);
  } catch (error) {
    testsFailed++;
    console.error(`❌ ${testName}`);
    console.error(`   Error: ${error.message}`);
  }
}

function setupTestEnvironment() {
  // Create test directory
  if (!fs.existsSync(testDir)) {
    fs.mkdirSync(testDir, { recursive: true });
  }
  
  // Clear any existing environment variables that might interfere
  delete process.env.TEST_LICENSE_KEY;
  delete process.env.TEST_STRIPE_KEY;
  delete process.env.TEST_APIFY_TOKEN;
}

function cleanupTestEnvironment() {
  // Clean up test files
  if (fs.existsSync(testConfigFile)) {
    fs.unlinkSync(testConfigFile);
  }
  if (fs.existsSync(testDir)) {
    fs.rmdirSync(testDir);
  }
  
  // Restore environment
  delete process.env.TEST_LICENSE_KEY;
  delete process.env.TEST_STRIPE_KEY;
  delete process.env.TEST_APIFY_TOKEN;
  delete process.env.comment;
}

// Test 1: Load valid configuration file
function testLoadValidConfiguration() {
  setupTestEnvironment();
  
  const config = {
    environment: {
      TEST_LICENSE_KEY: "sub_test_1234567890",
      TEST_STRIPE_KEY: "sk_test_abc123",
      TEST_APIFY_TOKEN: "apify_api_test_token"
    }
  };
  
  fs.writeFileSync(testConfigFile, JSON.stringify(config, null, 2));
  
  const loaded = loadConfiguration(testConfigFile);
  
  assert.strictEqual(loaded, true, "Should return true when config is loaded");
  assert.strictEqual(process.env.TEST_LICENSE_KEY, "sub_test_1234567890", "Should set TEST_LICENSE_KEY");
  assert.strictEqual(process.env.TEST_STRIPE_KEY, "sk_test_abc123", "Should set TEST_STRIPE_KEY");
  assert.strictEqual(process.env.TEST_APIFY_TOKEN, "apify_api_test_token", "Should set TEST_APIFY_TOKEN");
  
  cleanupTestEnvironment();
}

// Test 2: Handle missing configuration file gracefully
function testHandleMissingConfiguration() {
  setupTestEnvironment();
  
  const nonExistentFile = path.join(testDir, 'non-existent.json');
  const loaded = loadConfiguration(nonExistentFile);
  
  assert.strictEqual(loaded, false, "Should return false when file doesn't exist");
  
  cleanupTestEnvironment();
}

// Test 3: Handle invalid JSON gracefully
function testHandleInvalidJSON() {
  setupTestEnvironment();
  
  fs.writeFileSync(testConfigFile, "invalid json content {");
  
  const loaded = loadConfiguration(testConfigFile);
  
  assert.strictEqual(loaded, false, "Should return false for invalid JSON");
  
  cleanupTestEnvironment();
}

// Test 4: Handle missing environment section
function testHandleMissingEnvironmentSection() {
  setupTestEnvironment();
  
  const config = {
    description: "Test config without environment section"
  };
  
  fs.writeFileSync(testConfigFile, JSON.stringify(config, null, 2));
  
  const loaded = loadConfiguration(testConfigFile);
  
  assert.strictEqual(loaded, false, "Should return false when environment section is missing");
  
  cleanupTestEnvironment();
}

// Test 5: System environment variables take precedence
function testSystemEnvVarsTakePrecedence() {
  setupTestEnvironment();
  
  // Set system environment variable
  process.env.TEST_LICENSE_KEY = "system_override_value";
  
  const config = {
    environment: {
      TEST_LICENSE_KEY: "config_file_value",
      TEST_STRIPE_KEY: "sk_test_from_config"
    }
  };
  
  fs.writeFileSync(testConfigFile, JSON.stringify(config, null, 2));
  
  loadConfiguration(testConfigFile);
  
  // System env var should not be overwritten
  assert.strictEqual(process.env.TEST_LICENSE_KEY, "system_override_value", "System env var should take precedence");
  // Config file value should be set for other vars
  assert.strictEqual(process.env.TEST_STRIPE_KEY, "sk_test_from_config", "Config file value should be used when system var doesn't exist");
  
  cleanupTestEnvironment();
}

// Test 6: Convert values to strings
function testConvertValuesToStrings() {
  setupTestEnvironment();
  
  const config = {
    environment: {
      TEST_NUMBER: 12345,
      TEST_BOOLEAN: true,
      TEST_STRING: "test_value"
    }
  };
  
  fs.writeFileSync(testConfigFile, JSON.stringify(config, null, 2));
  
  loadConfiguration(testConfigFile);
  
  assert.strictEqual(typeof process.env.TEST_NUMBER, "string", "Numbers should be converted to strings");
  assert.strictEqual(process.env.TEST_NUMBER, "12345", "Number value should be correct");
  assert.strictEqual(process.env.TEST_BOOLEAN, "true", "Boolean should be converted to string");
  assert.strictEqual(process.env.TEST_STRING, "test_value", "String values should be preserved");
  
  cleanupTestEnvironment();
}

// Test 7: Ignore comment keys in environment section
function testIgnoreCommentKeys() {
  setupTestEnvironment();
  
  const config = {
    environment: {
      comment: "This should not be set as an environment variable",
      TEST_LICENSE_KEY: "sub_test_1234567890",
      _comment_test: "This should also be ignored"
    }
  };
  
  fs.writeFileSync(testConfigFile, JSON.stringify(config, null, 2));
  
  loadConfiguration(testConfigFile);
  
  // Comment keys should not be set as environment variables
  assert.strictEqual(process.env.comment, undefined, "Comment key should not be set as environment variable");
  assert.strictEqual(process.env._comment_test, undefined, "Keys starting with _comment_ should not be set");
  // Regular keys should still be set
  assert.strictEqual(process.env.TEST_LICENSE_KEY, "sub_test_1234567890", "Regular keys should still be set");
  
  cleanupTestEnvironment();
}

// Test 8: Empty string environment variables take precedence
function testEmptyStringEnvVarsTakePrecedence() {
  setupTestEnvironment();
  
  // Set system environment variable to empty string (explicitly disable)
  process.env.TEST_STRIPE_KEY = "";
  
  const config = {
    environment: {
      TEST_STRIPE_KEY: "sk_test_from_config_should_not_override",
      TEST_APIFY_TOKEN: "apify_token_from_config"
    }
  };
  
  fs.writeFileSync(testConfigFile, JSON.stringify(config, null, 2));
  
  loadConfiguration(testConfigFile);
  
  // Empty string should be preserved (not overridden by config file)
  assert.strictEqual(process.env.TEST_STRIPE_KEY, "", "Empty string env var should take precedence and not be overridden");
  // Config file value should be set for other vars
  assert.strictEqual(process.env.TEST_APIFY_TOKEN, "apify_token_from_config", "Config file value should be used when system var doesn't exist");
  
  cleanupTestEnvironment();
}

// Run all tests
console.log("Running configuration loading tests...\n");

runTest("Load valid configuration file", testLoadValidConfiguration);
runTest("Handle missing configuration file gracefully", testHandleMissingConfiguration);
runTest("Handle invalid JSON gracefully", testHandleInvalidJSON);
runTest("Handle missing environment section", testHandleMissingEnvironmentSection);
runTest("System environment variables take precedence", testSystemEnvVarsTakePrecedence);
runTest("Convert values to strings", testConvertValuesToStrings);
runTest("Ignore comment keys in environment section", testIgnoreCommentKeys);
runTest("Empty string environment variables take precedence", testEmptyStringEnvVarsTakePrecedence);

// Summary
console.log("\n" + "=".repeat(50));
console.log(`Tests passed: ${testsPassed}`);
console.log(`Tests failed: ${testsFailed}`);
console.log("=".repeat(50));

if (testsFailed === 0) {
  console.log("\n✅ All tests passed!");
  process.exit(0);
} else {
  console.log("\n❌ Some tests failed!");
  process.exit(1);
}


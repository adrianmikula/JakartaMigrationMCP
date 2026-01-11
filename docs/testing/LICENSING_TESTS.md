# Licensing Tests

## Overview

Tests to verify that free tools are accessible without a license and premium tools require a PREMIUM license.

## Test Coverage

### Free Tools Tests (Should Work Without License)

All free tools are tested to ensure they work with COMMUNITY tier (no premium license):

1. **`analyzeJakartaReadiness`** - ✅ Tested
   - Verifies tool works with COMMUNITY tier
   - No license check should occur

2. **`detectBlockers`** - ✅ Tested
   - Verifies tool works with COMMUNITY tier
   - No license check should occur

3. **`recommendVersions`** - ✅ Tested
   - Verifies tool works with COMMUNITY tier
   - No license check should occur

4. **`analyzeMigrationImpact`** - ✅ Tested
   - Verifies tool works with COMMUNITY tier
   - No license check should occur (analysis tool)

### Premium Tools Tests (Should Require License)

All premium tools are tested to ensure they:
- Return `upgrade_required` response without PREMIUM license
- Work correctly with PREMIUM license

1. **`createMigrationPlan`** - ✅ Tested
   - Without license: Returns `upgrade_required` with COMMUNITY tier
   - With PREMIUM license: Creates migration plan successfully

2. **`verifyRuntime`** - ✅ Tested
   - Without license: Returns `upgrade_required` with COMMUNITY tier
   - With PREMIUM license: Executes successfully

3. **`refactorProject`** - ✅ Tested
   - Without license: Returns `upgrade_required` with COMMUNITY tier
   - With PREMIUM license: Refactors files successfully and modifies source code

## Test Structure

Tests are located in: `src/test/java/unit/jakartamigration/mcp/JakartaMigrationToolsTest.java`

### Test Methods

**Free Tools:**
- `freeToolsShouldWorkWithoutPremiumLicense_analyzeJakartaReadiness()`
- `freeToolsShouldWorkWithoutPremiumLicense_detectBlockers()`
- `freeToolsShouldWorkWithoutPremiumLicense_recommendVersions()`
- `freeToolsShouldWorkWithoutPremiumLicense_analyzeMigrationImpact()`

**Premium Tools:**
- `premiumToolsShouldRequirePremiumLicense_createMigrationPlanWithoutLicense()`
- `premiumToolsShouldWorkWithPremiumLicense_createMigrationPlan()`
- `premiumToolsShouldRequirePremiumLicense_verifyRuntimeWithoutLicense()`
- `premiumToolsShouldWorkWithPremiumLicense_verifyRuntime()`
- `premiumToolsShouldRequirePremiumLicense_refactorProjectWithoutLicense()`
- `premiumToolsShouldWorkWithPremiumLicense_refactorProject()`

## Running the Tests

```bash
# Run all licensing tests
./gradlew test --tests "unit.jakartamigration.mcp.JakartaMigrationToolsTest.freeTools*" --no-daemon
./gradlew test --tests "unit.jakartamigration.mcp.JakartaMigrationToolsTest.premiumTools*" --no-daemon

# Run all JakartaMigrationTools tests
./gradlew test --tests "unit.jakartamigration.mcp.JakartaMigrationToolsTest" --no-daemon
```

## Test Assertions

### Free Tools
- ✅ Response contains `"status": "success"`
- ✅ Tool executes without license check
- ✅ No upgrade_required response

### Premium Tools (Without License)
- ✅ Response contains `"status": "upgrade_required"`
- ✅ Response contains `"currentTier": "COMMUNITY"`
- ✅ Response contains `"requiredTier": "PREMIUM"`
- ✅ Tool does NOT execute (verified with `never()`)

### Premium Tools (With License)
- ✅ Response contains `"status": "success"` (or appropriate success status)
- ✅ Tool executes successfully
- ✅ For `refactorProject`: Verifies files are actually modified

## Mocking

Tests use Mockito to mock:
- `FeatureFlagsService` - To simulate different license tiers
- `RefactoringEngine` - To simulate refactoring operations
- Other dependencies as needed

### License Tier Simulation

```java
// COMMUNITY tier (no premium license)
when(featureFlagsService.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(false);
when(featureFlagsService.getCurrentTier()).thenReturn(FeatureFlagsProperties.LicenseTier.COMMUNITY);

// PREMIUM tier
when(featureFlagsService.hasTier(FeatureFlagsProperties.LicenseTier.PREMIUM)).thenReturn(true);
when(featureFlagsService.getCurrentTier()).thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
```

## Expected Behavior

### Free Tools
- ✅ Always accessible (no license check)
- ✅ Work with COMMUNITY tier
- ✅ Work with PREMIUM tier
- ✅ Work with ENTERPRISE tier

### Premium Tools
- ❌ Return `upgrade_required` with COMMUNITY tier
- ✅ Work with PREMIUM tier
- ✅ Work with ENTERPRISE tier


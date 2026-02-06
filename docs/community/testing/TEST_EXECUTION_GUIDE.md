# Test Execution Guide - Stripe & File Storage Features

## Quick Start

### Option 1: Using Gradle Wrapper (Recommended)

```powershell
# Run all new tests
.\gradlew.bat test --tests "adrianmikula.jakartamigration.storage.*" --tests "adrianmikula.jakartamigration.api.service.StripePaymentLinkServiceTest" --tests "adrianmikula.jakartamigration.config.StripeEmailValidationTest"

# Run all tests
.\gradlew.bat test
```

### Option 2: Using Test Script

```powershell
# Run all tests
.\scripts\gradle-test.ps1

# Run unit tests only
.\scripts\gradle-test.ps1 -UnitOnly
```

### Option 3: Using Mise

```bash
# Run all tests
mise run test

# Run unit tests only
mise run test-unit
```

## Test Files to Verify

### 1. File Storage Tests
```powershell
.\gradlew.bat test --tests "adrianmikula.jakartamigration.storage.service.LocalLicenseStorageServiceTest"
```

**Expected**: 9 tests passing
- Store and retrieve by email
- Store and retrieve by license key
- Expiration handling
- Case-insensitive emails
- Session updates
- Deletion
- Null handling
- Default TTL

### 2. Payment Links Tests
```powershell
.\gradlew.bat test --tests "adrianmikula.jakartamigration.api.service.StripePaymentLinkServiceTest"
```

**Expected**: 10 tests passing
- Get payment link for product
- Handle unknown products
- Case-insensitive matching
- Null/empty handling
- Get all payment links
- Check if link exists

### 3. Email Validation Tests
```powershell
.\gradlew.bat test --tests "adrianmikula.jakartamigration.config.StripeEmailValidationTest"
```

**Expected**: 6 tests passing
- Null/blank email handling
- Invalid email format
- Customer not found
- API error handling
- Caching behavior

### 4. License Service Tests (Updated)
```powershell
.\gradlew.bat test --tests "adrianmikula.jakartamigration.config.LicenseServiceTest"
```

**Expected**: 9 tests passing
- All existing tests with updated constructor

## Verification Steps

### Step 1: Check Compilation
```powershell
.\gradlew.bat compileTestJava
```

**Expected**: No compilation errors

### Step 2: Run Tests
```powershell
.\gradlew.bat test
```

**Expected**: All tests pass

### Step 3: Check Coverage
```powershell
.\gradlew.bat jacocoTestReport
```

**Expected**: Coverage report generated at `build/reports/jacoco/test/html/index.html`

## Troubleshooting

### Issue: Gradle Wrapper Not Found

**Solution**:
```powershell
# Install Gradle first, then:
gradle wrapper --gradle-version 8.5

# Or use setup script:
.\scripts\setup.ps1
```

### Issue: Tests Fail with File Lock Errors

**Solution**: Close any processes that might have the test file open (IDE, other test runs)

### Issue: SQLite Errors (Should Not Occur)

**Solution**: SQLite has been removed. If you see SQLite errors, check that:
- `build.gradle.kts` doesn't have SQLite dependencies
- `application.yml` doesn't reference SQLite

## Test Results Summary

After running tests, you should see:

```
✅ LocalLicenseStorageServiceTest: 9/9
✅ StripePaymentLinkServiceTest: 10/10
✅ StripeEmailValidationTest: 6/6
✅ LicenseServiceTest: 9/9

Total: 34/34 tests passing
```

## Manual Testing

If Gradle is not available, you can manually verify:

1. **File Storage**:
   - Check that `LocalLicenseStorageService` compiles
   - Verify JSON file format is correct
   - Test file read/write operations

2. **Payment Links**:
   - Check that `StripePaymentLinkService` compiles
   - Verify configuration mapping works
   - Test product name lookup

3. **Email Validation**:
   - Check that `StripeLicenseService.validateLicenseByEmail()` compiles
   - Verify email format validation
   - Test caching logic

## Next Steps After Tests Pass

1. ✅ Review code coverage report
2. ✅ Check for any remaining linter warnings
3. ✅ Update documentation with test results
4. ✅ Prepare for integration testing


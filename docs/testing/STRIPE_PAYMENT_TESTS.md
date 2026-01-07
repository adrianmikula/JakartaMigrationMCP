# Stripe Payment Implementation Tests

## Overview

Comprehensive unit tests have been created for the Stripe payment processor implementation (`StripeLicenseService`).

## Test File

**Location**: `src/test/java/adrianmikula/jakartamigration/config/StripeLicenseServiceTest.java`

## Test Coverage

### 1. Basic Validation Tests (3 tests)
- ✅ Return null for null license key
- ✅ Return null for blank license key
- ✅ Return null for non-Stripe license keys

### 2. Simple Validation Tests (3 tests)
- ✅ Use simple validation when Stripe is disabled
- ✅ Use simple validation for enterprise test keys
- ✅ Return null for invalid simple validation keys

### 3. Error Handling Tests (5 tests)
- ✅ Return null when subscription not found (404)
- ✅ Return null when API key is invalid (401)
- ✅ Return null when API key is forbidden (403)
- ✅ Handle server errors gracefully (500)
- ✅ Handle generic exceptions gracefully

### 4. License Key Format Tests (5 tests)
- ✅ Recognize subscription ID format (`sub_...`)
- ✅ Recognize customer ID format (`cus_...`)
- ✅ Handle subscription ID with `stripe_` prefix
- ✅ Handle customer ID with `stripe_` prefix
- ✅ Return null for unknown key format

### 5. Caching Tests (2 tests)
- ✅ Clear cache for specific license key
- ✅ Clear all cache

### 6. Offline Validation Tests (2 tests)
- ✅ Fall back to simple validation when offline and allowed
- ✅ Return null when offline and offline validation not allowed

### 7. Configuration Tests (2 tests)
- ✅ Respect custom license key prefix
- ✅ Handle empty product IDs

**Total Test Methods**: 22

## Test Patterns

### WebClient Mocking

The tests use Mockito to mock the WebClient reactive chain:

```java
@Mock
private WebClient stripeWebClient;

@Mock
private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

@Mock
private WebClient.RequestHeadersSpec requestHeadersSpec;

@Mock
private WebClient.ResponseSpec responseSpec;
```

### Error Simulation

Tests simulate various HTTP error responses:

```java
WebClientResponseException notFoundException = WebClientResponseException.create(
    HttpStatus.NOT_FOUND.value(),
    "Not Found",
    null, null, null
);

when(responseSpec.bodyToMono(any(Class.class)))
    .thenReturn(Mono.error(notFoundException));
```

### Simple Validation Testing

Tests verify fallback validation when Stripe is disabled:

```java
when(properties.getEnabled()).thenReturn(false);
FeatureFlagsProperties.LicenseTier tier = service.validateLicense("stripe_PREMIUM-test-key");
assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
```

## Test Limitations

### WebClient Integration Testing

Full integration testing of WebClient responses (with actual subscription data) would require:
- **WireMock** or similar HTTP mock server
- **TestContainers** for full integration testing
- Actual Stripe test API keys and test subscriptions

The current tests focus on:
- ✅ Error handling paths
- ✅ Key format detection
- ✅ Configuration handling
- ✅ Caching behavior
- ✅ Offline validation fallback

### What's Not Fully Tested

Due to the complexity of mocking reactive WebClient chains with actual DTOs:
- Full subscription validation with real response data
- Full customer validation with real response data
- Tier determination from actual product/price mappings
- Retry logic with actual retries

**Note**: These would be better tested with integration tests using WireMock or actual Stripe test environment.

## Running the Tests

### Using Gradle

```powershell
# Run all Stripe tests
.\gradlew.bat test --tests "adrianmikula.jakartamigration.config.StripeLicenseServiceTest"

# Run all license service tests
.\gradlew.bat test --tests "*LicenseServiceTest"
```

### Using Mise

```bash
# Run all tests (includes Stripe tests)
mise run test

# Run only unit tests
mise run test-unit
```

## Expected Test Results

All 22 tests should pass:

```
✅ StripeLicenseServiceTest: 22/22 tests passing
```

## Test Statistics

- **Total Test Methods**: 22
- **Coverage Areas**:
  - Basic validation: 3 tests
  - Simple validation: 3 tests
  - Error handling: 5 tests
  - Key format detection: 5 tests
  - Caching: 2 tests
  - Offline validation: 2 tests
  - Configuration: 2 tests

## Integration Testing Recommendations

For full end-to-end testing of Stripe integration:

1. **Use WireMock** for HTTP mocking:
   ```java
   @WireMockTest
   class StripeLicenseServiceIntegrationTest {
       // Test with actual HTTP responses
   }
   ```

2. **Use Stripe Test Mode**:
   - Create test products and subscriptions
   - Use test API keys (`sk_test_...`)
   - Test with real Stripe API responses

3. **Test Tier Determination**:
   - Create test subscriptions with known product IDs
   - Verify tier mapping works correctly
   - Test price ID to tier mapping

## References

- [Stripe Setup Guide](../setup/STRIPE_SETUP.md)
- [Testing Standards](../standards/testing.md)
- [License API Tests](LICENSE_API_TESTS.md)


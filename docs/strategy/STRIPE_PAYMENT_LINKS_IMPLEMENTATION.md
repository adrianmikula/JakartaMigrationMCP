# Stripe Payment Links Implementation

## Overview

This document describes the implementation of Stripe Payment Links integration based on the requirements in `docs/research/stripe-licencing-options.md`. The implementation supports:

1. **Email-based License Validation** - Users can validate licenses using their email address
2. **Payment Links** - Pre-configured Stripe Payment Links for one-time purchases
3. **Freemium Model** - Free tier with limited tools, premium tools behind one-time payment

## Implementation Details

### 1. Email-Based License Validation

**Service**: `StripeLicenseService.validateLicenseByEmail(String email)`

**How it works**:
1. Validates email format (must contain "@")
2. Checks cache first (reduces API calls)
3. Calls Stripe API to find customer by email: `GET /customers?email={email}`
4. If customer found, validates their active subscriptions
5. Returns license tier (PREMIUM or ENTERPRISE) if valid

**Usage**:
```java
FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicenseByEmail("user@example.com");
```

**API Endpoint**:
```
GET /api/v1/licenses/email/{email}/validate
Authorization: Bearer {SERVER_API_KEY}
```

**Response**:
```json
{
  "valid": true,
  "tier": "PREMIUM",
  "message": "License validated successfully"
}
```

### 2. Payment Links Service

**Service**: `StripePaymentLinkService`

**Features**:
- Get payment link for a specific product
- Get all available payment links
- Check if payment link exists for a product

**Configuration** (in `application.yml`):
```yaml
jakarta:
  migration:
    stripe:
      payment-links:
        starter: https://buy.stripe.com/starter-link
        professional: https://buy.stripe.com/professional-link
        enterprise: https://buy.stripe.com/enterprise-link
        premium: https://buy.stripe.com/premium-link
```

**API Endpoints**:

#### Get Payment Link for Product
```
GET /api/v1/payment-links/{productName}
Authorization: Bearer {SERVER_API_KEY}
```

**Response**:
```json
{
  "success": true,
  "productName": "starter",
  "paymentLink": "https://buy.stripe.com/starter-link",
  "message": "Payment link retrieved successfully"
}
```

#### Get All Payment Links
```
GET /api/v1/payment-links
Authorization: Bearer {SERVER_API_KEY}
```

**Response**:
```json
{
  "starter": "https://buy.stripe.com/starter-link",
  "professional": "https://buy.stripe.com/professional-link",
  "enterprise": "https://buy.stripe.com/enterprise-link"
}
```

### 3. Updated Services

#### StripeLicenseService
- ✅ Added `validateLicenseByEmail(String email)` method
- ✅ Added email masking for logging
- ✅ Added customer lookup DTOs (`StripeCustomersListResponse`, `StripeCustomerResponse`)

#### LicenseService
- ✅ Added `validateLicenseByEmail(String email)` method
- ✅ Delegates to `StripeLicenseService` for email validation
- ✅ Added email masking for logging

#### StripeLicenseProperties
- ✅ Added `paymentLinks` map for payment link URLs
- ✅ Added `enableEmailValidation` flag (default: true)

### 4. API Endpoints

**New Endpoints**:
1. `GET /api/v1/licenses/email/{email}/validate` - Validate license by email
2. `GET /api/v1/payment-links/{productName}` - Get payment link for product
3. `GET /api/v1/payment-links` - Get all payment links

**Authentication**: All endpoints require Bearer token authentication via `SERVER_API_KEY`.

### 5. Configuration

**Environment Variables**:
```bash
# Stripe Configuration
STRIPE_VALIDATION_ENABLED=true
STRIPE_SECRET_KEY=sk_live_...  # or sk_test_... for testing
STRIPE_ENABLE_EMAIL_VALIDATION=true

# Payment Links (configured in application.yml or via environment variables)
# Format: STRIPE_PAYMENT_LINK_STARTER=https://buy.stripe.com/...
```

**application.yml**:
```yaml
jakarta:
  migration:
    stripe:
      enabled: true
      secret-key: ${STRIPE_SECRET_KEY:}
      enable-email-validation: ${STRIPE_ENABLE_EMAIL_VALIDATION:true}
      payment-links:
        starter: ${STRIPE_PAYMENT_LINK_STARTER:}
        professional: ${STRIPE_PAYMENT_LINK_PROFESSIONAL:}
        enterprise: ${STRIPE_PAYMENT_LINK_ENTERPRISE:}
        premium: ${STRIPE_PAYMENT_LINK_PREMIUM:}
```

## Usage Examples

### Email Validation Flow

1. **User purchases via Stripe Payment Link**
   - User clicks payment link (e.g., `https://buy.stripe.com/starter-link`)
   - Completes payment on Stripe-hosted page
   - Customer record created in Stripe with email

2. **User validates license**
   ```java
   // In MCP server startup or tool execution
   String email = System.getenv("JAKARTA_MCP_LICENSE_EMAIL");
   FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicenseByEmail(email);
   
   if (tier == null) {
       // Show payment link
       String paymentLink = paymentLinkService.getPaymentLink("starter");
       log.info("Trial expired. Please purchase access here: {}", paymentLink);
   }
   ```

3. **User configures email**
   ```json
   // In mcpServers.json or environment variables
   {
     "jakarta-migration": {
       "env": {
         "JAKARTA_MCP_LICENSE_EMAIL": "user@example.com"
       }
     }
   }
   ```

### Payment Link Flow

1. **Get payment link for product**
   ```java
   String paymentLink = paymentLinkService.getPaymentLink("starter");
   // Returns: https://buy.stripe.com/starter-link
   ```

2. **Show payment link to user**
   ```java
   if (!hasValidLicense) {
       String paymentLink = paymentLinkService.getPaymentLink("starter");
       return "Trial expired. Please purchase access here: " + paymentLink;
   }
   ```

## Freemium Model

Based on the requirements, the model supports:

### Free Tier (Community)
- ✅ Core tools available for free
- ✅ Limited usage (can be enforced via credit system)
- ✅ No payment required

### Premium Tier (One-Time Purchase)
- ✅ Advanced tools behind payment
- ✅ One-time purchase (no subscription)
- ✅ Email-based validation
- ✅ Payment via Stripe Payment Links

## Pricing Recommendations

Based on `docs/research/stripe-licencing-options.md`:

| Package | Credits | Price | Cost per Tool Call |
|---------|---------|-------|-------------------|
| **Starter** | 50 | $5 | $0.10 |
| **Professional** | 200 | $15 | $0.075 |
| **Enterprise** | 1000 | $50 | $0.05 |

**Note**: These are recommendations. Actual pricing should be configured in Stripe Dashboard.

## Security Considerations

1. **Email Privacy**: Emails are masked in logs (e.g., `us***@example.com`)
2. **API Authentication**: All endpoints require Bearer token
3. **Stripe API Security**: Uses Stripe secret key for API calls
4. **Caching**: Results are cached to reduce API calls and improve performance

## Testing

### Test Email Validation
```java
// Test with Stripe test mode
String testEmail = "test@example.com";
FeatureFlagsProperties.LicenseTier tier = licenseService.validateLicenseByEmail(testEmail);
```

### Test Payment Links
```java
// Test payment link retrieval
String paymentLink = paymentLinkService.getPaymentLink("starter");
assertThat(paymentLink).isNotNull();
```

## Future Enhancements

1. **SQLite Local Storage** (Optional)
   - Store verified sessions locally for offline support
   - Cache email-to-tier mappings locally
   - Reduce API calls for local stdio mode

2. **Webhook Integration**
   - Listen for `customer.created` events
   - Automatically validate licenses when customers are created
   - Update credit balances via webhooks

3. **Credit System Integration**
   - Link email validation to credit system
   - Initialize credits when email is validated
   - Sync credits with Stripe subscriptions

## References

- [Stripe Payment Links Documentation](https://stripe.com/docs/payment-links)
- [Stripe Customer API](https://stripe.com/docs/api/customers)
- [Original Requirements](docs/research/stripe-licencing-options.md)


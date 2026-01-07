# License API Implementation Summary

## Overview

License API endpoints have been successfully implemented in the existing MCP server, following **Option 1** from the Railway License Server guide. This provides a simple, integrated solution that can be easily split into a separate service later if needed.

**Payment Processor**: Stripe is the primary payment processor for license validation. Apify support is deprecated and disabled by default.

## Implementation Status

✅ **All components implemented and ready for use**

## Components Created

### 1. DTOs (Data Transfer Objects)

Located in `src/main/java/adrianmikula/jakartamigration/api/dto/`:

- **LicenseValidationResponse** - Response for license validation
- **CreditBalanceResponse** - Response for credit balance queries
- **ConsumeCreditsRequest** - Request for consuming credits
- **ConsumeCreditsResponse** - Response for credit consumption
- **SyncCreditsResponse** - Response for credit synchronization

### 2. CreditService

Located in `src/main/java/adrianmikula/jakartamigration/api/service/CreditService.java`:

- In-memory credit balance storage (can be upgraded to PostgreSQL later)
- Credit consumption with transaction tracking
- Credit synchronization support
- Thread-safe operations using ConcurrentHashMap

**Features:**
- Get credit balance for a license key
- Consume credits with transaction ID generation
- Sync credits from external sources (Stripe)
- Initialize credits for new licenses

### 3. LicenseApiController

Located in `src/main/java/adrianmikula/jakartamigration/api/controller/LicenseApiController.java`:

REST API endpoints:
- `GET /api/v1/licenses/{licenseKey}/validate` - Validate license key
- `GET /api/v1/credits/{licenseKey}/balance` - Get credit balance
- `POST /api/v1/credits/{licenseKey}/consume` - Consume credits
- `POST /api/v1/credits/{licenseKey}/sync` - Sync credits from Stripe

**Authentication:**
- All endpoints require `Authorization: Bearer {SERVER_API_KEY}` header
- API key configured via `LICENSE_API_SERVER_API_KEY` environment variable

### 4. Configuration

Added to `src/main/resources/application.yml`:

```yaml
jakarta:
  migration:
    license-api:
      server-api-key: ${LICENSE_API_SERVER_API_KEY:}
      enabled: ${LICENSE_API_ENABLED:true}
```

## API Endpoints

### 1. Validate License Key

```http
GET /api/v1/licenses/{licenseKey}/validate
Authorization: Bearer {SERVER_API_KEY}
```

**Response:**
```json
{
  "valid": true,
  "tier": "PREMIUM",
  "expiresAt": null
}
```

### 2. Get Credit Balance

```http
GET /api/v1/credits/{licenseKey}/balance
Authorization: Bearer {SERVER_API_KEY}
```

**Response:**
```json
{
  "balance": 48,
  "lastSync": "2026-01-07T10:30:00Z"
}
```

### 3. Consume Credits

```http
POST /api/v1/credits/{licenseKey}/consume
Authorization: Bearer {SERVER_API_KEY}
Content-Type: application/json

{
  "amount": 2,
  "tool": "createMigrationPlan",
  "timestamp": "2026-01-07T10:31:00Z"
}
```

**Response:**
```json
{
  "success": true,
  "newBalance": 46,
  "transactionId": "txn_abc123"
}
```

### 4. Sync Credits

```http
POST /api/v1/credits/{licenseKey}/sync
Authorization: Bearer {SERVER_API_KEY}
```

**Response:**
```json
{
  "balance": 50,
  "synced": true,
  "lastSync": "2026-01-07T10:32:00Z"
}
```

## Deployment

### Railway Configuration

The license API endpoints are automatically available when the MCP server is deployed to Railway with the `mcp-streamable-http` profile (which is the default in `railway.json`).

**Environment Variables to Set:**

```bash
# Required: API key for authenticating requests
LICENSE_API_SERVER_API_KEY=your-secret-api-key-here

# Optional: Enable/disable license API (default: true)
LICENSE_API_ENABLED=true
```

**Generate API Key:**
```bash
# Generate a secure random API key
openssl rand -hex 32
```

### Local Testing

To test locally with web server enabled:

```bash
# Run with streamable-http profile
java -jar build/libs/jakarta-migration-mcp-*.jar --spring.profiles.active=mcp-streamable-http

# Or set environment variable
export LICENSE_API_SERVER_API_KEY=your-test-key
export SPRING_PROFILES_ACTIVE=mcp-streamable-http
java -jar build/libs/jakarta-migration-mcp-*.jar
```

**Test Endpoints:**
```bash
# Validate license
curl -H "Authorization: Bearer your-test-key" \
  http://localhost:8080/api/v1/licenses/PREMIUM-TEST/validate

# Get balance
curl -H "Authorization: Bearer your-test-key" \
  http://localhost:8080/api/v1/credits/PREMIUM-TEST/balance

# Consume credits
curl -X POST \
  -H "Authorization: Bearer your-test-key" \
  -H "Content-Type: application/json" \
  -d '{"amount": 2, "tool": "createMigrationPlan"}' \
  http://localhost:8080/api/v1/credits/PREMIUM-TEST/consume
```

## Integration with Local MCP Server

To use the license API from your local MCP server, update `CreditService` to call the Railway API:

```java
@Service
public class CreditService {
    private final WebClient licenseApiClient;
    private final String licenseServerUrl = "https://your-railway-app.railway.app";
    private final String serverApiKey = System.getenv("LICENSE_API_SERVER_API_KEY");
    
    public int getBalance(String licenseKey) {
        return licenseApiClient
            .get()
            .uri("{licenseServerUrl}/api/v1/credits/{licenseKey}/balance", 
                 licenseServerUrl, licenseKey)
            .header("Authorization", "Bearer " + serverApiKey)
            .retrieve()
            .bodyToMono(CreditBalanceResponse.class)
            .map(CreditBalanceResponse::getBalance)
            .block();
    }
    
    public boolean consumeCredits(String licenseKey, int amount, String tool) {
        return licenseApiClient
            .post()
            .uri("{licenseServerUrl}/api/v1/credits/{licenseKey}/consume", 
                 licenseServerUrl, licenseKey)
            .header("Authorization", "Bearer " + serverApiKey)
            .bodyValue(new ConsumeCreditsRequest(amount, tool))
            .retrieve()
            .bodyToMono(ConsumeCreditsResponse.class)
            .map(ConsumeCreditsResponse::isSuccess)
            .block();
    }
}
```

## Credit Initialization

When a license is first validated:
- **PREMIUM tier**: Gets 50 initial credits
- **ENTERPRISE tier**: Gets 100 initial credits
- **COMMUNITY tier**: Gets 0 credits (free tier)

## Storage

**Current Implementation:**
- In-memory storage using ConcurrentHashMap
- Thread-safe operations
- Data is lost on server restart

**Future Upgrade (PostgreSQL):**
- Replace in-memory storage with JPA repositories
- Use Railway's PostgreSQL service
- Persist credit balances and transactions
- See `docs/strategy/RAILWAY_LICENSE_SERVER.md` for database schema

## Security

**API Authentication:**
- Bearer token authentication required for all endpoints
- API key should be strong and randomly generated
- Store securely in Railway environment variables

**Recommendations:**
- Use HTTPS in production (Railway provides this automatically)
- Rotate API keys periodically
- Monitor API usage and set up rate limiting if needed

## Next Steps

1. **Deploy to Railway:**
   - Set `LICENSE_API_SERVER_API_KEY` environment variable
   - Deploy using existing Railway configuration
   - Test endpoints with curl or Postman

2. **Integrate with Local MCP:**
   - Update local MCP `CreditService` to call Railway API
   - Test end-to-end flow

3. **Upgrade to PostgreSQL (Optional):**
   - Add PostgreSQL dependency to `build.gradle.kts`
   - Create JPA entities for credits and transactions
   - Update `CreditService` to use repositories
   - Add Liquibase migrations for schema

4. **Stripe Integration (Optional):**
   - Implement Stripe webhook handler for credit updates
   - Update `syncCredits` endpoint to query Stripe API
   - Add credit purchase flow

## Testing Checklist

- [x] DTOs created and validated
- [x] CreditService implemented with in-memory storage
- [x] LicenseApiController with all endpoints
- [x] API authentication configured
- [x] Application configuration updated
- [ ] Deploy to Railway and test endpoints
- [ ] Integrate with local MCP server
- [ ] End-to-end testing with credit consumption
- [ ] Load testing (if needed)

## Files Modified/Created

**Created:**
- `src/main/java/adrianmikula/jakartamigration/api/dto/*.java` (5 files)
- `src/main/java/adrianmikula/jakartamigration/api/service/CreditService.java`
- `src/main/java/adrianmikula/jakartamigration/api/controller/LicenseApiController.java`

**Modified:**
- `src/main/resources/application.yml` (added license-api configuration)

## References

- [Railway License Server Guide](RAILWAY_LICENSE_SERVER.md)
- [Deployment Strategy](DEPLOYMENT_MONETIZATION_STRATEGY.md)


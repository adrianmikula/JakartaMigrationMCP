package adrianmikula.jakartamigration.api.controller;

import adrianmikula.jakartamigration.api.dto.ConsumeCreditsRequest;
import adrianmikula.jakartamigration.api.service.CreditService;
import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import adrianmikula.jakartamigration.config.LicenseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for LicenseApiController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LicenseApiController Unit Tests")
class LicenseApiControllerTest {

    @Mock
    private LicenseService licenseService;

    @Mock
    private CreditService creditService;

    @InjectMocks
    private LicenseApiController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private static final String TEST_API_KEY = "test-api-key-123";
    private static final String TEST_LICENSE_KEY = "PREMIUM-test-key-123";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        objectMapper = new ObjectMapper();
        
        // Set API key using reflection
        ReflectionTestUtils.setField(controller, "serverApiKey", TEST_API_KEY);
    }

    // ========== Validate License Tests ==========

    @Test
    @DisplayName("Should validate license successfully")
    void shouldValidateLicenseSuccessfully() throws Exception {
        // Given
        when(licenseService.validateLicense(TEST_LICENSE_KEY))
                .thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        when(creditService.getBalance(TEST_LICENSE_KEY)).thenReturn(0);

        // When & Then
        mockMvc.perform(get("/api/v1/licenses/{licenseKey}/validate", TEST_LICENSE_KEY)
                        .header("Authorization", "Bearer " + TEST_API_KEY)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.tier").value("PREMIUM"));

        verify(licenseService, times(1)).validateLicense(TEST_LICENSE_KEY);
    }

    @Test
    @DisplayName("Should return invalid when license key is invalid")
    void shouldReturnInvalidWhenLicenseKeyIsInvalid() throws Exception {
        // Given
        when(licenseService.validateLicense(TEST_LICENSE_KEY)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/v1/licenses/{licenseKey}/validate", TEST_LICENSE_KEY)
                        .header("Authorization", "Bearer " + TEST_API_KEY)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Invalid license key"));

        verify(licenseService, times(1)).validateLicense(TEST_LICENSE_KEY);
    }

    @Test
    @DisplayName("Should return unauthorized when API key is missing")
    void shouldReturnUnauthorizedWhenApiKeyIsMissing() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/licenses/{licenseKey}/validate", TEST_LICENSE_KEY)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized: Invalid API key"));

        verify(licenseService, never()).validateLicense(anyString());
    }

    @Test
    @DisplayName("Should return unauthorized when API key is invalid")
    void shouldReturnUnauthorizedWhenApiKeyIsInvalid() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/licenses/{licenseKey}/validate", TEST_LICENSE_KEY)
                        .header("Authorization", "Bearer wrong-key")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized: Invalid API key"));

        verify(licenseService, never()).validateLicense(anyString());
    }

    @Test
    @DisplayName("Should initialize credits for premium tier on first validation")
    void shouldInitializeCreditsForPremiumTierOnFirstValidation() throws Exception {
        // Given
        when(licenseService.validateLicense(TEST_LICENSE_KEY))
                .thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        when(creditService.getBalance(TEST_LICENSE_KEY)).thenReturn(0);

        // When & Then
        mockMvc.perform(get("/api/v1/licenses/{licenseKey}/validate", TEST_LICENSE_KEY)
                        .header("Authorization", "Bearer " + TEST_API_KEY)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(creditService, times(1)).initializeCredits(TEST_LICENSE_KEY, 50);
    }

    @Test
    @DisplayName("Should initialize credits for enterprise tier on first validation")
    void shouldInitializeCreditsForEnterpriseTierOnFirstValidation() throws Exception {
        // Given
        when(licenseService.validateLicense(TEST_LICENSE_KEY))
                .thenReturn(FeatureFlagsProperties.LicenseTier.ENTERPRISE);
        when(creditService.getBalance(TEST_LICENSE_KEY)).thenReturn(0);

        // When & Then
        mockMvc.perform(get("/api/v1/licenses/{licenseKey}/validate", TEST_LICENSE_KEY)
                        .header("Authorization", "Bearer " + TEST_API_KEY)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(creditService, times(1)).initializeCredits(TEST_LICENSE_KEY, 100);
    }

    @Test
    @DisplayName("Should handle exception during license validation")
    void shouldHandleExceptionDuringLicenseValidation() throws Exception {
        // Given
        when(licenseService.validateLicense(TEST_LICENSE_KEY))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        mockMvc.perform(get("/api/v1/licenses/{licenseKey}/validate", TEST_LICENSE_KEY)
                        .header("Authorization", "Bearer " + TEST_API_KEY)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    // ========== Get Credit Balance Tests ==========

    @Test
    @DisplayName("Should get credit balance successfully")
    void shouldGetCreditBalanceSuccessfully() throws Exception {
        // Given
        when(creditService.getBalance(TEST_LICENSE_KEY)).thenReturn(50);
        when(creditService.getLastSync(TEST_LICENSE_KEY)).thenReturn(Instant.now());

        // When & Then
        mockMvc.perform(get("/api/v1/credits/{licenseKey}/balance", TEST_LICENSE_KEY)
                        .header("Authorization", "Bearer " + TEST_API_KEY)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50))
                .andExpect(jsonPath("$.lastSync").exists());

        verify(creditService, times(1)).getBalance(TEST_LICENSE_KEY);
    }

    @Test
    @DisplayName("Should return unauthorized when getting balance without API key")
    void shouldReturnUnauthorizedWhenGettingBalanceWithoutApiKey() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/credits/{licenseKey}/balance", TEST_LICENSE_KEY)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized: Invalid API key"));

        verify(creditService, never()).getBalance(anyString());
    }

    @Test
    @DisplayName("Should handle exception when getting balance")
    void shouldHandleExceptionWhenGettingBalance() throws Exception {
        // Given
        when(creditService.getBalance(TEST_LICENSE_KEY))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        mockMvc.perform(get("/api/v1/credits/{licenseKey}/balance", TEST_LICENSE_KEY)
                        .header("Authorization", "Bearer " + TEST_API_KEY)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").exists());
    }

    // ========== Consume Credits Tests ==========

    @Test
    @DisplayName("Should consume credits successfully")
    void shouldConsumeCreditsSuccessfully() throws Exception {
        // Given
        ConsumeCreditsRequest request = ConsumeCreditsRequest.builder()
                .amount(10)
                .tool("createMigrationPlan")
                .build();
        when(creditService.consumeCredits(TEST_LICENSE_KEY, 10, "createMigrationPlan"))
                .thenReturn("txn-123");
        when(creditService.getBalance(TEST_LICENSE_KEY)).thenReturn(40);

        // When & Then
        mockMvc.perform(post("/api/v1/credits/{licenseKey}/consume", TEST_LICENSE_KEY)
                        .header("Authorization", "Bearer " + TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.newBalance").value(40))
                .andExpect(jsonPath("$.transactionId").value("txn-123"));

        verify(creditService, times(1)).consumeCredits(TEST_LICENSE_KEY, 10, "createMigrationPlan");
    }

    @Test
    @DisplayName("Should return bad request when insufficient credits")
    void shouldReturnBadRequestWhenInsufficientCredits() throws Exception {
        // Given
        ConsumeCreditsRequest request = ConsumeCreditsRequest.builder()
                .amount(100)
                .tool("createMigrationPlan")
                .build();
        when(creditService.consumeCredits(TEST_LICENSE_KEY, 100, "createMigrationPlan"))
                .thenReturn(null);
        when(creditService.getBalance(TEST_LICENSE_KEY)).thenReturn(50);

        // When & Then
        mockMvc.perform(post("/api/v1/credits/{licenseKey}/consume", TEST_LICENSE_KEY)
                        .header("Authorization", "Bearer " + TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Insufficient credits"));
    }

    @Test
    @DisplayName("Should return unauthorized when consuming credits without API key")
    void shouldReturnUnauthorizedWhenConsumingCreditsWithoutApiKey() throws Exception {
        // Given
        ConsumeCreditsRequest request = ConsumeCreditsRequest.builder()
                .amount(10)
                .tool("createMigrationPlan")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/credits/{licenseKey}/consume", TEST_LICENSE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized: Invalid API key"));

        verify(creditService, never()).consumeCredits(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Should return bad request when request validation fails")
    void shouldReturnBadRequestWhenRequestValidationFails() throws Exception {
        // Given - invalid request (missing required fields)
        ConsumeCreditsRequest request = ConsumeCreditsRequest.builder()
                .amount(null) // Required field is null
                .tool("createMigrationPlan")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/credits/{licenseKey}/consume", TEST_LICENSE_KEY)
                        .header("Authorization", "Bearer " + TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(creditService, never()).consumeCredits(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Should handle exception when consuming credits")
    void shouldHandleExceptionWhenConsumingCredits() throws Exception {
        // Given
        ConsumeCreditsRequest request = ConsumeCreditsRequest.builder()
                .amount(10)
                .tool("createMigrationPlan")
                .build();
        when(creditService.consumeCredits(TEST_LICENSE_KEY, 10, "createMigrationPlan"))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        mockMvc.perform(post("/api/v1/credits/{licenseKey}/consume", TEST_LICENSE_KEY)
                        .header("Authorization", "Bearer " + TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    // ========== Sync Credits Tests ==========

    @Test
    @DisplayName("Should sync credits successfully")
    void shouldSyncCreditsSuccessfully() throws Exception {
        // Given
        when(creditService.getBalance(TEST_LICENSE_KEY)).thenReturn(50);
        when(creditService.syncCredits(TEST_LICENSE_KEY, 50)).thenReturn(true);
        when(creditService.getLastSync(TEST_LICENSE_KEY)).thenReturn(Instant.now());

        // When & Then
        mockMvc.perform(post("/api/v1/credits/{licenseKey}/sync", TEST_LICENSE_KEY)
                        .header("Authorization", "Bearer " + TEST_API_KEY)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(true))
                .andExpect(jsonPath("$.balance").value(50))
                .andExpect(jsonPath("$.lastSync").exists());

        verify(creditService, times(1)).syncCredits(TEST_LICENSE_KEY, 50);
    }

    @Test
    @DisplayName("Should return unauthorized when syncing credits without API key")
    void shouldReturnUnauthorizedWhenSyncingCreditsWithoutApiKey() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/credits/{licenseKey}/sync", TEST_LICENSE_KEY)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized: Invalid API key"));

        verify(creditService, never()).syncCredits(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should handle exception when syncing credits")
    void shouldHandleExceptionWhenSyncingCredits() throws Exception {
        // Given
        when(creditService.getBalance(TEST_LICENSE_KEY))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        mockMvc.perform(post("/api/v1/credits/{licenseKey}/sync", TEST_LICENSE_KEY)
                        .header("Authorization", "Bearer " + TEST_API_KEY)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.synced").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    // ========== API Key Configuration Tests ==========

    @Test
    @DisplayName("Should allow requests when API key is not configured (development mode)")
    void shouldAllowRequestsWhenApiKeyIsNotConfigured() throws Exception {
        // Given
        ReflectionTestUtils.setField(controller, "serverApiKey", "");
        when(licenseService.validateLicense(TEST_LICENSE_KEY))
                .thenReturn(FeatureFlagsProperties.LicenseTier.PREMIUM);
        when(creditService.getBalance(TEST_LICENSE_KEY)).thenReturn(0);

        // When & Then
        mockMvc.perform(get("/api/v1/licenses/{licenseKey}/validate", TEST_LICENSE_KEY)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(licenseService, times(1)).validateLicense(TEST_LICENSE_KEY);
    }
}


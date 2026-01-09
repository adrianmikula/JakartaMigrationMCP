package adrianmikula.jakartamigration.storage.service;

import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LocalLicenseStorageService (file-based storage).
 */
@DisplayName("LocalLicenseStorageService File-Based Storage Tests")
class LocalLicenseStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalLicenseStorageService service;
    private Path testStorageFile;

    @BeforeEach
    void setUp() {
        testStorageFile = tempDir.resolve("test_license_sessions.json");
        
        // Create service with test file path directly
        // The constructor takes the file path as a parameter
        service = new LocalLicenseStorageService(testStorageFile.toString());
    }

    @Test
    @DisplayName("Should store and retrieve license session by email")
    void shouldStoreAndRetrieveSessionByEmail() {
        // When
        service.storeSession("test@example.com", "license_key_123", 
            FeatureFlagsProperties.LicenseTier.PREMIUM, 24L);

        // Then
        FeatureFlagsProperties.LicenseTier tier = service.getTierByEmail("test@example.com");
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    @DisplayName("Should store and retrieve license session by license key")
    void shouldStoreAndRetrieveSessionByLicenseKey() {
        // When
        service.storeSession("test@example.com", "license_key_123", 
            FeatureFlagsProperties.LicenseTier.ENTERPRISE, 24L);

        // Then
        FeatureFlagsProperties.LicenseTier tier = service.getTierByLicenseKey("license_key_123");
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.ENTERPRISE);
    }

    @Test
    @DisplayName("Should return null for non-existent email")
    void shouldReturnNullForNonExistentEmail() {
        // When
        FeatureFlagsProperties.LicenseTier tier = service.getTierByEmail("nonexistent@example.com");

        // Then
        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("Should return null for expired session")
    void shouldReturnNullForExpiredSession() {
        // Given - store with very short TTL
        service.storeSession("test@example.com", "license_key_123", 
            FeatureFlagsProperties.LicenseTier.PREMIUM, 0L); // 0 hours = expired immediately

        // When
        FeatureFlagsProperties.LicenseTier tier = service.getTierByEmail("test@example.com");

        // Then
        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("Should delete session by email")
    void shouldDeleteSessionByEmail() {
        // Given
        service.storeSession("test@example.com", "license_key_123", 
            FeatureFlagsProperties.LicenseTier.PREMIUM, 24L);

        // When
        service.deleteSession("test@example.com");

        // Then
        FeatureFlagsProperties.LicenseTier tier = service.getTierByEmail("test@example.com");
        assertThat(tier).isNull();
    }

    @Test
    @DisplayName("Should handle case-insensitive email")
    void shouldHandleCaseInsensitiveEmail() {
        // Given
        service.storeSession("Test@Example.com", "license_key_123", 
            FeatureFlagsProperties.LicenseTier.PREMIUM, 24L);

        // When/Then
        assertThat(service.getTierByEmail("test@example.com")).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(service.getTierByEmail("TEST@EXAMPLE.COM")).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    @DisplayName("Should update existing session")
    void shouldUpdateExistingSession() {
        // Given
        service.storeSession("test@example.com", "license_key_123", 
            FeatureFlagsProperties.LicenseTier.PREMIUM, 24L);

        // When - update with new tier
        service.storeSession("test@example.com", "license_key_456", 
            FeatureFlagsProperties.LicenseTier.ENTERPRISE, 24L);

        // Then
        FeatureFlagsProperties.LicenseTier tier = service.getTierByEmail("test@example.com");
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.ENTERPRISE);
        
        // License key should also be updated
        FeatureFlagsProperties.LicenseTier tierByKey = service.getTierByLicenseKey("license_key_456");
        assertThat(tierByKey).isEqualTo(FeatureFlagsProperties.LicenseTier.ENTERPRISE);
    }

    @Test
    @DisplayName("Should handle null email gracefully")
    void shouldHandleNullEmailGracefully() {
        // When/Then
        assertThat(service.getTierByEmail(null)).isNull();
        assertThat(service.getTierByEmail("")).isNull();
        assertThat(service.getTierByEmail("   ")).isNull();
    }

    @Test
    @DisplayName("Should use default TTL when not specified")
    void shouldUseDefaultTtlWhenNotSpecified() {
        // When
        service.storeSession("test@example.com", "license_key_123", 
            FeatureFlagsProperties.LicenseTier.PREMIUM, null);

        // Then - should still be valid (default is 24 hours)
        FeatureFlagsProperties.LicenseTier tier = service.getTierByEmail("test@example.com");
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    @DisplayName("Should serialize Instant fields to JSON without exceptions")
    void shouldSerializeInstantFieldsToJsonWithoutExceptions() {
        // Given
        service.storeSession("test@example.com", "license_key_123", 
            FeatureFlagsProperties.LicenseTier.PREMIUM, 24L);

        // When/Then - should not throw InvalidDefinitionException
        // Verify file exists and contains valid JSON
        assertThat(testStorageFile).exists();
        assertThat(Files.exists(testStorageFile)).isTrue();
        
        // Read and parse JSON to verify it's valid
        try {
            String jsonContent = Files.readString(testStorageFile);
            assertThat(jsonContent).isNotEmpty();
            
            // Parse JSON to verify it's valid
            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            JsonNode jsonNode = mapper.readTree(jsonContent);
            assertThat(jsonNode).isNotNull();
        } catch (Exception e) {
            throw new AssertionError("Failed to serialize Instant fields: " + e.getMessage(), e);
        }
    }

    @Test
    @DisplayName("Should serialize Instant fields in ISO-8601 format")
    void shouldSerializeInstantFieldsInIso8601Format() throws Exception {
        // Given
        service.storeSession("test@example.com", "license_key_123", 
            FeatureFlagsProperties.LicenseTier.PREMIUM, 24L);

        // When
        String jsonContent = Files.readString(testStorageFile);
        
        // Then - verify JSON contains ISO-8601 formatted dates
        // ISO-8601 format: "2024-01-01T12:00:00Z" or "2024-01-01T12:00:00.123Z"
        Pattern iso8601Pattern = Pattern.compile("\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?Z\"");
        
        assertThat(jsonContent)
            .contains("\"createdAt\"")
            .contains("\"expiresAt\"")
            .contains("\"lastAccessedAt\"");
        
        // Verify at least one ISO-8601 formatted date exists
        assertThat(iso8601Pattern.matcher(jsonContent).find())
            .as("JSON should contain ISO-8601 formatted Instant values")
            .isTrue();
    }

    @Test
    @DisplayName("Should deserialize Instant fields from JSON without exceptions")
    void shouldDeserializeInstantFieldsFromJsonWithoutExceptions() {
        // Given - store a session
        service.storeSession("test@example.com", "license_key_123", 
            FeatureFlagsProperties.LicenseTier.PREMIUM, 24L);

        // When - retrieve the session (this triggers deserialization)
        // Then - should not throw InvalidDefinitionException
        FeatureFlagsProperties.LicenseTier tier = service.getTierByEmail("test@example.com");
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
    }

    @Test
    @DisplayName("Should correctly deserialize all Instant fields (createdAt, expiresAt, lastAccessedAt)")
    void shouldCorrectlyDeserializeAllInstantFields() throws Exception {
        // Given
        service.storeSession("test@example.com", "license_key_123", 
            FeatureFlagsProperties.LicenseTier.PREMIUM, 24L);

        // When - read the JSON file directly and deserialize
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String jsonContent = Files.readString(testStorageFile);
        TypeReference<Map<String, Map<String, Object>>> typeRef = new TypeReference<>() {};
        Map<String, Map<String, Object>> sessions = mapper.readValue(jsonContent, typeRef);

        // Then - verify all Instant fields are present and valid
        Map<String, Object> session = sessions.get("test@example.com");
        assertThat(session).isNotNull();
        assertThat(session.get("createdAt")).isNotNull();
        assertThat(session.get("expiresAt")).isNotNull();
        assertThat(session.get("lastAccessedAt")).isNotNull();
        
        // Verify they are Instant instances (when deserialized with JavaTimeModule)
        // Note: When deserialized as Map<String, Object>, Instant becomes String
        // But the important thing is that deserialization didn't fail
        assertThat(session.get("createdAt")).isInstanceOf(String.class);
        assertThat(session.get("expiresAt")).isInstanceOf(String.class);
        assertThat(session.get("lastAccessedAt")).isInstanceOf(String.class);
        
        // Verify they are valid ISO-8601 strings
        String createdAt = (String) session.get("createdAt");
        String expiresAt = (String) session.get("expiresAt");
        String lastAccessedAt = (String) session.get("lastAccessedAt");
        
        assertThat(Instant.parse(createdAt)).isNotNull();
        assertThat(Instant.parse(expiresAt)).isNotNull();
        assertThat(Instant.parse(lastAccessedAt)).isNotNull();
    }

    @Test
    @DisplayName("Should handle round-trip serialization/deserialization of Instant fields")
    void shouldHandleRoundTripSerializationDeserializationOfInstantFields() {
        // Given - store a session
        service.storeSession("test@example.com", "license_key_123", 
            FeatureFlagsProperties.LicenseTier.PREMIUM, 24L);

        // When - retrieve the session (triggers deserialization)
        FeatureFlagsProperties.LicenseTier tier = service.getTierByEmail("test@example.com");

        // Then - session should be valid and not expired
        assertThat(tier).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
        
        // Verify the session was stored and retrieved correctly
        // (This implicitly verifies Instant fields were serialized/deserialized correctly)
        assertThat(testStorageFile).exists();
    }

    @Test
    @DisplayName("Should handle multiple sessions with Instant fields correctly")
    void shouldHandleMultipleSessionsWithInstantFieldsCorrectly() {
        // Given - store multiple sessions
        service.storeSession("user1@example.com", "key1", 
            FeatureFlagsProperties.LicenseTier.COMMUNITY, 12L);
        service.storeSession("user2@example.com", "key2", 
            FeatureFlagsProperties.LicenseTier.PREMIUM, 24L);
        service.storeSession("user3@example.com", "key3", 
            FeatureFlagsProperties.LicenseTier.ENTERPRISE, 48L);

        // When - retrieve all sessions
        FeatureFlagsProperties.LicenseTier tier1 = service.getTierByEmail("user1@example.com");
        FeatureFlagsProperties.LicenseTier tier2 = service.getTierByEmail("user2@example.com");
        FeatureFlagsProperties.LicenseTier tier3 = service.getTierByEmail("user3@example.com");

        // Then - all sessions should be valid
        assertThat(tier1).isEqualTo(FeatureFlagsProperties.LicenseTier.COMMUNITY);
        assertThat(tier2).isEqualTo(FeatureFlagsProperties.LicenseTier.PREMIUM);
        assertThat(tier3).isEqualTo(FeatureFlagsProperties.LicenseTier.ENTERPRISE);
        
        // Verify JSON file contains all sessions with Instant fields
        try {
            String jsonContent = Files.readString(testStorageFile);
            assertThat(jsonContent)
                .contains("user1@example.com")
                .contains("user2@example.com")
                .contains("user3@example.com")
                .contains("\"createdAt\"")
                .contains("\"expiresAt\"")
                .contains("\"lastAccessedAt\"");
        } catch (Exception e) {
            throw new AssertionError("Failed to verify multiple sessions: " + e.getMessage(), e);
        }
    }

    @Test
    @DisplayName("Should update lastAccessedAt Instant field when session is accessed")
    void shouldUpdateLastAccessedAtInstantFieldWhenSessionIsAccessed() throws Exception {
        // Given - store a session
        service.storeSession("test@example.com", "license_key_123", 
            FeatureFlagsProperties.LicenseTier.PREMIUM, 24L);
        
        // Read initial lastAccessedAt
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String initialJson = Files.readString(testStorageFile);
        TypeReference<Map<String, Map<String, Object>>> typeRef = new TypeReference<>() {};
        Map<String, Map<String, Object>> initialSessions = mapper.readValue(initialJson, typeRef);
        String initialLastAccessedAt = (String) initialSessions.get("test@example.com").get("lastAccessedAt");
        Instant initialTime = Instant.parse(initialLastAccessedAt);
        
        // Wait a bit to ensure time difference
        Thread.sleep(100);
        
        // When - access the session (this should update lastAccessedAt)
        service.getTierByEmail("test@example.com");
        
        // Then - lastAccessedAt should be updated
        String updatedJson = Files.readString(testStorageFile);
        Map<String, Map<String, Object>> updatedSessions = mapper.readValue(updatedJson, typeRef);
        String updatedLastAccessedAt = (String) updatedSessions.get("test@example.com").get("lastAccessedAt");
        Instant updatedTime = Instant.parse(updatedLastAccessedAt);
        
        assertThat(updatedTime).isAfter(initialTime);
    }
}


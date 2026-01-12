package adrianmikula.jakartamigration.storage.service;

import adrianmikula.jakartamigration.config.FeatureFlagsProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for local file-based storage of license sessions.
 * 
 * This service provides offline caching of license validation results.
 * When enabled, it stores email-to-tier mappings locally in a JSON file
 * so the MCP server can work offline after initial validation.
 * 
 * Uses a simple JSON file for storage - no database required.
 * 
 * Only enabled when jakarta.migration.storage.file.enabled=true
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "jakarta.migration.storage.file.enabled", havingValue = "true", matchIfMissing = false)
public class LocalLicenseStorageService {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Path storageFile;
    private static final long DEFAULT_CACHE_TTL_HOURS = 24;

    public LocalLicenseStorageService(
            @Value("${jakarta.migration.storage.file.path:.mcp_license_sessions.json}") String filePath) {
        this.storageFile = Paths.get(filePath).toAbsolutePath();
        log.info("Local license storage file: {}", storageFile);
    }

    /**
     * Get license tier from local storage by email.
     * 
     * @param email The email address
     * @return License tier if found and not expired, null otherwise
     */
    public FeatureFlagsProperties.LicenseTier getTierByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        lock.readLock().lock();
        boolean readLockHeld = true;
        try {
            Map<String, LicenseSession> sessions = loadSessions();
            LicenseSession session = sessions.get(email.toLowerCase());
            
            if (session == null) {
                log.debug("No local session found for email: {}", maskEmail(email));
                return null;
            }

            // Check if expired
            if (session.getExpiresAt().isBefore(Instant.now())) {
                log.debug("Local session expired for email: {}", maskEmail(email));
                lock.readLock().unlock();
                readLockHeld = false;
                deleteSession(email);
                return null;
            }

            // Update last accessed time
            session.setLastAccessedAt(Instant.now());
            lock.readLock().unlock();
            readLockHeld = false;
            saveSession(session);

            try {
                return FeatureFlagsProperties.LicenseTier.valueOf(session.getTier());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid tier in local session: {}", session.getTier());
                return null;
            }
        } catch (Exception e) {
            log.error("Error retrieving license session by email: {}", e.getMessage(), e);
            return null;
        } finally {
            // Ensure read lock is always released
            if (readLockHeld) {
                lock.readLock().unlock();
            }
        }
    }

    /**
     * Get license tier from local storage by license key.
     * 
     * @param licenseKey The license key
     * @return License tier if found and not expired, null otherwise
     */
    public FeatureFlagsProperties.LicenseTier getTierByLicenseKey(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return null;
        }

        lock.readLock().lock();
        boolean readLockHeld = true;
        try {
            Map<String, LicenseSession> sessions = loadSessions();
            
            // Find session by license key
            LicenseSession session = sessions.values().stream()
                .filter(s -> licenseKey.equals(s.getLicenseKey()))
                .findFirst()
                .orElse(null);
            
            if (session == null) {
                log.debug("No local session found for license key: {}", maskKey(licenseKey));
                return null;
            }

            // Check if expired
            if (session.getExpiresAt().isBefore(Instant.now())) {
                log.debug("Local session expired for license key: {}", maskKey(licenseKey));
                lock.readLock().unlock();
                readLockHeld = false;
                deleteSession(session.getEmail());
                return null;
            }

            // Update last accessed time
            session.setLastAccessedAt(Instant.now());
            lock.readLock().unlock();
            readLockHeld = false;
            saveSession(session);

            try {
                return FeatureFlagsProperties.LicenseTier.valueOf(session.getTier());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid tier in local session: {}", session.getTier());
                return null;
            }
        } catch (Exception e) {
            log.error("Error retrieving license session by license key: {}", e.getMessage(), e);
            return null;
        } finally {
            // Ensure read lock is always released
            if (readLockHeld) {
                lock.readLock().unlock();
            }
        }
    }

    /**
     * Store license session in local storage.
     * 
     * @param email The email address
     * @param licenseKey The license key (optional)
     * @param tier The license tier
     * @param ttlHours TTL in hours (default: 24)
     */
    public void storeSession(String email, String licenseKey, FeatureFlagsProperties.LicenseTier tier, Long ttlHours) {
        if (email == null || email.isBlank() || tier == null) {
            return;
        }

        long ttl = (ttlHours != null && ttlHours > 0) ? ttlHours : DEFAULT_CACHE_TTL_HOURS;
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl, ChronoUnit.HOURS);

        LicenseSession session = new LicenseSession();
        session.setEmail(email.toLowerCase());
        session.setLicenseKey(licenseKey);
        session.setTier(tier.name());
        session.setCreatedAt(now);
        session.setExpiresAt(expiresAt);
        session.setLastAccessedAt(now);

        saveSession(session);
        log.debug("Stored license session for email: {} (tier: {}, expires: {})", 
            maskEmail(email), tier, expiresAt);
    }

    /**
     * Delete session by email.
     */
    public void deleteSession(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        lock.writeLock().lock();
        try {
            Map<String, LicenseSession> sessions = loadSessions();
            if (sessions.remove(email.toLowerCase()) != null) {
                saveAllSessions(sessions);
                log.debug("Deleted local session for email: {}", maskEmail(email));
            }
        } catch (Exception e) {
            log.error("Error deleting license session: {}", e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clean up expired sessions (runs hourly).
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredSessions() {
        lock.writeLock().lock();
        try {
            Map<String, LicenseSession> sessions = loadSessions();
            Instant now = Instant.now();
            int initialSize = sessions.size();
            
            sessions.entrySet().removeIf(entry -> 
                entry.getValue().getExpiresAt().isBefore(now));
            
            if (sessions.size() < initialSize) {
                saveAllSessions(sessions);
                log.debug("Cleaned up {} expired license sessions", initialSize - sessions.size());
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired sessions: {}", e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Load all sessions from file.
     */
    private Map<String, LicenseSession> loadSessions() {
        if (!Files.exists(storageFile)) {
            return new HashMap<>();
        }

        try {
            String content = Files.readString(storageFile);
            if (content == null || content.isBlank()) {
                return new HashMap<>();
            }
            
            TypeReference<Map<String, LicenseSession>> typeRef = new TypeReference<>() {};
            Map<String, LicenseSession> sessions = objectMapper.readValue(content, typeRef);
            return sessions != null ? sessions : new HashMap<>();
        } catch (IOException e) {
            log.warn("Error loading license sessions from file: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Save a single session (updates existing or adds new).
     */
    private void saveSession(LicenseSession session) {
        lock.writeLock().lock();
        try {
            Map<String, LicenseSession> sessions = loadSessions();
            sessions.put(session.getEmail(), session);
            saveAllSessions(sessions);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Save all sessions to file.
     */
    private void saveAllSessions(Map<String, LicenseSession> sessions) {
        try {
            // Create parent directory if it doesn't exist
            File file = storageFile.toFile();
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, sessions);
        } catch (IOException e) {
            log.error("Error saving license sessions to file: {}", e.getMessage(), e);
        }
    }

    /**
     * Mask email for logging.
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return "***@" + email.substring(atIndex + 1);
        }
        return email.substring(0, 2) + "***@" + email.substring(atIndex + 1);
    }

    /**
     * Mask license key for logging.
     */
    private String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    /**
     * License session data class.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class LicenseSession {
        private String email;
        private String licenseKey;
        private String tier;
        private Instant createdAt;
        private Instant expiresAt;
        private Instant lastAccessedAt;
    }
}

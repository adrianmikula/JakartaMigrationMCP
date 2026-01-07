package adrianmikula.jakartamigration.api.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing credit balances and transactions.
 * 
 * Currently uses in-memory storage. Can be upgraded to PostgreSQL later.
 * 
 * Credits are consumed when premium tools are used:
 * - createMigrationPlan: 2 credits
 * - analyzeMigrationImpact: 1 credit
 * - Other premium tools: 1-3 credits each
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditService {

    /**
     * In-memory storage for credit balances.
     * Key: license key, Value: credit balance info
     * 
     * TODO: Replace with PostgreSQL when ready:
     * - Create credits table
     * - Create credit_transactions table
     * - Use JPA repositories
     */
    private final Map<String, CreditBalance> creditBalances = new ConcurrentHashMap<>();
    
    /**
     * In-memory storage for transactions.
     * Key: transaction ID, Value: transaction info
     */
    private final Map<String, CreditTransaction> transactions = new ConcurrentHashMap<>();

    /**
     * Get credit balance for a license key.
     * 
     * @param licenseKey The license key
     * @return Current balance, or 0 if not found
     */
    public int getBalance(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return 0;
        }
        
        CreditBalance balance = creditBalances.get(licenseKey);
        if (balance == null) {
            log.debug("No credit balance found for license key: {}", maskKey(licenseKey));
            return 0;
        }
        
        return balance.getBalance();
    }

    /**
     * Get last sync time for a license key.
     * 
     * @param licenseKey The license key
     * @return Last sync time, or null if not found
     */
    public Instant getLastSync(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return null;
        }
        
        CreditBalance balance = creditBalances.get(licenseKey);
        if (balance == null) {
            return null;
        }
        
        return balance.getLastSync();
    }

    /**
     * Consume credits for a license key.
     * 
     * @param licenseKey The license key
     * @param amount Number of credits to consume
     * @param toolName Name of the tool being used
     * @return Transaction ID if successful, null if insufficient credits
     */
    public String consumeCredits(String licenseKey, int amount, String toolName) {
        if (licenseKey == null || licenseKey.isBlank()) {
            log.warn("Attempted to consume credits with null/blank license key");
            return null;
        }
        
        if (amount <= 0) {
            log.warn("Attempted to consume invalid amount of credits: {}", amount);
            return null;
        }
        
        CreditBalance balance = creditBalances.computeIfAbsent(
            licenseKey,
            k -> new CreditBalance(0, Instant.now())
        );
        
        synchronized (balance) {
            if (balance.getBalance() < amount) {
                log.warn("Insufficient credits for license key: {} (balance: {}, requested: {})",
                    maskKey(licenseKey), balance.getBalance(), amount);
                return null;
            }
            
            int newBalance = balance.getBalance() - amount;
            balance.setBalance(newBalance);
            balance.setLastSync(Instant.now());
            
            String transactionId = UUID.randomUUID().toString();
            CreditTransaction transaction = new CreditTransaction(
                transactionId,
                licenseKey,
                amount,
                toolName,
                Instant.now()
            );
            transactions.put(transactionId, transaction);
            
            log.info("Consumed {} credits for license key: {} (new balance: {})",
                amount, maskKey(licenseKey), newBalance);
            
            return transactionId;
        }
    }

    /**
     * Sync credits from Stripe (or other source).
     * This would typically be called when a user purchases credits or renews subscription.
     * 
     * @param licenseKey The license key
     * @param newBalance The new credit balance
     * @return true if successful
     */
    public boolean syncCredits(String licenseKey, int newBalance) {
        if (licenseKey == null || licenseKey.isBlank()) {
            log.warn("Attempted to sync credits with null/blank license key");
            return false;
        }
        
        if (newBalance < 0) {
            log.warn("Attempted to set negative credit balance: {}", newBalance);
            return false;
        }
        
        CreditBalance balance = creditBalances.computeIfAbsent(
            licenseKey,
            k -> new CreditBalance(0, Instant.now())
        );
        
        synchronized (balance) {
            int oldBalance = balance.getBalance();
            balance.setBalance(newBalance);
            balance.setLastSync(Instant.now());
            
            log.info("Synced credits for license key: {} (old: {}, new: {})",
                maskKey(licenseKey), oldBalance, newBalance);
            
            return true;
        }
    }

    /**
     * Initialize credits for a license key (e.g., when first validated).
     * 
     * @param licenseKey The license key
     * @param initialBalance Initial credit balance
     */
    public void initializeCredits(String licenseKey, int initialBalance) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return;
        }
        
        creditBalances.computeIfAbsent(
            licenseKey,
            k -> new CreditBalance(initialBalance, Instant.now())
        );
        
        log.debug("Initialized credits for license key: {} (balance: {})",
            maskKey(licenseKey), initialBalance);
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
     * Credit balance data structure.
     */
    @Data
    private static class CreditBalance {
        private int balance;
        private Instant lastSync;

        public CreditBalance(int balance, Instant lastSync) {
            this.balance = balance;
            this.lastSync = lastSync;
        }
    }

    /**
     * Credit transaction data structure.
     */
    @Data
    private static class CreditTransaction {
        private final String transactionId;
        private final String licenseKey;
        private final int amount;
        private final String toolName;
        private final Instant timestamp;

        public CreditTransaction(String transactionId, String licenseKey, int amount, String toolName, Instant timestamp) {
            this.transactionId = transactionId;
            this.licenseKey = licenseKey;
            this.amount = amount;
            this.toolName = toolName;
            this.timestamp = timestamp;
        }
    }
}


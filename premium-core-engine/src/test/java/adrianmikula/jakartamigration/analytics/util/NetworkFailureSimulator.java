package adrianmikula.jakartamigration.analytics.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility for simulating various network failure scenarios in tests.
 * Helps test error handling, retry mechanisms, and timeout behaviors.
 */
@Slf4j
public class NetworkFailureSimulator {
    
    public enum FailureType {
        NONE,
        CONNECTION_REFUSED,
        TIMEOUT,
        UNKNOWN_HOST,
        IO_EXCEPTION,
        SLOW_RESPONSE,
        INTERMITTENT_FAILURE
    }
    
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private FailureType failureType = FailureType.NONE;
    private int maxFailures = Integer.MAX_VALUE;
    private long delayMs = 0;
    private boolean intermittentMode = false;
    private double failureRate = 0.0; // 0.0 to 1.0
    
    /**
     * Sets the type of network failure to simulate.
     */
    public NetworkFailureSimulator withFailureType(FailureType type) {
        this.failureType = type;
        return this;
    }
    
    /**
     * Sets maximum number of failures before allowing success.
     */
    public NetworkFailureSimulator withMaxFailures(int maxFailures) {
        this.maxFailures = maxFailures;
        return this;
    }
    
    /**
     * Adds artificial delay to simulate slow responses.
     */
    public NetworkFailureSimulator withDelay(long delayMs) {
        this.delayMs = delayMs;
        return this;
    }
    
    /**
     * Enables intermittent failure mode with specified failure rate.
     */
    public NetworkFailureSimulator withIntermittentFailures(double failureRate) {
        this.intermittentMode = true;
        this.failureRate = Math.max(0.0, Math.min(1.0, failureRate));
        return this;
    }
    
    /**
     * Simulates a network operation that may fail based on configuration.
     */
    public void simulateNetworkOperation() throws IOException {
        // Add artificial delay if configured
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Operation interrupted", e);
            }
        }
        
        // Check if we should fail this operation
        if (shouldFail()) {
            failureCount.incrementAndGet();
            throw createFailureException();
        }
        
        successCount.incrementAndGet();
    }
    
    /**
     * Determines if the current operation should fail based on configuration.
     */
    private boolean shouldFail() {
        if (failureType == FailureType.NONE) {
            return false;
        }
        
        if (intermittentMode) {
            return Math.random() < failureRate;
        }
        
        return failureCount.get() < maxFailures;
    }
    
    /**
     * Creates the appropriate exception based on failure type.
     */
    private IOException createFailureException() {
        switch (failureType) {
            case CONNECTION_REFUSED:
                return new ConnectException("Connection refused: simulate network unreachable");
                
            case TIMEOUT:
                return new SocketTimeoutException("Read timeout: simulate slow response");
                
            case UNKNOWN_HOST:
                return new UnknownHostException("Unknown host: simulate DNS failure");
                
            case IO_EXCEPTION:
                return new IOException("Simulated I/O error during network operation");
                
            case SLOW_RESPONSE:
                // Simulate slow response by waiting longer than timeout
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new SocketTimeoutException("Simulated slow response timeout");
                
            case INTERMITTENT_FAILURE:
                return new IOException("Simulated intermittent network failure");
                
            default:
                return new IOException("Unknown network failure type");
        }
    }
    
    /**
     * Resets the failure simulator state.
     */
    public void reset() {
        failureCount.set(0);
        successCount.set(0);
        failureType = FailureType.NONE;
        maxFailures = Integer.MAX_VALUE;
        delayMs = 0;
        intermittentMode = false;
        failureRate = 0.0;
    }
    
    /**
     * Gets the current failure count.
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * Gets the current success count.
     */
    public int getSuccessCount() {
        return successCount.get();
    }
    
    /**
     * Gets the total operation count.
     */
    public int getTotalOperations() {
        return failureCount.get() + successCount.get();
    }
    
    /**
     * Creates a simulator for connection refused scenarios.
     */
    public static NetworkFailureSimulator connectionRefused() {
        return new NetworkFailureSimulator().withFailureType(FailureType.CONNECTION_REFUSED);
    }
    
    /**
     * Creates a simulator for timeout scenarios.
     */
    public static NetworkFailureSimulator timeout() {
        return new NetworkFailureSimulator().withFailureType(FailureType.TIMEOUT);
    }
    
    /**
     * Creates a simulator for unknown host scenarios.
     */
    public static NetworkFailureSimulator unknownHost() {
        return new NetworkFailureSimulator().withFailureType(FailureType.UNKNOWN_HOST);
    }
    
    /**
     * Creates a simulator for intermittent failures.
     */
    public static NetworkFailureSimulator intermittent(double failureRate) {
        return new NetworkFailureSimulator().withIntermittentFailures(failureRate);
    }
    
    /**
     * Creates a simulator for slow responses.
     */
    public static NetworkFailureSimulator slowResponse(long delayMs) {
        return new NetworkFailureSimulator().withFailureType(FailureType.SLOW_RESPONSE).withDelay(delayMs);
    }
}

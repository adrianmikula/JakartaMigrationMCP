package adrianmikula.jakartamigration.analytics.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility class for testing concurrent access scenarios.
 * Provides helpers for running multiple threads safely and collecting results.
 */
@Slf4j
public class ConcurrencyTestHelper {
    
    /**
     * Result of a concurrent operation.
     */
    public static class ConcurrentResult<T> {
        private final T result;
        private final Throwable error;
        private final long executionTimeMs;
        private final String threadName;
        
        public ConcurrentResult(T result, Throwable error, long executionTimeMs, String threadName) {
            this.result = result;
            this.error = error;
            this.executionTimeMs = executionTimeMs;
            this.threadName = threadName;
        }
        
        public T getResult() { return result; }
        public Throwable getError() { return error; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public String getThreadName() { return threadName; }
        public boolean hasError() { return error != null; }
        public boolean isSuccess() { return error == null; }
    }
    
    /**
     * Runs a task concurrently across multiple threads.
     */
    public static <T> List<ConcurrentResult<T>> runConcurrent(
            int threadCount, 
            Supplier<T> task) throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<ConcurrentResult<T>>> futures = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            futures.add(executor.submit(() -> {
                String threadName = "Thread-" + threadIndex + "-" + Thread.currentThread().getName();
                long startTime = System.currentTimeMillis();
                
                try {
                    T result = task.get();
                    long executionTime = System.currentTimeMillis() - startTime;
                    return new ConcurrentResult<>(result, null, executionTime, threadName);
                } catch (Throwable e) {
                    long executionTime = System.currentTimeMillis() - startTime;
                    return new ConcurrentResult<>(null, e, executionTime, threadName);
                }
            }));
        }
        
        List<ConcurrentResult<T>> results = new ArrayList<>();
        for (Future<ConcurrentResult<T>> future : futures) {
            try {
                results.add(future.get(30, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                results.add(new ConcurrentResult<>(null, e, 30000, "timeout"));
            } catch (Exception e) {
                results.add(new ConcurrentResult<>(null, e, 0, "exception"));
            }
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        return results;
    }
    
    /**
     * Runs a consumer task concurrently across multiple threads.
     */
    public static List<ConcurrentResult<Void>> runConcurrentConsumer(
            int threadCount, 
            Consumer<Integer> task) throws InterruptedException {
        
        return runConcurrent(threadCount, () -> {
            task.accept(threadCount);
            return null;
        });
    }
    
    /**
     * Runs tasks with timing and collects performance metrics.
     */
    public static <T> PerformanceMetrics runWithMetrics(
            int threadCount, 
            int iterationsPerThread,
            Supplier<T> task) throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<TaskResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            futures.add(executor.submit(() -> {
                return runTaskWithMetrics(threadIndex, iterationsPerThread, task);
            }));
        }
        
        List<TaskResult> taskResults = new ArrayList<>();
        for (Future<TaskResult> future : futures) {
            try {
                taskResults.add(future.get(60, TimeUnit.SECONDS));
            } catch (Exception e) {
                log.error("Task execution failed", e);
            }
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        return new PerformanceMetrics(taskResults);
    }
    
    /**
     * Runs a single task with metrics collection.
     */
    private static <T> TaskResult runTaskWithMetrics(
            int threadIndex, 
            int iterations, 
            Supplier<T> task) {
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int errorCount = 0;
        List<Long> executionTimes = new ArrayList<>();
        
        for (int i = 0; i < iterations; i++) {
            long iterationStart = System.currentTimeMillis();
            try {
                task.get();
                successCount++;
            } catch (Exception e) {
                errorCount++;
            } finally {
                executionTimes.add(System.currentTimeMillis() - iterationStart);
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        return new TaskResult(threadIndex, successCount, errorCount, totalTime, executionTimes);
    }
    
    /**
     * Result of a single task execution with metrics.
     */
    public static class TaskResult {
        private final int threadIndex;
        private final int successCount;
        private final int errorCount;
        private final long totalTimeMs;
        private final List<Long> executionTimes;
        
        public TaskResult(int threadIndex, int successCount, int errorCount, 
                        long totalTimeMs, List<Long> executionTimes) {
            this.threadIndex = threadIndex;
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.totalTimeMs = totalTimeMs;
            this.executionTimes = executionTimes;
        }
        
        public int getThreadIndex() { return threadIndex; }
        public int getSuccessCount() { return successCount; }
        public int getErrorCount() { return errorCount; }
        public long getTotalTimeMs() { return totalTimeMs; }
        public List<Long> getExecutionTimes() { return executionTimes; }
        public double getAverageExecutionTime() {
            return executionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        }
        public long getMinExecutionTime() {
            return executionTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        }
        public long getMaxExecutionTime() {
            return executionTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        }
    }
    
    /**
     * Performance metrics for concurrent execution.
     */
    public static class PerformanceMetrics {
        private final List<TaskResult> results;
        private final int totalThreads;
        private final int totalOperations;
        private final int totalSuccesses;
        private final int totalErrors;
        private final long totalTimeMs;
        
        public PerformanceMetrics(List<TaskResult> results) {
            this.results = results;
            this.totalThreads = results.size();
            this.totalOperations = results.stream().mapToInt(r -> r.getSuccessCount() + r.getErrorCount()).sum();
            this.totalSuccesses = results.stream().mapToInt(TaskResult::getSuccessCount).sum();
            this.totalErrors = results.stream().mapToInt(TaskResult::getErrorCount).sum();
            this.totalTimeMs = results.stream().mapToLong(TaskResult::getTotalTimeMs).max().orElse(0);
        }
        
        public int getTotalThreads() { return totalThreads; }
        public int getTotalOperations() { return totalOperations; }
        public int getTotalSuccesses() { return totalSuccesses; }
        public int getTotalErrors() { return totalErrors; }
        public long getTotalTimeMs() { return totalTimeMs; }
        public double getSuccessRate() { 
            return totalOperations > 0 ? (double) totalSuccesses / totalOperations : 0.0; 
        }
        public double getOperationsPerSecond() {
            return totalTimeMs > 0 ? (totalOperations * 1000.0) / totalTimeMs : 0.0;
        }
        public double getAverageExecutionTime() {
            return results.stream()
                .flatMap(r -> r.getExecutionTimes().stream())
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        }
        
        public void logMetrics() {
            log.info("=== Performance Metrics ===");
            log.info("Threads: {}", totalThreads);
            log.info("Total Operations: {}", totalOperations);
            log.info("Successes: {} ({:.2f}%)", totalSuccesses, getSuccessRate() * 100);
            log.info("Errors: {} ({:.2f}%)", totalErrors, (1 - getSuccessRate()) * 100);
            log.info("Total Time: {} ms", totalTimeMs);
            log.info("Operations/Second: {:.2f}", getOperationsPerSecond());
            log.info("Avg Execution Time: {:.2f} ms", getAverageExecutionTime());
        }
    }
    
    /**
     * Tests for race conditions by running identical operations concurrently.
     */
    public static <T> List<ConcurrentResult<T>> testRaceCondition(
            int threadCount,
            Supplier<T> task) throws InterruptedException {
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<ConcurrentResult<T>>> futures = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    return runTaskWithTiming(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new ConcurrentResult<>(null, e, 0, Thread.currentThread().getName());
                }
            }));
        }
        
        // Wait for all threads to be ready
        readyLatch.await();
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        List<ConcurrentResult<T>> results = new ArrayList<>();
        for (Future<ConcurrentResult<T>> future : futures) {
            try {
                results.add(future.get(30, TimeUnit.SECONDS));
            } catch (Exception e) {
                results.add(new ConcurrentResult<>(null, e, 0, "exception"));
            }
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        return results;
    }
    
    /**
     * Runs a single task and measures execution time.
     */
    private static <T> ConcurrentResult<T> runTaskWithTiming(Supplier<T> task) {
        String threadName = Thread.currentThread().getName();
        long startTime = System.currentTimeMillis();
        
        try {
            T result = task.get();
            long executionTime = System.currentTimeMillis() - startTime;
            return new ConcurrentResult<>(result, null, executionTime, threadName);
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new ConcurrentResult<>(null, e, executionTime, threadName);
        }
    }
}

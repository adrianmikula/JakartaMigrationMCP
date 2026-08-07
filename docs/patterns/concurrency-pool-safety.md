# Concurrency Pool Safety

## Overview

Java's `parallelStream()` and `CompletableFuture.supplyAsync()` both default to the
`ForkJoinPool.commonPool()`. Mixing blocking `get()` / `join()` calls with async work on
the same pool is a common source of hangs, especially under load or in CI where the
pool is small.

## The Problem

When you call `future.get()` from inside `parallelStream().forEach(...)`, the
`parallelStream` worker is a common-pool thread. The `CompletableFuture` task it is
waiting for is also scheduled on the common pool. If every worker ends up blocked
waiting for its own future, no threads remain to execute those futures:

```java
// BAD — deadlocks/common-pool starvation
items.parallelStream().forEach(item -> {
    CompletableFuture<Result> future = asyncLookup(item);
    Result result = future.get(); // blocks a common-pool thread
    store.put(item.getKey(), result);
});
```

## The Solution

### 1. Never block the common pool to wait for common-pool futures

Run the blocking `get()` on a dedicated executor, or collect the futures and wait on
the original thread:

```java
// GOOD — collect futures, wait once on the caller thread
List<CompletableFuture<Result>> futures = items.stream()
    .map(item -> CompletableFuture.supplyAsync(() -> lookup(item), dedicatedExecutor))
    .toList();

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .get(timeoutSeconds, TimeUnit.SECONDS);
```

### 2. Always use a dedicated executor for background work

```java
private static final ExecutorService LOOKUP_EXECUTOR = Executors.newFixedThreadPool(
    MAX_PARALLELISM,
    r -> {
        Thread t = new Thread(r, "lookup-" + counter.incrementAndGet());
        t.setDaemon(true);
        return t;
    });
```

### 3. Always put a timeout on `get()` / `join()`

```java
// BAD — can hang forever
var result = future.get();

// GOOD — bounded wait
var result = future.get(LOOKUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
```

### 4. Use `CompletableFuture.allOf` for batches

```java
private Map<String, Usage> enrichBatch(List<Usage> usages) {
    Map<String, Usage> results = new ConcurrentHashMap<>();
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (Usage usage : usages) {
        futures.add(CompletableFuture.supplyAsync(() -> enrich(usage), LOOKUP_EXECUTOR)
            .thenAccept(opt -> opt.ifPresent(u -> results.put(u.getKey(), u))));
    }

    if (!futures.isEmpty()) {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .get(BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    return results;
}
```

## Checklist

- [ ] Are `future.get()` / `join()` calls made from a thread that is *not* the common pool?
- [ ] Is there a timeout on every `get()` / `join()` call?
- [ ] Does batched async work use a dedicated, bounded executor?
- [ ] Is `parallelStream()` only used for CPU-bound work that does not block on async I/O?

## References

- `TransitiveDependencyScannerImpl.enrichWithMavenLookupsBatch`
- `ImprovedMavenCentralLookupService.findJakartaEquivalents`

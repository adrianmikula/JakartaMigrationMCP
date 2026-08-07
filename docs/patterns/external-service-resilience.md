# External Service Resilience

## Overview

Network calls and external command invocations fail or hang in CI for many reasons:
network blocks, missing tools, slow proxies, or unpopulated caches. Code that shells out
to `mvn`/`gradle` or calls Maven Central must be resilient by default.

## The Problem

- `mvn dependency:tree` may try to download plugins and hang for minutes in a sandbox.
- `CompletableFuture` based network lookups can deadlock the common pool (see
  [Concurrency Pool Safety](concurrency-pool-safety.md)).
- Repeatedly invoking an external command that has already failed wastes time and log
  noise on every file in a large project.

## The Solution

### 1. Provide an offline mode

Services that call the network should support an offline short-circuit that returns
empty immediately without making a request:

```java
private final boolean offline = Boolean.getBoolean("jakarta.migration.offline");

public CompletableFuture<List<Match>> findEquivalents(String group, String artifact) {
    if (offline) {
        return CompletableFuture.completedFuture(List.of());
    }
    return CompletableFuture.supplyAsync(() -> queryNetwork(group, artifact), executor);
}
```

Tests and CI should enable this by setting the property or by injecting a stub service.

### 2. Make timeouts configurable

Command and network timeouts should be overridable from system properties so CI can use
aggressive values:

```java
private static int commandTimeoutSeconds() {
    return Integer.parseInt(System.getProperty(
        "advanced.scan.command.timeout.seconds",
        String.valueOf(DependencyTreeCommandExecutor.DEFAULT_TIMEOUT_SECONDS)));
}
```

Use the configurable value in every `future.get(...)` call:

```java
var treeResult = future.get(commandTimeoutSeconds(), TimeUnit.SECONDS);
```

### 3. Pass offline flags to external commands

When offline mode is enabled, pass the tool's own offline flag so it fails fast instead
of trying to download artifacts:

```java
boolean offline = Boolean.getBoolean("jakarta.migration.offline");
List<String> cmd = new ArrayList<>();
cmd.add(mvnCommand);
if (offline) {
    cmd.add("-o"); // Maven offline
}
cmd.add("dependency:tree");
```

### 4. Short-circuit repeated command failures

In a multi-file scan, once a build command has failed for one file with a
non-recoverable error (command not found, empty dependency tree), assume subsequent
files of the same type will also fail and skip the command:

```java
String previousFailedBuildFile = null;
for (Path file : buildFiles) {
    String fileName = file.getFileName().toString().toLowerCase();
    boolean shouldSkip = previousFailedBuildFile != null
        && fileName.equals(previousFailedBuildFile);

    Result result = shouldSkip
        ? fallbackScan(file)
        : runBuildCommand(file);

    if (result.hasError() && isNonRecoverable(result.getErrorMessage())) {
        previousFailedBuildFile = fileName;
    }
}
```

## Checklist

- [ ] Is there a `jakarta.migration.offline` (or equivalent) toggle for network/service calls?
- [ ] Are command and network timeouts configurable via system properties?
- [ ] Do external build-tool commands receive an offline flag when the mode is enabled?
- [ ] Does repeated work short-circuit after a known non-recoverable failure?
- [ ] Does CI enable the offline toggle for network-sensitive test classes?

## References

- `ImprovedMavenCentralLookupService.findJakartaEquivalents`
- `DependencyTreeCommandExecutorImpl.buildMavenCommand`
- `TransitiveDependencyScannerImpl.scanProject` / `scanFileFallback`

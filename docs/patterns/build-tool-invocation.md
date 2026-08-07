# Build Tool Invocation Patterns

This document defines standards and anti-patterns for invoking external build tools (Gradle, Maven) from Java code, based on real failures encountered in this codebase.

## Overview

When a plugin or tool needs to invoke build tools (e.g., to resolve dependencies, run tasks), several non-obvious pitfalls can cause silent failures, exit code 1, or incorrect results. This document codifies the rules we've learned.

**Gradle dependency resolution in this codebase now uses the official Gradle Tooling API** (Rule 10). The earlier process-spawning rules (Rules 1, 4, 5, and 9) remain for the legacy path and for Maven, which is still invoked via `mvn`/`mvnw`.

---

## Rule 1: Never Pass Multiple `--configuration` Flags to Gradle

### Anti-pattern

```java
// ❌ BAD: --configuration only accepts a SINGLE value. Multiple flags cause:
// "Multiple arguments were provided for command-line option '--configuration'" → exit code 1
List<String> cmd = List.of("gradle", "dependencies",
    "--configuration", "compileClasspath",
    "--configuration", "runtimeClasspath"
);
```

### Correct

```java
// ✅ GOOD: Omit --configuration entirely — the output includes ALL configurations
// and the parser already handles them by detecting configuration header lines
// (lines ending with "Classpath" or "Configuration")
List<String> cmd = List.of("gradle", "dependencies", "--quiet", "--no-daemon");
```

### Why

Gradle's `--configuration` is a **task option** (not a project option), and task options only accept a single value. Passing `--configuration compileClasspath --configuration runtimeClasspath` causes Gradle to fail with:

```
* What went wrong:
Problem configuring task :dependencies from command line.
> Multiple arguments were provided for command-line option '--configuration'.
```

Without `--configuration`, `gradle dependencies` outputs ALL configurations (compileClasspath, runtimeClasspath, testCompileClasspath, etc.) with their full dependency trees. The parser groups output by configuration header, so all data is available without filtering at the command level.

### Reference

- `DependencyTreeCommandExecutorImpl.java` — `buildGradleCommand()`

---

## Rule 2: Exclude Build Output and Cache Directories When Discovering Build Files

### Anti-pattern

```java
// ❌ BAD: Files.walk() with no directory exclusions
try (Stream<Path> paths = Files.walk(projectRoot)) {
    return paths.filter(p -> p.toString().endsWith(".gradle") || p.toString().endsWith(".gradle.kts"))
        .collect(Collectors.toList());
}
// Result: Picks up Jacoco cache POMs, test resource .gradle files, build output artifacts
```

### Correct

```java
// ✅ GOOD: Files.walkFileTree with directory pruning
private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
    "target", "build", "out", ".gradle", ".git", ".svn",
    "node_modules", ".idea", ".mvn"
);

Files.walkFileTree(projectRoot, new SimpleFileVisitor<>() {
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        String dirName = dir.getFileName().toString();
        if (EXCLUDED_DIRECTORIES.contains(dirName)) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (isBuildFile(file)) {
            result.add(file);
        }
        return FileVisitResult.CONTINUE;
    }
});
```

### Why

`Files.walk()` visits every file in the tree including:
- `target/` and `build/` — compiled output, cached POMs, Jacoco reports
- `.gradle/` — Gradle cache with `.module` and POM files
- Test resource directories containing `.gradle` fixture files
- `.idea/` and other IDE metadata

Using `Files.walkFileTree` with `preVisitDirectory` pruning is O(n) on visited dirs only — skipped subtrees are never entered.

### Reference

- `BuildFileDiscovery.java` — `discoverBuildFiles()`

---

## Rule 3: Use Specific File Name Patterns, Not Wildcards

### Anti-pattern

```java
// ❌ BAD: Matches ALL .gradle files including test fixtures and Jacoco caches
path.getFileName().toString().endsWith(".gradle")
```

### Correct

```java
// ✅ GOOD: Only match actual build files
String name = path.getFileName().toString();
return name.equals("build.gradle") || name.equals("build.gradle.kts")
    || name.equals("pom.xml") || name.equals("settings.gradle.kts");
```

### Why

`*.gradle` matches:
- `build.gradle` — real build file
- `test-output.gradle` — test resource
- `jacoco-report.gradle` — Jacoco cache
- `subproject-test.gradle` — any test fixture

Build files have known, fixed names. Match them exactly.

### Reference

- `BuildFileDiscovery.java` — `isGradleFile()`, `isMavenFile()`

---

## Rule 4: Walk Up Directories to Find Wrapper Scripts

### Anti-pattern

```java
// ❌ BAD: Only checks the immediate project directory
File gradlew = new File(projectDir, "gradlew");
if (gradlew.exists()) {
    // use it
} else {
    // fall back to system gradle — fails for submodules
}
```

### Correct

```java
// ✅ GOOD: Walk up parent directories to find the wrapper
private static Optional<Path> findGradleWrapper(Path projectDir) {
    Path currentDir = projectDir;
    int depth = 0;
    while (currentDir != null && depth < 10) {
        Path gradlew = currentDir.resolve("gradlew");
        if (Files.exists(gradlew)) {
            return Optional.of(gradlew);
        }
        Path gradlewBat = currentDir.resolve("gradlew.bat");
        if (Files.exists(gradlewBat)) {
            return Optional.of(gradlewBat);
        }
        currentDir = currentDir.getParent();
        depth++;
        if (currentDir != null && currentDir.getNameCount() == 0) break;
    }
    return Optional.empty();
}
```

### Why

In multi-module projects, the wrapper (`gradlew`/`mvnw`) lives at the root. When scanning a submodule's `build.gradle.kts`, the submodule directory doesn't contain the wrapper — you must walk up to the root. A depth limit prevents infinite loops on unusual filesystems.

### Reference

- `DependencyTreeCommandExecutorImpl.java` — `findGradleWrapper()`, `findMavenWrapper()`

---

## Rule 5: Always Check Wrapper Availability Before System Command

### Anti-pattern

```java
// ❌ BAD: Only checks system command, projects with only a wrapper fail
if (!isCommandAvailable("gradle", "--version", 5)) {
    return DependencyTreeResult.error("gradle command not found");
}
```

### Correct

```java
// ✅ GOOD: Check system command first, then fall back to wrapper
private static boolean isGradleAvailableForProject(Path projectDir) {
    if (isCommandAvailable("gradle", "--version", 5)) return true;
    return findGradleWrapper(projectDir).isPresent();
}
```

### Why

Many projects only ship with a wrapper (`gradlew`), especially when targeting specific Gradle versions. Checking only the system `gradle`/`mvn` binary will incorrectly report "not found" for valid projects. The wrapper is the canonical way to invoke build tools in modern projects.

### Reference

- `DependencyTreeCommandExecutorImpl.java` — `isGradleAvailableForProject()`

---

## Rule 6: Reset UI State Before Starting Async Operations

### Anti-pattern

```java
// ❌ BAD: Previous scan results remain visible while new scan runs
private void handleScan(ActionEvent e) {
    setScanButtonsEnabled(false);
    CompletableFuture.runAsync(() -> {
        Results results = scan();
        ApplicationManager.getApplication().invokeLater(() -> {
            updateTable(results);
            setScanButtonsEnabled(true);
        });
    });
}
// User sees stale results from previous scan until new results arrive
```

### Correct

```java
// ✅ GOOD: Reset to pending state immediately, before async work starts
private void handleScan(ActionEvent e) {
    setScanButtonsEnabled(false);
    tableComponent.resetToPending();  // ← immediate visual feedback

    CompletableFuture.runAsync(() -> {
        Results results = scan();
        ApplicationManager.getApplication().invokeLater(() -> {
            updateTable(results);
            setScanButtonsEnabled(true);
        });
    });
}
```

### Why

Users interpret visible results as "current." If a scan takes 30 seconds, they'll act on stale data. Resetting to a "pending" or "analyzing" state:
1. Provides immediate visual feedback that a scan started
2. Prevents confusion about whether results are from the current or previous scan
3. Sets correct expectations about data freshness

### Reference

- `MigrationToolWindow.java` — `handleQuickScan()`, `handleDeepScan()`
- `DependenciesTableComponent.java` — `resetToPending()`

---

## Rule 7: Use Time-Based Cooldowns, Not Permanent Deduplication, for Notifications

### Anti-pattern

```java
// ❌ BAD: Notification is permanently suppressed after first show
private final Set<String> shownNotifications = ConcurrentHashMap.newKeySet();

public boolean showOnce(String key, String title, String message) {
    if (!shownNotifications.add(key)) {
        return false;  // Never shown again, even hours later
    }
    // show notification...
}
```

### Correct

```java
// ✅ GOOD: Time-based cooldown allows re-notification after window expires
private static final long COOLDOWN_MS = 5 * 60 * 1000L;
private final ConcurrentHashMap<String, Long> lastShownTimestamps = new ConcurrentHashMap<>();

public boolean showOnce(String key, String title, String message) {
    long now = System.currentTimeMillis();
    Long lastShown = lastShownTimestamps.get(key);
    if (lastShown != null && (now - lastShown) < COOLDOWN_MS) {
        return false;  // Still within cooldown
    }
    lastShownTimestamps.put(key, now);
    // show notification...
}
```

### Why

Permanent deduplication (`Set<String>`) causes a silent failure mode: after the first scan, all balloon notifications are permanently suppressed. If the user runs 10 scans over an hour and a build tool fails on scans 2–10, they'll never see the error because scan 1 already consumed the key.

A time-based cooldown (e.g., 5 minutes) allows the notification to re-appear after a reasonable interval, giving the user repeated opportunities to see transient errors.

### Reference

- `BalloonNotificationService.java` — `showOnce()`

---

## Rule 8: Use `Files.walkFileTree` Instead of `Files.walk` for Large Directory Trees

### Anti-pattern

```java
// ❌ BAD: Loads entire directory tree into memory
try (Stream<Path> paths = Files.walk(root, 10)) {
    return paths.filter(Files::isRegularFile)
        .filter(p -> p.toString().endsWith(".java"))
        .collect(Collectors.toList());
}
```

### Correct

```java
// ✅ GOOD: Visitor-based traversal with early pruning
List<Path> result = new ArrayList<>();
Files.walkFileTree(root, new SimpleFileVisitor<>() {
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (shouldSkip(dir)) return FileVisitResult.SKIP_SUBTREE;
        return FileVisitResult.CONTINUE;
    }
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (file.toString().endsWith(".java")) {
            result.add(file);
        }
        return FileVisitResult.CONTINUE;
    }
});
```

### Why

`Files.walk()` eagerly opens a `Stream<Path>` backed by a `DirectoryStream`. For large projects (10k+ files), this can:
- Exhaust file descriptors
- Hold open too many directory handles simultaneously
- Fail with `Too many open files` on some systems

`Files.walkFileTree` uses a callback model that opens/closes one directory at a time, with built-in support for pruning subtrees via `SKIP_SUBTREE`.

---

## Rule 9: Use Task Path Notation for Multi-Module Gradle Submodules

> **Note**: This rule has been superseded by the Gradle Tooling API migration (Rule 10).
> For new code, use the Tooling API and `GradleProject.getChildren()` for multi-module detection.
> Rule 9 remains documented for the legacy process-based path and Maven scenarios.
> See [ADR 0005](../adr/0005-adopt-gradle-tooling-api.md) and [migration roadmap](../roadmap/gradle-tooling-api-migration.md).

### Anti-pattern

```java
// ❌ BAD: Running `gradle dependencies` from a submodule directory
Path submoduleDir = projectRoot.resolve("community-core-engine");
List<String> cmd = List.of("gradlew", "dependencies", "--quiet", "--no-daemon");
// Gradle walks up to root, resolves to root project → root has NO dependencies → empty result
```

### Correct

```java
// ✅ GOOD: Detect submodule via settings.gradle(.kts) in ancestors, use task path notation
Optional<Path> projectRoot = findGradleProjectRoot(submoduleDir);
if (projectRoot.isPresent() && !projectRoot.get().equals(submoduleDir)) {
    String moduleName = computeGradleModuleName(projectRoot.get(), submoduleDir);
    // e.g. ":community-core-engine"
    List<String> cmd = List.of("gradlew", moduleName + ":dependencies", "--quiet", "--no-daemon");
    // Execute FROM the project root directory
}
```

### Why

When you run `gradle dependencies` from a submodule directory (e.g., `community-core-engine/`), Gradle walks up to the nearest `settings.gradle.kts` and resolves the **root project**, not the submodule. The root project typically has no dependencies (it's just the parent aggregator), so the command returns empty — no error, just no data.

The fix is to use Gradle's **task path notation** (`:moduleName:dependencies`) and execute from the project root. This tells Gradle to run the `dependencies` task on the specific subproject, which has its own dependencies.

Detecting whether a build file is in a submodule is done by walking up directories to find `settings.gradle(.kts)`. If found at a different level than the build file's parent, it's a submodule.

### Reference

- `DependencyTreeCommandExecutorImpl.java` — `findGradleProjectRoot()`, `computeGradleModuleName()`, `buildGradleModuleCommand()`

---

## Rule 10: Use the Gradle Tooling API for Gradle Dependency Resolution

### Anti-pattern

```java
// ❌ BAD: Spawning external gradle/gradlew processes and regex-parsing stdout
List<String> cmd = List.of("gradlew", "dependencies", "--quiet", "--no-daemon");
Process p = new ProcessBuilder(cmd).directory(projectRoot.toFile()).start();
String output = new String(p.getInputStream().readAllBytes(), UTF_8);
List<DependencyNode> deps = parseWithRegex(output);
```

### Correct

```java
// ✅ GOOD: Use the official Gradle Tooling API with explicit connection lifecycle
ProjectConnection connection = null;
try {
    connection = GradleConnector.newConnector()
            .forProjectDirectory(projectRoot.toFile())
            .connect();

    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    connection.newBuild()
            .forTasks("dependencies")
            .setStandardOutput(stdout)
            .setStandardError(new ByteArrayOutputStream())
            .run();

    String output = stdout.toString(StandardCharsets.UTF_8);
    // parse...
} catch (Exception e) {
    log.error("Tooling API execution failed for {}: {}", projectRoot, e.getMessage());
    return DependencyTreeResult.error("Gradle Tooling API failed: " + e.getMessage());
} finally {
    closeQuietly(connection);
}
```

### Why

The Gradle Tooling API replaces the process-spawning path because it:

- Reuses the Gradle daemon automatically, making repeated scans significantly faster
- Detects `gradlew`/wrapper automatically without manual directory walking
- Provides the `GradleProject` model for native multi-module enumeration (`getChildren()`, `getProjectDirectory()`, `getName()`, `getParent()`)
- Avoids stdout parsing fragility and version-specific CLI differences

Always close `ProjectConnection` in a `finally` block (or use a helper such as `closeQuietly`). Connections that are not closed leak file descriptors and daemon state.

The Tooling API dependency version must match the project's Gradle wrapper version. This codebase uses Gradle `8.5`, so the `gradle-tooling-api` dependency is `org.gradle:gradle-tooling-api:8.5` resolved from `https://repo.gradle.org/gradle/libs-releases/`.

### Multi-module projects

Use the `GradleProject` model to enumerate subprojects instead of parsing `settings.gradle(.kts)` manually:

```java
GradleProject rootProject = connection.model(GradleProject.class).get();
for (GradleProject child : rootProject.getChildren()) {
    Path childDir = child.getProjectDirectory().toPath();
    String taskPath = buildTaskPath(child); // e.g. ":app:dependencies"
    // run dependencies for each child
}
```

### Reference

- `GradleToolingApiExecutor.java` — `executeViaToolingApiWithProjectModel()`, `findTargetProject()`, `buildTaskPath()`
- `CompositeDependencyTreeCommandExecutor.java` — routing decision between Maven (process) and Gradle (Tooling API)

---

## Summary Checklist

When adding new build tool invocation code:

- [ ] Are you using the **Gradle Tooling API** for Gradle dependency resolution? (Rule 10)
- [ ] Are you closing `ProjectConnection` in `finally` (or `closeQuietly`)? (Rule 10)
- [ ] Are you using `GradleProject.getChildren()` to enumerate multi-module Gradle subprojects? (Rule 10)
- [ ] Are you passing only **resolvable** configurations to `--configuration`? (legacy process/Maven path — Rule 1)
- [ ] Are you excluding `build/`, `target/`, `.gradle/`, `.git/` when discovering build files? (Rule 2)
- [ ] Are you matching build files by **exact name** (not `*.gradle`)? (Rule 3)
- [ ] Are you walking up parent directories to find `gradlew`/`mvnw`? (legacy process/Maven path — Rule 4)
- [ ] Are you checking wrapper availability before failing on system command? (legacy process/Maven path — Rule 5)
- [ ] ~~For multi-module Gradle: are you using `:moduleName:dependencies` task path notation from the root?~~ (legacy process path only — superseded by Rule 10)
- [ ] Is the UI reset to a pending state before the async operation starts? (Rule 6)
- [ ] Are notification deduplication keys time-based (not permanent)? (Rule 7)
- [ ] Are you using `Files.walkFileTree` (not `Files.walk`) for deep directory traversal? (Rule 8)

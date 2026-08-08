# ADR 0005: Adopt Gradle Tooling API for Dependency Resolution

## Status

Accepted

## Context

The transitive dependency scanner (`TransitiveDependencyScannerImpl`) relies on `DependencyTreeCommandExecutorImpl` to resolve Gradle dependencies. The current approach runs `gradle dependencies` as an external process and parses stdout with regex. This has proven fragile across multiple failure modes:

### Current Architecture

```
DependencyTreeCommandExecutorImpl
  ├── findGradleWrapper()           — walks up 10 parent dirs to find gradlew
  ├── isGradleAvailableForProject() — checks system gradle, then wrapper
  ├── buildGradleCommand()          — constructs: gradlew dependencies --quiet --no-daemon
  ├── executeCommand()              — spawns Process, reads stdout
  └── parseGradleOutput()           — regex-parses tree characters (│, ├──, \) and group:artifact:version
```

### Known Fragility Points

1. **Multi-module projects**: Running `gradle dependencies` from a submodule directory resolves to the **root project** (which typically has no dependencies). We recently added `findGradleProjectRoot()` + `computeGradleModuleName()` to detect submodules and use `:moduleName:dependencies` task path notation — a workaround for a problem the Tooling API solves natively.

2. **Stdout format coupling**: `parseGradleOutput()` depends on Gradle's tree-drawing characters (`│`, `├──`, `\`). These are not part of any stable API and can change between Gradle versions. The parser also assumes configuration headers end with `"Classpath"` or `"Configuration"` — an undocumented convention.

3. **Wrapper detection**: `findGradleWrapper()` walks up 10 parent directories looking for `gradlew`/`gradlew.bat`. This duplicates logic that the Tooling API handles automatically.

4. **Configuration filtering**: We removed `--configuration` flags (Gradle only accepts a single value, causing exit code 1 with multiple flags). The parser now reads ALL configurations from output. This works but means we parse configuration sections we don't need.

5. **Daemon lifecycle**: Each scan spawns a new Gradle process. For projects with many submodules or repeated scans, this is wasteful. The Tooling API reuses the Gradle daemon across calls.

6. **Version compatibility**: No guarantee that our stdout parsing works across Gradle 4.x–10.x. The Tooling API is explicitly version-independent.

### What Works Well (Keep)

- `BuildFileDiscovery` — simple directory walking, correct exclusions, no fragility
- Maven path — `mvn dependency:tree` with JSON output (`-DoutputType=json`) works reliably; Maven Resolver (Aether) would be overkill
- `ScopeConstants` — configuration-to-scope mapping is still needed for classification

## Decision

Adopt the **Gradle Tooling API** (`org.gradle:gradle-tooling-api`) as the primary mechanism for Gradle dependency resolution.

### What Changes

| Component | Current | Proposed |
|-----------|---------|----------|
| Gradle dependency resolution | External process + stdout parsing | Tooling API (`ProjectConnection`) |
| Multi-module handling | `findGradleProjectRoot()` + `:moduleName:dependencies` | `GradleProject.getChildren()` + per-subproject resolution |
| Wrapper detection | Manual directory walking | Tooling API (wrapper-aware by default) |
| Daemon management | New process per scan | Daemon reuse via `GradleConnector` |
| Configuration filtering | Parse all, filter in code | Query specific configurations via model |

### What Stays

| Component | Reason |
|-----------|--------|
| `BuildFileDiscovery` | Simple, correct, needed for initial file discovery |
| Maven dependency resolution | `mvn dependency:tree` works; Aether is overkill |
| `ScopeConstants` | Still needed for configuration-to-scope mapping |
| `DependencyTreeResult` domain model | Shared between old and new paths |
| `TransitiveDependencyScannerImpl` | Orchestrator — calls new executor instead of old one |

### What Gets Deleted

| Component | Reason |
|-----------|--------|
| `GradleBuildParser` | Regex parsing of build.gradle — superseded by Tooling API; silent fallback hides data quality differences |
| `DependencyTreeCommandExecutorImpl` (Gradle methods) | All Gradle-specific code (~206 lines) replaced by `GradleToolingApiExecutor` |
| `findGradleProjectRoot()` | Tooling API detects project root via `GradleConnector.forProjectDirectory()` |
| `computeGradleModuleName()` | Tooling API provides `GradleProject.getChildren()` natively |
| `findGradleWrapper()` | Tooling API is wrapper-aware by default |
| `parseGradleOutput()` / `parseGradleLine()` | Structured model objects replace regex stdout parsing |

**Principle**: No hidden fallback paths. If the Tooling API can't resolve dependencies (e.g., Gradle not installed, build script broken), we surface a clear error to the user — not silently fall back to regex which produces incomplete/unreliable results.

### Proposed Architecture

```
GradleToolingApiExecutor (new)
  ├── GradleConnector.newConnector().forProjectDirectory(root)
  ├── connection.model(GradleProject.class)        — get project hierarchy
  ├── connection.newBuild().forTasks(task).run()    — run dependency resolution
  └── Structured model objects (no regex)

DependencyTreeCommandExecutorImpl (Maven only)
  └── Maven dependency:tree via external process (unchanged)
```

### Usage Example

```java
try (ProjectConnection connection = GradleConnector.newConnector()
        .forProjectDirectory(projectRoot.toFile())
        .connect()) {

    // Get full project hierarchy — no manual settings.gradle detection
    GradleProject project = connection.model(GradleProject.class).get();

    // Enumerate subprojects — replaces findGradleProjectRoot + computeGradleModuleName
    for (GradleProject child : project.getChildren()) {
        String taskPath = ":" + child.getName() + ":dependencies";
        connection.newBuild().forTasks(taskPath).run();
    }

    // Or resolve dependencies for the whole build
    connection.newBuild().forTasks("dependencies").run();
}
```

### Dependency Coordinates

```kotlin
// In premium-core-engine/build.gradle.kts
dependencies {
    implementation("org.gradle:gradle-tooling-api:8.12")
    // Runtime: Tooling API auto-downloads the correct Gradle version
    // SLF4J is already on the classpath
}
```

## Consequences

### Positive

- **Eliminates all stdout/regex parsing for Gradle** — structured model objects instead of fragile text parsing
- **Multi-module handled natively** — `GradleProject.getChildren()` returns all subprojects; no task path notation hacks
- **Wrapper-aware by default** — Tooling API finds `gradlew` automatically, no manual directory walking
- **Daemon reuse** — subsequent calls are fast (warm daemon); current approach spawns a new process each time
- **Version-independent** — Tooling API 8.x works with Gradle 4.x through 10.x+
- **Configuration-resolvable** — can query specific configurations programmatically instead of parsing all output
- **Better error handling** — structured exceptions instead of exit code + empty output guessing

### Negative

- **New runtime dependency** — ~2MB library added to `premium-core-engine`
- **Daemon startup cost** — first call in a session may take 5–10s to start daemon (offset by reuse on subsequent calls)
- **No offline resolution** — if Gradle daemon can't start (no JVM, corrupted install), the scan fails with a clear error message. No silent regex fallback.
- **IntelliJ Plugin considerations** — Tooling API spawns Gradle daemon processes; IntelliJ already manages its own Gradle integration. May need coordination to avoid duplicate daemons.

### Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Tooling API version incompatible with target Gradle | Low | Medium | Tooling API supports 5 major versions; pin to recent stable |
| Daemon startup too slow for first scan | Medium | Low | Show "Connecting to Gradle..." progress; daemon stays warm |
| IntelliJ daemon conflict | Medium | Medium | Use `GradleConnector.useGradleUserDir()` to share daemon with IDE |
| Gradle not installed on user machine | Low | High | Clear error message: "Gradle not found. Install Gradle or add gradlew to project." |

## References

- [Gradle Tooling API documentation](https://docs.gradle.org/current/userguide/tooling_api.html)
- [GradleProject model](https://docs.gradle.org/current/javadoc/org/gradle/tooling/model/GradleProject.html)
- [ProjectConnection](https://docs.gradle.org/current/javadoc/org/gradle/tooling/ProjectConnection.html)
- [Dependency tree resolution via Tooling API](https://docs.gradle.org/current/userguide/dependency_graph_resolution.html)
- Current implementation: `DependencyTreeCommandExecutorImpl.java`
- Multi-module workaround: `findGradleProjectRoot()`, `computeGradleModuleName()`, `buildGradleModuleCommand()`
- Related roadmap: [docs/roadmap/gradle-tooling-api-migration.md](../roadmap/gradle-tooling-api-migration.md)

# Fast Lint / Compile / Test Startup (Agentic Dev Feedback Loop)

This doc focuses on **speeding up the dev tooling feedback loop**: Gradle startup, compile, test, and lint (Checkstyle, PMD, SpotBugs)—not production application startup.

---

## 1. Gradle: Daemon, Configuration Cache, Build Cache

These affect **every** Gradle run (compile, test, lint, bootRun).

| Setting | Purpose | Already in this project |
|--------|----------|-------------------------|
| **Gradle daemon** | Keeps a JVM warm; avoids cold JVM startup on each run | ✅ `org.gradle.daemon=true` |
| **Parallel execution** | Run independent tasks in parallel | ✅ `org.gradle.parallel=true` |
| **Build cache** | Reuse task outputs (compile, test) when inputs unchanged | ✅ `org.gradle.caching=true` |
| **Configuration cache** | Skip configuration phase when build scripts unchanged | ✅ Enabled in `gradle.properties` (see below) |
| **Daemon JVM args** | Faster daemon cold start, lower memory | ✅ `TieredStopAtLevel=1`, smaller heap |

**Configuration cache** is the biggest win for “second and subsequent” runs: Gradle skips evaluating `build.gradle.kts` and plugin configuration. Enable it in `gradle.properties`:

```properties
org.gradle.configuration-cache=true
```

If a plugin or build script is not compatible, Gradle will fail with a clear message; you can then fix or temporarily disable. Gradle 9+ recommends it.

**Daemon JVM args** (already in this project’s `gradle.properties`):

- `-XX:TieredStopAtLevel=1` – C1 compiler only for the daemon: faster cold start, slightly slower peak throughput. Good for short, repeated runs (compile/test/lint).
- Smaller `-Xmx` / `-XX:MaxMetaspaceSize` – Less memory, faster GC during config. Increase if you see OOMs.

---

## 2. Test JVM Startup

The **test** task runs your tests in a **forked JVM**. That JVM starts from cold on each test run unless the daemon reuses workers. Faster startup for that JVM = faster `./gradlew test` feedback.

**Already done in this project:** The test task is configured with JVM args that favor fast startup:

- `-XX:TieredStopAtLevel=1` – C1 only; skip C2 warmup.
- `-XX:+UseSerialGC` – Less GC overhead during short test runs.
- `-Xshare:on` – Use the default CDS archive if the JDK provides one (faster class loading).

**Trade-off:** Peak throughput of the test JVM is slightly lower. For agentic “run tests often” loops, startup usually dominates.

**Optional:** Keep `maxParallelForks` at 1 (default) for small/medium suites so you only pay for one test JVM startup. Increase only if test wall-clock time is dominated by CPU and tests are safe to run in parallel.

---

## 3. Lint / Code-Quality Tasks (Checkstyle, PMD, SpotBugs)

These run as **Gradle worker processes** (separate JVMs). They benefit from:

1. **Same daemon JVM args** – Workers can inherit or use similar settings; `org.gradle.jvmargs` in `gradle.properties` (with `TieredStopAtLevel=1`, etc.) helps worker startup too.
2. **Build cache** – Cached task outputs mean Gradle may skip re-running lint when inputs are unchanged.
3. **Running only what you need** – e.g. `./gradlew checkstyleMain` instead of `codeQualityCheck` when you only care about style.

**Optional:** If a plugin exposes `jvmArgs` for the lint task (e.g. SpotBugs), you can add the same fast-startup flags (`-XX:TieredStopAtLevel=1`, `-XX:+UseSerialGC`) there. Not all plugins expose this; the daemon/worker defaults already help.

---

## 4. Compile

- **Incremental compilation** – Gradle’s Java plugin compiles only changed sources; keep it enabled (default).
- **Compile avoidance** – Gradle skips compile when outputs are up-to-date; build cache can reuse them across builds.
- **Single daemon** – Using one Gradle daemon (default) keeps compiled code and metaspace warm across `compileJava` / `test` / `check` runs.

No extra JVM “startup” per se for compile beyond Gradle’s daemon and any worker JVMs; configuration cache + daemon args are what matter.

---

## 5. Summary: What This Project Does for Lint/Compile/Test

| Area | What’s configured | Effect |
|------|-------------------|--------|
| **Gradle daemon** | `org.gradle.daemon=true`, `org.gradle.jvmargs` with TieredStopAtLevel=1, smaller heap | Faster daemon cold start, less memory |
| **Configuration cache** | `org.gradle.configuration-cache=true` | Skip config phase on repeat runs |
| **Build cache** | `org.gradle.caching=true` | Reuse compile/test/lint outputs |
| **Test JVM** | `jvmArgs` on `Test` task: TieredStopAtLevel=1, SerialGC, Xshare:on | Faster test process startup |
| **Lint** | Same daemon/worker environment; build cache for task output | Fewer redundant runs, faster worker startup |

---

## 6. Application Startup (Optional)

If you also want **faster application** startup (e.g. `bootRun` or `nativeDev`) for local runs, see the sections below. They are secondary to the lint/compile/test focus above.

### 6.1 Class Data Sharing (CDS) / AppCDS for bootRun

- **What:** Preload class metadata (and with AppCDS, app classes) from a shared archive so the JVM starts faster.
- **Use case:** Faster `bootRun` (or `bootRunWithCds` in this project).
- **Gain:** ~30–47% faster JVM startup for the app process.
- **How:** Use `./gradlew bootRunWithCds` (creates/uses `build/app.jsa`). Recreate archive after dependency changes.

### 6.2 CRaC (checkpoint/restore)

- **What:** Checkpoint a running JVM and restore from snapshot for later runs.
- **Use case:** Application startup only; **Linux only** (WSL2/Docker possible on Windows).
- **Gain:** Order-of-magnitude faster “start” after first checkpoint (e.g. 4s → 38ms).
- **Not applicable** to Gradle, test, or lint JVM startup.

### 6.3 GraalVM Native Image (nativeDev)

- **What:** Build a native executable; no JVM for the app at runtime.
- **Use case:** Fastest app startup when you’re willing to pay the native build time.
- **Not applicable** to compile/test/lint; those still run on the JVM (Gradle, test task, lint workers).

---

## 7. Recommended Order for Your Agentic Loop (Lint/Compile/Test)

1. **Keep** `org.gradle.daemon=true`, `org.gradle.parallel=true`, `org.gradle.caching=true`, and the current `org.gradle.jvmargs` (TieredStopAtLevel=1, etc.).
2. **Enable** configuration cache in `gradle.properties` (`org.gradle.configuration-cache=true`).
3. **Use** the test task’s JVM args (TieredStopAtLevel=1, SerialGC, Xshare:on) so each test run starts its JVM faster.
4. **Run** only the tasks you need (e.g. `test`, `checkstyleMain`, `compileJava`) instead of full `build` when iterating.
5. **Optionally** use `bootRunWithCds` or `nativeDev` when you care about app startup; that does not change lint/compile/test startup.

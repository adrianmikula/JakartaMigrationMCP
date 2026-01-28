# Gradle / Test / Compile Startup Benchmark Results

Comparison runs to verify the lint/compile/test JVM startup improvements (configuration cache, test JVM args, daemon JVM args).

---

## 1. Configuration Cache Impact (compileJava)

| Run | Time | Notes |
|-----|------|--------|
| **With configuration cache** | **~1.5–1.7s** | "Configuration cache entry reused." (2 runs avg ~1.6s) |
| **Without configuration cache** | **~3.5–4.2s** | Full configuration phase each run (2 runs avg ~3.9s) |

**Result:** Configuration cache saves **~58%** on subsequent runs (no cache ~3.9s → with cache ~1.6s).

*Measured on Windows with `.\scripts\benchmark-gradle-startup.ps1 -Iterations 2`; also observed ~48% in an earlier run (6.9s vs 13.3s with different daemon state).*

---

## 2. Daemon vs No-Daemon (compileJava)

| Run | Time | Notes |
|-----|------|--------|
| **Without daemon** (`--no-daemon --no-configuration-cache`) | **~13s** per run | New JVM each time (2 runs avg ~13.0s) |
| **With daemon** (`--configuration-cache`, warm) | **~1.5s** per run | Reuses warm JVM (2 runs avg ~1.6s) |

**Result:** Daemon saves **~88%** (no daemon ~13.0s → daemon ~1.6s).

*Measured with `.\scripts\benchmark-gradle-startup.ps1 -Iterations 2` after stopping the daemon. Daemon + configuration cache together give the largest improvement for repeated compile/test/lint runs.*

---

## 3. Test JVM Args (TieredStopAtLevel=1, SerialGC, CDS)

The test task is configured with:

- `-XX:TieredStopAtLevel=1` – C1 only; skip C2 warmup.
- `-XX:+UseSerialGC` – Less GC overhead for short test runs.
- `-Xshare:auto` – Use default CDS archive when available.

**Expected:** The forked test JVM starts faster; total `./gradlew test` time is lower when test execution is short. For long-running tests, the effect is smaller relative to test runtime.

*To compare manually: run `./gradlew test` with the current build (optimized) vs a build with the test `jvmArgs` block commented out; compare total elapsed time.*

---

## 4. How to Reproduce

### Configuration cache comparison

```powershell
# With configuration cache (after first run has populated cache)
.\gradlew compileJava --configuration-cache

# Without configuration cache
.\gradlew compileJava --no-configuration-cache
```

Compare the "BUILD SUCCESSFUL in Xs" lines or use a stopwatch around the full command.

### Full benchmark script

Run the benchmark script (may require fixing test task filters if tests fail in your environment):

```powershell
.\scripts\benchmark-gradle-startup.ps1 -Iterations 2
.\scripts\benchmark-gradle-startup.ps1 -Clean -Iterations 2
```

---

## 5. Summary

| Optimization | Effect |
|--------------|--------|
| **Configuration cache** | ~48% faster subsequent runs (compileJava) |
| **Gradle daemon** | Avoids JVM cold start on every run |
| **Daemon JVM args** (TieredStopAtLevel=1, smaller heap) | Faster daemon cold start |
| **Test JVM args** (TieredStopAtLevel=1, SerialGC, CDS) | Faster test process startup |

These optimizations target the **lint/compile/test feedback loop**, not production application startup.

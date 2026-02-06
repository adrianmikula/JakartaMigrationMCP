# Build tool startup benchmark

Quick comparison of **Gradle** vs **Mill** startup and first-task performance on this project.

## How to run locally

From repo root (Windows):

```powershell
.\scripts\benchmark-build-startup.ps1
```

Or run individual commands and time them:

```powershell
# Gradle cold
Measure-Command { .\gradlew.bat help --no-daemon -q }

# Gradle warm (daemon)
Measure-Command { .\gradlew.bat help -q }

# Mill
Measure-Command { .\mill.bat show version }
Measure-Command { .\mill.bat jakartaMigrationMcp.compile }
```

On Linux/WSL (full comparison including successful compile/test):

```bash
# Gradle
time ./gradlew help --no-daemon -q
time ./gradlew compileJava --no-daemon
time ./gradlew test --no-daemon

# Mill
time ./mill show version
time ./mill jakartaMigrationMcp.compile
time ./mill jakartaMigrationMcp.test
```

## Sample results (Windows, single run)

| Tool   | Task           | Time (s) | Note                          |
|--------|----------------|----------|-------------------------------|
| Gradle | help (cold)    | **17.55**| Build may fail (e.g. 25.0.2)  |
| Gradle | help (warm)    | **3.28** | With daemon already up        |
| Mill   | show version   | **2.6**  | Startup + build script load   |
| Mill   | compile        | **2.14** | Fails at build.sc compile on Windows |

**Takeaway:**
- **Cold:** Mill (~2.1–2.6 s) is **~6–8× faster** than Gradle (~17.5 s).
- **Warm:** Mill (~2.1–2.6 s) is still **~20–40% faster** than Gradle with daemon (~3.3 s).

## Caveats

- **Gradle:** Current project may fail with an error (e.g. `25.0.2`) on some tasks; times above include startup until that failure.
- **Mill on Windows:** `build.sc` can fail to compile with `value ivy is not a member of StringContext`; the reported “compile” time is still a good proxy for Mill startup + script compilation attempt.
- For **full compile + test** comparison (both tools succeeding), run the Linux/WSL commands above or use CI (Mill runs on Linux in GitHub Actions).

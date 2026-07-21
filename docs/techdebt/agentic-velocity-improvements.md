# Agentic Velocity Improvements

This document catalogs remaining improvements to optimize the repo for AI agent-assisted coding. These are items identified after the initial 5-layer optimisation.

## Quick Wins

### 1. Pre-Commit Hooks

**Problem:** No automated guard catches broken code before `git commit`. Agents can push code with compilation errors or failing tests.

**Solution:** Add a `.githooks/pre-commit` hook that runs:
```bash
./gradlew :community-core-engine:fastTest --no-daemon
```
Configure with `git config core.hooksPath .githooks`.

**Effort:** 15 minutes

---

### 2. GitHub Issue/PR Templates

**Problem:** No structured templates exist in `.github/ISSUE_TEMPLATE/` or `PULL_REQUEST_TEMPLATE.md`. Agents lack guidance on contribution structure.

**Solution:** Create templates:
- `.github/ISSUE_TEMPLATE/bug_report.yml` — structured bug report form
- `.github/ISSUE_TEMPLATE/feature_request.yml` — feature request form  
- `.github/PULL_REQUEST_TEMPLATE.md` — PR description template

**Effort:** 30 minutes

---

### 3. Gradle Build Scans

**Problem:** No telemetry on build performance. Without `--scan` or Develocity, it's unclear what's slow.

**Solution:** 
- Add `develocity` plugin or publish build scans to `scans.gradle.com` on CI
- Configure build scan terms-of-service acceptance in CI only

**Effort:** 30 minutes

## Medium Impact

### 4. Persistent Test Runner

**Problem:** Even `fastTest` tasks take 10-40s due to JVM startup per Gradle invocation. True sub-second feedback requires a persistent JVM.

**Solution:** Implement a JUnit 5 Launcher API daemon that:
- Starts a JVM once, loads test classes
- Receives hot code replacements via ByteBuddy/HotSwapAgent
- Executes tests as in-process method calls (<20ms per test)
- Supports incremental compilation between runs

**Reference:** `docs/techdebt/java-persistent-test-runner.md` if created, or see `docs/ai/fast-test-loop.md` for current setup.

**Effort:** 1-2 days

**Real-world numbers:**
- 50 tests: 50-100ms (vs 10-40s with Gradle)
- 200 tests: 200-400ms
- Single test: <20ms

---

### 5. Module-Level Code Map

**Problem:** Agents spend time locating which package lives in which module. The 5-module structure isn't mapped anywhere.

**Solution:** Create `CODEMAP.md` at root with:

```
## Module: community-core-engine
adrianmikula.jakartamigration.dependencyanalysis  — Maven dependency resolution
adrianmikula.jakartamigration.sourcecodescanning   — Java source file scanning
adrianmikula.jakartamigration.config               — Feature flags, YAML config
adrianmikula.jakartamigration.util                 — GitIgnore, file scanning, AgentLogger
```

**Effort:** 30 minutes

---

### 6. `.github/CODEOWNERS`

**Problem:** No ownership mapping for PR auto-assignment.

**Solution:** Create `.github/CODEOWNERS`:
```
/community-*/   @adrianmikula
/premium-*/     @adrianmikula
```

**Effort:** 5 minutes

## Transformative (Significant Effort)

### 7. Fix ErrorProne Plugin

**Problem:** `net.ltgt.errorprone` plugin is broken in `:community-mcp-server` and `:premium-mcp-server`, blocking compilation. This is the module that actually ships the product.

**Solution:** Either remove the plugin declaration from affected modules or upgrade to a compatible version. Documented in `docs/troubleshooting/COMMON_ISSUES.md` as ERR-005.

**Effort:** 1-2 hours

---

### 8. Kotlin Migration for Agent Velocity

**Problem:** Java boilerplate (getters, constructors, builders) forces verbose diffs and more tokens per change. LLMs produce cleaner Kotlin than Java.

**Solution:** Migrate implementation modules to Kotlin incrementally, starting with `community-core-engine` (no Spring, no IntelliJ SDK). Keep Java at module boundaries only.

**Reference:** `docs/techdebt/java-21-adoption-limitations.md`

**Effort:** 1-2 weeks (incremental, per-module)

---

### 9. CRaC-Enabled CI Pipeline

**Problem:** CRaC JDK is configured for local dev but CI still uses standard JDK.

**Solution:** Update CI workflows to use `zulu-21-crac` JDK and add checkpoint/restore smoke tests.

**Effort:** 1 day

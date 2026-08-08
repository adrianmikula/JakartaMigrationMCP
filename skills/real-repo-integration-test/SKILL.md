ana# Real-Repo Integration Test Skill

## Overview

This skill guides writing integration tests that download and test against **real GitHub repositories**. The tests verify that Jakarta Migration recipes, scans, and analysis work correctly against actual open-source projects.

## Reference Tests

- `RefactorRecipeIntegrationTest.java` — Recipe integration test with `downloadExample` helper, recipe seeding, undo verification, and execution history tests
- `MavenPropertyResolutionIntegrationTest.java` — Canonical download pattern for simple URL-based extraction

## Generic Download Helper Pattern

Use `ZipInputStream`-based streaming extraction with branch fallback:

```java
private Path downloadExample(String projectName) throws IOException {
    String repoUrl = findProjectUrl(projectName);
    Path extractDir = tempDir.resolve("examples").resolve(repoNameFromUrl(repoUrl));
    if (Files.exists(extractDir)) {
        return resolveProjectRoot(extractDir);
    }

    if (repoUrl.contains("/tree/")) {
        String zipUrl = toArchiveZipUrl(repoUrl);
        return downloadAndExtract(zipUrl, extractDir);
    }

    IOException lastException = null;
    for (String branch : new String[]{"main", "master", "develop"}) {
        String zipUrl = repoUrl + "/archive/refs/heads/" + branch + ".zip";
        try {
            return downloadAndExtract(zipUrl, extractDir);
        } catch (IOException e) {
            lastException = e;
        }
    }
    throw new IOException("Failed to download repo from known branches for: " + repoUrl, lastException);
}

private Path downloadAndExtract(String zipUrl, Path extractDir) throws IOException {
    URL url = new URL(zipUrl);
    try (InputStream in = url.openStream();
         ZipInputStream zis = new ZipInputStream(in)) {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            Path out = extractDir.resolve(entry.getName());
            if (entry.isDirectory()) {
                Files.createDirectories(out);
            } else {
                Files.createDirectories(out.getParent());
                Files.copy(zis, out);
            }
            zis.closeEntry();
        }
    }
    return resolveProjectRoot(extractDir);
}

private String findProjectUrl(String projectName) throws IOException {
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    Map<String, Object> data;
    try (InputStream is = getClass().getResourceAsStream("/examples.yaml")) {
        if (is == null) {
            throw new RuntimeException("examples.yaml not found on classpath");
        }
        data = yamlMapper.readValue(is, Map.class);
    }

    String target = projectName.toLowerCase();
    for (Map.Entry<String, Object> entry : data.entrySet()) {
        Object value = entry.getValue();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Object nameObj = map.get("name");
                    if (nameObj instanceof String name && name.toLowerCase().contains(target)) {
                        Object urlObj = map.get("url");
                        if (urlObj instanceof String url) {
                            return url;
                        }
                    }
                }
            }
        }
    }
    throw new IllegalArgumentException("Project not found in examples.yaml: " + projectName);
}

private String repoNameFromUrl(String url) {
    String clean = url.replaceAll("/$", "");
    int lastSlash = clean.lastIndexOf('/');
    if (lastSlash >= 0 && lastSlash < clean.length() - 1) {
        return clean.substring(lastSlash + 1);
    }
    return clean;
}

private String toArchiveZipUrl(String repoUrl) {
    String branch = repoUrl.substring(repoUrl.indexOf("/tree/") + 6);
    int nextSlash = branch.indexOf('/');
    if (nextSlash > 0) {
        branch = branch.substring(0, nextSlash);
    }
    String base = repoUrl.substring(0, repoUrl.indexOf("/tree/"));
    return base + "/archive/refs/heads/" + branch + ".zip";
}

private Path resolveProjectRoot(Path extractDir) throws IOException {
    try (var stream = Files.list(extractDir)) {
        List<Path> children = stream.toList();
        if (children.size() == 1 && Files.isDirectory(children.get(0))) {
            return children.get(0);
        }
    }
    return extractDir;
}
```

## How to Pick Repos from examples.yaml

### Step 1: Identify what the test needs

Determine which `javax.*` imports the recipe/scan targets:
- `javax.servlet` → Servlet APIs
- `javax.persistence` → JPA entities
- `javax.validation` → Bean Validation
- `javax.inject` → CDI/dependency injection
- `javax.ejb` → Enterprise JavaBeans
- `javax.ws.rs` → JAX-RS REST endpoints
- `javax.websocket` → WebSocket APIs

### Step 2: Verify the repo actually contains matching imports

**Never assume a repo has matching imports.** Verify by:
1. Downloading the repo zip to `/tmp/repo-scan/`
2. Grepping for the target imports: `grep -r "import javax\.servlet" repo/ --include="*.java" -l`
3. Counting matches to ensure meaningful coverage

### Step 3: Map repos to test methods

| Recipe | Verified Repo | Import Count |
|--------|--------------|-------------|
| MigrateServlet | `tgupta018/J2EE7Samples` | 154 files |
| MigrateJPA | `tgupta018/J2EE7Samples` | 93 files |
| MigrateBeanValidation | `asniii/JavaxValidation` | 2 files |
| MigrateCDI | `tgupta018/J2EE7Samples` | 194 files |
| MigrateEJB | `tgupta018/J2EE7Samples` | 118 files |
| MigrateREST | `tgupta018/J2EE7Samples` | 118 files |

## Data Quality Pitfalls

### Problem: Mismatched repo and recipe

The most common failure mode is using a repo that doesn't contain the `javax.*` imports the recipe targets. Example:
- Using `ismailfer/java-jetty10-websocket-javax` for `MigrateServlet` — this repo has `javax.websocket`, NOT `javax.servlet`
- Result: `filesChanged=0` because the regex never matches any file content

### Problem: URLs with `/tree/` paths

URLs like `https://github.com/org/repo/tree/main/subdir` get converted to archive URLs via `toArchiveZipUrl()` which extracts the branch from the path. If the branch in the URL doesn't exist (e.g., `main` when only `master` exists), the download fails. The `downloadExample()` method only falls back branches for non-`/tree/` URLs.

### Problem: Large repos

Some repos (like J2EE7Samples at 2.7MB zip) take longer to download and extract. Ensure test timeouts account for network latency.

## Checklist for Writing Real-Repo Integration Tests

- [ ] **Verify imports exist**: Download the repo and grep for the target `javax.*` imports before writing assertions
- [ ] **Use `@Tag("slow")`**: All real-repo tests must be tagged `slow` to exclude from `fastTest`
- [ ] **Hardcode repo-recipe pairings**: Each test method should use a specific repo known to contain matching imports
- [ ] **Assert `filesChanged() > 0`**: This confirms the recipe actually matched and modified files
- [ ] **Verify post-condition**: Check that changed files no longer contain the old `javax.*` imports
- [ ] **Use `changedFilePaths()` directly**: These are relative paths; resolve against `sampleDir` to read content
- [ ] **No `assumeTrue` skips**: Every test should execute meaningfully or be removed
- [ ] **Test undo on modified files**: Pick a file that WILL be changed by the recipe, not just any file

## Test Integrity and Anti-Patterns Review

When writing or reviewing real-repo integration tests for dependency/artifact classification, verify the tests can actually catch false positives and false negatives. Prefer assertions on concrete artifact coordinates, `Namespace`, `ScanReason`, `severity`, and `recommendation` content.

### Positive vs negative coverage checklist

- [ ] **Positive (Jakarta) cases**: use a known Jakarta-only real repo (e.g. Spring Boot 3/Quarkus 3 sample) and assert that no `javax`/`high` false positives appear, except for JDK-provided `javax.*` packages.
- [ ] **Negative (javax) cases**: use a Java EE 7/8 repo (e.g. `J2EE7Samples`) and assert that expected `javax.*` artifacts are `BLACKLISTED`/`high` with a correct Jakarta recommendation.
- [ ] **Unknown/JDK cases**: assert that `com.google.guava`, `javax.sql`, `javax.xml.parsers`, `javax.xml.stream`, etc. are `UNKNOWN` or `WHITELISTED` with `low` severity.
- [ ] **Mixed cases**: assert that a dependency tree with both `javax` and `jakarta` produces `MIXED`/`TRANSITIVE_INCOMPATIBLE` only where expected.

### Logic-flaw red flags

- **Vague pass conditions**: `isNotNull()`, `isGreaterThan(0)`, `isTrue()` for "at least one high" cannot catch mis-classification. Always assert *which* artifact/scope is high.
- **Self-fulfilling helpers**: helpers that build synthetic POMs using non-existent or internally-inconsistent coordinates (e.g. `javax.servlet:jakarta.servlet-api`) only prove the regex parses a string, not real classification.
- **Version preservation fallacy**: do not assert a Jakarta equivalent keeps the javax version (e.g. `jakarta.persistence-api:2.2`). The Jakarta version is independent and must be resolved from the mapping or Maven Central.
- **Vacuous binary compatibility tests**: do not assert `blockers` is merely non-null when the test setup already guarantees no blockers; add a second test that actually lacks a Jakarta mapping and expects a `NO_JAKARTA_EQUIVALENT` blocker.
- **One-sided propagation checks**: transitive propagation should mark the *incompatible child* as high, but should not turn a `JAKARTA` parent into `TRANSITIVE_INCOMPATIBLE` unless that is the intended behavior.

### Build-system variety to test

- Multi-module Maven with parent POM, `<modules>`, `<dependencyManagement>`, properties, BOMs, and wrapper.
- Multi-module Gradle with `settings.gradle(.kts)`, `build.gradle.kts` and Groovy DSL, version catalogs, `platform`, `project(':...')`, `buildSrc`.
- Old build-file formats vs new (Maven 3 minimal POM, `pom` packaging, legacy Gradle `apply plugin` style).
- IDE-only or build-tool-less projects (`.idea/libraries`, `.classpath`, `.project`, plain `lib/` JARs).

### Verification step

After applying fixes, re-run the relevant real-repo tests and inspect the first 5–10 `TransitiveDependencyUsage` records for each repo. Confirm:
1. Expected `javax` artifacts are `BLACKLISTED`/`high`.
2. Expected `jakarta` artifacts are `WHITELISTED`/`low`.
3. JDK `javax.*` packages are not flagged.
4. No `JAKARTA` parent is silently rewritten to `TRANSITIVE_INCOMPATIBLE`.

## RecipeServiceImpl.applyRegexRecipe Behavior

### Key return values

| Field | Meaning | How computed |
|-------|---------|-------------|
| `filesProcessed` | Total files matching `filePattern` glob | Count of files whose relative path matches the converted regex |
| `filesChanged` | Files where regex actually matched content | Subset of processed files where `pattern.matcher(content).find()` returned true |
| `changedFilePaths` | Relative paths of modified files | Same list as `filesChanged` — always in sync |

### When `filesChanged=0`

This happens when:
1. No files match the `filePattern` glob (e.g., `**/*.java` finds no `.java` files)
2. Files match the glob but don't contain the target pattern in their content
3. **Bug**: The glob-to-regex conversion was broken (see fixed `RecipeServiceImpl.java:152-156`)

### Glob-to-regex conversion (fixed)

```java
String regex = patternGlob
    .replace("**", "<<DSTAR>>")   // Protect ** before single * replacement
    .replace(".", "\\.")           // Escape literal dots
    .replace("*", "[^/]*")        // Single * matches non-slash chars
    .replace("<<DSTAR>>", ".*");  // Restore ** → .* (matches anything including /)
```

## Reference Files

- `RefactorRecipeIntegrationTest.java` — recipe integration test with download helper, undo, history, and category tests
- `MavenPropertyResolutionIntegrationTest.java` — simple download-and-extract pattern
- `RecipeServiceImpl.applyRegexRecipe()` — regex recipe execution logic
- `examples.yaml` — repo URLs for integration testing

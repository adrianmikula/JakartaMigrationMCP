# Hardcoded Jakarta Compatibility Detection

**Date:** 2026-07-26

## Issue

Jakarta migration classification and upgrade recommendations rely heavily on hand-maintained mappings and curated allowlists. The dynamic, “unknown in-the-wild” detection path is narrow and only fires for a small subset of artifacts.

## Current State

### Hardcoded sources

- **`ImprovedMavenCentralLookupService`**
  - `ARTIFACT_MAPPINGS`: 64 hardcoded artifact renames
  - `GROUP_MAPPINGS`: 40 hardcoded group renames
  - These cover Jakarta EE API renames (`javax.servlet-api` → `jakarta.servlet-api`) and same-group framework shortcuts (CXF, Hibernate, Wicket, Spring, Jersey, etc.)

- **`JakartaMappingServiceImpl` / `jakarta-mappings.yaml`**
  - 10 hand-maintained `javax` → `jakarta` coordinate mappings
  - Per-version mapping tables
  - Hardcoded framework compatibility rules (`isJakartaFramework`): Spring Boot ≥3, Quarkus, WildFly ≥26

- **`PlatformConfigLoader` / `platforms.yaml`**
  - 20 platform/framework definitions
  - 241 hardcoded `commonArtifacts` (Spring Boot, Tomcat, WildFly, Jetty, Liberty, WebSphere, TomEE, GlassFish, Payara, JBoss EAP, Arquillian, ShrinkWrap, WebLogic, NetBeans, etc.)

- **`RecipeBasedClassifier`**
  - Coordinate and package rename maps derived from OpenRewrite recipes (fetched from GitHub, not authored here, but still a curated upstream list)
  - JAR regex fallback scans for only six `javax` and six `jakarta` package prefixes
  - Hardcoded JDK-provided `javax` exclusions

### Dynamic sources

- **`ImprovedMavenCentralLookupService` live Maven Central search**
  - Only triggered for `javax.*` groupIds, artifacts whose names contain `javax`, or a small set of special cases (`javax-`, `validation-api`, `javax.mail`)
  - Strategies: exact match, naming variations, case-insensitive, framework-specific, Jersey, Spring Boot version tricks
  - Returns a Jakarta equivalent only if Maven Central has a `jakarta.*` (or mapped) coordinate

- **JAR bytecode scanning**
  - `RecipeBasedClassifier.scanJarForNamespaces`
  - `BytecodeNamespaceClassifier`
  - Detects hardcoded `javax`/`jakarta` package strings in JAR contents

## Impact

For a random third-party library such as `org.apache.axis:axis`, `com.google.code.findbugs:jsr305`, or `org.codehaus.jackson:jackson-jaxrs` that:

- is not in the hardcoded maps,
- is not a `javax.*` artifact,
- and has no `jakarta.*` coordinate on Maven Central,

the scanner cannot determine Jakarta compatibility. It will classify the dependency as `UNKNOWN` and will not flag it as a migration blocker unless the JAR happens to contain one of the six hardcoded `javax` package prefixes.

This means the scanner misses in-the-wild libraries that use `javax` packages internally but do not expose `javax` in their Maven coordinates. The dynamic path is essentially a coordinate-based lookup, not a true compatibility inference engine.

## Current Resolution

None. The existing approach is intentionally conservative: it reports confidently only on well-known libraries. Unknown libraries remain `UNKNOWN` and are left for manual review.

## Future Options

1. **Broaden bytecode/package scanning**
   - Scan any `javax/*` package in JAR contents, not just the six hardcoded ones.
   - Use ASM or ClassGraph for accurate package detection.

2. **Package-rename-based inference**
   - For a JAR that uses `javax/foo/Bar`, infer the likely `jakarta/foo/Bar` target and search Maven Central for artifacts that contain the renamed package.

3. **Transitive `javax` exposure detection**
   - Trace transitive dependencies of unknown libraries; if an unknown library pulls in a known `javax` API, treat it as needing attention.

4. **Heuristic Maven Central search by package name**
   - Query Maven Central for artifacts that contain specific `javax`/`jakarta` packages, not just by coordinate.

## References

- `ImprovedMavenCentralLookupService.java`
- `JakartaMappingServiceImpl.java`
- `jakarta-mappings.yaml`
- `platforms.yaml`
- `RecipeBasedClassifier.java`
- `BytecodeNamespaceClassifier.java`

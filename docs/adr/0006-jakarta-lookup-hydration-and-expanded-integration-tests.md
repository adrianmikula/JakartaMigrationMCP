# ADR 0006: Jakarta Maven Central Lookup Hydration and Expanded Integration Tests

## Status

Accepted

## Context

The `ImprovedMavenCentralLookupService` relies on a mixture of static mappings (`ARTIFACT_MAPPINGS` and `GROUP_MAPPINGS`) and live Maven Central searches to recommend Jakarta-equivalent artifacts. The static fast path historically returned coordinates without a version, which made it impossible for callers to verify whether the resolved artifact was actually a Jakarta-compatible release.

At the same time, the Jakarta lookup integration tests (`JakartaLookupVerificationTest`) only covered a handful of standard `javax` artifacts. We wanted to expand coverage to ten popular third-party `javax` libraries that are not represented in any whitelist, blacklist, or framework-specific static logic, so that `ImprovedMavenCentralLookupService` and the downstream scanner are validated against real Maven Central data.

## Decision

### 1. Hydrate the static fast path with a live Maven Central lookup

When `ImprovedMavenCentralLookupService` resolves a static mapping that actually changes the group or artifact coordinate, it now performs a live `performMavenCentralSearch` for the mapped coordinate before returning. This gives the result a real `latestVersion` instead of a hard-coded `null`.

- If the live lookup succeeds, the matched coordinate with its version is returned and cached.
- If the live lookup is empty or unreachable, the service falls back to the mapped coordinate with a `null` version.
- If the static mapping is trivial (same group and same artifact, e.g. `org.apache.cxf` → `org.apache.cxf`), the live lookup is skipped to avoid duplicating the exact-match search and to keep the lookup resilient against Maven Central flakiness.

### 2. Parse both `latestVersion` and `v` fields from Maven Central responses

The Maven Central Solr response exposes the latest version under either `latestVersion` or `v` depending on the queried index. The parser now uses `latestVersion` first and falls back to `v` when `latestVersion` is absent.

### 3. Expanded `JakartaLookupVerificationTest` coverage

We extended the real-network integration test to exercise ten third-party `javax` libraries that are not covered by whitelists, blacklists, or static framework-specific logic:

- **With Jakarta equivalents:**
  - `org.apache.cxf:cxf-rt-frontend-jaxws`
  - `org.apache.cxf:cxf-rt-frontend-jaxrs`
  - `org.hibernate:hibernate-core`
  - `org.apache.wicket:wicket`
  - `org.apache.myfaces.core:myfaces-api`

- **Without Jakarta equivalents (legacy):**
  - `com.google.code.findbugs:jsr305`
  - `org.codehaus.jackson:jackson-jaxrs`
  - `org.codehaus.jackson:jackson-xc`
  - `org.apache.axis:axis`
  - `org.apache.axis:axis-jaxrpc`

The `with` assertions verify that the lookup returns the expected Jakarta-compatible coordinate. The `without` assertions verify that no `jakarta.*` group or artifact appears in the lookup results.

## Consequences

- **Positive:** Static mappings that rename an artifact or group now produce actionable, versioned recommendations without requiring a manually curated version database.
- **Positive:** The test suite now exercises both the static-mapping and live-lookup code paths against real Maven Central data for well-known third-party libraries.
- **Positive:** The `v` fallback improves robustness when Maven Central returns the latest version under the alternate field name.
- **Trade-off:** Trivial self-mappings still return `null` versions because the artifact coordinate itself does not change; the caller must rely on other signals (e.g. framework version heuristics) for those cases.
- **Trade-off:** Real-network integration tests remain tagged `slow` because they depend on Maven Central availability.

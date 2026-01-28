# Free / Premium Build and Deploy

This document summarizes how the free vs premium split affects **compile**, **test**, **run**, **deploy**, and **publish**.

## Build layout

| Aspect | Free (root) | Premium (`jakarta-migration-mcp-premium`) |
|--------|-------------|-------------------------------------------|
| **Project** | Root `build.gradle.kts` | Subproject `jakarta-migration-mcp-premium/build.gradle.kts` |
| **Compile** | `./gradlew compileJava` | `./gradlew :jakarta-migration-mcp-premium:compileJava` (depends on root) |
| **Test** | `./gradlew test` | `./gradlew :jakarta-migration-mcp-premium:test` |
| **JAR** | `./gradlew bootJar` → `build/libs/jakarta-migration-mcp-*.jar` | `./gradlew :jakarta-migration-mcp-premium:bootJar` → `jakarta-migration-mcp-premium/build/libs/jakarta-migration-mcp-premium-*.jar` |
| **Run** | `java -jar build/libs/jakarta-migration-mcp-*.jar --spring.profiles.active=mcp-streamable-http` | Same pattern with premium JAR and profile if needed |
| **Deploy** | Dockerfile, Railway, Apify use root build → free JAR | Premium image/deploy would use premium module build |
| **Publish** | Release workflow builds root `bootJar` and publishes `jakarta-migration-mcp-<version>.jar` | Premium JAR can be built/released separately |

## CI

- **`.github/workflows/ci.yml`**: Runs `./gradlew test :jakarta-migration-mcp-premium:test` so both free and premium tests are executed.

## Release

- **`.github/workflows/release.yml`**: Builds root with `./gradlew bootJar`, attaches `jakarta-migration-mcp-<version>.jar` to the GitHub release. NPM publish is for the JS MCP wrapper (`@jakarta-migration/mcp-server`), not the Java JAR.

## Deploy

- **Dockerfile**: Builds root (free) and runs `jakarta-migration-mcp-*.jar`.
- **railway.json**: `buildCommand: ./gradlew bootJar`, `startCommand: java -jar build/libs/jakarta-migration-mcp-*.jar` → free build.

## Dependencies

- **Root (free)** keeps only what the free tools need: dependency analysis, source code scanning (OpenRewrite for parsing), feature flags. No ASM, no refactoring/runtime verification implementations (those are in premium).
- **Premium** adds OpenRewrite recipes, ASM, and all pro packages (coderefactoring, runtimeverification, api, storage, licensing) and depends on root via `implementation(project(":"))`.

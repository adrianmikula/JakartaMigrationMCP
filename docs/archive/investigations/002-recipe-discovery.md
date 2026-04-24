# Investigation: OpenRewrite Recipe Discovery Failures

## Problem Statement
After resolving the Jackson conflict, standard Jakarta migration recipes (e.g., `JaxbMigrationToJakarta`) were reported as "Not found" by OpenRewrite.

## Root Causes
1. **Missing Dependencies**: To avoid building Kotlin-related conflicts, `rewrite-static-analysis` had been excluded in `build.gradle.kts`. However, many standard migration recipes depend on visitors/classes located within that module.
2. **Recipe Naming Mismatch**: The seeded recipe names in `RecipeSeeder.java` were based on older versions or shortened names (e.g., `JafMigrationToJakarta`), whereas OpenRewrite 8.x and `rewrite-migrate-java` 2.5.0 use explicit names (e.g., `JavaxActivationMigrationToJakartaActivation`).
3. **Classloader Scanning in IDE**: In the IntelliJ environment, `ClassGraph` (scanning tool) sometimes fails to find resources if the `ClassLoader` does not robustly expose URLs or if it's strictly filtered.

## Investigation Steps
1. **Diagnostic Test**: Created a temporary unit test (`ListRecipesTest.java`) to list all recipes discovered by `Environment.builder().scanRuntimeClasspath().build()`.
2. **Missing Classes**: Identified `NoClassDefFoundError` for `org.openrewrite.staticanalysis.RemoveMethodCallVisitor` when restored dependencies were scanned, confirming the need for `rewrite-static-analysis`.
3. **Name Verification**: Captured the output of the list test into `recipes_utf8.txt` to confirm the exact FQNs of the 700+ available recipes.

## Solution
1. **Dependency Restoration**: Re-enabled `rewrite-static-analysis` in `community-core-engine/build.gradle.kts`.
2. **Seeder Update**: Updated `RecipeSeeder.java` to use the confirmed names:
    - `ServletMigrationToJakarta` -> `JavaxServletToJakartaServlet`
    - `JaxbMigrationToJakarta` -> `JavaxXmlBindMigrationToJakartaXmlBind`
    - `JafMigrationToJakarta` -> `JavaxActivationMigrationToJakartaActivation`
    - etc.
3. **Robust Discovery**: Updated `RecipeServiceImpl.java` to use `scanRuntimeClasspath()` instead of a targeted classloader scan, ensuring the TCCL environment is fully utilized.

## Outcome
Recipes are now consistently discovered both in unit tests and within the IntelliJ plugin environment.

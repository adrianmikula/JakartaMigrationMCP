# Use IntelliJ SDK Indexing for Plugin Scans

Replace the `ProjectFileSystemScanner`/`DependencyTreeCommandExecutorImpl` file-walking and build-tool spawning with IntelliJ's built-in `ProjectFileIndex`, `ProjectRootManager`, and `OrderEnumerator` for source/dependency discovery.

## Current Approach

- `ProjectFileSystemScanner` (`community-core-engine/src/main/java/adrianmikula/jakartamigration/util/ProjectFileSystemScanner.java`) walks the filesystem via `Files.walkFileTree` and applies a hard-coded ignore set plus `.gitignore` rules.
- `AdvancedScanningService.discoverAllFilesOnce()` (`premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/service/AdvancedScanningService.java:469`) calls it multiple times to collect `.java`, `.jsp`, config, build and Docker files.
- `DependencyTreeCommandExecutorImpl` (`premium-core-engine/src/main/java/adrianmikula/jakartamigration/advancedscanning/service/impl/DependencyTreeCommandExecutorImpl.java:28`) spawns `mvn dependency:tree` and uses a `GradleToolingApiExecutor` to resolve transitive dependencies.

These ignore project-excluded folders and build-tool indices that IntelliJ already maintains.

## Available IntelliJ SDK APIs

The following platform APIs are designed for exactly this:

- **`ProjectFileIndex`** (`ProjectRootManager.getInstance(project).getFileIndex()`)
  - Fast membership queries: `getContentRootForFile()`, `getSourceRootForFile()`, `getClassRootForFile()`, `isInProject()`, `isInLibraryClasses()`, `isInLibrarySource()`.
  - Is the source-of-truth for which files are part of the project, are excluded, or are library classes/sources.
  - Requires read lock (`@RequiresReadLock`).

- **`ProjectRootManager` / `ModuleRootManager`**
  - `ProjectRootManager.getContentSourceRoots()` / `getContentRoots()` — all module source/content roots.
  - `ModuleRootManager.getInstance(module).orderEntries()` — enumerate module/order entries.

- **`OrderEnumerator`**
  - `ModuleRootManager.getInstance(module).orderEntries().librariesOnly().classes().getRoots()` — all library class roots (jars).
  - `.recursively()`, `.productionOnly()`, `.compileOnly()`, `.runtimeOnly()` to match the current `compileClasspath`/`runtimeClasspath` behaviour.

- **`LibraryTable` / `LibraryTablesRegistrar.getLibraryTable(project)`**
  - List all project libraries and their `getUrls(OrderRootType.CLASSES)` / `getUrls(OrderRootType.SOURCES)`.

- **External System / Gradle APIs**
  - `ExternalSystemApiUtil` and `GradleProjectResolverUtil` can read the already-resolved Gradle project model inside the IDE.
  - `ProjectDataManager` / `ExternalSystemApiUtil.find` give access to `LibraryData`/`ModuleData` nodes produced by the last Gradle import — this is the closest thing to "IntelliJ's Gradle tree" and avoids spawning Gradle.

## Proposed Change

1. Add an `IntelliJProjectSource` adapter in the `premium-intellij-plugin` module that wraps the platform APIs above and returns `List<Path>` / `List<DependencyInfo>` to feed the existing scanners.
2. Use `ProjectFileIndex` to enumerate source files instead of `ProjectFileSystemScanner` for the IntelliJ plugin path.
3. Use `OrderEnumerator`/`LibraryTable` to obtain resolved library jars and modules instead of `mvn dependency:tree` / Gradle Tooling API where the project is already imported into IntelliJ.
4. Keep the existing command/FS-based fallbacks for non-IntelliJ environments (headless / CLI / tests) so the core engine stays independent.

## Benefits

- Faster scans: no redundant file walking; uses already-cached indexes.
- Correct excluded/test/prod classification driven by the user's project model, not path heuristics.
- No external process spawning for dependency trees.
- Respects source-set/module scope automatically.

## Risks / Open Questions

- API calls require read actions (`ReadAction.compute(...)`) on the EDT or a background thread.
- `LibraryTable` and `OrderEnumerator` give jar-level class roots, not textual `group:artifact:version` coordinates; `LibraryData` from External System is needed for the GAV mapping.
- Works only when the project is successfully imported in IntelliJ; partially-loaded or non-Gradle/Maven projects still need the current fallback.
- Requires testing with multi-module Gradle source sets to ensure all `src/main/java` / `src/test` roots are covered.

## Recommended Next Steps

1. Spike: add a small `IntelliJProjectResolver` class that uses `ProjectFileIndex` and `OrderEnumerator` to collect the same file lists produced by `discoverAllFilesOnce()` and compare them on a few real projects.
2. If the spike matches, replace `discoverAllFilesOnce()` in the plugin with the new resolver and benchmark `AdvancedScanningService` scan time.
3. For transitive dependency GAV data, extend the spike to read `LibraryData` from `ExternalSystemApiUtil` for Gradle and Maven imported projects.

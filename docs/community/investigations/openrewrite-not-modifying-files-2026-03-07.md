# OpenRewrite Not Modifying Files — Root Cause Investigation
**Date:** 2026-03-07  
**Reporter:** Adrian Mikula  
**Status:** Root cause identified, fix pending

---

## Symptom

When running any OpenRewrite recipe (e.g. `MigrateBeanValidation`) through the IntelliJ plugin's Refactor tab, the result shows **0 files modified**, even against projects with known `javax.*` references.

---

## Investigation Summary

### Trace of Recipe Execution

```
UI (RefactorTabComponent)
  → MigrationAnalysisService.applyRecipe(recipeName, projectPath)
    → RecipeLibrary.getRecipe(recipeName)             ✓ recipe found
    → findSourceFiles(projectPath)                     ✓ files found
    → RefactoringEngine.refactorFile(filePath, ...)    ← BUG HERE
      → determineToolToUse()                           ← returns SIMPLE_STRING_REPLACEMENT
```

---

## Root Causes

### 1. `RefactoringEngine` Always Defaults to `SIMPLE_STRING_REPLACEMENT`

**File:** `MigrationAnalysisService.java` (line 62)

```java
// BUG: no-arg constructor defaults to SIMPLE_STRING_REPLACEMENT
this.refactoringEngine = new RefactoringEngine();
```

The `RefactoringEngine` no-arg constructor sets `preferredTool = MigrationTool.SIMPLE_STRING_REPLACEMENT`. This completely bypasses OpenRewrite — the `refactorWithOpenRewrite()` method is **never called**.

**Fix:** Initialize with `MigrationTool.OPENREWRITE`:
```java
this.refactoringEngine = new RefactoringEngine(MigrationTool.OPENREWRITE);
```

---

### 2. File Parsed as Raw String — No Path Context

**File:** `RefactoringEngine.java` (line 201)

```java
// BUG: parses raw string content — no source path, no classpath
List<SourceFile> sourceFiles = javaParser.parse(originalContent).collect(Collectors.toList());
```

OpenRewrite's `JavaParser.parse(String)` does not know the real file path. This means:
- OpenRewrite cannot perform type resolution (it doesn't know which class is which).
- The recipe may find no matching AST nodes, returning 0 changes.

**Fix:** Use `javaParser.parse(List<Path>)` or pass the file via `Input` with a real path:
```java
List<SourceFile> sourceFiles = javaParser.parse(List.of(filePath), ...).collect(...);
```

---

### 3. Files Processed One-by-One Instead of as a Full Project

**File:** `MigrationAnalysisService.java` (lines 234–251)

```java
// BUG: each file is parsed and refactored independently
for (Path filePath : sourceFiles) {
    RefactoringChanges changes = refactoringEngine.refactorFile(filePath, recipes);
    ...
}
```

OpenRewrite is designed to run on a **full source set** at once. Processing each file individually:
- Breaks cross-file type resolution (the parser can't resolve types used in other files).
- Means some recipes (especially those that trace imports across files) simply don't find any changes.

**Fix:** Collect all source file `Path` objects, parse them all at once with OpenRewrite's `JavaParser`, run the recipe on the full `SourceSet`, then write each modified file back.

---

## Recommended Fix Strategy

Rewrite `MigrationAnalysisService.applyRecipe()` to use a direct OpenRewrite project-level execution:

1. Collect all `.java` source file paths as a `List<Path>`.
2. Use `JavaParser.fromJavaVersion().build()` to parse **all files at once** using `javaParser.parse(paths, baseDir, ctx)`.
3. Run the `org.openrewrite.Recipe` on the resulting `SourceSet`.
4. Iterate the `Result` list — for each result where `getAfter() != null`, write the modified content back to the original file.
5. Remove the `RefactoringEngine.refactorFile()` delegation for OpenRewrite recipes — the engine file-by-file approach is fundamentally incompatible with how OpenRewrite works.

This keeps the `SIMPLE_STRING_REPLACEMENT` path unchanged for XML files and other non-Java files.

---

## Why Previous Fixes Didn't Work

The previous session corrected the **recipe name strings** in `RecipeMapper` (e.g. `JavaxValidationMigrationToJakartaValidation` → correct). These were genuine typos that would have prevented recipes from activating. However the recipe names were never even reached in practice because the `RefactoringEngine` was dispatching to `SIMPLE_STRING_REPLACEMENT`, not `OPENREWRITE`.

---

## Related Files

| File | Issue |
|---|---|
| `MigrationAnalysisService.java` | Initializes `RefactoringEngine` with wrong `MigrationTool` |
| `RefactoringEngine.java` | `refactorWithOpenRewrite()` parses content as string, not path |
| `RecipeMapper.java` | Recipe names corrected (previous session) — this part is correct |

# Code Quality Fixes Applied

## High-Priority PMD Issues Fixed

### 1. ConstructorCallsOverridableMethod (RecipeLibrary.java)
**Issue**: Calling `registerRecipe()` (a public method) in constructor
**Fix**: 
- Made `registerRecipe()` final
- Created private `registerRecipeInternal()` method
- Constructor now calls the private method

**File**: `src/main/java/adrianmikula/jakartamigration/coderefactoring/service/RecipeLibrary.java`

### 2. AvoidFileStream (ToolDownloader.java)
**Issue**: Using `FileOutputStream` instead of NIO API
**Fix**: 
- Replaced `new FileOutputStream(destination.toFile())` with `Files.newOutputStream(destination)`
- Removed unused `FileOutputStream` import

**File**: `src/main/java/adrianmikula/jakartamigration/coderefactoring/service/ToolDownloader.java`

### 3. AvoidThrowingRawExceptionTypes (McpStreamableHttpController.java, McpSseController.java)
**Issue**: Methods throwing raw `Exception` instead of specific exceptions
**Fix**:
- Changed `executeTool()` to throw `IllegalArgumentException, ReflectiveOperationException`
- Changed `invokeTool()` to throw `ReflectiveOperationException, IllegalArgumentException`
- Updated catch blocks to catch `ReflectiveOperationException` instead of generic `Exception`

**Files**:
- `src/main/java/adrianmikula/jakartamigration/mcp/McpStreamableHttpController.java`
- `src/main/java/adrianmikula/jakartamigration/mcp/McpSseController.java`

## Remaining Issues

If PMD still reports issues, check the HTML report at `build/reports/pmd/main.html` for:
- Specific file locations
- Line numbers
- Detailed error messages

## SpotBugs Issues

SpotBugs found issues that need to be reviewed. Check the report at:
- `build/reports/spotbugs/main.html`

Common SpotBugs issues to look for:
- Null pointer dereferences
- Resource leaks
- Dead code
- Incorrect equals()/hashCode() implementations

## Next Steps

1. Run the build again: `./gradlew codeQualityVerify`
2. Review any remaining high-priority issues in the reports
3. Fix issues or add suppressions for false positives
4. For SpotBugs, review the HTML report and fix critical bugs


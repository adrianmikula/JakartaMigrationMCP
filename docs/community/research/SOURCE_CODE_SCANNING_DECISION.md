# Source Code Scanning - Decision Summary

## Research Findings

After researching existing solutions, here are the key findings:

### ✅ Best Option: OpenRewrite JavaParser

**Why This is the Best Choice:**

1. **Already Available**: OpenRewrite is already in our dependencies (`build.gradle.kts` line 108)
2. **AST-Based**: Provides accurate parsing, handles edge cases (comments, string literals, etc.)
3. **Fast**: Parses ~1000 files/second, can be parallelized
4. **Consistent**: We already use OpenRewrite for refactoring, so this keeps the codebase consistent
5. **Well-Maintained**: Active project, widely used in industry

### Performance Characteristics

- **Speed**: Fast (can parse large codebases in seconds)
- **Memory**: Moderate (~50-100MB for large projects) - acceptable for our use case
- **I/O**: Can be made non-blocking with parallel streams
- **Scalability**: Handles large codebases well

### Implementation Strategy

**Phase 1: Basic Implementation** (Recommended Start)
- Use OpenRewrite JavaParser directly
- Parallel file discovery and parsing
- Filter build directories (`target/`, `build/`, `.git/`)

**Phase 2: Optimization** (If Needed)
- Add regex pre-filter for very large codebases
- Only parse files that contain `javax.*` imports
- Implement incremental scanning

---

## Alternative Options Considered

### Option 2: Code-Index-MCP Integration
- **Pros**: Specialized for indexing, multi-language support
- **Cons**: Requires separate MCP server, adds complexity, network overhead
- **Verdict**: Not recommended - adds complexity without clear benefit for Java-only scanning

### Option 3: Hybrid Approach (Regex + OpenRewrite)
- **Pros**: Fastest overall, lower memory
- **Cons**: More complex, may be overkill initially
- **Verdict**: Good optimization for later, but not needed initially

### Option 4: Simple Regex
- **Pros**: Simplest, very fast
- **Cons**: Less accurate, not consistent with OpenRewrite
- **Verdict**: Not recommended - we have OpenRewrite, should use it

---

## Recommended Implementation Plan

### Step 1: Create Domain Classes
- `SourceCodeAnalysisResult`
- `FileUsage`
- `ImportStatement`

### Step 2: Implement SourceCodeScanner
- Use OpenRewrite `JavaParser`
- Parallel file discovery with `Files.walk().parallel()`
- Parallel parsing with `parallelStream()`
- Filter out build directories

### Step 3: Add MCP Tool
- `scanSourceCode` tool in `JakartaMigrationTools`
- Returns JSON with file-by-file javax usage

### Step 4: Add Tests
- Unit tests for scanner
- Integration tests with sample projects

### Estimated Time: 1.5-2 days

---

## Performance Optimization Tips

1. **Parallel Processing**: Use `parallelStream()` for both file discovery and parsing
2. **Early Filtering**: Skip `target/`, `build/`, `.git/`, `node_modules/` directories
3. **Caching**: Cache parsed ASTs if scanning the same project multiple times
4. **Batch Processing**: Process files in batches to control memory (if needed)

---

## Code Example

```java
public class SourceCodeScanner {
    private final JavaParser javaParser;
    
    public SourceCodeScanner() {
        this.javaParser = JavaParser.fromJavaVersion()
            .build();
    }
    
    public SourceCodeAnalysisResult scanProject(Path projectPath) {
        List<FileUsage> usages = discoverJavaFiles(projectPath)
            .parallelStream()
            .filter(this::shouldScanFile)
            .map(this::scanFile)
            .filter(FileUsage::hasJavaxUsage)
            .collect(Collectors.toList());
        
        return new SourceCodeAnalysisResult(usages);
    }
    
    private boolean shouldScanFile(Path file) {
        String path = file.toString();
        return !path.contains("/target/") &&
               !path.contains("/build/") &&
               !path.contains("/.git/") &&
               !path.contains("/node_modules/");
    }
}
```

---

## Next Steps

1. ✅ Research complete - OpenRewrite is the best choice
2. ⏭️ Ready to implement SourceCodeScanner using OpenRewrite
3. ⏭️ Add MCP tool for source code scanning
4. ⏭️ Add tests and verify performance

**Ready to proceed with implementation?**


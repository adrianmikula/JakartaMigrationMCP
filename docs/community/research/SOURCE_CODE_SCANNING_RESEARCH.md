# Source Code Scanning Research - Implementation Options

## Executive Summary

After researching existing solutions, we have **three viable approaches** for implementing source code scanning:

1. **Use OpenRewrite JavaParser** (Recommended) - Already in dependencies, fast, AST-based
2. **Integrate Code-Index-MCP** - External MCP server for code indexing
3. **Hybrid Approach** - Fast regex pre-filter + OpenRewrite for detailed analysis

**Recommendation**: Use **OpenRewrite JavaParser** - it's already in our dependencies, provides AST-based parsing, and is designed for code analysis.

---

## Option 1: OpenRewrite JavaParser ⭐ RECOMMENDED

### Current Status
- ✅ **Already in dependencies**: `org.openrewrite:rewrite-java`
- ✅ **Already used for refactoring**: Project uses OpenRewrite for Jakarta migration
- ✅ **AST-based parsing**: Accurate, handles edge cases
- ✅ **Well-maintained**: Active project, widely used

### Performance Characteristics
- **Speed**: Fast for typical codebases (parses ~1000 files/second)
- **Memory**: Moderate (~50-100MB for large projects)
- **I/O**: Can be made non-blocking with parallel streams
- **Scalability**: Handles large codebases well

### Implementation Approach

```java
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.CompilationUnit;

public class SourceCodeScanner {
    private final JavaParser javaParser;
    
    public SourceCodeScanner() {
        this.javaParser = JavaParser.fromJavaVersion()
            .build();
    }
    
    public SourceCodeAnalysisResult scanProject(Path projectPath) {
        List<FileUsage> usages = new ArrayList<>();
        
        // Parallel file discovery
        List<Path> javaFiles = discoverJavaFiles(projectPath)
            .parallelStream()
            .collect(Collectors.toList());
        
        // Parallel parsing (non-blocking)
        javaFiles.parallelStream()
            .forEach(file -> {
                try {
                    FileUsage usage = scanFile(file);
                    if (usage.hasJavaxUsage()) {
                        synchronized(usages) {
                            usages.add(usage);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to scan file: {}", file, e);
                }
            });
        
        return new SourceCodeAnalysisResult(usages);
    }
    
    private FileUsage scanFile(Path file) {
        try {
            String content = Files.readString(file);
            CompilationUnit cu = javaParser.parse(content).get(0);
            
            List<ImportStatement> imports = extractJavaxImports(cu);
            
            return new FileUsage(file, imports, countLines(content));
        } catch (Exception e) {
            log.error("Error scanning file: {}", file, e);
            return new FileUsage(file, List.of(), 0);
        }
    }
    
    private List<ImportStatement> extractJavaxImports(CompilationUnit cu) {
        List<ImportStatement> imports = new ArrayList<>();
        
        cu.getImports().forEach(imp -> {
            String importName = imp.getQualid().toString();
            if (importName.startsWith("javax.")) {
                String jakartaEquivalent = importName.replace("javax.", "jakarta.");
                imports.add(new ImportStatement(
                    importName,
                    jakartaEquivalent,
                    imp.getCoordinates().getLineNumber()
                ));
            }
        });
        
        return imports;
    }
}
```

### Pros
- ✅ Already in dependencies (no new dependencies)
- ✅ AST-based (accurate, handles edge cases)
- ✅ Fast parsing
- ✅ Can be parallelized
- ✅ Consistent with existing refactoring code

### Cons
- ⚠️ Moderate memory usage (acceptable for our use case)
- ⚠️ Requires parsing entire file (but fast)

### Performance Optimization Tips
1. **Parallel Processing**: Use `parallelStream()` for file discovery and parsing
2. **Early Filtering**: Skip files in `target/`, `build/`, `.git/` directories
3. **Caching**: Cache parsed ASTs if scanning multiple times
4. **Batch Processing**: Process files in batches to control memory

---

## Option 2: Code-Index-MCP Integration

### Overview
- **External MCP Server**: Separate service for code indexing
- **SCIP-based**: Uses Source Code Indexing Protocol
- **Multi-language**: Supports Java, Python, JavaScript, etc.
- **Tree-Sitter**: Uses Tree-Sitter for parsing

### Integration Approach
1. Install Code-Index-MCP as separate MCP server
2. Configure in MCP client (Cursor)
3. Use MCP tools from Code-Index-MCP for indexing
4. Our Jakarta Migration MCP queries Code-Index-MCP for source info

### Pros
- ✅ Specialized for code indexing
- ✅ Multi-language support
- ✅ Can be shared across multiple MCP servers
- ✅ Optimized for large codebases

### Cons
- ❌ Requires separate MCP server installation
- ❌ Additional dependency/configuration
- ❌ Network overhead (MCP-to-MCP communication)
- ❌ Less control over scanning logic
- ❌ May not be necessary for our use case

### When to Use
- If we need multi-language support
- If we want to share indexing across multiple tools
- If codebase is extremely large (>100k files)

### Recommendation
**Not recommended** for initial implementation - adds complexity without clear benefit for Java-only scanning.

---

## Option 3: Hybrid Approach (Fast Pre-filter + OpenRewrite)

### Strategy
1. **Fast Regex Pre-filter**: Quickly identify files with `javax.*` imports
2. **OpenRewrite Deep Analysis**: Only parse files that have javax usage

### Implementation

```java
public class SourceCodeScanner {
    private static final Pattern JAVAX_IMPORT_PATTERN = 
        Pattern.compile("^import\\s+(javax\\.\\w+.*);", Pattern.MULTILINE);
    
    public SourceCodeAnalysisResult scanProject(Path projectPath) {
        // Step 1: Fast regex pre-filter
        List<Path> candidateFiles = discoverJavaFiles(projectPath)
            .parallelStream()
            .filter(file -> hasJavaxImports(file))  // Quick regex check
            .collect(Collectors.toList());
        
        // Step 2: Deep AST analysis only on candidates
        List<FileUsage> usages = candidateFiles.parallelStream()
            .map(this::scanFileWithAST)
            .filter(FileUsage::hasJavaxUsage)
            .collect(Collectors.toList());
        
        return new SourceCodeAnalysisResult(usages);
    }
    
    private boolean hasJavaxImports(Path file) {
        try {
            String content = Files.readString(file);
            return JAVAX_IMPORT_PATTERN.matcher(content).find();
        } catch (IOException e) {
            return false;
        }
    }
    
    private FileUsage scanFileWithAST(Path file) {
        // Use OpenRewrite for detailed analysis
        // ...
    }
}
```

### Pros
- ✅ Fastest overall (skips parsing files without javax)
- ✅ Lower memory usage (only parses relevant files)
- ✅ Best of both worlds

### Cons
- ⚠️ More complex implementation
- ⚠️ Regex pre-filter may miss some cases (but OpenRewrite catches them)

### Recommendation
**Good optimization** for very large codebases, but may be overkill initially.

---

## Option 4: Simple Regex (Quick Start)

### Overview
- Simple regex-based scanning
- No AST parsing
- Fastest to implement

### Implementation

```java
public class SourceCodeScanner {
    private static final Pattern JAVAX_IMPORT_PATTERN = 
        Pattern.compile("^import\\s+(javax\\.\\w+.*);", Pattern.MULTILINE);
    
    public SourceCodeAnalysisResult scanProject(Path projectPath) {
        List<FileUsage> usages = discoverJavaFiles(projectPath)
            .parallelStream()
            .map(this::scanFile)
            .filter(FileUsage::hasJavaxUsage)
            .collect(Collectors.toList());
        
        return new SourceCodeAnalysisResult(usages);
    }
    
    private FileUsage scanFile(Path file) {
        try {
            String content = Files.readString(file);
            List<ImportStatement> imports = extractJavaxImports(content);
            return new FileUsage(file, imports, countLines(content));
        } catch (IOException e) {
            return new FileUsage(file, List.of(), 0);
        }
    }
}
```

### Pros
- ✅ Simplest implementation
- ✅ Very fast
- ✅ Low memory

### Cons
- ❌ Less accurate (may miss edge cases)
- ❌ Doesn't handle comments, string literals well
- ❌ Not consistent with OpenRewrite approach

### Recommendation
**Not recommended** - we already have OpenRewrite, should use it for consistency.

---

## Performance Comparison

| Approach | Speed | Memory | Accuracy | Complexity |
|----------|-------|--------|----------|------------|
| OpenRewrite | Fast | Moderate | High | Low |
| Code-Index-MCP | Fast | Low | High | High |
| Hybrid | Very Fast | Low | High | Medium |
| Simple Regex | Very Fast | Low | Low | Low |

---

## Final Recommendation

### Primary Approach: OpenRewrite JavaParser

**Why**:
1. ✅ Already in dependencies
2. ✅ AST-based (accurate)
3. ✅ Consistent with existing code
4. ✅ Fast enough for our needs
5. ✅ Can be parallelized

**Implementation Plan**:
1. Create `SourceCodeScanner` using OpenRewrite JavaParser
2. Use parallel streams for file discovery and parsing
3. Filter out build directories (`target/`, `build/`, `.git/`)
4. Add caching if needed for repeated scans

**Optimization (if needed)**:
- Add regex pre-filter for very large codebases
- Implement incremental scanning (only scan changed files)

### Future Enhancement: Hybrid Approach

If performance becomes an issue with very large codebases:
- Add regex pre-filter
- Only parse files with javax imports using OpenRewrite

---

## Implementation Steps

1. **Create Domain Classes** (1 hour)
   - `SourceCodeAnalysisResult`
   - `FileUsage`
   - `ImportStatement`

2. **Implement SourceCodeScanner** (4-6 hours)
   - Use OpenRewrite JavaParser
   - Parallel file discovery
   - Parallel parsing
   - Filter build directories

3. **Add MCP Tool** (1 hour)
   - `scanSourceCode` tool
   - JSON response formatting

4. **Add Tests** (2-3 hours)
   - Unit tests for scanner
   - Integration tests with sample projects

5. **Performance Testing** (2 hours)
   - Test with large codebase
   - Optimize if needed

**Total Estimated Time**: 10-14 hours (1.5-2 days)

---

## References

- [OpenRewrite Documentation](https://docs.openrewrite.org/)
- [Code-Index-MCP GitHub](https://github.com/ViperJuice/Code-Index-MCP)
- [SCIP Protocol](https://github.com/sourcegraph/scip)
- [Java NIO Performance](https://docs.oracle.com/javase/tutorial/essential/io/fileio.html)


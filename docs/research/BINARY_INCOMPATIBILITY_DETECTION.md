# Binary Incompatibility Detection - Research & Best Practices

## Problem Statement

The "Ghost in the Machine" problem: While the MCP can find and replace `javax` with `jakarta` in source code, it cannot easily detect **binary incompatibility** in closed-source third-party JARs. If your project relies on a legacy library that hasn't migrated, the MCP might "fix" your code, but the application will still crash at runtime with a `ClassNotFoundException`.

## Gemini AI Recommendations

This research incorporates recommendations from Gemini AI evaluation (see `docs/improvements/LIMITATIONS.md`), which specifically recommends:

1. **japicmp Tool** - Highly recommended as the fastest and most modern tool for binary compatibility checking
   - Can be used programmatically as a Java library
   - Supports CLI, Maven, Gradle, Ant integrations
   - Ideal for comparing `javax` version vs `jakarta` version of dependencies

2. **Architecture Best Practices**:
   - **Version Pinning & Hashing**: Never use `LATEST` or ranges, hard-code versions with SHA-256 hashes
   - **Isolated Classloading**: Load third-party JARs in custom `URLClassLoader` per plugin/server
   - **Pre-Flight Validation**: Use japicmp to validate JARs before adding to classpath

3. **Build-Time Integration**: Use `japicmp-maven-plugin` or Gradle equivalent to fail builds if dependency updates break compatibility

These recommendations are integrated throughout this document, with japicmp highlighted as a key complementary tool alongside our existing ASM-based bytecode analyzer.

## Current State

### Existing Implementation

We already have a solid foundation:

1. **ASM-based Bytecode Analyzer** (`AsmBytecodeAnalyzer`)
   - ✅ Can analyze JAR files for `javax.*` and `jakarta.*` references
   - ✅ Detects namespace usage in bytecode (classes, methods, fields, annotations)
   - ✅ Identifies mixed namespace issues
   - ✅ Fast and lightweight (ASM library)

2. **Blocker Detection** (`BlockerType.BINARY_INCOMPATIBLE`)
   - ✅ Already has enum value for binary incompatibility
   - ⚠️ Not yet fully implemented for third-party JAR analysis

3. **Dependency Analysis Module**
   - ✅ Can analyze dependency trees
   - ⚠️ Currently focuses on metadata, not bytecode analysis

### Gap Analysis

**What we have:**
- ✅ Bytecode analysis capability (ASM)
- ✅ JAR file reading capability
- ✅ Dependency tree resolution

**What we're missing:**
- ❌ Automatic analysis of third-party JARs from dependency tree
- ❌ Integration of bytecode analysis into blocker detection
- ❌ Detection of transitive dependency binary incompatibilities
- ❌ Reporting of binary incompatibility issues in a user-friendly way

## Best Practices & Industry Standards

### 1. Static Bytecode Analysis (Primary Approach) ⭐ **RECOMMENDED**

**Technology**: ASM (already in use), Javassist, or ByteBuddy

**How It Works**:
1. Resolve dependency tree (Maven/Gradle)
2. Download or locate JAR files for each dependency
3. Analyze each JAR's bytecode for `javax.*` references
4. Report dependencies that contain `javax.*` bytecode references
5. Flag as binary incompatible if found

**Pros**:
- ✅ **Fast**: < 1 second per JAR for analysis
- ✅ **Accurate**: Direct bytecode inspection, no guessing
- ✅ **No Execution Required**: Pure static analysis
- ✅ **Comprehensive**: Can analyze all transitive dependencies
- ✅ **Already Implemented**: We have ASM analyzer ready

**Cons**:
- ⚠️ Requires JAR files to be available (may need to download from Maven Central)
- ⚠️ Some JARs may be signed (signatures may be invalidated, but that's expected)

**Implementation Strategy**:
```java
// Extend existing AsmBytecodeAnalyzer
public class ThirdPartyJarAnalyzer {
    private final BytecodeAnalyzer bytecodeAnalyzer;
    private final DependencyResolver dependencyResolver;
    
    public BinaryIncompatibilityReport analyzeDependencies(Path projectPath) {
        // 1. Resolve dependency tree
        DependencyTree tree = dependencyResolver.resolve(projectPath);
        
        // 2. For each dependency, analyze its JAR
        List<BinaryIncompatibility> issues = new ArrayList<>();
        for (Dependency dep : tree.getAllDependencies()) {
            Path jarPath = downloadOrLocateJar(dep);
            BytecodeAnalysisResult result = bytecodeAnalyzer.analyzeJar(jarPath);
            
            if (result.hasJavaxClasses()) {
                issues.add(new BinaryIncompatibility(
                    dep,
                    result.getJavaxClasses(),
                    "Dependency contains javax.* bytecode references"
                ));
            }
        }
        
        return new BinaryIncompatibilityReport(issues);
    }
}
```

### 2. JDK `jdeps` Tool (Complementary Approach)

**Technology**: `jdeps` command-line tool (included with JDK)

**How It Works**:
- `jdeps` analyzes class files and JARs to identify dependencies
- Can show package-level dependencies
- Can identify internal API usage

**Pros**:
- ✅ **Standard Tool**: Included with JDK, no additional dependencies
- ✅ **Fast**: Optimized C++ implementation
- ✅ **Comprehensive**: Shows all package dependencies

**Cons**:
- ❌ **External Process**: Requires spawning `jdeps` process
- ❌ **Output Parsing**: Need to parse text output
- ❌ **Less Control**: Can't customize analysis as easily as ASM

**Usage Example**:
```bash
jdeps -summary -cp dependency.jar
# Output shows package dependencies, can grep for javax.*
```

**Integration Strategy**:
```java
public class JdepsAnalyzer {
    public BinaryIncompatibilityReport analyzeWithJdeps(Path jarPath) {
        ProcessBuilder pb = new ProcessBuilder(
            "jdeps", "-summary", "-cp", jarPath.toString()
        );
        // Parse output for javax.* package references
    }
}
```

**Recommendation**: Use as **fallback** or **validation** for ASM analysis, not primary method.

### 3. Eclipse Transformer (Transformation Tool)

**Technology**: Eclipse Transformer - bytecode transformation tool

**How It Works**:
- Transforms JAR files from `javax.*` to `jakarta.*` at bytecode level
- Can be used to test if transformation is possible
- If transformation succeeds, dependency is transformable (but may still have issues)

**Pros**:
- ✅ **Industry Standard**: Official Eclipse tool
- ✅ **Proven**: Used by many Jakarta EE migrations
- ✅ **Can Transform**: Not just detect, but also fix

**Cons**:
- ❌ **Transformation Only**: Doesn't detect issues, transforms them
- ❌ **May Miss Edge Cases**: Some bytecode patterns may not transform correctly
- ❌ **Signatures**: Removes cryptographic signatures from JARs

**Use Case**: 
- **Not for detection**, but for **mitigation**
- Can be used to transform incompatible JARs if no Jakarta version exists
- Should be used with caution and testing

**Recommendation**: Use for **transformation** when needed, not for detection.

### 4. API Compatibility Checkers (Advanced) ⭐ **HIGHLY RECOMMENDED BY GEMINI**

**Tools**: japicmp (⭐ **Highly Recommended**), Revapi, JAPICC, Clirr

#### japicmp - Primary Recommendation

**Technology**: japicmp - Fastest and most modern binary compatibility checker

**How It Works**:
- Compares two JAR files and generates detailed reports of binary-breaking changes
- Detects removed classes, methods, fields
- Identifies signature changes (method parameters, return types)
- Can be used programmatically as a Java library
- Supports CLI, Maven, Gradle, Ant integrations

**Pros**:
- ✅ **Fast**: Optimized for performance
- ✅ **Modern**: Actively maintained, Java 8+ support
- ✅ **Programmatic API**: Can be integrated directly into Java code
- ✅ **Comprehensive**: Detects all types of binary incompatibilities
- ✅ **Flexible**: Multiple integration options (CLI, Maven, Gradle, Java API)

**Cons**:
- ⚠️ Requires two JAR files to compare (old vs new version)
- ⚠️ Less useful for initial `javax` detection (needs comparison baseline)

**Use Cases**:
1. **Version Comparison**: Compare `javax` version vs `jakarta` version of same library
2. **Breaking Change Detection**: Detect if upgrading a dependency will break binary compatibility
3. **Pre-Flight Validation**: Validate JARs before adding to classpath
4. **Build-Time Checks**: Fail builds if dependency updates break compatibility

**Programmatic Usage Example** (from Gemini recommendations):
```java
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.model.JApiClass;

public class CompatibilityGuard {
    public void verify(File oldJar, File newJar) {
        JarArchiveComparatorOptions options = new JarArchiveComparatorOptions();
        JarArchiveComparator comparator = new JarArchiveComparator(options);
        
        // Compare two binaries directly
        List<JApiClass> changes = comparator.compare(oldJar, newJar);
        
        for (JApiClass clazz : changes) {
            if (clazz.getChangeStatus().name().equals("REMOVED")) {
                System.err.println("CRITICAL: Class " + 
                    clazz.getFullyQualifiedName() + " was removed!");
            }
            // Check for method removals, signature changes, etc.
        }
    }
}
```

**Integration Strategy**:
- Use japicmp to compare `javax` version vs `jakarta` version of dependencies
- Detect if Jakarta version has breaking changes
- Validate that all required classes/methods exist in Jakarta version

**Recommendation**: **Highly Recommended** - Use japicmp for version comparison and breaking change detection. Can complement ASM-based analysis.

#### Other Tools

**Revapi**:
- Most comprehensive (checks classes, config files, SPIs)
- Maven, Gradle, CLI support
- More complex setup than japicmp

**JAPI Compliance Checker (JAPICC)**:
- Great for legacy systems
- Requires Perl on host machine
- CLI-only

**Animal Sniffer**:
- Checks if JAR uses APIs not present in specific signature
- Useful for JDK version compatibility
- Maven, Ant support

**Recommendation**: Use **japicmp** as primary tool for version comparison. Use ASM for initial `javax` detection, then japicmp to verify Jakarta version compatibility.

## Recommended Implementation Strategy

### Phase 0: Hybrid Detection Approach (Recommended by Gemini)

**Strategy**: Combine ASM-based detection with japicmp for comprehensive analysis

1. **ASM Analysis** (Primary): Detect `javax.*` references in JAR bytecode
2. **japicmp Comparison** (Secondary): Compare `javax` version vs `jakarta` version when available
3. **Combined Reporting**: Merge results from both analyses

**Benefits**:
- ASM finds the problem (javax references)
- japicmp validates the solution (Jakarta version compatibility)
- Comprehensive coverage of both detection and validation

### Phase 1: Extend Existing Bytecode Analyzer (Immediate)

**Goal**: Integrate bytecode analysis into dependency analysis workflow

**Steps**:

1. **Create `ThirdPartyJarAnalyzer` Service**
   ```java
   @Service
   public class ThirdPartyJarAnalyzer {
       private final BytecodeAnalyzer bytecodeAnalyzer;
       private final MavenResolver mavenResolver; // or GradleResolver
       
       public List<BinaryIncompatibility> analyzeDependencyTree(Path projectPath) {
           // Resolve dependencies
           // For each dependency, analyze JAR
           // Return list of binary incompatibilities
       }
   }
   ```

2. **Integrate into `detectBlockers` Tool**
   - Add bytecode analysis step
   - Check each dependency's JAR for `javax.*` references
   - Report as `BlockerType.BINARY_INCOMPATIBLE`

3. **Enhance `Blocker` Domain Model**
   ```java
   public class Blocker {
       private BlockerType type;
       private String artifactId;
       private String groupId;
       private String version;
       private List<String> javaxClasses; // NEW: classes found in bytecode
       private String recommendation; // NEW: suggested action
   }
   ```

### Phase 2: JAR Resolution & Caching (Short-term)

**Goal**: Efficiently locate and cache JAR files for analysis

**Steps**:

1. **JAR Location Strategy**
   - Check local Maven repository (`~/.m2/repository`)
   - Check Gradle cache (`~/.gradle/caches`)
   - Download from Maven Central if not found (optional, may be slow)

2. **Caching Strategy**
   - Cache analysis results per JAR (hash-based)
   - Avoid re-analyzing same JAR version
   - Store results in temporary cache directory

3. **Parallel Analysis**
   - Analyze multiple JARs in parallel
   - Use thread pool for concurrent analysis
   - Limit concurrent downloads to avoid rate limiting

### Phase 3: Enhanced Reporting (Medium-term)

**Goal**: User-friendly reporting of binary incompatibility issues

**Steps**:

1. **Detailed Reports**
   - List all `javax.*` classes found in each dependency
   - Show which dependencies are affected
   - Provide recommendations (upgrade, replace, transform)

2. **Integration with Existing Tools**
   - Show binary incompatibilities in `analyzeJakartaReadiness`
   - Include in `detectBlockers` output
   - Add to migration plan as risks

3. **Mitigation Suggestions**
   - Check if Jakarta-compatible version exists
   - Suggest Eclipse Transformer if no alternative
   - Warn about transformation risks

## Implementation Details

### 1. Dependency JAR Resolution

**Maven**:
```java
public class MavenJarResolver {
    public Path resolveJar(Dependency dep) {
        // ~/.m2/repository/{groupId}/{artifactId}/{version}/{artifactId}-{version}.jar
        Path localRepo = Paths.get(System.getProperty("user.home"), 
            ".m2", "repository");
        Path jarPath = localRepo
            .resolve(dep.groupId().replace('.', '/'))
            .resolve(dep.artifactId())
            .resolve(dep.version())
            .resolve(dep.artifactId() + "-" + dep.version() + ".jar");
        
        if (Files.exists(jarPath)) {
            return jarPath;
        }
        
        // Optionally download from Maven Central
        return downloadFromMavenCentral(dep);
    }
}
```

**Gradle**:
```java
public class GradleJarResolver {
    public Path resolveJar(Dependency dep) {
        // ~/.gradle/caches/modules-2/files-2.1/{groupId}/{artifactId}/{version}/hash/{file}
        // More complex, may need to parse Gradle cache structure
        // Or use Gradle Tooling API
    }
}
```

### 2. japicmp Integration (Gemini Recommendation)

**Add japicmp Dependency**:
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.siom79.japicmp:japicmp:0.18.0")
}
```

**Create japicmp Service**:
```java
@Service
public class JapicmpCompatibilityChecker {
    
    /**
     * Compare javax version vs jakarta version of a dependency.
     */
    public CompatibilityReport compareVersions(
            Dependency javaxVersion, 
            Dependency jakartaVersion) {
        
        Path javaxJar = jarResolver.resolveJar(javaxVersion);
        Path jakartaJar = jarResolver.resolveJar(jakartaVersion);
        
        JarArchiveComparatorOptions options = new JarArchiveComparatorOptions();
        JarArchiveComparator comparator = new JarArchiveComparator(options);
        
        List<JApiClass> changes = comparator.compare(
            javaxJar.toFile(), 
            jakartaJar.toFile()
        );
        
        // Analyze changes for breaking compatibility
        List<BreakingChange> breakingChanges = analyzeChanges(changes);
        
        return new CompatibilityReport(
            javaxVersion,
            jakartaVersion,
            breakingChanges,
            isCompatible(breakingChanges)
        );
    }
    
    private List<BreakingChange> analyzeChanges(List<JApiClass> changes) {
        List<BreakingChange> breaking = new ArrayList<>();
        
        for (JApiClass clazz : changes) {
            if (clazz.getChangeStatus() == JApiChangeStatus.REMOVED) {
                breaking.add(new BreakingChange(
                    "Class removed: " + clazz.getFullyQualifiedName(),
                    BreakingChangeType.CLASS_REMOVED
                ));
            }
            
            // Check for method removals, signature changes, etc.
            for (JApiMethod method : clazz.getMethods()) {
                if (method.getChangeStatus() == JApiChangeStatus.REMOVED) {
                    breaking.add(new BreakingChange(
                        "Method removed: " + method.getName(),
                        BreakingChangeType.METHOD_REMOVED
                    ));
                }
            }
        }
        
        return breaking;
    }
}
```

### 3. Bytecode Analysis Integration

**Extend `AsmBytecodeAnalyzer`**:
```java
public class EnhancedBytecodeAnalyzer extends AsmBytecodeAnalyzer {
    
    /**
     * Analyzes a dependency JAR and returns binary incompatibility report.
     */
    public BinaryIncompatibilityReport analyzeDependency(Dependency dep, Path jarPath) {
        BytecodeAnalysisResult result = analyzeJar(jarPath);
        
        if (result.getJavaxClasses().isEmpty()) {
            return BinaryIncompatibilityReport.compatible(dep);
        }
        
        return BinaryIncompatibilityReport.incompatible(
            dep,
            result.getJavaxClasses(),
            generateRecommendation(dep, result)
        );
    }
    
    private String generateRecommendation(Dependency dep, BytecodeAnalysisResult result) {
        // Check if Jakarta version exists
        if (hasJakartaVersion(dep)) {
            return "Upgrade to Jakarta-compatible version: " + getJakartaVersion(dep);
        }
        
        // Check if Eclipse Transformer can be used
        if (isTransformable(result)) {
            return "Consider using Eclipse Transformer to transform this JAR";
        }
        
        return "No Jakarta-compatible version available. Consider replacing this dependency.";
    }
}
```

### 4. Integration with `detectBlockers`

**Modify `DependencyAnalysisModuleImpl`**:
```java
@Override
public BlockerReport detectBlockers(String projectPath) {
    // Existing logic...
    
    // NEW: Binary incompatibility detection
    List<Blocker> binaryBlockers = detectBinaryIncompatibilities(projectPath);
    blockers.addAll(binaryBlockers);
    
    return new BlockerReport(blockers);
}

private List<Blocker> detectBinaryIncompatibilities(String projectPath) {
    List<Blocker> blockers = new ArrayList<>();
    
    // Resolve dependency tree
    DependencyTree tree = resolveDependencyTree(projectPath);
    
    // Analyze each dependency's JAR
    for (Dependency dep : tree.getAllDependencies()) {
        try {
            Path jarPath = jarResolver.resolveJar(dep);
            BinaryIncompatibilityReport report = bytecodeAnalyzer.analyzeDependency(dep, jarPath);
            
            if (report.isIncompatible()) {
                blockers.add(Blocker.builder()
                    .type(BlockerType.BINARY_INCOMPATIBLE)
                    .artifactId(dep.artifactId())
                    .groupId(dep.groupId())
                    .version(dep.version())
                    .description("Binary incompatibility: JAR contains " + 
                        report.getJavaxClasses().size() + " javax.* class references")
                    .javaxClasses(report.getJavaxClasses()) // NEW field
                    .recommendation(report.getRecommendation())
                    .build());
            }
        } catch (Exception e) {
            // Log warning, continue with other dependencies
            log.warn("Failed to analyze JAR for {}: {}", dep, e.getMessage());
        }
    }
    
    return blockers;
}
```

## Performance Considerations

### Optimization Strategies

1. **Parallel Analysis**
   - Analyze multiple JARs concurrently
   - Use `ForkJoinPool` or `ExecutorService`
   - Limit concurrency to avoid resource exhaustion

2. **Caching**
   - Cache analysis results per JAR (hash-based key)
   - Store in temporary directory with TTL
   - Avoid re-analyzing same JAR version

3. **Lazy Loading**
   - Only analyze JARs when needed
   - Skip analysis if dependency is already known to be compatible
   - Use dependency metadata to short-circuit analysis

4. **Incremental Analysis**
   - Only analyze new/changed dependencies
   - Track analysis timestamps
   - Re-analyze only if JAR has changed

### Expected Performance

- **Single JAR Analysis**: 50-200ms (depending on JAR size)
- **Small Project** (10-20 dependencies): 1-2 seconds
- **Medium Project** (50-100 dependencies): 5-10 seconds
- **Large Project** (200+ dependencies): 20-40 seconds

**Optimization Target**: < 10 seconds for most projects

## Error Handling

### Common Issues

1. **JAR Not Found**
   - **Handling**: Skip dependency, log warning
   - **User Message**: "Could not locate JAR for {dependency}. Skipping binary analysis."

2. **Corrupted JAR**
   - **Handling**: Skip dependency, log warning
   - **User Message**: "JAR file for {dependency} appears corrupted. Skipping binary analysis."

3. **Signed JARs**
   - **Handling**: Analysis still works, signatures are not affected by reading
   - **Note**: Transformation would invalidate signatures, but we're only analyzing

4. **Nested JARs** (JARs within JARs)
   - **Handling**: Recursively analyze nested JARs
   - **Example**: Spring Boot fat JARs contain other JARs

## Testing Strategy

### Unit Tests

1. **Test JAR Analysis**
   - Create test JARs with `javax.*` references
   - Verify detection works correctly
   - Test edge cases (nested JARs, signed JARs)

2. **Test Dependency Resolution**
   - Mock Maven/Gradle resolvers
   - Test JAR location logic
   - Test caching behavior

3. **Test Integration**
   - Test `detectBlockers` with binary incompatibility detection
   - Verify blocker reports include binary issues

### Integration Tests

1. **Real Dependency Analysis**
   - Test with real Maven/Gradle projects
   - Verify JAR resolution from local repositories
   - Test with various dependency types

2. **Performance Tests**
   - Measure analysis time for projects of various sizes
   - Verify caching improves performance
   - Test parallel analysis

## User Experience

### Reporting Format

**In `detectBlockers` output**:
```json
{
  "blockers": [
    {
      "type": "BINARY_INCOMPATIBLE",
      "groupId": "com.example",
      "artifactId": "legacy-lib",
      "version": "1.2.3",
      "description": "Binary incompatibility detected: JAR contains 15 javax.* class references",
      "javaxClasses": [
        "javax.servlet.Servlet",
        "javax.servlet.http.HttpServlet",
        ...
      ],
      "recommendation": "Upgrade to jakarta-compatible version: com.example:legacy-lib:2.0.0",
      "severity": "HIGH"
    }
  ]
}
```

### User-Friendly Messages

**In migration plan**:
```
⚠️ Binary Incompatibility Detected

The following dependencies contain javax.* bytecode references and may cause 
runtime ClassNotFoundException errors:

- com.example:legacy-lib:1.2.3
  Found 15 javax.* class references in bytecode
  Recommendation: Upgrade to version 2.0.0 (Jakarta-compatible)
  
- org.old:old-framework:3.1.0
  Found 8 javax.* class references in bytecode
  Recommendation: No Jakarta version available. Consider replacing with 
  alternative library or using Eclipse Transformer.
```

## Future Enhancements

### Phase 4: Advanced Features

1. **japicmp Integration** (Recommended by Gemini)
   - Compare `javax` version vs `jakarta` version of dependencies
   - Detect breaking changes in Jakarta versions
   - Pre-flight validation before adding JARs to classpath
   - Build-time checks via Maven/Gradle plugins

2. **Eclipse Transformer Integration**
   - Automatically transform incompatible JARs
   - Test transformed JARs for compatibility
   - Provide transformed JARs as artifacts

3. **Classpath Analysis**
   - Analyze full classpath for conflicts
   - Detect mixed namespace issues at classpath level
   - Verify no `javax.*` classes in classpath when using Jakarta

4. **Isolated Classloading** (Gemini Recommendation)
   - Load third-party JARs in custom `URLClassLoader` per plugin/server
   - Prevent binary incompatibility in one tool from crashing entire MCP server
   - Version pinning & hashing for untrusted JARs

5. **Runtime Verification Integration**
   - Use bytecode analysis results to inform runtime verification
   - Skip runtime tests for known incompatible dependencies
   - Provide more accurate error predictions

6. **CI/CD Integration**
   - Fail builds if binary incompatibilities detected
   - Generate reports for dependency updates
   - Track binary compatibility over time
   - Use japicmp Maven/Gradle plugins for automated checks

## Conclusion

### Recommended Approach

**Primary Method**: Extend existing ASM-based bytecode analyzer
- ✅ Already implemented and tested
- ✅ Fast and accurate
- ✅ No external dependencies
- ✅ Full control over analysis

**Secondary Method**: japicmp for version comparison (Gemini Recommendation)
- ✅ Fastest and most modern tool
- ✅ Programmatic Java API
- ✅ Detects breaking changes between versions
- ✅ Can compare `javax` version vs `jakarta` version
- ✅ Validates Jakarta version compatibility

**Complementary Methods**:
- `jdeps`: Use as validation/fallback
- Eclipse Transformer: Use for transformation, not detection
- Revapi: Optional, for comprehensive API checking

### Implementation Priority

1. **High Priority**: Extend `AsmBytecodeAnalyzer` to analyze dependency JARs
2. **High Priority**: Integrate into `detectBlockers` workflow
3. **High Priority**: Add japicmp integration for version comparison (Gemini recommendation)
4. **Medium Priority**: Add JAR resolution and caching
5. **Medium Priority**: Enhanced reporting and recommendations
6. **Medium Priority**: japicmp Maven/Gradle plugin integration for build-time checks
7. **Low Priority**: Eclipse Transformer integration
8. **Low Priority**: Advanced features (classpath analysis, isolated classloading, CI/CD)

### Success Criteria

- ✅ Can detect `javax.*` references in third-party JAR bytecode (ASM)
- ✅ Can compare `javax` vs `jakarta` versions for compatibility (japicmp)
- ✅ Reports binary incompatibilities in `detectBlockers`
- ✅ Provides actionable recommendations
- ✅ Performance: < 10 seconds for typical projects
- ✅ Handles edge cases gracefully (missing JARs, corrupted files, etc.)
- ✅ Validates Jakarta version compatibility before recommending upgrades

---

**Next Steps**: 
1. Review and approve this approach
2. Create implementation plan with tasks
3. Start with Phase 1: Extend existing bytecode analyzer
4. Integrate into `detectBlockers` tool
5. Test with real projects
6. Iterate based on feedback


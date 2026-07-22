# Performance & Memory Profiling Skill

## Overview

This skill guides scanning the codebase for memory and performance code smells,
debugging performance problems, writing performance/memory tests that follow
established project conventions, and updating `docs/patterns/` when new categories
of issues are discovered and fixed.

## When to Use This Skill

- A user reports slow scans, high memory usage, or IDE freezes during analysis
- A code review raises concerns about allocation overhead in a hot path
- A new domain class with collection accessors is added to the codebase
- A pipeline or batch process needs memory budgeting before shipping
- After fixing a memory/performance bug, to write regression tests

## Code Smell Catalogue

Scan for these patterns in Java source files. Each has a documented fix in
`docs/patterns/memory_efficiency.md`.

### SMELL-1: Defensive Copy in Accessor

**Pattern:** A getter returns `new HashSet<>(field)`, `new ArrayList<>(field)`,
or `new HashMap<>(field)`.

**Detection:**
```
grep -rn "return new HashSet<>(" --include="*.java" src/main/java
grep -rn "return new ArrayList<>(" --include="*.java" src/main/java
grep -rn "return new HashMap<>(" --include="*.java" src/main/java
```

**Checklist:**
- Is the class effectively immutable (no public mutators that modify the
  returned collection)?
- Do callers mutate the returned set? (`grep` for `.add(`, `.remove(`, `.clear(`
  on the return value)
- If the class is immutable, remove the defensive copy and return the internal
  field directly.
- If callers need mutation, provide a separate copy method, not a defensive-copy
  accessor.

**Reference fix:** `DependencyGraph.java` — removed defensive copies from
`getNodes()`, `getEdges()`, and the constructor.

### SMELL-2: Repeated Collection Access in Pipeline

**Pattern:** A method calls `graph.getNodes()` or `graph.getEdges()` multiple
times within the same method, and each call may allocate a new collection.

**Detection:**
```
grep -rn "graph\.getNodes()\|graph\.getEdges()" --include="*.java" src/main/java
```
Count unique call sites within the same method. More than one is a smell.

**Fix:** Snapshot once at the pipeline entry point, pass the snapshot through.

```java
// Snapshot once
List<Artifact> artifacts = new ArrayList<>(graph.getNodes());
// Pass to sub-methods
identifyNamespaces(artifacts);
detectBlockers(artifacts);
recommendVersions(artifacts);
```

**Reference fix:** `DependencyAnalysisModuleImpl.analyzeProject()` — reduced from
3 `getNodes()` calls to 1 snapshot.

### SMELL-3: Duplicate File/Resource Discovery

**Pattern:** `scanner.findFiles(path, ...)` is called twice with the same
arguments within the same method.

**Detection:**
```
grep -rn "findFiles(" --include="*.java" src/main/java
```
Look for identical or overlapping arguments in the same method.

**Fix:** Call once, reuse the result for both purposes.

**Reference fix:** `AdvancedScanningService.discoverAllFilesOnce()` — reused a
single `scanner.findFiles(projectPath, List.of(".java"))` call for both JAVA and
TEST categories.

### SMELL-4: Fake Streaming

**Pattern:** `Files.lines().collect(Collectors.joining())` or
`stream.collect(Collectors.toList())` where the result is immediately used as a
whole (not iterated lazily).

**Detection:**
```
grep -rn "Files\.lines.*collect" --include="*.java" src/main/java
grep -rn "\.collect(Collectors\.joining" --include="*.java" src/main/java
```

**Fix:** Either use `Files.readString()` directly (simpler, same memory) or
process line-by-line without materialising. Don't claim streaming when the whole
content ends up in memory anyway.

**Reference fix:** `SourceCodeScannerImpl.scanFile()` — removed misleading
streaming path that loaded the entire file into a String via `Files.lines()`.

### SMELL-5: `String.split()` for Line Counting

**Pattern:** `content.split("\n").length` or `content.split("\\n").length`

**Detection:**
```
grep -rn "\.split.*\\\\n" --include="*.java" src/main/java
```

**Fix:** Count lines from the file via `Files.lines().count()` in a
try-with-resources, or track line numbers during processing.

**Reference fix:** `SourceCodeScannerImpl.scanFile()` — replaced
`countLines(content)` (which split the String) with
`Files.lines(filePath).count()`.

### SMELL-6: Unclosed File Streams

**Pattern:** `Files.lines()`, `Files.list()`, or `Files.walk()` used without
try-with-resources.

**Detection:**
```
grep -rn "Files\.\(lines\|list\|walk\)(" --include="*.java" src/main/java
```
Check each usage for a surrounding try-with-resources.

**Fix:** Wrap in `try (var stream = Files.lines(path)) { ... }`.

### SMELL-7: Unbounded Result Collection

**Pattern:** A `stream().collect(Collectors.toList())` that accumulates all
results before any processing occurs, especially in parallel streams.

**Detection:**
```
grep -rn "\.collect(Collectors\.toList())" --include="*.java" src/main/java
```
Look for cases where the collected list is then iterated — the intermediate list
is wasteful.

**Fix:** Process in place with `.forEach()`, or use bounded batch sizes.

### SMELL-8: Constructor Defensive Copy

**Pattern:** A constructor or record copies a collection parameter:
`this.items = new ArrayList<>(items)`.

**Detection:**
```
grep -rn "this\.\w* = new HashSet<>\|this\.\w* = new ArrayList<>\|this\.\w* = new HashMap<>" --include="*.java" src/main/java
```

**Fix:** If the constructor is only called with internally-built collections
(not from untrusted callers), assign directly. Verify no caller passes a set
they later mutate.

**Reference fix:** `DependencyGraph` constructor — changed from
`new HashSet<>(nodes)` to direct assignment.

## Debugging Performance/Memory Problems

Follow this workflow when a user reports a performance or memory issue.

### Step 1: Identify the hot path

Ask the user which operation is slow or uses too much memory:
- Quick scan (`handleQuickScan`)
- Deep scan (`handleDeepScan`)
- Dependency graph building (`buildFromProject`)
- Source code scanning (`scanFile`)
- Report generation (PDF, HTML)

### Step 2: Trace the code path

Use the codebase index and source files to trace the full call chain.
Key entry points:

| Operation | Entry point | Key files |
|-----------|-------------|-----------|
| Quick scan | `MigrationToolWindow.handleQuickScan()` | `AdvancedScanningService`, `DependencyAnalysisModuleImpl` |
| Deep scan | `MigrationToolWindow.performDeepScan()` | `TransitiveDependencyScannerImpl`, `DependencyTreeCommandExecutorImpl` |
| Dependency graph | `MavenDependencyGraphBuilder.buildFromProject()` | `GradleMultiModuleParser`, `DirectoryCrawlerDependencyGraphBuilder` |
| Source scan | `SourceCodeScannerImpl.scanFile()` | `ProjectFileSystemScanner` |

### Step 3: Apply the smell catalogue

Check each file in the call chain against SMELL-1 through SMELL-8 above.

### Step 4: Profile if needed

For complex issues, use `jcmd` or VisualVM:
```bash
# Attach to running process and dump heap
jcmd <pid> GC.heap_dump /tmp/heap.hprof

# Or run with memory tracking
java -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/tmp/gc.log ...
```

### Step 5: Fix and verify

1. Apply the fix following patterns in `docs/patterns/memory_efficiency.md`
2. Run `./gradlew :community-core-engine:compileJava` to verify compilation
3. Run `./gradlew :community-core-engine:fastTest` for quick signal
4. If you added `@Tag("slow")` tests, run
   `./gradlew :community-core-engine:slowTest` to verify them
5. Check if the fix introduces a new category of issue — if so, update
   `docs/patterns/memory_efficiency.md`

## Writing Performance Tests

### Conventions

All performance/memory tests in this project follow these conventions:

| Aspect | Convention |
|--------|-----------|
| **Tag** | `@Tag("slow")` — excluded from `fastTest`, run in CI via `slowTest` |
| **Display name** | `@DisplayName("...")` — human-readable description |
| **Assertions** | AssertJ (`assertThat`) over JUnit assertions |
| **Memory measurement** | `ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()` |
| **GC before measurement** | `System.gc()` + `Thread.sleep(50)` to stabilise |
| **Graph sizes** | 500-1000 nodes (realistic), NOT 2-5 nodes |
| **Mocking** | Mockito `lenient()` for stubs that may not be hit in every test |
| **Test location** | Same package as the code under test, under `src/test/java/.../unit/...` |

### Test Type 1: Accessor Identity Tests

Verify that collection accessors return the same instance, not a copy.

```java
@Tag("slow")
@DisplayName("DependencyGraph allocation tests")
class DependencyGraphMemoryTest {

    @Test
    @DisplayName("getNodes() should return the same Set instance, not a copy")
    void getNodesShouldReturnSameInstance() {
        DependencyGraph graph = new DependencyGraph();
        graph.addNode(new Artifact("g", "a", "1.0", "compile", false));

        Set<Artifact> first = graph.getNodes();
        Set<Artifact> second = graph.getNodes();

        assertThat(first).isSameAs(second);
    }
}
```

### Test Type 2: Memory Budget Tests

Build a realistic-sized structure, process it, assert memory stays within budget.

```java
@Tag("slow")
@ExtendWith(MockitoExtension.class)
@DisplayName("Analysis pipeline memory budget tests")
class AnalysisPipelineMemoryBudgetTest {

    private MemoryMXBean memoryBean;

    @BeforeEach
    void setUp() {
        memoryBean = ManagementFactory.getMemoryMXBean();
    }

    @Test
    @DisplayName("Analyzing a 500-node graph should use less than 50MB of heap")
    void analyze500NodeGraphShouldStayWithinBudget() {
        DependencyGraph graph = buildLargeGraph(500, 800);
        // ... set up module with StubGraphBuilder ...

        forceGC();
        long heapBefore = usedHeap();

        module.analyzeProject(Path.of("/tmp/test-project"));

        long heapAfter = usedHeap();
        long heapDelta = heapAfter - heapBefore;

        assertThat(heapDelta)
                .as("Heap increase should be < 50MB, was %d bytes", heapDelta)
                .isLessThan(50L * 1024 * 1024);
    }

    private DependencyGraph buildLargeGraph(int nodeCount, int edgeCount) {
        DependencyGraph graph = new DependencyGraph();
        List<Artifact> artifacts = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            Artifact a = new Artifact("g" + i, "a" + i, "1.0", "compile", false);
            artifacts.add(a);
            graph.addNode(a);
        }
        for (int i = 0; i < edgeCount && i + 1 < nodeCount; i++) {
            graph.addEdge(new Dependency(artifacts.get(i), artifacts.get(i + 1), "compile", false));
        }
        return graph;
    }

    private void forceGC() {
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private long usedHeap() {
        return memoryBean.getHeapMemoryUsage().getUsed();
    }
}
```

### Test Type 3: Scaling Regression Tests

Verify that a repeated operation does not allocate proportionally.

```java
@Test
@DisplayName("Repeated accessor calls should not grow heap proportionally")
void repeatedAccessShouldNotAllocate() {
    DependencyGraph graph = buildLargeGraph(500, 800);

    forceGC();
    long before = usedHeap();

    for (int i = 0; i < 1000; i++) {
        Set<Artifact> nodes = graph.getNodes();
        assertThat(nodes).hasSize(500);
    }

    forceGC();
    long after = usedHeap();
    // Should be near-zero if no copies are made
    assertThat(after - before).isLessThan(5L * 1024 * 1024); // 5MB tolerance
}
```

### Naming Conventions for Performance Tests

| Pattern | Example |
|---------|---------|
| `{Component}MemoryTest` | `DependencyGraphMemoryTest` |
| `{Pipeline}MemoryBudgetTest` | `AnalysisPipelineMemoryBudgetTest` |
| `should{Behavior}Within{Budget}` | `shouldStayWithinBudget50MB` |
| `noCopyOverheadAt{Size}` | `noCopyOverheadAt500Nodes` |

### Memory Budget Guidelines

| Graph size | Expected heap delta | Use case |
|------------|---------------------|----------|
| 100 nodes | < 10MB | Small project |
| 500 nodes | < 50MB | Medium project |
| 1000 nodes | < 100MB | Large project |
| 5000 nodes | < 300MB | Enterprise monolith |

Budgets should be generous enough to avoid flaky failures on CI but tight
enough to catch a 2-3x regression from reintroducing defensive copies.

## Updating Documentation

When a new category of memory/performance issue is discovered and fixed:

### 1. Add to `docs/patterns/memory_efficiency.md`

Add a new numbered section following the existing format:

```markdown
## N. <Pattern Name>

**Anti-pattern:** <description of what NOT to do>

**Fix:** <description of the correct approach>

**Checklist:**
- <check 1>
- <check 2>

**Reference fix:** `<ClassName>.<method>()` — <brief description>
```

Also update the Quick Reference table at the bottom.

### 2. Add a regression test

Write a test (see "Writing Performance Tests" above) that would fail if the
anti-pattern were reintroduced. Tag it `@Tag("slow")`.

### 3. Update this skill

Add the new pattern to the "Code Smell Catalogue" section above with its
detection grep command and reference fix.

### 4. Update `docs/patterns/test-organization.md`

If the fix introduces a new test category or changes conventions, update the
test organization document accordingly.

## Reference Files

- `docs/patterns/memory_efficiency.md` — all memory efficiency patterns and fixes
- `docs/patterns/test-organization.md` — test conventions, naming, and structure
- `community-core-engine/.../DependencyGraph.java` — example of defensive copy fix
- `community-core-engine/.../DependencyAnalysisModuleImpl.java` — example of pipeline snapshot fix
- `premium-intellij-plugin/.../AdvancedScanningService.java` — example of duplicate discovery fix
- `community-core-engine/.../SourceCodeScannerImpl.java` — example of fake streaming fix
- `community-core-engine/src/test/.../DependencyGraphMemoryTest.java` — reference identity tests
- `community-core-engine/src/test/.../AnalysisPipelineMemoryBudgetTest.java` — reference budget tests
- `skills/real-repo-integration-test/SKILL.md` — example of skill format

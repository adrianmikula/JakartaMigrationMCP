# Memory Efficiency Patterns

Rules for avoiding unnecessary memory allocation and overhead. Apply these during
new development and when reviewing existing code.

## 1. Avoid Defensive Copies in Domain Classes

**Anti-pattern:** `getNodes()` returns `new HashSet<>(nodes)` on every call.

```java
// BAD — allocates a new HashSet on every access
public Set<Artifact> getNodes() {
    return new HashSet<>(nodes);
}
```

**Fix:** Return the internal collection directly when the class is effectively
immutable (no public mutators that modify the returned collection).

```java
// GOOD — zero allocation
public Set<Artifact> getNodes() {
    return nodes;
}
```

**Checklist:**
- Does the class have any public method that mutates the returned collection?
  If no, return the internal set/list directly.
- If callers need to mutate, provide a separate mutable copy method instead of
  making every accessor defensive-copy.
- Verify no caller mutates the returned set before removing defensive copies
  (`grep` for `.getNodes().add`, `.getEdges().remove`, etc.).

## 2. Snapshot Data Once, Pass Through Pipeline

**Anti-pattern:** Calling a method that returns a collection multiple times in a
pipeline, each call potentially allocating a new collection.

```java
// BAD — 3 separate getNodes() calls, each may copy
NamespaceCompatibilityMap nsMap = identifyNamespaces(graph);  // copies nodes
List<Blocker> blockers = detectBlockers(graph);                // copies nodes again
List<Artifact> artifacts = graph.getNodes().stream()...;       // copies nodes a third time
```

**Fix:** Snapshot the collection once at the pipeline entry point.

```java
// GOOD — single snapshot, reused
List<Artifact> artifacts = new ArrayList<>(graph.getNodes());
List<Dependency> edges = new ArrayList<>(graph.getEdges());

NamespaceCompatibilityMap nsMap = identifyNamespaces(artifacts);
List<Blocker> blockers = detectBlockers(artifacts, nsMap);
List<VersionRecommendation> recommendations = recommendVersions(artifacts);
```

## 3. Don't Discover the Same Data Twice

**Anti-pattern:** Scanning the file tree twice for the same extension when both
results are needed.

```java
// BAD — two full directory walks for .java files
files.put(JAVA, scanner.findFiles(projectPath, List.of(".java")));
List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
List<Path> testFiles = javaFiles.stream().filter(this::isTestFile).collect(toList());
```

**Fix:** Scan once, reuse the result for both purposes.

```java
// GOOD — single walk
List<Path> javaFiles = scanner.findFiles(projectPath, List.of(".java"));
files.put(JAVA, javaFiles);
files.put(TEST, javaFiles.stream().filter(this::isTestFile).collect(toList()));
```

## 4. Don't Fake Streaming

**Anti-pattern:** Collecting a stream back into a single String and calling it
"streaming".

```java
// BAD — loads entire file into memory as a String
content = Files.lines(filePath).collect(Collectors.joining("\n"));
```

Either read the file directly (`Files.readString`) or process it line-by-line
without materialising the whole content. If the downstream API requires a String
(such as a parser), accept that the file will be in memory and skip the stream
overhead.

## 5. Count Lines Without Splitting the Content

**Anti-pattern:** Splitting a large String into a `String[]` just to count lines.

```java
// BAD — allocates a String[] with one entry per line
private int countLines(String content) {
    return content.split("\n").length;
}
```

**Fix:** Count lines from the file directly without loading the content first.

```java
// GOOD — no intermediate array
int lineCount;
try (var lines = Files.lines(filePath)) {
    lineCount = (int) lines.count();
}
```

## 6. Use try-with-resources for File Streams

Always close file-backed streams. `Files.lines()`, `Files.list()`,
`Files.walk()` return streams that hold native resources.

```java
// BAD — stream never closed
Files.lines(path).forEach(...);

// GOOD
try (var stream = Files.lines(path)) {
    stream.forEach(...);
}
```

## 7. Avoid Unbounded Collection Growth

**Anti-pattern:** Collecting all results into a `List` before processing.

```java
// BAD — holds entire result set in memory
List<Result> all = files.parallelStream()
    .map(this::process)
    .filter(Objects::nonNull)
    .collect(Collectors.toList());

// Then processes the whole list
for (Result r : all) { ... }
```

**Fix:** Process results as they arrive, or use bounded batch sizes.

```java
// GOOD — process in place
files.parallelStream().forEach(file -> {
    Result r = process(file);
    if (r != null) {
        store.add(r);  // store is a concurrent collection
    }
});
```

## 8. Prefer Direct Construction Over Constructor Copying

When building a data structure from pre-computed sets, pass them directly to the
constructor instead of constructing then copying.

```java
// BAD — constructor copies both sets
Set<Node> nodes = buildNodes(data);
Set<Edge> edges = buildEdges(data);
return new Graph(nodes, edges);  // constructor does new HashSet<>(nodes)

// GOOD — constructor takes ownership
Set<Node> nodes = buildNodes(data);
Set<Edge> edges = buildEdges(data);
return new Graph(nodes, edges);  // constructor assigns directly: this.nodes = nodes
```

## Quick Reference

| Pattern | Memory cost | Where to look |
|---------|-------------|---------------|
| Defensive copy per accessor | O(n) per call | Domain classes with `getNodes()`, `getEdges()`, `getItems()` |
| Repeated collection access in pipeline | O(n) × calls | Analysis/reporting pipelines |
| Duplicate file discovery | 2× tree walk + 2× list | File scanning / discovery methods |
| Fake streaming (stream → join → String) | Full file in memory + stream overhead | File reading utilities |
| `String.split()` for line counting | Full copy as `String[]` | Line count utilities |
| Unclosed file streams | Native resource leak | `Files.lines`, `Files.walk`, `Files.list` |
| Unbounded result collection | All results held before processing | Parallel stream collectors |
| Constructor defensive copy | O(n) per construction | Record/class constructors taking collections |

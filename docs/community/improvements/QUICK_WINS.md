# Quick Wins - Immediate Improvements

## 🚨 Critical Fixes (Do First - 1-2 Days Each)

### 1. Fix Spring Boot Blocker Detection (HIGHEST PRIORITY)
**File**: `DependencyAnalysisModuleImpl.java`
**Lines**: 255-264

**Current Code**:
```java
private boolean hasJakartaEquivalent(Artifact artifact) {
    if (artifact.groupId().startsWith("javax.")) {
        String jakartaGroupId = artifact.groupId().replace("javax.", "jakarta.");
        return isKnownJakartaEquivalent(jakartaGroupId, artifact.artifactId());
    }
    return false;
}
```

**Fix**:
```java
private boolean hasJakartaEquivalent(Artifact artifact) {
    // Framework detection - Spring Boot 3.x IS Jakarta
    if (isJakartaFramework(artifact)) {
        return true;
    }
    
    // Existing javax.* detection
    if (artifact.groupId().startsWith("javax.")) {
        String jakartaGroupId = artifact.groupId().replace("javax.", "jakarta.");
        return isKnownJakartaEquivalent(jakartaGroupId, artifact.artifactId());
    }
    
    return false;
}

private boolean isJakartaFramework(Artifact artifact) {
    // Spring Boot 3.x+ uses Jakarta
    if (artifact.groupId().equals("org.springframework.boot")) {
        return isVersion3OrHigher(artifact.version());
    }
    
    // Quarkus uses Jakarta
    if (artifact.groupId().equals("io.quarkus")) {
        return true;
    }
    
    // Jakarta EE implementations
    if (artifact.groupId().startsWith("jakarta.")) {
        return true;
    }
    
    return false;
}

private boolean isVersion3OrHigher(String version) {
    if (version == null) return false;
    try {
        String[] parts = version.split("\\.");
        if (parts.length > 0) {
            int major = Integer.parseInt(parts[0]);
            return major >= 3;
        }
    } catch (NumberFormatException e) {
        // Check string patterns
        return version.startsWith("3.") || version.contains("-3.");
    }
    return false;
}
```

**Impact**: Eliminates false positives that confuse users.

---

### 2. Expand Dependency Knowledge Base
**File**: `DependencyAnalysisModuleImpl.java`
**Lines**: 266-272

**Current Code** (only 4 packages):
```java
private boolean isKnownJakartaEquivalent(String groupId, String artifactId) {
    return (groupId.equals("jakarta.servlet") && artifactId.contains("servlet")) ||
           (groupId.equals("jakarta.persistence") && artifactId.contains("persistence")) ||
           (groupId.equals("jakarta.validation") && artifactId.contains("validation")) ||
           (groupId.equals("jakarta.ejb") && artifactId.contains("ejb"));
}
```

**Fix**: Create comprehensive mapping file:

**New File**: `src/main/resources/jakarta-mappings.yaml`
```yaml
mappings:
  - javax:
      groupId: "javax.mail"
      artifactId: "javax.mail"
    jakarta:
      groupId: "com.sun.mail"
      artifactId: "jakarta.mail"
    versionMapping:
      "1.5.5": "2.0.1"
      "1.6.2": "2.0.1"
  
  - javax:
      groupId: "javax.servlet"
      artifactId: "javax.servlet-api"
    jakarta:
      groupId: "jakarta.servlet"
      artifactId: "jakarta.servlet-api"
    versionMapping:
      "4.0.1": "6.0.0"
      "3.1.0": "5.0.0"
  
  - javax:
      groupId: "javax.validation"
      artifactId: "validation-api"
    jakarta:
      groupId: "jakarta.validation"
      artifactId: "jakarta.validation-api"
    versionMapping:
      "2.0.1.Final": "3.1.0"
      "1.1.0.Final": "2.0.2"
  
  - javax:
      groupId: "javax.persistence"
      artifactId: "javax.persistence-api"
    jakarta:
      groupId: "jakarta.persistence"
      artifactId: "jakarta.persistence-api"
    versionMapping:
      "2.2": "3.0.0"
      "2.1": "2.2.3"
```

**Update Code**:
```java
private final JakartaMappingService mappingService;

private boolean isKnownJakartaEquivalent(String groupId, String artifactId) {
    return mappingService.hasMapping(groupId, artifactId);
}

private Artifact findJakartaEquivalent(Artifact javaxArtifact) {
    JakartaMapping mapping = mappingService.findMapping(
        javaxArtifact.groupId(), 
        javaxArtifact.artifactId()
    );
    
    if (mapping != null) {
        String jakartaVersion = mappingService.getJakartaVersion(
            javaxArtifact.groupId(),
            javaxArtifact.artifactId(),
            javaxArtifact.version()
        );
        
        return new Artifact(
            mapping.getJakartaGroupId(),
            mapping.getJakartaArtifactId(),
            jakartaVersion != null ? jakartaVersion : "6.0.0",
            javaxArtifact.scope(),
            javaxArtifact.transitive()
        );
    }
    
    return null;
}
```

**Impact**: Finds ALL javax.* dependencies, not just 4.

---

### 3. Add Source Code Scanning
**New File**: `src/main/java/.../service/SourceCodeScanner.java`

```java
@Component
public class SourceCodeScanner {
    
    public SourceCodeAnalysisResult scanProject(Path projectPath) {
        List<FileUsage> usages = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(file -> {
                    FileUsage usage = scanFile(file);
                    if (usage.hasJavaxUsage()) {
                        usages.add(usage);
                    }
                });
        } catch (IOException e) {
            log.error("Error scanning project", e);
        }
        
        return new SourceCodeAnalysisResult(usages);
    }
    
    private FileUsage scanFile(Path file) {
        try {
            String content = Files.readString(file);
            List<ImportStatement> imports = extractJavaxImports(content);
            List<String> references = findJavaxReferences(content);
            
            return new FileUsage(file, imports, references);
        } catch (IOException e) {
            log.error("Error scanning file: " + file, e);
            return new FileUsage(file, List.of(), List.of());
        }
    }
    
    private List<ImportStatement> extractJavaxImports(String content) {
        List<ImportStatement> imports = new ArrayList<>();
        Pattern importPattern = Pattern.compile("^import\\s+(javax\\.\\w+.*);", Pattern.MULTILINE);
        Matcher matcher = importPattern.matcher(content);
        
        int lineNumber = 1;
        String[] lines = content.split("\n");
        
        while (matcher.find()) {
            String fullImport = matcher.group(1);
            // Find line number
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains(fullImport)) {
                    lineNumber = i + 1;
                    break;
                }
            }
            
            String jakartaEquivalent = fullImport.replace("javax.", "jakarta.");
            imports.add(new ImportStatement(fullImport, "javax.*", jakartaEquivalent, lineNumber));
        }
        
        return imports;
    }
}
```

**Add MCP Tool**:
```java
@McpTool(name = "scanSourceCode", description = "Scans source code for javax.* usage")
public String scanSourceCode(String projectPath) {
    SourceCodeAnalysisResult result = sourceCodeScanner.scanProject(Paths.get(projectPath));
    return buildSourceCodeReport(result);
}
```

**Impact**: Provides complete migration scope.

---

### 4. Add Gradle Support
**File**: `DependencyGraphBuilder.java` (or similar)

**Current**: Only parses `pom.xml`

**Fix**: Add Gradle parser:

```java
public class GradleDependencyParser implements BuildFileParser {
    
    @Override
    public boolean supports(Path buildFile) {
        String fileName = buildFile.getFileName().toString();
        return fileName.equals("build.gradle") || fileName.equals("build.gradle.kts");
    }
    
    @Override
    public List<Artifact> parseDependencies(Path buildFile) {
        // Use Gradle Tooling API or simple regex parsing
        // For quick win, use regex (later upgrade to Tooling API)
        
        try {
            String content = Files.readString(buildFile);
            return parseGradleDependencies(content);
        } catch (IOException e) {
            log.error("Error parsing Gradle file", e);
            return List.of();
        }
    }
    
    private List<Artifact> parseGradleDependencies(String content) {
        List<Artifact> artifacts = new ArrayList<>();
        
        // Match: implementation 'groupId:artifactId:version'
        Pattern pattern = Pattern.compile(
            "(?:implementation|api|compile|runtime)\\s+['\"]([^:]+):([^:]+):([^'\"]+)['\"]"
        );
        
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            artifacts.add(new Artifact(
                matcher.group(1),
                matcher.group(2),
                matcher.group(3),
                "compile",
                false
            ));
        }
        
        return artifacts;
    }
}
```

**Update DependencyGraphBuilder**:
```java
public DependencyGraph buildFromProject(Path project) {
    List<Artifact> artifacts = new ArrayList<>();
    
    // Try Maven
    Path pomXml = project.resolve("pom.xml");
    if (Files.exists(pomXml)) {
        artifacts.addAll(mavenParser.parseDependencies(pomXml));
    }
    
    // Try Gradle
    Path buildGradle = project.resolve("build.gradle");
    Path buildGradleKts = project.resolve("build.gradle.kts");
    if (Files.exists(buildGradle)) {
        artifacts.addAll(gradleParser.parseDependencies(buildGradle));
    } else if (Files.exists(buildGradleKts)) {
        artifacts.addAll(gradleParser.parseDependencies(buildGradleKts));
    }
    
    return buildGraph(artifacts);
}
```

**Impact**: Supports majority of Java projects.

---

## 📊 Medium Priority (1 Week Each)

### 5. Improve Migration Plan Specificity
**File**: `MigrationPlanner.java`

**Current**: Generic "Refactor Java files (batch 1)"

**Fix**: Add file-specific actions:

```java
private RefactoringPhase createDetailedPhase(List<String> files, Path project) {
    List<PhaseAction> actions = new ArrayList<>();
    
    for (String file : files) {
        Path filePath = project.resolve(file);
        if (file.endsWith(".java")) {
            SourceCodeAnalysisResult analysis = sourceCodeScanner.analyzeFile(filePath);
            actions.add(new PhaseAction(
                file,
                "Update imports",
                analysis.getJavaxImports().stream()
                    .map(imp -> "Replace '" + imp.getFullImport() + "' with '" + imp.getJakartaEquivalent() + "'")
                    .collect(Collectors.toList())
            ));
        } else if (file.contains("pom.xml")) {
            // Parse pom.xml and generate specific dependency updates
            actions.add(new PhaseAction(
                file,
                "Update dependencies",
                generateDependencyUpdates(filePath)
            ));
        }
    }
    
    return new RefactoringPhase(phaseNumber, description, files, actions, ...);
}
```

---

### 6. Add Version Recommendation for All javax.* Packages
**File**: `DependencyAnalysisModuleImpl.java`

**Current**: Only recommends if `groupId.startsWith("javax.")`

**Fix**: Also check artifactId and transitive dependencies:

```java
@Override
public List<VersionRecommendation> recommendVersions(List<Artifact> artifacts) {
    List<VersionRecommendation> recommendations = new ArrayList<>();
    
    for (Artifact artifact : artifacts) {
        // Check groupId
        if (artifact.groupId().startsWith("javax.")) {
            Artifact jakarta = findJakartaEquivalent(artifact);
            if (jakarta != null) {
                recommendations.add(createRecommendation(artifact, jakarta));
            }
        }
        
        // Check artifactId (e.g., javax.mail:javax.mail)
        if (artifact.artifactId().startsWith("javax-") || 
            artifact.artifactId().equals("javax.mail") ||
            artifact.artifactId().equals("validation-api")) {
            Artifact jakarta = findJakartaEquivalentByArtifactId(artifact);
            if (jakarta != null) {
                recommendations.add(createRecommendation(artifact, jakarta));
            }
        }
    }
    
    return recommendations;
}
```

---

## 🎯 Implementation Order

1. **Day 1-2**: Fix Spring Boot blocker detection
2. **Day 3-4**: Expand dependency knowledge base (add 20+ mappings)
3. **Day 5-7**: Add basic source code scanning
4. **Week 2**: Add Gradle support
5. **Week 3**: Improve migration plan specificity

After these quick wins, the tool will be significantly more useful and accurate!


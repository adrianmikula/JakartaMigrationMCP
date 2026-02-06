# MCP Tools Implementation

## Overview

The Jakarta Migration MCP server exposes six core tools that enable AI assistants to help with Jakarta EE migration tasks.

## Implemented Tools

### 1. `analyze_jakarta_readiness`

Analyzes a Java project for Jakarta migration readiness.

**Parameters**:
- `projectPath` (String): Path to the project root directory

**Returns**: JSON string containing:
- `status`: "success" or "error"
- `readinessScore`: 0.0 to 1.0
- `readinessMessage`: Human-readable message
- `totalDependencies`: Number of dependencies found
- `blockers`: Number of blockers
- `recommendations`: Number of version recommendations
- `riskScore`: Risk assessment score
- `riskFactors`: List of risk factors

**Example**:
```json
{
  "status": "success",
  "readinessScore": 0.75,
  "readinessMessage": "Mostly ready, some issues to resolve",
  "totalDependencies": 45,
  "blockers": 2,
  "recommendations": 8,
  "riskScore": 0.3,
  "riskFactors": ["2 dependency blockers found"]
}
```

---

### 2. `detect_blockers`

Detects blockers that prevent Jakarta migration.

**Parameters**:
- `projectPath` (String): Path to the project root directory

**Returns**: JSON string containing:
- `status`: "success" or "error"
- `blockerCount`: Number of blockers found
- `blockers`: Array of blocker objects with:
  - `artifact`: Artifact identifier
  - `type`: Blocker type (NO_JAKARTA_EQUIVALENT, etc.)
  - `reason`: Explanation
  - `confidence`: Confidence score (0.0 to 1.0)
  - `mitigationStrategies`: List of mitigation suggestions

**Example**:
```json
{
  "status": "success",
  "blockerCount": 2,
  "blockers": [
    {
      "artifact": "com.legacy:legacy-lib:1.0.0",
      "type": "NO_JAKARTA_EQUIVALENT",
      "reason": "No Jakarta equivalent found",
      "confidence": 0.9,
      "mitigationStrategies": ["Consider finding alternative library"]
    }
  ]
}
```

---

### 3. `recommend_versions`

Recommends Jakarta-compatible versions for dependencies.

**Parameters**:
- `projectPath` (String): Path to the project root directory

**Returns**: JSON string containing:
- `status`: "success" or "error"
- `recommendationCount`: Number of recommendations
- `recommendations`: Array of recommendation objects with:
  - `current`: Current artifact identifier
  - `recommended`: Recommended Jakarta artifact identifier
  - `migrationPath`: Migration instructions
  - `compatibilityScore`: Compatibility score (0.0 to 1.0)
  - `breakingChanges`: List of breaking changes

**Example**:
```json
{
  "status": "success",
  "recommendationCount": 5,
  "recommendations": [
    {
      "current": "javax.servlet:javax.servlet-api:4.0.1",
      "recommended": "jakarta.servlet:jakarta.servlet-api:6.0.0",
      "migrationPath": "Migrate to Jakarta namespace",
      "compatibilityScore": 0.95,
      "breakingChanges": ["Update imports from javax.* to jakarta.*"]
    }
  ]
}
```

---

### 4. `create_migration_plan`

Creates a migration plan for Jakarta migration.

**Parameters**:
- `projectPath` (String): Path to the project root directory

**Returns**: JSON string containing:
- `status`: "success" or "error"
- `phaseCount`: Number of migration phases
- `estimatedDuration`: Estimated duration in minutes
- `riskScore`: Risk assessment score
- `prerequisites`: List of prerequisites
- `phases`: Array of phase objects with:
  - `number`: Phase number
  - `description`: Phase description
  - `fileCount`: Number of files in phase
  - `estimatedDuration`: Estimated duration in minutes

**Example**:
```json
{
  "status": "success",
  "phaseCount": 5,
  "estimatedDuration": "120 minutes",
  "riskScore": 0.3,
  "prerequisites": ["Resolve 2 dependency blockers"],
  "phases": [
    {
      "number": 1,
      "description": "Update build files and dependencies",
      "fileCount": 2,
      "estimatedDuration": "4 minutes"
    }
  ]
}
```

---

### 5. `analyze_migration_impact`

Analyzes full migration impact combining dependency analysis and source code scanning.

**Parameters**:
- `projectPath` (String): Path to the project root directory

**Returns**: JSON string containing:
- `status`: "success" or "error"
- `totalFilesToMigrate`: Total number of files that need migration
- `totalJavaxImports`: Total number of javax.* imports found
- `totalBlockers`: Number of migration blockers
- `totalRecommendations`: Number of version recommendations
- `estimatedEffortMinutes`: Estimated migration effort in minutes
- `complexity`: Migration complexity level (LOW, MEDIUM, HIGH)
- `riskScore`: Risk assessment score
- `riskFactors`: List of risk factors
- `readinessScore`: Migration readiness score
- `readinessMessage`: Human-readable readiness message
- `totalFilesScanned`: Total number of source files scanned
- `totalDependencies`: Total number of dependencies

**Example**:
```json
{
  "status": "success",
  "totalFilesToMigrate": 15,
  "totalJavaxImports": 42,
  "totalBlockers": 2,
  "totalRecommendations": 8,
  "estimatedEffortMinutes": 120,
  "complexity": "MEDIUM",
  "riskScore": 0.3,
  "riskFactors": ["2 dependency blockers found"],
  "readinessScore": 0.75,
  "readinessMessage": "Mostly ready, some issues to resolve",
  "totalFilesScanned": 25,
  "totalDependencies": 45
}
```

---

### 6. `verify_runtime`

Verifies runtime execution of a migrated application.

**Parameters**:
- `jarPath` (String): Path to the JAR file to execute
- `timeoutSeconds` (Integer, optional): Timeout in seconds (default: 300)

**Returns**: JSON string containing:
- `status`: Verification status (SUCCESS, FAILED, PARTIAL, TIMEOUT)
- `errorCount`: Number of errors found
- `warningCount`: Number of warnings found
- `executionTime`: Execution time in seconds
- `exitCode`: Process exit code
- `message`: Human-readable message
- `errors`: Array of error messages (if any)

**Example**:
```json
{
  "status": "SUCCESS",
  "errorCount": 0,
  "warningCount": 2,
  "executionTime": "15 seconds",
  "exitCode": 0,
  "message": "Runtime verification passed"
}
```

---

## Architecture

### Components

1. **JakartaMigrationTools**: Main MCP tools class
   - Location: `src/main/java/com/bugbounty/jakartamigration/mcp/JakartaMigrationTools.java`
   - Exposes all 5 MCP tools as public methods

2. **DependencyAnalysisModuleImpl**: Dependency analysis implementation
   - Location: `src/main/java/com/bugbounty/jakartamigration/dependencyanalysis/service/impl/DependencyAnalysisModuleImpl.java`
   - Analyzes project dependencies and identifies Jakarta compatibility

3. **JakartaMigrationConfig**: Spring configuration
   - Location: `src/main/java/com/bugbounty/jakartamigration/config/JakartaMigrationConfig.java`
   - Wires up all service beans

### Integration Points

- **Dependency Analysis**: Uses `DependencyGraphBuilder` and `NamespaceClassifier`
- **Migration Planning**: Uses `MigrationPlanner` and `RecipeLibrary`
- **Runtime Verification**: Uses `RuntimeVerificationModule`

---

## Usage

### As Spring Component

The tools are available as a Spring `@Component` and can be injected:

```java
@Autowired
private JakartaMigrationTools tools;

String result = tools.analyzeJakartaReadiness("/path/to/project");
```

### As MCP Server

When Spring AI MCP Server is available, add `@Tool` annotations:

```java
@Tool(
    name = "analyze_jakarta_readiness",
    description = "Analyzes a Java project for Jakarta migration readiness"
)
public String analyzeJakartaReadiness(@ToolParam("projectPath") String projectPath) {
    // Implementation
}
```

Or use the official MCP Java SDK to expose these methods as MCP tools.

---

## Error Handling

All tools return JSON responses with a `status` field:
- `"success"`: Operation completed successfully
- `"error"`: Operation failed with error message

Error responses include:
```json
{
  "status": "error",
  "message": "Error description"
}
```

---

## Future Enhancements

1. **Add @Tool annotations** when Spring AI MCP Server is available
2. **Add migrate_to_jakarta tool** for actual migration execution
3. **Add rollback tool** for migration rollback
4. **Add progress tracking** for long-running migrations
5. **Add batch operations** for multiple projects

---

*Last Updated: 2026-01-27*


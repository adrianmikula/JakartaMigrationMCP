# Migration Tools Support

## Overview

The Jakarta Migration MCP supports multiple migration tools for converting Java EE 8 applications to Jakarta EE 9. This document describes the available tools and how to configure them.

## Supported Migration Tools

### 1. Simple String Replacement (Default)

**Status**: ✅ Available (Fallback)

- **Description**: Basic string-based refactoring for Java source code files (.java)
- **Input**: Java source code files
- **Output**: Modified source code files
- **Pros**: Fast, no external dependencies, always available
- **Cons**: Less accurate, may miss edge cases, doesn't handle all Java EE packages
- **Use Case**: Quick source code migrations, fallback when other tools are unavailable
- **When to Use**: Refactoring individual Java source files from javax.* to jakarta.*

### 2. Apache Tomcat Jakarta EE Migration Tool

**Status**: ✅ Available (Auto-downloads on first use)

- **Description**: Official Apache tool for migrating COMPILED JAR/WAR files (bytecode), NOT source code
- **Input**: Compiled JAR, WAR, or EAR files (bytecode)
- **Output**: New migrated JAR file with Jakarta namespace
- **Source**: [Apache Tomcat Jakarta EE Migration Tool](https://github.com/apache/tomcat-jakartaee-migration)
- **Pros**: 
  - Official Apache tool
  - Comprehensive bytecode coverage (classes, String constants, config files, JSPs, TLDs)
  - Handles JAR file signatures (removes them as needed)
  - Well-tested and maintained
  - Auto-downloads on first use
- **Cons**: 
  - Works on bytecode only, not source code
  - Command-line tool (process execution overhead)
- **Primary Use Cases**:
  1. **Compatibility Testing**: Test if compiled application works after Jakarta migration
  2. **Bytecode Validation**: Cross-check source code migration completeness
  3. **Library Assessment**: Check if third-party libraries are Jakarta-compatible
- **When to Use**: 
  - Testing compiled JAR/WAR compatibility
  - Validating bytecode after source migration
  - Assessing third-party library compatibility
  - Identifying runtime issues before source migration

**Installation**:
- **Automatic (Recommended)**: The tool is automatically downloaded on first use and cached locally
- **Manual Override (Optional)**: Set environment variable: `JAKARTA_MIGRATION_TOOL_PATH=/path/to/jakartaee-migration-*-shaded.jar`
- **Manual Download**: Download from [Apache Tomcat Downloads](https://tomcat.apache.org/download-migration.cgi) and place in cache directory

**Cache Location**:
- **Windows**: `%USERPROFILE%\AppData\Local\jakarta-migration-tools\`
- **Linux/macOS**: `~/.cache/jakarta-migration-tools/`

**Usage**:
```java
RefactoringEngine engine = new RefactoringEngine(MigrationTool.APACHE_TOMCAT_MIGRATION);
RefactoringChanges changes = engine.refactorFile(filePath, recipes);
```

### 3. OpenRewrite

**Status**: ⚠️ Planned (Not yet implemented)

- **Description**: AST-based refactoring for Java source code files
- **Input**: Java source code files (.java, .xml)
- **Output**: Modified source code files
- **Source**: [OpenRewrite](https://docs.openrewrite.org)
- **Pros**: 
  - AST-based (more accurate than string replacement)
  - Programmatic API
  - Comprehensive recipe library
  - Extensible
- **Cons**: 
  - Requires dependency management
  - Learning curve
- **Use Case**: Production source code migrations, complex refactoring scenarios
- **When to Use**: High-accuracy source code refactoring with complex patterns

**Status**: TODO - Integration pending dependency addition

## Tool Selection

The `RefactoringEngine` automatically selects the best available tool based on:

1. **Preferred Tool**: Set via constructor parameter
2. **Tool Availability**: Checks if tool is installed/available
3. **Fallback**: Falls back to simple string replacement if preferred tool unavailable

### Example Usage

```java
// Use Apache Tomcat tool (if available)
RefactoringEngine engine = new RefactoringEngine(MigrationTool.APACHE_TOMCAT_MIGRATION);

// Use specific tool JAR path
Path toolJar = Paths.get("/path/to/jakartaee-migration-1.0.0-shaded.jar");
RefactoringEngine engine = new RefactoringEngine(
    MigrationTool.APACHE_TOMCAT_MIGRATION, 
    toolJar
);

// Use default (simple string replacement)
RefactoringEngine engine = new RefactoringEngine();
```

## Configuration

### Environment Variables

- `JAKARTA_MIGRATION_TOOL_PATH`: (Optional) Override path to Apache Tomcat migration tool JAR

### Tool Detection and Auto-Download

The system automatically handles tool availability:

1. **Environment Variable Override** (Optional): If `JAKARTA_MIGRATION_TOOL_PATH` is set, uses that path
2. **Cache Directory**: Checks for auto-downloaded tool in cache directory
3. **Auto-Download**: If not found, automatically downloads the latest version from GitHub releases
4. **Manual Installations**: Falls back to checking common installation locations:
   - `~/.local/share/jakartaee-migration/`
   - `/usr/share/java/`
   - `/opt/jakartaee-migration/`
   - Current directory

**No manual setup required** - the tool is downloaded automatically on first use!

## Tool Comparison

| Feature | Simple Replacement | Apache Tool | OpenRewrite |
|---------|-------------------|-------------|-------------|
| Accuracy | Medium | High | High |
| Speed | Fast | Medium | Fast |
| Dependencies | None | JAR file | Maven/Gradle |
| Coverage | Basic | Comprehensive | Comprehensive |
| JAR Support | No | Yes | Yes |
| Config Files | Partial | Yes | Yes |
| JSP/TLD | No | Yes | Yes |
| String Constants | No | Yes | Yes |
| Programmatic API | Yes | No (CLI) | Yes |

## Recommendations

### For Production Use
- **Recommended**: Apache Tomcat Migration Tool
- **Reason**: Official tool, comprehensive coverage, well-tested

### For Development/Testing
- **Recommended**: Simple String Replacement
- **Reason**: Fast, no setup required, good for quick tests

### For Advanced Use Cases
- **Recommended**: OpenRewrite (when available)
- **Reason**: Programmatic API, extensible, AST-based accuracy

## Implementation Details

### ApacheTomcatMigrationTool

The `ApacheTomcatMigrationTool` service:
- Executes the migration tool as a separate process
- Captures stdout/stderr for error reporting
- Handles timeouts (default: 5 minutes)
- Creates temporary directories for migration results
- Cleans up temporary files after migration

### Process Execution

The tool is executed as:
```bash
java -jar jakartaee-migration-*-shaded.jar <source> <destination>
```

The service:
1. Creates a temporary destination directory
2. Executes the migration tool
3. Reads the migrated content
4. Generates change details
5. Cleans up temporary files

## Future Enhancements

1. **OpenRewrite Integration**: Full OpenRewrite support when dependency is added
2. **Tool Chaining**: Use multiple tools in sequence for best results
3. **Tool Comparison**: Compare results from different tools
4. **Automatic Tool Selection**: AI-based selection of best tool for specific files
5. **Tool Caching**: Cache tool results for repeated migrations

---

*Last Updated: 2026-01-27*
*Status: Apache Tomcat tool support implemented*


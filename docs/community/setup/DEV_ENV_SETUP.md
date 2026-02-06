# Development Environment Setup

This document outlines the complete development environment setup for the Jakarta Migration MCP server project, including OpenRewrite, MCP servers, and Java development tools.

## Quick Reference

### ✅ What's Already Set Up

1. **OpenRewrite ✅**
   - Added to both `pom.xml` and `build.gradle.kts`
   - Version: 8.10.0
   - Jakarta migration recipes pre-configured
   - Usage: `mvn rewrite:dryRun` or `./gradlew rewriteDryRun`

2. **Spring AI MCP Server ✅**
   - Dependency added (ready to use)
   - Version: 1.1.2
   - Usage: Add `@McpTool` annotations to create MCP tools

3. **Java Development Tools ✅**
   - Java 21: Configured via `mise` (`.mise.toml`)
   - Gradle 8.5: Configured
   - Maven: Available

### 🚀 Quick Start: Building Your First MCP Tool

1. **Create a Tool Component**:
   ```java
   @Component
   public class JakartaMigrationTools {
       @McpTool(name = "analyzeJakartaReadiness", description = "...")
       public String analyzeReadiness(String projectPath) {
           // Your implementation
       }
   }
   ```

2. **Configure MCP Server** in `application.yml` (already configured)

3. **Run**: `./gradlew bootRun`

### 🛠️ Useful Commands

**OpenRewrite**:
- `./gradlew rewriteDryRun` - Preview changes
- `./gradlew rewriteRun` - Apply changes

**Build & Test**:
- `./gradlew build` - Build project
- `./gradlew test` - Run tests
- `./gradlew bootRun` - Run application

For detailed information, see sections below.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [OpenRewrite Installation](#openrewrite-installation)
3. [MCP Servers for Jakarta/Java Migration](#mcp-servers-for-jakartajava-migration)
4. [Java Development Tools](#java-development-tools)
5. [Quick Setup Scripts](#quick-setup-scripts)

---

## Prerequisites

### Required Tools

- **Java 21+** - Already configured via `mise` (see `.mise.toml`)
- **Maven 3.6+** or **Gradle 8.5+** - Already configured
- **Docker** - For running PostgreSQL and Redis
- **Git** - For repository operations

### Verify Installation

```bash
# Check Java version
java -version  # Should show Java 21

# Check Maven (if using Maven)
mvn -version

# Check Gradle (if using Gradle)
./gradlew --version

# Check Docker
docker --version
```

---

## OpenRewrite Installation

OpenRewrite is now integrated into the project as a dependency. You can use it in two ways:

### 1. As a Library (Programmatic Use)

OpenRewrite is already added to both `pom.xml` and `build.gradle.kts`. The dependencies include:

- `rewrite-java` - Core Java refactoring engine
- `rewrite-maven` - Maven project support
- `rewrite-migrate-java` - Java migration recipes (including Jakarta)
- `rewrite-spring` - Spring Boot migration recipes

### 2. As a Build Plugin

#### Maven

The OpenRewrite Maven plugin is configured in `pom.xml`. Run recipes with:

```bash
# Dry run (preview changes)
mvn rewrite:dryRun

# Apply changes
mvn rewrite:run

# Generate rewrite configuration
mvn rewrite:discover
```

#### Gradle

The OpenRewrite Gradle plugin is configured in `build.gradle.kts`. Run recipes with:

```bash
# Dry run (preview changes)
./gradlew rewriteDryRun

# Apply changes
./gradlew rewriteRun

# Generate rewrite configuration
./gradlew rewriteDiscover
```

### Active Recipes

The following recipes are pre-configured:

1. **UpgradeToJava21** - Migrates code to Java 21 syntax
2. **AddJakartaNamespace** - Converts `javax.*` to `jakarta.*`
3. **UpgradeSpringBoot_3_2** - Migrates Spring Boot to 3.2+

### Using OpenRewrite Programmatically

Example Java code to use OpenRewrite:

```java
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.xml.tree.Xml;

// Parse and refactor Java files
JavaParser javaParser = JavaParser.fromJavaVersion()
    .classpath("classpath")
    .build();

List<J.CompilationUnit> cu = javaParser.parse("src/main/java/**/*.java");

// Apply recipes programmatically
// (See OpenRewrite documentation for recipe examples)
```

### OpenRewrite CLI (Optional)

For standalone CLI usage:

```bash
# Install via SDKMAN
sdk install rewrite

# Or download from: https://github.com/openrewrite/rewrite/releases
```

---

## MCP Servers for Jakarta/Java Migration

### 1. ArchLift Java Modernization MCP Server ⭐⭐⭐

**Purpose**: Comprehensive Java legacy modernization with OpenRewrite integration

**Key Features**:
- AST-based code generation
- Template-driven generation
- Source code parsing
- JSP analysis
- Bytecode analysis
- **OpenRewrite integration** for automated refactoring
- Jakarta migration workflows

**Installation**:

```bash
# Via npm (wrapper)
npm install -g @archlift/remodern-java-mcp

# Or via Docker
docker pull phodal/remodern-java-mcp:latest
```

**Configuration** (add to your MCP config):

```json
{
  "mcpServers": {
    "archlift-java": {
      "command": "npx",
      "args": ["-y", "@archlift/remodern-java-mcp"],
      "env": {
        "JAVA_HOME": "/path/to/java17+"
      }
    }
  }
}
```

**Resources**:
- Website: https://playbooks.com/mcp/phodal-remodern-java
- Prerequisites: Java 17+, Maven 3.6+

---

### 2. Maven Tools MCP Server ⭐⭐

**Purpose**: AI-powered Maven Central intelligence and dependency analysis

**Key Features**:
- Instant dependency analysis for Maven, Gradle, SBT, Mill
- Bulk operations
- Version intelligence
- Age analysis
- Context7 integration for documentation

**Installation**:

```bash
npm install -g maven-tools-mcp
```

**Configuration**:

```json
{
  "mcpServers": {
    "maven-tools": {
      "command": "npx",
      "args": ["-y", "maven-tools-mcp"]
    }
  }
}
```

**Resources**:
- Website: https://mcpservers.org/servers/arvindand/maven-tools-mcp

---

### 3. Spring Web to MCP Converter ⭐⭐

**Purpose**: Converts Spring Web REST APIs to MCP server tools using OpenRewrite recipes

**Key Features**:
- Automatic conversion of Spring REST APIs to MCP tools
- Uses OpenRewrite recipes
- Supports Spring Boot 3.2+

**Installation**:

```bash
# Clone the repository
git clone https://github.com/addozhang/spring-rest-to-mcp.git
cd spring-rest-to-mcp

# Build and install
mvn clean install
```

**Resources**:
- GitHub: https://github.com/addozhang/spring-rest-to-mcp
- Website: https://lobehub.com/nl/mcp/addozhang-spring-rest-to-mcp

---

## Java Development Tools

### Dependency Analysis Tools

#### 1. Maven Dependency Plugin

```bash
# Analyze dependencies
mvn dependency:tree

# Analyze dependency conflicts
mvn dependency:analyze

# Copy dependencies to target/dependency
mvn dependency:copy-dependencies
```

#### 2. Gradle Dependency Insights

```bash
# Show dependency tree
./gradlew dependencies

# Show dependency insight
./gradlew dependencyInsight --dependency <groupId>:<artifactId>

# Check for dependency updates
./gradlew dependencyUpdates
```

#### 3. OWASP Dependency-Check

Security vulnerability scanning:

```bash
# Install
npm install -g dependency-check

# Scan Maven project
dependency-check --project "My Project" --scan pom.xml --format HTML

# Scan Gradle project
dependency-check --project "My Project" --scan build.gradle.kts --format HTML
```

### Java Linting Tools

#### 1. Checkstyle

Already integrated with OpenRewrite recipes:

```bash
# Install Checkstyle
mvn checkstyle:check

# Or with Gradle
./gradlew checkstyleMain
```

**OpenRewrite Checkstyle Recipes**:
- Automatically fixes Checkstyle violations
- GitHub: https://github.com/checkstyle/checkstyle-openrewrite-recipes

#### 2. SpotBugs

Static analysis tool:

```bash
# Add to pom.xml or build.gradle.kts
# Then run:
mvn spotbugs:check
# Or
./gradlew spotbugsMain
```

#### 3. PMD

Code quality analysis:

```bash
# Add PMD plugin to build file
# Then run:
mvn pmd:check
# Or
./gradlew pmdMain
```

### JVM Management Tools

#### 1. SDKMAN! (Recommended)

Manage multiple JDK versions:

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash

# Install Java versions
sdk install java 21.0.0-tem
sdk install java 17.0.9-tem
sdk install java 11.0.20-tem

# Switch versions
sdk use java 21.0.0-tem

# List installed versions
sdk list java
```

**Windows Alternative**: Use `mise` (already configured in `.mise.toml`)

#### 2. Jabba (Alternative to SDKMAN)

```bash
# Install Jabba
curl -sL https://github.com/shyiko/jabba/raw/master/install.sh | bash

# Install Java versions
jabba install openjdk@1.21.0
jabba install openjdk@1.17.0

# Use version
jabba use openjdk@1.21.0
```

### Micro-JVMs

#### 1. GraalVM Native Image

Create native executables:

```bash
# Install GraalVM
sdk install java 21.0.0-graal

# Build native image
native-image -jar your-app.jar your-app

# Or with Maven
mvn -Pnative native:compile

# Or with Gradle
./gradlew nativeCompile
```

#### 2. Eclipse OpenJ9

Lightweight JVM alternative:

```bash
# Download from: https://adoptium.net/temurin/releases/
# Use with JAVA_HOME pointing to OpenJ9 installation
```

#### 3. Quarkus (Micro-JVM Framework)

If building microservices:

```bash
# Create Quarkus project
mvn io.quarkus.platform:quarkus-maven-plugin:3.6.0:create

# Build native image
./mvnw package -Pnative
```

---

## Quick Setup Scripts

### Windows (PowerShell)

```powershell
# Install OpenRewrite dependencies
.\gradlew build --refresh-dependencies

# Verify OpenRewrite
.\gradlew rewriteDiscover

# Install MCP servers (if using npm)
npm install -g @archlift/remodern-java-mcp
npm install -g maven-tools-mcp

# Verify Java tools
java -version
mvn -version
./gradlew --version
```

### Linux/Mac (Bash)

```bash
# Install OpenRewrite dependencies
./gradlew build --refresh-dependencies

# Verify OpenRewrite
./gradlew rewriteDiscover

# Install MCP servers (if using npm)
npm install -g @archlift/remodern-java-mcp
npm install -g maven-tools-mcp

# Verify Java tools
java -version
mvn -version
./gradlew --version
```

---

## Testing Your Setup

### 1. Test OpenRewrite

```bash
# Maven
mvn rewrite:dryRun

# Gradle
./gradlew rewriteDryRun
```

### 2. Test MCP Servers

Add to your MCP configuration and verify they appear in your AI assistant.

### 3. Test Java Tools

```bash
# Dependency analysis
mvn dependency:tree
# Or
./gradlew dependencies

# Linting
mvn checkstyle:check
# Or
./gradlew checkstyleMain
```

---

## Troubleshooting

### OpenRewrite Issues

**Problem**: Recipes not found
**Solution**: Ensure dependencies are in classpath and recipes are correctly named

**Problem**: Build fails after applying recipes
**Solution**: Use `rewrite:dryRun` first to preview changes, then apply incrementally

### MCP Server Issues

**Problem**: MCP server not starting
**Solution**: Check Java version (requires 17+), verify JAVA_HOME is set correctly

**Problem**: Cannot find MCP server
**Solution**: Verify npm global installation path is in PATH, or use npx

### JVM Management Issues

**Problem**: Wrong Java version
**Solution**: Use `mise` (already configured) or SDKMAN to switch versions

---

## Java MCP Server Building Blocks

For building your own MCP server, see the comprehensive guide:

**📖 [Java MCP Server Libraries Guide](./JAVA_MCP_LIBRARIES.md)**

This guide covers:
- **Spring AI MCP Server** (Recommended for this project) ⭐⭐⭐
- **Official MCP Java SDK** (Framework-agnostic) ⭐⭐⭐
- **Quarkus MCP Server SDK** (For Quarkus projects) ⭐⭐
- **Tinystruct MCP** (Lightweight option) ⭐

**Quick Start**: Since this project uses Spring Boot, the **Spring AI MCP Server** is already added as a dependency. Just use `@Tool` annotations to create MCP tools!

---

## Next Steps

1. **Configure MCP Servers**: Add the recommended MCP servers to your MCP configuration
2. **Test OpenRewrite**: Run a dry-run on a sample project
3. **Set Up Linting**: Configure Checkstyle, SpotBugs, or PMD
4. **Explore Recipes**: Browse OpenRewrite recipes at https://docs.openrewrite.org/recipes

---

## Resources

- **OpenRewrite Documentation**: https://docs.openrewrite.org
- **OpenRewrite Recipes**: https://docs.openrewrite.org/reference/recipes
- **ArchLift Java MCP**: https://playbooks.com/mcp/phodal-remodern-java
- **Maven Tools MCP**: https://mcpservers.org/servers/arvindand/maven-tools-mcp
- **Spring AI MCP**: https://docs.spring.io/spring-ai/reference/
- **SDKMAN**: https://sdkman.io
- **GraalVM**: https://www.graalvm.org

---

*Last Updated: January 2026*


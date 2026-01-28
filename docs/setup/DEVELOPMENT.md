# Development Setup

This guide is for developers who want to build, test, and contribute to the Jakarta Migration MCP Server.

## Prerequisites

- **Java 21+** - [Download from Adoptium](https://adoptium.net/)
- **Node.js 18+** - [Download from nodejs.org](https://nodejs.org/)
- **Docker & Docker Compose** - For local services (optional, for integration tests)

## Quick Start

### Using Mise (Recommended)

[mise](https://mise.jdx.dev/) is a tool version manager that handles all dependencies automatically.

**Install mise:**
```bash
# Windows (winget)
winget install jdx.mise

# Linux/Mac
curl https://mise.run | sh

# Mac (Homebrew)
brew install mise
```

**Setup project:**
```bash
cd JakartaMigrationMCP
mise install          # Installs Java 21, Gradle, Node.js
mise run setup        # Runs setup script
```

**Common mise commands:**
```bash
mise tasks            # View all available commands
mise run test         # Run all tests
mise run test-unit    # Run unit tests only
mise run build        # Build the project (without tests)
mise run build-all    # Build with tests
mise run run          # Run the application
mise run coverage     # Generate code coverage report
mise run clean        # Clean build artifacts
mise run start-services # Start Docker services (PostgreSQL, Redis)
```

See [Mise Setup Guide](MISE_SETUP.md) for complete task reference.

### Manual Setup

**1. Install Java 21+**
```bash
# Verify installation
java -version
```

**2. Install Node.js 18+**
```bash
# Verify installation
node --version
npm --version
```

**3. Build the project**
```bash
./gradlew build
```

**4. Run the application**
```bash
./gradlew bootRun
```

### GraalVM Native Image Setup (Optional)

For **faster startup times** and tighter agentic feedback loops, you can build and run the MCP server as a GraalVM native image. This is especially useful when iterating quickly with AI assistants.

#### Prerequisites

- **GraalVM JDK 21+** with `native-image` component
  - Download from [GraalVM Releases](https://github.com/graalvm/graalvm-ce-builds/releases) or [Oracle GraalVM](https://www.graalvm.org/downloads/)
  - Ensure the `native-image` component is included (most distributions include it)

#### Installation

**Windows:**

1. Download and extract GraalVM to a location like `C:\Runtimes\graalvm-jdk-25.0.2+10.1`
2. Set environment variables (User or System):
   - `JAVA_HOME` = `C:\Runtimes\graalvm-jdk-25.0.2+10.1`
   - `GRAALVM_HOME` = `C:\Runtimes\graalvm-jdk-25.0.2+10.1`
   - Add `%JAVA_HOME%\bin` to your `PATH` (before any other Java entries)

3. **Important**: Remove or move to the bottom of PATH:
   - `C:\Program Files\Common Files\Oracle\Java\javapath` (Oracle JDK shim)

4. Verify installation:
   ```powershell
   java -version        # Should show GraalVM
   native-image --version
   ```

**macOS/Linux:**

1. Download and extract GraalVM:
   ```bash
   # Example: Extract to ~/runtimes/graalvm-jdk-25.0.2+10.1
   tar -xzf graalvm-jdk-25.0.2+10.1.tar.gz -C ~/runtimes/
   ```

2. Set environment variables in `~/.bashrc` or `~/.zshrc`:
   ```bash
   export JAVA_HOME=~/runtimes/graalvm-jdk-25.0.2+10.1
   export GRAALVM_HOME=$JAVA_HOME
   export PATH=$JAVA_HOME/bin:$PATH
   ```

3. Verify installation:
   ```bash
   java -version        # Should show GraalVM
   native-image --version
   ```

#### Cursor IDE Configuration

This project is configured to work seamlessly with Cursor's sandbox restrictions:

- **Workspace-local Gradle cache**: The `gradlew` scripts automatically set `GRADLE_USER_HOME` to `.gradle/` in the project root, so Gradle can access its cache within Cursor's sandbox.

**No additional configuration needed** - just ensure GraalVM is installed and `JAVA_HOME` points to it.

#### Building Native Images

**Build the native image:**
```bash
# Windows
.\gradlew.bat nativeCompile -x test

# macOS/Linux
./gradlew nativeCompile -x test
```

This produces a native binary at:
- **Windows**: `build/native/nativeCompile/jakarta-migration-mcp.exe`
- **macOS/Linux**: `build/native/nativeCompile/jakarta-migration-mcp`

**Note**: First build takes 5-15 minutes. Subsequent builds are faster due to caching.

#### Running Native Images

**Direct execution:**
```bash
# Windows PowerShell
build\native\nativeCompile\jakarta-migration-mcp.exe --spring.profiles.active=mcp-streamable-http

# macOS/Linux
build/native/nativeCompile/jakarta-migration-mcp --spring.profiles.active=mcp-streamable-http
```

**Using the convenience task:**
```bash
# Windows
.\gradlew.bat nativeDev

# macOS/Linux
./gradlew nativeDev
```

This task automatically builds (if needed) and runs the native image with the `mcp-streamable-http` profile.

#### Development Workflow

- **Use JVM for active development**: `./gradlew bootRun` - faster iteration, easier debugging
- **Use native image for fast startup loops**: `./gradlew nativeDev` - when you need to quickly test startup time or run tight agentic feedback loops

#### JVM and Gradle startup optimizations

These options reduce **JVM** and **Spring Boot** startup time when using GraalVM (or any JDK) for `bootRun` and Gradle. They are optional and dev-oriented.

**1. Fast startup for `bootRun` (JVM flags)**

Use the `fastStartup` project property to enable JVM options that prioritize startup over peak throughput:

```bash
./gradlew bootRun -PfastStartup --args='--spring.profiles.active=mcp-streamable-http'
```

What it does:
- **`-XX:TieredStopAtLevel=1`** – C1 compiler only (faster startup, slower warmup to peak).
- **`-XX:+UseSerialGC`** – Serial GC (lower footprint, faster for small heaps).
- **GraalVM only**: **`-Djdk.graal.CompilerConfiguration=economy`** – Economy JIT (faster compilation, slightly lower peak performance). Ignored on non-GraalVM JDKs.

**2. Lazy initialization (Spring)**

Activate the `dev-fast` profile so beans are created on first use instead of at startup:

```bash
./gradlew bootRun -PfastStartup --args='--spring.profiles.active=mcp-streamable-http,dev-fast'
```

Trade-off: first request can be slower; startup is faster. See `src/main/resources/application-dev-fast.yml`.

**3. Gradle daemon (cold start)**

In `gradle.properties`, an optional commented block suggests lower memory and `-XX:TieredStopAtLevel=1` for the **Gradle daemon** to speed up cold starts (e.g. CI or after killing the daemon). Uncomment and adjust if you need faster daemon startup at the cost of slightly slower peak builds.

**4. Class Data Sharing (CDS)**

On JDKs that support it, CDS is often enabled by default and reduces startup. To force use of the default CDS archive (if available): `-Xshare:on`. Omit if the JVM reports CDS errors.

**Summary**

| Goal | Command / config |
|------|-------------------|
| Fastest JVM bootRun (GraalVM) | `./gradlew bootRun -PfastStartup --args='--spring.profiles.active=mcp-streamable-http,dev-fast'` |
| Fastest overall startup | Build and run the **native image**: `./gradlew nativeDev` |

#### Troubleshooting

**Issue**: `java -version` shows Oracle JDK instead of GraalVM
- **Solution**: Check `PATH` order - GraalVM's `bin` must come before Oracle's `javapath`
- **Windows**: Remove `C:\Program Files\Common Files\Oracle\Java\javapath` from PATH or move it to the bottom

**Issue**: `native-image --version` not found
- **Solution**: Most GraalVM distributions include `native-image`. If missing, install it:
  ```bash
  gu install native-image  # GraalVM Updater
  ```

**Issue**: Gradle can't access cache in Cursor
- **Solution**: Already handled! The project's `gradlew` scripts set `GRADLE_USER_HOME` to `.gradle/` in the workspace, which is accessible to Cursor's sandbox.

**Issue**: Native build fails with reflection errors
- **Solution**: Add reflection hints to `src/main/java/adrianmikula/jakartamigration/config/NativeHintsConfig.java` as needed. Spring Boot 3's AOT engine handles most cases automatically.

## Project Structure

```
src/
├── main/java/adrianmikula/jakartamigration/
│   ├── config/              # Configuration (feature flags, license validation)
│   ├── mcp/                 # MCP tools implementation
│   ├── dependencyanalysis/  # Dependency analysis module
│   ├── coderefactoring/     # Code refactoring module
│   └── runtimeverification/ # Runtime verification module
└── test/
    ├── java/unit/           # Unit tests
    ├── java/component/      # Component tests
    └── java/e2e/            # End-to-end tests
```

## Running Tests

```bash
# All tests
./gradlew test

# Unit tests only
./gradlew test --tests "*Test" --exclude-tests "*ComponentTest" --exclude-tests "*E2ETest"

# Component tests
./gradlew test --tests "adrianmikula.jakartamigration.component.*"

# Specific test class
./gradlew test --tests "component.jakartamigration.mcp.McpSseControllerIntegrationTest"

# With coverage
./gradlew test jacocoTestReport
```

## Building

```bash
# Build JAR
./gradlew bootJar

# Build for release
./scripts/build-release.sh  # Linux/macOS
.\scripts\build-release.ps1  # Windows
```

## Code Coverage

Coverage reports are generated automatically after tests:

- **HTML Report**: `build/reports/jacoco/test/html/index.html`
- **XML Report**: `build/reports/jacoco/test/jacocoTestReport.xml`

```bash
# Generate coverage
./gradlew jacocoTestReport

# View coverage summary
./gradlew jacocoCoverageSummary
```

## Tech Stack

### Core Technologies

- **Java 21** - Modern Java with virtual threads and pattern matching
- **Spring Boot 3.2+** - Application framework with Spring AI MCP integration
- **Spring AI 1.1.2** - MCP server framework and AI integration
- **Gradle** - Build automation and dependency management
- **OpenRewrite** - Automated code refactoring and migration recipes

### Key Libraries

- **JGit** - Git repository operations
- **ASM** - Bytecode analysis for runtime verification
- **Resilience4j** - Circuit breakers and rate limiting
- **TestContainers** - Integration testing with Docker
- **JaCoCo** - Code coverage reporting

### Infrastructure

- **PostgreSQL** - State management (optional, for advanced features)
- **Redis** - Caching and queues (optional)
- **Docker Compose** - Local development environment

## Development Workflow

1. **Fork and clone** the repository
2. **Create a branch** for your feature: `git checkout -b feature/my-feature`
3. **Make changes** and write tests
4. **Run tests**: `./gradlew test`
5. **Check coverage**: `./gradlew jacocoTestReport`
6. **Commit changes**: `git commit -m "Add feature X"`
7. **Push and create PR**: `git push origin feature/my-feature`

## Code Quality

- Follow [Coding Standards](../standards/README.md)
- Write tests for new features
- Maintain code coverage above 80%
- Run linters before committing

## Additional Resources

- **[Installation Guide](INSTALLATION.md)** - Complete installation instructions
- **[Mise Setup](MISE_SETUP.md)** - Tool version management with mise
- **[Feature Flags Setup](FEATURE_FLAGS_SETUP.md)** - License and feature configuration
- **[Packaging Guide](PACKAGING.md)** - Build and release process


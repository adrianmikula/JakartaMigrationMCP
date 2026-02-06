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

## Project Structure

```
src/
├── main/java/adrianmikula/jakartamigration/
│   ├── config/              # Configuration (feature flags)
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
- **[Feature Flags Setup](FEATURE_FLAGS_SETUP.md)** - Feature flags configuration
- **[Packaging Guide](PACKAGING.md)** - Build and release process


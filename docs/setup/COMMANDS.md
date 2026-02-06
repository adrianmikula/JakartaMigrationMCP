# Jakarta Migration MCP - Command Reference

This document provides a comprehensive reference for all development commands. All commands should be run via **mise** for cross-platform consistency.

## Quick Start

```bash
# List all available commands
mise task list

# Run the most common tasks
mise run build              # Compile the project
mise run test               # Run all tests
mise run run                # Start the application
mise run start-services     # Start Docker services
```

## Core Build Commands

### Compilation

| Command | Description | Cross-Platform |
|---------|-------------|----------------|
| `mise run build` | Compile Java sources | ✅ |
| `mise run rebuild` | Clean and rebuild | ✅ |
| `mise run clean` | Clean build artifacts | ✅ |

### Testing

| Command | Description | Cross-Platform |
|---------|-------------|----------------|
| `mise run test` | Run all tests | ✅ |
| `mise run test-coverage` | Run tests with Jacoco coverage | ✅ |
| `mise run test-mcp` | Run MCP integration tests | ✅ |
| `mise run test-ui` | Run IntelliJ UI tests | ✅ |

### Execution

| Command | Description | Cross-Platform |
|---------|-------------|----------------|
| `mise run run` | Run MCP server application | ✅ |
| `mise run run-mcp-server` | Run MCP server standalone | ✅ |

### Assembly & Release

| Command | Description | Cross-Platform |
|---------|-------------|----------------|
| `mise run assembly` | Build fat JAR | ✅ |
| `mise run release-jar` | Build release JAR | ✅ |
| `mise run release-all` | Build all release artifacts | ✅ |

## Subproject Commands

### migration-core Module

| Command | Description | Gradle Equivalent |
|---------|-------------|-------------------|
| `mise run build-migration-core` | Compile migration-core | `:migration-core:compileJava` |
| `mise run test-migration-core` | Test migration-core | `:migration-core:test` |
| `mise run build-migration-core-jar` | Build JAR | `:migration-core:jar` |

### mcp-server Module

| Command | Description | Gradle Equivalent |
|---------|-------------|-------------------|
| `mise run build-mcp-server` | Compile MCP server | `:mcp-server:compileJava` |
| `mise run test-mcp-server` | Test MCP server | `:mcp-server:test` |
| `mise run run-mcp-server` | Run MCP server | `:mcp-server:bootRun` |
| `mise run build-mcp-server-jar` | Build fat JAR | `:mcp-server:bootJar` |

### intellij-plugin Module

| Command | Description | Gradle Equivalent |
|---------|-------------|-------------------|
| `mise run build-intellij-plugin` | Build plugin | `:intellij-plugin:build` |
| `mise run test-intellij-plugin` | Test plugin | `:intellij-plugin:test` |
| `mise run build-intellij-plugin-zip` | Build plugin ZIP | `:intellij-plugin:buildPlugin` |
| `mise run intellij-patch-xml` | Patch plugin.xml | `:intellij-plugin:patchPluginXml` |

## Code Quality Commands

### All Quality Checks

```bash
mise run check                 # Run all checks
mise run validate-all          # Run all validations
```

### Individual Quality Tools

| Command | Description | Tool |
|---------|-------------|------|
| `mise run checkstyle` | Code style checks | Checkstyle |
| `mise run spotbugs` | Static analysis | SpotBugs |
| `mise run pmd` | Code analysis | PMD |
| `mise run format` | Auto-format code | Spotless |
| `mise run format-check` | Check formatting | Spotless |

### Validation

| Command | Description |
|---------|-------------|
| `mise run license-headers` | Validate license headers |
| `mise run validate-dependencies` | Validate dependency licenses |
| `mise run validate-boundaries` | Validate module boundaries |

## Docker & Services

### Service Management

| Command | Description |
|---------|-------------|
| `mise run start-services` | Start PostgreSQL & Redis |
| `mise run stop-services` | Stop Docker services |
| `mise run docker-ps` | Check service status |
| `mise run docker-logs` | View service logs (follow) |

### Database

| Command | Description |
|---------|-------------|
| `mise run db-connect` | Connect to PostgreSQL |
| `mise run redis-connect` | Connect to Redis CLI |
| `mise run db-migrate` | Run database migrations |

### Docker

| Command | Description |
|---------|-------------|
| `mise run docker-build` | Build Docker image |
| `mise run docker-run` | Run container |

## Health & Monitoring

| Command | Description | Endpoint |
|---------|-------------|----------|
| `mise run health` | Health check | `/actuator/health` |
| `mise run info` | App info | `/actuator/info` |
| `mise run metrics` | Metrics | `/actuator/metrics` |

## Frontend Development

| Command | Description |
|---------|-------------|
| `mise run frontend-install` | Install dependencies |
| `mise run frontend-dev` | Start dev server (Vite) |
| `mise run frontend-build` | Build for production |
| `mise run frontend-preview` | Preview production build |
| `mise run frontend-clean` | Clean build artifacts |

## Storybook

| Command | Description |
|---------|-------------|
| `mise run storybook` | Start Storybook server |
| `mise run storybook-build` | Build static Storybook |

## Premium Module Commands

**Requires**: Premium module sources with `-PpremiumEnabled=true`

| Command | Description |
|---------|-------------|
| `mise run build-premium` | Build premium engine |
| `mise run build-premium-all` | Build premium with tests |
| `mise run build-premium-intellij` | Build premium IntelliJ |
| `mise run build-all-modules` | Build all modules |

## Development Utilities

| Command | Description |
|---------|-------------|
| `mise run watch` | Hot reload development |
| `mise run shell` | Interactive shell |
| `mise run projects` | List projects |
| `mise run dependencies` | Dependency tree |
| `mise run properties` | Project properties |
| `mise run help` | Show help |

## Release Commands

| Command | Description |
|---------|-------------|
| `mise run release-jar` | Build release JAR |
| `mise run release-docker` | Build & tag Docker |
| `mise run release-all` | All release artifacts |

## Documentation

| Command | Description |
|---------|-------------|
| `mise run docs` | Generate docs |
| `mise run readme` | Generate README |

## Alternative: Direct Gradle Commands

If mise is not available, use these platform-appropriate commands:

### Windows

```powershell
.\gradlew.bat <task>
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat bootRun
.\gradlew.bat clean build
```

### Linux/macOS

```bash
./mill jakartaMigrationMcp.<task>
./mill jakartaMigrationMcp.compile
./mill jakartaMigrationMcp.test
./mill jakartaMigrationMcp.run
```

## Task Discovery

List all available tasks:

```bash
mise task list              # Short list
mise task list --verbose    # Detailed list
mise task list -T           # Tree view
```

Get task details:

```bash
mise run <task> --help      # Task help
```

## Configuration

The mise configuration is defined in [`.mise.toml`](.mise.toml). Key sections:

- `[tools]`: Tool versions (Java, Mill, Node)
- `[env]`: Environment variables
- `[tasks]`: All available tasks with descriptions

## Best Practices

1. **Always use mise run** for consistent, cross-platform commands
2. **Use module-specific tasks** for faster builds (e.g., `build-migration-core` instead of full `build`)
3. **Run validate-all** before committing to catch issues early
4. **Use test-coverage** to ensure adequate test coverage
5. **Run format** before committing to maintain code style

## Examples

### Daily Development Workflow

```bash
# Start services
mise run start-services

# Make code changes
# ...

# Build and test
mise run rebuild
mise run test

# Run the application
mise run run

# Check code quality
mise run format-check
mise run validate-all
```

### Release Workflow

```bash
# Full validation
mise run validate-all
mise run check

# Build all artifacts
mise run release-all

# Docker release
mise run release-docker
```

### CI/CD Pipeline

```bash
# Build
mise run build-all

# Test with coverage
mise run test-coverage

# Validate
mise run validate-all
```

# CLAUDE.md

This file provides guidance for Claude Code (and other AI assistants) when working with this codebase.

## Project Overview

Jakarta Migration MCP - A tool for analyzing and migrating Java applications from javax to jakarta namespace. The project includes:
- **migration-core**: Core analysis and scanning logic (Apache 2.0)
- **mcp-server**: MCP server with community tools (Apache 2.0)
- **intellij-plugin**: IntelliJ plugin (Proprietary)

## Build System

This project uses **Gradle** with **Mill** support on Linux/macOS and pure Gradle on Windows. All build commands MUST use **mise** for cross-platform consistency.

## Mandatory: Use mise Commands

**All AI assistants MUST prefer `mise run <task>` over direct `gradlew`, `mill`, or PowerShell commands.**

### Why mise?

- Cross-platform consistency (Mill on Linux/macOS, Gradle on Windows)
- Version pinning via `.mise.toml`
- Discoverable tasks with `mise task list`
- Abstracted implementation details

### Quick Reference

| Task | mise Command | Forbidden Direct Commands |
|------|--------------|---------------------------|
| Compile | `mise run build` | `./gradlew.bat compileJava`, `mill compile` |
| Test | `mise run test` | `./gradlew.bat test`, `mill test` |
| Run | `mise run run` | `./gradlew.bat bootRun`, `mill run` |
| Build JAR | `mise run assembly` | `./gradlew.bat bootJar`, `mill assembly` |
| Clean | `mise run clean` | `./gradlew.bat clean`, `mill clean` |
| Rebuild | `mise run rebuild` | `./gradlew.bat clean build` |

### Subproject Tasks

| Module | Task | mise Command |
|--------|------|--------------|
| migration-core | Compile | `mise run build-migration-core` |
| migration-core | Test | `mise run test-migration-core` |
| mcp-server | Compile | `mise run build-mcp-server` |
| mcp-server | Test | `mise run test-mcp-server` |
| mcp-server | Run | `mise run run-mcp-server` |
| intellij-plugin | Build | `mise run build-intellij-plugin` |
| intellij-plugin | Test | `mise run test-intellij-plugin` |

### Code Quality Tasks

| Task | mise Command |
|------|--------------|
| All checks | `mise run check` |
| Format code | `mise run format` |
| Check format | `mise run format-check` |
| Validate all | `mise run validate-all` |
| License headers | `mise run license-headers` |

### Docker & Services

| Task | mise Command |
|------|--------------|
| Start services | `mise run start-services` |
| Stop services | `mise run stop-services` |
| Docker status | `mise run docker-ps` |
| DB connect | `mise run db-connect` |

### Frontend Tasks

| Task | mise Command |
|------|--------------|
| Install deps | `mise run frontend-install` |
| Dev server | `mise run frontend-dev` |
| Build | `mise run frontend-build` |
| Storybook | `mise run storybook` |

### Task Discovery

```bash
mise task list              # List all tasks
mise task list --verbose   # Detailed list
mise run <task> --help     # Task help
```

### Fallback Commands (Only When mise is Unavailable)

**Windows:**
```powershell
.\gradlew.bat <task>
```

**Linux/macOS:**
```bash
./mill jakartaMigrationMcp.<task>
```

## Gradle Build Cache Configuration

This project is configured to use Gradle's **Configuration Cache** and **Build Cache** for faster builds.

### Configuration Cache

Enabled in `gradle.properties`:
```
org.gradle.configuration-cache=true
```

**Requirements for Tasks:**
- Tasks must be Gradle-compatible
- No configuration during execution
- No random/reorder-dependent configurations

### Build Cache

Enabled in `gradle.properties`:
```
org.gradle.caching=true
org.gradle.parallel=true
```

### Cache Commands

```bash
# Clean all caches
mise run clean
rm -rf ~/.gradle/caches

# Disable cache for debugging
./gradlew.bat build --no-build-cache
```

## Development Workflow

### Daily Development

```bash
# Start services
mise run start-services

# Make code changes

# Build and test
mise run rebuild
mise run test

# Run application
mise run run

# Check quality before commit
mise run format
mise run validate-all
```

### Release Workflow

```bash
# Full validation
mise run validate-all
mise run check

# Build release
mise run release-all

# Or step by step
mise run assembly
mise run frontend-build
mise run docker-build
```

## Project Structure

```
├── migration-core/        # Core analysis logic (Java 17)
├── mcp-server/            # MCP server (Spring Boot, Java 21)
├── intellij-plugin/       # IntelliJ plugin (Java 17)
├── frontend/              # Frontend (Node.js)
├── docs/                  # Documentation
├── config/               # Checkstyle, PMD, SpotBugs configs
└── .mise.toml            # Mise configuration
```

## Key Technologies

- **Java**: 17 (core, intellij-plugin), 21 (mcp-server)
- **Build**: Gradle 8.x, Mill 0.12.x
- **Framework**: Spring Boot 3.2.x
- **MCP**: Spring AI MCP Server
- **Database**: PostgreSQL, SQLite
- **Frontend**: Node.js 20, Vite
- **IDE**: IntelliJ Platform

## Testing

- Unit tests: JUnit 5, Mockito, AssertJ
- Integration tests: Spring Boot Test
- Code coverage: Jacoco

## Code Quality Tools

| Tool | Purpose | Config Location |
|------|---------|-----------------|
| Checkstyle | Code style | `config/checkstyle/` |
| PMD | Code analysis | `config/pmd/` |
| SpotBugs | Bug detection | `config/spotbugs/` |

## Validation Tasks

```bash
# License headers validation
mise run license-headers

# Dependency license validation
mise run validate-dependencies

# Module boundary validation
mise run validate-boundaries

# Run all validations
mise run validate-all
```

## Premium Features

Premium features require `-PpremiumEnabled=true`:

```bash
mise run build-premium
mise run build-premium-all
mise run build-all-modules
```

## Troubleshooting

### Build Issues

```bash
# Clean and rebuild
mise run rebuild

# Clear all caches
rm -rf .gradle build */build */.gradle

# Full clean
mise run clean
```

### IDE Sync Issues

```bash
# Regenerate IDE files
./gradlew.bat idea
./gradlew.bat eclipse
```

### Performance Issues

```bash
# Check build with profiling
./gradlew.bat build --profile

# Use parallel builds
./gradlew.bat build --parallel
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `REDIS_HOST` | Redis host |
| `REDIS_PORT` | Redis port |
| `OLLAMA_BASE_URL` | Ollama LLM endpoint |
| `OLLAMA_MODEL` | Ollama model name |
| `REPO_CLONE_PATH` | Repository clone path |

## Docker Services

This project uses Docker Compose for local development:

- **PostgreSQL**: `jakarta-migration-postgres` (port 5432)
- **Redis**: `jakarta-migration-redis` (port 6379)

### Start/Stop Services

```bash
mise run start-services   # Start all services
mise run stop-services   # Stop all services
mise run docker-ps       # Check status
mise run docker-logs      # View logs
```

## Useful Commands Reference

| Command | Description |
|---------|-------------|
| `mise run build` | Compile project |
| `mise run test` | Run all tests |
| `mise run run` | Run MCP server |
| `mise run assembly` | Build fat JAR |
| `mise run check` | All code quality checks |
| `mise run format` | Auto-format code |
| `mise run validate-all` | All validations |
| `mise run start-services` | Start Docker services |
| `mise run frontend-dev` | Start frontend dev server |
| `mise run storybook` | Start Storybook |
| `mise run release-all` | Build all release artifacts |

## Documentation

- Setup: `docs/setup/`
- Architecture: `docs/architecture/`
- Testing: `docs/testing/`
- Research: `docs/research/`
- Strategy: `docs/strategy/`
- Command reference: `docs/COMMANDS.md`

## Getting Help

- Check `mise task list` for available commands
- Check `docs/COMMANDS.md` for detailed command reference
- Check `docs/setup/` for setup instructions

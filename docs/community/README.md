# Community Documentation

This directory contains documentation for the **community modules** of the Jakarta Migration project.

## Overview

Community modules are open-source and licensed under **Apache License 2.0**. They provide the core functionality for analyzing and migrating Java applications from Java EE 8 (`javax.*`) to Jakarta EE 9+ (`jakarta.*`).

## Community Modules

| Module | Description | License |
|--------|-------------|---------|
| `community-core-engine` | Core migration logic, dependency analysis, and refactoring services | Apache 2.0 |
| `community-mcp-server` | Model Context Protocol server for AI assistant integration | Apache 2.0 |
| `community-intellij-plugin` | IntelliJ IDEA plugin with UI, MCP registration, and tool window | Apache 2.0 |

## Key Features

### Core Engine

- **Migration Analysis** - Analyzes projects for Jakarta EE migration readiness
- **Dependency Analysis** - Identifies dependencies and transitive conflicts
- **Code Refactoring** - Automated migration using OpenRewrite recipes
- **Runtime Verification** - Tests migrated applications

### MCP Server

- **AI Assistant Tools** - Provides MCP tools for AI coding assistants
- **Multiple Transports** - Supports STDIO, Streamable HTTP, and SSE
- **Tool Discovery** - Dynamic tool registration and discovery

### IntelliJ Plugin

- **Tool Window** - Dedicated Jakarta Migration panel
- **MCP Integration** - Automatic MCP server registration
- **Dependency Graph** - Visualizes module dependencies
- **AI Assistant** - Integration with IntelliJ AI Assistant

## Directory Structure

```
docs/community/
├── architecture/       - System architecture and design documents
├── deployment/         - Deployment guides
├── improvements/       - Implementation improvements
├── investigations/     - Research and investigation notes
├── licensing/          - Licensing documentation
├── mcp/                - MCP server documentation
├── requirements/       - Requirements specifications
├── research/           - Research notes
├── schemas/            - JSON schemas
├── setup/              - Setup and configuration guides
├── standards/          - Code standards and conventions
├── strategy/           - Strategic decisions
└── testing/            - Testing documentation
```

## License

All community modules are licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

## Contributing

Contributions to community modules are welcome. Please see the root [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

## Third-Party Dependencies

Community modules use the following major dependencies:

- **Spring AI** - MCP protocol implementation
- **OpenRewrite** - Code refactoring framework
- **ASM** - Bytecode analysis
- **SQLite JDBC** - Local data storage

All dependencies are compatible with Apache 2.0 licensing.

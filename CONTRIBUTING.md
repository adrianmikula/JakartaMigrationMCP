# Contributing to Jakarta Migration MCP

Thank you for your interest in contributing to the Jakarta Migration MCP project!

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [What to Contribute](#what-to-contribute)
- [How to Contribute](#how-to-contribute)
- [Community Features vs Premium Features](#community-features-vs-premium-features)
- [Licensing](#licensing)
- [Questions](#questions)

---

## Code of Conduct

This project adheres to the Apache Software Foundation's [Code of Conduct](https://www.apache.org/foundation/policies/conduct.html). By participating, you are expected to uphold this code.

---

## Getting Started

### Prerequisites

- Java 17 or later
- Gradle 8.x
- IntelliJ IDEA (recommended)

### Setup

```bash
# Clone the repository
git clone https://github.com/adrianmikula/jakarta-migration-mcp.git
cd jakarta-migration-mcp

# Build the project
./gradlew build

# Run tests
./gradlew test
```

---

## What to Contribute

### Community Features (Priority)

We welcome contributions to the **Community Edition** features:

- **Source Code Scanning**: Improved AST-based detection of `javax.*` usage
- **Dependency Analysis**: Better Maven/Gradle dependency parsing
- **Jakarta Mappings**: Additional `javax` → `jakarta` coordinate mappings
- **XML Scanning**: Enhanced detection in `persistence.xml`, `web.xml`, etc.
- **Documentation**: Improvements to guides and examples

### Premium Features

**Premium features are proprietary** and not open for external contributions:

- One-click refactoring
- Automatic code fixes
- Runtime verification
- Enterprise compliance reporting

If you have feature requests for premium features, please open a [feature request issue](https://github.com/adrianmikula/jakarta-migration-mcp/issues).

---

## How to Contribute

### 1. Find an Issue

Browse [good first issues](https://github.com/adrianmikula/jakarta-migration-mcp/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) for beginner-friendly tasks.

### 2. Fork the Repository

```bash
# Fork on GitHub, then clone your fork
git clone https://github.com/YOUR-USERNAME/jakarta-migration-mcp.git
cd jakarta-migration-mcp
```

### 3. Create a Branch

```bash
git checkout -b feature/your-feature-name
```

### 4. Make Changes

Follow the coding standards:
- Use checkstyle (`./gradlew checkstyleMain checkstyleTest`)
- Use spotbugs (`./gradlew spotbugsMain spotbugsTest`)
- Write tests for new functionality

### 5. Submit a Pull Request

1. Push your branch to your fork
2. Open a Pull Request against the `main` branch
3. Describe your changes and why they're needed
4. Link any related issues

---

## Community Features vs Premium Features

### Community Modules

Contributions are welcome for these modules:

| Module | License | Description |
|--------|---------|-------------|
| `migration-core` | Apache 2.0 | Core analysis and scanning logic |
| `mcp-server` | Apache 2.0 | MCP protocol server (community tools only) |
| `intellij-plugin` | Apache 2.0 | IntelliJ plugin (community features only) |

### Premium Modules

**Do not contribute to premium modules** - they are proprietary:

| Module | License | Description |
|--------|---------|-------------|
| `premium-engine` | Proprietary | Advanced refactoring and automation |
| `premium-intellij` | Proprietary | Premium IntelliJ features |

If you're unsure which module your contribution belongs to, ask in the PR description.

---

## Licensing

### Community Contributions

By contributing to community modules, you agree that your contributions will be licensed under the **Apache License 2.0**.

### Contributor License Agreement

For substantial contributions, we may ask you to sign a lightweight Contributor License Agreement (CLA) to clarify the intellectual property license granted to the project.

### Third-Party Dependencies

All third-party dependencies must be compatible with Apache License 2.0. Before adding a new dependency:

1. Verify the license is Apache 2.0 compatible
2. Check for security vulnerabilities (use `OWASP dependency-check`)
3. Add the dependency to `config/owasp/suppressions.xml` if needed

---

## Questions?

- **Issues**: [Open a GitHub issue](https://github.com/adrianmikula/jakarta-migration-mcp/issues)
- **Discussions**: [GitHub Discussions](https://github.com/adrianmikula/jakarta-migration-mcp/discussions)
- **Email**: adrian.mikula@example.com

---

## Recognition

Contributors are recognized in:
- The [`AUTHORS`](AUTHORS) file
- Release notes
- The GitHub contributors graph

Thank you for helping make Jakarta migration easier for everyone!

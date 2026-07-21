# CODEMAP

Package-to-module mapping for the JakartaMigrationMCP monorepo.

## Module: community-core-engine

| Package | Description |
|---------|-------------|
| `adrianmikula.jakartamigration.analysis.persistence` | Migration analysis data storage, schema management, and scanner type registry (SQLite, JSON persistence) |
| `adrianmikula.jakartamigration.coderefactoring.domain` | Code refactoring recipe models (definitions, execution history, categories, types) |
| `adrianmikula.jakartamigration.config` | Feature flags, licensing services, and marketplace license configuration |
| `adrianmikula.jakartamigration.dependencyanalysis.domain` | Dependency analysis domain models (artifacts, blockers, namespaces, conflict reports, risk assessments) |
| `adrianmikula.jakartamigration.dependencyanalysis.service` | Dependency graph building, Jakarta namespace mapping, japicmp compatibility checking, and Maven Central lookup |
| `adrianmikula.jakartamigration.dependencyanalysis.service.impl` | Implementations of dependency graph builders, namespace classifiers, and Jakarta mapping service |
| `adrianmikula.jakartamigration.sourcecodescanning.domain` | Source code scanning domain models (file usage, import statements, XML file usage, analysis results) |
| `adrianmikula.jakartamigration.sourcecodescanning.service` | Source code scanning service interface and implementations for analyzing project source files |
| `adrianmikula.jakartamigration.sourcecodescanning.service.impl` | Implementations of source code scanners (full and simplified) |
| `adrianmikula.jakartamigration.util` | Utility classes for project file system scanning, gitignore handling, and agent logging |

## Module: community-mcp-server

| Package | Description |
|---------|-------------|
| `adrianmikula.jakartamigration` | Spring Boot application entry point (JakartaMigrationMcpApplication) |
| `adrianmikula.jakartamigration.config` | Jakarta migration server configuration (JakartaMigrationConfig) |
| `adrianmikula.jakartamigration.mcp` | MCP server controllers, tool definitions, and tool configuration (SSE, Streamable HTTP endpoints, community migration tools) |
| `adrianmikula.jakartamigration.mcp.util` | JSON utilities for MCP tool response building |

## Module: premium-core-engine

| Package | Description |
|---------|-------------|
| `adrianmikula.jakartamigration.advancedscanning.domain` | Advanced scanning domain models (scan results, usage types, project-level results) |
| `adrianmikula.jakartamigration.advancedscanning.service` | Advanced scanner interfaces and base classes for deep migration analysis |
| `adrianmikula.jakartamigration.advancedscanning.service.impl` | Implementations of all advanced scanners (bean validation, CDI, JPA, servlet, REST, Docker, JMS, security, config) |
| `adrianmikula.jakartamigration.analytics.config` | Supabase analytics configuration |
| `adrianmikula.jakartamigration.analytics.model` | Analytics domain models (usage events, error reports) |
| `adrianmikula.jakartamigration.analytics.service` | Analytics services for usage tracking, error reporting, user identification, and Supabase integration |
| `adrianmikula.jakartamigration.analytics.util` | Environment detection utility for analytics context |
| `adrianmikula.jakartamigration.coderefactoring.service` | Code refactoring module and recipe service interfaces for OpenRewrite-based refactoring |
| `adrianmikula.jakartamigration.coderefactoring.service.impl` | Implementation of recipe service with OpenRewrite recipe execution |
| `adrianmikula.jakartamigration.coderefactoring.service.util` | Utility classes for isolated class loading, recipe seeding, and OpenRewrite recipe execution |
| `adrianmikula.jakartamigration.config` | Premium Jakarta migration configuration service |
| `adrianmikula.jakartamigration.credits` | Freemium credits management (credit types, credit service, freemium configuration) |
| `adrianmikula.jakartamigration.dependencyanalysis.config` | Dependency analysis configuration (compatibility config, Java packages, Maven artifacts) |
| `adrianmikula.jakartamigration.jaranalysis.classifier` | Bytecode-based namespace classifier for JAR compatibility analysis |
| `adrianmikula.jakartamigration.jaranalysis.config` | JAR scanning configuration |
| `adrianmikula.jakartamigration.jaranalysis.domain` | JAR analysis domain models (compatibility levels, reports, scan signals, classification results) |
| `adrianmikula.jakartamigration.jaranalysis.service` | JAR compatibility scanning services (bytecode/metadata signal extraction, scoring engine) |
| `adrianmikula.jakartamigration.pdfreporting.domain` | PDF report domain models (report sections, templates, validation errors/warnings) |
| `adrianmikula.jakartamigration.pdfreporting.service` | PDF report generation service interface |
| `adrianmikula.jakartamigration.pdfreporting.service.impl` | HTML-to-PDF report generation implementation (HTML generator, CSS cache, migration strategy provider) |
| `adrianmikula.jakartamigration.pdfreporting.snippet` | HTML snippet generators for PDF report sections |
| `adrianmikula.jakartamigration.pdfreporting.util` | HTML validation utility for PDF report output |
| `adrianmikula.jakartamigration.platforms.config` | Platform detection and risk scoring configuration loaders |
| `adrianmikula.jakartamigration.platforms.model` | Platform detection domain models |
| `adrianmikula.jakartamigration.platforms.service` | Simplified platform detection service |
| `adrianmikula.jakartamigration.preferences` | User preferences management service |
| `adrianmikula.jakartamigration.risk` | Risk scoring and enhanced test coverage analysis services |
| `adrianmikula.jakartamigration.runtimeverification.domain` | Runtime verification domain models |
| `adrianmikula.jakartamigration.runtimeverification.service` | Runtime verification services (bytecode analysis, error analysis, process execution) |
| `adrianmikula.jakartamigration.runtimeverification.service.impl` | ASM-based bytecode analyzer and runtime verification module implementation |
| `adrianmikula.jakartamigration.scraping` | Recipe discovery service for scanning and discovering migration recipes |
| `adrianmikula.jakartamigration.storage` | Centralized migration store and plugin storage service |

## Module: premium-intellij-plugin

| Package | Description |
|---------|-------------|
| `adrianmikula.jakartamigration.intellij` | IntelliJ plugin entry points (migration action, license check, MCP registration) |
| `adrianmikula.jakartamigration.intellij.config` | IntelliJ plugin feature flags and license failsafe configuration |
| `adrianmikula.jakartamigration.intellij.discovery` | Jakarta migration discoverability service for IDE integration |
| `adrianmikula.jakartamigration.intellij.license` | IntelliJ license checking, validation, and expiration notification |
| `adrianmikula.jakartamigration.intellij.mcp` | MCP client integration for IntelliJ (SSE client, tool registry, server provider) |
| `adrianmikula.jakartamigration.intellij.model` | IntelliJ plugin domain models (migration phases, dashboards, dependency summaries) |
| `adrianmikula.jakartamigration.intellij.service` | IntelliJ plugin services (migration analysis, advanced scanning, runtime verification) |
| `adrianmikula.jakartamigration.intellij.ui` | IntelliJ UI components (tool window, dashboard, dependency graph/tree, scan panels) |
| `adrianmikula.jakartamigration.intellij.ui.components` | Reusable UI widget components (gauges, truncation helpers, premium upgrade button) |
| `adrianmikula.jakartamigration.intellij.ui.tree` | Dependency tree UI components (tree nodes, custom renderer) |
| `adrianmikula.jakartamigration.intellij.util` | IntelliJ utility classes (notification helper, dev mode logger) |
| `adrianmikula.jakartamigration.reporting.domain` | Reporting domain models (report sections, comprehensive scan results) |
| `adrianmikula.jakartamigration.reporting.service` | Comprehensive report generation service for IntelliJ |

## Module: premium-mcp-server

| Package | Description |
|---------|-------------|
| `adrianmikula.jakartamigration.mcp` | Premium MCP server tool definitions (premium migration tools, premium experiment tools) |
| `adrianmikula.jakartamigration.mcp.util` | JSON response building utilities for premium MCP tools |

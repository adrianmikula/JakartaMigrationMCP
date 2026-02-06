# MCP Server Recommendations for Spring React Template

This document outlines Model Context Protocol (MCP) servers that will maximize developer effectiveness when using agentic AI for coding, enhance LLM understanding of the codebase and architecture, and integrate seamlessly with project tools (Gradle, npm, Vite, Spring).

## Overview

MCP (Model Context Protocol) is an open standard that enables AI systems to integrate with external tools, systems, and data sources. These servers will help AI assistants better understand and work with your Spring Boot + React project.

### Key Categories Covered

- **Design & Architecture**: Language-agnostic pseudocode and symbolic representations (UML, Logic-LM, diagrams)
- **Database Integration**: PostgreSQL management and operations
- **Frontend Tools**: React, Storybook, npm, Vite integration
- **Backend Tools**: Spring Boot, Gradle, JUnit, Liquibase
- **Build & Testing**: Build analysis, test patterns, performance monitoring

---

## 🎯 Top Priority MCP Servers

### 1. **Develocity MCP Server** ⭐⭐⭐
**Purpose**: Build data analysis and insights for Gradle, Maven, npm, and Python projects

**Key Features**:
- Investigate build issues with detailed exception information and stack traces
- Analyze test patterns (flaky tests, failure trends, performance)
- Monitor build performance (execution times, resource usage, caching effectiveness)
- Explore build data across projects, users, and time periods

**Why It's Essential**:
- Direct integration with Gradle (your build system)
- Provides AI with deep understanding of build failures and patterns
- Helps AI suggest fixes based on historical build data

**Resources**:
- Documentation: https://docs.gradle.com/develocity/mcp-server/
- Supports: Gradle, Maven, sbt, npm, Python projects

---

### 2. **Spring AI MCP Server** ⭐⭐⭐
**Purpose**: Native Spring integration for MCP servers

**Key Features**:
- Annotation-based approach (`@Tool` annotation)
- Automatic parameter conversion to MCP format
- Supports both stdio and HTTP-based SSE endpoints
- Seamless integration with Spring Boot applications

**Why It's Essential**:
- Built specifically for Spring ecosystem
- Allows your Spring application to expose functionality as MCP tools
- Perfect for creating custom MCP servers that understand your Spring architecture

**Resources**:
- Spring Blog: https://spring.io/blog/2025/05/20/spring-ai-1-0-GA-released
- Dependency: `spring-ai-starter-mcp-server`
- Examples: Spring Batch MCP Server, Spring Cloud Config MCP Server

---

### 3. **Spring Tools 5 Embedded MCP Server** ⭐⭐⭐
**Purpose**: Deep Spring project understanding for AI assistants

**Key Features**:
- Provides resolved classpath information
- Spring Boot version detection
- Bean definitions and component stereotypes
- Project structure analysis

**Why It's Essential**:
- Gives AI assistants precise information about your Spring project
- Helps AI understand bean relationships and dependencies
- Enables more accurate code suggestions and refactoring

**Resources**:
- Spring Blog: https://spring.io/blog/2025/12/04/towards-spring-tools-5-part3
- Integrated into Spring Tools 5 IDE

---

### 4. **Spring Initializr MCP** ⭐⭐
**Purpose**: AI-powered Spring Boot project generation

**Key Features**:
- Generate Spring Boot projects from natural language descriptions
- Configure dependencies, Java versions, and project structure
- Streamline project setup and scaffolding

**Why It's Useful**:
- Helps AI assistants create new Spring Boot modules or microservices
- Ensures consistent project structure
- Speeds up project initialization

**Resources**:
- Available at: https://antigravity.codes/mcp/spring-initializr

---

### 5. **NPM Plus MCP Server** ⭐⭐
**Purpose**: AI-powered JavaScript package management

**Key Features**:
- Security scanning for npm packages
- Bundle analysis
- Intelligent dependency management
- Integration with MCP-compatible environments

**Why It's Useful**:
- Essential for React frontend (npm-based)
- Helps AI understand and manage frontend dependencies
- Security scanning prevents vulnerable packages

**Resources**:
- Available at: https://antigravity.codes/mcp/npm-plus

---

## 🎨 Design & Architecture MCP Servers

### 6. **UML-MCP Server** ⭐⭐⭐
**Purpose**: Language-agnostic symbolic representations for design and architecture planning

**Key Features**:
- Generates UML diagrams (class, sequence, activity, use case) from natural language
- Supports PlantUML, Mermaid, and Kroki diagram formats
- Returns diagram code and shareable URLs (SVG, PNG, PDF)
- Language-independent design documentation
- Real-time bidirectional communication with AI assistants

**Why It's Essential**:
- Creates LLM-friendly, language-agnostic design references
- Independent of specific code implementation
- Perfect for architecture planning before coding
- Visual representations that AI can understand and reference

**Resources**:
- GitHub: https://github.com/antoinebou12/uml-mcp
- Supports: PlantUML, Mermaid, Kroki
- Compatible with: Claude Desktop, Cursor IDE

---

### 7. **Logic-LM MCP Server** ⭐⭐
**Purpose**: Symbolic reasoning and logical representations

**Key Features**:
- Symbolic reasoning using Logic-LM framework
- Answer Set Programming (ASP) integration
- Translates logical problems to ASP
- Verifies program logic and consistency
- Structured reasoning for design verification

**Why It's Useful**:
- Creates symbolic representations of system behavior
- Language-agnostic logical models
- Helps verify design decisions before implementation
- Useful for complex system interaction planning

**Resources**:
- PyPI: https://pypi.org/project/logic-lm-mcp-server/
- Framework: Logic-LM with Clingo

---

### 8. **AI Diagram Prototype Generator MCP Server** ⭐⭐
**Purpose**: AI-generated architecture diagrams and prototypes

**Key Features**:
- Generates architecture diagrams from natural language
- Creates flowcharts and mobile app prototypes
- Specialized prompt templates for system architecture
- Multiple AI provider support (ZhipuAI, OpenAI, Gemini)
- Visual design planning

**Why It's Useful**:
- Quick iteration on design concepts
- Visual communication of architecture
- LLM-friendly diagram generation
- Supports both system architecture and UI prototypes

**Resources**:
- Available at: https://playbooks.com/mcp/ai-diagram-prototype-generator
- Also: https://www.pulsemcp.com/servers/ai-diagram-prototype-generator

---

### 9. **Architect MCP Server** ⭐⭐
**Purpose**: Comprehensive architectural expertise and patterns

**Key Features**:
- Specialized architectural agents and tools
- Design templates and best practices
- Pattern catalogs (microservices, software architecture)
- Architecture evaluation and design generation
- Domain-specific architectural guidance

**Why It's Useful**:
- Access to architectural best practices
- Pattern-based design assistance
- Evaluation of existing architectures
- Language-agnostic architectural guidance

**Resources**:
- Available at: https://www.mcp.bar/server/squirrelogic/mcp-architect

---

### 10. **PRD Creator MCP Server** ⭐
**Purpose**: Product Requirements Document generation

**Key Features**:
- Generates comprehensive PRDs from descriptions
- User story integration
- Multiple AI provider support
- Industry-standard PRD templates
- Validates PRD completeness

**Why It's Useful**:
- Well-structured design documentation
- LLM-friendly requirement specifications
- Consistent documentation format
- Helps bridge design and implementation

**Resources**:
- Available at: https://www.mcp.pizza/mcp-server/PMtp/PRD-MCP-Server

---

### 11. **Mindmap MCP Server** ⭐
**Purpose**: Visual hierarchical planning and organization

**Key Features**:
- Converts Markdown to interactive mindmaps
- Uses `markmap-cli` library
- Visual hierarchical representation
- Planning and organization tool

**Why It's Useful**:
- Visual project planning
- Hierarchical design organization
- AI-friendly structured content
- Useful for brainstorming and architecture sessions

**Resources**:
- Available at: https://hexmos.com/freedevtools/mcp/content-creation/YuChenSSR--mindmap-mcp-server/

---

## 🗄️ Database & Tool Integration MCP Servers

### 12. **PostgreSQL MCP Server** ⭐⭐⭐
**Purpose**: Comprehensive PostgreSQL database management

**Key Features**:
- Safe, parameterized database operations
- Query execution and analysis
- Database migrations support
- Performance analysis and monitoring
- Comment and security policy management
- 17 intelligent tools for database operations

**Why It's Essential**:
- Direct integration with your PostgreSQL database
- AI assistants can safely interact with database
- Supports your Liquibase migration workflow
- Performance insights and optimization suggestions

**Resources**:
- Available at: https://www.mcpnow.io/en/server/postgresql-database-management-henkdz-postgresql-mcp-server
- Also: https://playbooks.com/mcp/henkdz-postgresql

---

### 13. **Storybook MCP Addon** ⭐⭐⭐
**Purpose**: React component development and testing with Storybook

**Key Features**:
- Runs MCP server within Storybook development environment
- Automatically generates example stories for UI components
- Visual verification and documentation
- Supports Vite-based Storybook setups
- Component state testing and documentation

**Why It's Essential**:
- Perfect for your React frontend
- Integrates with Vite (your build tool)
- Automates component story generation
- Enhances UI component development workflow

**Resources**:
- Storybook Addon: https://storybook.js.org/addons/@storybook/addon-mcp
- Supports: `@storybook/react-vite`, `@storybook/nextjs-vite`, `@storybook/sveltekit`

---

## 🔧 Additional Useful MCP Servers

### 14. **SystemPrompt Code Orchestrator** ⭐⭐
**Purpose**: Comprehensive AI coding agent orchestration

**Key Features**:
- Task management for AI agents
- Process execution
- Git integration (JGit compatible)
- Dynamic resource discovery
- TypeScript implementation with Docker support

**Why It's Useful**:
- Coordinates multiple AI agents working on your codebase
- Integrates with Git operations (you use JGit)
- Manages complex multi-step coding tasks

**Resources**:
- GitHub: https://github.com/systempromptio/systemprompt-code-orchestrator

---

### 15. **Stainless MCP Servers** ⭐
**Purpose**: Production-ready MCP servers from OpenAPI specs

**Key Features**:
- Auto-generates MCP servers from OpenAPI specifications
- OAuth flow support
- Client compatibility (Claude, OpenAI, Cursor)
- Multiple deployment options (NPM, Cloudflare Workers, Docker)

**Why It's Useful**:
- If you have REST APIs, automatically expose them as MCP tools
- Production-ready with proper authentication
- GitHub workflows for automatic regeneration

**Resources**:
- Website: https://www.stainless.com/products/mcp

---

### 16. **MCP-MultiServer-Interoperable-Agent2Agent** ⭐
**Purpose**: Multi-agent workflow orchestration

**Key Features**:
- Real-time, scalable multi-agent workflows
- LangGraph and LangChain integration
- Server-Sent Events (SSE) and STDIO support
- Asynchronous, concurrent tool execution

**Why It's Useful**:
- For complex agentic AI workflows
- Enables multiple AI agents to collaborate
- Cloud deployment ready

**Resources**:
- Available at: https://www.mcp.pizza/mcp-server/pJgD/MCP-MultiServer-Interoperable-Agent2Agent-LangGraph-AI-System

---

### 17. **OpenAPI MCP Server** ⭐
**Purpose**: Access any API with existing documentation

**Key Features**:
- Dockerized MCP server
- Works with any OpenAPI/Swagger documentation
- Enables AI agents to interact with APIs

**Why It's Useful**:
- If you have API documentation, expose it to AI
- Helps AI understand your API contracts
- Useful for integration testing and documentation

**Resources**:
- GitHub: ckanthony/openapi-mcp

---

### 18. **Code Execution MCP Servers** ⭐
**Purpose**: Secure code execution for AI agents

**Options**:
- **pydantic-ai/mcp-run-python**: Run Python code in secure sandbox
- **yepcode/mcp-server-js**: Execute JavaScript/Python with NPM and PyPI support

**Why It's Useful**:
- Allows AI to test code snippets safely
- Useful for validating suggestions before applying
- Supports both Python (backend utilities) and JavaScript (frontend)

**Resources**:
- Awesome MCP Servers: https://github.com/waanvar/awesome-mcp-servers

---

## 🔄 Jakarta Migration & Java Modernization MCP Servers

### 19. **ArchLift Java Modernization MCP Server** ⭐⭐⭐
**Purpose**: Comprehensive Java legacy modernization with OpenRewrite integration and Jakarta migration support

**Key Features**:
- AST-based code generation
- Template-driven generation
- Source code parsing (Java, JSP)
- Bytecode analysis
- **OpenRewrite integration** for automated refactoring
- Jakarta EE migration workflows (`javax.*` → `jakarta.*`)
- Spring Boot migration support
- Dependency analysis and coordinate resolution

**Why It's Essential for Jakarta Migration**:
- Direct OpenRewrite integration means you can leverage existing recipes
- Handles complex migration scenarios (XML configs, shaded dependencies)
- Bytecode analysis finds `javax` references in compiled JARs
- Provides migration blueprints and step-by-step plans

**Installation**:
```bash
npm install -g @archlift/remodern-java-mcp
# Or via Docker
docker pull phodal/remodern-java-mcp:latest
```

**Configuration**:
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
- **Perfect for**: Jakarta migration projects, legacy Java modernization

---

### 20. **Maven Tools MCP Server** ⭐⭐
**Purpose**: AI-powered Maven Central intelligence for dependency analysis and Jakarta coordinate resolution

**Key Features**:
- Instant dependency analysis for Maven, Gradle, SBT, Mill
- Bulk dependency operations
- Version intelligence and age analysis
- **Jakarta coordinate mapping** (finds `jakarta.*` equivalents for `javax.*` artifacts)
- Context7 integration for documentation
- Dependency conflict detection

**Why It's Useful for Jakarta Migration**:
- Quickly identifies which dependencies have Jakarta equivalents
- Resolves correct Jakarta coordinates (e.g., `javax.servlet:javax.servlet-api` → `jakarta.servlet:jakarta.servlet-api:6.0.0`)
- Analyzes transitive dependency hell common in Jakarta migrations

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
- **Perfect for**: Dependency analysis, coordinate resolution, version management

---

### 21. **Spring Web to MCP Converter** ⭐⭐
**Purpose**: Converts Spring Web REST APIs to MCP server tools using OpenRewrite recipes

**Key Features**:
- Automatic conversion of Spring REST APIs to MCP tools
- Uses OpenRewrite recipes for transformation
- Supports Spring Boot 3.2+ (Jakarta-compatible)
- Generates MCP tool definitions from `@RestController` annotations

**Why It's Useful**:
- If you're building Jakarta migration tools, you can expose them as MCP servers
- Leverages OpenRewrite for code transformation
- Native Spring Boot integration

**Installation**:
```bash
git clone https://github.com/addozhang/spring-rest-to-mcp.git
cd spring-rest-to-mcp
mvn clean install
```

**Resources**:
- GitHub: https://github.com/addozhang/spring-rest-to-mcp
- Website: https://lobehub.com/nl/mcp/addozhang-spring-rest-to-mcp
- **Perfect for**: Building custom Jakarta migration MCP servers

---

### OpenRewrite Integration Notes

**OpenRewrite** is a powerful automated refactoring engine that's essential for Jakarta migrations. While there isn't a dedicated "OpenRewrite MCP Server," you can:

1. **Use OpenRewrite as a Library**: Already integrated in `pom.xml` and `build.gradle.kts`
2. **Leverage ArchLift MCP**: Uses OpenRewrite under the hood
3. **Build Custom MCP Tools**: Use Spring AI MCP Server to expose OpenRewrite recipes as MCP tools

**Key OpenRewrite Recipes for Jakarta Migration**:
- `org.openrewrite.java.migrate.javax.AddJakartaNamespace` - Converts `javax.*` to `jakarta.*`
- `org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_2` - Spring Boot 3 migration (includes Jakarta)
- `org.openrewrite.java.migrate.UpgradeToJava21` - Java version upgrades

**Resources**:
- OpenRewrite Documentation: https://docs.openrewrite.org
- Jakarta Migration Recipes: https://docs.openrewrite.org/reference/recipes/java/migrate/javax
- Spring Boot Migration: https://docs.openrewrite.org/reference/recipes/java/spring/boot3

---

## 🧪 Testing & Migration Tools

### Note on JUnit, Liquibase, and mise

While dedicated MCP servers for **JUnit**, **Liquibase**, and **mise** were not found in the current ecosystem, here are alternative approaches:

#### JUnit Testing
- **Develocity MCP Server** can analyze JUnit test patterns and failures
- **Spring AI MCP Server** can be extended with custom `@Tool` annotations to expose test execution capabilities
- **SystemPrompt Code Orchestrator** can execute test commands and parse results
- Consider creating a custom MCP server using Spring AI to expose JUnit test execution as MCP tools

#### Liquibase Database Migrations
- **PostgreSQL MCP Server** supports database migrations and can work alongside Liquibase
- **Spring AI MCP Server** can be extended to expose Liquibase operations as MCP tools
- Liquibase changelog files can be analyzed by AI assistants through file reading capabilities
- Consider creating a custom MCP server that wraps Liquibase CLI commands

#### mise (Tool Version Manager)
- **SystemPrompt Code Orchestrator** can execute mise commands
- mise configuration files (`.mise.toml`) can be read and analyzed by AI assistants
- Consider creating a simple MCP server that exposes mise operations (install, use, list tools)
- Integration can be achieved through command execution MCP servers

**Recommendation**: Use **Spring AI MCP Server** to create custom tools for JUnit and Liquibase integration, as it provides the most native integration with your Spring Boot stack.

---

## 📋 Implementation Priority

### Phase 1: Core Integration (Start Here)
1. **Spring AI MCP Server** - Native Spring integration
2. **Spring Tools 5 Embedded MCP Server** - Project understanding
3. **Develocity MCP Server** - Build system integration
4. **PostgreSQL MCP Server** - Database management
5. **ArchLift Java Modernization MCP Server** - Jakarta migration & OpenRewrite ⭐ **ESSENTIAL FOR JAKARTA PROJECT**

### Phase 2: Design & Architecture Planning
5. **UML-MCP Server** - Language-agnostic design representations ⭐ **HIGH PRIORITY**
6. **Logic-LM MCP Server** - Symbolic reasoning for design verification
7. **AI Diagram Prototype Generator** - Architecture diagram generation

### Phase 3: Frontend & Package Management
8. **Storybook MCP Addon** - React component development ⭐ **HIGH PRIORITY**
9. **NPM Plus MCP Server** - Frontend dependency management
10. **Spring Initializr MCP** - Project scaffolding

### Phase 4: Advanced Orchestration & Custom Tools
11. **SystemPrompt Code Orchestrator** - Multi-agent coordination
12. **Architect MCP Server** - Architectural patterns and best practices
13. **Maven Tools MCP Server** - Dependency analysis and Jakarta coordinate resolution ⭐ **FOR JAKARTA PROJECT**
14. **Custom MCP Tools** - Extend Spring AI MCP Server for JUnit and Liquibase
15. **Spring Web to MCP Converter** - Expose Jakarta migration tools as MCP servers
16. **Stainless MCP Servers** - API integration (if applicable)

---

## 🔗 Additional Resources

- **Awesome MCP Servers Collection**: https://github.com/waanvar/awesome-mcp-servers
- **MCP Protocol Documentation**: https://modelcontextprotocol.io
- **Spring AI Documentation**: https://docs.spring.io/spring-ai/reference/

---

## 💡 Integration Tips

1. **Start Small**: Begin with Spring AI MCP Server and Spring Tools 5 to get immediate value
2. **Build Data**: Set up Develocity MCP Server early to start collecting build insights
3. **Custom Tools**: Use Spring AI MCP Server to create custom tools specific to your domain
4. **Security**: Ensure proper authentication and sandboxing for code execution servers
5. **Monitoring**: Track which MCP tools are most used to optimize your setup

---

## 🎯 Expected Benefits

- **Faster Development**: AI assistants understand your project structure and can make more accurate suggestions
- **Better Debugging**: Build data analysis helps identify patterns in failures
- **Consistent Architecture**: Spring Initializr ensures new modules follow best practices
- **Security**: NPM Plus helps prevent vulnerable dependencies
- **Documentation**: Better AI understanding leads to better code documentation and suggestions
- **Design Planning**: Language-agnostic design representations enable architecture planning before implementation
- **Database Management**: Safe, AI-assisted database operations and migrations
- **Component Development**: Automated Storybook story generation for React components
- **Symbolic Reasoning**: Logical verification of designs before coding

---

*Last Updated: Based on research from 2024-2025 MCP server ecosystem*


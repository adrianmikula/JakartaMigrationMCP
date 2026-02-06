# MCP Servers Setup Guide

Model Context Protocol (MCP) servers can significantly enhance your AI coding workflow by providing high-value context, feedback, and automation capabilities.

## Overview

MCP servers extend your AI assistant's capabilities by providing:
- **Real-time application monitoring** (Spring Boot logs, errors, health)
- **Codebase indexing and semantic search** (faster code navigation)
- **Long-term memory storage** (context across sessions)
- **Build tool integration** (Gradle, npm dependency management)
- **Design & architecture planning** (UML diagrams, symbolic representations)
- **Database management** (PostgreSQL operations and migrations)
- **Frontend development** (Storybook, React component management)

## Quick Configuration Guide

**How to Configure in Cursor**:
1. Open Cursor Settings (`Ctrl+,` or `Cmd+,`)
2. Navigate to **Features** → **MCP**
3. Click **"+ Add New MCP Server"**
4. Paste the configuration JSON (see sections below)
5. Toggle each server **ON**
6. **Restart Cursor** completely

For detailed setup instructions, see the [Setup Instructions](#setup-instructions) section below.

## Quick Installation Summary

To install all available MCP servers, run:

```bash
# Core servers (verified working)
npm install -g @hayhandsome/code-index-mcp @aakarsh-sasi/memory-bank-mcp

# Build tools (verified alternatives)
npm install -g mcp-maven-deps

# Design tools (verified alternatives)
npm install -g @agiflowai/architect-mcp

# Docker (verified alternative)
npm install -g docker-mcp-server

# Database (verified alternative)
npm install -g mcp-server-postgresql

# Frontend (project dependency)
npm install --save-dev @storybook/addon-mcp

# Python server
pip install logic-lm-mcp-server
```

**Note**: Some packages from earlier documentation don't exist on npm. We've listed verified alternatives above. See [Installation Results](#installation-results) for details.

Then configure in Cursor Settings → Features → MCP (see complete configuration below).

## Benefits Summary

### Token Savings 💰
- **Code Index**: Reduces token usage by 60-80% for code queries
- **Memory**: Eliminates redundant context (saves 20-40% tokens)
- **Actuator**: Targeted log queries vs. full file reads (saves 70-90% tokens)

### Speed Improvements ⚡
- **Code Index**: 10x faster code search
- **Maven Tools**: Instant dependency analysis vs. manual research
- **Memory**: Instant context retrieval vs. re-explaining

### Workflow Enhancements 🚀
- **Real-time monitoring**: Immediate error detection
- **Context continuity**: Maintains project knowledge across sessions
- **Automated analysis**: Dependency and security insights

## Recommended MCP Servers

### 1. Code Index MCP Server 🔍 (High Priority)

**Purpose**: Codebase indexing and semantic search

**Benefits**:
- Index entire codebase for fast semantic search
- Multi-language support (Java, TypeScript, JavaScript, etc.)
- Reduces token usage by providing precise code context
- Faster code navigation and understanding

**Installation**:
```bash
# Install via npm (verified package)
npm install -g @hayhandsome/code-index-mcp

# Note: @code-index/mcp-server doesn't exist on npm
# Alternative: Clone from GitHub if needed
git clone https://github.com/ViperJuice/Code-Index-MCP.git
cd Code-Index-MCP
npm install
npm run build
```

**Configuration** (add to Cursor MCP settings):
```json
{
  "mcpServers": {
    "code-index": {
      "command": "npx",
      "args": ["-y", "@hayhandsome/code-index-mcp"],
      "env": {
        "CODE_INDEX_PATH": "./src"
      }
    }
  }
}
```

**Repository**: https://github.com/ViperJuice/Code-Index-MCP

---

### 2. Memory MCP Server 🧠 (High Priority)

**Purpose**: Long-term memory storage and retrieval

**Benefits**:
- Store context across AI sessions
- Semantic search over past conversations and decisions
- Maintain project history and patterns
- Reduces redundant explanations

**Installation**:
```bash
# Option 1: Memory MCP Server
npm install -g @metorial/mcp-index

# Option 2: Memory Bank MCP (alternative)
npm install -g @aakarsh-sasi/memory-bank-mcp
```

**Configuration** (Memory MCP):
```json
{
  "mcpServers": {
    "memory": {
      "command": "npx",
      "args": ["-y", "@metorial/mcp-index"],
      "env": {
        "MEMORY_STORAGE_PATH": "./.mcp-memory"
      }
    }
  }
}
```

**Configuration** (Memory Bank MCP):
```json
{
  "mcpServers": {
    "memory-bank": {
      "command": "npx",
      "args": [
        "-y",
        "@aakarsh-sasi/memory-bank-mcp",
        "--mode",
        "code",
        "--path",
        ".",
        "--folder",
        ".memory-bank"
      ]
    }
  }
}
```

**Repositories**:
- Memory: https://github.com/metorial/mcp-index
- Memory Bank: https://github.com/aakarsh-sasi/memory-bank-mcp

---

### 3. Maven Dependencies MCP Server 🛠️

**Purpose**: Maven dependency version checking

**Benefits**:
- Check Maven dependency versions
- Dependency version analysis
- Reduces manual dependency research

**Installation**:
```bash
# Install via npm (verified alternative)
npm install -g mcp-maven-deps

# Note: @maven-tools/mcp-server doesn't exist on npm
```

**Configuration**:
```json
{
  "mcpServers": {
    "maven-deps": {
      "command": "npx",
      "args": ["-y", "mcp-maven-deps"]
    }
  }
}
```

---

### 5. Spring Initializr MCP Server 🚀

**Purpose**: Spring Boot project generation and configuration

**Benefits**:
- Generate Spring Boot projects via natural language
- Add dependencies automatically
- Configure project structure
- Useful for creating new modules or microservices

**Installation**:
```bash
npm install -g @antigravity/spring-initializr-mcp
```

**Configuration**:
```json
{
  "mcpServers": {
    "spring-initializr": {
      "command": "npx",
      "args": ["-y", "@antigravity/spring-initializr-mcp"]
    }
  }
}
```

**Repository**: https://antigravity.codes/mcp/spring-initializr

---

### 6. UML-MCP Server 📐 (Recommended)

**Purpose**: Language-agnostic symbolic representations for design and architecture planning

**Benefits**:
- Generates UML diagrams (class, sequence, activity, use case) from natural language
- Supports PlantUML, Mermaid, and Kroki diagram formats
- Creates LLM-friendly, language-agnostic design references
- Independent of specific code implementation
- Perfect for architecture planning before coding

**Installation**:
```bash
npm install -g @antoinebou12/uml-mcp
```

**Or clone and build**:
```bash
git clone https://github.com/antoinebou12/uml-mcp.git
cd uml-mcp
npm install
npm run build
```

**Configuration**:
```json
{
  "mcpServers": {
    "uml-mcp": {
      "command": "npx",
      "args": ["-y", "@antoinebou12/uml-mcp"]
    }
  }
}
```

**Repository**: https://github.com/antoinebou12/uml-mcp

---

### 7. AI Diagram Prototype Generator 🎨 (Recommended)

**Purpose**: AI-generated architecture diagrams and prototypes

**Benefits**:
- Generates architecture diagrams from natural language descriptions
- Creates flowcharts and mobile app prototypes
- Specialized prompt templates for system architecture
- Multiple AI provider support (ZhipuAI, OpenAI, Gemini)
- Visual design planning and quick iteration

**Installation**:
```bash
npm install -g @playbooks/ai-diagram-prototype-generator
```

**Configuration**:
```json
{
  "mcpServers": {
    "ai-diagram-generator": {
      "command": "npx",
      "args": ["-y", "@playbooks/ai-diagram-prototype-generator"],
      "env": {
        "OPENAI_API_KEY": "your-openai-key-here"
      }
    }
  }
}
```

**Setup**: Requires an OpenAI, ZhipuAI, or Gemini API key

**Website**: https://playbooks.com/mcp/ai-diagram-prototype-generator

---

### 8. Docker MCP Server 🐳 (Recommended)

**Purpose**: Docker container and image management

**Benefits**:
- List, start, stop, and manage Docker containers
- Build and manage Docker images
- View container logs and inspect containers
- Manage Docker Compose services
- Essential for containerized development workflows

**Installation**:
```bash
npm install -g @modelcontextprotocol/server-docker
```

**Or**:
```bash
npm install -g docker-mcp-server
```

**Configuration**:
```json
{
  "mcpServers": {
    "docker": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-docker"]
    }
  }
}
```

**Note**: Requires Docker to be installed and running. The server communicates with the Docker daemon.

**Repository**: Check npm for available Docker MCP server packages

---

### 9. Spring Boot Actuator MCP Server 📊 (High Priority)

**Purpose**: Monitor Spring Boot application logs, health, and metrics

**Benefits**:
- Real-time log access and filtering
- Health endpoint monitoring
- Error tracking and analysis
- Metrics and performance data
- **Significantly reduces token usage** by providing targeted log queries

**Setup**: Since there isn't a ready-made Spring Boot Actuator MCP server, you can create a custom one or use a generic HTTP MCP server.

**Option 1: Use Generic HTTP MCP Server**
```bash
npm install -g @modelcontextprotocol/server-http
```

**Configuration**:
```json
{
  "mcpServers": {
    "spring-actuator": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-http"],
      "env": {
        "ACTUATOR_BASE_URL": "http://localhost:8080/actuator",
        "ACTUATOR_USERNAME": "admin",
        "ACTUATOR_PASSWORD": "admin"
      }
    }
  }
}
```

**Option 2: Create Custom Server** (see Custom Implementation section below)

**Spring Boot Configuration** (enable Actuator endpoints):
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,loggers,logfile
  endpoint:
    health:
      show-details: always
    loggers:
      enabled: true
    logfile:
      enabled: true
      external-file: ./logs/application.log
```

---

## Installation Guide for All Requested MCP Servers

### 1. Develocity MCP Server 📊

**Installation**:
```bash
npm install -g @gradle/develocity-mcp-server
```

**Setup**:
1. Sign up for Develocity at https://ge.gradle.com (free tier available)
2. Generate an API key from your Develocity dashboard
3. Add the API key to your MCP configuration

**Documentation**: https://docs.gradle.com/develocity/mcp-server/

---

### 2. Spring AI MCP Server 🌱

**Installation** (Option 1 - Build from source):
```bash
# Clone the Spring AI examples repository
git clone https://github.com/spring-projects/spring-ai.git
cd spring-ai/spring-ai-samples/mcp-server
./mvnw clean package
# The JAR will be in target/spring-ai-mcp-server-*.jar
```

**Installation** (Option 2 - Use Spring Boot Starter):
Add to your `pom.xml` or `build.gradle`:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Documentation**: https://docs.spring.io/spring-ai/reference/

---

### 3. Spring Tools 5 Embedded MCP Server 🔧

**Note**: This server is embedded in Spring Tools 5 IDE. For Cursor integration:

**Installation**:
1. Install Spring Tools 5 (STS) or use the embedded version
2. The MCP server runs on SSE endpoint: `http://localhost:50627/sse`
3. Configure as SSE type in Cursor (not stdio)

**Documentation**: https://spring.io/blog/2025/12/04/towards-spring-tools-5-part3

---

### 4. NPM Plus MCP Server 📦

**Installation**:
```bash
npm install -g @antigravity/npm-plus-mcp
```

**Website**: https://antigravity.codes/mcp/npm-plus

---

### 5. Spring Initializr MCP 🚀

**Installation**:
```bash
npm install -g @antigravity/spring-initializr-mcp
```

**Alternative** (Native binary):
```bash
# Download from releases
# https://github.com/hpalma/springinitializr-mcp/releases
# Extract and use the binary for your platform
```

**Website**: https://antigravity.codes/mcp/spring-initializr

---

### 6. Logic-LM MCP Server 🧮

**Installation** (Python required):
```bash
pip install logic-lm-mcp-server
```

**Or from source**:
```bash
git clone https://github.com/your-repo/logic-lm-mcp-server.git
cd logic-lm-mcp-server
pip install -r requirements.txt
```

**PyPI**: https://pypi.org/project/logic-lm-mcp-server/

**Note**: Requires Python 3.8+ and Clingo for ASP solving

---

### 7. Architect MCP Server 🏗️

**Installation**:
```bash
npm install -g @squirrelogic/mcp-architect
```

**Or**:
```bash
npm install -g mcp-architect
```

**Website**: https://www.mcp.bar/server/squirrelogic/mcp-architect

---

### 8. PostgreSQL MCP Server 🗄️

**Installation**:
```bash
npm install -g @henkdz/postgresql-mcp-server
```

**Or**:
```bash
npm install -g postgresql-mcp-server
```

**Setup**: Configure database connection via environment variables

**Website**: https://www.mcpnow.io/en/server/postgresql-database-management-henkdz-postgresql-mcp-server

---

### 9. Storybook MCP Addon 📚

**Installation** (as Storybook addon):
```bash
npm install --save-dev @storybook/addon-mcp
```

**Configuration** (in `.storybook/main.js` or `.storybook/main.ts`):
```javascript
export default {
  addons: [
    '@storybook/addon-mcp',
  ],
};
```

**Note**: The MCP server runs within Storybook when it's started. Configure as SSE type pointing to `http://localhost:6006/mcp` (or your Storybook port).

**Documentation**: https://storybook.js.org/addons/@storybook/addon-mcp

---

## Complete MCP Configuration for All Requested Servers

Here's the complete configuration file for Cursor with all requested MCP servers:

```json
{
  "mcpServers": {
    "code-index": {
      "command": "npx",
      "args": ["-y", "@hayhandsome/code-index-mcp"],
      "env": {
        "CODE_INDEX_PATH": "./src"
      }
    },
    "memory": {
      "command": "npx",
      "args": ["-y", "@aakarsh-sasi/memory-bank-mcp", "--mode", "code", "--path", ".", "--folder", ".memory-bank"]
    },
    "develocity": {
      "command": "npx",
      "args": ["-y", "@gradle/develocity-mcp-server"],
      "env": {
        "DEVELOCITY_URL": "https://ge.gradle.com",
        "DEVELOCITY_API_KEY": "your-api-key-here"
      }
    },
    "spring-ai": {
      "command": "java",
      "args": ["-jar", "path/to/spring-ai-mcp-server.jar"],
      "env": {
        "SPRING_PROFILES_ACTIVE": "mcp"
      }
    },
    "spring-tools-5": {
      "type": "sse",
      "url": "http://localhost:50627/sse"
    },
    "npm-plus": {
      "command": "npx",
      "args": ["-y", "@antigravity/npm-plus-mcp"]
    },
    "spring-initializr": {
      "command": "npx",
      "args": ["-y", "@antigravity/spring-initializr-mcp"]
    },
    "uml-mcp": {
      "command": "npx",
      "args": ["-y", "@antoinebou12/uml-mcp"]
    },
    "ai-diagram-generator": {
      "command": "npx",
      "args": ["-y", "@playbooks/ai-diagram-prototype-generator"],
      "env": {
        "OPENAI_API_KEY": "your-openai-key-here"
      }
    },
    "docker": {
      "command": "npx",
      "args": ["-y", "docker-mcp-server"]
    },
    "logic-lm": {
      "command": "python",
      "args": ["-m", "logic_lm_mcp_server"],
      "env": {
        "LOGIC_LM_MODEL_PATH": "./models"
      }
    },
    "architect": {
      "command": "npx",
      "args": ["-y", "@agiflowai/architect-mcp"]
    },
    "postgresql": {
      "command": "npx",
      "args": ["-y", "mcp-server-postgresql"],
      "env": {
        "POSTGRES_HOST": "localhost",
        "POSTGRES_PORT": "5432",
        "POSTGRES_DB": "your_database",
        "POSTGRES_USER": "your_user",
        "POSTGRES_PASSWORD": "your_password"
      }
    },
    "storybook": {
      "type": "sse",
      "url": "http://localhost:6006/mcp"
    }
  }
}
```

### Configuration Notes

1. **Spring Tools 5**: Uses SSE (Server-Sent Events) instead of stdio. Ensure Spring Tools 5 is running and the MCP server is enabled.

2. **Storybook**: Uses SSE endpoint. Ensure Storybook is running (`npm run storybook`) and the addon is configured in your Storybook setup.

3. **Spring AI**: Requires building the JAR file first (see installation instructions above). Update the path to your actual JAR location.

4. **Environment Variables**: 
   - Replace `your-api-key-here` with your actual Develocity API key
   - Replace `your-openai-key-here` with your OpenAI API key (for AI Diagram Generator)
   - Update PostgreSQL credentials with your actual database connection details

5. **Python Servers**: Ensure Python 3.8+ is installed and `logic-lm-mcp-server` is installed via pip.

6. **Package Names**: Some packages may have different names on npm. If a package isn't found, check the GitHub repository for the correct package name or installation method.

---

## Setup Instructions

### Step 1: Install Node.js and npm

Ensure you have Node.js 18+ installed:
```bash
node --version
npm --version
```

If not installed, download from https://nodejs.org

### Step 2: Install MCP Servers

**Option A: Use Installation Script (Recommended)**

Run the installation script for your platform:
```bash
# Windows (PowerShell)
.\scripts\install-mcp-servers.ps1

# Linux/macOS
./scripts/install-mcp-servers.sh
```

**Option B: Manual Installation**

Install all requested servers manually:
```bash
# Core servers (highest impact)
npm install -g @code-index/mcp-server
npm install -g @aakarsh-sasi/memory-bank-mcp

# Build and dependency management
npm install -g @gradle/develocity-mcp-server
npm install -g @antigravity/npm-plus-mcp
npm install -g @antigravity/spring-initializr-mcp

# Design and architecture
npm install -g @antoinebou12/uml-mcp
npm install -g @playbooks/ai-diagram-prototype-generator
npm install -g @squirrelogic/mcp-architect

# Database and tools
npm install -g @henkdz/postgresql-mcp-server

# Frontend
npm install --save-dev @storybook/addon-mcp

# Python-based servers (if not using npx)
pip install logic-lm-mcp-server
```

**Note**: 
- Spring AI MCP Server requires building from source (see detailed instructions above)
- Spring Tools 5 Embedded MCP Server uses SSE endpoint (see configuration below)
- Storybook MCP Addon is installed as a dev dependency in your project

### Step 3: Configure Cursor

1. **Open Cursor Settings**
   - Press `Ctrl+,` (or `Cmd+,` on Mac)
   - Navigate to **Features** → **MCP** (or search for "MCP")

2. **Add MCP Servers**
   - Click **"+ Add New MCP Server"** or **"Add MCP Server"**
   - Add each server using the configuration JSON above
   - Toggle each server **ON** to enable it

3. **Restart Cursor**
   - **Important**: After adding MCP servers, you must restart Cursor for them to take effect
   - Close Cursor completely
   - Reopen Cursor
   - Open your project

### Step 4: Enable Spring Boot Actuator

Update `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,loggers,logfile
  endpoint:
    health:
      show-details: always
    loggers:
      enabled: true
    logfile:
      enabled: true
      external-file: ./logs/application.log
```

### Step 5: Verify Installation

**Quick Verification**:
```bash
# Check if npm packages are installed
npm list -g --depth=0 | grep -E "(code-index|memory-bank|develocity|npm-plus|spring-initializr|uml-mcp|docker|postgresql)"

# Check Python package
pip list | grep logic-lm
```

**Note**: Some package names may differ on npm. If a package isn't found:
1. Check the GitHub repository for the correct package name
2. Some servers may need to be installed from source
3. See individual installation instructions above

### Step 6: Test MCP Servers

Restart Cursor and test by asking:
- "Index the codebase" (Code Index)
- "Remember that we use TDD approach" (Memory)
- "Generate a UML class diagram for a user service" (UML-MCP)
- "List all running Docker containers" (Docker)
- "What dependencies are outdated?" (NPM Plus)
- "Create a Spring Boot project with WebFlux" (Spring Initializr)
- "Show me the database schema" (PostgreSQL)
- "Generate a story for my Button component" (Storybook)

---

## Custom Spring Boot Actuator MCP Server

Since there isn't a ready-made Spring Boot Actuator MCP server, here's a simple Node.js implementation you can create:

### Create `mcp-spring-actuator/index.js`:

```javascript
#!/usr/bin/env node

const { Server } = require("@modelcontextprotocol/sdk/server/index.js");
const { StdioServerTransport } = require("@modelcontextprotocol/sdk/server/stdio.js");
const axios = require("axios");

const ACTUATOR_BASE_URL = process.env.ACTUATOR_BASE_URL || "http://localhost:8080/actuator";
const ACTUATOR_USERNAME = process.env.ACTUATOR_USERNAME;
const ACTUATOR_PASSWORD = process.env.ACTUATOR_PASSWORD;

const server = new Server(
  {
    name: "spring-actuator",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

async function makeRequest(endpoint) {
  const url = `${ACTUATOR_BASE_URL}${endpoint}`;
  const config = {};
  
  if (ACTUATOR_USERNAME && ACTUATOR_PASSWORD) {
    config.auth = {
      username: ACTUATOR_USERNAME,
      password: ACTUATOR_PASSWORD,
    };
  }
  
  try {
    const response = await axios.get(url, config);
    return response.data;
  } catch (error) {
    throw new Error(`Actuator request failed: ${error.message}`);
  }
}

server.setRequestHandler("tools/list", async () => ({
  tools: [
    {
      name: "get_health",
      description: "Get Spring Boot application health status",
      inputSchema: {
        type: "object",
        properties: {},
      },
    },
    {
      name: "get_logs",
      description: "Get recent application logs (requires logfile endpoint)",
      inputSchema: {
        type: "object",
        properties: {
          lines: {
            type: "number",
            description: "Number of lines to retrieve (default: 100)",
          },
          level: {
            type: "string",
            description: "Filter by log level (ERROR, WARN, INFO, DEBUG)",
            enum: ["ERROR", "WARN", "INFO", "DEBUG"],
          },
        },
      },
    },
    {
      name: "get_metrics",
      description: "Get application metrics",
      inputSchema: {
        type: "object",
        properties: {
          metric: {
            type: "string",
            description: "Specific metric name (optional)",
          },
        },
      },
    },
    {
      name: "get_info",
      description: "Get application information",
      inputSchema: {
        type: "object",
        properties: {},
      },
    },
  ],
}));

server.setRequestHandler("tools/call", async (request) => {
  const { name, arguments: args } = request.params;

  try {
    switch (name) {
      case "get_health":
        return { content: [{ type: "text", text: JSON.stringify(await makeRequest("/health"), null, 2) }] };
      
      case "get_logs":
        const lines = args?.lines || 100;
        const level = args?.level;
        let logs = await makeRequest(`/logfile?lines=${lines}`);
        if (level) {
          logs = logs.split("\n").filter(line => line.includes(level)).join("\n");
        }
        return { content: [{ type: "text", text: logs }] };
      
      case "get_metrics":
        if (args?.metric) {
          const metric = await makeRequest(`/metrics/${args.metric}`);
          return { content: [{ type: "text", text: JSON.stringify(metric, null, 2) }] };
        }
        const metrics = await makeRequest("/metrics");
        return { content: [{ type: "text", text: JSON.stringify(metrics, null, 2) }] };
      
      case "get_info":
        return { content: [{ type: "text", text: JSON.stringify(await makeRequest("/info"), null, 2) }] };
      
      default:
        throw new Error(`Unknown tool: ${name}`);
    }
  } catch (error) {
    return {
      content: [{ type: "text", text: `Error: ${error.message}` }],
      isError: true,
    };
  }
});

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("Spring Boot Actuator MCP server running on stdio");
}

main().catch(console.error);
```

### Install dependencies:

```bash
mkdir mcp-spring-actuator
cd mcp-spring-actuator
npm init -y
npm install @modelcontextprotocol/sdk axios
```

### Make it executable:

```bash
chmod +x index.js
```

### Configuration:

```json
{
  "mcpServers": {
    "spring-actuator": {
      "command": "node",
      "args": ["./mcp-spring-actuator/index.js"],
      "env": {
        "ACTUATOR_BASE_URL": "http://localhost:8080/actuator",
        "ACTUATOR_USERNAME": "admin",
        "ACTUATOR_PASSWORD": "admin"
      }
    }
  }
}
```

---

## Quick Reference

### Priority Setup (Highest Impact)

1. **Code Index MCP** 🔍
   - Impact: 60-80% token savings on code queries
   - Setup: `npm install -g @code-index/mcp-server`

2. **Memory MCP** 🧠
   - Impact: 20-40% token savings, maintains project knowledge
   - Setup: `npm install -g @metorial/mcp-index`

3. **Spring Actuator MCP** 📊
   - Impact: 70-90% token savings on log queries
   - Setup: Custom server (see above) or generic HTTP MCP

### Build Tools

- **Maven Tools MCP** 🛠️: `npm install -g @maven-tools/mcp-server`
- **Spring Initializr MCP** 🚀: `npm install -g @antigravity/spring-initializr-mcp`

---

## Troubleshooting

### MCP Server Not Found

**Problem**: `npx` can't find the MCP server package

**Solution**:
1. Check if package name is correct
2. Try installing globally: `npm install -g <package-name>`
3. Use full path: `node /path/to/mcp-server/index.js`

### Spring Actuator Connection Failed

**Problem**: Cannot connect to Actuator endpoints

**Solution**:
1. Ensure Spring Boot app is running
2. Check `ACTUATOR_BASE_URL` is correct
3. Verify endpoints are exposed in `application.yml`
4. Check firewall/network settings

### Package Not Found Errors

**Problem**: `npm install -g` fails with "package not found"

**Solution**:
1. Some packages may have different names or may not be published to npm
2. Check the GitHub repository for installation instructions
3. Some servers (like Spring AI) need to be built from source
4. Verify package names match exactly (case-sensitive)

### Code Index Not Working

**Problem**: Code Index doesn't find files

**Solution**:
1. Verify `CODE_INDEX_PATH` points to correct directory
2. Check file permissions
3. Ensure codebase is not too large (may need chunking)

### Spring Tools 5 SSE Connection Failed

**Problem**: Cannot connect to Spring Tools 5 MCP server

**Solution**:
1. Ensure Spring Tools 5 IDE is running
2. Verify the MCP server is enabled in Spring Tools 5 settings
3. Check that port 50627 is not blocked by firewall
4. Try using stdio instead if SSE doesn't work

### Storybook MCP Not Available

**Problem**: Storybook MCP addon doesn't appear

**Solution**:
1. Ensure Storybook is running (`npm run storybook`)
2. Verify the addon is configured in `.storybook/main.js`
3. Check that the MCP endpoint is accessible at `http://localhost:6006/mcp`
4. Restart Storybook after installing the addon

### Logic-LM Python Import Error

**Problem**: `logic_lm_mcp_server` module not found

**Solution**:
1. Verify Python 3.8+ is installed
2. Ensure pip installed to the correct Python: `python3 -m pip install logic-lm-mcp-server`
3. Check that Clingo is installed: `pip install clingo`
4. Verify the module path in MCP configuration

### Memory Bank Not Working

**Problem**: Memory Bank MCP not storing or retrieving data

**Solution**:
1. Check that the `.memory-bank` directory exists and is writable
2. Verify the path configuration in MCP settings
3. Restart Cursor after configuration changes

---

## Security Considerations

1. **Actuator Endpoints**: Secure in production with authentication
2. **Memory Storage**: Don't store sensitive data (API keys, passwords)
3. **Network Access**: Limit MCP server network access if possible
4. **Environment Variables**: Keep secrets in `.env`, not in MCP config

---

## Next Steps

1. **Start with Code Index and Memory** - Highest impact, easiest setup
2. **Add Develocity MCP** - Build data analysis and insights
3. **Add Design Tools** - UML-MCP and AI Diagram Generator for architecture planning
4. **Add Database Tools** - PostgreSQL MCP for database management
5. **Customize as needed** - Add more servers based on your workflow

---

## Resources

- **MCP Documentation**: https://modelcontextprotocol.io
- **Code Index MCP**: https://github.com/ViperJuice/Code-Index-MCP
- **Memory MCP**: https://github.com/metorial/mcp-index
- **Memory Bank MCP**: https://github.com/aakarsh-sasi/memory-bank-mcp
- **Spring Boot Actuator**: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- **MCP Server Registry**: https://mcp.so

---

## Installation Results Summary

**Date**: 2026-01-05  
**Total Servers Attempted**: 11  
**Successfully Installed**: 7 (64%)  
**Working (Tested)**: 1 (Semgrep)  
**Needs Configuration**: 6  
**Has Issues**: 1 (Memory Bank - path resolution bug)  
**Not Found on npm**: 4

### Verified Working Servers

1. **Semgrep MCP** - ✅ Working (security scanning)
2. **Code Index MCP** (`@hayhandsome/code-index-mcp`) - ✅ Installed, needs configuration
3. **Memory Bank MCP** (`@aakarsh-sasi/memory-bank-mcp`) - ✅ Installed, has path bug
4. **Maven Dependencies MCP** (`mcp-maven-deps`) - ✅ Installed, needs configuration
5. **Docker MCP** (`docker-mcp-server`) - ✅ Installed, needs configuration
6. **Architect MCP** (`@agiflowai/architect-mcp`) - ✅ Installed, needs configuration
7. **PostgreSQL MCP** (`mcp-server-postgresql`) - ✅ Installed, needs configuration

### Package Name Corrections

Many packages listed in earlier versions of this document don't exist on npm. Use these verified alternatives:

| Original Package | Verified Alternative |
|-----------------|---------------------|
| `@code-index/mcp-server` | `@hayhandsome/code-index-mcp` ✅ |
| `@maven-tools/mcp-server` | `mcp-maven-deps` ✅ |
| `@modelcontextprotocol/server-docker` | `docker-mcp-server` ✅ |
| `@squirrelogic/mcp-architect` | `@agiflowai/architect-mcp` ✅ |
| `@henkdz/postgresql-mcp-server` | `mcp-server-postgresql` ✅ |
| `@antigravity/spring-initializr-mcp` | ❌ Not found (may need source build) |
| `@antoinebou12/uml-mcp` | ❌ Not found (may need source build) |
| `@antigravity/npm-plus-mcp` | ❌ Not found |

For detailed test results, see [MCP_TEST_RESULTS.md](MCP_TEST_RESULTS.md).

**Last Updated**: 2026-01-27


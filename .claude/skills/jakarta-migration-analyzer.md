---
description: Analyze and refactor a Java project for javax-to-Jakarta EE migration
---

# Jakarta Migration Analyzer

> Spec: `spec/mcp-integration.tsp`  
> Tools: `CommunityMigrationTools.java`, `JakartaMigrationTools.java`, `SentinelTools.java`, `PremiumMigrationTools.java`

Analyze and optionally auto-refactor any Java source repository to assess the risk, effort, and automated changes required to migrate from `javax.*` to Jakarta EE (`jakarta.*`).

## 0. Tool Discovery & Setup

Before doing anything else, verify that the Jakarta Migration MCP tools are available.

1. Try calling `scanForJavaxBasic` with a dummy path (e.g., `.`).
2. If the call fails or the tools are not found:
   - Ask the user whether the Jakarta Migration MCP server is running.
   - Provide setup guidance:
     - **Option A (from source)**: Build and run the community MCP server from this workspace:
       ```bash
       ./gradlew :community-mcp-server:bootJar
       java -jar community-mcp-server/build/libs/jakarta-migration-mcp.jar \
         --spring.main.web-application-type=none \
         --spring.profiles.active=mcp-stdio \
         --spring.ai.mcp.server.transport=stdio
       ```
     - **Option B**: Install popular free tools such as OpenRewrite (`org.openrewrite:rewrite-java`) or the Eclipse Transformer (`org.eclipse.transformer:transformer-maven-plugin`) to perform javax-to-jakarta transformations.
     - **Option C**: Install the IntelliJ plugin (premium) which bundles the MCP server and provides a GUI for migration tasks.
3. Do **not** proceed to the main menu until the tools are confirmed reachable.

## 1. Collect Project Path

Ask the user for the absolute path to the Java project they want to analyze. Validate that the directory exists and contains a `pom.xml`, `build.gradle(.kts)`, or `build.mill` file.

## 2. Present Menu

Present the following numbered options and ask the user to choose one (or type `exit` to quit):

| # | Option | Description | Tier |
|---|--------|-------------|------|
| 1 | **Quick scan** | Surface-level snapshot: Jakarta readiness score, top-level dependencies, and rough complexity estimate. | Community |
| 2 | **Deep scan** | Comprehensive analysis: source code imports, transitive dependencies, configuration files, and compatibility matrix. | Community |
| 3 | **Risk analysis** | Detect migration blockers, compute risk score, and list mitigation strategies. | Community |
| 4 | **Compare migration strategies** | Synthesize a side-by-side comparison of Big Bang, Incremental, and Dependencies-First approaches using existing analysis data. | Community |
| 5 | **List refactor recipes** | Show all available OpenRewrite recipes for the project, grouped by category (Annotations, Web, APIs, CDI, Database, Configuration, Security, Other). | Premium |
| 6 | **Apply refactor recipe** | Run an automated OpenRewrite recipe on the project source code. Prompt the user to confirm before applying destructive changes. | Premium |
| 7 | **View refactoring history** | List past recipe executions, their status, affected files, and whether they have been undone. | Premium |
| 8 | **Undo refactoring action** | Revert a previous recipe execution by its execution ID. | Premium |

After each action, return to this menu so the user can run another option or exit.

## 3. Available MCP Tools

### Community Tools (Free — Apache 2.0)

| Tool | Description |
|------|-------------|
| `scanForJavaxBasic` | Basic Jakarta EE usage scanning (source, dependencies, config). |
| `analyzeJakartaReadiness` | Overall readiness analysis with score and recommendations. |
| `detectBlockers` | Detect migration blockers with severity and mitigation strategies. |
| `recommendVersions` | Recommend Jakarta-compatible dependency versions. |
| `listDependenciesCompatibility` | Compatibility matrix for dependencies. |
| `check_env` | Sentinel tool to verify environment variables. |

### Premium Tools (Requires JetBrains Marketplace license)

| Tool | Description |
|------|-------------|
| `listRefactorRecipes` | Lists all available OpenRewrite recipes for a project with name, description, category, and run status. |
| `listRefactorRecipesByCategory` | Lists recipes filtered by a single category (e.g., `WEB`, `APIS`, `ANNOTATIONS`). |
| `applyRefactorRecipe` | Applies a named OpenRewrite recipe to the project source code. Returns execution ID, files changed, and success status. |
| `getRefactorHistory` | Returns execution history: recipe names, timestamps, affected files, success/failure, and undo status. |
| `undoRefactorRecipe` | Reverts a previous recipe execution by execution ID. |
| `createReport` | Generates a comprehensive PDF migration report. |
| `generateHtmlReport` | Generates an HTML migration report (`riskAnalysis`, `refactoringAction`, or `consolidated`). |
| `runAdvancedScan` | Deep technology-specific scan (JPA, Servlet, CDI, REST, SOAP, Security, Build, or `all`). |

> **Note:** When calling a premium tool without a valid license, the server returns an error. Explain that the feature requires a premium subscription and suggest community alternatives or installing the IntelliJ plugin for premium access.

## 4. Option Mappings

### 1. Quick scan
- Call `scanForJavaxBasic` with `scanTypes = "source,dependencies,config"`.
- Summarize the readiness score, total dependencies, and estimated complexity in 2–3 sentences.

### 2. Deep scan
- Call `analyzeJakartaReadiness` for the overall baseline.
- Call `listDependenciesCompatibility` for the compatibility matrix.
- Call `recommendVersions` for specific upgrade paths.
- Present a structured summary: readiness score, dependency breakdown, recommended upgrades, source-code findings, and config-file findings.

### 3. Risk analysis
- Call `detectBlockers`.
- Summarize the blocker count, severity, and top mitigation strategies. Highlight any **critical** or **high-confidence** blockers first.

### 4. Compare migration strategies
- If not already done in this session, run the **Deep scan** (options above) first to gather data.
- Synthesize a comparison table based on the analysis results:
  - **Big Bang**: single pass, highest risk, shortest calendar time. Good when blocker count and dependency count are low.
  - **Incremental**: module-by-module, lower risk, longer calendar time. Good for monoliths or large codebases.
  - **Dependencies-First**: update dependencies before touching source code. Good when most blockers are dependency-related.
- Recommend the best strategy with a 1-sentence justification.

### 5. List refactor recipes
- Call `listRefactorRecipes` with the project path.
- Group results by `category` (Annotations, Web, APIs, CDI, Database, Configuration, Security, Other).
- Show the recipe `name`, `description`, and `status` (`NEVER_RUN`, `RUN`, etc.).
- If the user wants to filter by category, call `listRefactorRecipesByCategory` with the uppercase category name.

### 6. Apply refactor recipe
- Call `listRefactorRecipes` first to show the user available recipes.
- Ask the user to confirm the recipe name they want to apply.
- Before executing, warn the user that this will modify source files and suggest they have the project under version control.
- Call `applyRefactorRecipe` with `projectPath` and `recipeName`.
- Summarize the result: success/failure, number of files changed, list of changed file paths, and the execution ID (needed for undo).

### 7. View refactoring history
- Call `getRefactorHistory` with the project path.
- Present a table of executions: ID, recipe name, timestamp, success/failure, number of affected files, and whether it has already been undone.
- Highlight any failed executions or un-undone changes.

### 8. Undo refactoring action
- Call `getRefactorHistory` first so the user can see available execution IDs.
- Ask the user for the execution ID to undo.
- Call `undoRefactorRecipe` with `projectPath` and `executionId`.
- Summarize the undo result: success/failure and which files were restored.

## 5. Error Handling

- If any MCP call returns an error JSON, parse it and present a human-readable message.
- If a premium tool is called without a valid license, explain that the feature requires a premium subscription and suggest alternatives (e.g., use community tools only, or install the IntelliJ plugin for premium access).
- Always return to the menu after handling an error so the user can try a different option.

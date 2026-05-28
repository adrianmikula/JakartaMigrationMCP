---
description: Analyze a Java project for javax-to-Jakarta EE migration risk and effort
---

# Jakarta Migration Analyzer

> Spec: `spec/mcp-integration.tsp`
> Tools: `CommunityMigrationTools.java`, `PremiumMigrationTools.java`

Analyze any Java source repository to assess the risk and effort required to migrate from `javax.*` to Jakarta EE (`jakarta.*`).

## 0. Tool Discovery & Setup

Before doing anything else, verify that the Jakarta Migration MCP tools are available.

1. Try calling `scanForJavaxBasic` with a dummy path (e.g., `.`).
2. If the call fails or the tools are not found:
   - Ask the user whether the Jakarta Migration MCP server is running.
   - Provide setup guidance:
     - **Option A**: Install the Jakarta Migration MCP server (community or premium edition).
     - **Option B**: Install popular free tools such as OpenRewrite (e.g., `org.openrewrite:rewrite-java`) or the Eclipse Transformer (e.g., `org.eclipse.transformer:transformer-maven-plugin`) to perform javax-to-jakarta transformations.
     - **Option C**: Install the IntelliJ plugin (premium) which bundles the MCP server and provides a GUI for migration tasks.
3. Do **not** proceed to the main menu until the tools are confirmed reachable.

## 1. Collect Project Path

Ask the user for the absolute path to the Java project they want to analyze. Validate that the directory exists and contains a `pom.xml`, `build.gradle(.kts)`, or `build.mill` file.

## 2. Present Menu

Present the following numbered options and ask the user to choose one (or type `exit` to quit):

| # | Option | Description |
|---|--------|-------------|
| 1 | **Quick scan** | Surface-level snapshot: Jakarta readiness score, top-level dependencies, and rough complexity estimate. |
| 2 | **Deep scan** | Comprehensive analysis: source code imports, transitive dependencies, configuration files, and compatibility matrix. |
| 3 | **Risk analysis** | Detect migration blockers, compute risk score, and list mitigation strategies. |
| 4 | **Print reports** | Generate a comprehensive migration report (PDF) saved into the project's `reports/` directory. |
| 5 | **Compare migration strategies** | Synthesize a side-by-side comparison of Big Bang, Incremental, and Dependencies-First approaches using existing analysis data. |
| 6 | **Automated refactor** | List available refactor recipes, let the user pick one, and apply it to the project. |
| 7 | **Undo** | Show refactor execution history and allow undoing a previous execution. |

After each action, return to this menu so the user can run another option or exit.

## 3. Option Mappings

### 1. Quick scan
- Call `scanForJavaxBasic` with `scanTypes = "source,dependencies,config"`.
- Summarize the readiness score, total dependencies, and estimated complexity in 2-3 sentences.

### 2. Deep scan
- Call `analyzeJakartaReadiness` for the overall baseline.
- Call `listDependenciesCompatibility` for the compatibility matrix.
- Call `recommendVersions` for specific upgrade paths.
- Present a structured summary: readiness score, dependency breakdown, recommended upgrades, source-code findings, and config-file findings.

### 3. Risk analysis
- Call `detectBlockers`.
- Summarize the blocker count, severity, and top mitigation strategies. Highlight any **critical** or **high-confidence** blockers first.

### 4. Print reports
- Call `createReport`.
- Confirm the report file path and offer to display a brief summary of what was included.

### 5. Compare migration strategies
- If not already done in this session, run the **Deep scan** (options above) first to gather data.
- Synthesize a comparison table based on the analysis results:
  - **Big Bang**: single pass, highest risk, shortest calendar time. Good when blocker count and dependency count are low.
  - **Incremental**: module-by-module, lower risk, longer calendar time. Good for monoliths or large codebases.
  - **Dependencies-First**: update dependencies before touching source code. Good when most blockers are dependency-related.
- Recommend the best strategy with a 1-sentence justification.

### 6. Automated refactor
- Call `listRefactorRecipes` to show available recipes.
- Ask the user which recipe to apply (by name).
- Confirm before applying.
- Call `applyRefactorRecipe` with the chosen recipe name.
- Report the result: files changed, success/failure, and any error messages.

### 7. Undo
- Call `getRefactorHistory` to show past executions.
- Ask the user which execution to undo (by ID).
- Call `undoRefactorRecipe` with the chosen execution ID.
- Report the undo result.

## 4. Error Handling

- If any MCP call returns an error JSON, parse it and present a human-readable message.
- If a premium tool is called without a valid license, explain that the feature requires a premium subscription and suggest alternatives (e.g., use community tools only, or install the IntelliJ plugin for premium access).
- Always return to the menu after handling an error so the user can try a different option.


# AI Agent Rules 

## Command Catalogue
See `COMMANDS.md` in the project root for all build, test, lint, run, and debug commands with expected execution times. Agents MUST check this file before guessing CLI syntax.

## Known Issues
See `docs/troubleshooting/COMMON_ISSUES.md` for catalogued error patterns and their resolutions. Always check there first when encountering errors — do not re-investigate known problems.

## Agent Setup
- Set up any useful MCP servers which will significantly speed up agent context, simplify workflows, and speed up feedback loops.
- Set up agent allowlist to include all common non-destructive development commands we will want to use, e.g. for build tools, test commands, etc.

### Available MCP Servers
MCP servers are configured per-agent for compatibility:

| Server | Root `.mcp.json` | `opencode.jsonc` | `.cursor/mcp.json` | `.kilocode/mcp.json` | `.devin/config.json` |
|--------|:-:|:-:|:-:|:-:|:-:|
| gradle | yes | yes | yes | yes | yes |
| github | yes | yes | yes | yes | yes |
| filesystem | yes | yes | yes | yes | yes |
| sequentialthinking | yes | yes | yes | yes | yes |
| memory | yes | yes | yes | yes | yes |
| postgres | yes | yes | yes | yes | yes |
| codebase-indexer | yes | yes | — | — | yes |

To add MCP servers for a specific agent, edit the corresponding file above.
Windsurf uses the root `.mcp.json` and `.windsurfrules` for instructions.


## Debugging Runtime Errors

When encountering a runtime error, follow this workflow:
1. **Prerequisite check** — Verify the dev server or service is running (`mise run docker-ps`, `mise run health`)
2. **Diagnose** — Use MCP tools if available, or check logs directly:
   - Enable debug mode: `LOG_LEVEL=debug mise run run`
   - Check Docker logs: `mise run docker-logs`
   - Review known issues: `docs/troubleshooting/COMMON_ISSUES.md`
3. **Analyze** — Identify the root cause from log output
4. **Fix** — Apply the fix
5. **Verify** — Run the fast test loop (`mise run fast-test`) to confirm

## CI/CD Workflow — Signal vs Confidence

Split every pipeline into two tiers:
- **Signal** (inner loop, <30s): lint → typecheck → fast tests — run before every commit
- **Confidence** (CI only, minutes): integration → security → full matrix → deploy — run async in CI

Validate pipeline config before pushing: `gh workflow run --dry-run`

## Spec-Driven Development Workflow

When starting a new feature:
1. Write a spec file at `docs/spec/<feature>.md` describing expected behavior
2. Implement code matching the spec
3. Verify alignment — spec first, code second

For data models, use the TypeSpec definitions in `spec/` as the source of truth.

## Tasks

### Pre-Task Steps
- Check the codebase and index/codemap for existing implementations to avoid duplicating the same functionality (DRY principle) 
- review specifications under root level spec folder to understand existing implementation
- Check the codebase for existing tests to avoid duplicating the same tests (DRY principle)
- Check the licensing structure so we know the correct code module to put the new code in.



### Task Rules
- Complete all tasks in a task list in order
- Always use SDD (spec driven development) to implement new features, with specifications located under docs/spec
- Always use TDD (test driven development) to implement new features, with tests located in the same module as the code they test
- ensure all requirements are implemented
- ensure all new features have tests


### Post-Task Steps

After completing a task list, do the following:
- ensure all compile errors are fixed: `./gradlew :community-core-engine:compileJava`
- Add any missing tests for important/critical code paths
- ensure all tests pass. Use `./gradlew :community-core-engine:fastTest` for quick signal, `mise run test` for full confidence
- Review the code implementation to ensure it meets our code quality standards
- Update documentation under the docs folder to provide details of features and architecture
- update specifications under root level spec folder to reflect changes
- Run `mise run build` to verify compilation before concluding




## Architecture

### Architectural decisions
- Architectural decisions should never be made solely by AI.  AI can recommend arcitectural options, but these should always be approved by a human before being implemented.
- All architectural decisions should be documented via ADR (architectural decision records) under docs/adr 
- When changing an existing architectural design, always check the ADR first to understand the reasoning behind the original architectural decisions.


### Architectural patterns
- New features should follow existing architectural patterns rather than creating new patterns, where possible.

Full architectural rules are documented in AgentRules\ARCHITECTURE.md



## Code Quality

### Best Practices

- KISS. Source files should be kept under 500 lines, and split up if they get too large.
- DRY. Check for and re-use existing code wherever possible.
- Use 2026 industry best-practices for high-quality software development.  OOP, SOLID, etc.
- Avoid hard coding string constants in code which should be loaded from configuration (YAML, JSON, properties, or env vars).

Full coding standards are documented in AgentRules\CODING.md


### Simplicity and Consistency
- remove duplication and unused files, methods or abstractions
- make the code 50% shorter/simpler if possible
- don't over-complicate or over-engineer something simple.
- Use pre-existing conventions/patterns 
- Don't add fallback logic if it's not part of the requirements.

Full simplicity guidelines are documented in docs/patterns/simplicity_and_consistency.md


### Automated Testing

- Use the fast test loop for quick feedback during development
- Always use TDD (test driven development) to implement new features
- Set up code coverage tracking for all modules
- Minimum 50% code coverage, with unit tests as a minimum requirement for all features.
- Add a small number of integration and performance tests
- Projects should configure a subset of unit tests as 'fast tests' for fast agentic AI feeback.

Full testing standards are documented in AgentRules\TESTING.md and docs/FAST_TEST_LOOP.md


### Performance

- Use try/catch with resources, especially inside loops.
- Use streaming rather than loading everything into memory at once when possible.
- Avoid manually forcing GC calls inside our code. 
- if loading large DB datasets into memory, use cursors or paging where possible

Full memory efficiency patterns are documented in docs/patterns/memory_efficiency.md

- When invoking external build tools (Gradle, Maven) from Java code, follow the standards in docs/patterns/build-tool-invocation.md — especially regarding resolvable configurations, wrapper detection, and build file discovery.



## Debugging

- Solutions to common code issues or persistent problems should be documented in docs/troubleshooting/COMMON_ISSUES.md
- When debugging persistent problems/errors, always check the list of known issues in docs/troubleshooting/COMMON_ISSUES.md
- Don't report that a bug is fixed based on a guess, assumption, or hunch. Always prove/test/verify that your solution actually fixed the problem
- If a specific bug never gets fixed, even though the AI agent keeps trying different fixes and incorrectly reporting that the bug was successfully fixed, then change approach:
  1. Step back to look at the bigger picture
  2. Optimise the problematic part of the code for Simplicity and Consistency (see docs/patterns/simplicity_and_consistency.md)
  3. As a last resort, consider deleting and completely re-implementing the feature
- Use the MCP server debugging flow: get_errors → get_logs → analyze → fix → verify
- All logging must use SLF4J (`log.info`, `log.debug`, etc.) — never `System.out.println` in production code
- Respect `LOG_LEVEL` env var: debug, info, warn, error




## Licensing
- Community modules/classes should never reference premium modules/classes (strict open-core licensing structure).
- All new UI features should be added to the premium-intellij and premium-core modules by default, unless otherwise specified.
- All premium features should be feature-flagged with a premium feature flag, and all community features should have no feature flag (always enabled)




## Velocity

- Prefer using commands from the mise-en-place catalogue or the IDE's whitelist. Avoid using commands on the IDE's blacklist.
- Agentic coding AIs should default to running the 'fast tests' subset for faster feedback while working.
- Agents should have relevant/useful MCP servers installed to speed up coding workflows.
- We should run build/test commands using a fast-start JVM like Graal or CRAK to improve agent feedback time.

### Preferred Agent Iteration Commands

Use these commands for the fastest feedback loop:

| Step | Command | Time |
|------|---------|------|
| Compile check | `./gradlew :community-core-engine:compileJava` | ~0.8s |
| Fast tests (single module) | `./gradlew :community-core-engine:fastTest` | ~0.76s |
| Fast tests (all modules) | `mise run fast-test` | ~2.0s (cold) / ~1.9s (warm) |
| Direct Gradle (all modules) | `./gradlew :community-core-engine:fastTest :premium-core-engine:fastTest :premium-experiment-engine:fastTest :community-mcp-server:fastTest :premium-mcp-server:fastTest --configure-on-demand` | ~2.0s (cold) / ~1.9s (warm) |

See `COMMANDS.md` for full command catalogue.
Full efficiency tweaks are documented in AgentRules/EFFICIENCY.md and docs/ai/fast-test-loop.md


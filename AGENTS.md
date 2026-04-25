
# AI Agent Rules 



## Agent Setup
- Set up any useful MCP servers which will significantly speed up agent context, simplify workflows, and speed up feedback loops.
- Set up agent allowlist to include all common non-destructive development commands we will want to use, e.g. for build tools, test commands, etc.


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
- ensure all compile errors are fixed
- Add any missing tests for important/critical code paths
- ensure all tests pass.  
- Review the code implementation to ensure it meets our code quality standards.
- Update documentation under the docs folder to provide details of features and architecture.  
- update specifications under root level spec folder to reflect changes
- Add code comments to mention the requirements and specifications in the source code and the test code.




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

Full simplicity guidelines are documented in docs\standards\simplicity_and_consistency.md


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



## Debugging

- Solutions to common code issues or persistent problems should be documented in docs/COMMON_ISSUES.md
- When debugging persistent problems/errors, always check the list of known issues in docs/COMMON_ISSUES.md
- Don't report that a bug is fixed based on a guess, assumption, or hunch. Always prove/test/verify that your solution actually fixed the problem. 
- If a specific bug never gets fixed, even though the AI agent keeps trying different fixes and incorrectly reporting that the bug was successfully fixed, then we need to change our approach. Try the following:
1. step back to look at the bigger picture
2. Optimise the problematic part of the code for Simplicity and Consistency, following the guidelines in docs\standards\simplicity_and_consistency.md 
3. As a last resort, consider deleting and completely re-implementing the feature.




## Licensing
- Community modules/classes should never reference premium modules/classes (strict open-core licensing structure).
- All new UI features should be added to the premium-intellij and premium-core modules by default, unless otherwise specified.
- All premium features should be feature-flagged with a premium feature flag, and all community features should have no feature flag (always enabled)




## Velocity

- Prefer using commands from the mise-en-place catalogue or the IDE's whitelist. Avoid using commands on the IDE's blacklist. 
- Agentic coding AIs should default to running the 'fast tests' subset for faster feeback while working.
- Agents should have relevant/useful MCP servers installed to speed up coding workflows.
- We should run build/test commands using a fast-start JVM like Graal or CRAK to improve agent feedback time.

Full efficiency tweaks are documented in AgentRules\EFFICIENCy.md and docs/FAST_TEST_LOOP.md


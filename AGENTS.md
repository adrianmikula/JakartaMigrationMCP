
# AI Agent Rules 

## Tasks

### Pre-Task Steps
- Check the codebase for existing implementations to avoid duplicating the same functionality (DRY principle) 
- Check the codebase for existing tests to avoid duplicating the same tests (DRY principle)
- Check the licensing structure so we know the correct code module to put the new code in.



### Task Rules
- Complete all tasks in a task list in order
- Always use SDD (spec driven development) to implement new features, with specifications located under docs/spec
- Always use TDD (test driven development) to implement new features, with tests located in the same module as the code they test



### Post-Task Steps

After completing a task list, do the following:
- ensure all requirements are implemented
- ensure all compile errors are fixed
- ensure all new features have tests
- Add any missing tests for important/critical code paths
- ensure all tests pass.  
- Review the code implementation to ensure it meets our code quality standards.
- Update documentation under the docs folder to provide details of features and architecture.  
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

Full coding standards are documented in AgentRules\CODING.md

### Automated Testing

- Always use TDD (test driven development) to implement new features
- Set up code coverage tracking for all modules
- Minimum 50% code coverage, with unit tests as a minimum requirement for all features.
- Projects should configure a subset of unit tests as 'fast tests' for fast agentic AI feeback.

Full testing standards are documented in AgentRules\TESTING.md


## Debugging

- Solutions to common code issues or persistent problems should be documented in docs/COMMON_ISSUES.md
- When debugging persistent problems/errors, always check the list of known issues in docs/COMMON_ISSUES.md



## Licensing




## Efficiency

- Agentic coding AIs should default to running the 'fast tests' subset for faster feeback while working.



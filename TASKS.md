Lets complete the following tasks in order. While implementing the tasks, always ensure we follow the coding guidelines defined in AGENTS.md





# bug fixes

the reports tab has disappeared. restore it.

the experimental feature tabs are not dynamically displayed after enabling experimental features from the support UI tab. Fix this.

after clicking 'analyse project' in the  platforms tab, it always reports "No application servers detected" even when I've opened an example repo which I know contains an appserver. Soemthing is wrong with how we are scanning for appservers. Start by checking that our platform integration tests testing with real github projects from examples.yaml, and that the tests are passing  





# performance

lets load the example repo for the performance tests from the 'project_complexity' section in examples.yaml. project_complexity is already present in examples.yaml, and it's spelt correctly. for some reason the AI agent keeps thinking it's not there or misspelt. Look into other reasons why its not loading

it's bad practice to manually force GC calls inside our code. Instead of this, optimise any loops to use try/catch with resources, and also optimise any large datasets being loaded to use streaming rather than loading everything into memory at once.




# testing

lets review our existing integration tests, and ensure that wherever possible, they are loading real repos as examples from examples.yaml (via the ExampleProjectManager) instead of hardcoding fake mocked input data.

lets write some realistic integration tests for the maven artifact lookup service, which pass in common javax artifact coordinates, and verify that the service found matching jakarta maven artifact coordinates



# configuration

lets remove feature-flags.yaml and keep all of the feature flags defined in code.




# quality

now lets review the rest of our codebase and apply the same simplicity and consistency optimisations across our entire codebase, ensuring we don't break our tests while doing so

let's fix any compilation issues, ensure all the tests still pass, and fix any test failures


implement deduplication checks as a gradle task using:
- PMD CPD (copy-paste detector)
- Semgrep patterns



# user help

lets update the MCP tool list on the AI tab to reflect the current set of available tools
lets optimise the AI prompt suggestions on the AI tab, and keep them simple and concise 











After completing all the tasks, always check the rules in AGENTS.md to ensure that our implementation follows all the rules.  Then finish by checking that the code compiles, and complete the post-task steps in AGENTS.md
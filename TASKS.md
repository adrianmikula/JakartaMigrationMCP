Lets complete the following tasks in order. While implementing the tasks, always ensure we follow the coding guidelines defined in AGENTS.md





# bug fixes

the reports tab has disappeared. restore it.

plugin UI tabs often disappear after making UI code changes. investigate if our code for UI tabs is messy, flawed, inconsistent, or duplicated in some way which is exasberating these mistakes. Fix this.

the experimental feature tabs are not dynamically displayed after enabling experimental features from the support UI tab. Fix this.





# performance

lets load the example repo for the performance tests from the 'project_complexity' section in examples.yaml. project_complexity is already present in examples.yaml, and it's spelt correctly. for some reason the AI agent keeps thinking it's not there or misspelt. Look into other reasons why its not loading

it's bad practice to manually force GC calls inside our code. Instead of this, optimise any loops to use try/catch with resources, and also optimise any large datasets being loaded to use streaming rather than loading everything into memory at once.




# testing

lets review our existing integration tests, and ensure that wherever possible, they are loading real repos as examples from examples.yaml (via the ExampleProjectManager) instead of hardcoding fake mocked input data.

lets review test coverage of all the major tabs in the intellij plugin UI, and ensure all critical paths of our main features have test coverage.  

lets ensure all tests compile and pass



# cicd

lets fix the github actions - they aren't running the tests when i push to a branch with an open PR


# discoverability

Lets research how we could use the IntelliJ plugin API to auto-suggest our plugin to users who open source files containing javax imports etc.  We tried using the old v1 API but it's not working in the newer versions of IntelliJ.  What's the 2026 way of doing it?   It's possible we need to upgrade our stack to use the v2 intellij gradle plugin.





After completing all the tasks, always check the rules in AGENTS.md to ensure that our implementation follows all the rules.  Then finish by checking that the code compiles, and complete the post-task steps in AGENTS.md
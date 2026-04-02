Lets complete the following tasks in order. While implementing the tasks, always ensure we follow the coding guidelines defined in AGENTS.md



# testing

now lets run all of the tests and ensure they all pass, fixing any errors as we go





# fixes

lets review the premium MCP tools that we just removed from the community-mcp-server module (JakartaMigrationTooos.java) and add them into the premium-mcp-server module

lets remove createMigrationPlan from both modules, and detectBlockers from community. Let's move createReport to premkium, and let's move listDependenciesCompatibility to community. 

lets also add scanForJavaxAdvanced to premium, and make it run all 3 scan types.

lets rename analyseJakartaReadiness to scanForJavaxBasic in community, and make it run just the basic scan

lets remove createReport from community

also move any tests into the same module as the tool that they are testing

i can also see the scanForJavaxBasic provides its own implementation to scan the project, rather than hooking into the same scanning logic used by the intellij plugin UI


lets also check that the premium MCP also uses jackson for JSON response building, and hooks into the same advanced/platform scanning core logic as the intellij UI. for the basic scans, it can just delegate to the community MCP module and include these results in the premium result 

lets review all of the other community and premium mcp tools to ensure they are using jackson for maintainable JSON results, and that they hook into the same core logic as the intellij UI




# publishing

Lets review our existing NPM package and update it to work with the current codebase, ensuring that it only exposes tools from the community MCP server module, and doesn not bundle or expose any of the premium tools (premium-mcp-server module)







After completing all the tasks, always check the rules in AGENTS.md to ensure that our implementation follows all the rules.  Then finish by checking that the code compiles, and complete the post-task steps in AGENTS.md
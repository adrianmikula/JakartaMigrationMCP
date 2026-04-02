Lets complete the following tasks in order. While implementing the tasks, always ensure we follow the coding guidelines defined in AGENTS.md




# improvements

lets check if we can find more javax to jakarta migration recipes using the approaches documented in docs\research\scraping-all-jakarta-migration-recipes.md




# monetisation

Apply the intellij discoverability ideas outlined in docs\premium\INTELLIJ_MARKETPLACE_DISCOVERABILITY.md



Fix this IntelliJ compatibility error.
The plugin distribution bundles IDE package 'org.jetbrains.concurrency'. Bundling IDE packages is considered bad practice and may lead to sophisticated compatibility problems. Consider excluding these IDE packages from the plugin distribution. If your plugin depends on classes of an IDE bundled plugin, explicitly specify dependency on that plugin instead of bundling it.


we keep regressing and causing the org.jetbrains.concurrency issue, so lets add a kotest to ensure that this package is not bundled. Lets also document the issue in common_issues.md 



# testing

lets disable the MCP performance tests for now

lets finish implementing our integration tests which use real github example projects to test the scans and refactor recipes. don't worry aobut the ? urls in the YAML for now - we will replace them with real Github URLs later

lets fix all premium test failures. ignore community test failures for now




# final checks

fix all compilation issues
fix all test compile errors
fix all test failures



# cicd

Lets check that our Github actions pipeline will run all the tests for commits pushed to a branch which has an open pull request



After completing all the tasks, always check the rules in AGENTS.md to ensure that our implementation follows all the rules.  Then finish by checking that the code compiles, and complete the post-task steps in AGENTS.md
Lets complete the following tasks in order using SDD (spec driven development), TDD (test driven development), and the principles documented under the E:\Source\JakartaMigrationMCP\AgentRules folder: 


# minor UI fixes

1. The risk score on the dashboard appears to always be hardcoded to 45 and medium.  Ensure it is dynamically re-calculated from the scan results when the dashboard is loaded.

2. After you click Apply next to a Recipe, the button name changes to Undo.  I don't want the button names to change, just disable it.

3. Add an SQLite table which tracks recipes which have been run via the refactor tab.

4. The bullet list characters used on the migration strategy tab are displaying incorrectly 

5. the Runtime tab should be marked as experimental.


# new major features

10. Add a History tab, which tracks every code change we have ever made to the codebase via the plugin, along with undo button for reversible changes like openrewirte recipes. (Premium)
 


# cicd

1. Set up a new cicd (github actions) job for commits that get pushed to the main branch (e.g. when a PR is merged).  Use the intellij plugin to auto-publish the new version of the plugin. Follow the guidelines at https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000373070-Is-there-any-API-to-upload-plugin-to-repository- 



After completing the task list, ensure all new features have test coverage, all tests pass, and the plugin changelog (and feature list if new features were added) has been updated to mention the recent changes. fix all compile errors, even unrelated ones.

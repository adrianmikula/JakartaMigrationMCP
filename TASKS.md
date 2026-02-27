Lets complete the following tasks in order using SDD and TDD principles (as documented under the Agentrules folder): 

1. Add a UI tab to the intellij plugin for each of the following premium scanners:

DeprecatedApiScanner 
SecurityApiScanner 
JmsMessagingScanner 
TransitiveDependencyScanner 
ConfigFileScanner 
ClassloaderModuleScanner 
Previously Implemented Scanners:
JPA/Hibernate Annotation Scanner
Bean Validation Scanner
Servlet/JSP Scanner
CDI/Injection Scanner
Build Configuration Scanner
REST/SOAP Scanner

2. For each UI tab, add a scan button, and link it to the scan implementation in the premium-core module.

3. For each UI tab, and a table where the results of the scan can be displayed.

4. In the dashboard tab, add a concise count/total for each scan type

5. Ensure all new tests pass.

6. Update the intellij plugin metadata and changelog to mention the new features.


---------------------------------------------

NEXT TASKS

1. Go through any classes which didn't origianlyl follow SDD and TDD, and add specs for them.

2. Enforce a mimimum of 50% code coverage (per source file) across the whole project

3. Check that our open-core licensing split is still being strictly followed.

4. Add a support UI tab to the plugin, with a link to my LinkedIn and Github pages.  Also link to my Github sponsor page.

5. Optimize Project for AI Coding Velocity (using suggestions under the AgentRules folder)

---------------------------------------------

NEXT TASKS


1. Create schemas in the SQLite DB to keep a registry of different types of migration issues, along with which UI tab scans for them, and which refactor recipe (or equivalent) refactors the legacy namespace.

2. For each type of migration issue in the DB, add the anticipated error messages

3. Use the anticipated error message to suggest the solution in the Runtime UI tab when the user pastes in a stack trace


-------------------------------------------
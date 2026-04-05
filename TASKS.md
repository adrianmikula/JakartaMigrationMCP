Lets complete the following tasks in order. While implementing the tasks, always ensure we follow the coding guidelines defined in AGENTS.md





# bug fixes

the experimental feature tabs are not dynamically displayed after enabling experimental features from the support UI tab. Fix this.

failed: Recipe not found: org.openrewrite.java.migrate.jakarta.JavaxAnnotationToJakartaAnnotation. Discovered 730 recipes. Top ones: [org.openrewrite.DeleteSourceFiles,
This error only occurs for refactor recipes on the 'Annotatiojns' sub-tab
lets investigate why it only happens here. other rrefactor recipes work



Undo history fails. Failed to undo: No changed files found for execution ID: 2. The recipe may not have modified any files.




# performance


it's bad practice to manually force GC calls inside our code. Instead of this, optimise any loops to use try/catch with resources, and also optimise any large datasets being loaded to use streaming rather than loading everything into memory at once.





# dependencies

why are our local gradle tasks not finding and reusing cached maven artifacts?  Gradle keeps downloading the same artifacts every time





# dependency graph improvements

improve the force-directed dependency graph so the nodes aren't bunched together so close that they overlap each other

Lets choose the default view based on the number of dependencies:
- 5 or less: tree mode
- 5 to 25: circular mode
- 25 or more: force-directed mode





# advanced scans

we will also need to add detection of dockerfile changes using examples like https://github.com/lurodrig/log4j2-in-tomcat





# dependency tests

we are still not finding jakarta equivalents for a lot of very common javax artifacts.  
lets review the test coverage for maven lookups to find jakarta-equivalents for javax artifacts. identify any gaps in the test coverage or flaws in the design.
 lets review how our tests are implemented and why they might not reflect real-world lookups


2026-04-04 23:46:03,828 [  28590]   INFO - #c.i.c.ComponentStoreImpl - Saving Project(name=tomcat-realistic9280630051733489702, containerState=COMPONENT_CREATED, componentStore=/media/adrian/SHARED/Source/JakartaMigrationMCP/examples/tomcat-realistic9280630051733489702)RunManager took 30 ms
2026-04-04 23:46:03,992 [  28754]   INFO - adrianmikula.jakartamigration.intellij.service.MavenCentralService - Maven Central response structure: {"responseHeader":{"status":0,"QTime":0,"params":{"q":"g:javax.servlet AND a:jakarta.servlet-api","core":"","indent":"off","spellcheck":"true","fl":"id,g,a,latestVersion,p,ec,repositoryId,text,timestamp,versionCount","start":"","spellcheck.count":"5","sort":"score desc,timestamp desc,g asc,a asc","rows":"20","wt":"json","version":"2.2"}},"response":{"numFound":0,"start":0,"docs":[]},"spellcheck":{"suggestions":[]}}
2026-04-04 23:46:03,993 [  28755]   INFO - adrianmikula.jakartamigration.intellij.service.MavenCentralService - Docs node found: true, isArray: false, size: 0
2026-04-04 23:46:03,993 [  28755]   WARN - adrianmikula.jakartamigration.intellij.service.MavenCentralService - No docs array found in Maven Central response or empty array
2026-04-04 23:46:03,993 [  28755]   INFO - adrianmikula.jakartamigration.intellij.service.MavenCentralService - Found 0 Jakarta artifacts for javax.servlet:jakarta.servlet-api
2026-04-04 23:46:03,993 [  28755]   INFO - adrianmikula.jakartamigration.intellij.service.MavenCentralService - No results from primary endpoint, trying alternative...




# ui changes

lets remove all the 'Refresh' buttons from the UI

lets remove the 'AI assistant guidelines' and 'example projects' boxes from the AI tab

lets hide scans which got 0 results from the dashboard tab

lets list platforms that were detected in the dashboard tab

dependencies tab should combine status + compatibility columns into a single column, and should be colour coded to indicate if a jakarta-equivalent artifact was found. or not.

lets update the MCP tool list on the AI tab to reflect the current set of available tools


the platforms tab gets a JPanel error as soon as you click on the scan button.




# pdf reports

WWe keep getting PDF errors like
"Error generating report: PDF generation failed" despite multiple fix attempts.
let's step back fro the problem to review the existing reports code, eliminate any duplication or unnecessary complexity, and identify any flaws in the current design.




# bug fixes


2026-04-05 11:15:15,235 [ 134954]   INFO - adrianmikula.jakartamigration.intellij.service.MavenCentralService - Searching for Jakarta equivalents for javax dependency: javax.servlet:javax.servlet-api
2026-04-05 11:15:15,235 [ 134954]   INFO - adrianmikula.jakartamigration.intellij.service.MavenCentralService - Searching Maven Central: https://search.maven.org/solrsearch/select?q=g%3Ajavax.servlet+AND+a%3Ajavax.servlet-api&rows=20&wt=json
2026-04-05 11:15:16,070 [ 135789]   INFO - adrianmikula.jakartamigration.intellij.service.MavenCentralService - Maven Central response structure: {"responseHeader":{"status":0,"QTime":0,"params":{"q":"g:javax.servlet AND a:javax.servlet-api","core":"","indent":"off","spellcheck":"true","fl":"id,g,a,latestVersion,p,ec,repositoryId,text,timestamp,versionCount","start":"","spellcheck.count":"5","sort":"score desc,timestamp desc,g asc,a asc","rows":"20","wt":"json","version":"2.2"}},"response":{"numFound":1,"start":0,"docs":[{"id":"javax.servlet:javax.servlet-api","g":"javax.servlet","a":"javax.servlet-api","latestVersion":"4.0.1","repository
2026-04-05 11:15:16,070 [ 135789]   INFO - adrianmikula.jakartamigration.intellij.service.MavenCentralService - Docs node found: true, isArray: false, size: 0
2026-04-05 11:15:16,070 [ 135789]   WARN - adrianmikula.jakartamigration.intellij.service.MavenCentralService - No docs array found in Maven Central response or empty array
2026-04-05 11:15:16,070 [ 135789]   INFO - adrianmikula.jakartamigration.intellij.service.MavenCentralService - Found 0 Jakarta artifacts for javax.servlet:javax.servlet-api
2026-04-05 11:15:16,070 [ 135789]   INFO - adrianmikula.jakartamigration.intellij.service.MavenCentralService - No results from primary endpoint, trying alternative...





# refactoring

lets simplify the UIIntegrationTests and remove any useless tests.  lets keep it basic - we want to make the UI robust, without having complex, unmaintainable UI tests

lets review our recent code changes and staged changes, and eliminate any code duplication of unnecessary complexity. try to reduce the length of the code by 50% without breaking the code or loosing functionality

lets review recent commits and eliminate any code duplication of unnecessary complexity. try to reduce the length of the code by 50% without breaking the code or loosing functionality



# final checks

lets remove any temporary or legacy files which are no longer needed

let's fix any compilation issues, ensure all the tests still pass, and fix any test failures




After completing all the tasks, always check the rules in AGENTS.md to ensure that our implementation follows all the rules.  Then finish by checking that the code compiles, and complete the post-task steps in AGENTS.md
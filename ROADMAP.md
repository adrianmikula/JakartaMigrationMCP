







# improvements

The history tab should show successful actions in green, failed actions in red, and 'undo' actions in yellow.  


The migration strategy tab text content needs refinement. 
- The 'migration strategy:' label should be removed, since it uses up a lot of horizontal space which we want to use for the migration strategy cards.
- The risks and benefits boxes should have 50% less text so they are more readable.
- We can remove the migration phases box to the right of the risks and benefits boxes. (but don't remove the migration phases tabs at the bottom of the page)
- Keep the migration phases tabs at the bottom of the page, but triple the amount of text. each phase description should be descriptive without exceeding the size of the tab.









------------


# code health checks

1. check our available MCP tools. Check to see if there are any MCP tools which are highly-relevant to our development of this plugin and could speed up our coding agents?  Docuemnt recommended MCPS

2. check our choice of JVM and gradle settings. Are there any fast-start improvements or a super-fast test loop we can set up to improve our agent's fast-feedback loop to get in down to seconds?  Look into fast-start JVMs like crack and graal.

3. add a requirements folder under docs/community and docs/premium, where we will document all existing feature descriptions/requirements

4. Add/update typespec documents to the spec folder for all source code in this repo so we can follow SDD principles for 100% of code changes going forward. Put comments in code and tests to reference the related spec for each module.

5. Check the whole codebase for any hardcoded artifact names, version numbers, etc. Replace them with dynamic lookups where possible, or if not possible, move them all into YAML config files.

6. check the whole codebase for code quality, duplication, best practices, coverage, etc. 

7. Lets move all of the strategy types, benefits, risks, and phase descriptions out of the code and into a YAML config file. 

8. Let's move the list of openrewrite recipes out of the code and into a YAML config file.

9. Check for TODO and FIXME in the codebase, and resolve/fix them.

10. Let's split any super-large source files (>500 lines) into smaller source files where possible, for better LLM context understanding.




------------------------

# improvements for existing features

3. Add a lookup service which takes a javax maven artifact, and finds the matching jakarta maven artifact.  The matching jakarta artifact should be displayed in the dependencies tab. None of the artifact matches should be hardocded in the code - use dynamic lookups in maven central or similar.  




docs\research\scraping-all-jakarta-migration-recipes.md



11. Review all existing scans for accuracy. Do they accurately detect real javax.* to jakarta.* incompatibilities?  Do any of them detect something irrelevant, or give false positives/false negatives?  Do any of them incorrectly flag uses of the packages in the "javax" namespace which were not given to the eclipse foundation and therefore don't need to be changed at all?


--------------------

# plugin page imporvements

1. Add details in plugin.xml for "Getting Started" section

2. Add details in plugin.xml for "Technical Information" section

3. Add more details in plugin.xml under "Supported Technologies", e.g. mention more appservers (tomcat, wildfly etc), and more supported frameworks/platforms/libraries.

-------------------



--------------------

# new major features

 
5. Add support for scanning intellij run configurations (premium)

6. Add support for scanning dockerfiles and docker-compose files (premium)

7. Add support for scanning gitlab, github, azure CICD files (premium)

7. Add a reports tab, and add buttons to generate PDF reports based on the scan data. Find the simplest PDF library in 2026 which can easily create basic PDFs with intiuitive formatting/layouts, ideally based on a plain-text template which is easy to maintain like JSON, YAML, or Markdown. The PDF logic should go in the premium-core module.

8. To begin with, we will just have a single PDF report, which will display the following headings with scan data under each heading: 
- dependency tree with only organisation dependencies
- maven dependency list
- maven dependency tree 
- advanced scan results (heading per advanced scan type)
- Add a footer at the end of the PDF with the same links from the support tab

9. Add a Platforms tab, which shows major J2EE/appserver frameworks/platforms (e.g Spring, Spring Boot, Wildlfy, Tomcat etc) and indicates your current version, and which versions support jakarta, along with other requirements for the upgrade (e.g. minimum Java version). Avoid hardcoding version numbers and platform names - instead, create a YAML file which controls the info in this tab. The platform logic should go in the premium-core module.





----------------------

# public MCP tool

1. Create an npm package which can install our community MCP server

2. create a private npm package which installs our premium MCP server locally - not published.


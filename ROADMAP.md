



# minor UI issues

4. The dependencies graph should highlight dependencies which are jakarta-compatible in green, dependencies which need an upgrade in yellow, and dependencies which have no jakarta version in red. (same as the dependency list tab)


6. Add a colour legend to the bottom of the dependencies graph tab so it's clear what each colour represents in the graph.

9. Adjust the vertical size of each strategy box in the migration strategy tab, to be shorter vertically. We want more space for the text description boxes at the bottom.  


8. The dashboard elements vertical positioning is off. The scan counts overlap with the status/last analysed timestamp. Move the status/timestamp to be below the scan counts.

9. Add a progress bar to the advanced scans, so we can see how many of the scans have been completed so far.

10. highlight the scan counts which are >0 on the dashboard in red, and counts which =0 in green


5. In the dependencies graph tab, Instead of making organisational dependencies a different colour, give them a thicker border and a larger font, but use the same colours as all the other dependencies.

6. In the dependencies graph tab, display a popup info box when the user hovers over a dependency in the graph, with details of the maven coordinates, and the jakarta compatibility status of the dependency the mouse is over.


7. The refactor tab never shows any files changed. The 'refactoring results' box always shows 'No execution history found for this recipe' after a recipe has run. Check that the file changes and refactor status are being detected and displayed correctly in the UI.


8. the words 'total advanced issues' is in the middle of the scan counts. It should be at the bottom of them.

9. Remove the 'jakarta status indicator' from the main UI dashboard.


9. Adjust the vertical size of each strategy box in the migration strategy tab, to be slightly bigger vertically. 

9. Adjust the text descriptions of the strategy phases to be have longer explanations, while still maintainign clarity. I want them to be concise but informative.


-----------


# licensing

1. Advanced scans should be hidden until the user upgrades to premium or starts the free trial.


2. I can't see the button to upgrade to premium on the main plugin toolbar when I install the plugin directly from the IntelliJ marketplace. 


3. Let's check that our plugin implements all of the requirements for Freemium licensing outlined at https://plugins.jetbrains.com/plugin/30093-jakarta-migration/edit/monetization and https://plugins.jetbrains.com/docs/marketplace/freemium.html#how-to-make-your-plugin-freemium and https://plugins.jetbrains.com/docs/marketplace/add-marketplace-license-verification-calls-to-the-plugin-code.html



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

11. Lets add an AGENTS.md file at the root level, which centralises all of our most important AI agent coding rules and standards, e.g. SDD, TDD, 50% test coverage, mise-en-place commands, and a list of installed MCP tools.  Also mention a concise list of common issues with links to the full documentation under the docs/standards folder.



------------------------

# improvements for existing features

3. Add a lookup service which takes a javax maven artifact, and finds the matching jakarta maven artifact.  The matching jakarta artifact should be displayed in the dependencies tab. None of the artifact matches should be hardocded in the code - use dynamic lookups in maven central or similar.  

7. Add support for undoing openrewrite recipes in the refactor tab.

8. Add support for 'beta' feature flags, and only display them via an 'experimental features' checkbox in the support tab. Make the runtime tab into a beta feature.

9. Add risk scoring for each scan type, controlled via yaml configuration (no hardcoding values).  We want to grade things found in scans as low risk, med risk, or high risk.  

10. Display the overall risk score on the dashboard UI, and give it a risk category (Again controlled by the yaml configuration, no hardocidng value).  The risk categories can range from 'trivial', to 'low', 'medium',  'high' and 'extreme'

11. Review all existing scans for accuracy. Do they accurately detect real javax.* to jakarta.* incompatibilities?  Do any of them detect something irrelevant, or give false positives/false negatives?  Do any of them incorrectly flag uses of the packages in the "javax" namespace which were not given to the eclipse foundation and therefore don't need to be changed at all?


10. Add a new premium UI tab for the MCP Server, with a description of the tools available, and the MCP Server status.  Remove the MCP status indicator from the Dashboard tab.



1. Advanced scans tab disappears as soon as the user starts a free trial. It should be feature flagged, not hidden.

2. "no jakarta support" on dashboard tab is not highlighted in red if >0. And same for 'migratable' and 'transitive deps'.

3. 'mcp server' tab shows link for mcp documentation to a site which doesn't exist. For now lets just link to the github repo.



--------------------

# plugin page imporvements

1. Add details in plugin.xml for "Getting Started" section

2. Add details in plugin.xml for "Technical Information" section

3. Add more details in plugin.xml under "Supported Technologies", e.g. mention more appservers (tomcat, wildfly etc), and more supported frameworks/platforms/libraries.

-------------------

# new major features

10. Add a History tab, which tracks every code change we have ever made to the codebase via the plugin, along with undo button for reversible changes like openrewirte recipes. (Premium)
 
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


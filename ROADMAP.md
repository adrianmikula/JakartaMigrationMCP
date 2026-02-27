

# compatibility

3. Increase the version compatibility range in plugins.xml to support IntelliJ 2024, 2025 and 2026. lets set these values as configurable properties in gradle.properties instead of hardocidng them.  On the IntelliJ marketplace, it's listed as "Compatibility Range 233.0 — 233.*"  Refer to https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html#build-number-format


double-check that our version compatibility fix is correct.  our compatibility actually got worse - in the original v 1.0.0, we were listed as compatible with 233.0 — 243.*

When I try manually setting the build number to 253.* online, I get the error
"You cannot set an until-build value greater than 243.*"  So we should just have no untilversion set so we match all future versions, rather than setting a specific untilversion.




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



------------


# code health checks

1. Add/update typespec documents to the spec folder for all source code in this repo so we can follow SDD principles for 100% of code changes going forward. Put comments in code and tests to reference the related spec for each module.

2. Check the whole codebase for any hardcoded artifact names, version numbers, etc. Replace them with dynamic lookups where possible, or if not possible, move them all into YAML config files.

3. check the whole codebase for code quality, duplication, best practices, etc. 



------------------------

# improvements for existing features

3. Add a lookup service which takes a javax maven artifact, and finds the matching jakarta maven artifact.  The matching jakarta artifact should be displayed in the dependencies tab. None of the artifact matches should be hardocded in the code - use dynamic lookups in maven central or similar.  

7. Add support for undoing openrewrite recipes in the refactor tab.

8. Add support for 'beta' feature flags, and only display them via an 'experimental features' checkbox in the support tab. Make the runtime tab into a beta feature.

9. Add risk scoring for each scan type, controlled via yaml configuration (no hardcoding values).  We want to grade things found in scans as low risk, med risk, or high risk.  

10. Display the overall risk score on the dashboard UI, and give it a risk category (Again controlled by the yaml configuration, no hardocidng value).  The risk categories can range from 'trivial', to 'low', 'medium',  'high' and 'extreme'


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

10. Add a new premium UI tab for the MCP Server, with a description of the tools available, and the MCP Server status.  Remove the MCP status indicator from the Dashboard tab.

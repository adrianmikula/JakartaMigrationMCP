





# Startup Logic

## Database
- The upgrade-recommendation DB table should contain pairs of javax -> jakarta artifact upgrade recommendations.
- The list of suggested jakarta equivalents for javax dependency artifacts should be auto-generated from our list of openrewrite recipes at startup, and used to populate the upgrade-recommendation DB table.
- Startup order should be refactor recipes loding first, then dependency upgrade recommendations second.
- We need a way to load the openrewrite recipe list so the dependency recommendations can be loaded, even for non-premium users.




# Dependency tab


## Licensing
- Feature flagged as community.

## Layout (community intellij module)
- A table layout is shows with one dependency artifact per row, and columns for artifact name, version, suggested jakarta equivalent artifact name/version, and jakarta compatibility status.
-  If a javax dependency is found, we should use the upgrade recommendtation service to show the suggested jakarta equivalent in the UI.
- If the user clicks on a dependency, then refactor recipes which can migrate that dependency should be shown at the bottom of the tab, with a button to apply the recipe (only if user has a premium license).

## Actions (premium intellij module)
- If the user has the premium license, and clicks on the Apply button for a recipe that was suggesrted for a dependency, then run the same code that would be run in the refactor tab to trigger the recipe.

## Logic  (community core module)
- When a project dependnecy is found, we should check the upgrade recommendation DB table to see if there is a recommended jakarta equivalent we can use.




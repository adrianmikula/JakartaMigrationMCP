Lets complete the following tasks in order using SDD and TDD principles (as documented under the Agentrules folder): 





# minor UI issues

1. The dependencies graph should highlight dependencies which are jakarta-compatible in green, dependencies which need an upgrade in yellow, and dependencies which have no jakarta version in red. (same as the dependency list tab)


2. Add a colour legend to the bottom of the dependencies graph tab so it's clear what each colour represents in the graph.

3. Adjust the vertical size of each strategy box in the migration strategy tab, to be shorter vertically. We want more space for the text description boxes at the bottom.  


4. The dashboard elements vertical positioning is off. The scan counts overlap with the status/last analysed timestamp. Move the status/timestamp to be below the scan counts.

5. Add a progress bar to the advanced scans, so we can see how many of the scans have been completed so far.

6. highlight the scan counts which are >0 on the dashboard in red, and counts which =0 in green


7. In the dependencies graph tab, Instead of making organisational dependencies a different colour, give them a thicker border and a larger font, but use the same colours as all the other dependencies.

8. In the dependencies graph tab, display a popup info box when the user hovers over a dependency in the graph, with details of the maven coordinates, and the jakarta compatibility status of the dependency the mouse is over.


9. The refactor tab never shows any files changed. The 'refactoring results' box always shows 'No execution history found for this recipe' after a recipe has run. Check that the file changes and refactor status are being detected and displayed correctly in the UI.



After completing the task list, ensure all new features have test coverage, all tests pass, and the plugin changelog (and feature list if new features were added) has been updated to mention the recent changes.








# data model

1. Review our existing advanced scans to see how many of them overlap with refactor recipes.  Do they all overlap, or only some of them?  For ones which don't overlap, is this just because we should add more recipes?
2. Review our scan and recipe data models, to see how we could re-structure our DB and config files to link scans to recipes.
3. After restructuring, we want to be able to list/recommend refactor recipes which can be used to migrate files that were found by our advanced scans.



------------------------

# improvements for existing features

3. Add a lookup service which takes a javax maven artifact, and finds the matching jakarta maven artifact.  The matching jakarta artifact should be displayed in the dependencies tab. None of the artifact matches should be hardocded in the code - use dynamic lookups in maven central or similar.  




docs\research\scraping-all-jakarta-migration-recipes.md



11. Review all existing scans for accuracy. Do they accurately detect real javax.* to jakarta.* incompatibilities?  Do any of them detect something irrelevant, or give false positives/false negatives?  Do any of them incorrectly flag uses of the packages in the "javax" namespace which were not given to the eclipse foundation and therefore don't need to be changed at all?




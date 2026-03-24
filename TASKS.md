Lets complete the following tasks in order. While implementing the tasks, always ensure we follow the coding guidelines defined in AGENTS.md




# data model

1. Review our existing advanced scans to see how many of them overlap with refactor recipes.  Do they all overlap, or only some of them?  For ones which don't overlap, is this just because we should add more recipes?
2. Review our scan and recipe data models, to see how we could re-structure our DB and config files to link scans to recipes.
3. After restructuring, we want to be able to list/recommend refactor recipes which can be used to migrate files that were found by our advanced scans.






# new major features

 
7. Add a reports tab, and add buttons to generate PDF reports based on the scan data. Find the simplest PDF library in 2026 which can easily create basic PDFs with intiuitive formatting/layouts, ideally based on a plain-text template which is easy to maintain like JSON, YAML, or Markdown. The PDF logic should go in the premium-core module.

8. To begin with, we will just have a single PDF report, which will display the following headings with scan data under each heading: 
- dependency tree with only organisation dependencies
- maven dependency list
- maven dependency tree 
- advanced scan results (heading per advanced scan type)
- Add a footer at the end of the PDF with the same links from the support tab






# code health checks

4. Add/update typespec documents to the spec folder for all source code in this repo so we can follow SDD principles for 100% of code changes going forward. Put comments in code and tests to reference the related spec for each module.

5. Check the whole codebase for any hardcoded artifact names, version numbers, etc. Replace them with dynamic lookups where possible, or if not possible, move them all into YAML config files.

6. check the whole codebase for code quality, duplication, best practices, coverage, etc. 

7. Lets move all of the strategy types, benefits, risks, and phase descriptions out of the code and into YAML, JSON, or properties config files. 

9. Check for TODO and FIXME in the codebase, and resolve/fix them.

10. Let's split any super-large source files (>500 lines) into smaller source files where possible, for better LLM context understanding.

11. Lets check every source class to ensure important code paths are covered by meaningful tests.  As a minimum, each class should have 50% unit test coverage.







After completing all the tasks, always check the rules in AGENTS.md to ensure that our implementation follows all the rules.  Then finish by completing the post-task steps in AGENTS.md
 
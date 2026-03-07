Lets complete the following tasks in order using SDD (spec driven development), TDD (test driven development), and the principles documented under the E:\Source\JakartaMigrationMCP\AgentRules folder: 




# bug fixes

1. The main dashboard always shows a risk of 45 and a category of 'medium', even when i try scanning different example projects. Debug why the risk score seems to be hardocded (or calculated incorrectly), and fix it to be generated based on scan results scoring which is derived from YAML configuration.

2. The list of openrewrite recipes is missing some important refactors, like javax.validation, etc. 

3. The runtime tab is displayed, even though it's an experimental feature and should be hidden by default unless the 'experimental features' setting is enabled.

4. the 'dependencies' tab is not showing any upgrade version recommendations. is our dynamic recommendation lookup system which checks maven central even working properly?  and is the UI linked to it correctly?

5. some dependencies in the dependencies graph tab list 2 different typs in the UI mouseover infobox.  they show 'type: organisation dependnecy' and 'type:transitive dependnecy', but they look like direct dependencies from the maven or gradle file (e.g. spring)



# licensing

1. use the jetbrains marketplace api to set up automated licencing tests, using the demo store so our tests can verify the actual API calls without needing to make real purchases. 

2. Ensure we have tests for our 7 day free trial, our premium monthly subscription, and our downgrade to the community version after the free trial ends. 





After completing the task list, ensure all new features have test coverage, all tests pass, and the plugin changelog (and feature list if new features were added) has been updated to mention the recent changes. fix all compile errors, even unrelated ones.



# OpenRewrite execution fixes

Root cause investigation: `docs/community/investigations/openrewrite-not-modifying-files-2026-03-07.md`

6. **Fix `RefactoringEngine` tool selection** — `MigrationAnalysisService` initialises `RefactoringEngine` with the no-arg constructor which defaults to `SIMPLE_STRING_REPLACEMENT`. Change the initialisation to `new RefactoringEngine(MigrationTool.OPENREWRITE)` so that the OpenRewrite code path is actually exercised.

7. **Fix OpenRewrite file parsing — use paths, not raw strings** — `RefactoringEngine.refactorWithOpenRewrite()` currently calls `javaParser.parse(originalContent)` (raw string, no path). Change it to `javaParser.parse(List.of(filePath), baseDir, ctx)` so that OpenRewrite knows the real source path and can perform type resolution correctly.

8. **Rewrite `MigrationAnalysisService.applyRecipe` for project-level batch execution** — OpenRewrite must parse all source files at once, not one-by-one. The current per-file loop breaks cross-file type resolution and prevents many recipes from matching. The fix:
   - Collect all `.java` source file `Path` objects.
   - Parse all of them in a single `javaParser.parse(allPaths, baseDir, ctx)` call.
   - Run the activated OpenRewrite `Recipe` on the full `SourceSet`.
   - For each `Result` where `getAfter() != null`, write the modified content back to the original file path.
   - XML files that don't benefit from OpenRewrite can continue using the existing simple replacement fallback.


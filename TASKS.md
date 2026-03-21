Lets complete the following tasks in order following the guidelines in AGENTS.md



# tests
1. lets create a test which loops to run all recipes in recipes.yaml to ensure they exist and are configured correctly
2. lets create a test which loops to run all advanced scan types to ensure they can run successfully
3. Lets find example/template javax projects on github containing each major javax namespace. Add them to examples.yaml



# code quality

1. The RecipeSeeder class hardcodes a lot of javax -> jakarta upgrade artifact names.  We want to avoid any hardocding of artifact names in java code. Load these from YAML configuration instead. Check other classes for similar hardcodings and move them all to YAML configuration.

2. lets update recipeseeder.java to seed all recipes using recipes.yaml, so the different parts of the code are all connected and aligned with recipes.yaml


# bug fixes

1. In the Dependencies tab, the 'Apply Recipe' button should not show a 'premium feature' dialog if the user has a premium license, it should just apply the recipe.

6. If the risk score on the dashboard tab has already been calculated from a basic scan, then later the advanced scans are run, the risk score should be updated/changed to account for the new advanced scan findings. 

7. The Runtime tab should be hidden if experimental features are disabled, and visible if experimental features are enabled.

9. In the history tab, the Action column should show 'Applied' for historical actions which were applying a recipe, and 'Undo' for historical actions which were undoing a recipe.

10. The bullet symbols in the Mitration Strategy tab should render correctly.  Ensure they are valid characters. 



# debugging

1. > Task :premium-intellij-plugin:initializeIntelliJPlugin FAILED
[gradle-intellij-plugin :premium-intellij-plugin premium-intellij-plugin:premium-intellij-plugin:initializeIntelliJPlugin] Cannot resolve the latest Gradle IntelliJ Plugin version
org.gradle.api.GradleException: Cannot resolve the latest Gradle IntelliJ Plugin version
	at org.jetbrains.intellij.utils.LatestVersionResolver$Companion.fromGitHub(LatestVersionResolver.kt:31)
	at org.jetbrains.intellij.tasks.InitializeIntelliJPluginTask.checkPluginVersion(InitializeIntelliJPluginTask.kt:66)
	at org.jetbrains.intellij.tasks.InitializeIntelliJPluginTask.initialize(InitializeIntelliJPluginTask.kt:50)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at org.gradle.internal.reflect.JavaMethod.invoke(JavaMethod.java:125)
	at org.gradle.api.internal.project.taskfactory.StandardTaskAction.doExecute(StandardTaskAction.java:58)
...
	at org.gradle.execution.plan.DefaultPlanExecutor$ExecutorWorker.run(DefaultPlanExecutor.java:380)
	at org.gradle.internal.concurrent.ExecutorPolicy$CatchAndRecordFailures.onExecute(ExecutorPolicy.java:64)
	at org.gradle.internal.concurrent.AbstractManagedExecutor$1.run(AbstractManagedExecutor.java:47)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
	at java.base/java.lang.Thread.run(Thread.java:1583)
Caused by: java.lang.NullPointerException: getHeaderField("Location") must not be null
	at org.jetbrains.intellij.utils.LatestVersionResolver$Companion.fromGitHub(LatestVersionResolver.kt:28)
	... 128 more
Caused by: java.lang.NullPointerException: getHeaderField("Location") must not be null


Execution failed for task ':premium-intellij-plugin:initializeIntelliJPlugin'.
> java.nio.file.NoSuchFileException: E:\Source\JakartaMigrationMCP\premium-intellij-plugin\build\tmp\initializeIntelliJPlugin\coroutines-javaagent.jar


in docs\standards\COMMON_ISSUES.md we documented "IntelliJ Platform Gradle Plugin v1.x Incompatible with Java 25 / Gradle 9.0"



2. Failed: Recipe not found: org.openrewrite.java.migrate.jakarta.JavaxValidationToJakartaValidation. Discovered 730 recipes. Top ones: [org.openrewrite.DeleteSourceFiles, org.openrewrite.FindCollidingSourceFiles, org.openrewrite.FindGitProvenance, org.openrewrite.FindLstProvenance, org.openrewrite.FindParseFailures, org.openrewrite.FindQuarks, org.openrewrite.FindSourceFiles, org.openrewrite.IsInRepository, org.openrewrite.ListRuntimeClasspath, org.openrewrite.RenameFile, org.openrewrite.SetFilePermissions, org.openrewrite.analysis.controlflow.ControlFlowVisualization, org.openrewrite.analysis.search.FindFlowBetweenMethods, org.openrewrite.analysis.search.FindMethods, org.openrewrite.analysis.search.UriCreatedWithHttpScheme, org.openrewrite.config.CompositeRecipe, org.openrewrite.github.AddCronTrigger, org.openrewrite.github.AddManualTrigger, org.openrewrite.github.AutoCancelInProgressWorkflow, org.openrewrite.github.ChangeActionVersion, org.openrewrite.github.ChangeDependabotScheduleInterval, org.openrewrite.github.DependabotCheckForGithubActionsUpdatesDaily, org.openrewrite.github.DependabotCheckForGithubActionsUpdatesWeekly, org.openrewrite.github.PreferTemurinDistributions, org.openrewrite.github.RemoveAllCronTriggers, org.openrewrite.github.ReplaceRunners, org.openrewrite.github.SetupJavaAdoptOpenJDKToTemurin, org.openrewrite.github.SetupJavaAdoptOpenj9ToSemeru, org.openrewrite.github.SetupJavaCaching, org.openrewrite.github.SetupJavaUpgradeJavaVersion, org.openrewrite.gradle.AddDependency, org.openrewrite.gradle.AddProperty, org.openrewrite.gradle.ChangeDependency, org.openrewrite.gradle.ChangeDependencyArtifactId, org.openrewrite.gradle.ChangeDependencyClassifier, org.openrewrite.gradle.ChangeDependencyConfiguration, org.openrewrite.gradle.ChangeDependencyExtension, org.openrewrite.gradle.ChangeDependencyGroupId, org.openrewrite.gradle.ChangeExtraProperty, org.openrewrite.gradle.DependencyUseMapNotation, org.openrewrite.gradle.DependencyUseStringNotation, org.openrewrite.gradle.EnableGradleBuildCache, org.openrewrite.gradle.EnableGradleParallelExecution, org.openrewrite.gradle.RemoveDependency, org.openrewrite.gradle.RemoveRepository, org.openrewrite.gradle.UpdateGradleWrapper, org.openrewrite.gradle.UpdateJavaCompatibility, org.openrewrite.gradle.UpgradeDependencyVersion, org.openrewrite.gradle.plugins.AddBuildPlugin, org.openrewrite.gradle.plugins.AddDevelocityGradlePlugin]



After completing tasks, make sure you follow the post-task steps in AGENTS.md
 
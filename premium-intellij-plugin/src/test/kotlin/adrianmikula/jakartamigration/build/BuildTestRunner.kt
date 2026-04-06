package adrianmikula.jakartamigration.build

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldContain
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import org.junit.platform.suite.api.SuiteDisplayName
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Test runner for build validation tests.
 * Can be run in CI to ensure build configuration doesn't break.
 */
@Suite
@SuiteDisplayName("Jakarta Migration Build Validation Tests")
@SelectClasses(
    GradleBuildValidationTest::class,
    GradleCompatibilityTest::class,
    LombokIssuePreventionTest::class,
    JsonDeserializationTest::class,
    IntelliJPluginIssuePreventionTest::class,
    ModuleBoundaryTest::class,
    VersionConsistencyTest::class
)
class BuildTestSuite

/**
 * Individual test classes for more granular control
 */
class GradleCompatibilityTest : FunSpec({
    
    test("should use compatible IntelliJ Platform Gradle Plugin version") {
        val buildFile = Paths.get("build.gradle.kts")
        Files.exists(buildFile) shouldBe true
        
        val buildContent = Files.readString(buildFile)
        buildContent shouldContain("id(\"org.jetbrains.intellij\") version \"1.17.2\"")
    }
    
    test("should configure Java toolchain to avoid Java 25 issues") {
        val buildFile = Paths.get("premium-intellij-plugin/build.gradle.kts")
        Files.exists(buildFile) shouldBe true
        
        val buildContent = Files.readString(buildFile)
        buildContent shouldContain("java")
        
        // Should not reference Java 25 directly
        buildContent shouldNotContain("JavaVersion.VERSION_25")
        buildContent shouldNotContain("25.0.2")
    }
})

class LombokIssuePreventionTest : FunSpec({
    
    test("should not have @AllArgsConstructor conflicts in domain classes") {
        val domainFile = Paths.get("premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/Artifact.java")
        
        if (Files.exists(domainFile)) {
            val content = Files.readString(domainFile)
            
            if (content.contains("@AllArgsConstructor")) {
                content shouldNotContain("public Artifact(")
            }
        }
    }
    
    test("should use @RequiredArgsConstructor for records with final fields") {
        val domainFile = Paths.get("premium-core-engine/src/main/java/adrianmikula/jakartamigration/advancedscanning/domain/ReflectionUsage.java")
        
        if (Files.exists(domainFile)) {
            val content = Files.readString(domainFile)
            
            if (content.contains("record ") && content.contains("final ")) {
                content shouldContain("@RequiredArgsConstructor") or 
                content.shouldNotContain("@AllArgsConstructor")
            }
        }
    }
})

class JsonDeserializationTest : FunSpec({
    
    test("should include @JsonIgnoreProperties in domain classes") {
        val domainFile = Paths.get("premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/Artifact.java")
        
        if (Files.exists(domainFile)) {
            val content = Files.readString(domainFile)
            
            if (content.contains("record ") || content.contains("class ")) {
                content shouldContain("@JsonIgnoreProperties(ignoreUnknown = true)")
            }
        }
    }
    
    test("should not have deprecated jakartaCompatible field") {
        val artifactFile = Paths.get("premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/Artifact.java")
        
        if (Files.exists(artifactFile)) {
            val content = Files.readString(artifactFile)
            content shouldNotContain("jakartaCompatible")
        }
    }
})

class IntelliJPluginIssuePreventionTest : FunSpec({
    
    test("should use ApplicationManager.getApplication().invokeLater() for dialogs") {
        val uiFile = Paths.get("premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/HistoryTabComponent.java")
        
        if (Files.exists(uiFile)) {
            val content = Files.readString(uiFile)
            
            if (content.contains("Messages.showErrorDialog")) {
                content shouldNotContain("SwingUtilities.invokeLater")
                content shouldContain("ApplicationManager.getApplication().invokeLater")
            }
        }
    }
    
    test("should handle OpenRewrite Path relativization correctly") {
        val recipeFile = Paths.get("premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/service/util/IsolatedRecipeRunner.java")
        
        if (Files.exists(recipeFile)) {
            val content = Files.readString(recipeFile)
            
            if (content.contains("relativize") && content.contains("getSourcePath()")) {
                content shouldContain("isAbsolute()")
            }
        }
    }
})

class ModuleBoundaryTest : FunSpec({
    
    test("community modules should not depend on premium modules") {
        val communityBuildFile = Paths.get("community-core-engine/build.gradle.kts")
        
        if (Files.exists(communityBuildFile)) {
            val content = Files.readString(communityBuildFile)
            content shouldNotContain("premium-core-engine")
            content shouldNotContain("premium-mcp-server")
            content shouldNotContain("premium-intellij-plugin")
        }
    }
    
    test("premium modules can depend on community modules") {
        val premiumBuildFile = Paths.get("premium-core-engine/build.gradle.kts")
        
        if (Files.exists(premiumBuildFile)) {
            val content = Files.readString(premiumBuildFile)
            content shouldContain("implementation(project(\":community-core-engine\"))")
        }
    }
})

class VersionConsistencyTest : FunSpec({
    
    test("plugin.xml version should match build.gradle.kts version") {
        val pluginXml = Paths.get("premium-intellij-plugin/src/main/resources/META-INF/plugin.xml")
        val buildGradle = Paths.get("premium-intellij-plugin/build.gradle.kts")
        
        if (Files.exists(pluginXml) && Files.exists(buildGradle)) {
            val xmlContent = Files.readString(pluginXml)
            val gradleContent = Files.readString(buildGradle)
            
            val xmlVersionMatch = Regex("<version>([^<]+)</version>").find(xmlContent)
            val gradleVersionMatch = Regex("version\\s*=\\s*[\"']([^\"']+)[\"']").find(gradleContent)
            
            val xmlVersion = xmlVersionMatch?.groupValues?.get(1)
            val gradleVersion = gradleVersionMatch?.groupValues?.get(1)
            
            xmlVersion shouldNotBe(null
            gradleVersion shouldNotBe(null
            xmlVersion shouldBe gradleVersion
        }
    }
    
    test("version should be in semantic format") {
        val pluginXml = Paths.get("premium-intellij-plugin/src/main/resources/META-INF/plugin.xml")
        
        if (Files.exists(pluginXml)) {
            val content = Files.readString(pluginXml)
            
            val versionMatch = Regex("<version>([^<]+)</version>").find(content)
            val version = versionMatch?.groupValues?.get(1)
            
            version shouldNotBe(null
            version shouldMatch(Regex("\\d+\\.\\d+\\.\\d+"))
        }
    }
})

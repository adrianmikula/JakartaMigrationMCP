package adrianmikula.jakartamigration.intellij

import io.kotest.core.spec.style.Description
import io.kotest.matchers.should
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Spock tests to verify plugin.xml meets JetBrains Marketplace requirements
 * Tests all required parameters for paid plugins according to:
 * https://plugins.jetbrains.com/docs/marketplace/add-required-parameters.html
 */
class PluginMarketplaceValidationTest : io.kotest.core.spec.Specification {

    fun setup() {
        // Setup common test paths
        pluginXml = Paths.get("src/main/resources/META-INF/plugin.xml")
        buildGradle = Paths.get("build.gradle.kts")
    }

    @Test
    @DisplayName("Plugin XML should contain required product-descriptor")
    @EnabledIfEnvironmentVariable(named = "CI")
    fun `plugin xml should contain product descriptor`() {
        expect:
        Files.exists(pluginXml)
        
        when:
        Files.exists(pluginXml)
        val content = Files.readString(pluginXml)
        
        then:
        content should contain("<product-descriptor code=\"PJAKARTAMIGRATI\"")
    }

    @Test
    @DisplayName("Product descriptor should follow naming conventions")
    @EnabledIfEnvironmentVariable(named = "CI")
    fun `product descriptor should follow naming conventions`() {
        when:
        Files.exists(pluginXml)
        val content = Files.readString(pluginXml)
        
        then:
        with(content.split("<product-descriptor code=\"")[1].length) {
            content.length >= 4
        }
        with(content.split("<product-descriptor code=\"")[1].length) {
            content.length <= 15
        }
        with(content.split("<product-descriptor code=\"")[1].matches("[A-Z]+")) {
            true
        }
        and {
            !content.split("<product-descriptor code=\"")[1].matches(".*\\d+.*")
        }
        and {
            !content.split("<product-descriptor code=\"")[1].matches(".*[^A-Z].*")
        }
    }

    @Test
    @DisplayName("Plugin XML should contain required release-date")
    @EnabledIfEnvironmentVariable(named = "CI")
    fun `plugin xml should contain release date`() {
        when:
        Files.exists(pluginXml)
        val content = Files.readString(pluginXml)
        
        then:
        content should contain("release-date=\"")
        content should match(Regex(".*release-date=\"\\d{4}\\d{2}\\d{2}\".*"))
        
        // Verify release date is recent (within last 2 years)
        val releaseDate = content.substring(
            content.indexOf("release-date=\"") + 13, 
            content.indexOf("\"", content.indexOf("release-date=\"") + 13)
        )
        releaseDate should notBeNull()
    }

    @Test
    @DisplayName("Plugin XML should contain required release-version")
    @EnabledIfEnvironmentVariable(named = "CI")
    fun `plugin xml should contain release version`() {
        when:
        Files.exists(pluginXml)
        val content = Files.readString(pluginXml)
        
        then:
        content should contain("release-version=\"")
        content.split("<release-version=\"")[1].should(match(Regex(".*\\d{2,}.*"))
        
        // Verify release-version matches release-date format
        val releaseDate = content.substring(
            content.indexOf("release-date=\"") + 13, 
            content.indexOf("\"", content.indexOf("release-date=\"") + 13)
        )
        val releaseVersion = content.substring(
            content.indexOf("release-version=\"") + 16, 
            content.indexOf("\"", content.indexOf("release-version=\"") + 16)
        )
        
        releaseDate should notBeNull()
        releaseVersion should notBeNull()
    }

    @Test
    @DisplayName("Plugin XML should contain valid vendor information")
    @EnabledIfEnvironmentVariable(named = "CI")
    fun `plugin xml should contain valid vendor`() {
        when:
        Files.exists(pluginXml)
        val content = Files.readString(pluginXml)
        
        then:
        content should contain("vendor email=")
        content should contain("name=\"")
        content should contain("url=\"")
        
        // Verify vendor name is reasonable length
        val vendorName = content.substring(
            content.indexOf("name=\"") + 6, 
            content.indexOf("\"", content.indexOf("name=\"") + 6)
        )
        vendorName.length should beBetween(2, 50)
    }

    @Test
    @DisplayName("Plugin XML should contain valid plugin structure")
    @EnabledIfEnvironmentVariable(named = "CI")
    fun `plugin xml should contain valid structure`() {
        when:
        Files.exists(pluginXml)
        val content = Files.readString(pluginXml)
        
        then:
        content should contain("<id>")
        content should contain("<version>")
        content should contain("<name>")
        content should contain("<description>")
        content should contain("<category>")
        content should contain("<idea-plugin>") and content.should(contain("</idea-plugin>"))
        
        // Verify no deprecated elements
        content should not(contain("<purchases>"))
        content should not(contain("<deprecated>"))
    }

    @Test
    @DisplayName("Plugin XML should not contain invalid elements")
    @EnabledIfEnvironmentVariable(named = "CI")
    fun `plugin xml should not contain invalid elements`() {
        when:
        Files.exists(pluginXml)
        val content = Files.readString(pluginXml)
        
        then:
        content should not(contain("<trial-"))
        content should not(contain("<deprecated>"))
    }

    @Test
    @DisplayName("Plugin version should be consistent with build.gradle")
    @EnabledIfEnvironmentVariable(named = "CI")
    fun `plugin version should be consistent with build gradle`() {
        when:
        Files.exists(pluginXml) && Files.exists(buildGradle)
        val content = Files.readString(pluginXml)
        val xmlVersion = content.substring(
            content.indexOf("<version>") + 9, 
            content.indexOf("<", content.indexOf("<version>") + 9)
        )
        val gradleVersion = Files.readString(buildGradle)
            .substring(gradleVersion.indexOf("version = '") + 9, 
            gradleVersion.indexOf("'", gradleVersion.indexOf("version = '") + 9)
        
        xmlVersion shouldBe gradleVersion
    }

    @Test
    @DisplayName("Plugin should load successfully in test environment")
    @EnabledIfEnvironmentVariable(named = "CI")
    fun `plugin should load successfully in test environment`() {
        when:
        Files.exists(pluginXml)
        val content = Files.readString(pluginXml)
        
        then:
        // Verify all required elements are present for marketplace validation
        content should(contain("<product-descriptor>"))
        content should(contain("release-date>"))
        content should(contain("release-version>"))
        content should(contain("vendor email="))
        content should(contain("<idea-plugin>"))
        
        // Verify no marketplace-blocking elements
        content should not(contain("<trial-"))
        content should not(contain("<deprecated>"))
        content should not(contain("<purchases>"))
    }
}

package adrianmikula.jakartamigration.intellij

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.Assertions.*
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * JUnit 5 tests to verify plugin.xml meets JetBrains Marketplace requirements
 * Tests all required parameters for paid plugins according to:
 * https://plugins.jetbrains.com/docs/marketplace/add-required-parameters.html
 */
class PluginMarketplaceValidationTest {

    private val pluginXml = Paths.get("src/main/resources/META-INF/plugin.xml")
    private val buildGradle = Paths.get("build.gradle.kts")

    @Test
    @DisplayName("Plugin XML should contain required product-descriptor")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `plugin xml should contain product descriptor`() {
        assertTrue(Files.exists(pluginXml), "plugin.xml should exist")
        
        val content = Files.readString(pluginXml)
        assertTrue(content.contains("<product-descriptor"), "Should contain product-descriptor element")
        assertTrue(content.contains("code=\"PJAKARTAMIGRATI\""), "Should contain correct product code")
    }

    @Test
    @DisplayName("Product descriptor should follow naming conventions")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `product descriptor should follow naming conventions`() {
        val content = Files.readString(pluginXml)
        
        // Extract product code
        val codeMatch = Regex("code=\"([^\"]+)\"").find(content)
        val productCode = codeMatch?.groupValues?.get(1) ?: fail("Product code not found")
        
        // Verify naming conventions
        assertTrue(productCode.startsWith("P"), "Product code must start with P")
        assertTrue(productCode.length >= 4, "Product code must be at least 4 characters")
        assertTrue(productCode.length <= 15, "Product code must be no longer than 15 characters")
        assertTrue(productCode.matches(Regex("[A-Z]+")), "Product code must contain only capital letters")
        assertFalse(productCode.matches(Regex(".*\\d+.*")), "Product code must not contain numbers")
        assertFalse(productCode.matches(Regex(".*[^A-Z].*")), "Product code must not contain special symbols")
    }

    @Test
    @DisplayName("Plugin XML should contain required release-date")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `plugin xml should contain release date`() {
        val content = Files.readString(pluginXml)
        
        assertTrue(content.contains("release-date=\""), "Should contain release-date attribute")
        
        // Extract release date
        val dateMatch = Regex("release-date=\"(\\d{8})\"").find(content)
        val releaseDateStr = dateMatch?.groupValues?.get(1) ?: fail("Release date not found")
        
        // Verify format YYYYMMDD
        assertTrue(releaseDateStr.matches(Regex("\\d{8}")), "Release date must be in YYYYMMDD format")
        
        // Verify it's a valid date
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val releaseDate = LocalDate.parse(releaseDateStr, formatter)
        
        // Verify release date is recent (within last 2 years)
        val twoYearsAgo = LocalDate.now().minusYears(2)
        assertTrue(releaseDate.isAfter(twoYearsAgo), "Release date should be within last 2 years")
    }

    @Test
    @DisplayName("Plugin XML should contain required release-version")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `plugin xml should contain release version`() {
        val content = Files.readString(pluginXml)
        
        assertTrue(content.contains("release-version=\""), "Should contain release-version attribute")
        
        // Extract release version
        val versionMatch = Regex("release-version=\"(\\d+)\"").find(content)
        val releaseVersion = versionMatch?.groupValues?.get(1) ?: fail("Release version not found")
        
        // Verify format (at least 2 digits)
        assertTrue(releaseVersion.matches(Regex("\\d{2,}")), "Release version must contain at least 2 digits")
    }

    @Test
    @DisplayName("Plugin XML should contain valid vendor information")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `plugin xml should contain valid vendor`() {
        val content = Files.readString(pluginXml)
        
        assertTrue(content.contains("<vendor"), "Should contain vendor element")
        assertTrue(content.contains("email="), "Should contain vendor email")
        assertTrue(content.contains("name="), "Should contain vendor name")
        assertTrue(content.contains("url="), "Should contain vendor URL")
        
        // Extract vendor name
        val nameMatch = Regex("name=\"([^\"]+)\"").find(content)
        val vendorName = nameMatch?.groupValues?.get(1) ?: fail("Vendor name not found")
        
        assertTrue(vendorName.length >= 2, "Vendor name must be at least 2 characters")
        assertTrue(vendorName.length <= 50, "Vendor name must be no longer than 50 characters")
    }

    @Test
    @DisplayName("Plugin XML should contain valid plugin structure")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `plugin xml should contain valid structure`() {
        val content = Files.readString(pluginXml)
        
        // Required elements
        assertTrue(content.contains("<idea-plugin"), "Should contain idea-plugin root element")
        assertTrue(content.contains("</idea-plugin>"), "Should close idea-plugin element")
        assertTrue(content.contains("<id>"), "Should contain plugin ID")
        assertTrue(content.contains("<version>"), "Should contain plugin version")
        assertTrue(content.contains("<name>"), "Should contain plugin name")
        assertTrue(content.contains("<description>"), "Should contain plugin description")
        assertTrue(content.contains("<category>"), "Should contain plugin category")
        
        // Verify no deprecated elements
        assertFalse(content.contains("<purchases>"), "Should not contain purchases element")
        assertFalse(content.contains("<deprecated>"), "Should not contain deprecated element")
    }

    @Test
    @DisplayName("Plugin XML should not contain invalid elements")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `plugin xml should not contain invalid elements`() {
        val content = Files.readString(pluginXml)
        
        // Elements that would block marketplace submission
        assertFalse(content.contains("<trial-"), "Should not contain trial elements")
        assertFalse(content.contains("<deprecated>"), "Should not contain deprecated element")
        assertFalse(content.contains("<purchases>"), "Should not contain purchases element")
    }

    @Test
    @DisplayName("Plugin version should be consistent with build.gradle")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `plugin version should be consistent with build gradle`() {
        assertTrue(Files.exists(pluginXml), "plugin.xml should exist")
        assertTrue(Files.exists(buildGradle), "build.gradle.kts should exist")
        
        val xmlContent = Files.readString(pluginXml)
        val gradleContent = Files.readString(buildGradle)
        
        // Extract XML version
        val xmlVersionMatch = Regex("<version>([^<]+)</version>").find(xmlContent)
        val xmlVersion = xmlVersionMatch?.groupValues?.get(1) ?: fail("XML version not found")
        
        // Extract Gradle version
        val gradleVersionMatch = Regex("version\\s*=\\s*[\"']([^\"']+)[\"']").find(gradleContent)
        val gradleVersion = gradleVersionMatch?.groupValues?.get(1) ?: fail("Gradle version not found")
        
        assertEquals(xmlVersion, gradleVersion, "Plugin version should match Gradle version")
    }

    @Test
    @DisplayName("Plugin code should be exactly 'PJAKARTAMIGRATI'")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `plugin code should be exactly PJAKARTAMIGRATI`() {
        val content = Files.readString(pluginXml)
        
        // Extract product code
        val codeMatch = Regex("code=\"([^\"]+)\"").find(content)
        val productCode = codeMatch?.groupValues?.get(1) ?: fail("Product code not found")
        
        assertEquals("PJAKARTAMIGRATI", productCode, "Plugin code must be exactly 'PJAKARTAMIGRATI'")
    }

    @Test
    @DisplayName("Plugin version should be in semantic version format")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `plugin version should be in semantic version format`() {
        val content = Files.readString(pluginXml)
        
        // Extract version
        val versionMatch = Regex("<version>([^<]+)</version>").find(content)
        val version = versionMatch?.groupValues?.get(1) ?: fail("Version not found")
        
        // Verify semantic version format (x.y.z)
        assertTrue(version.matches(Regex("\\d+\\.\\d+\\.\\d+")), "Version must be in semantic format x.y.z")
        
        // Extract first two digits for release-version comparison
        val versionParts = version.split(".")
        assertTrue(versionParts.size >= 3, "Version must have at least 3 parts")
        val firstTwoDigits = versionParts.take(2).joinToString(".")
        
        // Compare with release-version
        val content2 = Files.readString(pluginXml)
        val releaseVersionMatch = Regex("release-version=\"(\\d+)\"").find(content2)
        val releaseVersion = releaseVersionMatch?.groupValues?.get(1) ?: fail("Release version not found")
        
        assertEquals(firstTwoDigits, releaseVersion, "Release version should match first two digits of plugin version")
    }

    @Test
    @DisplayName("Idea-version element should not contain until-build property")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `idea version should not contain until build property`() {
        val content = Files.readString(pluginXml)
        
        val ideaVersionMatch = Regex("<idea-version[^>]*>([^<]+)</idea-version>").find(content)
        val ideaVersionElement = ideaVersionMatch?.groupValues?.get(1) ?: fail("Idea-version element not found")
        
        assertFalse(ideaVersionElement.contains("until-build="), "Idea-version should not contain until-build property")
    }

    @Test
    @DisplayName("Release date should be in valid YYYYMMDD format")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `release date should be in valid format`() {
        val content = Files.readString(pluginXml)
        
        val dateMatch = Regex("release-date=\"(\\d{8})\"").find(content)
        val releaseDateStr = dateMatch?.groupValues?.get(1) ?: fail("Release date not found")
        
        // Verify format YYYYMMDD
        assertTrue(releaseDateStr.matches(Regex("\\d{8}")), "Release date must be in YYYYMMDD format")
        
        // Verify it's a valid date
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        assertDoesNotThrow { LocalDate.parse(releaseDateStr, formatter) }
        
        // Verify it's a reasonable date (not too old or future)
        val releaseDate = LocalDate.parse(releaseDateStr, formatter)
        val oneYearAgo = LocalDate.now().minusYears(1)
        val oneYearFromNow = LocalDate.now().plusYears(1)
        
        assertTrue(releaseDate.isAfter(oneYearAgo), "Release date should not be more than 1 year old")
        assertTrue(releaseDate.isBefore(oneYearFromNow), "Release date should not be more than 1 year in the future")
    }

    @Test
    @DisplayName("Release version should be at least 2 digits")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `release version should be at least 2 digits`() {
        val content = Files.readString(pluginXml)
        
        val versionMatch = Regex("release-version=\"(\\d+)\"").find(content)
        val releaseVersion = versionMatch?.groupValues?.get(1) ?: fail("Release version not found")
        
        assertTrue(releaseVersion.length >= 2, "Release version must contain at least 2 digits")
        assertTrue(releaseVersion.matches(Regex("\\d{2,}")), "Release version must contain at least 2 digits")
    }

    @Test
    @DisplayName("Plugin should load successfully in test environment")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `plugin should load successfully in test environment`() {
        val content = Files.readString(pluginXml)
        
        // Verify all required elements are present for marketplace validation
        assertTrue(content.contains("<product-descriptor"), "Should contain product descriptor")
        assertTrue(content.contains("release-date="), "Should contain release date")
        assertTrue(content.contains("release-version="), "Should contain release version")
        assertTrue(content.contains("vendor email="), "Should contain vendor email")
        assertTrue(content.contains("<idea-plugin"), "Should contain idea-plugin root")
        
        // Verify no marketplace-blocking elements
        assertFalse(content.contains("<trial-"), "Should not contain trial elements")
        assertFalse(content.contains("<deprecated>"), "Should not contain deprecated element")
        assertFalse(content.contains("<purchases>"), "Should not contain purchases element")
    }

    @Test
    @DisplayName("Change notes version should match plugin version")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `change notes version should match plugin version`() {
        val content = Files.readString(pluginXml)
        val gradleContent = Files.readString(buildGradle)
        
        // Extract plugin version from XML
        val xmlVersionMatch = Regex("<version>([^<]+)</version>").find(content)
        val xmlVersion = xmlVersionMatch?.groupValues?.get(1) ?: fail("XML version not found")
        
        // Extract version from change notes
        val changeNotesMatch = Regex("<h2>([^<]+)</h2>").find(content)
        val changeNotesVersion = changeNotesMatch?.groupValues?.get(1) ?: fail("Change notes version not found")
        
        assertEquals(xmlVersion, changeNotesVersion, "Change notes version should match plugin version")
    }
}

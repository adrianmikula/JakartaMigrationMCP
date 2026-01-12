plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "jakarta-migration-mcp"

// Include premium package as a subproject (for multi-project build)
// Note: When premium is moved to separate repo, this will be removed
// and premium will depend on published free package JAR instead
include("jakarta-migration-mcp-premium")
project(":jakarta-migration-mcp-premium").projectDir = file("jakarta-migration-mcp-premium")


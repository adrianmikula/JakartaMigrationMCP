rootProject.name = "jakarta-migration-mcp-premium"

// Allow building this subproject standalone by treating the parent build as a composite build.
// When running as part of the root multi-project build, this file is not used.
includeBuild("..") {
    dependencySubstitution {
        substitute(module("adrianmikula:jakarta-migration-mcp")).using(project(":"))
    }
}


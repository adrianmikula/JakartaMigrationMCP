plugins {
    id("org.jetbrains.intellij")
    java
    jacoco
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.withType<JacocoReport> {
    dependsOn("test")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

dependencies {
    // Jackson for JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    
    // Community Core Engine - local project dependency (Apache 2.0)
    implementation(project(":community-core-engine"))

    // Premium Core Engine - local project dependency (Proprietary)
    // Contains premium features: refactoring, runtime verification, etc.
    implementation(project(":premium-core-engine"))

    // UI Testing dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.junit.platform:junit-platform-suite:1.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.5.0")
}

intellij {
    version.set("2023.3.4")
    type.set("IC")
    plugins.set(listOf("com.intellij.java"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("243.*")
    }

    // Disable buildSearchableOptions task to avoid JavaVersion.parse() failure with JDK 25
    buildSearchableOptions {
        onlyIf { false }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Create a task to generate MCP tool definitions JSON
tasks.register("generateMcpToolsJson") {
    doLast {
        val toolsJson = File(project.buildDir, "mcp-tools.json")
        toolsJson.parentFile.mkdirs()
        toolsJson.writeText("""
            |{
            |  "server": {
            |    "name": "jakarta-migration-mcp",
            |    "version": "1.0.0",
            |    "description": "MCP server for Jakarta EE migration analysis and automation",
            |    "author": "Jakarta Migration Team",
            |    "vendor": "jakarta-migration.com"
            |  },
            |  "tools": [
            |    {
            |      "name": "analyzeJakartaReadiness",
            |      "description": "Analyzes a Java project's readiness for migration from Java EE 8 (javax.*) to Jakarta EE 9+ (jakarta.*)",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory to analyze"
            |          },
            |          "includeTransitiveDependencies": {
            |            "type": "boolean",
            |            "description": "Whether to include transitive dependencies in the analysis"
            |          },
            |          "analysisLevel": {
            |            "type": "string",
            |            "enum": ["basic", "detailed", "comprehensive"]
            |          }
            |        },
            |        "required": ["projectPath"]
            |      }
            |    },
            |    {
            |      "name": "analyzeMigrationImpact",
            |      "description": "Provides detailed analysis of migration impact including affected dependencies, breaking changes, and estimated effort",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory"
            |          },
            |          "scope": {
            |            "type": "string",
            |            "enum": ["dependencies", "code", "configuration", "all"]
            |          }
            |        },
            |        "required": ["projectPath"]
            |      }
            |    },
            |    {
            |      "name": "detectBlockers",
            |      "description": "Identifies migration blockers that prevent successful Jakarta EE migration",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory"
            |          }
            |        },
            |        "required": ["projectPath"]
            |      }
            |    },
            |    {
            |      "name": "recommendVersions",
            |      "description": "Analyzes project dependencies and recommends compatible Jakarta EE versions",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory"
            |          }
            |        },
            |        "required": ["projectPath"]
            |      }
            |    },
            |    {
            |      "name": "applyOpenRewriteRefactoring",
            |      "description": "Applies OpenRewrite refactoring recipes to automatically migrate javax packages to jakarta equivalents",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory"
            |          },
            |          "filePatterns": {
            |            "type": "array",
            |            "description": "Glob patterns for files to refactor"
            |          },
            |          "dryRun": {
            |            "type": "boolean",
            |            "description": "If true, only preview changes without applying them"
            |          }
            |        },
            |        "required": ["projectPath", "filePatterns"]
            |      }
            |    },
            |    {
            |      "name": "scanBinaryDependency",
            |      "description": "Scans a compiled JAR dependency for Jakarta EE compatibility issues",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "jarPath": {
            |            "type": "string",
            |            "description": "Absolute path to the JAR file to scan"
            |          }
            |        },
            |        "required": ["jarPath"]
            |      }
            |    },
            |    {
            |      "name": "updateDependency",
            |      "description": "Updates a single dependency to a recommended Jakarta-compatible version",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory"
            |          },
            |          "groupId": {
            |            "type": "string",
            |            "description": "Maven group ID of the dependency"
            |          },
            |          "artifactId": {
            |            "type": "string",
            |            "description": "Maven artifact ID of the dependency"
            |          },
            |          "currentVersion": {
            |            "type": "string",
            |            "description": "Current version of the dependency"
            |          },
            |          "recommendedVersion": {
            |            "type": "string",
            |            "description": "Version to upgrade to"
            |          }
            |        },
            |        "required": ["projectPath", "groupId", "artifactId", "currentVersion", "recommendedVersion"]
            |      }
            |    },
            |    {
            |      "name": "generateMigrationPlan",
            |      "description": "Generates a detailed, phased migration plan for Jakarta EE migration",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory"
            |          }
            |        },
            |        "required": ["projectPath"]
            |      }
            |    },
            |    {
            |      "name": "validateMigration",
            |      "description": "Validates that migration was successful by running compile checks and test suites",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory"
            |          }
            |        },
            |        "required": ["projectPath"]
            |      }
            |    }
            |  ]
            |}
        """.trimMargin())
    }
}

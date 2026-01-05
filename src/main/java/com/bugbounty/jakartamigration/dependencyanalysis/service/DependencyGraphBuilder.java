package com.bugbounty.jakartamigration.dependencyanalysis.service;

import com.bugbounty.jakartamigration.dependencyanalysis.domain.DependencyGraph;

import java.nio.file.Path;

/**
 * Builds dependency graphs from Maven or Gradle project files.
 */
public interface DependencyGraphBuilder {
    
    /**
     * Builds a dependency graph from a Maven pom.xml file.
     *
     * @param pomXmlPath Path to the pom.xml file
     * @return Dependency graph containing all dependencies
     * @throws DependencyGraphException if the pom.xml cannot be parsed
     */
    DependencyGraph buildFromMaven(Path pomXmlPath);
    
    /**
     * Builds a dependency graph from a Gradle build file.
     *
     * @param buildFilePath Path to build.gradle or build.gradle.kts file
     * @return Dependency graph containing all dependencies
     * @throws DependencyGraphException if the build file cannot be parsed
     */
    DependencyGraph buildFromGradle(Path buildFilePath);
    
    /**
     * Builds a dependency graph by auto-detecting the build system.
     *
     * @param projectRoot Path to the project root directory
     * @return Dependency graph containing all dependencies
     * @throws DependencyGraphException if no build file is found or cannot be parsed
     */
    DependencyGraph buildFromProject(Path projectRoot);
}


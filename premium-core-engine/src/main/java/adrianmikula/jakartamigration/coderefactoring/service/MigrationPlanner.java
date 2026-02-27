package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.MigrationPlan;
import adrianmikula.jakartamigration.coderefactoring.domain.PhaseAction;
import adrianmikula.jakartamigration.coderefactoring.domain.RefactoringPhase;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.domain.RiskAssessment;
import adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Plans the migration by determining optimal refactoring order and phases.
 */
public class MigrationPlanner {
    
    private static final int ESTIMATED_MINUTES_PER_FILE = 2;
    private final SourceCodeScanner sourceCodeScanner;
    
    public MigrationPlanner() {
        this.sourceCodeScanner = null; // Can be null for backward compatibility
    }
    
    public MigrationPlanner(SourceCodeScanner sourceCodeScanner) {
        this.sourceCodeScanner = sourceCodeScanner;
    }
    
    /**
     * Creates a migration plan for the given project.
     */
    public MigrationPlan createPlan(String projectPath, DependencyAnalysisReport report) {
        Path project = Paths.get(projectPath);
        
        // Discover all Java files and configuration files
        List<String> allFiles = discoverFiles(project);
        
        // Determine optimal order
        List<String> orderedFiles = determineOptimalOrder(allFiles);
        
        // Create phases
        List<RefactoringPhase> phases = createPhases(orderedFiles, project);
        
        // Calculate estimated duration
        Duration estimatedDuration = calculateEstimatedDuration(phases);
        
        // Use risk assessment from dependency analysis
        RiskAssessment overallRisk = report.riskAssessment();
        
        // Prerequisites
        List<String> prerequisites = determinePrerequisites(report);
        
        return new MigrationPlan(
            phases,
            orderedFiles,
            estimatedDuration,
            overallRisk,
            prerequisites
        );
    }
    
    /**
     * Determines the optimal order for refactoring files.
     */
    public List<String> determineOptimalOrder(List<String> files) {
        // Separate files by type
        List<String> buildFiles = files.stream()
            .filter(f -> f.contains("pom.xml") || f.contains("build.gradle"))
            .sorted()
            .collect(Collectors.toList());
        
        List<String> configFiles = files.stream()
            .filter(f -> f.contains(".xml") && !f.contains("pom.xml"))
            .sorted()
            .collect(Collectors.toList());
        
        List<String> javaFiles = files.stream()
            .filter(f -> f.endsWith(".java"))
            .sorted()
            .collect(Collectors.toList());
        
        // Order: build files -> config files -> Java files
        List<String> ordered = new ArrayList<>();
        ordered.addAll(buildFiles);
        ordered.addAll(configFiles);
        ordered.addAll(javaFiles);
        
        return ordered;
    }
    
    private List<String> discoverFiles(Path projectPath) {
        List<String> files = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths
                .filter(Files::isRegularFile)
                .filter(p -> isRelevantFile(p.toString()))
                .map(p -> projectPath.relativize(p).toString())
                .forEach(files::add);
        } catch (IOException e) {
            // Return empty list if discovery fails
        }
        
        return files;
    }
    
    private boolean isRelevantFile(String filePath) {
        return filePath.endsWith(".java") ||
               filePath.endsWith("pom.xml") ||
               filePath.endsWith("build.gradle") ||
               filePath.contains("persistence.xml") ||
               filePath.contains("web.xml");
    }
    
    private List<RefactoringPhase> createPhases(List<String> files, Path projectRoot) {
        List<RefactoringPhase> phases = new ArrayList<>();
        
        // Phase 1: Build files
        List<String> buildFiles = files.stream()
            .filter(f -> f.contains("pom.xml") || f.contains("build.gradle"))
            .toList();
        if (!buildFiles.isEmpty()) {
            phases.add(new RefactoringPhase(
                1,
                "Update build files and dependencies",
                buildFiles,
                createBuildFileActions(buildFiles, projectRoot),
                List.of("UpdateMavenCoordinates", "UpdateGradleDependencies"),
                List.of(),
                Duration.ofMinutes(buildFiles.size() * ESTIMATED_MINUTES_PER_FILE)
            ));
        }
        
        // Phase 2: Configuration files
        List<String> configFiles = files.stream()
            .filter(f -> f.contains(".xml") && !f.contains("pom.xml"))
            .toList();
        if (!configFiles.isEmpty()) {
            phases.add(new RefactoringPhase(
                2,
                "Update XML configuration files",
                configFiles,
                createConfigFileActions(configFiles, projectRoot),
                List.of("UpdatePersistenceXml", "UpdateWebXml"),
                phases.isEmpty() ? List.of() : List.of("Phase 1"),
                Duration.ofMinutes(configFiles.size() * ESTIMATED_MINUTES_PER_FILE)
            ));
        }
        
        // Phase 3: Java files (in batches)
        List<String> javaFiles = files.stream()
            .filter(f -> f.endsWith(".java"))
            .toList();
        
        int batchSize = 10; // Refactor 10 files at a time
        int phaseNumber = phases.size() + 1;
        
        for (int i = 0; i < javaFiles.size(); i += batchSize) {
            int end = Math.min(i + batchSize, javaFiles.size());
            List<String> batch = javaFiles.subList(i, end);
            
            List<String> dependencies = new ArrayList<>();
            if (phaseNumber > 1) {
                dependencies.add("Phase " + (phaseNumber - 1));
            }
            
            phases.add(new RefactoringPhase(
                phaseNumber++,
                "Refactor Java files (batch " + ((i / batchSize) + 1) + ")",
                batch,
                createJavaFileActions(batch, projectRoot),
                List.of("AddJakartaNamespace"),
                dependencies,
                Duration.ofMinutes(batch.size() * ESTIMATED_MINUTES_PER_FILE)
            ));
        }
        
        return phases;
    }
    
    private Duration calculateEstimatedDuration(List<RefactoringPhase> phases) {
        return phases.stream()
            .map(RefactoringPhase::estimatedDuration)
            .reduce(Duration.ZERO, Duration::plus);
    }
    
    private List<String> determinePrerequisites(DependencyAnalysisReport report) {
        List<String> prerequisites = new ArrayList<>();
        
        if (!report.blockers().isEmpty()) {
            prerequisites.add("Resolve " + report.blockers().size() + " dependency blockers");
        }
        
        if (report.readinessScore().score() < 0.5) {
            prerequisites.add("Improve migration readiness score");
        }
        
        return prerequisites;
    }
    
    private List<PhaseAction> createBuildFileActions(List<String> buildFiles, Path projectRoot) {
        List<PhaseAction> actions = new ArrayList<>();
        for (String file : buildFiles) {
            List<String> changes = new ArrayList<>();
            if (file.contains("pom.xml")) {
                changes.add("Update Maven dependencies from javax.* to jakarta.*");
                changes.add("Update Spring Boot version to 3.x if needed");
            } else if (file.contains("build.gradle")) {
                changes.add("Update Gradle dependencies from javax.* to jakarta.*");
                changes.add("Update Spring Boot version to 3.x if needed");
            }
            actions.add(new PhaseAction(file, "UPDATE_DEPENDENCY", changes));
        }
        return actions;
    }
    
    private List<PhaseAction> createConfigFileActions(List<String> configFiles, Path projectRoot) {
        List<PhaseAction> actions = new ArrayList<>();
        for (String file : configFiles) {
            List<String> changes = new ArrayList<>();
            if (file.contains("persistence.xml")) {
                changes.add("Update namespace from http://java.sun.com/xml/ns/persistence to https://jakarta.ee/xml/ns/persistence");
            } else if (file.contains("web.xml")) {
                changes.add("Update namespace from http://java.sun.com/xml/ns/javaee to https://jakarta.ee/xml/ns/jakartaee");
            }
            actions.add(new PhaseAction(file, "UPDATE_XML_NAMESPACE", changes));
        }
        return actions;
    }
    
    private List<PhaseAction> createJavaFileActions(List<String> javaFiles, Path projectRoot) {
        List<PhaseAction> actions = new ArrayList<>();
        
        if (sourceCodeScanner == null) {
            // Fallback: create generic actions if scanner not available
            for (String file : javaFiles) {
                actions.add(new PhaseAction(file, "UPDATE_IMPORTS", List.of("Replace all javax.* imports with jakarta.* equivalents")));
            }
            return actions;
        }
        
        // Use source code scanner to get specific import changes
        for (String file : javaFiles) {
            Path filePath = projectRoot.resolve(file);
            try {
                if (Files.exists(filePath)) {
                    adrianmikula.jakartamigration.sourcecodescanning.domain.FileUsage usage = 
                        sourceCodeScanner.scanFile(filePath);
                    
                    if (usage.hasJavaxUsage()) {
                        List<String> changes = usage.javaxImports().stream()
                            .map(imp -> String.format("Line %d: Replace '%s' with '%s'", 
                                imp.lineNumber(), imp.fullImport(), imp.jakartaEquivalent()))
                            .collect(Collectors.toList());
                        actions.add(new PhaseAction(file, "UPDATE_IMPORTS", changes));
                    } else {
                        actions.add(new PhaseAction(file, "UPDATE_IMPORTS", List.of("No javax imports found - file may already be migrated")));
                    }
                }
            } catch (Exception e) {
                // If scanning fails, create generic action
                actions.add(new PhaseAction(file, "UPDATE_IMPORTS", List.of("Replace all javax.* imports with jakarta.* equivalents")));
            }
        }
        
        return actions;
    }
}


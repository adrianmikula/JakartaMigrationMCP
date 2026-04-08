package adrianmikula.jakartamigration.pdfreporting.demo;

import adrianmikula.jakartamigration.pdfreporting.service.impl.PdfReportServiceImpl;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Demo class to showcase PDFBox-based PDF report generation.
 * Run this class to generate sample PDF reports.
 */
public class PdfReportDemo {
    
    public static void main(String[] args) {
        System.out.println("🚀 Starting PDFBox Report Generation Demo...");
        
        try {
            PdfReportServiceImpl pdfService = new PdfReportServiceImpl();
            
            // Create sample dependency graph
            DependencyGraph dependencyGraph = createSampleDependencyGraph();
            
            // Create sample scan results
            ComprehensiveScanResults scanResults = createSampleScanResults();
            
            // Generate comprehensive report
            generateComprehensiveReport(pdfService, dependencyGraph, scanResults);
            
            // Generate dependency report
            generateDependencyReport(pdfService, dependencyGraph);
            
            // Generate scan results report
            generateScanResultsReport(pdfService, scanResults);
            
            System.out.println("✅ All PDF reports generated successfully!");
            System.out.println("📁 Check the current directory for generated PDF files.");
            
        } catch (Exception e) {
            System.err.println("❌ Error generating PDF reports: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static DependencyGraph createSampleDependencyGraph() {
        DependencyGraph graph = new DependencyGraph();
        
        // Add Jakarta compatible artifacts
        Artifact jakartaServlet = new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false);
        Artifact jakartaJpa = new Artifact("jakarta.persistence", "jakarta.persistence-api", "3.1.0", "compile", false);
        
        // Add javax artifacts (need migration)
        Artifact javaxServlet = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        Artifact javaxJpa = new Artifact("javax.persistence", "javax.persistence-api", "2.2.1", "compile", false);
        Artifact javaxValidation = new Artifact("javax.validation", "validation-api", "2.0.1.Final", "compile", false);
        
        // Add nodes
        graph.addNode(jakartaServlet);
        graph.addNode(jakartaJpa);
        graph.addNode(javaxServlet);
        graph.addNode(javaxJpa);
        graph.addNode(javaxValidation);
        
        // Add dependencies
        graph.addEdge(new Dependency(javaxServlet, jakartaServlet, "compile", false));
        graph.addEdge(new Dependency(javaxJpa, jakartaJpa, "compile", false));
        graph.addEdge(new Dependency(javaxValidation, jakartaJpa, "compile", false));
        
        return graph;
    }
    
    private static ComprehensiveScanResults createSampleScanResults() {
        ComprehensiveScanResults.ScanSummary summary = new ComprehensiveScanResults.ScanSummary(
            250, 15, 3, 8, 4, 78.5
        );
        
        return new ComprehensiveScanResults(
            "/demo/jakarta-migration-project",
            LocalDateTime.now(),
            Map.of("jpaEntities", "12 entities found", "jpaRepositories", "8 repositories found"),
            Map.of("validationConstraints", "25 constraints found"),
            Map.of("servletFilters", "5 filters found", "servletListeners", "3 listeners found"),
            Map.of("springBoot", "2.7.x", "hibernate", "5.6.x"),
            Map.of("transitiveDeps", "45 dependencies analyzed"),
            Map.of("maven", "pom.xml updated", "gradle", "build.gradle analyzed"),
            List.of(
                "Update javax.servlet to jakarta.servlet",
                "Update javax.persistence to jakarta.persistence",
                "Update javax.validation to jakarta.validation",
                "Review Spring Boot compatibility",
                "Test all JPA entities after migration"
            ),
            15,
            summary
        );
    }
    
    private static void generateComprehensiveReport(PdfReportServiceImpl pdfService, 
                                                   DependencyGraph dependencyGraph, 
                                                   ComprehensiveScanResults scanResults) throws Exception {
        System.out.println("📄 Generating comprehensive report...");
        
        Map<String, Object> customData = Map.of(
            "projectName", "Jakarta Migration Demo Project",
            "description", "Sample project demonstrating Jakarta EE migration capabilities",
            "organization", "Demo Organization",
            "version", "1.0.0"
        );
        
        Path outputPath = Paths.get("jakarta-migration-comprehensive-report.pdf");
        
        var request = new adrianmikula.jakartamigration.pdfreporting.service.PdfReportService.GeneratePdfReportRequest(
            outputPath,
            dependencyGraph,
            null,
            scanResults,
            null,
            pdfService.getDefaultTemplate(),
            customData
        );
        
        Path result = pdfService.generateComprehensiveReport(request);
        System.out.println("✅ Comprehensive report generated: " + result.toAbsolutePath());
    }
    
    private static void generateDependencyReport(PdfReportServiceImpl pdfService, 
                                               DependencyGraph dependencyGraph) throws Exception {
        System.out.println("📄 Generating dependency report...");
        
        Path outputPath = Paths.get("jakarta-migration-dependency-report.pdf");
        Path result = pdfService.generateDependencyReport(dependencyGraph, outputPath);
        
        System.out.println("✅ Dependency report generated: " + result.toAbsolutePath());
    }
    
    private static void generateScanResultsReport(PdfReportServiceImpl pdfService, 
                                                ComprehensiveScanResults scanResults) throws Exception {
        System.out.println("📄 Generating scan results report...");
        
        Path outputPath = Paths.get("jakarta-migration-scan-results-report.pdf");
        Path result = pdfService.generateScanResultsReport(scanResults, outputPath);
        
        System.out.println("✅ Scan results report generated: " + result.toAbsolutePath());
    }
}

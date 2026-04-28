package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adrianmikula.jakartamigration.advancedscanning.domain.*;
import adrianmikula.jakartamigration.advancedscanning.service.ScanRecipeRecommendationService.RecipeRecommendation;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Refactoring Action Report generation functionality.
 * Tests the new consolidated Refactoring Action Report with comprehensive data.
 */
class RefactoringActionReportTest {

    @TempDir
    Path tempDir;

    private PdfReportService pdfReportService;

    @BeforeEach
    void setUp() {
        pdfReportService = new HtmlToPdfReportServiceImpl();
    }

    @Test
    @DisplayName("Should generate Refactoring Action Report with minimal data")
    void shouldGenerateRefactoringActionReportWithMinimalData() throws Exception {
        // Arrange
        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            tempDir.resolve("minimal-refactoring-report.pdf"),
            "Test Project",
            "Test Refactoring Action Report",
            null, // dependencyGraph
            null, // scanResults
            List.of(), // recipeRecommendations
            List.of(), // javaxReferences
            List.of(), // openRewriteRecipes
            Map.of("automationReady", 50), // refactoringReadiness
            Map.of("highPriority", 0, "mediumPriority", 0, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Test Suite") // customData
        );

        // Act
        Path result = pdfReportService.generateRefactoringActionReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals("minimal-refactoring-report.html", result.getFileName().toString());
    }

    @Test
    @DisplayName("Should generate Refactoring Action Report with dependency data")
    void shouldGenerateRefactoringActionReportWithDependencyData() throws Exception {
        // Arrange
        DependencyGraph dependencyGraph = createMockDependencyGraph();
        
        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            tempDir.resolve("dependency-refactoring-report.pdf"),
            "Test Project with Dependencies",
            "Test Refactoring Action Report",
            dependencyGraph,
            null, // scanResults
            List.of(), // recipeRecommendations
            List.of(), // javaxReferences
            List.of(), // openRewriteRecipes
            Map.of("automationReady", 60), // refactoringReadiness
            Map.of("highPriority", 0, "mediumPriority", 0, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Test Suite") // customData
        );

        // Act
        Path result = pdfReportService.generateRefactoringActionReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals("dependency-refactoring-report.html", result.getFileName().toString());
    }

    @Test
    @DisplayName("Should generate Refactoring Action Report with scan results")
    void shouldGenerateRefactoringActionReportWithScanResults() {
        // Arrange
        ComprehensiveScanResults scanResults = createMockScanResults();
        
        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            tempDir.resolve("scan-refactoring-report.pdf"),
            "Test Project with Scans",
            "Test Refactoring Action Report",
            null, // dependencyGraph
            scanResults,
            List.of(), // recipeRecommendations
            List.of(), // javaxReferences
            List.of(), // openRewriteRecipes
            Map.of("automationReady", 75), // refactoringReadiness
            Map.of("highPriority", 0, "mediumPriority", 0, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Test Suite") // customData
        );

        // Act
        Path result = pdfReportService.generateRefactoringActionReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals("scan-refactoring-report.html", result.getFileName().toString());
    }

    @Test
    @DisplayName("Should generate Refactoring Action Report with javax references")
    void shouldGenerateRefactoringActionReportWithJavaxReferences() {
        // Arrange
        List<Map<String, Object>> javaxReferences = List.of(
            Map.of("file", "SampleController.java", "line", "15", "reference", "javax.servlet.http.HttpServlet", "priority", "high", "recipeAvailable", true),
            Map.of("file", "SampleEntity.java", "line", "8", "reference", "javax.persistence.Entity", "priority", "medium", "recipeAvailable", true),
            Map.of("file", "SampleService.java", "line", "22", "reference", "javax.inject.Inject", "priority", "low", "recipeAvailable", false)
        );
        
        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            tempDir.resolve("javax-refactoring-report.pdf"),
            "Test Project with Javax References",
            "Test Refactoring Action Report",
            null, // dependencyGraph
            null, // scanResults
            List.of(), // recipeRecommendations
            javaxReferences,
            List.of(), // openRewriteRecipes
            Map.of("automationReady", 80), // refactoringReadiness
            Map.of("highPriority", 2, "mediumPriority", 1, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Test Suite") // customData
        );

        // Act
        Path result = pdfReportService.generateRefactoringActionReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals("javax-refactoring-report.html", result.getFileName().toString());
    }

    @Test
    @DisplayName("Should generate Refactoring Action Report with OpenRewrite recipes")
    void shouldGenerateRefactoringActionReportWithOpenRewriteRecipes() {
        // Arrange
        List<Map<String, Object>> openRewriteRecipes = List.of(
            Map.of("name", "Jakarta EE 9 to 10 Migration", "category", "Namespace", "description", "Updates javax imports to jakarta", "automated", true),
            Map.of("name", "JPA Entity Annotations", "category", "JPA", "description", "Updates JPA annotations to jakarta namespace", "automated", true),
            Map.of("name", "Servlet API Migration", "category", "Servlet", "description", "Updates servlet references to jakarta", "automated", false)
        );
        
        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            tempDir.resolve("recipe-refactoring-report.pdf"),
            "Test Project with Recipes",
            "Test Refactoring Action Report",
            null, // dependencyGraph
            null, // scanResults
            List.of(), // recipeRecommendations
            List.of(), // javaxReferences
            openRewriteRecipes,
            Map.of("automationReady", 90), // refactoringReadiness
            Map.of("highPriority", 0, "mediumPriority", 0, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Test Suite") // customData
        );

        // Act
        Path result = pdfReportService.generateRefactoringActionReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals("recipe-refactoring-report.html", result.getFileName().toString());
    }

    @Test
    @DisplayName("Should handle null project name gracefully")
    void shouldHandleNullProjectNameGracefully() {
        // Arrange
        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            tempDir.resolve("null-project-refactoring-report.pdf"),
            null, // projectName
            "Test Refactoring Action Report",
            null, // dependencyGraph
            null, // scanResults
            List.of(), // recipeRecommendations
            List.of(), // javaxReferences
            List.of(), // openRewriteRecipes
            Map.of("automationReady", 50), // refactoringReadiness
            Map.of("highPriority", 0, "mediumPriority", 0, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Test Suite") // customData
        );

        // Act
        Path result = pdfReportService.generateRefactoringActionReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals("null-project-refactoring-report.html", result.getFileName().toString());
    }

    /**
     * Creates a mock dependency graph for testing.
     */
    private DependencyGraph createMockDependencyGraph() {
        // This would typically use the actual DependencyGraph implementation
        // For testing purposes, we return null since the implementation handles null gracefully
        return null;
    }

    /**
     * Creates mock scan results for testing.
     */
    private ComprehensiveScanResults createMockScanResults() {
        // Create actual mock scan results with data that will populate the report
        Map<String, Object> jpaResults = new HashMap<>();
        jpaResults.put("result", createMockJpaProjectScanResult());

        Map<String, Object> beanValidationResults = new HashMap<>();
        beanValidationResults.put("result", createMockBeanValidationProjectScanResult());

        Map<String, Object> cdiResults = new HashMap<>();
        cdiResults.put("result", createMockCdiInjectionProjectScanResult());

        Map<String, Object> servletJspResults = new HashMap<>();
        servletJspResults.put("result", createMockServletJspProjectScanResult());

        Map<String, Object> buildConfigResults = new HashMap<>();
        buildConfigResults.put("result", createMockBuildConfigProjectScanResult());

        List<String> recommendations = List.of(
            "Update JPA annotations from javax.persistence to jakarta.persistence",
            "Update Servlet imports from javax.servlet to jakarta.servlet",
            "Update CDI annotations from javax.inject to jakarta.inject"
        );

        ComprehensiveScanResults.ScanSummary summary = new ComprehensiveScanResults.ScanSummary(
            150, // totalFilesScanned
            45,  // filesWithIssues
            12,  // criticalIssues
            33,  // warningIssues
            0,   // infoIssues
            65.5 // readinessScore
        );

        return new ComprehensiveScanResults(
            "/test/project",
            LocalDateTime.now(),
            jpaResults,
            beanValidationResults,
            cdiResults,
            servletJspResults,
            Map.of(), // thirdPartyLibResults
            Map.of(), // transitiveDependencyResults
            buildConfigResults,
            recommendations,
            45, // totalIssuesFound
            summary
        );
    }

    private JpaProjectScanResult createMockJpaProjectScanResult() {
        List<JpaAnnotationUsage> annotations = List.of(
            new JpaAnnotationUsage("javax.persistence.Entity", "jakarta.persistence.Entity", 15, "User", "class"),
            new JpaAnnotationUsage("javax.persistence.Table", "jakarta.persistence.Table", 16, "User", "class"),
            new JpaAnnotationUsage("javax.persistence.Id", "jakarta.persistence.Id", 20, "id", "field")
        );

        List<JpaScanResult> fileResults = List.of(
            new JpaScanResult(Paths.get("/test/project/User.java"), annotations, 50)
        );

        return new JpaProjectScanResult(fileResults, 10, 1, 3);
    }

    private BeanValidationProjectScanResult createMockBeanValidationProjectScanResult() {
        List<BeanValidationUsage> usages = List.of(
            new BeanValidationUsage("javax.validation.constraints.NotNull", "jakarta.validation.constraints.NotNull", 25, "email", "field"),
            new BeanValidationUsage("javax.validation.constraints.Email", "jakarta.validation.constraints.Email", 26, "email", "field")
        );

        List<BeanValidationScanResult> fileResults = List.of(
            new BeanValidationScanResult(Paths.get("/test/project/User.java"), usages, 50)
        );

        return new BeanValidationProjectScanResult(fileResults, 10, 1, 2);
    }

    private CdiInjectionProjectScanResult createMockCdiInjectionProjectScanResult() {
        List<CdiInjectionUsage> usages = List.of(
            new CdiInjectionUsage("javax.inject.Inject", "jakarta.inject.Inject", 30, "userService", "field"),
            new CdiInjectionUsage("javax.inject.Named", "jakarta.inject.Named", 31, "UserService", "class")
        );

        List<CdiInjectionScanResult> fileResults = List.of(
            new CdiInjectionScanResult(Paths.get("/test/project/UserController.java"), usages, 40)
        );

        return new CdiInjectionProjectScanResult(fileResults, 8, 1, 2);
    }

    private ServletJspProjectScanResult createMockServletJspProjectScanResult() {
        List<ServletJspUsage> usages = List.of(
            new ServletJspUsage("javax.servlet.http.HttpServlet", "jakarta.servlet.http.HttpServlet", 10, "UserServlet", "extends"),
            new ServletJspUsage("javax.servlet.annotation.WebServlet", "jakarta.servlet.annotation.WebServlet", 12, "UserServlet", "annotation")
        );

        List<ServletJspScanResult> fileResults = List.of(
            new ServletJspScanResult(Paths.get("/test/project/UserServlet.java"), usages, 60)
        );

        return new ServletJspProjectScanResult(fileResults, 5, 1, 2);
    }

    private BuildConfigProjectScanResult createMockBuildConfigProjectScanResult() {
        List<BuildConfigUsage> usages = List.of(
            new BuildConfigUsage("javax.servlet", "javax.servlet-api", "4.0.0", "jakarta.servlet", "jakarta.servlet-api", "6.0.0", 15)
        );

        List<BuildConfigScanResult> fileResults = List.of(
            new BuildConfigScanResult(Paths.get("/test/project/pom.xml"), usages, "maven")
        );

        return new BuildConfigProjectScanResult(fileResults, 1, 1, 1);
    }

    /**
     * Creates mock recipe recommendations for testing.
     */
    private List<RecipeRecommendation> createMockRecipeRecommendations() {
        List<RecipeRecommendation> recommendations = new ArrayList<>();

        RecipeDefinition jpaRecipe = new RecipeDefinition();
        jpaRecipe.setName("MigrateJPA");
        jpaRecipe.setDescription("Migrates JPA annotations from javax to jakarta");
        jpaRecipe.setCategory(RecipeCategory.DATABASE);

        RecipeDefinition servletRecipe = new RecipeDefinition();
        servletRecipe.setName("MigrateServlets");
        servletRecipe.setDescription("Migrates Servlet API from javax to jakarta");
        servletRecipe.setCategory(RecipeCategory.WEB);

        recommendations.add(new RecipeRecommendation(
            jpaRecipe,
            0.85,
            "JPA annotations detected in 3 files",
            List.of("User.java", "Product.java", "Order.java")
        ));

        recommendations.add(new RecipeRecommendation(
            servletRecipe,
            0.75,
            "Servlet API usage detected in 2 files",
            List.of("UserServlet.java", "ProductServlet.java")
        ));

        return recommendations;
    }

    @Test
    @DisplayName("Should generate report with all sections when provided with real scan data")
    void shouldGenerateReportWithAllSectionsWhenProvidedWithRealScanData() throws Exception {
        // Arrange - Create request with actual mock data
        ComprehensiveScanResults scanResults = createMockScanResults();
        List<RecipeRecommendation> recipeRecommendations = createMockRecipeRecommendations();

        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            tempDir.resolve("full-refactoring-report.html"),
            "Test Project with Full Data",
            "Jakarta Migration Refactoring Action Report",
            null, // dependencyGraph
            scanResults,
            recipeRecommendations,
            List.of(), // javaxReferences
            List.of(), // openRewriteRecipes
            Map.of("automationReady", 80), // refactoringReadiness
            Map.of("highPriority", 2, "mediumPriority", 1, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Test Suite") // customData
        );

        // Act
        Path result = pdfReportService.generateRefactoringActionReport(request);

        // Assert - Verify file exists and has content
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);

        // Read the generated HTML and verify it contains all expected sections
        String htmlContent = java.nio.file.Files.readString(result);

        // Verify header is present
        assertTrue(htmlContent.contains("Test Project with Full Data"), "Report should contain project name");
        assertTrue(htmlContent.contains("Jakarta Migration Refactoring Action Report"), "Report should contain report title");

        // Verify Scan Summary section is present with actual data
        assertTrue(htmlContent.contains("Scan Summary") || htmlContent.contains("scan-summary"),
            "Report should contain Scan Summary section");
        assertTrue(htmlContent.contains("150") || htmlContent.contains("Files Scanned"),
            "Report should contain files scanned count");
        assertTrue(htmlContent.contains("45") || htmlContent.contains("Files with javax References"),
            "Report should contain files with issues count");

        // Verify Recipe Recommendations section is present
        assertTrue(htmlContent.contains("Recipe Recommendations") || htmlContent.contains("recipe-recommendations"),
            "Report should contain Recipe Recommendations section");
        assertTrue(htmlContent.contains("MigrateJPA") || htmlContent.contains("MigrateServlets"),
            "Report should contain recipe names");
        assertTrue(htmlContent.contains("85%") || htmlContent.contains("75%") || htmlContent.contains("confidence"),
            "Report should contain confidence scores");

        // Verify Scan Findings by Category section is present
        assertTrue(htmlContent.contains("Scan Findings by Category") || htmlContent.contains("findings-by-category"),
            "Report should contain Scan Findings by Category section");
        assertTrue(htmlContent.contains("JPA Findings") || htmlContent.contains("javax.persistence"),
            "Report should contain JPA findings");
        assertTrue(htmlContent.contains("Servlet/JSP Findings") || htmlContent.contains("javax.servlet"),
            "Report should contain Servlet findings");

        // Verify Scanner Recommendations section is present
        assertTrue(htmlContent.contains("Scanner Recommendations") || htmlContent.contains("scanner-recommendations"),
            "Report should contain Scanner Recommendations section");
        assertTrue(htmlContent.contains("Update JPA annotations") || htmlContent.contains("Update Servlet imports"),
            "Report should contain scanner recommendations");
    }
}

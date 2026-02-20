package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.advancedscanning.domain.BeanValidationProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JpaProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ServletJspProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.AdvancedScanningModule;
import adrianmikula.jakartamigration.advancedscanning.service.BeanValidationScanner;
import adrianmikula.jakartamigration.advancedscanning.service.JpaAnnotationScanner;
import adrianmikula.jakartamigration.advancedscanning.service.ServletJspScanner;
import com.intellij.openapi.diagnostic.Logger;

import java.nio.file.Path;

/**
 * Service for performing advanced scanning using premium core engine.
 * This service provides access to the premium scanning features.
 */
public class AdvancedScanningService {
    private static final Logger LOG = Logger.getInstance(AdvancedScanningService.class);

    private final AdvancedScanningModule scanningModule;
    private final JpaAnnotationScanner jpaScanner;
    private final BeanValidationScanner beanValidationScanner;
    private final ServletJspScanner servletJspScanner;

    public AdvancedScanningService() {
        // Initialize the scanning module
        this.scanningModule = new AdvancedScanningModule();
        this.jpaScanner = scanningModule.getJpaAnnotationScanner();
        this.beanValidationScanner = scanningModule.getBeanValidationScanner();
        this.servletJspScanner = scanningModule.getServletJspScanner();

        LOG.info("AdvancedScanningService initialized with premium scanning features");
    }

    /**
     * Scans a project for JPA/Hibernate annotations.
     *
     * @param projectPath Path to the project root directory
     * @return JpaProjectScanResult containing the analysis results
     */
    public JpaProjectScanResult scanForJpaAnnotations(Path projectPath) {
        LOG.info("Scanning for JPA annotations in: " + projectPath);
        return jpaScanner.scanProject(projectPath);
    }

    /**
     * Scans a project for Bean Validation constraints.
     *
     * @param projectPath Path to the project root directory
     * @return BeanValidationProjectScanResult containing the analysis results
     */
    public BeanValidationProjectScanResult scanForBeanValidation(Path projectPath) {
        LOG.info("Scanning for Bean Validation in: " + projectPath);
        return beanValidationScanner.scanProject(projectPath);
    }

    /**
     * Scans a project for Servlet/JSP usage.
     *
     * @param projectPath Path to the project root directory
     * @return ServletJspProjectScanResult containing the analysis results
     */
    public ServletJspProjectScanResult scanForServletJsp(Path projectPath) {
        LOG.info("Scanning for Servlet/JSP in: " + projectPath);
        return servletJspScanner.scanProject(projectPath);
    }

    /**
     * Scans a project for all advanced scanning types.
     *
     * @param projectPath Path to the project root directory
     * @return AdvancedScanSummary containing combined results
     */
    public AdvancedScanSummary scanAll(Path projectPath) {
        LOG.info("Running all advanced scans in: " + projectPath);
        
        JpaProjectScanResult jpaResult = scanForJpaAnnotations(projectPath);
        BeanValidationProjectScanResult beanValidationResult = scanForBeanValidation(projectPath);
        ServletJspProjectScanResult servletJspResult = scanForServletJsp(projectPath);
        
        return new AdvancedScanSummary(
            jpaResult,
            beanValidationResult,
            servletJspResult
        );
    }

    /**
     * Summary of all advanced scanning results.
     */
    public record AdvancedScanSummary(
        JpaProjectScanResult jpaResult,
        BeanValidationProjectScanResult beanValidationResult,
        ServletJspProjectScanResult servletJspResult
    ) {
        /**
         * Returns the total number of issues found across all scans.
         */
        public int getTotalIssuesFound() {
            int total = 0;
            if (jpaResult != null) {
                total += jpaResult.totalAnnotationsFound();
            }
            if (beanValidationResult != null) {
                total += beanValidationResult.totalAnnotationsFound();
            }
            if (servletJspResult != null) {
                total += servletJspResult.totalUsagesFound();
            }
            return total;
        }

        /**
         * Returns true if any issues were found.
         */
        public boolean hasIssues() {
            return getTotalIssuesFound() > 0;
        }
    }
}

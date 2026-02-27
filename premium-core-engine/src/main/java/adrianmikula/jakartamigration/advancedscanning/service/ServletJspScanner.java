package adrianmikula.jakartamigration.advancedscanning.service;

import adrianmikula.jakartamigration.advancedscanning.domain.ServletJspProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ServletJspScanResult;

import java.nio.file.Path;

/**
 * Service for scanning source code for javax.servlet.*, javax.servlet.jsp.*, and EL usage.
 * This is a premium feature that provides detailed analysis of web application components.
 */
public interface ServletJspScanner {

    /**
     * Scans a project for javax.servlet.* usage in source code and JSP files.
     *
     * @param projectPath Path to the project root directory
     * @return Project scan result with all files containing javax.servlet.* usage
     */
    ServletJspProjectScanResult scanProject(Path projectPath);

    /**
     * Scans a single file for javax.servlet.* usage.
     *
     * @param filePath Path to the file to scan (Java or JSP)
     * @return File scan result with servlet/JSP usages found
     */
    ServletJspScanResult scanFile(Path filePath);
}

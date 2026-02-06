package adrianmikula.jakartamigration.sourcecodescanning.service;

import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;

import java.nio.file.Path;

/**
 * Service for scanning source code for javax.* usage.
 */
public interface SourceCodeScanner {
    
    /**
     * Scans a project for javax.* usage in source code.
     *
     * @param projectPath Path to the project root directory
     * @return Analysis result with all files containing javax.* usage
     */
    SourceCodeAnalysisResult scanProject(Path projectPath);
    
    /**
     * Scans a single file for javax.* usage.
     *
     * @param filePath Path to the Java file to scan
     * @return File usage information
     */
    adrianmikula.jakartamigration.sourcecodescanning.domain.FileUsage scanFile(Path filePath);
    
    /**
     * Scans XML files for javax.* usage in namespaces and class references.
     *
     * @param projectPath Path to the project root directory
     * @return List of XML files with javax.* usage
     */
    java.util.List<adrianmikula.jakartamigration.sourcecodescanning.domain.XmlFileUsage> scanXmlFiles(java.nio.file.Path projectPath);
}


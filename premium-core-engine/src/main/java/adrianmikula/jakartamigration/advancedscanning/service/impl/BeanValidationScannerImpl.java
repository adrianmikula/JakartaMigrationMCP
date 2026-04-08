package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.JavaxUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.BaseScanner;
import adrianmikula.jakartamigration.advancedscanning.service.BeanValidationScanner;
import lombok.extern.slf4j.Slf4j;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.CompilationUnit;
import org.openrewrite.SourceFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of BeanValidationScanner using OpenRewrite JavaParser.
 * Provides AST-based scanning for javax.validation.* annotations.
 */
@Slf4j
public class BeanValidationScannerImpl extends BaseScanner<JavaxUsage> implements BeanValidationScanner {

    private static final Map<String, String> BEAN_VALIDATION_MAPPINGS = new HashMap<>();

    static {
        // Constraint annotations
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.NotNull", "jakarta.validation.constraints.NotNull");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.Null", "jakarta.validation.constraints.Null");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.NotBlank", "jakarta.validation.constraints.NotBlank");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.NotEmpty", "jakarta.validation.constraints.NotEmpty");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.Size", "jakarta.validation.constraints.Size");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.Min", "jakarta.validation.constraints.Min");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.Max", "jakarta.validation.constraints.Max");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.DecimalMin", "jakarta.validation.constraints.DecimalMin");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.DecimalMax", "jakarta.validation.constraints.DecimalMax");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.Digits", "jakarta.validation.constraints.Digits");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.Positive", "jakarta.validation.constraints.Positive");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.PositiveOrZero", "jakarta.validation.constraints.PositiveOrZero");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.Negative", "jakarta.validation.constraints.Negative");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.NegativeOrZero", "jakarta.validation.constraints.NegativeOrZero");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.Past", "jakarta.validation.constraints.Past");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.PastOrPresent", "jakarta.validation.constraints.PastOrPresent");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.Future", "jakarta.validation.constraints.Future");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.FutureOrPresent", "jakarta.validation.constraints.FutureOrPresent");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.Pattern", "jakarta.validation.constraints.Pattern");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.Email", "jakarta.validation.constraints.Email");

        // Constraint composition
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.constraints.List", "jakarta.validation.constraints.List");

        // Payload and Constraint definition
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.Payload", "jakarta.validation.Payload");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.Constraint", "jakarta.validation.Constraint");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.ConstraintValidator", "jakarta.validation.ConstraintValidator");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.ConstraintViolation", "jakarta.validation.ConstraintViolation");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.Validation", "jakarta.validation.Validation");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.Validator", "jakarta.validation.Validator");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.ValidatorFactory", "jakarta.validation.ValidatorFactory");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.groups.Default", "jakarta.validation.groups.Default");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.GroupSequence", "jakarta.validation.GroupSequence");

        // Hibernate-specific
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.NotBlank", "jakarta.validation.constraints.NotBlank");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.NotEmpty", "jakarta.validation.constraints.NotEmpty");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.Email", "jakarta.validation.constraints.Email");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.Length", "jakarta.validation.constraints.Length");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.Range", "jakarta.validation.constraints.Range");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.URL", "jakarta.validation.constraints.URL");
    }

    @Override
    public ProjectScanResult<FileScanResult<JavaxUsage>> scanProject(Path projectPath) {
        return scanProjectGeneric(projectPath, "Bean Validation");
    }

    @Override
    public FileScanResult<JavaxUsage> scanFile(Path filePath) {
        Path validatedPath = validateFilePath(filePath);
        if (validatedPath == null) {
            return FileScanResult.empty(filePath);
        }

        try {
            String content = Files.readString(validatedPath);
            int lineCount = countLines(content);

            JavaParser parser = javaParserThreadLocal.get();
            parser.reset();

            List<SourceFile> sourceFiles = parser.parse(content).collect(java.util.stream.Collectors.toList());

            if (sourceFiles.isEmpty()) {
                return FileScanResult.empty(filePath);
            }

            List<JavaxUsage> usages = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile instanceof CompilationUnit cu) {
                    usages.addAll(extractUsages(cu, content));
                }
            }

            return new FileScanResult<>(filePath, usages, lineCount);

        } catch (Exception e) {
            log.warn("Error scanning file for Bean Validation: {}", filePath, e);
            return FileScanResult.empty(filePath);
        }
    }

    private List<JavaxUsage> extractUsages(CompilationUnit cu, String content) {
        List<JavaxUsage> usages = new ArrayList<>();
        String[] lines = content.split("\n");

        // Check imports
        for (J.Import imp : cu.getImports()) {
            String importName = imp.getQualid().toString();
            if (importName.startsWith("javax.validation.") || importName.startsWith("org.hibernate.validator.constraints.")) {
                String jakartaEquivalent = BEAN_VALIDATION_MAPPINGS.get(importName);
                int lineNumber = findLineNumber(lines, importName);
                usages.add(new JavaxUsage(importName, jakartaEquivalent != null ? jakartaEquivalent : importName.replace("javax.", "jakarta."), lineNumber, "import"));
            }
        }

        // Search for annotation usages
        Pattern annotationPattern = Pattern.compile("@((?:javax\\.validation[\\w.]*|org\\.hibernate\\.validator\\.constraints[\\w.]*))");
        Matcher matcher = annotationPattern.matcher(content);

        while (matcher.find()) {
            String annotationName = matcher.group(1);
            if (annotationName.startsWith("javax.validation") || annotationName.startsWith("org.hibernate.validator")) {
                String jakartaEquivalent = BEAN_VALIDATION_MAPPINGS.get(annotationName);
                int lineNumber = findLineNumber(lines, matcher.group(0));
                usages.add(new JavaxUsage(annotationName, jakartaEquivalent != null ? jakartaEquivalent : annotationName.replace("javax.", "jakarta."), lineNumber, "annotation"));
            }
        }

        return usages;
    }
}

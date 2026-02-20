package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.BeanValidationProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.BeanValidationScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.BeanValidationUsage;
import adrianmikula.jakartamigration.advancedscanning.service.BeanValidationScanner;
import lombok.extern.slf4j.Slf4j;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.CompilationUnit;
import org.openrewrite.SourceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of BeanValidationScanner using OpenRewrite JavaParser.
 * Provides AST-based scanning for javax.validation.* annotations.
 */
@Slf4j
public class BeanValidationScannerImpl implements BeanValidationScanner {

    // Map of javax.validation annotations to their Jakarta equivalents
    private static final Map<String, String> BEAN_VALIDATION_MAPPINGS = new HashMap<>();
    
    static {
        // Built-in constraint annotations
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
        
        // Payload
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.Payload", "jakarta.validation.Payload");
        
        // Constraint definition
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.Constraint", "jakarta.validation.Constraint");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.ConstraintValidator", "jakarta.validation.ConstraintValidator");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.ConstraintValidatorContext", "jakarta.validation.ConstraintValidatorContext");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.ConstraintViolationException", "jakarta.validation.ConstraintViolationException");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.ValidationException", "jakarta.validation.ValidationException");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.UnexpectedTypeException", "jakarta.validation.UnexpectedTypeException");
        
        // Validation helper classes
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.Validation", "jakarta.validation.Validation");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.Validator", "jakarta.validation.Validator");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.ValidatorFactory", "jakarta.validation.ValidatorFactory");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.ValidatorContext", "jakarta.validation.ValidatorContext");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.MessageInterpolator", "jakarta.validation.MessageInterpolator");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.ParameterNameProvider", "jakarta.validation.ParameterNameProvider");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.TraversableResolver", "jakarta.validation.TraversableResolver");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.ClockProvider", "jakarta.validation.ClockProvider");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.ValueExtractor", "jakarta.validation.ValueExtractor");
        
        // Constraint violation
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.ConstraintViolation", "jakarta.validation.ConstraintViolation");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.Path", "jakarta.validation.Path");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.Payload", "jakarta.validation.Payload");
        
        // Groups
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.groups.Default", "jakarta.validation.groups.Default");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.GroupSequence", "jakarta.validation.GroupSequence");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.GroupSequenceProvider", "jakarta.validation.GroupSequenceProvider");
        
        // XML configuration
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.ValidationFactory", "jakarta.validation.ValidationFactory");
        BEAN_VALIDATION_MAPPINGS.put("javax.validation.Configuration", "jakarta.validation.Configuration");
        
        // Hibernate-specific constraints (if present)
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.CreditCardNumber", "jakarta.validation.constraints.CreditCardNumber");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.Currency", "jakarta.validation.constraints.Currency");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.Email", "jakarta.validation.constraints.Email");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.Length", "jakarta.validation.constraints.Length");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.LuhnCheck", "jakarta.validation.constraints.LuhnCheck");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.Mod10Check", "jakarta.validation.constraints.Mod10Check");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.Mod11Check", "jakarta.validation.constraints.Mod11Check");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.NotBlank", "jakarta.validation.constraints.NotBlank");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.NotEmpty", "jakarta.validation.constraints.NotEmpty");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.Range", "jakarta.validation.constraints.Range");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.SafeHtml", "jakarta.validation.constraints.SafeHtml");
        BEAN_VALIDATION_MAPPINGS.put("org.hibernate.validator.constraints.URL", "jakarta.validation.constraints.URL");
    }

    // Use ThreadLocal to avoid JavaParser reset() issues when parsing files
    private final ThreadLocal<JavaParser> javaParserThreadLocal = ThreadLocal.withInitial(() ->
        JavaParser.fromJavaVersion().build()
    );

    @Override
    public BeanValidationProjectScanResult scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            log.warn("Invalid project path: {}", projectPath);
            return BeanValidationProjectScanResult.empty();
        }

        try {
            // Discover all Java files
            List<Path> javaFiles = discoverJavaFiles(projectPath);

            if (javaFiles.isEmpty()) {
                log.info("No Java files found in project: {}", projectPath);
                return BeanValidationProjectScanResult.empty();
            }

            log.info("Scanning {} Java files for Bean Validation in project: {}", javaFiles.size(), projectPath);

            // Scan files in parallel
            AtomicInteger totalScanned = new AtomicInteger(0);
            List<BeanValidationScanResult> results = javaFiles.parallelStream()
                .map(file -> {
                    totalScanned.incrementAndGet();
                    BeanValidationScanResult result = scanFile(file);
                    if (result.hasJavaxUsage()) {
                        log.debug("Found Bean Validation in: {}", file);
                        return result;
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

            int totalAnnotations = results.stream()
                .mapToInt(r -> r.annotations().size())
                .sum();

            log.info("Bean Validation scan complete: {} files scanned, {} files with usage, {} total annotations",
                totalScanned.get(), results.size(), totalAnnotations);

            return new BeanValidationProjectScanResult(
                results,
                totalScanned.get(),
                results.size(),
                totalAnnotations
            );

        } catch (Exception e) {
            log.error("Error scanning project for Bean Validation: {}", projectPath, e);
            return BeanValidationProjectScanResult.empty();
        }
    }

    @Override
    public BeanValidationScanResult scanFile(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("filePath cannot be null");
        }
        if (!Files.exists(filePath)) {
            log.warn("File does not exist: {}", filePath);
            return BeanValidationScanResult.empty(filePath);
        }

        try {
            String content = Files.readString(filePath);
            int lineCount = countLines(content);

            JavaParser parser = javaParserThreadLocal.get();
            parser.reset();

            List<SourceFile> sourceFiles = parser.parse(content).collect(java.util.stream.Collectors.toList());

            if (sourceFiles.isEmpty()) {
                log.debug("No source files found in file: {}", filePath);
                return BeanValidationScanResult.empty(filePath);
            }

            // Extract Bean Validation annotations from all compilation units
            List<BeanValidationUsage> annotations = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile instanceof CompilationUnit) {
                    CompilationUnit cu = (CompilationUnit) sourceFile;
                    annotations.addAll(extractBeanValidationAnnotations(cu, content));
                }
            }

            return new BeanValidationScanResult(filePath, annotations, lineCount);

        } catch (Exception e) {
            log.warn("Error scanning file for Bean Validation: {}", filePath, e);
            return BeanValidationScanResult.empty(filePath);
        }
    }

    /**
     * Discovers all Java files in the project, excluding build directories.
     */
    private List<Path> discoverJavaFiles(Path projectPath) {
        List<Path> javaFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(this::shouldScanFile)
                .forEach(javaFiles::add);
        } catch (IOException e) {
            log.error("Error discovering Java files in: {}", projectPath, e);
        }

        return javaFiles;
    }

    /**
     * Determines if a file should be scanned (excludes build directories only).
     */
    private boolean shouldScanFile(Path file) {
        String path = file.toString().replace('\\', '/');
        return !path.contains("/target/") &&
               !path.contains("/build/") &&
               !path.contains("/.git/") &&
               !path.contains("/node_modules/") &&
               !path.contains("/.gradle/") &&
               !path.contains("/.mvn/") &&
               !path.contains("/.idea/") &&
               !path.contains("/.vscode/") &&
               !path.contains("/out/") &&
               !path.contains("/bin/");
    }

    /**
     * Extracts javax.validation.* annotations from a compilation unit.
     */
    private List<BeanValidationUsage> extractBeanValidationAnnotations(CompilationUnit cu, String content) {
        List<BeanValidationUsage> annotations = new ArrayList<>();
        
        String[] lines = content.split("\n");
        
        // Check imports first
        for (J.Import imp : cu.getImports()) {
            String importName = imp.getQualid().toString();
            
            if (importName.startsWith("javax.validation.") || 
                importName.startsWith("org.hibernate.validator.constraints.")) {
                
                String jakartaEquivalent = BEAN_VALIDATION_MAPPINGS.get(importName);
                
                // Find line number
                int lineNumber = findLineNumberInContent(lines, importName);
                
                annotations.add(new BeanValidationUsage(
                    importName,
                    jakartaEquivalent != null ? jakartaEquivalent : importName.replace("javax.", "jakarta."),
                    lineNumber,
                    "import",
                    "import"
                ));
            }
        }
        
        // Search for annotation usages in content using regex
        // Pattern: @javax.validation.constraints.SomeAnnotation
        Pattern annotationPattern = Pattern.compile("@((?:javax\\.validation[\\w.]*|org\\.hibernate\\.validator\\.constraints[\\w.]*|\\w+(?=\\s*\\()))");
        Matcher matcher = annotationPattern.matcher(content);
        
        while (matcher.find()) {
            String annotationName = matcher.group(1);
            
            // Skip if it doesn't start with javax.validation or org.hibernate.validator
            if (!annotationName.startsWith("javax.validation") && !annotationName.startsWith("org.hibernate.validator")) {
                continue;
            }
            
            String jakartaEquivalent = BEAN_VALIDATION_MAPPINGS.get(annotationName);
            
            // Find line number
            int lineNumber = findLineNumberInContent(lines, matcher.group(0));
            
            annotations.add(new BeanValidationUsage(
                annotationName,
                jakartaEquivalent != null ? jakartaEquivalent : annotationName.replace("javax.", "jakarta."),
                lineNumber,
                "annotation",
                "annotation"
            ));
        }
        
        return annotations;
    }

    /**
     * Finds the line number of a string in the content.
     */
    private int findLineNumberInContent(String[] lines, String searchText) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(searchText)) {
                return i + 1;
            }
        }
        return 1;
    }

    /**
     * Counts lines in content.
     */
    private int countLines(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.split("\n").length;
    }
}

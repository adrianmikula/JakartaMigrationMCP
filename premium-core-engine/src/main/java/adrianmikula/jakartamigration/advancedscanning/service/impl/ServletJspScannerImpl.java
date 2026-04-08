package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ServletJspUsage;
import adrianmikula.jakartamigration.advancedscanning.service.BaseScanner;
import adrianmikula.jakartamigration.advancedscanning.service.ServletJspScanner;
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
import java.util.stream.Collectors;

/**
 * Implementation of ServletJspScanner using OpenRewrite JavaParser.
 * Provides AST-based scanning for javax.servlet.* and javax.servlet.jsp.*
 * usage.
 */
@Slf4j
public class ServletJspScannerImpl extends BaseScanner<ServletJspUsage> implements ServletJspScanner {

    private static final Map<String, String> SERVLET_MAPPINGS = new HashMap<>();

    static {
        // Core Servlet API
        SERVLET_MAPPINGS.put("javax.servlet.Servlet", "jakarta.servlet.Servlet");
        SERVLET_MAPPINGS.put("javax.servlet.ServletConfig", "jakarta.servlet.ServletConfig");
        SERVLET_MAPPINGS.put("javax.servlet.ServletContext", "jakarta.servlet.ServletContext");
        SERVLET_MAPPINGS.put("javax.servlet.ServletRequest", "jakarta.servlet.ServletRequest");
        SERVLET_MAPPINGS.put("javax.servlet.ServletResponse", "jakarta.servlet.ServletResponse");
        SERVLET_MAPPINGS.put("javax.servlet.ServletException", "jakarta.servlet.ServletException");
        SERVLET_MAPPINGS.put("javax.servlet.SingleThreadModel", "jakarta.servlet.SingleThreadModel");
        SERVLET_MAPPINGS.put("javax.servlet.UnavailableException", "jakarta.servlet.UnavailableException");

        // Servlet Container
        SERVLET_MAPPINGS.put("javax.servlet.http.HttpServlet", "jakarta.servlet.http.HttpServlet");
        SERVLET_MAPPINGS.put("javax.servlet.http.HttpServletRequest", "jakarta.servlet.http.HttpServletRequest");
        SERVLET_MAPPINGS.put("javax.servlet.http.HttpServletResponse", "jakarta.servlet.http.HttpServletResponse");
        SERVLET_MAPPINGS.put("javax.servlet.http.HttpSession", "jakarta.servlet.http.HttpSession");
        SERVLET_MAPPINGS.put("javax.servlet.http.Cookie", "jakarta.servlet.http.Cookie");
        SERVLET_MAPPINGS.put("javax.servlet.http.HttpUtils", "jakarta.servlet.http.HttpUtils");

        // Filters
        SERVLET_MAPPINGS.put("javax.servlet.Filter", "jakarta.servlet.Filter");
        SERVLET_MAPPINGS.put("javax.servlet.FilterChain", "jakarta.servlet.FilterChain");
        SERVLET_MAPPINGS.put("javax.servlet.FilterConfig", "jakarta.servlet.FilterConfig");

        // Listeners
        SERVLET_MAPPINGS.put("javax.servlet.ServletContextListener", "jakarta.servlet.ServletContextListener");
        SERVLET_MAPPINGS.put("javax.servlet.ServletContextAttributeListener",
                "jakarta.servlet.ServletContextAttributeListener");
        SERVLET_MAPPINGS.put("javax.servlet.ServletRequestListener", "jakarta.servlet.ServletRequestListener");
        SERVLET_MAPPINGS.put("javax.servlet.ServletRequestAttributeListener",
                "jakarta.servlet.ServletRequestAttributeListener");
        SERVLET_MAPPINGS.put("javax.servlet.http.HttpSessionListener", "jakarta.servlet.http.HttpSessionListener");
        SERVLET_MAPPINGS.put("javax.servlet.http.HttpSessionAttributeListener",
                "jakarta.servlet.http.HttpSessionAttributeListener");
        SERVLET_MAPPINGS.put("javax.servlet.http.HttpSessionBindingListener",
                "jakarta.servlet.http.HttpSessionBindingListener");
        SERVLET_MAPPINGS.put("javax.servlet.http.HttpSessionActivationListener",
                "jakarta.servlet.http.HttpSessionActivationListener");

        // Servlet Container Lifecycle
        SERVLET_MAPPINGS.put("javax.servlet.ServletContextEvent", "jakarta.servlet.ServletContextEvent");
        SERVLET_MAPPINGS.put("javax.servlet.ServletContextAttributeEvent",
                "jakarta.servlet.ServletContextAttributeEvent");
        SERVLET_MAPPINGS.put("javax.servlet.ServletRequestEvent", "jakarta.servlet.ServletRequestEvent");
        SERVLET_MAPPINGS.put("javax.servlet.ServletRequestAttributeEvent",
                "jakarta.servlet.ServletRequestAttributeEvent");
        SERVLET_MAPPINGS.put("javax.servlet.http.HttpSessionEvent", "jakarta.servlet.http.HttpSessionEvent");
        SERVLET_MAPPINGS.put("javax.servlet.http.HttpSessionBindingEvent",
                "jakarta.servlet.http.HttpSessionBindingEvent");

        // Async
        SERVLET_MAPPINGS.put("javax.servlet.AsyncEvent", "jakarta.servlet.AsyncEvent");
        SERVLET_MAPPINGS.put("javax.servlet.AsyncListener", "jakarta.servlet.AsyncListener");
        SERVLET_MAPPINGS.put("javax.servlet.AsyncContext", "jakarta.servlet.AsyncContext");
        SERVLET_MAPPINGS.put("javax.servlet.ReadListener", "jakarta.servlet.ReadListener");
        SERVLET_MAPPINGS.put("javax.servlet.WriteListener", "jakarta.servlet.WriteListener");

        // JSP (deprecated in Jakarta EE 9+, but still scanned)
        SERVLET_MAPPINGS.put("javax.servlet.jsp.JspPage", "jakarta.servlet.jsp.JspPage");
        SERVLET_MAPPINGS.put("javax.servlet.jsp.HttpJspPage", "jakarta.servlet.jsp.HttpJspPage");
        SERVLET_MAPPINGS.put("javax.servlet.jsp.JspWriter", "jakarta.servlet.jsp.JspWriter");
        SERVLET_MAPPINGS.put("javax.servlet.jsp.PageContext", "jakarta.servlet.jsp.PageContext");
        SERVLET_MAPPINGS.put("javax.servlet.jsp.JspException", "jakarta.servlet.jsp.JspException");
        SERVLET_MAPPINGS.put("javax.servlet.jsp.JspTagException", "jakarta.servlet.jsp.JspTagException");

        // JSP Tags
        SERVLET_MAPPINGS.put("javax.servlet.jsp.tagext.BodyTagSupport", "jakarta.servlet.jsp.tagext.BodyTagSupport");
        SERVLET_MAPPINGS.put("javax.servlet.jsp.tagext.IterationTag", "jakarta.servlet.jsp.tagext.IterationTag");
        SERVLET_MAPPINGS.put("javax.servlet.jsp.tagext.TagSupport", "jakarta.servlet.jsp.tagext.TagSupport");
        SERVLET_MAPPINGS.put("javax.servlet.jsp.tagext.Tag", "jakarta.servlet.jsp.tagext.Tag");
        SERVLET_MAPPINGS.put("javax.servlet.jsp.tagext.BodyTag", "jakarta.servlet.jsp.tagext.BodyTag");

        // JSP Attributes
        SERVLET_MAPPINGS.put("javax.servlet.jsp.tagext.VariableInfo", "jakarta.servlet.jsp.tagext.VariableInfo");
        SERVLET_MAPPINGS.put("javax.servlet.jsp.tagext.TryCatchFinally", "jakarta.servlet.jsp.tagext.TryCatchFinally");

        // JSP EL
        SERVLET_MAPPINGS.put("javax.el.ELContext", "jakarta.el.ELContext");
        SERVLET_MAPPINGS.put("javax.el.ELResolver", "jakarta.el.ELResolver");
        SERVLET_MAPPINGS.put("javax.el.ExpressionFactory", "jakarta.el.ExpressionFactory");
        SERVLET_MAPPINGS.put("javax.el.ValueExpression", "jakarta.el.ValueExpression");
        SERVLET_MAPPINGS.put("javax.el.MethodExpression", "jakarta.el.MethodExpression");
        SERVLET_MAPPINGS.put("javax.el.ELException", "jakarta.el.ELException");
        SERVLET_MAPPINGS.put("javax.el.ArrayELResolver", "jakarta.el.ArrayELResolver");
        SERVLET_MAPPINGS.put("javax.el.BeanELResolver", "jakarta.el.BeanELResolver");
        SERVLET_MAPPINGS.put("javax.el.CompositeELResolver", "jakarta.el.CompositeELResolver");
        SERVLET_MAPPINGS.put("javax.el.ListELResolver", "jakarta.el.ListELResolver");
        SERVLET_MAPPINGS.put("javax.el.MapELResolver", "jakarta.el.MapELResolver");
    }

    @Override
    public ProjectScanResult<FileScanResult<ServletJspUsage>> scanProject(Path projectPath) {
        return scanProjectGeneric(projectPath, "Servlet/JSP");
    }

    @Override
    public FileScanResult<ServletJspUsage> scanFile(Path filePath) {
        Path validatedPath = validateFilePath(filePath);
        if (validatedPath == null) {
            return FileScanResult.empty(filePath);
        }

        // Skip files in temporary or system directories
        String fullPath = validatedPath.toString().toLowerCase();
        if (fullPath.contains("tmp") || fullPath.contains("temp") || 
            fullPath.contains("idea-sandbox") || fullPath.contains("system/")) {
            return FileScanResult.empty(filePath);
        }

        String fileName = validatedPath.getFileName().toString();
        if (fileName.endsWith(".jsp")) {
            return scanJspFile(validatedPath);
        } else {
            return scanJavaFile(validatedPath);
        }
    }

    private FileScanResult<ServletJspUsage> scanJavaFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            int lineCount = countLines(content);

            JavaParser parser = javaParserThreadLocal.get();
            parser.reset();

            List<SourceFile> sourceFiles = parser.parse(content).collect(Collectors.toList());

            if (sourceFiles.isEmpty()) {
                log.debug("No source files found in file: {}", filePath);
                return FileScanResult.empty(filePath);
            }

            List<ServletJspUsage> usages = new ArrayList<>();
            for (SourceFile sourceFile : sourceFiles) {
                if (sourceFile instanceof CompilationUnit cu) {
                    usages.addAll(extractServletUsages(cu, content));
                }
            }

            return new FileScanResult<>(filePath, usages, lineCount);

        } catch (Exception e) {
            log.warn("Error scanning Java file for Servlet/JSP: {}", filePath, e);
            return FileScanResult.empty(filePath);
        }
    }

    private FileScanResult<ServletJspUsage> scanJspFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            int lineCount = countLines(content);

            List<ServletJspUsage> usages = extractJspUsages(content);

            return new FileScanResult<>(filePath, usages, lineCount);

        } catch (Exception e) {
            log.warn("Error scanning JSP file for Servlet/JSP: {}", filePath, e);
            return FileScanResult.empty(filePath);
        }
    }

    /**
     * Extracts javax.servlet usages from JSP content.
     */
    private List<ServletJspUsage> extractJspUsages(String content) {
        List<ServletJspUsage> usages = new ArrayList<>();
        String[] lines = content.split("\n");

        // Match servlet class references in JSP (e.g., extends HttpServlet)
        Pattern classPattern = Pattern.compile("extends\\s+(javax\\.servlet[\\w.]+)");
        Matcher classMatcher = classPattern.matcher(content);
        while (classMatcher.find()) {
            String className = classMatcher.group(1);
            usages.add(new ServletJspUsage(
                    className,
                    SERVLET_MAPPINGS.getOrDefault(className, className.replace("javax.", "jakarta.")),
                    findLineNumber(lines, classMatcher.group(0)),
                    "class declaration",
                    "servlet"));
        }

        // Match taglib directives
        Pattern taglibPattern = Pattern.compile("taglib\\s+uri=[\"']http://java\\.sun\\.com[^\"']+[\"']");
        Matcher taglibMatcher = taglibPattern.matcher(content);
        while (taglibMatcher.find()) {
            usages.add(new ServletJspUsage(
                    "http://java.sun.com/xml/ns/javaee",
                    "https://jakarta.ee/xml/ns/jakartaee",
                    findLineNumber(lines, taglibMatcher.group(0)),
                    "taglib directive",
                    "jsp"));
        }

        return usages;
    }

    private List<ServletJspUsage> extractServletUsages(CompilationUnit cu, String content) {
        List<ServletJspUsage> usages = new ArrayList<>();
        String[] lines = content.split("\n");

        for (J.Import imp : cu.getImports()) {
            String importName = imp.getQualid().toString();

            if (importName.startsWith("javax.servlet.") || importName.startsWith("javax.el.")) {
                String jakartaEquivalent = SERVLET_MAPPINGS.get(importName);
                int lineNumber = findLineNumber(lines, importName);
                String usageType = importName.contains("jsp") ? "jsp" : importName.contains("el") ? "el" : "servlet";

                usages.add(new ServletJspUsage(
                        importName,
                        jakartaEquivalent != null ? jakartaEquivalent : importName.replace("javax.", "jakarta."),
                        lineNumber, "import", usageType));
            }
        }

        return usages;
    }
}

package adrianmikula.jakartamigration.pdfreporting.util;

import lombok.extern.slf4j.Slf4j;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for validating HTML content before PDF conversion.
 * Detects common XML parsing issues that would cause Flying Saucer to fail.
 */
@Slf4j
public class HtmlValidator {

    private static final Pattern UNESCAPED_AMPERSAND_PATTERN = Pattern.compile(" & ");
    private static final Pattern UNESCAPED_LT_PATTERN = Pattern.compile(" <[a-zA-Z]");
    private static final Pattern UNESCAPED_GT_PATTERN = Pattern.compile("[a-zA-Z]> ");
    private static final Pattern UNESCAPED_QUOTE_PATTERN = Pattern.compile("=\"[^\"]*\"[^&]");

    /**
     * Validates HTML content for XML compliance before PDF conversion.
     * 
     * @param htmlContent The HTML content to validate
     * @throws HtmlValidationException if the HTML contains XML parsing issues
     */
    public static void validateHtml(String htmlContent) throws HtmlValidationException {
        List<String> errors = new ArrayList<>();

        // Check for common XML parsing issues
        checkForUnescapedAmpersands(htmlContent, errors);
        checkForUnescapedCharacters(htmlContent, errors);
        checkXmlWellFormedness(htmlContent, errors);

        if (!errors.isEmpty()) {
            String errorMessage = String.join("; ", errors);
            log.error("HTML validation failed: {}", errorMessage);
            throw new HtmlValidationException(errorMessage);
        }

        log.debug("HTML validation passed");
    }

    /**
     * Checks for unescaped ampersands that cause XML parsing errors.
     */
    private static void checkForUnescapedAmpersands(String html, List<String> errors) {
        if (UNESCAPED_AMPERSAND_PATTERN.matcher(html).find()) {
            errors.add("Found unescaped ampersands (&) that must be escaped as &amp;");
        }
    }

    /**
     * Checks for other unescaped special characters.
     */
    private static void checkForUnescapedCharacters(String html, List<String> errors) {
        // Check for unescaped less than signs (not part of HTML tags)
        java.util.regex.Matcher ltMatcher = UNESCAPED_LT_PATTERN.matcher(html);
        while (ltMatcher.find()) {
            int pos = ltMatcher.start();
            String context = getContextSnippet(html, pos, 50);
            errors.add("Found unescaped less than sign (<) at position " + pos + " that must be escaped as &lt;: ...\"" + context + "\"...");
        }

        // Check for unescaped greater than signs (not part of HTML tags)
        java.util.regex.Matcher gtMatcher = UNESCAPED_GT_PATTERN.matcher(html);
        while (gtMatcher.find()) {
            int pos = gtMatcher.start();
            String context = getContextSnippet(html, pos, 50);
            errors.add("Found unescaped greater than sign (>) at position " + pos + " that must be escaped as &gt;: ...\"" + context + "\"...");
        }
    }
    
    /**
     * Get a context snippet around an error position for better debugging.
     */
    private static String getContextSnippet(String html, int position, int contextLength) {
        int start = Math.max(0, position - contextLength);
        int end = Math.min(html.length(), position + contextLength);
        return html.substring(start, end).replace("\n", "\\n").replace("\r", "\\r");
    }

    /**
     * Attempts to parse the HTML as XML to check for well-formedness.
     * This simulates what Flying Saucer does during PDF conversion.
     * Strips DOCTYPE declaration to avoid security conflicts while maintaining validation.
     */
    private static void checkXmlWellFormedness(String html, List<String> errors) {
        try {
            // Strip DOCTYPE declaration to avoid security conflicts
            String htmlWithoutDoctype = html.replaceAll("(?s)^<!DOCTYPE[^>]*>", "");
            
            // Pre-process to escape common XML content that might be embedded
            String processedHtml = preprocessEmbeddedXml(htmlWithoutDoctype);
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable external entity resolution for security
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler()); // Ignore warnings, fail on errors
            builder.parse(new ByteArrayInputStream(processedHtml.getBytes("UTF-8")));
            
        } catch (SAXException e) {
            String errorMsg = e.getMessage();
            // Try to extract location information from SAX error
            if (errorMsg.contains("at line") || errorMsg.contains("column")) {
                errors.add("XML parsing error: " + errorMsg);
            } else {
                errors.add("XML parsing error: " + errorMsg + ". This may be caused by unescaped XML content or malformed HTML tags.");
            }
        } catch (ParserConfigurationException | IOException e) {
            errors.add("XML validation configuration error: " + e.getMessage());
        }
    }
    
    /**
     * Pre-process HTML to escape common XML patterns that might be embedded in content.
     * This helps prevent XML fragments from causing parsing errors.
     */
    private static String preprocessEmbeddedXml(String html) {
        // Simple, reliable approach: escape problematic characters in code content
        String processed = html;
        
        // Escape < characters that appear to be part of code examples or annotations
        // Look for patterns like <SomeClass>, <variable>, <method> etc.
        processed = processed.replaceAll("<([A-Z][a-zA-Z0-9]+)>", "&lt;$1&gt;");
        processed = processed.replaceAll("</([A-Z][a-zA-Z0-9]+)>", "&lt;/$1&gt;");
        
        // Also escape mixed case patterns
        processed = processed.replaceAll("<([a-z]+[A-Z][a-zA-Z0-9]+)>", "&lt;$1&gt;");
        processed = processed.replaceAll("</([a-z]+[A-Z][a-zA-Z0-9]+)>", "&lt;/$1&gt;");
        
        // Escape any remaining < characters that aren't part of common HTML tags
        // This is a safety net for edge cases
        String[] commonHtmlTags = {"html", "head", "title", "meta", "style", "body", "div", "span", 
            "h1", "h2", "h3", "h4", "h5", "h6", "p", "br", "hr", "table", "thead", "tbody", 
            "tr", "th", "td", "ul", "ol", "li", "a", "img", "svg", "defs", "linearGradient", 
            "stop", "rect", "path", "circle", "strong", "em", "code", "pre", "blockquote"};
        
        for (String tag : commonHtmlTags) {
            // Don't escape if it's a valid HTML tag
            processed = processed.replaceAll("<(?!" + tag + "(\\s|>|/>))", "&lt;");
            processed = processed.replaceAll("(?<!/" + tag + "(\\s|>))", "&gt;");
        }
        
        return processed;
    }
    
        
    /**
     * Escape HTML content for safe display.
     * 
     * @param content The content to escape
     * @return Escaped content
     */
    private static String escapeHtml(String content) {
        if (content == null) return "";
        return content.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * Quick validation that only checks for the most common issues.
     * Use this for fast feedback during development.
     */
    public static void quickValidate(String htmlContent) throws HtmlValidationException {
        List<String> errors = new ArrayList<>();
        
        checkForUnescapedAmpersands(htmlContent, errors);
        
        if (!errors.isEmpty()) {
            throw new HtmlValidationException(String.join("; ", errors));
        }
    }

    /**
     * Exception thrown when HTML validation fails.
     */
    public static class HtmlValidationException extends Exception {
        public HtmlValidationException(String message) {
            super(message);
        }
        
        public HtmlValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

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
        if (UNESCAPED_LT_PATTERN.matcher(html).find()) {
            errors.add("Found unescaped less than signs (<) that must be escaped as &lt;");
        }

        // Check for unescaped greater than signs (not part of HTML tags)
        if (UNESCAPED_GT_PATTERN.matcher(html).find()) {
            errors.add("Found unescaped greater than signs (>) that must be escaped as &gt;");
        }
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
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable external entity resolution for security
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler()); // Ignore warnings, fail on errors
            builder.parse(new ByteArrayInputStream(htmlWithoutDoctype.getBytes("UTF-8")));
            
        } catch (SAXException e) {
            errors.add("XML parsing error: " + e.getMessage());
        } catch (ParserConfigurationException | IOException e) {
            errors.add("XML validation configuration error: " + e.getMessage());
        }
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

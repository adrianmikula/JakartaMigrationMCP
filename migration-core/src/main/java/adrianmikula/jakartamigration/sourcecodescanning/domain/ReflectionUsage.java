package adrianmikula.jakartamigration.sourcecodescanning.domain;

import java.util.List;

/**
 * Represents a reflection-based reference to a javax class.
 * This captures patterns like:
 * - Class.forName("javax.servlet.Filter")
 * - ClassUtils.loadClass("javax.persistence.Entity")
 * - "javax.servlet.http.HttpServlet" in property files
 */
public record ReflectionUsage(
        String className,
        String jakartaEquivalent,
        ReflectionType type,
        String context,
        int lineNumber
) {
    /**
     * The type of reflection usage detected.
     */
    public enum ReflectionType {
        CLASS_FOR_NAME,           // Class.forName("javax.xxx")
        CLASS_LOADER,             // classLoader.loadClass("javax.xxx")
        SPRING_BEAN_NAME,         // @Bean(name = "javax.xxx")
        JNDI_LOOKUP,              // context.lookup("java:comp/env/javax.xxx")
        PROPERTIES_FILE,          // In .properties files
        CONFIGURATION,            // In XML config files
        ANNOTATION_VALUE,         // @SomeAnnotation("javax.xxx")
        OTHER                     // Other reflection patterns
    }
    
    /**
     * Returns the Jakarta-equivalent class name.
     */
    public String getJakartaEquivalent() {
        if (jakartaEquivalent != null && !jakartaEquivalent.isEmpty()) {
            return jakartaEquivalent;
        }
        // Auto-generate equivalent if not provided
        return className.replace("javax.", "jakarta.");
    }
    
    /**
     * Returns the package containing the class.
     */
    public String getPackage() {
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0 && lastDot < className.length() - 1) {
            return className.substring(0, lastDot);
        }
        return "";
    }
    
    /**
     * Checks if this is a critical reflection usage that might break migration.
     */
    public boolean isCritical() {
        return type == ReflectionType.CLASS_FOR_NAME || 
               type == ReflectionType.CLASS_LOADER ||
               type == ReflectionType.JNDI_LOOKUP;
    }
    
    /**
     * Returns suggestions for fixing this reflection usage.
     */
    public List<String> getFixSuggestions() {
        return switch (type) {
            case CLASS_FOR_NAME -> List.of(
                    "Replace with direct class reference: " + getJakartaEquivalent() + ".class",
                    "Use Class.forName(\"" + getJakartaEquivalent() + "\") instead",
                    "Consider using " + getPackage() + ".* for package-level imports"
            );
            case CLASS_LOADER -> List.of(
                    "Update class loader code to use Jakarta equivalent",
                    "Consider using Thread.currentThread().getContextClassLoader().loadClass(\"" + 
                        getJakartaEquivalent() + "\")"
            );
            case JNDI_LOOKUP -> List.of(
                    "Update JNDI lookup to use jakarta namespace",
                    "Check web.xml for namespace updates"
            );
            case SPRING_BEAN_NAME -> List.of(
                    "Update @Bean name to use Jakarta equivalent",
                    "Consider using fully qualified class name"
            );
            case PROPERTIES_FILE -> List.of(
                    "Update property values to use Jakarta class names",
                    "Consider migrating to application.yaml for type-safe configuration"
            );
            case CONFIGURATION -> List.of(
                    "Update configuration to reference Jakarta classes",
                    "Review XML/JSON configuration files"
            );
            case ANNOTATION_VALUE -> List.of(
                    "Update annotation value to use Jakarta equivalent",
                    "Consider using Class<?>::class reference"
            );
            case OTHER -> List.of(
                    "Review and update reflection code to use Jakarta equivalent",
                    "Test thoroughly after migration"
            );
        };
    }
}

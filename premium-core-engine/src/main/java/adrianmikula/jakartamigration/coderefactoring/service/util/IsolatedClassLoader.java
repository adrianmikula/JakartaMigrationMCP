package adrianmikula.jakartamigration.coderefactoring.service.util;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

/**
 * A ClassLoader that is strictly isolated from the IDE's classloader.
 * It uses the platform classloader as its parent, ensuring it only sees
 * standard Java classes and the explicitly provided URLs.
 */
public class IsolatedClassLoader extends URLClassLoader {

    /**
     * Creates a new IsolatedClassLoader.
     * 
     * @param urls The URLs for the dependencies (our plugin's classpath).
     */
    public IsolatedClassLoader(URL[] urls) {
        // Use the platform classloader as parent to exclude IDE libraries
        super(urls, getPlatformOrEmptyLoader());
    }

    private static ClassLoader getPlatformOrEmptyLoader() {
        try {
            // Java 9+ way to get a minimal parent
            return ClassLoader.getPlatformClassLoader();
        } catch (Throwable t) {
            // Fallback for older Java (not expected here since it's Java 17+)
            return null;
        }
    }
}

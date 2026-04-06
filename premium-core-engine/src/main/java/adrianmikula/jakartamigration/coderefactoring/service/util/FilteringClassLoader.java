package adrianmikula.jakartamigration.coderefactoring.service.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * A ClassLoader that delegates to a parent but filters out specific classes and
 * resources.
 * Used to prevent Jackson from discovering conflicting IDE modules.
 */
public class FilteringClassLoader extends ClassLoader {
    private final Set<String> blockedPackages;
    private final Set<String> blockedResources;

    public FilteringClassLoader(ClassLoader parent, Collection<String> blockedPackages,
            Collection<String> blockedResources) {
        super(parent);
        this.blockedPackages = new HashSet<>(blockedPackages);
        this.blockedResources = new HashSet<>(blockedResources);
    }

    /**
     * Expose URLs from parent if it's a URLClassLoader or has getURLs() method.
     * Filters out URLs that contain blocked resources to prevent ClassGraph leaks.
     */
    public URL[] getURLs() {
        try {
            URL[] urls = null;
            Method getURLsMethod = null;
            try {
                getURLsMethod = getParent().getClass().getMethod("getURLs");
            } catch (NoSuchMethodException e) {
                try {
                    getURLsMethod = getParent().getClass().getMethod("getUrls");
                } catch (NoSuchMethodException e2) {
                    // Ignore
                }
            }

            if (getURLsMethod != null) {
                Object result = getURLsMethod.invoke(getParent());
                if (result instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<URL> list = (List<URL>) result;
                    urls = list.toArray(new URL[0]);
                } else if (result instanceof URL[]) {
                    urls = (URL[]) result;
                }
            }

            if (urls != null) {
                String pluginPrefix = getPluginPathPrefix();
                List<URL> filtered = new ArrayList<>();
                for (URL url : urls) {
                    if (isSafeUrl(url, pluginPrefix)) {
                        filtered.add(url);
                    }
                }
                return filtered.toArray(new URL[0]);
            }

            return new URL[0];
        } catch (Exception e) {
            return new URL[0];
        }
    }

    private String getPluginPathPrefix() {
        try {
            URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
            if (location != null) {
                String path = location.toString();
                // If it's a jar, get the directory or prefix
                if (path.contains("!/")) {
                    return path.substring(0, path.indexOf("!/"));
                }
                return path;
            }
        } catch (Exception e) {
            // Ignore
        }
        return "___UNKNOWN___";
    }

    private boolean isSafeUrl(URL url, String pluginPrefix) {
        String urlStr = url.toString();

        // If it's from our own plugin, it's safe
        if (!pluginPrefix.equals("___UNKNOWN___") && urlStr.startsWith(pluginPrefix)) {
            return true;
        }

        // If it's from the parent, check if it contains any blocked terms
        String lowerUrl = urlStr.toLowerCase();
        for (String res : blockedResources) {
            if (lowerUrl.contains(res.toLowerCase())) {
                return false;
            }
        }

        // Broader check for any jackson module in intellij core
        if (lowerUrl.contains("intellij") && lowerUrl.contains("jackson") && lowerUrl.contains("module")) {
            return false;
        }

        return true;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        checkNotBlocked(name);
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        checkNotBlocked(name);
        return super.findClass(name);
    }

    private void checkNotBlocked(String name) throws ClassNotFoundException {
        for (String pkg : blockedPackages) {
            if (name.startsWith(pkg)) {
                throw new ClassNotFoundException("Blocked by filter: " + name);
            }
        }
    }

    @Override
    public URL getResource(String name) {
        URL url = super.getResource(name);
        if (url != null && !isSafeUrl(url, getPluginPathPrefix())) {
            return null;
        }
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> resources = super.getResources(name);
        String pluginPrefix = getPluginPathPrefix();

        List<URL> filtered = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            if (isSafeUrl(url, pluginPrefix)) {
                filtered.add(url);
            }
        }
        return Collections.enumeration(filtered);
    }
}

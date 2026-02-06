package adrianmikula.jakartamigration.dependencyanalysis.service;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Service for checking binary compatibility between JAR files using japicmp library.
 * 
 * This class uses reflection to access japicmp classes to avoid hard dependency,
 * allowing the code to work even if japicmp is not available at runtime.
 */
@Slf4j
public class JapicmpCompatibilityChecker {
    
    private static final String JAR_ARCHIVE_FILE_CLASS = "japicmp.model.JarArchiveFile";
    private static final String JAR_ARCHIVE_COMPARATOR_CLASS = "japicmp.cmp.JarArchiveComparator";
    private static final String JAR_ARCHIVE_COMPARATOR_OPTIONS_CLASS = "japicmp.cmp.JarArchiveComparatorOptions";
    
    /**
     * Checks binary compatibility between two JAR files.
     * 
     * @param oldJarFile The old version JAR file
     * @param newJarFile The new version JAR file
     * @return "compatible" if no breaking changes detected, "incompatible" if breaking changes found,
     *         or "error" if the check could not be performed
     */
    public String checkCompatibility(File oldJarFile, File newJarFile) {
        if (oldJarFile == null || newJarFile == null) {
            log.warn("JAR files cannot be null for compatibility check");
            return "error";
        }
        
        if (!oldJarFile.exists() || !newJarFile.exists()) {
            log.warn("One or both JAR files do not exist: old={}, new={}", 
                oldJarFile.getPath(), newJarFile.getPath());
            return "error";
        }
        
        try {
            // Load japicmp classes using reflection
            Class<?> jarArchiveFileClass = Class.forName(JAR_ARCHIVE_FILE_CLASS);
            Class<?> jarArchiveComparatorClass = Class.forName(JAR_ARCHIVE_COMPARATOR_CLASS);
            Class<?> jarArchiveComparatorOptionsClass = Class.forName(JAR_ARCHIVE_COMPARATOR_OPTIONS_CLASS);
            
            // FIX: Wrap File objects in JarArchiveFile instances before passing to compare()
            // The japicmp API requires JarArchive objects, not raw File objects
            Constructor<?> jarArchiveFileConstructor = jarArchiveFileClass.getConstructor(File.class);
            Object oldJarArchive = jarArchiveFileConstructor.newInstance(oldJarFile);
            Object newJarArchive = jarArchiveFileConstructor.newInstance(newJarFile);
            
            // Create JarArchiveComparatorOptions
            Object comparatorOptions = jarArchiveComparatorOptionsClass.getConstructor().newInstance();
            
            // Create JarArchiveComparator
            Constructor<?> comparatorConstructor = jarArchiveComparatorClass.getConstructor(jarArchiveComparatorOptionsClass);
            Object comparator = comparatorConstructor.newInstance(comparatorOptions);
            
            // FIX: Use JarArchive.class instead of File.class in getMethod()
            // The original buggy code was: getMethod("compare", File.class, File.class)
            // This would throw NoSuchMethodException because compare() takes JarArchive, not File
            // Create lists containing the JarArchive instances
            List<?> oldArchives = Collections.singletonList(oldJarArchive);
            List<?> newArchives = Collections.singletonList(newJarArchive);
            
            // Get the compare method - it takes List<JarArchive> for old and new archives
            Method compareMethod = jarArchiveComparatorClass.getMethod(
                "compare", 
                List.class, 
                List.class
            );
            
            // Invoke the compare method with JarArchive instances
            List<?> jApiClasses = (List<?>) compareMethod.invoke(comparator, oldArchives, newArchives);
            
            // Check if there are any breaking changes
            boolean hasBreakingChanges = false;
            if (jApiClasses != null) {
                for (Object jApiClassObj : jApiClasses) {
                    // Check if the class has breaking changes
                    // JApiClass has methods like getChangeStatus() or isBinaryCompatible()
                    Method getChangeStatusMethod = null;
                    try {
                        getChangeStatusMethod = jApiClassObj.getClass().getMethod("getChangeStatus");
                    } catch (NoSuchMethodException e) {
                        // Try alternative method name
                        try {
                            getChangeStatusMethod = jApiClassObj.getClass().getMethod("isBinaryCompatible");
                        } catch (NoSuchMethodException ex) {
                            log.debug("Could not find change status method in JApiClass");
                        }
                    }
                    
                    if (getChangeStatusMethod != null) {
                        Object status = getChangeStatusMethod.invoke(jApiClassObj);
                        // Check if status indicates breaking change
                        // This depends on japicmp's ChangeStatus enum
                        if (status != null && status.toString().contains("BREAKING")) {
                            hasBreakingChanges = true;
                            break;
                        }
                    }
                }
            }
            
            return hasBreakingChanges ? "incompatible" : "compatible";
            
        } catch (ClassNotFoundException e) {
            log.warn("japicmp library not found in classpath, skipping binary compatibility check", e);
            return "error";
        } catch (NoSuchMethodException e) {
            log.error("Failed to find required method in japicmp library. " +
                "This may indicate a version mismatch or API change.", e);
            return "error";
        } catch (Exception e) {
            log.error("Error during binary compatibility check", e);
            return "error";
        }
    }
}


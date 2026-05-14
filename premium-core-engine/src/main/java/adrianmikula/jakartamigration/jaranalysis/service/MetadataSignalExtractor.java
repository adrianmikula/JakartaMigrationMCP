package adrianmikula.jakartamigration.jaranalysis.service;

import adrianmikula.jakartamigration.jaranalysis.domain.JarScanSignal;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Extracts metadata signals from JAR files.
 * Analyzes pom.xml and pom.properties files for dependency information,
 * and reads MANIFEST.MF for module names.
 */
@Slf4j
public class MetadataSignalExtractor {

    public MetadataSignalExtractor() {
        // No initialization needed
    }

    /**
     * Enhances a JarScanSignal with metadata from the JAR.
     *
     * @param jarPath Path to the JAR file
     * @param signal Original signal from bytecode analysis
     * @return Enhanced signal with metadata information
     */
    public JarScanSignal enhanceSignal(Path jarPath, JarScanSignal signal) {
        Objects.requireNonNull(jarPath, "jarPath cannot be null");
        Objects.requireNonNull(signal, "signal cannot be null");

        SignalCollection signals = new SignalCollection();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            searchAllPomProperties(jarFile, signals);
            searchAllPomXml(jarFile, signals);
            extractFromManifest(jarFile, signals);
        } catch (IOException e) {
            log.warn("Failed to read JAR metadata for {}: {}", jarPath, e.getMessage());
        }

        return new JarScanSignal.Builder()
            .artifactCoordinate(signal.artifactCoordinate())
            .javaxClassRefs(signal.javaxClassRefs())
            .jakartaClassRefs(signal.jakartaClassRefs())
            .apiUsage(signal.apiUsage())
            .reflectionStrings(signal.reflectionStrings())
            .hasPomMetadata(signals.hasPomMetadata)
            .pomIndicatesJavax(signals.javaxDepsInPom)
            .pomIndicatesJakarta(signals.jakartaDepsInPom)
            .automaticModuleName(signals.manifestModuleName)
            .hasShadedPackages(signal.hasShadedPackages())
            .testOnlyPatterns(signal.testOnlyPatterns())
            .build();
    }

    /**
     * Searches all pom.properties entries for namespace indicators.
     */
    private void searchAllPomProperties(JarFile jarFile, SignalCollection signals) {
        signals.hasPomMetadata = true;
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith("META-INF/maven/") && name.endsWith("/pom.properties")) {
                try (InputStream is = jarFile.getInputStream(entry)) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    if (content.contains("jakarta")) {
                        signals.jakartaDepsInPom = true;
                    }
                    if (content.contains("javax")) {
                        signals.javaxDepsInPom = true;
                    }
                } catch (IOException e) {
                    // Skip this entry
                }
            }
        }
    }

    /**
     * Searches all pom.xml entries for namespace indicators.
     */
    private void searchAllPomXml(JarFile jarFile, SignalCollection signals) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith("META-INF/maven/") && name.endsWith("/pom.xml")) {
                parsePomXml(jarFile, entry, signals);
            }
        }
    }

    /**
     * Parses a pom.xml entry for javax/jakarta dependencies.
     */
    private boolean parsePomXml(JarFile jarFile, java.util.zip.ZipEntry entry, SignalCollection signals) {
        try (InputStream is = jarFile.getInputStream(entry)) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            boolean hasJavax = content.contains("javax.") ||
                content.contains(">javax") ||
                content.contains("\"javax");

            boolean hasJakarta = content.contains("jakarta.") ||
                content.contains(">jakarta") ||
                content.contains("\"jakarta");

            if (content.contains("jakarta.xml.bind") ||
                content.contains("jakarta.persistence") ||
                content.contains("jakarta.servlet")) {
                hasJakarta = true;
            }

            if (content.contains("javax.xml.bind") ||
                content.contains("javax.persistence") ||
                content.contains("javax.servlet")) {
                hasJavax = true;
            }

            if (hasJavax && !hasJakarta) {
                signals.javaxDepsInPom = true;
            } else if (hasJakarta && !hasJavax) {
                signals.jakartaDepsInPom = true;
            } else if (hasJavax && hasJakarta) {
                int javaxCount = countOccurrences(content, "javax.");
                int jakartaCount = countOccurrences(content, "jakarta.");
                if (jakartaCount > javaxCount) {
                    signals.jakartaDepsInPom = true;
                } else if (javaxCount > jakartaCount) {
                    signals.javaxDepsInPom = true;
                } else {
                    signals.javaxDepsInPom = true;
                    signals.jakartaDepsInPom = true;
                }
            }
            return true;
        } catch (Exception e) {
            log.trace("Failed to parse pom.xml {}: {}", entry.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Extracts Automatic-Module-Name from JAR manifest.
     */
    private void extractFromManifest(JarFile jarFile, SignalCollection signals) {
        try {
            java.util.jar.Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                return;
            }

            String moduleName = manifest.getMainAttributes()
                .getValue("Automatic-Module-Name");
            if (moduleName != null) {
                signals.manifestModuleName = moduleName.trim();
            }

            String implTitle = manifest.getMainAttributes()
                .getValue("Implementation-Title");
            if (implTitle != null && !signals.jakartaDepsInPom && !signals.javaxDepsInPom) {
                if (implTitle.toLowerCase().contains("jakarta")) {
                    signals.jakartaDepsInPom = true;
                } else if (implTitle.toLowerCase().contains("javax")) {
                    signals.javaxDepsInPom = true;
                }
            }
        } catch (IOException e) {
            log.trace("Failed to read manifest: {}", e.getMessage());
        }
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(substring, idx)) >= 0) {
            count++;
            idx += substring.length();
        }
        return count;
    }

    /**
     * Internal class to collect signal results during extraction.
     */
    private static class SignalCollection {
        boolean hasPomMetadata = false;
        boolean jakartaDepsInPom = false;
        boolean javaxDepsInPom = false;
        String manifestModuleName = null;
    }
}
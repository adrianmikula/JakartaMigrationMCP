package adrianmikula.jakartamigration.scanning;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scrapes OpenRewrite recipes from GitHub to extract coordinate mappings
 * and package rename patterns for Jakarta EE migration.
 *
 * Caches results to ~/.jakarta-migration/recipe-patterns.json with a 7-day TTL.
 */
@Slf4j
public class RecipePatternExtractor {

    private static final String RECIPE_URL =
        "https://raw.githubusercontent.com/openrewrite/rewrite-migrate-java/main/src/main/resources/META-INF/rewrite/jakarta-ee-9.yml";
    private static final Path CACHE_DIR = Path.of(System.getProperty("user.home"), ".jakarta-migration");
    private static final Path CACHE_FILE = CACHE_DIR.resolve("recipe-patterns.json");
    private static final long CACHE_TTL_DAYS = 7;
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private final ObjectMapper objectMapper;
    private volatile RecipePatterns cachedPatterns;

    // Per-JVM caches for the derived quick-lookup maps so that multiple scanner
    // instances do not repeatedly re-parse the recipe patterns file.
    private static volatile Map<String, String> coordinateMapCache;
    private static volatile Map<String, String> packageRenameMapCache;
    private static final Object MAP_CACHE_LOCK = new Object();

    public RecipePatternExtractor() {
        this(new ObjectMapper());
    }

    public RecipePatternExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Returns cached or freshly loaded recipe patterns.
     * Refreshes from GitHub if cache is stale (>7 days old).
     */
    public RecipePatterns getPatterns() {
        RecipePatterns patterns = cachedPatterns;
        if (patterns != null && !isStale(patterns)) {
            return patterns;
        }
        return loadOrRefresh();
    }

    /**
     * Forces a refresh from GitHub, bypassing cache.
     */
    public RecipePatterns refreshPatterns() {
        try {
            String yamlContent = fetchRecipeYaml();
            if (yamlContent == null) {
                log.warn("Failed to fetch recipe YAML, falling back to cache or empty");
                return cachedPatterns != null ? cachedPatterns : RecipePatterns.empty();
            }
            RecipePatterns parsed = parseRecipeYaml(yamlContent);
            RecipePatterns withTimestamp = new RecipePatterns(
                parsed.coordinateMappings(), parsed.packageRenames(), Instant.now());
            saveToCache(withTimestamp);
            cachedPatterns = withTimestamp;
            log.info("Refreshed recipe patterns: {} coordinate mappings, {} package renames",
                withTimestamp.coordinateMappings().size(), withTimestamp.packageRenames().size());
            return withTimestamp;
        } catch (Exception e) {
            log.error("Failed to refresh recipe patterns", e);
            return cachedPatterns != null ? cachedPatterns : RecipePatterns.empty();
        }
    }

    private synchronized RecipePatterns loadOrRefresh() {
        RecipePatterns fromCache = loadFromCache();
        if (fromCache != null && !isStale(fromCache)) {
            cachedPatterns = fromCache;
            return fromCache;
        }
        return refreshPatterns();
    }

    private boolean isStale(RecipePatterns patterns) {
        if (patterns.fetchedAt() == null) return true;
        return Instant.now().isAfter(patterns.fetchedAt().plus(CACHE_TTL_DAYS, ChronoUnit.DAYS));
    }

    private RecipePatterns loadFromCache() {
        if (!Files.exists(CACHE_FILE)) return null;
        try {
            byte[] bytes = Files.readAllBytes(CACHE_FILE);
            return objectMapper.readValue(bytes, RecipePatterns.class);
        } catch (Exception e) {
            log.debug("Failed to read cache file: {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    private void saveToCache(RecipePatterns patterns) {
        try {
            Files.createDirectories(CACHE_DIR);
            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(patterns);
            Files.write(CACHE_FILE, bytes);
        } catch (Exception e) {
            log.debug("Failed to write cache file: {}: {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private String fetchRecipeYaml() {
        try {
            URL url = URI.create(RECIPE_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "Jakarta-Migration-MCP/1.0");

            if (conn.getResponseCode() != 200) {
                log.warn("HTTP {} fetching recipe from {}", conn.getResponseCode(), RECIPE_URL);
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.error("Failed to fetch recipe YAML from GitHub", e);
            return null;
        }
    }

    /**
     * Parses the OpenRewrite YAML recipe to extract ChangeDependency and ChangePackage blocks.
     */
    RecipePatterns parseRecipeYaml(String yaml) {
        List<CoordinateMapping> coordinateMappings = new ArrayList<>();
        List<PackageRename> packageRenames = new ArrayList<>();

        String[] lines = yaml.split("\n");
        String currentType = null;
        String oldGroupId = null, oldArtifactId = null, oldVersion = null;
        String newGroupId = null, newArtifactId = null, newVersion = null;
        String oldPackageName = null, newPackageName = null;

        for (String line : lines) {
            String trimmed = line.trim();

            // Detect recipe type
            if (trimmed.startsWith("- org.openrewrite.java.ChangeDependency")) {
                currentType = "ChangeDependency";
                oldGroupId = null; oldArtifactId = null; oldVersion = null;
                newGroupId = null; newArtifactId = null; newVersion = null;
            } else if (trimmed.startsWith("- org.openrewrite.java.ChangePackage")) {
                currentType = "ChangePackage";
                oldPackageName = null; newPackageName = null;
            } else if (trimmed.startsWith("- org.openrewrite.java")) {
                // Other recipe types - skip
                flushPending(currentType, oldGroupId, oldArtifactId, oldVersion,
                    newGroupId, newArtifactId, newVersion, oldPackageName, newPackageName,
                    coordinateMappings, packageRenames);
                currentType = null;
            }

            if ("ChangeDependency".equals(currentType)) {
                if (trimmed.startsWith("oldGroupId:")) {
                    oldGroupId = extractValue(trimmed);
                } else if (trimmed.startsWith("oldArtifactId:")) {
                    oldArtifactId = extractValue(trimmed);
                } else if (trimmed.startsWith("oldVersion:")) {
                    oldVersion = extractValue(trimmed);
                } else if (trimmed.startsWith("newGroupId:")) {
                    newGroupId = extractValue(trimmed);
                } else if (trimmed.startsWith("newArtifactId:")) {
                    newArtifactId = extractValue(trimmed);
                } else if (trimmed.startsWith("newVersion:")) {
                    newVersion = extractValue(trimmed);
                } else if (oldGroupId != null && trimmed.startsWith("-")) {
                    // New recipe block starting - flush current
                    flushPending(currentType, oldGroupId, oldArtifactId, oldVersion,
                        newGroupId, newArtifactId, newVersion, oldPackageName, newPackageName,
                        coordinateMappings, packageRenames);
                    oldGroupId = null;
                }
            } else if ("ChangePackage".equals(currentType)) {
                if (trimmed.startsWith("oldPackageName:")) {
                    oldPackageName = extractValue(trimmed);
                } else if (trimmed.startsWith("newPackageName:")) {
                    newPackageName = extractValue(trimmed);
                } else if (oldPackageName != null && trimmed.startsWith("-")) {
                    flushPending(currentType, oldGroupId, oldArtifactId, oldVersion,
                        newGroupId, newArtifactId, newVersion, oldPackageName, newPackageName,
                        coordinateMappings, packageRenames);
                    oldPackageName = null;
                }
            }
        }

        // Flush any remaining
        flushPending(currentType, oldGroupId, oldArtifactId, oldVersion,
            newGroupId, newArtifactId, newVersion, oldPackageName, newPackageName,
            coordinateMappings, packageRenames);

        return new RecipePatterns(coordinateMappings, packageRenames, null);
    }

    private void flushPending(String type, String oldGroupId, String oldArtifactId, String oldVersion,
                               String newGroupId, String newArtifactId, String newVersion,
                               String oldPackageName, String newPackageName,
                               List<CoordinateMapping> coordinateMappings, List<PackageRename> packageRenames) {
        if ("ChangeDependency".equals(type) && oldGroupId != null && oldArtifactId != null) {
            coordinateMappings.add(new CoordinateMapping(
                oldGroupId, oldArtifactId, oldVersion,
                newGroupId, newArtifactId, newVersion));
        } else if ("ChangePackage".equals(type) && oldPackageName != null && newPackageName != null) {
            packageRenames.add(new PackageRename(oldPackageName, newPackageName));
        }
    }

    private String extractValue(String line) {
        int colonIdx = line.indexOf(':');
        if (colonIdx < 0) return null;
        String value = line.substring(colonIdx + 1).trim();
        // Remove surrounding quotes
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.isEmpty() ? null : value;
    }

    /**
     * Returns a quick-lookup map of "groupId:artifactId" → "newGroupId:newArtifactId".
     * The map is built once per JVM and reused across instances.
     */
    public Map<String, String> getCoordinateMap() {
        Map<String, String> cache = coordinateMapCache;
        if (cache != null) {
            return cache;
        }
        synchronized (MAP_CACHE_LOCK) {
            if (coordinateMapCache != null) {
                return coordinateMapCache;
            }
            coordinateMapCache = Map.copyOf(getPatterns().toCoordinateMap());
            return coordinateMapCache;
        }
    }

    /**
     * Returns a quick-lookup map of "javax.*" → "jakarta.*" package prefixes.
     * The map is built once per JVM and reused across instances.
     */
    public Map<String, String> getPackageRenameMap() {
        Map<String, String> cache = packageRenameMapCache;
        if (cache != null) {
            return cache;
        }
        synchronized (MAP_CACHE_LOCK) {
            if (packageRenameMapCache != null) {
                return packageRenameMapCache;
            }
            packageRenameMapCache = Map.copyOf(getPatterns().toPackageRenameMap());
            return packageRenameMapCache;
        }
    }

    // --- Domain records ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecipePatterns(
        List<CoordinateMapping> coordinateMappings,
        List<PackageRename> packageRenames,
        Instant fetchedAt) {

        static RecipePatterns empty() {
            return new RecipePatterns(List.of(), List.of(), null);
        }

        /**
         * Builds a quick-lookup map of "groupId:artifactId" → "newGroupId:newArtifactId".
         */
        public Map<String, String> toCoordinateMap() {
            var map = new ConcurrentHashMap<String, String>();
            for (var m : coordinateMappings) {
                String key = m.oldGroupId() + ":" + m.oldArtifactId();
                String val = m.newGroupId() + ":" + m.newArtifactId();
                map.put(key, val);
            }
            return map;
        }

        /**
         * Builds a quick-lookup map of "javax.*" → "jakarta.*" package prefixes.
         */
        public Map<String, String> toPackageRenameMap() {
            var map = new ConcurrentHashMap<String, String>();
            for (var r : packageRenames) {
                map.put(r.oldPackageName(), r.newPackageName());
            }
            return map;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CoordinateMapping(
        String oldGroupId, String oldArtifactId, String oldVersion,
        String newGroupId, String newArtifactId, String newVersion) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PackageRename(String oldPackageName, String newPackageName) {
    }
}

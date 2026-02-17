package adrianmikula.jakartamigration.intellij.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for loading UI text from property files.
 * Provides centralized access to localized UI text.
 */
public final class UiTextLoader {

    private static final Properties UI_TEXT = new Properties();
    private static final Properties STRATEGIES = new Properties();
    private static final Properties PHASES = new Properties();

    static {
        loadProperties();
    }

    private UiTextLoader() {
        // Utility class - prevent instantiation
    }

    private static void loadProperties() {
        loadProperties(UI_TEXT, "ui-text.properties");
        loadProperties(STRATEGIES, "migration-strategies.properties");
        loadProperties(PHASES, "migration-phases.properties");
    }

    private static void loadProperties(Properties props, String filename) {
        try (InputStream is = UiTextLoader.class.getClassLoader().getResourceAsStream(filename)) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("Warning: Could not find property file: " + filename);
            }
        } catch (IOException e) {
            System.err.println("Error loading property file " + filename + ": " + e.getMessage());
        }
    }

    /**
     * Get UI text by key.
     * @param key the property key
     * @return the property value, or null if not found
     */
    @Nullable
    public static String get(@NotNull String key) {
        String value = UI_TEXT.getProperty(key);
        if (value == null) {
            value = STRATEGIES.getProperty(key);
        }
        if (value == null) {
            value = PHASES.getProperty(key);
        }
        return value;
    }

    /**
     * Get UI text by key with a default value.
     * @param key the property key
     * @param defaultValue the default value if key not found
     * @return the property value, or defaultValue if not found
     */
    @NotNull
    public static String get(@NotNull String key, @NotNull String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get UI text with newlines converted to actual line breaks.
     * @param key the property key
     * @return the property value with \n converted to actual newlines
     */
    @Nullable
    public static String getWithNewlines(@NotNull String key) {
        String value = get(key);
        if (value != null) {
            return value.replace("\\n", "\n");
        }
        return null;
    }

    /**
     * Get UI text with newlines converted, using default if not found.
     * @param key the property key
     * @param defaultValue the default value if key not found
     * @return the property value with \n converted to actual newlines
     */
    @NotNull
    public static String getWithNewlines(@NotNull String key, @NotNull String defaultValue) {
        String value = getWithNewlines(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Check if a key exists in any of the property files.
     * @param key the property key
     * @return true if the key exists
     */
    public static boolean hasKey(@NotNull String key) {
        return UI_TEXT.containsKey(key) || STRATEGIES.containsKey(key) || PHASES.containsKey(key);
    }

    /**
     * Get strategy benefits.
     * @param strategy the strategy name (e.g., "big_bang", "incremental")
     * @return the benefits text
     */
    @NotNull
    public static String getStrategyBenefits(@NotNull String strategy) {
        return getWithNewlines("strategy." + strategy + ".benefits", "");
    }

    /**
     * Get strategy risks.
     * @param strategy the strategy name (e.g., "big_bang", "incremental")
     * @return the risks text
     */
    @NotNull
    public static String getStrategyRisks(@NotNull String strategy) {
        return getWithNewlines("strategy." + strategy + ".risks", "");
    }

    /**
     * Get phase title.
     * @param strategy the strategy name
     * @param phaseNumber the phase number (1-based)
     * @return the phase title
     */
    @NotNull
    public static String getPhaseTitle(@NotNull String strategy, int phaseNumber) {
        return get("phase." + strategy + "." + phaseNumber + ".title", "");
    }

    /**
     * Get phase description.
     * @param strategy the strategy name
     * @param phaseNumber the phase number (1-based)
     * @return the phase description
     */
    @NotNull
    public static String getPhaseDescription(@NotNull String strategy, int phaseNumber) {
        return getWithNewlines("phase." + strategy + "." + phaseNumber + ".description", "");
    }

    /**
     * Get phase steps (newline-separated).
     * @param strategy the strategy name
     * @param phaseNumber the phase number (1-based)
     * @return the phase steps as newline-separated string
     */
    @NotNull
    public static String getPhaseSteps(@NotNull String strategy, int phaseNumber) {
        return get("phase." + strategy + "." + phaseNumber + ".steps", "");
    }

    /**
     * Get phase steps as array.
     * @param strategy the strategy name
     * @param phaseNumber the phase number (1-based)
     * @return the phase steps as array
     */
    @NotNull
    public static String[] getPhaseStepsArray(@NotNull String strategy, int phaseNumber) {
        String steps = getPhaseSteps(strategy, phaseNumber);
        if (steps.isEmpty()) {
            return new String[0];
        }
        return steps.split("\n");
    }

    /**
     * Get the number of phases for a strategy.
     * This is determined by checking which phase numbers exist.
     * @param strategy the strategy name
     * @return the number of phases
     */
    public static int getPhaseCount(@NotNull String strategy) {
        int count = 0;
        for (int i = 1; i <= 10; i++) {
            if (hasKey("phase." + strategy + "." + i + ".title")) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }
}

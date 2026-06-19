package adrianmikula.jakartamigration.util;

import java.time.Instant;
import java.util.Map;

public class AgentLogger {
  private static final Map<String, Integer> LEVELS = Map.of(
    "debug", 0, "info", 1, "warn", 2, "error", 3
  );
  private static final Map<String, String> PREFIXES = Map.of(
    "debug", "DEBUG", "info", "INFO ", "warn", "WARN ", "error", "ERROR"
  );
  private static int currentLevel = -1;

  private static int getLevel() {
    if (currentLevel >= 0) return currentLevel;
    String env = System.getenv("LOG_LEVEL");
    if (env != null && LEVELS.containsKey(env)) {
      currentLevel = LEVELS.get(env);
    } else {
      currentLevel = "true".equals(System.getenv("DEV")) ? 0 : 2;
    }
    return currentLevel;
  }

  private final String context;

  private AgentLogger(String context) { this.context = context; }

  public static AgentLogger logger(String context) { return new AgentLogger(context); }

  public void debug(String message, Object data) { log("debug", message, data); }
  public void info(String message, Object data) { log("info", message, data); }
  public void warn(String message, Object data) { log("warn", message, data); }
  public void error(String message, Object data) { log("error", message, data); }

  private void log(String level, String message, Object data) {
    if (LEVELS.getOrDefault(level, 99) < getLevel()) return;
    String ts = Instant.now().toString();
    String prefix = PREFIXES.get(level);
    String base = ts + " [" + prefix + "] [" + context + "] " + message;
    if (data instanceof Throwable) {
      System.err.println(base);
      ((Throwable) data).printStackTrace(System.err);
    } else if (data != null) {
      System.out.println(base + " " + data.toString());
    } else {
      System.out.println(base);
    }
  }
}

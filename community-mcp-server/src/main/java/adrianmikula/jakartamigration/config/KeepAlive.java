package adrianmikula.jakartamigration.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps the JVM alive in stdio mode by blocking a non-daemon thread.
 * Spring AI MCP stdio transport may use a daemon thread for reading stdin,
 * which does not prevent the JVM from exiting when the main thread completes.
 */
public class KeepAlive {

    private static final Logger log = LoggerFactory.getLogger(KeepAlive.class);

    private final boolean active;
    private Thread keepAliveThread;

    public KeepAlive(boolean active) {
        this.active = active;
    }

    @PostConstruct
    public void start() {
        if (!active) {
            return;
        }
        keepAliveThread = new Thread(() -> {
            log.info("Keep-alive thread started for stdio transport");
            try {
                while (!Thread.interrupted()) {
                    Thread.sleep(60_000); // Sleep in chunks to allow interruption
                }
            } catch (InterruptedException e) {
                log.info("Keep-alive thread interrupted, shutting down");
                Thread.currentThread().interrupt();
            }
        }, "mcp-stdio-keep-alive");
        keepAliveThread.setDaemon(false);
        keepAliveThread.start();
    }

    @PreDestroy
    public void stop() {
        if (keepAliveThread != null) {
            keepAliveThread.interrupt();
        }
    }
}

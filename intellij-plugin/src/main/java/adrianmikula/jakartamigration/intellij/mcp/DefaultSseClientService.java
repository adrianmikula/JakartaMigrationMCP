package adrianmikula.jakartamigration.intellij.mcp;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SSE (Server-Sent Events) client for real-time updates from the MCP server.
 */
public class DefaultSseClientService {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultSseClientService.class);

    private final String serverUrl;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private final AtomicBoolean isConnected;
    private Thread sseThread;
    private List<SseUpdateListener> listeners;

    public DefaultSseClientService() {
        this.serverUrl = System.getProperty("jakarta.mcp.server.url", "http://localhost:8080");
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newCachedThreadPool();
        this.isConnected = new AtomicBoolean(false);
        this.listeners = new ArrayList<>();
    }

    public void addListener(SseUpdateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SseUpdateListener listener) {
        listeners.remove(listener);
    }

    /**
     * Connect to the SSE stream for analysis updates.
     */
    public void connectToAnalysisStream(String projectPath) {
        if (isConnected.get()) {
            disconnect();
        }

        isConnected.set(true);
        sseThread = new Thread(() -> {
            try {
                String sseUrl = serverUrl + "/mcp/stream?projectPath=" + projectPath;
                URL url = URI.create(sseUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(0);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    StringBuilder eventData = new StringBuilder();
                    String eventType = null;

                    while (isConnected.get() && (line = reader.readLine()) != null) {
                        if (line.startsWith("event:")) {
                            eventType = line.substring(6).trim();
                        } else if (line.startsWith("data:")) {
                            eventData.append(line.substring(5).trim());
                        } else if (line.isEmpty()) {
                            // Empty line indicates end of event
                            if (eventData.length() > 0) {
                                processEvent(eventType, eventData.toString());
                                eventData.setLength(0);
                                eventType = null;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (isConnected.get()) {
                    LOG.error("SSE connection error: {}", e.getMessage());
                    notifyError("Connection lost: " + e.getMessage());
                }
            } finally {
                isConnected.set(false);
            }
        }, "SSE-Analysis-Stream");
        sseThread.start();
    }

    /**
     * Disconnect from the SSE stream.
     */
    public void disconnect() {
        isConnected.set(false);
        if (sseThread != null && sseThread.isAlive()) {
            sseThread.interrupt();
            try {
                sseThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Check if currently connected to SSE stream.
     */
    public boolean isConnected() {
        return isConnected.get();
    }

    private void processEvent(String eventType, String data) {
        try {
            switch (eventType) {
                case "dependency-update":
                    DependencyInfo dependency = objectMapper.readValue(data, DependencyInfo.class);
                    notifyDependencyUpdate(dependency);
                    break;
                case "progress":
                    ProgressEvent progress = objectMapper.readValue(data, ProgressEvent.class);
                    notifyAnalysisProgress(progress.progress, progress.message);
                    break;
                case "complete":
                    notifyAnalysisComplete();
                    break;
                case "error":
                    notifyError(data);
                    break;
                default:
                    LOG.debug("Unknown SSE event type: {}", eventType);
            }
        } catch (Exception e) {
            LOG.error("Error processing SSE event: {}", e.getMessage());
        }
    }

    private void notifyDependencyUpdate(DependencyInfo dependency) {
        listeners.forEach(listener -> listener.onDependencyUpdate(dependency));
    }

    private void notifyAnalysisProgress(int progress, String message) {
        listeners.forEach(listener -> listener.onAnalysisProgress(progress, message));
    }

    private void notifyAnalysisComplete() {
        listeners.forEach(SseUpdateListener::onAnalysisComplete);
    }

    private void notifyError(String error) {
        listeners.forEach(listener -> listener.onError(error));
    }

    /**
     * Progress event for SSE deserialization.
     */
    private static class ProgressEvent {
        int progress;
        String message;
    }
}

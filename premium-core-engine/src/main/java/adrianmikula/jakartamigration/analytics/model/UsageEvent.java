package adrianmikula.jakartamigration.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a usage event for analytics tracking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageEvent {
    
    /**
     * Type of usage event.
     */
    public enum EventType {
        CREDIT_USED("credit_used"),
        UPGRADE_CLICKED("upgrade_clicked");
        
        private final String value;
        
        EventType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static EventType fromValue(String value) {
            for (EventType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return null;
        }
    }
    
    /**
     * The anonymous user ID.
     */
    private String userId;
    
    /**
     * Type of the event.
     */
    private EventType eventType;
    
    /**
     * Credit type (for credit usage events).
     */
    private String creditType;
    
    /**
     * Additional event-specific data.
     */
    private Map<String, Object> eventData;
    
    /**
     * Timestamp when the event occurred.
     */
    private Instant timestamp;
    
    /**
     * Plugin version when the event occurred.
     */
    private String pluginVersion;
    
    /**
     * Creates a credit usage event with context information.
     */
    public static UsageEvent creditUsed(String userId, String creditType, 
                                      String currentUiTab, String triggerAction, String pluginVersion) {
        Map<String, Object> eventData = Map.of(
            "current_ui_tab", currentUiTab,
            "trigger_action", triggerAction,
            "plugin_version", pluginVersion
        );
        
        return UsageEvent.builder()
            .userId(userId)
            .eventType(EventType.CREDIT_USED)
            .creditType(creditType)
            .eventData(eventData)
            .pluginVersion(pluginVersion)
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Creates an upgrade clicked event with context information.
     */
    public static UsageEvent upgradeClicked(String userId, String source, 
                                         String currentUiTab, String pluginVersion) {
        Map<String, Object> eventData = Map.of(
            "source", source,
            "current_ui_tab", currentUiTab,
            "plugin_version", pluginVersion
        );
        
        return UsageEvent.builder()
            .userId(userId)
            .eventType(EventType.UPGRADE_CLICKED)
            .eventData(eventData)
            .pluginVersion(pluginVersion)
            .timestamp(Instant.now())
            .build();
    }
}

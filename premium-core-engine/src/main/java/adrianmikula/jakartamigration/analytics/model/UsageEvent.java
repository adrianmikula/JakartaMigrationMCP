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
     * Currently active UI tab when event occurred.
     */
    private String currentUiTab;
    
    /**
     * Plugin version when the event occurred.
     */
    private String pluginVersion;
    
    /**
     * Action that triggered the event.
     */
    private String triggerAction;
    
    /**
     * Additional event-specific data.
     */
    private Map<String, Object> eventData;
    
    /**
     * Timestamp when the event occurred.
     */
    private Instant timestamp;
    
    /**
     * Creates a credit usage event with context information.
     */
    public static UsageEvent creditUsed(String userId, String currentUiTab, 
                                      String triggerAction, String pluginVersion) {
        
        return UsageEvent.builder()
            .userId(userId)
            .eventType(EventType.CREDIT_USED)
            .currentUiTab(currentUiTab)
            .triggerAction(triggerAction)
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
            "source", source
        );
        
        return UsageEvent.builder()
            .userId(userId)
            .eventType(EventType.UPGRADE_CLICKED)
            .currentUiTab(currentUiTab)
            .pluginVersion(pluginVersion)
            .eventData(eventData)
            .timestamp(Instant.now())
            .build();
    }
}

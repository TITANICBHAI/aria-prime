package com.aiassistant.core.orchestration;

import java.util.HashMap;
import java.util.Map;

public class OrchestrationEvent {
    private final String eventType;
    private final String source;
    private final long timestamp;
    private final Map<String, Object> data;
    
    public OrchestrationEvent(String eventType, String source, Map<String, Object> data) {
        this.eventType = eventType;
        this.source = source;
        this.timestamp = System.currentTimeMillis();
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public String getSource() {
        return source;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public Object getData(String key) {
        return data.get(key);
    }
    
    @Override
    public String toString() {
        return "OrchestrationEvent{" +
                "type='" + eventType + '\'' +
                ", source='" + source + '\'' +
                ", timestamp=" + timestamp +
                ", data=" + data +
                '}';
    }
}

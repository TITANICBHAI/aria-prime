package com.aiassistant.core.orchestration;

import java.util.HashMap;
import java.util.Map;

public class ComponentStateSnapshot {
    private final String componentId;
    private final long timestamp;
    private final int version;
    private final Map<String, Object> state;
    private final String stateHash;
    
    public ComponentStateSnapshot(String componentId, int version, Map<String, Object> state) {
        this.componentId = componentId;
        this.version = version;
        this.timestamp = System.currentTimeMillis();
        this.state = state != null ? new HashMap<>(state) : new HashMap<>();
        this.stateHash = calculateHash(this.state);
    }
    
    private String calculateHash(Map<String, Object> state) {
        int hash = 17;
        for (Map.Entry<String, Object> entry : state.entrySet()) {
            hash = 31 * hash + entry.getKey().hashCode();
            if (entry.getValue() != null) {
                hash = 31 * hash + entry.getValue().hashCode();
            }
        }
        return String.valueOf(hash);
    }
    
    public String getComponentId() {
        return componentId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getVersion() {
        return version;
    }
    
    public Map<String, Object> getState() {
        return new HashMap<>(state);
    }
    
    public Object getState(String key) {
        return state.get(key);
    }
    
    public String getStateHash() {
        return stateHash;
    }
    
    @Override
    public String toString() {
        return "ComponentStateSnapshot{" +
                "componentId='" + componentId + '\'' +
                ", version=" + version +
                ", timestamp=" + timestamp +
                ", hash='" + stateHash + '\'' +
                '}';
    }
}

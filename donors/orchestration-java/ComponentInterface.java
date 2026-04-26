package com.aiassistant.core.orchestration;

import java.util.List;
import java.util.Map;

public interface ComponentInterface {
    
    String getComponentId();
    
    String getComponentName();
    
    List<String> getCapabilities();
    
    void initialize();
    
    void start();
    
    void stop();
    
    ComponentStateSnapshot captureState();
    
    void restoreState(ComponentStateSnapshot snapshot);
    
    Map<String, Object> execute(Map<String, Object> input);
    
    boolean isHealthy();
    
    void heartbeat();
    
    String getStatus();
}

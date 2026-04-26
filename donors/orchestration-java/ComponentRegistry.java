package com.aiassistant.core.orchestration;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ComponentRegistry {
    private static final String TAG = "ComponentRegistry";
    
    private final Map<String, RegisteredComponent> components;
    private final Map<String, List<String>> capabilityMap;
    private EventRouter eventRouter;
    
    public ComponentRegistry() {
        this.components = new ConcurrentHashMap<>();
        this.capabilityMap = new ConcurrentHashMap<>();
    }
    
    public void setEventRouter(EventRouter eventRouter) {
        this.eventRouter = eventRouter;
    }
    
    public void registerComponent(String componentId, String componentName, 
                                  List<String> capabilities) {
        RegisteredComponent component = new RegisteredComponent(
            componentId,
            componentName,
            capabilities,
            System.currentTimeMillis()
        );
        
        components.put(componentId, component);
        
        for (String capability : capabilities) {
            capabilityMap.computeIfAbsent(capability, k -> new ArrayList<>()).add(componentId);
        }
        
        Log.i(TAG, "Registered component: " + componentName + " (" + componentId + 
              ") with " + capabilities.size() + " capabilities");
        
        if (eventRouter != null) {
            OrchestrationEvent event = new OrchestrationEvent(
                "component.registered",
                componentId,
                new HashMap<String, Object>() {{
                    put("component_name", componentName);
                    put("capabilities", capabilities);
                }}
            );
            eventRouter.publish(event);
        }
    }
    
    public void unregisterComponent(String componentId) {
        RegisteredComponent component = components.remove(componentId);
        
        if (component != null) {
            for (String capability : component.capabilities) {
                List<String> providers = capabilityMap.get(capability);
                if (providers != null) {
                    providers.remove(componentId);
                }
            }
            
            Log.i(TAG, "Unregistered component: " + componentId);
            
            if (eventRouter != null) {
                OrchestrationEvent event = new OrchestrationEvent(
                    "component.unregistered",
                    componentId,
                    null
                );
                eventRouter.publish(event);
            }
        }
    }
    
    public void updateComponentStatus(String componentId, ComponentStatus status) {
        RegisteredComponent component = components.get(componentId);
        
        if (component != null) {
            component.status = status;
            component.lastStatusUpdate = System.currentTimeMillis();
            
            Log.d(TAG, "Component " + componentId + " status updated to " + status);
            
            if (eventRouter != null) {
                OrchestrationEvent event = new OrchestrationEvent(
                    "component.status.changed",
                    componentId,
                    new HashMap<String, Object>() {{
                        put("status", status.name());
                    }}
                );
                eventRouter.publish(event);
            }
        }
    }
    
    public void updateHeartbeat(String componentId) {
        RegisteredComponent component = components.get(componentId);
        
        if (component != null) {
            component.lastHeartbeat = System.currentTimeMillis();
        }
    }
    
    public RegisteredComponent getComponent(String componentId) {
        return components.get(componentId);
    }
    
    public List<String> getComponentsByCapability(String capability) {
        List<String> providers = capabilityMap.get(capability);
        return providers != null ? new ArrayList<>(providers) : new ArrayList<>();
    }
    
    public List<RegisteredComponent> getAllComponents() {
        return new ArrayList<>(components.values());
    }
    
    public List<RegisteredComponent> getComponentsByStatus(ComponentStatus status) {
        List<RegisteredComponent> result = new ArrayList<>();
        for (RegisteredComponent component : components.values()) {
            if (component.status == status) {
                result.add(component);
            }
        }
        return result;
    }
    
    public boolean isComponentHealthy(String componentId) {
        RegisteredComponent component = components.get(componentId);
        if (component == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long heartbeatAge = currentTime - component.lastHeartbeat;
        
        return component.status == ComponentStatus.ACTIVE && 
               heartbeatAge < 30000;
    }
    
    public enum ComponentStatus {
        INACTIVE,
        INITIALIZING,
        ACTIVE,
        DEGRADED,
        ERROR,
        ISOLATED
    }
    
    public static class RegisteredComponent {
        public final String componentId;
        public final String componentName;
        public final List<String> capabilities;
        public final long registrationTime;
        
        public ComponentStatus status;
        public long lastHeartbeat;
        public long lastStatusUpdate;
        public Map<String, Object> metadata;
        
        public RegisteredComponent(String componentId, String componentName, 
                                  List<String> capabilities, long registrationTime) {
            this.componentId = componentId;
            this.componentName = componentName;
            this.capabilities = new ArrayList<>(capabilities);
            this.registrationTime = registrationTime;
            this.status = ComponentStatus.INACTIVE;
            this.lastHeartbeat = System.currentTimeMillis();
            this.lastStatusUpdate = System.currentTimeMillis();
            this.metadata = new HashMap<>();
        }
        
        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }
        
        public Object getMetadata(String key) {
            return metadata.get(key);
        }
    }
}

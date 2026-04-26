package com.aiassistant.core.orchestration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProblemTicket {
    private final String ticketId;
    private final String componentId;
    private final String problemType;
    private final String description;
    private final long timestamp;
    private final Map<String, Object> context;
    private final List<String> attemptedRemedies;
    
    private TicketStatus status;
    private String resolution;
    private long resolvedTime;
    
    public ProblemTicket(String componentId, String problemType, 
                        String description, Map<String, Object> context) {
        this.ticketId = generateTicketId();
        this.componentId = componentId;
        this.problemType = problemType;
        this.description = description;
        this.timestamp = System.currentTimeMillis();
        this.context = context != null ? new HashMap<>(context) : new HashMap<>();
        this.attemptedRemedies = new ArrayList<>();
        this.status = TicketStatus.OPEN;
    }
    
    private String generateTicketId() {
        return "TICKET-" + System.currentTimeMillis() + "-" + 
               (int)(Math.random() * 10000);
    }
    
    public void addAttemptedRemedy(String remedy) {
        attemptedRemedies.add(remedy);
    }
    
    public void setStatus(TicketStatus status) {
        this.status = status;
    }
    
    public void resolve(String resolution) {
        this.resolution = resolution;
        this.status = TicketStatus.RESOLVED;
        this.resolvedTime = System.currentTimeMillis();
    }
    
    public void escalate() {
        this.status = TicketStatus.ESCALATED;
    }
    
    public String getTicketId() {
        return ticketId;
    }
    
    public String getComponentId() {
        return componentId;
    }
    
    public String getProblemType() {
        return problemType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Map<String, Object> getContext() {
        return new HashMap<>(context);
    }
    
    public List<String> getAttemptedRemedies() {
        return new ArrayList<>(attemptedRemedies);
    }
    
    public TicketStatus getStatus() {
        return status;
    }
    
    public String getResolution() {
        return resolution;
    }
    
    public long getResolvedTime() {
        return resolvedTime;
    }
    
    public enum TicketStatus {
        OPEN,
        IN_PROGRESS,
        RESOLVED,
        ESCALATED,
        FAILED
    }
    
    @Override
    public String toString() {
        return "ProblemTicket{" +
                "id='" + ticketId + '\'' +
                ", component='" + componentId + '\'' +
                ", type='" + problemType + '\'' +
                ", status=" + status +
                '}';
    }
}

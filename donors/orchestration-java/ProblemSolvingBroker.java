package com.aiassistant.core.orchestration;

import android.content.Context;
import android.util.Log;

import com.aiassistant.services.GroqApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class ProblemSolvingBroker {
    private static final String TAG = "ProblemSolvingBroker";
    
    private final Context context;
    private final GroqApiService groqApiService;
    private final Map<String, ProblemTicket> tickets;
    private final ExecutorService executorService;
    private final Semaphore rateLimiter;
    
    private static final int MAX_CONCURRENT_REQUESTS = 3;
    
    public ProblemSolvingBroker(Context context) {
        this.context = context.getApplicationContext();
        this.groqApiService = GroqApiService.getInstance(context);
        this.tickets = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool();
        this.rateLimiter = new Semaphore(MAX_CONCURRENT_REQUESTS);
    }
    
    public void submitProblem(ProblemTicket ticket) {
        tickets.put(ticket.getTicketId(), ticket);
        ticket.setStatus(ProblemTicket.TicketStatus.IN_PROGRESS);
        
        Log.i(TAG, "Problem ticket submitted: " + ticket.getTicketId() + 
              " for component " + ticket.getComponentId());
        
        executorService.execute(() -> solveProblem(ticket));
    }
    
    private void solveProblem(ProblemTicket ticket) {
        try {
            if (!rateLimiter.tryAcquire()) {
                Log.w(TAG, "Rate limit reached, queuing ticket: " + ticket.getTicketId());
                rateLimiter.acquire();
            }
            
            String prompt = buildProblemPrompt(ticket);
            
            groqApiService.chatCompletion(prompt, new GroqApiService.ChatCompletionCallback() {
                @Override
                public void onSuccess(String response) {
                    try {
                        handleSolution(ticket, response);
                    } finally {
                        rateLimiter.release();
                    }
                }
                
                @Override
                public void onError(String error) {
                    try {
                        handleError(ticket, error);
                    } finally {
                        rateLimiter.release();
                    }
                }
            });
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Interrupted while waiting for rate limiter", e);
            ticket.setStatus(ProblemTicket.TicketStatus.FAILED);
            rateLimiter.release();
        }
    }
    
    private String buildProblemPrompt(ProblemTicket ticket) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert AI system troubleshooter. ");
        prompt.append("Analyze this problem and provide a concise, actionable solution.\n\n");
        
        prompt.append("Component: ").append(ticket.getComponentId()).append("\n");
        prompt.append("Problem Type: ").append(ticket.getProblemType()).append("\n");
        prompt.append("Description: ").append(ticket.getDescription()).append("\n\n");
        
        if (!ticket.getAttemptedRemedies().isEmpty()) {
            prompt.append("Attempted Remedies:\n");
            for (String remedy : ticket.getAttemptedRemedies()) {
                prompt.append("- ").append(remedy).append("\n");
            }
            prompt.append("\n");
        }
        
        if (!ticket.getContext().isEmpty()) {
            prompt.append("Context: ").append(ticket.getContext()).append("\n\n");
        }
        
        prompt.append("Provide:\n");
        prompt.append("1. Root cause analysis\n");
        prompt.append("2. Recommended solution\n");
        prompt.append("3. Preventive measures\n");
        prompt.append("\nKeep your response concise and actionable.");
        
        return prompt.toString();
    }
    
    private void handleSolution(ProblemTicket ticket, String solution) {
        Log.i(TAG, "Solution received for ticket " + ticket.getTicketId());
        Log.d(TAG, "Solution: " + solution);
        
        ticket.resolve(solution);
        
        translateAndExecute(ticket, solution);
    }
    
    private void handleError(ProblemTicket ticket, String error) {
        Log.e(TAG, "Error solving ticket " + ticket.getTicketId() + ": " + error);
        
        ticket.setStatus(ProblemTicket.TicketStatus.FAILED);
        ticket.escalate();
    }
    
    private void translateAndExecute(ProblemTicket ticket, String solution) {
        Log.i(TAG, "Translating solution to actionable commands for " + 
              ticket.getComponentId());
        
        String componentId = ticket.getComponentId();
        
        if (solution.toLowerCase().contains("restart")) {
            Log.i(TAG, "Executing command: Restart component " + componentId);
        } else if (solution.toLowerCase().contains("reset")) {
            Log.i(TAG, "Executing command: Reset component " + componentId);
        } else if (solution.toLowerCase().contains("increase") && 
                   solution.toLowerCase().contains("timeout")) {
            Log.i(TAG, "Executing command: Increase timeout for " + componentId);
        }
        
        Log.d(TAG, "Full solution stored for component learning: " + solution);
    }
    
    public ProblemTicket getTicket(String ticketId) {
        return tickets.get(ticketId);
    }
    
    public List<ProblemTicket> getTicketsByComponent(String componentId) {
        List<ProblemTicket> result = new ArrayList<>();
        for (ProblemTicket ticket : tickets.values()) {
            if (ticket.getComponentId().equals(componentId)) {
                result.add(ticket);
            }
        }
        return result;
    }
    
    public List<ProblemTicket> getOpenTickets() {
        List<ProblemTicket> result = new ArrayList<>();
        for (ProblemTicket ticket : tickets.values()) {
            if (ticket.getStatus() == ProblemTicket.TicketStatus.OPEN ||
                ticket.getStatus() == ProblemTicket.TicketStatus.IN_PROGRESS) {
                result.add(ticket);
            }
        }
        return result;
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}

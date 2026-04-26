package com.aiassistant.core.orchestration;

import android.content.Context;
import android.util.Log;

import com.aiassistant.core.ErrorResolutionWorkflow;
import com.aiassistant.core.FeedbackSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrchestrationScheduler {
    private static final String TAG = "OrchestrationScheduler";
    
    private final Context context;
    private final ComponentRegistry componentRegistry;
    private final EventRouter eventRouter;
    
    private DiffEngine diffEngine;
    private HealthMonitor healthMonitor;
    private ProblemSolvingBroker problemSolvingBroker;
    private FeedbackSystem feedbackSystem;
    private ErrorResolutionWorkflow errorResolutionWorkflow;
    
    private final ExecutorService parallelExecutor;
    private final ExecutorService sequentialExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    
    private final Map<String, OrchestrationPipeline> pipelines;
    private final Map<String, TriggerRule> triggerRules;
    
    private boolean isRunning = false;
    
    public OrchestrationScheduler(Context context, ComponentRegistry componentRegistry, 
                                 EventRouter eventRouter) {
        this.context = context.getApplicationContext();
        this.componentRegistry = componentRegistry;
        this.eventRouter = eventRouter;
        
        this.parallelExecutor = Executors.newCachedThreadPool();
        this.sequentialExecutor = Executors.newSingleThreadExecutor();
        this.scheduledExecutor = Executors.newScheduledThreadPool(4);
        
        this.pipelines = new HashMap<>();
        this.triggerRules = new HashMap<>();
        
        initializeDefaultPipelines();
    }
    
    public void setDiffEngine(DiffEngine diffEngine) {
        this.diffEngine = diffEngine;
    }
    
    public void setHealthMonitor(HealthMonitor healthMonitor) {
        this.healthMonitor = healthMonitor;
    }
    
    public void setProblemSolvingBroker(ProblemSolvingBroker problemSolvingBroker) {
        this.problemSolvingBroker = problemSolvingBroker;
    }
    
    public void setFeedbackSystem(FeedbackSystem feedbackSystem) {
        this.feedbackSystem = feedbackSystem;
    }
    
    public void setErrorResolutionWorkflow(ErrorResolutionWorkflow errorResolutionWorkflow) {
        this.errorResolutionWorkflow = errorResolutionWorkflow;
    }
    
    private void initializeDefaultPipelines() {
        OrchestrationPipeline gameAnalysisPipeline = new OrchestrationPipeline("game_analysis");
        gameAnalysisPipeline.addStage("GameAnalyzer", true);
        gameAnalysisPipeline.addStage("BehaviorDetector", true);
        gameAnalysisPipeline.addStage("ActionRecommender", true);
        pipelines.put("game_analysis", gameAnalysisPipeline);
        
        OrchestrationPipeline voicePipeline = new OrchestrationPipeline("voice_processing");
        voicePipeline.addStage("VoiceRecognizer", true);
        voicePipeline.addStage("VoiceCommandProcessor", true);
        voicePipeline.addStage("VoiceResponseGenerator", true);
        pipelines.put("voice_processing", voicePipeline);
        
        OrchestrationPipeline monitoringPipeline = new OrchestrationPipeline("monitoring");
        monitoringPipeline.addStage("ScreenMonitor", false);
        monitoringPipeline.addStage("NetworkMonitor", false);
        monitoringPipeline.addStage("ContextAnalyzer", false);
        pipelines.put("monitoring", monitoringPipeline);
        
        Log.i(TAG, "Initialized " + pipelines.size() + " default pipelines");
    }
    
    public void start() {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        
        scheduleTriggerRules();
        
        Log.i(TAG, "Orchestration Scheduler started");
    }
    
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        
        Log.i(TAG, "Orchestration Scheduler stopped");
    }
    
    private void scheduleTriggerRules() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                evaluateTriggerRules();
            } catch (Exception e) {
                Log.e(TAG, "Error evaluating trigger rules", e);
            }
        }, 5, 10, TimeUnit.SECONDS);
    }
    
    private void evaluateTriggerRules() {
        for (Map.Entry<String, TriggerRule> entry : triggerRules.entrySet()) {
            TriggerRule rule = entry.getValue();
            
            if (rule.shouldTrigger()) {
                executePipeline(rule.pipelineName, rule.triggerData);
            }
        }
    }
    
    public void executePipeline(String pipelineName, Map<String, Object> data) {
        OrchestrationPipeline pipeline = pipelines.get(pipelineName);
        
        if (pipeline == null) {
            Log.w(TAG, "Pipeline not found: " + pipelineName);
            return;
        }
        
        Log.i(TAG, "Executing pipeline: " + pipelineName);
        
        long startTime = System.currentTimeMillis();
        
        if (pipeline.isSequential) {
            executeSequential(pipeline, data);
        } else {
            executeParallel(pipeline, data);
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Pipeline " + pipelineName + " completed in " + executionTime + "ms");
    }
    
    private void executeSequential(OrchestrationPipeline pipeline, Map<String, Object> data) {
        sequentialExecutor.execute(() -> {
            Map<String, Object> stageData = new HashMap<>(data);
            
            for (PipelineStage stage : pipeline.stages) {
                if (!isComponentHealthy(stage.componentId)) {
                    Log.w(TAG, "Skipping unhealthy component: " + stage.componentId);
                    continue;
                }
                
                CircuitBreaker breaker = healthMonitor.getCircuitBreaker(stage.componentId);
                if (breaker != null && !breaker.allowExecution()) {
                    Log.w(TAG, "Circuit breaker blocking execution: " + stage.componentId);
                    continue;
                }
                
                try {
                    Map<String, Object> result = executeStage(stage, stageData);
                    
                    if (result != null) {
                        stageData.putAll(result);
                        healthMonitor.recordSuccess(stage.componentId);
                    } else {
                        healthMonitor.recordError(stage.componentId, "execution_failed");
                        break;
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error executing stage: " + stage.componentId, e);
                    healthMonitor.recordError(stage.componentId, "exception");
                    
                    if (stage.critical) {
                        break;
                    }
                }
            }
        });
    }
    
    private void executeParallel(OrchestrationPipeline pipeline, Map<String, Object> data) {
        for (PipelineStage stage : pipeline.stages) {
            parallelExecutor.execute(() -> {
                if (!isComponentHealthy(stage.componentId)) {
                    Log.w(TAG, "Skipping unhealthy component: " + stage.componentId);
                    return;
                }
                
                CircuitBreaker breaker = healthMonitor.getCircuitBreaker(stage.componentId);
                if (breaker != null && !breaker.allowExecution()) {
                    Log.w(TAG, "Circuit breaker blocking execution: " + stage.componentId);
                    return;
                }
                
                try {
                    Map<String, Object> result = executeStage(stage, data);
                    
                    if (result != null) {
                        healthMonitor.recordSuccess(stage.componentId);
                    } else {
                        healthMonitor.recordError(stage.componentId, "execution_failed");
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error executing stage: " + stage.componentId, e);
                    healthMonitor.recordError(stage.componentId, "exception");
                }
            });
        }
    }
    
    private Map<String, Object> executeStage(PipelineStage stage, Map<String, Object> data) {
        Log.d(TAG, "Executing stage: " + stage.componentId);
        
        healthMonitor.recordHeartbeat(stage.componentId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("component", stage.componentId);
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }
    
    private boolean isComponentHealthy(String componentId) {
        return componentRegistry.isComponentHealthy(componentId);
    }
    
    public void adjustScheduling(String componentId, boolean enable) {
        if (enable) {
            Log.i(TAG, "Re-enabling component in scheduling: " + componentId);
        } else {
            Log.i(TAG, "Disabling component in scheduling: " + componentId);
        }
    }
    
    public void registerTriggerRule(String ruleId, TriggerRule rule) {
        triggerRules.put(ruleId, rule);
        Log.i(TAG, "Registered trigger rule: " + ruleId);
    }
    
    public void registerPipeline(String pipelineName, OrchestrationPipeline pipeline) {
        pipelines.put(pipelineName, pipeline);
        Log.i(TAG, "Registered pipeline: " + pipelineName);
    }
    
    public void shutdown() {
        parallelExecutor.shutdown();
        sequentialExecutor.shutdown();
        scheduledExecutor.shutdown();
    }
    
    public static class OrchestrationPipeline {
        public final String name;
        public final List<PipelineStage> stages;
        public boolean isSequential;
        
        public OrchestrationPipeline(String name) {
            this.name = name;
            this.stages = new ArrayList<>();
            this.isSequential = true;
        }
        
        public void addStage(String componentId, boolean critical) {
            stages.add(new PipelineStage(componentId, critical));
        }
        
        public void setParallel() {
            this.isSequential = false;
        }
    }
    
    public static class PipelineStage {
        public final String componentId;
        public final boolean critical;
        
        public PipelineStage(String componentId, boolean critical) {
            this.componentId = componentId;
            this.critical = critical;
        }
    }
    
    public static class TriggerRule {
        public final String pipelineName;
        public final String condition;
        public Map<String, Object> triggerData;
        
        private long lastTriggerTime;
        private final long minimumInterval;
        
        public TriggerRule(String pipelineName, String condition, long minimumInterval) {
            this.pipelineName = pipelineName;
            this.condition = condition;
            this.minimumInterval = minimumInterval;
            this.lastTriggerTime = 0;
            this.triggerData = new HashMap<>();
        }
        
        public boolean shouldTrigger() {
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - lastTriggerTime < minimumInterval) {
                return false;
            }
            
            boolean shouldTrigger = evaluateCondition();
            
            if (shouldTrigger) {
                lastTriggerTime = currentTime;
            }
            
            return shouldTrigger;
        }
        
        private boolean evaluateCondition() {
            return true;
        }
    }
}

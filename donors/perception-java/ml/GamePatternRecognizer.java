package com.aiassistant.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.aiassistant.detection.GameAppElementDetector;
import utils.RectHelper;

/**
 * Game Pattern Recognizer for Complex Game Strategies and Enemy Pattern Recognition
 * 
 * This system analyzes gameplay to identify recurring patterns in enemy behavior,
 * level design, and gameplay mechanics, enabling sophisticated counter-strategies.
 */
public class GamePatternRecognizer {
    private static final String TAG = "GamePatternRecognizer";
    
    // Singleton instance
    private static GamePatternRecognizer instance;
    
    // Context
    private final Context context;
    
    // Pattern databases
    private final Map<String, EnemyPattern> enemyPatterns = new ConcurrentHashMap<>();
    private final Map<String, GameStrategyPattern> strategyPatterns = new ConcurrentHashMap<>();
    private final Map<String, LevelPattern> levelPatterns = new ConcurrentHashMap<>();
    
    // Observation history
    private final List<GameObservation> recentObservations = new LinkedList<>();
    private final int maxObservationHistory = 1000;
    
    // Enemy tracking
    private final Map<String, EnemyTrackingData> enemyTracking = new ConcurrentHashMap<>();
    
    // Pattern detection executors
    private final Executor processingExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    
    // Pattern detection state
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong frameCounter = new AtomicLong(0);
    private String currentGameType = "unknown";
    
    // Pattern detection callbacks
    private final List<PatternRecognitionCallback> callbacks = new ArrayList<>();
    
    /**
     * Game observation
     */
    private static class GameObservation {
        final long timestamp;
        final List<GameAppElementDetector.UIElement> elements;
        final Map<String, Object> gameState;
        final Map<String, List<EnemyTrackingData>> enemies;
        
        public GameObservation(
                List<GameAppElementDetector.UIElement> elements,
                Map<String, Object> gameState,
                Map<String, List<EnemyTrackingData>> enemies) {
            this.timestamp = System.currentTimeMillis();
            this.elements = new ArrayList<>(elements);
            this.gameState = new HashMap<>(gameState);
            this.enemies = new HashMap<>();
            
            // Deep copy enemies data
            for (Map.Entry<String, List<EnemyTrackingData>> entry : enemies.entrySet()) {
                this.enemies.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public List<GameAppElementDetector.UIElement> getElements() {
            return elements;
        }
        
        public Map<String, Object> getGameState() {
            return gameState;
        }
        
        public Map<String, List<EnemyTrackingData>> getEnemies() {
            return enemies;
        }
    }
    
    /**
     * Enemy tracking data
     */
    public static class EnemyTrackingData {
        final String enemyId;
        final String enemyType;
        final RectF position;
        final float health;
        final Map<String, Object> attributes;
        final long timestamp;
        
        public EnemyTrackingData(
                String enemyId,
                String enemyType,
                RectF position,
                float health,
                Map<String, Object> attributes) {
            this.enemyId = enemyId;
            this.enemyType = enemyType;
            this.position = new RectF(position);
            this.health = health;
            this.attributes = new HashMap<>(attributes);
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getEnemyId() {
            return enemyId;
        }
        
        public String getEnemyType() {
            return enemyType;
        }
        
        public RectF getPosition() {
            return position;
        }
        
        public float getHealth() {
            return health;
        }
        
        public Map<String, Object> getAttributes() {
            return attributes;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * Enemy pattern
     */
    public static class EnemyPattern {
        final String patternId;
        final String enemyType;
        final List<PatternStep> steps;
        final long patternDuration;
        final float confidence;
        final long detectionCount;
        final String description;
        final Map<String, Object> metadata;
        
        public EnemyPattern(
                String patternId,
                String enemyType,
                List<PatternStep> steps,
                long patternDuration,
                float confidence) {
            this.patternId = patternId;
            this.enemyType = enemyType;
            this.steps = new ArrayList<>(steps);
            this.patternDuration = patternDuration;
            this.confidence = confidence;
            this.detectionCount = 1;
            this.description = generateDescription();
            this.metadata = new HashMap<>();
        }
        
        private String generateDescription() {
            if (steps.isEmpty()) {
                return "Unknown pattern";
            }
            
            StringBuilder description = new StringBuilder();
            description.append(enemyType).append(": ");
            
            for (int i = 0; i < Math.min(steps.size(), 3); i++) {
                PatternStep step = steps.get(i);
                if (i > 0) {
                    description.append(" → ");
                }
                description.append(step.getDescription());
            }
            
            if (steps.size() > 3) {
                description.append(" → ...");
            }
            
            return description.toString();
        }
        
        public String getPatternId() {
            return patternId;
        }
        
        public String getEnemyType() {
            return enemyType;
        }
        
        public List<PatternStep> getSteps() {
            return steps;
        }
        
        public long getPatternDuration() {
            return patternDuration;
        }
        
        public float getConfidence() {
            return confidence;
        }
        
        public long getDetectionCount() {
            return detectionCount;
        }
        
        public String getDescription() {
            return description;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
    
    /**
     * Game strategy pattern
     */
    public static class GameStrategyPattern {
        final String patternId;
        final String strategyName;
        final List<PatternStep> steps;
        final float successRate;
        final long detectionCount;
        final String gameContext;
        final String description;
        final Map<String, Object> preconditions;
        final Map<String, Object> outcomes;
        
        public GameStrategyPattern(
                String patternId,
                String strategyName,
                List<PatternStep> steps,
                float successRate,
                String gameContext) {
            this.patternId = patternId;
            this.strategyName = strategyName;
            this.steps = new ArrayList<>(steps);
            this.successRate = successRate;
            this.detectionCount = 1;
            this.gameContext = gameContext;
            this.description = generateDescription();
            this.preconditions = new HashMap<>();
            this.outcomes = new HashMap<>();
        }
        
        private String generateDescription() {
            StringBuilder description = new StringBuilder();
            description.append(strategyName).append(" (").append(gameContext).append("): ");
            
            for (int i = 0; i < Math.min(steps.size(), 3); i++) {
                PatternStep step = steps.get(i);
                if (i > 0) {
                    description.append(" → ");
                }
                description.append(step.getDescription());
            }
            
            if (steps.size() > 3) {
                description.append(" → ...");
            }
            
            return description.toString();
        }
        
        public String getPatternId() {
            return patternId;
        }
        
        public String getStrategyName() {
            return strategyName;
        }
        
        public List<PatternStep> getSteps() {
            return steps;
        }
        
        public float getSuccessRate() {
            return successRate;
        }
        
        public long getDetectionCount() {
            return detectionCount;
        }
        
        public String getGameContext() {
            return gameContext;
        }
        
        public String getDescription() {
            return description;
        }
        
        public Map<String, Object> getPreconditions() {
            return preconditions;
        }
        
        public Map<String, Object> getOutcomes() {
            return outcomes;
        }
    }
    
    /**
     * Level pattern
     */
    public static class LevelPattern {
        final String patternId;
        final String levelType;
        final Map<String, List<String>> elementDistribution;
        final List<String> sequence;
        final Map<String, Float> resourceDistribution;
        final Map<String, Object> metadata;
        
        public LevelPattern(
                String patternId,
                String levelType,
                Map<String, List<String>> elementDistribution,
                List<String> sequence,
                Map<String, Float> resourceDistribution) {
            this.patternId = patternId;
            this.levelType = levelType;
            this.elementDistribution = new HashMap<>(elementDistribution);
            this.sequence = new ArrayList<>(sequence);
            this.resourceDistribution = new HashMap<>(resourceDistribution);
            this.metadata = new HashMap<>();
        }
        
        public String getPatternId() {
            return patternId;
        }
        
        public String getLevelType() {
            return levelType;
        }
        
        public Map<String, List<String>> getElementDistribution() {
            return elementDistribution;
        }
        
        public List<String> getSequence() {
            return sequence;
        }
        
        public Map<String, Float> getResourceDistribution() {
            return resourceDistribution;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
    
    /**
     * Pattern step
     */
    public static class PatternStep {
        final String actionType;
        final Map<String, Object> parameters;
        final long durationMs;
        final Map<String, Object> conditions;
        final String description;
        
        public PatternStep(
                String actionType,
                Map<String, Object> parameters,
                long durationMs,
                Map<String, Object> conditions) {
            this.actionType = actionType;
            this.parameters = new HashMap<>(parameters);
            this.durationMs = durationMs;
            this.conditions = new HashMap<>(conditions);
            this.description = generateDescription();
        }
        
        private String generateDescription() {
            StringBuilder desc = new StringBuilder(actionType);
            
            if (parameters.containsKey("direction")) {
                desc.append(" ").append(parameters.get("direction"));
            }
            
            if (parameters.containsKey("target")) {
                desc.append(" at ").append(parameters.get("target"));
            }
            
            if (durationMs > 0) {
                desc.append(" (").append(durationMs).append("ms)");
            }
            
            return desc.toString();
        }
        
        public String getActionType() {
            return actionType;
        }
        
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        public long getDurationMs() {
            return durationMs;
        }
        
        public Map<String, Object> getConditions() {
            return conditions;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Game context information
     */
    public static class GameContext {
        final String gameType;
        final String gameMode;
        final String screenType;
        final Map<String, Object> gameState;
        
        public GameContext(
                String gameType,
                String gameMode,
                String screenType,
                Map<String, Object> gameState) {
            this.gameType = gameType;
            this.gameMode = gameMode;
            this.screenType = screenType;
            this.gameState = new HashMap<>(gameState);
        }
        
        public String getGameType() {
            return gameType;
        }
        
        public String getGameMode() {
            return gameMode;
        }
        
        public String getScreenType() {
            return screenType;
        }
        
        public Map<String, Object> getGameState() {
            return gameState;
        }
    }
    
    /**
     * Pattern recognition result
     */
    public static class PatternRecognitionResult {
        final String patternType; // "enemy", "strategy", "level"
        final String patternId;
        final String description;
        final float confidence;
        final long detectionTimestamp;
        final Map<String, Object> details;
        
        public PatternRecognitionResult(
                String patternType,
                String patternId,
                String description,
                float confidence,
                Map<String, Object> details) {
            this.patternType = patternType;
            this.patternId = patternId;
            this.description = description;
            this.confidence = confidence;
            this.detectionTimestamp = System.currentTimeMillis();
            this.details = details;
        }
        
        public String getPatternType() {
            return patternType;
        }
        
        public String getPatternId() {
            return patternId;
        }
        
        public String getDescription() {
            return description;
        }
        
        public float getConfidence() {
            return confidence;
        }
        
        public long getDetectionTimestamp() {
            return detectionTimestamp;
        }
        
        public Map<String, Object> getDetails() {
            return details;
        }
    }
    
    /**
     * Pattern recognition callback
     */
    public interface PatternRecognitionCallback {
        void onPatternRecognized(PatternRecognitionResult result);
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized GamePatternRecognizer getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new GamePatternRecognizer(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private GamePatternRecognizer(Context context) {
        this.context = context;
        this.processingExecutor = Executors.newFixedThreadPool(2);
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        
        Log.i(TAG, "Game pattern recognizer created");
    }
    
    /**
     * Register pattern recognition callback
     */
    public void registerCallback(PatternRecognitionCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }
    
    /**
     * Unregister pattern recognition callback
     */
    public void unregisterCallback(PatternRecognitionCallback callback) {
        callbacks.remove(callback);
    }
    
    /**
     * Start pattern recognition
     */
    public void start() {
        if (running.get()) {
            return;
        }
        
        running.set(true);
        
        // Schedule pattern analysis
        scheduledExecutor.scheduleAtFixedRate(
                this::analyzePatterns,
                500, // Initial delay
                1000, // 1 second interval
                TimeUnit.MILLISECONDS);
        
        Log.i(TAG, "Game pattern recognizer started");
    }
    
    /**
     * Stop pattern recognition
     */
    public void stop() {
        if (!running.get()) {
            return;
        }
        
        running.set(false);
        
        Log.i(TAG, "Game pattern recognizer stopped");
    }
    
    /**
     * Set game type
     */
    public void setGameType(String gameType) {
        this.currentGameType = gameType;
    }
    
    /**
     * Process a game frame
     */
    public void processFrame(List<GameAppElementDetector.UIElement> elements, 
                           Map<String, Object> gameState) {
        if (!running.get() || elements == null || gameState == null) {
            return;
        }
        
        // Update frame counter
        frameCounter.incrementAndGet();
        
        // Extract enemies from elements
        Map<String, List<EnemyTrackingData>> frameEnemies = extractEnemiesFromElements(elements, gameState);
        
        // Create observation
        GameObservation observation = new GameObservation(elements, gameState, frameEnemies);
        
        // Add to recent observations
        synchronized (recentObservations) {
            recentObservations.add(observation);
            
            // Keep observations bounded
            while (recentObservations.size() > maxObservationHistory) {
                recentObservations.remove(0);
            }
        }
        
        // Process new observation
        processingExecutor.execute(() -> processNewObservation(observation));
    }
    
    /**
     * Extract enemies from UI elements
     */
    private Map<String, List<EnemyTrackingData>> extractEnemiesFromElements(
            List<GameAppElementDetector.UIElement> elements, Map<String, Object> gameState) {
        Map<String, List<EnemyTrackingData>> enemies = new HashMap<>();
        
        for (GameAppElementDetector.UIElement element : elements) {
            String type = element.getType().toLowerCase();
            
            // Check if element appears to be an enemy
            if (type.contains("enemy") || type.contains("opponent") || 
                    type.contains("monster") || type.contains("boss")) {
                
                // Generate enemy ID if not available
                String enemyId = element.getId();
                if (enemyId == null || enemyId.isEmpty()) {
                    enemyId = "enemy_" + element.getBounds().centerX() + "_" + 
                            element.getBounds().centerY();
                }
                
                // Extract health if available
                float health = 1.0f;
                if (element.getAttributes().containsKey("health")) {
                    Object healthObj = element.getAttributes().get("health");
                    if (healthObj instanceof Number) {
                        health = ((Number) healthObj).floatValue();
                    }
                }
                
                // Create enemy tracking data - convert Rect to RectF using helper
                android.graphics.Rect rectBounds = element.getBounds();
                android.graphics.RectF rectFBounds = RectHelper.toRectF(rectBounds);
                
                EnemyTrackingData trackingData = new EnemyTrackingData(
                        enemyId,
                        type,
                        rectFBounds,
                        health,
                        element.getAttributes());
                
                // Store tracking data
                String enemyType = getEnemyType(element);
                if (!enemies.containsKey(enemyType)) {
                    enemies.put(enemyType, new ArrayList<>());
                }
                enemies.get(enemyType).add(trackingData);
                
                // Update ongoing enemy tracking
                enemyTracking.put(enemyId, trackingData);
            }
        }
        
        return enemies;
    }
    
    /**
     * Get enemy type from UI element
     */
    private String getEnemyType(GameAppElementDetector.UIElement element) {
        // Try to extract enemy type from attributes
        if (element.getAttributes().containsKey("enemyType")) {
            return element.getAttributes().get("enemyType").toString();
        }
        
        // Fall back to element type
        String type = element.getType().toLowerCase();
        
        // Extract more specific type if available
        if (type.contains("boss")) {
            return "boss";
        } else if (type.contains("ranged")) {
            return "ranged_enemy";
        } else if (type.contains("melee")) {
            return "melee_enemy";
        } else if (type.contains("flying")) {
            return "flying_enemy";
        } else {
            return "generic_enemy";
        }
    }
    
    /**
     * Process new observation for pattern detection
     */
    private void processNewObservation(GameObservation observation) {
        // Check enemy patterns
        for (Map.Entry<String, List<EnemyTrackingData>> entry : observation.getEnemies().entrySet()) {
            String enemyType = entry.getKey();
            List<EnemyTrackingData> enemies = entry.getValue();
            
            for (EnemyTrackingData enemy : enemies) {
                checkEnemyPatterns(enemy, observation);
            }
        }
        
        // Check game strategy patterns
        checkStrategyPatterns(observation);
        
        // Check level patterns
        checkLevelPatterns(observation);
    }
    
    /**
     * Check for enemy patterns
     */
    private void checkEnemyPatterns(EnemyTrackingData enemy, GameObservation observation) {
        // Get recent observations for this enemy
        List<GameObservation> relevantObservations = new ArrayList<>();
        String enemyId = enemy.getEnemyId();
        
        synchronized (recentObservations) {
            for (GameObservation obs : recentObservations) {
                for (List<EnemyTrackingData> enemyList : obs.getEnemies().values()) {
                    for (EnemyTrackingData e : enemyList) {
                        if (e.getEnemyId().equals(enemyId)) {
                            relevantObservations.add(obs);
                            break;
                        }
                    }
                }
            }
        }
        
        // Need enough observations for pattern detection
        if (relevantObservations.size() < 3) {
            return;
        }
        
        // Extract movement pattern
        List<PatternStep> steps = extractEnemyMovementPattern(enemy, relevantObservations);
        if (steps.size() >= 2) {
            // Calculate pattern duration
            long duration = relevantObservations.get(relevantObservations.size() - 1).getTimestamp() -
                    relevantObservations.get(0).getTimestamp();
            
            // Check if pattern matches existing patterns
            boolean foundMatch = false;
            for (EnemyPattern pattern : enemyPatterns.values()) {
                if (pattern.getEnemyType().equals(enemy.getEnemyType()) &&
                        patternStepsMatch(steps, pattern.getSteps())) {
                    foundMatch = true;
                    
                    // Notify pattern match
                    notifyPatternRecognized(
                            "enemy",
                            pattern.getPatternId(),
                            pattern.getDescription(),
                            pattern.getConfidence(),
                            createEnemyPatternDetails(pattern, enemy));
                    
                    break;
                }
            }
            
            // If no match, create new pattern
            if (!foundMatch && steps.size() >= 3) {  // Require 3+ steps for new pattern
                String patternId = "enemy_pattern_" + enemy.getEnemyType() + "_" + 
                        System.currentTimeMillis();
                
                EnemyPattern newPattern = new EnemyPattern(
                        patternId,
                        enemy.getEnemyType(),
                        steps,
                        duration,
                        0.6f);  // Initial confidence
                
                enemyPatterns.put(patternId, newPattern);
                
                // Notify new pattern
                notifyPatternRecognized(
                        "enemy",
                        patternId,
                        newPattern.getDescription(),
                        newPattern.getConfidence(),
                        createEnemyPatternDetails(newPattern, enemy));
                
                Log.d(TAG, "New enemy pattern detected: " + newPattern.getDescription());
            }
        }
    }
    
    /**
     * Extract enemy movement pattern
     */
    private List<PatternStep> extractEnemyMovementPattern(
            EnemyTrackingData enemy, List<GameObservation> observations) {
        List<PatternStep> steps = new ArrayList<>();
        
        if (observations.size() < 2) {
            return steps;
        }
        
        // Track last position
        RectF lastPosition = null;
        long lastTimestamp = 0;
        String lastDirection = null;
        
        for (GameObservation obs : observations) {
            // Find this enemy in the observation
            EnemyTrackingData currentEnemy = null;
            for (List<EnemyTrackingData> enemies : obs.getEnemies().values()) {
                for (EnemyTrackingData e : enemies) {
                    if (e.getEnemyId().equals(enemy.getEnemyId())) {
                        currentEnemy = e;
                        break;
                    }
                }
                if (currentEnemy != null) {
                    break;
                }
            }
            
            // Skip if enemy not found
            if (currentEnemy == null) {
                continue;
            }
            
            RectF currentPosition = currentEnemy.getPosition();
            long currentTimestamp = obs.getTimestamp();
            
            // Skip first observation
            if (lastPosition == null) {
                lastPosition = currentPosition;
                lastTimestamp = currentTimestamp;
                continue;
            }
            
            // Calculate movement
            float dx = currentPosition.centerX() - lastPosition.centerX();
            float dy = currentPosition.centerY() - lastPosition.centerY();
            long duration = currentTimestamp - lastTimestamp;
            
            // Determine direction
            String direction;
            if (Math.abs(dx) > Math.abs(dy)) {
                // Horizontal movement dominates
                direction = dx > 0 ? "right" : "left";
            } else {
                // Vertical movement dominates
                direction = dy > 0 ? "down" : "up";
            }
            
            // If direction changed or significant time passed, record as step
            if (lastDirection == null || !lastDirection.equals(direction) || duration > 1000) {
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("direction", direction);
                parameters.put("speed", Math.sqrt(dx * dx + dy * dy) / duration);
                
                Map<String, Object> conditions = new HashMap<>();
                
                PatternStep step = new PatternStep(
                        "move",
                        parameters,
                        duration,
                        conditions);
                
                steps.add(step);
                lastDirection = direction;
            }
            
            // Update last position
            lastPosition = currentPosition;
            lastTimestamp = currentTimestamp;
        }
        
        return steps;
    }
    
    /**
     * Check for strategy patterns
     */
    private void checkStrategyPatterns(GameObservation observation) {
        // Strategy pattern detection is more complex and would require 
        // analysis of player actions and outcomes over time
        // This is a simplified placeholder implementation
        
        // In a full implementation, this would:
        // 1. Extract player actions from recent observations
        // 2. Identify sequences that led to positive outcomes
        // 3. Compare with known strategy patterns
        // 4. Create new patterns for successful sequences
    }
    
    /**
     * Check for level patterns
     */
    private void checkLevelPatterns(GameObservation observation) {
        // In a full implementation, this would:
        // 1. Extract level features (obstacles, enemies, resources)
        // 2. Build a map of the level as it's explored
        // 3. Identify common structures and sequences
        // 4. Recognize level generation patterns
    }
    
    /**
     * Compare pattern steps for similarity
     */
    private boolean patternStepsMatch(List<PatternStep> steps1, List<PatternStep> steps2) {
        if (steps1.size() != steps2.size()) {
            return false;
        }
        
        for (int i = 0; i < steps1.size(); i++) {
            PatternStep step1 = steps1.get(i);
            PatternStep step2 = steps2.get(i);
            
            // Check action type
            if (!step1.getActionType().equals(step2.getActionType())) {
                return false;
            }
            
            // Check direction parameter
            if (step1.getParameters().containsKey("direction") && 
                    step2.getParameters().containsKey("direction")) {
                if (!step1.getParameters().get("direction").equals(
                        step2.getParameters().get("direction"))) {
                    return false;
                }
            }
            
            // Duration can vary somewhat
            long duration1 = step1.getDurationMs();
            long duration2 = step2.getDurationMs();
            if (Math.abs(duration1 - duration2) > Math.max(duration1, duration2) * 0.3) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Create enemy pattern details
     */
    private Map<String, Object> createEnemyPatternDetails(
            EnemyPattern pattern, EnemyTrackingData enemy) {
        Map<String, Object> details = new HashMap<>();
        
        details.put("enemyType", enemy.getEnemyType());
        details.put("enemyId", enemy.getEnemyId());
        details.put("patternSteps", pattern.getSteps().size());
        details.put("patternDuration", pattern.getPatternDuration());
        details.put("confidence", pattern.getConfidence());
        details.put("currentPosition", new float[] {
                enemy.getPosition().centerX(),
                enemy.getPosition().centerY()
        });
        
        // Add counter strategy
        details.put("counterStrategy", generateCounterStrategy(pattern));
        
        return details;
    }
    
    /**
     * Generate counter strategy for enemy pattern
     */
    private String generateCounterStrategy(EnemyPattern pattern) {
        // Analyze pattern to determine counter strategy
        StringBuilder strategy = new StringBuilder();
        
        // Count movement directions
        int leftCount = 0;
        int rightCount = 0;
        int upCount = 0;
        int downCount = 0;
        
        for (PatternStep step : pattern.getSteps()) {
            if (step.getParameters().containsKey("direction")) {
                String direction = (String) step.getParameters().get("direction");
                if ("left".equals(direction)) {
                    leftCount++;
                } else if ("right".equals(direction)) {
                    rightCount++;
                } else if ("up".equals(direction)) {
                    upCount++;
                } else if ("down".equals(direction)) {
                    downCount++;
                }
            }
        }
        
        // Determine dominant direction
        if (leftCount > rightCount && leftCount > upCount && leftCount > downCount) {
            strategy.append("Counter: Position to the right. ");
        } else if (rightCount > leftCount && rightCount > upCount && rightCount > downCount) {
            strategy.append("Counter: Position to the left. ");
        } else if (upCount > leftCount && upCount > rightCount && upCount > downCount) {
            strategy.append("Counter: Position below. ");
        } else if (downCount > leftCount && downCount > rightCount && downCount > upCount) {
            strategy.append("Counter: Position above. ");
        } else {
            strategy.append("Counter: Stay mobile and observe pattern. ");
        }
        
        // Add timing advice
        long patternDuration = pattern.getPatternDuration();
        if (patternDuration > 0) {
            strategy.append("Pattern repeats every ~").append(patternDuration / 1000)
                    .append(" seconds. ");
        }
        
        // Add attack advice
        strategy.append("Attack during direction changes for best results.");
        
        return strategy.toString();
    }
    
    /**
     * Analyze patterns periodically
     */
    private void analyzePatterns() {
        if (!running.get()) {
            return;
        }
        
        // Get a snapshot of recent observations
        List<GameObservation> observations;
        synchronized (recentObservations) {
            observations = new ArrayList<>(recentObservations);
        }
        
        // Need enough observations for analysis
        if (observations.size() < 10) {
            return;
        }
        
        // Extract game context
        GameContext context = extractGameContext(observations);
        
        // Analyze enemy patterns
        analyzeEnemyPatterns(observations, context);
        
        // Analyze strategy patterns
        analyzeStrategyPatterns(observations, context);
        
        // Analyze level patterns
        analyzeLevelPatterns(observations, context);
    }
    
    /**
     * Extract game context from observations
     */
    private GameContext extractGameContext(List<GameObservation> observations) {
        // Use most recent observation for state
        GameObservation latestObs = observations.get(observations.size() - 1);
        Map<String, Object> gameState = latestObs.getGameState();
        
        // Extract game mode if available
        String gameMode = "unknown";
        if (gameState.containsKey("gameMode")) {
            gameMode = gameState.get("gameMode").toString();
        }
        
        // Extract screen type if available
        String screenType = "unknown";
        if (gameState.containsKey("screenType")) {
            screenType = gameState.get("screenType").toString();
        }
        
        return new GameContext(
                currentGameType,
                gameMode,
                screenType,
                gameState);
    }
    
    /**
     * Analyze enemy patterns deeply
     */
    private void analyzeEnemyPatterns(List<GameObservation> observations, GameContext context) {
        // In a full implementation, this would:
        // 1. Identify recurring enemy behaviors across multiple encounters
        // 2. Determine attack patterns, vulnerabilities, and timing
        // 3. Generate optimal counter-strategies
    }
    
    /**
     * Analyze strategy patterns deeply
     */
    private void analyzeStrategyPatterns(List<GameObservation> observations, GameContext context) {
        // In a full implementation, this would:
        // 1. Analyze successful player strategies
        // 2. Identify key decision points and optimal choices
        // 3. Refine strategies based on outcomes
    }
    
    /**
     * Analyze level patterns deeply
     */
    private void analyzeLevelPatterns(List<GameObservation> observations, GameContext context) {
        // In a full implementation, this would:
        // 1. Map level layouts and structures
        // 2. Identify procedural generation patterns
        // 3. Optimize navigation and resource collection
    }
    
    /**
     * Notify pattern recognition callbacks
     */
    private void notifyPatternRecognized(
            String patternType, String patternId, String description, 
            float confidence, Map<String, Object> details) {
        
        PatternRecognitionResult result = new PatternRecognitionResult(
                patternType, patternId, description, confidence, details);
        
        for (PatternRecognitionCallback callback : callbacks) {
            try {
                callback.onPatternRecognized(result);
            } catch (Exception e) {
                Log.e(TAG, "Error in pattern recognition callback: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get detected enemy patterns
     */
    public List<EnemyPattern> getEnemyPatterns() {
        return new ArrayList<>(enemyPatterns.values());
    }
    
    /**
     * Get detected strategy patterns
     */
    public List<GameStrategyPattern> getStrategyPatterns() {
        return new ArrayList<>(strategyPatterns.values());
    }
    
    /**
     * Get detected level patterns
     */
    public List<LevelPattern> getLevelPatterns() {
        return new ArrayList<>(levelPatterns.values());
    }
    
    /**
     * Get enemies currently being tracked
     */
    public List<EnemyTrackingData> getTrackedEnemies() {
        return new ArrayList<>(enemyTracking.values());
    }
    
    /**
     * Get statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("running", running.get());
        stats.put("frameCount", frameCounter.get());
        stats.put("observationCount", recentObservations.size());
        stats.put("enemyPatternCount", enemyPatterns.size());
        stats.put("strategyPatternCount", strategyPatterns.size());
        stats.put("levelPatternCount", levelPatterns.size());
        stats.put("trackedEnemyCount", enemyTracking.size());
        
        return stats;
    }
    
    /**
     * Reset statistics
     */
    public void resetStats() {
        frameCounter.set(0);
    }
    
    /**
     * Release resources
     */
    public void release() {
        // Stop pattern recognition
        stop();
        
        // Shutdown executors
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Clear data
        synchronized (recentObservations) {
            recentObservations.clear();
        }
        enemyPatterns.clear();
        strategyPatterns.clear();
        levelPatterns.clear();
        enemyTracking.clear();
        callbacks.clear();
        
        Log.i(TAG, "Game pattern recognizer released");
    }
}
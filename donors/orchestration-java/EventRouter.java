package com.aiassistant.core.orchestration;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventRouter {
    private static final String TAG = "EventRouter";
    
    private final Context context;
    private final Map<String, List<EventSubscriber>> subscribers;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    
    public EventRouter(Context context) {
        this.context = context.getApplicationContext();
        this.subscribers = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public void subscribe(String eventType, EventSubscriber subscriber) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(subscriber);
        Log.d(TAG, "Subscriber added for event type: " + eventType);
    }
    
    public void unsubscribe(String eventType, EventSubscriber subscriber) {
        List<EventSubscriber> eventSubscribers = subscribers.get(eventType);
        if (eventSubscribers != null) {
            eventSubscribers.remove(subscriber);
        }
    }
    
    public void publish(OrchestrationEvent event) {
        publish(event, false);
    }
    
    public void publish(OrchestrationEvent event, boolean synchronous) {
        String eventType = event.getEventType();
        List<EventSubscriber> eventSubscribers = subscribers.get(eventType);
        
        if (eventSubscribers == null || eventSubscribers.isEmpty()) {
            Log.d(TAG, "No subscribers for event: " + eventType);
            return;
        }
        
        Log.d(TAG, "Publishing event: " + eventType + " from " + event.getSource() + 
              " to " + eventSubscribers.size() + " subscribers");
        
        if (synchronous) {
            for (EventSubscriber subscriber : eventSubscribers) {
                try {
                    subscriber.onEvent(event);
                } catch (Exception e) {
                    Log.e(TAG, "Error in subscriber for event " + eventType, e);
                }
            }
        } else {
            executorService.execute(() -> {
                for (EventSubscriber subscriber : eventSubscribers) {
                    try {
                        subscriber.onEvent(event);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in subscriber for event " + eventType, e);
                    }
                }
            });
        }
    }
    
    public void publishOnMainThread(OrchestrationEvent event) {
        mainHandler.post(() -> publish(event, true));
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
    
    public interface EventSubscriber {
        void onEvent(OrchestrationEvent event);
    }
}

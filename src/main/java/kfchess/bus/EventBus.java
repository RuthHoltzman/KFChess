package kfchess.bus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Generic publish/subscribe hub: lets GameEngine announce events without knowing who's listening. */
public class EventBus {

    private final Map<Class<? extends GameEvent>, List<Consumer<? extends GameEvent>>> listeners =
            new ConcurrentHashMap<>();

    /** Registers a listener for one specific event type. */
    public <T extends GameEvent> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, type -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /** Delivers an event to every listener subscribed to its exact type. */
    @SuppressWarnings("unchecked")
    public void publish(GameEvent event) {
        List<Consumer<? extends GameEvent>> subscribers = listeners.get(event.getClass());
        if (subscribers == null) {
            return;
        }
        for (Consumer<? extends GameEvent> listener : subscribers) {
            ((Consumer<GameEvent>) listener).accept(event);
        }
    }
}
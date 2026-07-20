package kfchess.bus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Event bus גנרי: אפשר להירשם (subscribe) לפי *סוג* אירוע ספציפי,
 * ולפרסם (publish) אירוע - כל המאזינים הרשומים לאותו סוג בדיוק מקבלים
 * אותו. thread-safe: ConcurrentHashMap + CopyOnWriteArrayList, כי
 * בהמשך (שרת) כמה חוטי לקוחות יכולים לפרסם/להירשם בו-זמנית.
 */
public class EventBus {

    private final Map<Class<? extends GameEvent>, List<Consumer<? extends GameEvent>>> listeners =
            new ConcurrentHashMap<>();

    public <T extends GameEvent> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, type -> new CopyOnWriteArrayList<>()).add(listener);
    }

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
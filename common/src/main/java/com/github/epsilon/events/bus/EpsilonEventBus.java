package com.github.epsilon.events.bus;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight reflection-based event bus for Epsilon.
 * Scans for methods annotated with {@link EventHandler} that accept a single event parameter.
 */
public class EpsilonEventBus {

    public static final EpsilonEventBus INSTANCE = new EpsilonEventBus();

    private final Map<Object, List<ListenerMethod>> listenerCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, CopyOnWriteArrayList<ListenerMethod>> listenerMap = new ConcurrentHashMap<>();

    private EpsilonEventBus() {
    }

    /**
     * Posts a non-cancellable event.
     */
    @SuppressWarnings("unchecked")
    public <T> T post(T event) {
        CopyOnWriteArrayList<ListenerMethod> listeners = listenerMap.get(event.getClass());
        if (listeners != null) {
            for (ListenerMethod lm : listeners) {
                try {
                    lm.method.invoke(lm.instance, event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return event;
    }

    /**
     * Posts a cancellable event — stops propagation once cancelled.
     */
    public <T extends ICancellable> T postCancellable(T event) {
        CopyOnWriteArrayList<ListenerMethod> listeners = listenerMap.get(event.getClass());
        if (listeners != null) {
            for (ListenerMethod lm : listeners) {
                try {
                    lm.method.invoke(lm.instance, event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (event.isCancelled()) break;
            }
        }
        return event;
    }

    public void subscribe(Object object) {
        List<ListenerMethod> methods = listenerCache.computeIfAbsent(object, o -> collectListeners(o.getClass(), o));
        for (ListenerMethod lm : methods) {
            CopyOnWriteArrayList<ListenerMethod> list = listenerMap.computeIfAbsent(lm.eventType, k -> new CopyOnWriteArrayList<>());
            insertSorted(list, lm);
        }
    }

    public void unsubscribe(Object object) {
        List<ListenerMethod> methods = listenerCache.remove(object);
        if (methods == null) return;
        for (ListenerMethod lm : methods) {
            CopyOnWriteArrayList<ListenerMethod> list = listenerMap.get(lm.eventType);
            if (list != null) list.remove(lm);
        }
    }

    private void insertSorted(CopyOnWriteArrayList<ListenerMethod> list, ListenerMethod lm) {
        // Higher priority value = called first
        int i = 0;
        for (; i < list.size(); i++) {
            if (lm.priority > list.get(i).priority) break;
        }
        list.add(i, lm);
    }

    private List<ListenerMethod> collectListeners(Class<?> klass, Object instance) {
        List<ListenerMethod> result = new CopyOnWriteArrayList<>();
        collectListenersRecursive(result, klass, instance);
        return result;
    }

    private void collectListenersRecursive(List<ListenerMethod> result, Class<?> klass, Object instance) {
        for (Method method : klass.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(EventHandler.class)) continue;
            if (method.getParameterCount() != 1) continue;
            if (method.getReturnType() != void.class) continue;
            Class<?> eventType = method.getParameterTypes()[0];
            if (eventType.isPrimitive()) continue;

            method.setAccessible(true);
            int priority = method.getAnnotation(EventHandler.class).priority();
            result.add(new ListenerMethod(instance, method, eventType, priority));
        }
        Class<?> superclass = klass.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            collectListenersRecursive(result, superclass, instance);
        }
    }

    private static class ListenerMethod {
        final Object instance;
        final Method method;
        final Class<?> eventType;
        final int priority;

        ListenerMethod(Object instance, Method method, Class<?> eventType, int priority) {
            this.instance = instance;
            this.method = method;
            this.eventType = eventType;
            this.priority = priority;
        }
    }
}


package com.github.epsilon.events.tick;

/**
 * Fired every client tick. Replaces NeoForge's ClientTickEvent.
 */
public class TickEvent {

    public static class Pre extends TickEvent {
    }

    public static class Post extends TickEvent {
    }
}


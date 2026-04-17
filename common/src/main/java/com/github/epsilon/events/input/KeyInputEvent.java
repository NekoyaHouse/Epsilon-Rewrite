package com.github.epsilon.events.input;

/**
 * Fired when a key is pressed. Replaces NeoForge's InputEvent.Key.
 */
public class KeyInputEvent {

    private final int key;
    private final int scanCode;
    private final int action;
    private final int modifiers;

    public KeyInputEvent(int key, int scanCode, int action, int modifiers) {
        this.key = key;
        this.scanCode = scanCode;
        this.action = action;
        this.modifiers = modifiers;
    }

    public int getKey() {
        return key;
    }

    public int getScanCode() {
        return scanCode;
    }

    public int getAction() {
        return action;
    }

    public int getModifiers() {
        return modifiers;
    }
}


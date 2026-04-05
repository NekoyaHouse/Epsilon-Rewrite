package com.github.epsilon.managers;

public class TimerManager {

    public static final TimerManager INSTANCE = new TimerManager();

    private TimerManager() {
    }

    private float timer = 1f;
    private float lastTimer;

    public void set(float factor) {
        if (factor < 0.1f) factor = 0.1f;
        timer = factor;
    }

    public void reset() {
        timer = getDefault();
        lastTimer = timer;
    }

    public void tryReset() {
        if (lastTimer != getDefault()) {
            reset();
        }
    }

    public float get() {
        return timer;
    }

    public float getDefault() {
        return 1f;//return TimerModule.INSTANCE.isOn() ? (TimerModule.INSTANCE.boostKey.isPressed() ? TimerModule.INSTANCE.boost.getValueFloat() : TimerModule.INSTANCE.multiplier.getValueFloat()) : 1f;
    }

}

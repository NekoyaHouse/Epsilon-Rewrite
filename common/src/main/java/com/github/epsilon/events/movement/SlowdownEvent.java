package com.github.epsilon.events.movement;

public class SlowdownEvent {
    private boolean slowdown;
    public SlowdownEvent(boolean slowdown) { this.slowdown = slowdown; }
    public boolean isSlowdown() { return slowdown; }
    public void setSlowdown(boolean slowdown) { this.slowdown = slowdown; }
}


package com.github.epsilon.events.movement;

public class FallFlyingEvent {
    private float pitch;
    public FallFlyingEvent(float pitch) { this.pitch = pitch; }
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
}


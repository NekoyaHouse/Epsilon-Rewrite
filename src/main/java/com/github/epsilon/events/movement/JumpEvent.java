package com.github.epsilon.events.movement;

public class JumpEvent {
    private float yaw;
    public JumpEvent(float yaw) { this.yaw = yaw; }
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
}


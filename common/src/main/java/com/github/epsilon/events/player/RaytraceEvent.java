package com.github.epsilon.events.player;

import net.minecraft.world.entity.Entity;

/**
 * Fired during raytrace to allow rotation modification.
 */
public class RaytraceEvent {
    private float yaw;
    private float pitch;
    private final Entity entity;

    public RaytraceEvent(Entity entity, float yaw, float pitch) {
        this.entity = entity;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Entity getEntity() { return entity; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public void setPitch(float pitch) { this.pitch = pitch; }
}


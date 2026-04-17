package com.github.epsilon.events.world;

import net.minecraft.world.entity.Entity;

/**
 * Fired when an entity joins the client level. Replaces NeoForge's EntityJoinLevelEvent.
 */
public class EntityJoinWorldEvent {

    private final Entity entity;

    public EntityJoinWorldEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}


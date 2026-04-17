package com.github.epsilon.events.render;

import net.minecraft.client.DeltaTracker;

/**
 * Fired each frame. Replaces NeoForge's RenderFrameEvent.
 */
public class RenderFrameEvent {

    public static class Pre extends RenderFrameEvent {
        private final DeltaTracker partialTick;

        public Pre(DeltaTracker partialTick) {
            this.partialTick = partialTick;
        }

        public DeltaTracker getPartialTick() {
            return partialTick;
        }
    }

    public static class Post extends RenderFrameEvent {
    }
}

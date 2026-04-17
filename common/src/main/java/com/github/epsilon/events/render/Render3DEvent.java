package com.github.epsilon.events.render;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Fired after the 3D world is rendered. Replaces NeoForge's RenderLevelStageEvent.AfterLevel.
 */
public class Render3DEvent {

    private final PoseStack poseStack;

    public Render3DEvent(PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }
}

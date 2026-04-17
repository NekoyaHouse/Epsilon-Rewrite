package com.github.epsilon.mixins.render;

import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.render.RenderFrameEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderPre(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        EpsilonEventBus.INSTANCE.post(new RenderFrameEvent.Pre(deltaTracker));
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderPost(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        EpsilonEventBus.INSTANCE.post(new RenderFrameEvent.Post());
    }
}

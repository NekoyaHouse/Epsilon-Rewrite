package com.github.epsilon;

import com.github.epsilon.assets.i18n.LanguageReloadListener;
import com.github.epsilon.assets.resources.ResourceLocationUtils;
import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.input.KeyInputEvent;
import com.github.epsilon.events.render.Render2DEvent;
import com.github.epsilon.events.render.RenderFrameEvent;
import com.github.epsilon.graphics.LuminRenderPipelines;
import com.github.epsilon.managers.ModuleManager;
import com.github.epsilon.managers.RenderManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Epsilon.MODID, value = Dist.CLIENT)
public class EventHandler {

    @SubscribeEvent
    private static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
        LuminRenderPipelines.onRegisterRenderPipelines(event);
    }

    @SubscribeEvent
    public static void onResourcesReload(AddClientReloadListenersEvent event) {
        event.addListener(ResourceLocationUtils.getIdentifier("objects/reload_listener"), new LanguageReloadListener());
    }

    public static void registerCustomListeners() {
        EpsilonEventBus.INSTANCE.subscribe(CustomListeners.INSTANCE);
    }

    public static class CustomListeners {

        public static final CustomListeners INSTANCE = new CustomListeners();

        private CustomListeners() {}

        @com.github.epsilon.events.bus.EventHandler
        public void onKeyPress(KeyInputEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.screen != null || event.getKey() == GLFW.GLFW_KEY_UNKNOWN) return;
            ModuleManager.INSTANCE.onKeyEvent(event.getKey(), event.getAction());
        }

        @com.github.epsilon.events.bus.EventHandler
        public void onRenderFramePost(RenderFrameEvent.Post event) {
            RenderSystem.backupProjectionMatrix();
            RenderManager.INSTANCE.callAfterFrame(Minecraft.getInstance().getDeltaTracker());
            RenderSystem.restoreProjectionMatrix();
            RenderManager.INSTANCE.clear();
        }

        @com.github.epsilon.events.bus.EventHandler
        public void onRenderInGameGuiPre(Render2DEvent.BeforeInGameGui event) {
            RenderSystem.backupProjectionMatrix();
            RenderManager.INSTANCE.callInGameGui(Minecraft.getInstance().getDeltaTracker());
            RenderSystem.restoreProjectionMatrix();
        }
    }

}

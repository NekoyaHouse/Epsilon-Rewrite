package com.github.epsilon.gui.panel.popup;

import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.StaticFontLoader;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.dsl.PanelUiCompiler;
import com.github.epsilon.gui.panel.dsl.PanelUiTree;
import com.github.epsilon.managers.RenderManager;
import com.github.epsilon.modules.Module;
import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class KeyBindPopup implements PanelPopupHost.Popup {

    private final PanelLayout.Rect bounds;
    private final Module module;
    private final RoundRectRenderer roundRectRenderer = new RoundRectRenderer();
    private final ShadowRenderer shadowRenderer = new ShadowRenderer();
    private final TextRenderer textRenderer = new TextRenderer();
    private final Animation openAnimation = new Animation(Easing.EASE_OUT_CUBIC, 140L);
    private boolean closeAfterClick;

    public KeyBindPopup(PanelLayout.Rect bounds, Module module) {
        this.bounds = bounds;
        this.module = module;
        this.openAnimation.setStartValue(0.0f);
    }

    public Module getModule() {
        return module;
    }

    @Override
    public PanelLayout.Rect getBounds() {
        return bounds;
    }

    @Override
    public void extractGui(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        PanelUiTree tree = PanelUiTree.build(scope -> {
            float progress = scope.animate(openAnimation, 1.0f);
            float popupY = bounds.y() - (1.0f - progress) * 6.0f;
            scope.shadow(bounds.x(), popupY, bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS, POPUP_SHADOW_RADIUS,
                    MD3Theme.withAlpha(MD3Theme.SHADOW, (int) (MD3Theme.POPUP_SHADOW_ALPHA * progress)));
            scope.roundRect(bounds.x(), popupY, bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS, MD3Theme.SURFACE_CONTAINER_LOW);
            scope.text(module.getTranslatedName(), bounds.x() + 12.0f, popupY + 10.0f, 0.66f, MD3Theme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);
            scope.text("Press a key to bind", bounds.x() + 12.0f, popupY + 24.0f, 0.56f, MD3Theme.TEXT_SECONDARY);
            scope.text("ESC to cancel · Backspace to clear", bounds.x() + 12.0f, popupY + 37.0f, 0.52f, MD3Theme.TEXT_MUTED);
        });
        PanelUiCompiler.render(tree, shadowRenderer, roundRectRenderer, null, textRenderer);
        RenderManager.INSTANCE.applyRender(() -> {
            shadowRenderer.drawAndClear();
            roundRectRenderer.drawAndClear();
            textRenderer.drawAndClear();
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        closeAfterClick = !bounds.contains(event.x(), event.y());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            return true;
        }
        if (event.key() == 259 || event.key() == 261) {
            module.setKeyBind(-1);
            closeAfterClick = true;
            return true;
        }
        module.setKeyBind(event.key());
        closeAfterClick = true;
        return true;
    }

    @Override
    public boolean shouldCloseAfterClick() {
        return closeAfterClick;
    }
}

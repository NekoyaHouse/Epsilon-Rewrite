package com.github.epsilon.gui.panel.component;

import com.github.epsilon.assets.i18n.EpsilonTranslateComponent;
import com.github.epsilon.assets.i18n.TranslateComponent;
import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.StaticFontLoader;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.adapter.ModuleViewModel;
import com.github.epsilon.gui.panel.dsl.PanelUiCompiler;
import com.github.epsilon.gui.panel.dsl.PanelUiTree;
import com.mojang.blaze3d.platform.InputConstants;

import java.awt.*;

public class ModuleRow {

    public static final float HEIGHT = 34.0f;

    private final ModuleViewModel module;
    private final PanelLayout.Rect bounds;
    private final PanelLayout.Rect toggleBounds;

    private static final TranslateComponent noneComponent = EpsilonTranslateComponent.create("keybind", "none");

    public ModuleRow(ModuleViewModel module, PanelLayout.Rect bounds) {
        this.module = module;
        this.bounds = bounds;
        this.toggleBounds = PanelElements.switchBounds(bounds);
    }

    public ModuleViewModel getModule() {
        return module;
    }

    public PanelLayout.Rect getBounds() {
        return bounds;
    }

    public PanelLayout.Rect getToggleBounds() {
        return toggleBounds;
    }

    public void render(RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, float hoverProgress, float selectedProgress, float toggleProgress, float toggleHoverProgress) {
        PanelUiTree tree = PanelUiTree.build(scope -> buildUi(scope, textRenderer, hoverProgress, selectedProgress, toggleProgress, toggleHoverProgress));
        PanelUiCompiler.render(tree, roundRectRenderer, rectRenderer, textRenderer);
    }

    public void buildUi(PanelUiTree.Scope scope, TextRenderer textRenderer, float hoverProgress, float selectedProgress, float toggleProgress, float toggleHoverProgress) {
        float titleScale = 0.70f;
        float subScale = 0.60f;
        float keyScale = 0.6f;
        float titleHeight = textRenderer.getHeight(titleScale, StaticFontLoader.DUCKSANS);
        float subHeight = textRenderer.getHeight(subScale);
        float lineGap = 3.0f;
        float totalTextHeight = titleHeight + lineGap + subHeight;
        float titleY = bounds.y() + (bounds.height() - totalTextHeight) / 2.0f - 1.0f;
        float subY = titleY + titleHeight + lineGap - 1.0f;
        float keyY = bounds.y() + (bounds.height() - textRenderer.getHeight(keyScale)) / 2.0f - 1.0f;
        Color titleColor = MD3Theme.lerp(MD3Theme.TEXT_PRIMARY, MD3Theme.ON_PRIMARY_CONTAINER, selectedProgress);
        Color subColor = MD3Theme.lerp(MD3Theme.TEXT_SECONDARY, MD3Theme.withAlpha(MD3Theme.ON_PRIMARY_CONTAINER, 180), selectedProgress);
        Color keyColor = MD3Theme.isLightTheme() ? MD3Theme.TEXT_SECONDARY : MD3Theme.TEXT_MUTED;
        String keybindText = formatKeybind(module.module().getKeyBind());
        float keyWidth = textRenderer.getWidth(keybindText, keyScale);
        float keyX = toggleBounds.x() - 8.0f - keyWidth;

        scope.roundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS, MD3Theme.rowSurface(hoverProgress));
        if (selectedProgress > 0.01f) {
            scope.roundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS,
                    MD3Theme.stateLayer(MD3Theme.PRIMARY, selectedProgress, 42));
        }

        scope.text(module.displayName(), PanelElements.rowLabelX(bounds), titleY, titleScale, titleColor, StaticFontLoader.DUCKSANS);
        String addonText = module.module().getAddonId() != null ? module.module().getAddonId() : "unknown";
        scope.text(addonText, PanelElements.rowLabelX(bounds), subY, subScale, subColor);
        scope.toggle(toggleBounds, toggleProgress, toggleHoverProgress);
        scope.text(keybindText, keyX, keyY, keyScale, keyColor);
    }



    private String formatKeybind(int keyCode) {
        if (keyCode < 0) {
            return noneComponent.getTranslatedName();
        }
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString().toUpperCase();
    }

}

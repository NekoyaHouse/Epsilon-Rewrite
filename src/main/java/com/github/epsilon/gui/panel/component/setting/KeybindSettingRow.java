package com.github.epsilon.gui.panel.component.setting;

import com.github.epsilon.assets.i18n.TranslateComponent;
import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.component.SettingRow;
import com.github.epsilon.settings.impl.KeybindSetting;
import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;
import java.util.Locale;

public class KeybindSettingRow extends SettingRow<KeybindSetting> {

    private static final TranslateComponent noneComponent = TranslateComponent.create("keybind", "none");

    private final Animation chipHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation focusAnimation = new Animation(Easing.EASE_OUT_CUBIC, 150L);

    private boolean listening;

    public KeybindSettingRow(KeybindSetting setting) {
        super(setting);
        chipHoverAnimation.setStartValue(0.0f);
        focusAnimation.setStartValue(0.0f);
    }

    public boolean isListening() {
        return listening;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    @Override
    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, PanelLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        float labelScale = 0.68f;
        float labelY = bounds.y() + (bounds.height() - textRenderer.getHeight(labelScale)) / 2.0f - 1.0f;

        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS, MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER, MD3Theme.SURFACE_CONTAINER_HIGH, hoverProgress));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + MD3Theme.ROW_CONTENT_INSET, labelY, labelScale, MD3Theme.TEXT_PRIMARY);

        PanelLayout.Rect chipBounds = getChipBounds(bounds);
        chipHoverAnimation.run(chipBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);
        focusAnimation.run(listening ? 1.0f : 0.0f);
        float chipHover = chipHoverAnimation.getValue();
        float focusProgress = focusAnimation.getValue();

        float radius = 8.0f;

        if (focusProgress > 0.01f) {
            float haloInset = 1.5f * focusProgress;
            roundRectRenderer.addRoundRect(chipBounds.x() - haloInset, chipBounds.y() - haloInset, chipBounds.width() + haloInset * 2.0f, chipBounds.height() + haloInset * 2.0f, radius + haloInset, MD3Theme.withAlpha(MD3Theme.PRIMARY, (int) (28 * focusProgress)));
        }

        Color background = MD3Theme.lerp(MD3Theme.SECONDARY_CONTAINER, MD3Theme.PRIMARY_CONTAINER, focusProgress);
        Color foreground = MD3Theme.lerp(MD3Theme.ON_SECONDARY_CONTAINER, MD3Theme.ON_PRIMARY_CONTAINER, focusProgress);
        roundRectRenderer.addRoundRect(chipBounds.x(), chipBounds.y(), chipBounds.width(), chipBounds.height(), radius, background);

        if (chipHover > 0.01f) {
            int hoverAlpha = listening ? 18 : 12;
            roundRectRenderer.addRoundRect(chipBounds.x(), chipBounds.y(), chipBounds.width(), chipBounds.height(), radius, MD3Theme.withAlpha(foreground, (int) (hoverAlpha * chipHover)));
        }

        String label = listening ? "..." : formatCompactKeybind(setting.getValue());
        float chipTextScale = 0.52f;
        float textWidth = textRenderer.getWidth(label, chipTextScale);
        float textHeight = textRenderer.getHeight(chipTextScale);
        float textX = chipBounds.x() + (chipBounds.width() - textWidth) / 2.0f;
        float textY = chipBounds.y() + (chipBounds.height() - textHeight) / 2.0f - 1.0f;
        textRenderer.addText(label, textX, textY, chipTextScale, foreground);
    }

    public PanelLayout.Rect getChipBounds(PanelLayout.Rect bounds) {
        float chipWidth = 56.0f;
        float chipHeight = 18.0f;
        float chipX = bounds.right() - MD3Theme.ROW_TRAILING_INSET - chipWidth;
        float chipY = bounds.y() + (bounds.height() - chipHeight) / 2.0f;
        return new PanelLayout.Rect(chipX, chipY, chipWidth, chipHeight);
    }

    @Override
    public boolean mouseClicked(PanelLayout.Rect bounds, MouseButtonEvent event, boolean isDoubleClick) {
        if (!bounds.contains(event.x(), event.y()) || event.button() != 0) {
            return false;
        }
        PanelLayout.Rect chipBounds = getChipBounds(bounds);
        return chipBounds.contains(event.x(), event.y());
    }

    @Override
    public boolean hasActiveAnimation() {
        return !chipHoverAnimation.isFinished() || !focusAnimation.isFinished();
    }

    private String formatCompactKeybind(int keyCode) {
        if (keyCode < 0) {
            return noneComponent.getTranslatedName();
        }
        String label = formatKeybind(keyCode).trim();
        if (label.isEmpty()) {
            return "?";
        }

        String[] parts = label.split("[^A-Za-z0-9]+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && Character.isLetterOrDigit(part.charAt(0))) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 3) {
                break;
            }
        }
        if (initials.length() >= 2) {
            return initials.toString();
        }

        String compact = label.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (compact.isEmpty()) {
            return "?";
        }
        return compact.length() > 3 ? compact.substring(0, 3) : compact;
    }

    private String formatKeybind(int keyCode) {
        if (keyCode < 0) {
            return "None";
        }
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
    }
}


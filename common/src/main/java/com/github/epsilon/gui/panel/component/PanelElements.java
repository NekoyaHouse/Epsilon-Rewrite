package com.github.epsilon.gui.panel.component;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.ttf.TtfFontLoader;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import org.jspecify.annotations.Nullable;

import java.awt.*;

public final class PanelElements {

    public static final float ROW_LABEL_INSET = MD3Theme.ROW_CONTENT_INSET + 4.0f;
    public static final float ICON_BUTTON_SIZE = 20.0f;

    private PanelElements() {
    }

    public static void drawRowSurface(RoundRectRenderer roundRectRenderer, PanelLayout.Rect bounds, float hoverProgress) {
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS, MD3Theme.rowSurface(hoverProgress));
    }

    public static float rowLabelX(PanelLayout.Rect bounds) {
        return bounds.x() + ROW_LABEL_INSET;
    }

    public static PanelLayout.Rect alignTrailing(PanelLayout.Rect bounds, float width, float height) {
        return new PanelLayout.Rect(bounds.right() - MD3Theme.ROW_TRAILING_INSET - width, bounds.y() + (bounds.height() - height) / 2.0f, width, height);
    }

    public static PanelLayout.Rect switchBounds(PanelLayout.Rect bounds) {
        return alignTrailing(bounds, MD3Theme.SWITCH_WIDTH, MD3Theme.SWITCH_HEIGHT);
    }

    public static PanelLayout.Rect compactFieldBounds(PanelLayout.Rect bounds, float width) {
        return alignTrailing(bounds, width, MD3Theme.CONTROL_HEIGHT);
    }

    public static void drawSwitch(RoundRectRenderer roundRectRenderer, PanelLayout.Rect rect, float toggleProgress, float hoverProgress) {
        Color track = MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER_HIGHEST, MD3Theme.PRIMARY, toggleProgress);
        Color knob = MD3Theme.lerp(MD3Theme.OUTLINE, MD3Theme.ON_PRIMARY, toggleProgress);

        float knobSize = 8.0f + 3.0f * toggleProgress;
        float knobTravel = rect.width() - 10.0f - knobSize;
        float knobX = rect.x() + 5.0f + knobTravel * toggleProgress;
        float knobY = rect.centerY() - knobSize / 2.0f;

        roundRectRenderer.addRoundRect(rect.x(), rect.y(), rect.width(), rect.height(), rect.height() / 2.0f, track);
        if (hoverProgress > 0.02f) {
            float haloSize = 16.0f;
            float haloX = knobX + knobSize / 2.0f - haloSize / 2.0f;
            float haloY = rect.centerY() - haloSize / 2.0f;
            roundRectRenderer.addRoundRect(haloX, haloY, haloSize, haloSize, haloSize / 2.0f,
                    MD3Theme.stateLayer(MD3Theme.TEXT_PRIMARY, hoverProgress, 18));
        }
        roundRectRenderer.addRoundRect(knobX, knobY, knobSize, knobSize, knobSize / 2.0f, knob);
    }

    public static FilledFieldColors drawFilledField(RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, PanelLayout.Rect bounds,
                                                    boolean focused, float hoverProgress) {
        Color container = MD3Theme.filledFieldSurface(focused, hoverProgress);
        Color text = MD3Theme.filledFieldContent(focused);
        Color caret = MD3Theme.filledFieldCaret(focused);
        Color indicator = MD3Theme.filledFieldIndicator(focused, hoverProgress);

        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.CONTROL_RADIUS, container);
        float indicatorHeight = focused ? 1.5f : 1.0f;
        float indicatorInset = 4.0f;
        rectRenderer.addRect(bounds.x() + indicatorInset, bounds.bottom() - indicatorHeight,
                Math.max(0.0f, bounds.width() - indicatorInset * 2.0f), indicatorHeight, indicator);
        return new FilledFieldColors(text, caret, indicator);
    }

    public static PanelLayout.Rect measureAssistChipBounds(TextRenderer textRenderer, PanelLayout.Rect rowBounds, String label,
                                                           float textScale, float horizontalPadding, float trailingSlotWidth, float maxWidth) {
        float desiredWidth = textRenderer.getWidth(label, textScale) + horizontalPadding * 2.0f + trailingSlotWidth;
        return alignTrailing(rowBounds, Math.min(maxWidth, desiredWidth), MD3Theme.COMPACT_CHIP_HEIGHT);
    }

    public static void drawAssistChip(RoundRectRenderer roundRectRenderer, TextRenderer textRenderer, PanelLayout.Rect bounds,
                                      String label, float textScale, Color background, Color foreground,
                                      @Nullable String trailingIcon, float trailingIconScale, @Nullable TtfFontLoader trailingIconFont) {
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.CONTROL_RADIUS, background);
        float textY = bounds.y() + (bounds.height() - textRenderer.getHeight(textScale)) / 2.0f - 1.0f;
        textRenderer.addText(label, bounds.x() + 8.0f, textY, textScale, foreground);
        if (trailingIcon == null || trailingIcon.isEmpty() || trailingIconFont == null) {
            return;
        }
        float iconWidth = textRenderer.getWidth(trailingIcon, trailingIconScale, trailingIconFont);
        float iconY = bounds.y() + (bounds.height() - textRenderer.getHeight(trailingIconScale, trailingIconFont)) / 2.0f - 1.0f;
        textRenderer.addText(trailingIcon, bounds.right() - 8.0f - iconWidth, iconY, trailingIconScale, foreground, trailingIconFont);
    }

    public static void drawSegmentedControl(RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer,
                                            PanelLayout.Rect bounds, String leadingLabel, String trailingLabel,
                                            float progress, float hoverProgress) {
        float outerRadius = MD3Theme.CONTROL_RADIUS;
        float shellInset = 1.0f;
        float innerX = bounds.x() + shellInset;
        float innerY = bounds.y() + shellInset;
        float innerWidth = bounds.width() - shellInset * 2.0f;
        float innerHeight = bounds.height() - shellInset * 2.0f;
        float segmentWidth = innerWidth / 2.0f;
        float indicatorInset = 1.5f;
        float indicatorWidth = segmentWidth - indicatorInset * 2.0f;
        float indicatorX = innerX + indicatorInset + segmentWidth * progress;
        float indicatorY = innerY + indicatorInset;
        float indicatorHeight = innerHeight - indicatorInset * 2.0f;
        float indicatorRadius = Math.max(4.0f, outerRadius - 2.0f);
        float labelScale = 0.52f;
        float labelY = innerY + (innerHeight - textRenderer.getHeight(labelScale)) / 2.0f - 1.0f;
        Color inactiveLabel = MD3Theme.segmentedControlInactiveLabel();
        Color activeLabel = MD3Theme.segmentedControlActiveLabel();

        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), outerRadius, MD3Theme.OUTLINE_SOFT);
        roundRectRenderer.addRoundRect(innerX, innerY, innerWidth, innerHeight, Math.max(outerRadius - shellInset, 1.0f), MD3Theme.segmentedControlSurface());
        if (hoverProgress > 0.01f) {
            roundRectRenderer.addRoundRect(innerX, innerY, innerWidth, innerHeight, Math.max(outerRadius - shellInset, 1.0f),
                    MD3Theme.stateLayer(MD3Theme.TEXT_PRIMARY, hoverProgress, MD3Theme.isLightTheme() ? 10 : 14));
        }
        rectRenderer.addRect(innerX + segmentWidth - 0.5f, innerY + 3.0f, 1.0f, innerHeight - 6.0f, MD3Theme.OUTLINE_SOFT);
        roundRectRenderer.addRoundRect(indicatorX, indicatorY, indicatorWidth, indicatorHeight, indicatorRadius, MD3Theme.segmentedControlIndicator());

        float leadingWidth = textRenderer.getWidth(leadingLabel, labelScale);
        float trailingWidth = textRenderer.getWidth(trailingLabel, labelScale);
        textRenderer.addText(leadingLabel, innerX + (segmentWidth - leadingWidth) / 2.0f, labelY, labelScale, MD3Theme.lerp(activeLabel, inactiveLabel, progress));
        textRenderer.addText(trailingLabel, innerX + segmentWidth + (segmentWidth - trailingWidth) / 2.0f, labelY, labelScale, MD3Theme.lerp(inactiveLabel, activeLabel, progress));
    }

    public static void drawIconButton(RoundRectRenderer roundRectRenderer, TextRenderer textRenderer, PanelLayout.Rect bounds,
                                      String label, float scale, Color tone, float hoverProgress) {
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), bounds.height() / 2.0f,
                MD3Theme.stateLayer(tone, hoverProgress, 32));
        Color labelColor = MD3Theme.lerp(MD3Theme.TEXT_MUTED, tone, hoverProgress);
        float textWidth = textRenderer.getWidth(label, scale);
        float textHeight = textRenderer.getHeight(scale);
        textRenderer.addText(label,
                bounds.x() + (bounds.width() - textWidth) / 2.0f,
                bounds.y() + (bounds.height() - textHeight) / 2.0f - 1.0f,
                scale,
                labelColor);
    }

    public record FilledFieldColors(Color text, Color caret, Color indicator) {
    }
}


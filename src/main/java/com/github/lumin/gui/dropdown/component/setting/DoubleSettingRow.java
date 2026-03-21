package com.github.lumin.gui.dropdown.component.setting;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.component.SettingRow;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

public class DoubleSettingRow extends SettingRow<DoubleSetting> {

    private final Animation hoverAnimation = new Animation(Easing.EASE_OUT_QUART, 150L);
    private final Animation pressAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation indicatorAnimation = new Animation(Easing.EASE_OUT_QUART, 150L);
    private boolean dragging;

    public DoubleSettingRow(DoubleSetting setting) {
        super(setting);
        hoverAnimation.setStartValue(0.0f);
        pressAnimation.setStartValue(0.0f);
        indicatorAnimation.setStartValue(0.0f);
    }

    @Override
    public void render(GuiGraphics guiGraphics, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, DropdownLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        hoverAnimation.run(dragging ? 1.0f : hoverProgress);
        pressAnimation.run(dragging ? 1.0f : 0.0f);
        indicatorAnimation.run((dragging || hoverProgress > 0.01f) ? 1.0f : 0.0f);

        float animatedHover = hoverAnimation.getValue();
        float animatedPress = pressAnimation.getValue();
        float indicatorProgress = indicatorAnimation.getValue();

        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER, DropdownTheme.SURFACE_CONTAINER_HIGH, animatedHover));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET, bounds.y() + 7.0f, 0.68f, DropdownTheme.TEXT_PRIMARY);

        DropdownLayout.Rect trackBounds = getTrackBounds(bounds);
        float progress = getProgress();
        float handleWidth = 4.0f - animatedPress * 2.0f;
        float handleHeight = 14.0f;
        float handleX = trackBounds.x() + trackBounds.width() * progress - handleWidth / 2.0f;
        float handleY = trackBounds.centerY() - handleHeight / 2.0f;
        float handleGap = 4.0f;
        float activeWidth = Math.max(2.0f, trackBounds.width() * progress - handleWidth / 2.0f - handleGap);
        float inactiveX = handleX + handleWidth + handleGap;
        float inactiveWidth = Math.max(2.0f, trackBounds.right() - inactiveX);

        if (shouldDrawSteps()) {
            drawSteps(rectRenderer, trackBounds, progress);
        }

        roundRectRenderer.addRoundRect(trackBounds.x(), trackBounds.y(), activeWidth, trackBounds.height(), 3.0f, 0.0f, 0.0f, 3.0f, DropdownTheme.PRIMARY);
        roundRectRenderer.addRoundRect(inactiveX, trackBounds.y(), inactiveWidth, trackBounds.height(), 0.0f, 3.0f, 3.0f, 0.0f, DropdownTheme.SECONDARY_CONTAINER);
        roundRectRenderer.addRoundRect(handleX, handleY, handleWidth, handleHeight, 2.0f, DropdownTheme.PRIMARY);

        if (indicatorProgress > 0.01f) {
            String label = formatValue();
            float textScale = 0.62f;
            float bubbleWidth = textRenderer.getWidth(label, textScale) + 16.0f;
            float bubbleHeight = 18.0f;
            float bubbleX = handleX + handleWidth / 2.0f - bubbleWidth / 2.0f;
            float bubbleY = bounds.y() - 22.0f;
            int bubbleAlpha = (int) (255 * indicatorProgress);
            roundRectRenderer.addRoundRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, 9.0f, DropdownTheme.withAlpha(DropdownTheme.INVERSE_SURFACE, bubbleAlpha));
            float textWidth = textRenderer.getWidth(label, textScale);
            float textHeight = textRenderer.getHeight(textScale);
            float textX = bubbleX + (bubbleWidth - textWidth) / 2.0f;
            float textY = bubbleY + (bubbleHeight - textHeight) / 2.0f - 1.0f;
            textRenderer.addText(label, textX, textY, textScale, DropdownTheme.withAlpha(DropdownTheme.INVERSE_ON_SURFACE, bubbleAlpha));
        }
    }

    public DropdownLayout.Rect getTrackBounds(DropdownLayout.Rect bounds) {
        return new DropdownLayout.Rect(bounds.right() - DropdownTheme.ROW_TRAILING_INSET - 128.0f, bounds.y() + 12.0f, 100.0f, 6.0f);
    }

    @Override
    public boolean mouseClicked(DropdownLayout.Rect bounds, MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() != 0 || !getInteractiveBounds(bounds).contains(event.x(), event.y())) {
            return false;
        }
        dragging = true;
        updateFromMouse(bounds, event.x());
        return true;
    }

    @Override
    public boolean mouseReleased(DropdownLayout.Rect bounds, MouseButtonEvent event) {
        dragging = false;
        return false;
    }

    public void updateFromMouse(DropdownLayout.Rect bounds, double mouseX) {
        DropdownLayout.Rect trackBounds = getTrackBounds(bounds);
        double progress = (mouseX - trackBounds.x()) / trackBounds.width();
        progress = Math.max(0.0, Math.min(1.0, progress));
        double rawValue = setting.getMin() + (setting.getMax() - setting.getMin()) * progress;
        double step = setting.getStep() <= 0.0 ? 0.01 : setting.getStep();
        double snapped = setting.getMin() + Math.round((rawValue - setting.getMin()) / step) * step;
        setting.setValue(snapped);
    }

    public boolean isDragging() {
        return dragging;
    }

    private DropdownLayout.Rect getInteractiveBounds(DropdownLayout.Rect bounds) {
        DropdownLayout.Rect track = getTrackBounds(bounds);
        return new DropdownLayout.Rect(track.x(), track.y() - 6.0f, track.width(), track.height() + 12.0f);
    }

    private float getProgress() {
        if (setting.getMax() <= setting.getMin()) {
            return 0.0f;
        }
        return (float) ((setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin()));
    }

    private boolean shouldDrawSteps() {
        double step = setting.getStep() <= 0.0 ? 0.0 : setting.getStep();
        double range = setting.getMax() - setting.getMin();
        return range > 0.0 && step / range > 0.08;
    }

    private void drawSteps(RectRenderer rectRenderer, DropdownLayout.Rect trackBounds, float progress) {
        double step = setting.getStep() <= 0.0 ? 0.0 : setting.getStep();
        double range = setting.getMax() - setting.getMin();
        int steps = Math.max(1, (int) Math.floor(range / step));
        for (int i = 0; i <= steps; i++) {
            float stepProgress = i / (float) steps;
            if (Math.abs(stepProgress - progress) < (1.0f / steps) * 0.5f) {
                continue;
            }
            float x = trackBounds.x() + trackBounds.width() * stepProgress;
            rectRenderer.addRect(x, trackBounds.centerY() - 1.0f, 2.0f, 2.0f, stepProgress <= progress ? DropdownTheme.ON_PRIMARY : DropdownTheme.ON_SECONDARY_CONTAINER);
        }
    }

    private String formatValue() {
        String label = Math.abs(setting.getValue() - Math.round(setting.getValue())) < 0.0001 ? Integer.toString((int) Math.round(setting.getValue())) : String.format("%.2f", setting.getValue());
        if (setting.isPercentageMode()) {
            label += "%";
        }
        return label;
    }

}

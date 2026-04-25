package com.github.epsilon.gui.panel.component.setting;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.StaticFontLoader;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.component.PanelElements;
import com.github.epsilon.gui.panel.component.SettingRow;
import com.github.epsilon.settings.impl.EnumSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public class EnumSettingRow extends SettingRow<EnumSetting<?>> {

    private static final String DROPDOWN_ICON = "v";

    public EnumSettingRow(EnumSetting<?> setting) {
        super(setting);
    }

    @Override
    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, PanelLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        float labelScale = 0.68f;
        float labelY = bounds.y() + (bounds.height() - textRenderer.getHeight(labelScale)) / 2.0f - 1.0f;
        float chipTextScale = 0.60f;
        PanelElements.drawRowSurface(roundRectRenderer, bounds, hoverProgress);
        textRenderer.addText(setting.getDisplayName(), bounds.x() + MD3Theme.ROW_CONTENT_INSET, labelY, labelScale, MD3Theme.TEXT_PRIMARY);
        PanelLayout.Rect chipBounds = getChipBounds(textRenderer, bounds);
        PanelElements.drawAssistChip(roundRectRenderer, textRenderer, chipBounds,
                setting.getTranslatedValue(), chipTextScale,
                MD3Theme.SECONDARY_CONTAINER, MD3Theme.ON_SECONDARY_CONTAINER,
                DROPDOWN_ICON, 0.58f, StaticFontLoader.ICONS);
    }

    public PanelLayout.Rect getChipBounds(TextRenderer textRenderer, PanelLayout.Rect bounds) {
        return PanelElements.measureAssistChipBounds(textRenderer, bounds, setting.getTranslatedValue(), 0.60f, 8.0f, 18.0f, 96.0f);
    }

    @Override
    public boolean mouseClicked(PanelLayout.Rect bounds, MouseButtonEvent event, boolean isDoubleClick) {
        return bounds.contains(event.x(), event.y()) && event.button() == 0;
    }

}

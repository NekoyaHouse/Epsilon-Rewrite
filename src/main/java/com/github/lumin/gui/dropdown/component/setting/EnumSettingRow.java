package com.github.lumin.gui.dropdown.component.setting;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.component.SettingRow;
import com.github.lumin.settings.impl.EnumSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

public class EnumSettingRow extends SettingRow<EnumSetting<?>> {

    public EnumSettingRow(EnumSetting<?> setting) {
        super(setting);
    }

    @Override
    public void render(GuiGraphics guiGraphics, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, DropdownLayout.Rect bounds, boolean hovered, int mouseX, int mouseY, float partialTick) {
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, hovered ? DropdownTheme.SURFACE_CONTAINER_HIGH : DropdownTheme.SURFACE_CONTAINER);
        textRenderer.addText(setting.getDisplayName(), bounds.x() + 10.0f, bounds.y() + 8.0f, 0.74f, DropdownTheme.TEXT_PRIMARY);
        String value = setting.getTranslatedValue();
        float chipWidth = Math.min(100.0f, textRenderer.getWidth(value, 0.66f) + 18.0f);
        roundRectRenderer.addRoundRect(bounds.right() - chipWidth - 10.0f, bounds.y() + 6.0f, chipWidth, 18.0f, DropdownTheme.CARD_RADIUS, DropdownTheme.SECONDARY_CONTAINER);
        textRenderer.addText(value, bounds.right() - chipWidth + 2.0f, bounds.y() + 10.0f, 0.66f, DropdownTheme.ON_SECONDARY_CONTAINER);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean mouseClicked(DropdownLayout.Rect bounds, MouseButtonEvent event, boolean isDoubleClick) {
        if (!bounds.contains(event.x(), event.y()) || event.button() != 0) {
            return false;
        }
        Enum[] modes = (Enum[]) setting.getModes();
        if (modes.length == 0) {
            return false;
        }
        int next = (setting.getModeIndex() + 1) % modes.length;
        ((EnumSetting) setting).setMode(modes[next]);
        return true;
    }
}

package com.github.lumin.gui.dropdown.component.setting;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.component.SettingRow;
import com.github.lumin.settings.impl.ColorSetting;
import net.minecraft.client.gui.GuiGraphics;

public class ColorSettingRow extends SettingRow<ColorSetting> {

    public ColorSettingRow(ColorSetting setting) {
        super(setting);
    }

    @Override
    public void render(GuiGraphics guiGraphics, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, DropdownLayout.Rect bounds, boolean hovered, int mouseX, int mouseY, float partialTick) {
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, hovered ? DropdownTheme.SURFACE_CONTAINER_HIGH : DropdownTheme.SURFACE_CONTAINER);
        textRenderer.addText(setting.getDisplayName(), bounds.x() + 10.0f, bounds.y() + 8.0f, 0.74f, DropdownTheme.TEXT_PRIMARY);
        roundRectRenderer.addRoundRect(bounds.right() - 30.0f, bounds.y() + 8.0f, 14.0f, 14.0f, 6.0f, setting.getValue());
        rectRenderer.addRect(bounds.right() - 30.0f, bounds.y() + 23.0f, 14.0f, 1.0f, DropdownTheme.OUTLINE_SOFT);
    }
}

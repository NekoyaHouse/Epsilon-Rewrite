package com.github.lumin.gui.dropdown.panel;

import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownState;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.modules.Category;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;

public class CategoryRailPanel {

    protected final DropdownState state;
    private final RoundRectRenderer roundRectRenderer;
    private final TextRenderer textRenderer;
    private DropdownLayout.Rect bounds;

    public CategoryRailPanel(DropdownState state, RoundRectRenderer roundRectRenderer, TextRenderer textRenderer) {
        this.state = state;
        this.roundRectRenderer = roundRectRenderer;
        this.textRenderer = textRenderer;
    }

    public void render(GuiGraphics guiGraphics, DropdownLayout.Rect bounds, int mouseX, int mouseY, float partialTick) {
        this.bounds = bounds;

        textRenderer.addText("Lu", bounds.x() + 10.0f, bounds.y() + 10.0f, 0.88f, DropdownTheme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);
        textRenderer.addText("M3", bounds.x() + 10.0f, bounds.y() + 23.0f, 0.62f, DropdownTheme.TEXT_SECONDARY);

        float itemY = bounds.y() + 42.0f;
        for (Category category : Category.values()) {
            DropdownLayout.Rect itemRect = new DropdownLayout.Rect(bounds.x() + 6.0f, itemY, bounds.width() - 12.0f, 36.0f);
            boolean hovered = itemRect.contains(mouseX, mouseY);
            boolean selected = state.getSelectedCategory() == category;

            Color background = selected ? DropdownTheme.SECONDARY_CONTAINER : hovered ? DropdownTheme.SURFACE_CONTAINER : DropdownTheme.withAlpha(DropdownTheme.SURFACE_CONTAINER, 0);
            Color iconColor = selected ? DropdownTheme.ON_SECONDARY_CONTAINER : hovered ? DropdownTheme.TEXT_PRIMARY : DropdownTheme.TEXT_SECONDARY;
            Color labelColor = selected ? DropdownTheme.ON_SECONDARY_CONTAINER : DropdownTheme.TEXT_SECONDARY;

            roundRectRenderer.addRoundRect(itemRect.x(), itemRect.y(), itemRect.width(), itemRect.height(), DropdownTheme.CARD_RADIUS, background);
            textRenderer.addText(category.icon, itemRect.x() + 11.0f, itemRect.y() + 6.0f, 0.86f, iconColor, StaticFontLoader.ICONS);
            textRenderer.addText(category.getName(), itemRect.x() + 10.0f, itemRect.y() + 22.0f, 0.62f, labelColor);
            itemY += 41.0f;
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (bounds == null || event.button() != 0) {
            return false;
        }
        float itemY = bounds.y() + 42.0f;
        for (Category category : Category.values()) {
            DropdownLayout.Rect itemRect = new DropdownLayout.Rect(bounds.x() + 6.0f, itemY, bounds.width() - 12.0f, 36.0f);
            if (itemRect.contains(event.x(), event.y())) {
                state.setSelectedCategory(category);
                return true;
            }
            itemY += 41.0f;
        }
        return false;
    }
}

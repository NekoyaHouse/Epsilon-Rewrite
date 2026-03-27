package com.github.epsilon.gui.dropdown.panel;

import com.github.epsilon.gui.dropdown.DropdownLayout;
import com.github.epsilon.gui.dropdown.DropdownState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public class TopBarPanel {

    protected final DropdownState state;

    public TopBarPanel(DropdownState state) {
        this.state = state;
    }

    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, DropdownLayout.Rect bounds, int mouseX, int mouseY, float partialTick) {
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return false;
    }
}

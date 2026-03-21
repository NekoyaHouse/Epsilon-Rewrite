package com.github.lumin.gui.dropdown.popup;

import com.github.lumin.gui.dropdown.DropdownLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class DropdownPopupHost {

    private Popup activePopup;

    public void open(Popup popup) {
        this.activePopup = popup;
    }

    public void close() {
        this.activePopup = null;
    }

    public Popup getActivePopup() {
        return activePopup;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (activePopup != null) {
            activePopup.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return activePopup != null && activePopup.mouseClicked(event, isDoubleClick);
    }

    public boolean keyPressed(KeyEvent event) {
        return activePopup != null && activePopup.keyPressed(event);
    }

    public interface Popup {
        DropdownLayout.Rect getBounds();

        void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

        boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick);

        default boolean keyPressed(KeyEvent event) {
            return false;
        }
    }
}

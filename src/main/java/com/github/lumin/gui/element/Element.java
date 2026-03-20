package com.github.lumin.gui.element;

import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.utils.render.MouseUtils;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;

/**
 * @author LangYa466
 * @date 2026/3/20
 */
public class Element {
    public int xPos, yPos;

    public int startX, startY;
    public boolean dragging;

    public int width, height;
    public boolean isHovering;
    public float scale = 1;

    public void onHover(int mouseX, int mouseY, TextRenderer textRenderer) {
        if (dragging) {
            xPos = mouseX - startX;
            yPos = mouseY - startY;

            alignWithOtherElements();
        }

        isHovering = MouseUtils.isHovering(xPos, yPos, width * scale, height * scale, mouseX, mouseY);

        if (isHovering || dragging) {
            textRenderer.addText("EDIT", xPos, yPos - 10 * scale, 0.8f * scale, Color.WHITE);
        }
    }

    private void alignWithOtherElements() {
        int alignThreshold = 5;

        for (Element otherElement : ElementManager.INSTANCE.elements.values()) {
            if (otherElement == this || !otherElement.dragging) continue;

            if (Math.abs(xPos - otherElement.xPos) <= alignThreshold) {
                xPos = otherElement.xPos;
            } else if (Math.abs(xPos + width - otherElement.xPos) <= alignThreshold) {
                xPos = otherElement.xPos - width;
            }

            if (Math.abs(yPos - otherElement.yPos) <= alignThreshold) {
                yPos = otherElement.yPos;
            } else if (Math.abs(yPos + height - otherElement.yPos) <= alignThreshold) {
                yPos = otherElement.yPos - height;
            }
        }
    }

    public void onClick(int mouseX, int mouseY, int button) {
        isHovering = MouseUtils.isHovering(xPos, yPos, width * scale, height * scale, mouseX, mouseY);
        ElementManager.INSTANCE.dragging = isHovering;
        if (button == 0 && isHovering) {
            dragging = true;
            startX = mouseX - xPos;
            startY = mouseY - yPos;
        }
    }

    public void onRelease(int button) {
        if (button == 0) dragging = false;
    }

    public void setScale(float scale) {
        if (scale > 5.0 || scale < 0.2) {

            if (scale > 5.0) {
                this.scale = 5.0F;
            }

            if (scale < 0.2) {
                this.scale = 0.2F;
            }

            return;
        }

        this.scale = scale;
    }

    public void preRender(GuiGraphics graphics) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(xPos, yPos);
        graphics.pose().scale(scale, scale);
    }

    public void postRender(GuiGraphics graphics) {
        graphics.pose().popMatrix();
    }
}
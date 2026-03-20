package com.github.lumin.gui.element;

import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.modules.Module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LangYa466
 * @date 2026/3/20
 */
public class ElementManager {
    public static final ElementManager INSTANCE = new ElementManager();

    public final Map<Module, Element> elements = new HashMap<>();
    public boolean dragging;

    public Element addElement(Object object) {
        if (object instanceof Module module) {
            var element = new Element();
            elements.put(module, element);
            return element;
        }
        return null;
    }

    private final TextRenderer textRenderer = new TextRenderer();

    public void render(int mouseX, int mouseY) {
        for (Element element : elements.values()) {
            element.onHover(mouseX, mouseY, textRenderer);
        }
        textRenderer.drawAndClear();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Element element : elements.values()) {
            element.onClick((int) mouseX, (int) mouseY, button);
            if (element.dragging) return true;
        }
        return false;
    }

    public void mouseReleased(int button) {
        for (Element element : elements.values()) {
            element.onRelease(button);
        }
        dragging = false;
    }

    public List<Module> getModulesWithElements() {
        return new ArrayList<>(elements.keySet());
    }
}

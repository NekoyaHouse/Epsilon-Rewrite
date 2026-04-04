package com.github.epsilon.gui.hudeditor;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.gui.panel.PanelScreen;
import com.github.epsilon.managers.ConfigManager;
import com.github.epsilon.managers.ModuleManager;
import com.github.epsilon.managers.RenderManager;
import com.github.epsilon.modules.HudModule;
import com.github.epsilon.modules.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class HudEditorScreen extends Screen {

    private static final float SNAP_RANGE = 6.0f;
    private static final Color BOX_COLOR = new Color(0, 0, 0, 100);
    private static final Color HOVER_COLOR = new Color(255, 255, 255, 70);
    private static final Color SELECTED_COLOR = new Color(120, 190, 255, 110);
    private static final Color GUIDE_COLOR = new Color(120, 190, 255, 65);
    private static final Color SNAP_GUIDE_COLOR = new Color(255, 255, 255, 90);

    public static final HudEditorScreen INSTANCE = new HudEditorScreen();

    private HudEditorScreen() {
        super(Component.literal("Epsilon-HUDEditor"));
    }

    private final RectRenderer rectRenderer = new RectRenderer();

    private HudModule dragging = null;
    private HudModule selected = null;
    private double dragOffsetX, dragOffsetY;
    private Float snapGuideX = null;
    private Float snapGuideY = null;

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        HudModule hovered = getHovered(mouseX, mouseY);

        RenderManager.INSTANCE.applyRenderAfterFrame(delta -> {
            ModuleManager.INSTANCE.getModules().forEach(module -> {
                if (module instanceof HudModule hudModule) {
                    hudModule.updateBounds(delta);
                    hudModule.refreshPosition();
                    rectRenderer.addRect(hudModule.x, hudModule.y, hudModule.width, hudModule.height, BOX_COLOR);
                    if (hudModule == hovered) {
                        rectRenderer.addRect(hudModule.x, hudModule.y, hudModule.width, hudModule.height, HOVER_COLOR);
                    }
                    if (hudModule == selected) {
                        addAnchorGuides(hudModule);
                        rectRenderer.addRect(hudModule.x, hudModule.y, hudModule.width, hudModule.height, SELECTED_COLOR);
                    }
                }
            });

            if (snapGuideX != null) {
                rectRenderer.addRect(snapGuideX, 0.0f, 1.0f, height, SNAP_GUIDE_COLOR);
            }
            if (snapGuideY != null) {
                rectRenderer.addRect(0.0f, snapGuideY, width, 1.0f, SNAP_GUIDE_COLOR);
            }

            rectRenderer.drawAndClear();

            ModuleManager.INSTANCE.getModules().forEach(module -> {
                if (module instanceof HudModule hudModule) {
                    hudModule.render(delta);
                }
            });
        });

    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            HudModule hovered = getHovered((int) mx, (int) my);
            selected = hovered;
            if (hovered != null) {
                dragging = hovered;
                dragOffsetX = mx - hovered.x;
                dragOffsetY = my - hovered.y;
                return true;
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double mouseX, double mouseY) {
        if (dragging != null && event.button() == 0) {
            SnapResult snap = snap((float) (event.x() - dragOffsetX), (float) (event.y() - dragOffsetY), dragging);
            dragging.moveTo(snap.x(), snap.y());
            snapGuideX = snap.guideX();
            snapGuideY = snap.guideY();
            return true;
        }
        return super.mouseDragged(event, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (dragging != null && event.button() == 0) {
            dragging = null;
            snapGuideX = null;
            snapGuideY = null;
            ConfigManager.INSTANCE.saveNow();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (selected == null) {
            return super.keyPressed(event);
        }

        int key = event.key();
        if (key == GLFW.GLFW_KEY_H) {
            selected.cycleHorizontalAnchor();
            ConfigManager.INSTANCE.saveNow();
            return true;
        }
        if (key == GLFW.GLFW_KEY_V) {
            selected.cycleVerticalAnchor();
            ConfigManager.INSTANCE.saveNow();
            return true;
        }

        int step = event.modifiers() != 0 ? 10 : 1;
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> selected.moveBy(-step, 0);
            case GLFW.GLFW_KEY_RIGHT -> selected.moveBy(step, 0);
            case GLFW.GLFW_KEY_UP -> selected.moveBy(0, -step);
            case GLFW.GLFW_KEY_DOWN -> selected.moveBy(0, step);
            default -> {
                return super.keyPressed(event);
            }
        }

        ConfigManager.INSTANCE.saveNow();
        return true;
    }

    @Override
    public void onClose() {
        super.onClose();
        minecraft.setScreen(PanelScreen.INSTANCE);
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }

    private HudModule getHovered(int mouseX, int mouseY) {
        HudModule hovered = null;

        for (Module module : ModuleManager.INSTANCE.getModules()) {
            if (module instanceof HudModule hudModule) {
                hudModule.updateBounds(null);
                hudModule.refreshPosition();
                if (hudModule.contains(mouseX, mouseY)) {
                    hovered = hudModule;
                }
            }
        }

        return hovered;
    }

    private void addAnchorGuides(HudModule module) {
        float verticalGuideX = switch (module.getHorizontalAnchor()) {
            case LEFT -> 0.0f;
            case CENTER -> width / 2.0f;
            case RIGHT -> width - 1.0f;
        };

        float horizontalGuideY = switch (module.getVerticalAnchor()) {
            case TOP -> 0.0f;
            case CENTER -> height / 2.0f;
            case BOTTOM -> height - 1.0f;
        };

        rectRenderer.addRect(verticalGuideX, 0.0f, 1.0f, height, GUIDE_COLOR);
        rectRenderer.addRect(0.0f, horizontalGuideY, width, 1.0f, GUIDE_COLOR);
    }

    private SnapResult snap(float x, float y, HudModule target) {
        float snappedX = x;
        float snappedY = y;
        Float guideX = null;
        Float guideY = null;

        float[] targetXEdges = new float[]{x, x + target.width / 2.0f, x + target.width};
        float[] targetYEdges = new float[]{y, y + target.height / 2.0f, y + target.height};

        float bestXDistance = SNAP_RANGE + 1.0f;
        float bestYDistance = SNAP_RANGE + 1.0f;

        float[] screenXGuides = new float[]{0.0f, width / 2.0f, width};
        float[] screenYGuides = new float[]{0.0f, height / 2.0f, height};

        for (int i = 0; i < targetXEdges.length; i++) {
            float targetEdge = targetXEdges[i];
            for (float guide : screenXGuides) {
                float distance = Math.abs(targetEdge - guide);
                if (distance <= SNAP_RANGE && distance < bestXDistance) {
                    bestXDistance = distance;
                    snappedX = x + (guide - targetEdge);
                    guideX = normalizeVerticalGuide(guide);
                }
            }
        }

        for (int i = 0; i < targetYEdges.length; i++) {
            float targetEdge = targetYEdges[i];
            for (float guide : screenYGuides) {
                float distance = Math.abs(targetEdge - guide);
                if (distance <= SNAP_RANGE && distance < bestYDistance) {
                    bestYDistance = distance;
                    snappedY = y + (guide - targetEdge);
                    guideY = normalizeHorizontalGuide(guide);
                }
            }
        }

        for (Module module : ModuleManager.INSTANCE.getModules()) {
            if (!(module instanceof HudModule hudModule) || hudModule == target) {
                continue;
            }

            hudModule.updateBounds(null);
            hudModule.refreshPosition();

            float[] otherXEdges = new float[]{hudModule.x, hudModule.x + hudModule.width / 2.0f, hudModule.x + hudModule.width};
            float[] otherYEdges = new float[]{hudModule.y, hudModule.y + hudModule.height / 2.0f, hudModule.y + hudModule.height};

            for (float targetEdge : targetXEdges) {
                for (float otherEdge : otherXEdges) {
                    float distance = Math.abs(targetEdge - otherEdge);
                    if (distance <= SNAP_RANGE && distance < bestXDistance) {
                        bestXDistance = distance;
                        snappedX = x + (otherEdge - targetEdge);
                        guideX = otherEdge;
                    }
                }
            }

            for (float targetEdge : targetYEdges) {
                for (float otherEdge : otherYEdges) {
                    float distance = Math.abs(targetEdge - otherEdge);
                    if (distance <= SNAP_RANGE && distance < bestYDistance) {
                        bestYDistance = distance;
                        snappedY = y + (otherEdge - targetEdge);
                        guideY = otherEdge;
                    }
                }
            }
        }

        return new SnapResult(snappedX, snappedY, guideX, guideY);
    }

    private float normalizeVerticalGuide(float guide) {
        if (guide <= 0.0f) {
            return 0.0f;
        }
        if (guide >= width) {
            return width - 1.0f;
        }
        return guide;
    }

    private float normalizeHorizontalGuide(float guide) {
        if (guide <= 0.0f) {
            return 0.0f;
        }
        if (guide >= height) {
            return height - 1.0f;
        }
        return guide;
    }

    private record SnapResult(float x, float y, Float guideX, Float guideY) {
    }

}

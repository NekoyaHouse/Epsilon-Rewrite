package com.github.epsilon.gui.hudeditor;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.gui.panel.PanelScreen;
import com.github.epsilon.managers.ConfigManager;
import com.github.epsilon.managers.ModuleManager;
import com.github.epsilon.managers.RenderManager;
import com.github.epsilon.modules.HudModule;
import com.github.epsilon.modules.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

import java.awt.*;

public class HudEditorScreen extends Screen {

    private static final Color BOX_COLOR = new Color(0, 0, 0, 100);
    private static final Color HOVER_COLOR = new Color(255, 255, 255, 70);
    private static final Color DRAGGING_COLOR = new Color(120, 190, 255, 80);
    private static final Color GUIDE_LINE_COLOR = new Color(255, 255, 255, 26);
    private static final Color GUIDE_BAND_COLOR = new Color(255, 255, 255, 10);
    private static final Color DRAG_GUIDE_LINE_COLOR = new Color(145, 205, 255, 52);
    private static final Color DRAG_GUIDE_BAND_COLOR = new Color(120, 190, 255, 20);
    private static final Color SNAP_PREVIEW_COLOR = new Color(170, 220, 255, 145);
    private static final Color ANCHOR_LABEL_BG = new Color(8, 12, 18, 150);
    private static final Color ANCHOR_LABEL_TEXT = new Color(236, 241, 247, 220);
    private static final Color ANCHOR_MARKER_OUTLINE = new Color(5, 8, 12, 190);
    private static final Color ANCHOR_MARKER_COLOR = new Color(236, 241, 247, 230);
    private static final Color DRAG_ANCHOR_MARKER_COLOR = new Color(170, 220, 255, 235);
    private static final float GUIDE_LABEL_SCALE = 0.72f;
    private static final float GUIDE_THICKNESS = 1.0f;
    private static final float ANCHOR_MARKER_OUTER_SIZE = 7.0f;
    private static final float ANCHOR_MARKER_INNER_SIZE = 4.0f;
    private static final float LABEL_PADDING_X = 6.0f;
    private static final float LABEL_PADDING_Y = 4.0f;
    private static final float LABEL_MARGIN = 6.0f;
    private static final float SNAP_THRESHOLD = 8.0f;

    public static final HudEditorScreen INSTANCE = new HudEditorScreen();

    private HudEditorScreen() {
        super(Component.literal("HUDEditor"));
    }

    private final RectRenderer rectRenderer = new RectRenderer();
    private final RectRenderer overlayRectRenderer = new RectRenderer();
    private final TextRenderer overlayTextRenderer = new TextRenderer();

    private HudModule dragging = null;
    private double dragOffsetX, dragOffsetY;
    private Float snapPreviewX;
    private Float snapPreviewY;

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        HudModule hovered = getHovered(mouseX, mouseY);
        HudModule focus = dragging != null ? dragging : hovered;
        boolean draggingFocus = focus != null && focus == dragging;

        RenderManager.INSTANCE.applyRenderAfterFrame(delta -> {
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();

            if (focus != null) {
                addThirdGuides(focus, draggingFocus, screenWidth, screenHeight);
            }

            ModuleManager.INSTANCE.getModules().forEach(module -> {
                if (module instanceof HudModule hudModule) {
                    hudModule.updateLayout(delta);
                    rectRenderer.addRect(hudModule.x, hudModule.y, hudModule.width, hudModule.height, BOX_COLOR);
                    if (hudModule == hovered) {
                        rectRenderer.addRect(hudModule.x, hudModule.y, hudModule.width, hudModule.height, HOVER_COLOR);
                    }
                    if (hudModule == dragging) {
                        rectRenderer.addRect(hudModule.x, hudModule.y, hudModule.width, hudModule.height, DRAGGING_COLOR);
                    }
                }
            });

            rectRenderer.drawAndClear();

            ModuleManager.INSTANCE.getModules().forEach(module -> {
                if (module instanceof HudModule hudModule) {
                    hudModule.render(delta);
                }
            });

            if (focus != null) {
                addAnchorOverlay(focus, draggingFocus, screenWidth, screenHeight);
            }

            addSnapPreview(screenWidth, screenHeight);

            overlayRectRenderer.drawAndClear();
            overlayTextRenderer.drawAndClear();
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            HudModule hovered = getHovered((int) mx, (int) my);
            if (hovered != null) {
                dragging = hovered;
                dragOffsetX = mx - hovered.x;
                dragOffsetY = my - hovered.y;
                clearSnapPreview();
                return true;
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double mouseX, double mouseY) {
        if (dragging != null && event.button() == 0) {
            dragging.updateLayout(null);

            float targetX = (float) (event.x() - dragOffsetX);
            float targetY = (float) (event.y() - dragOffsetY);
            SnapPosition snap = event.hasAltDown()
                    ? new SnapPosition(targetX, targetY, null, null)
                    : snapPosition(dragging, targetX, targetY);

            dragging.moveTo(snap.renderX(), snap.renderY());
            snapPreviewX = snap.guideX();
            snapPreviewY = snap.guideY();
            return true;
        }
        return super.mouseDragged(event, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        if (dragging != null && event.button() == 0) {
            dragging = null;
            clearSnapPreview();
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
                hudModule.updateLayout(null);
                if (hudModule.contains(mouseX, mouseY)) {
                    hovered = hudModule;
                }
            }
        }

        return hovered;
    }

    private void addThirdGuides(HudModule focus, boolean draggingFocus, int screenWidth, int screenHeight) {
        float splitX1 = screenWidth / 3.0f;
        float splitX2 = splitX1 * 2.0f;
        float splitY1 = screenHeight / 3.0f;
        float splitY2 = splitY1 * 2.0f;
        Color bandColor = draggingFocus ? DRAG_GUIDE_BAND_COLOR : GUIDE_BAND_COLOR;
        Color lineColor = draggingFocus ? DRAG_GUIDE_LINE_COLOR : GUIDE_LINE_COLOR;

        switch (focus.getHorizontalAnchor()) {
            case Left -> rectRenderer.addRect(0.0f, 0.0f, splitX1, screenHeight, bandColor);
            case Center -> rectRenderer.addRect(splitX1, 0.0f, splitX1, screenHeight, bandColor);
            case Right -> rectRenderer.addRect(splitX2, 0.0f, screenWidth - splitX2, screenHeight, bandColor);
        }

        switch (focus.getVerticalAnchor()) {
            case Top -> rectRenderer.addRect(0.0f, 0.0f, screenWidth, splitY1, bandColor);
            case Center -> rectRenderer.addRect(0.0f, splitY1, screenWidth, splitY1, bandColor);
            case Bottom -> rectRenderer.addRect(0.0f, splitY2, screenWidth, screenHeight - splitY2, bandColor);
        }

        rectRenderer.addRect(splitX1, 0.0f, GUIDE_THICKNESS, screenHeight, lineColor);
        rectRenderer.addRect(splitX2, 0.0f, GUIDE_THICKNESS, screenHeight, lineColor);
        rectRenderer.addRect(0.0f, splitY1, screenWidth, GUIDE_THICKNESS, lineColor);
        rectRenderer.addRect(0.0f, splitY2, screenWidth, GUIDE_THICKNESS, lineColor);
    }

    private void addAnchorOverlay(HudModule focus, boolean draggingFocus, int screenWidth, int screenHeight) {
        float anchorX = switch (focus.getHorizontalAnchor()) {
            case Left -> focus.x;
            case Center -> focus.x + focus.width / 2.0f;
            case Right -> focus.x + focus.width;
        };
        float anchorY = switch (focus.getVerticalAnchor()) {
            case Top -> focus.y;
            case Center -> focus.y + focus.height / 2.0f;
            case Bottom -> focus.y + focus.height;
        };

        float outerHalf = ANCHOR_MARKER_OUTER_SIZE / 2.0f;
        float innerHalf = ANCHOR_MARKER_INNER_SIZE / 2.0f;
        Color markerColor = draggingFocus ? DRAG_ANCHOR_MARKER_COLOR : ANCHOR_MARKER_COLOR;
        overlayRectRenderer.addRect(anchorX - outerHalf, anchorY - outerHalf, ANCHOR_MARKER_OUTER_SIZE, ANCHOR_MARKER_OUTER_SIZE, ANCHOR_MARKER_OUTLINE);
        overlayRectRenderer.addRect(anchorX - innerHalf, anchorY - innerHalf, ANCHOR_MARKER_INNER_SIZE, ANCHOR_MARKER_INNER_SIZE, markerColor);

        String label = focus.getHorizontalAnchor().name() + " / " + focus.getVerticalAnchor().name();
        float textWidth = overlayTextRenderer.getWidth(label, GUIDE_LABEL_SCALE);
        float textHeight = overlayTextRenderer.getHeight(GUIDE_LABEL_SCALE);
        float labelWidth = textWidth + LABEL_PADDING_X * 2.0f;
        float labelHeight = textHeight + LABEL_PADDING_Y * 2.0f;
        float labelX = Mth.clamp(focus.x + focus.width / 2.0f - labelWidth / 2.0f, LABEL_MARGIN, screenWidth - labelWidth - LABEL_MARGIN);
        float preferredLabelY = focus.y - labelHeight - LABEL_MARGIN;
        float labelY = preferredLabelY >= LABEL_MARGIN
                ? preferredLabelY
                : Mth.clamp(focus.y + focus.height + LABEL_MARGIN, LABEL_MARGIN, screenHeight - labelHeight - LABEL_MARGIN);

        overlayRectRenderer.addRect(labelX, labelY, labelWidth, labelHeight, ANCHOR_LABEL_BG);
        overlayTextRenderer.addText(label, labelX + LABEL_PADDING_X, labelY + LABEL_PADDING_Y - 1.0f, GUIDE_LABEL_SCALE, ANCHOR_LABEL_TEXT);
    }

    private void addSnapPreview(int screenWidth, int screenHeight) {
        if (snapPreviewX != null) {
            float x = Mth.clamp(snapPreviewX, 0.0f, screenWidth - GUIDE_THICKNESS);
            overlayRectRenderer.addRect(x, 0.0f, GUIDE_THICKNESS, screenHeight, SNAP_PREVIEW_COLOR);
        }

        if (snapPreviewY != null) {
            float y = Mth.clamp(snapPreviewY, 0.0f, screenHeight - GUIDE_THICKNESS);
            overlayRectRenderer.addRect(0.0f, y, screenWidth, GUIDE_THICKNESS, SNAP_PREVIEW_COLOR);
        }
    }

    private SnapPosition snapPosition(HudModule module, float renderX, float renderY) {
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        AxisSnap screenXSnap = snapAxis(
                renderX,
                module.width,
                screenWidth,
                new float[]{0.0f, screenWidth / 3.0f, screenWidth / 2.0f, screenWidth * 2.0f / 3.0f, screenWidth},
                true
        );
        AxisSnap screenYSnap = snapAxis(
                renderY,
                module.height,
                screenHeight,
                new float[]{0.0f, screenHeight / 3.0f, screenHeight / 2.0f, screenHeight * 2.0f / 3.0f, screenHeight},
                false
        );
        AxisSnap moduleXSnap = snapAxisToModules(module, renderX, module.width, screenWidth, true);
        AxisSnap moduleYSnap = snapAxisToModules(module, renderY, module.height, screenHeight, false);
        AxisSnap xSnap = pickCloserSnap(screenXSnap, moduleXSnap);
        AxisSnap ySnap = pickCloserSnap(screenYSnap, moduleYSnap);

        return new SnapPosition(xSnap.renderPosition(), ySnap.renderPosition(), xSnap.guide(), ySnap.guide());
    }

    private AxisSnap snapAxis(float renderPosition, float size, int screenSize, float[] targets, boolean horizontal) {
        float clampedPosition = Mth.clamp(renderPosition, 0.0f, Math.max(0.0f, screenSize - size));
        float bestPosition = clampedPosition;
        Float bestGuide = null;
        float bestDistance = SNAP_THRESHOLD + 1.0f;

        if (horizontal) {
            for (HudModule.HorizontalAnchor anchor : HudModule.HorizontalAnchor.values()) {
                float offset = getHorizontalAnchorOffset(anchor, size);
                for (float target : targets) {
                    float candidate = Mth.clamp(target - offset, 0.0f, Math.max(0.0f, screenSize - size));
                    if (resolveHorizontalAnchor(candidate, size, screenSize) != anchor) continue;

                    float distance = Math.abs(candidate - clampedPosition);
                    if (distance <= SNAP_THRESHOLD && distance < bestDistance) {
                        bestDistance = distance;
                        bestPosition = candidate;
                        bestGuide = target;
                    }
                }
            }
        } else {
            for (HudModule.VerticalAnchor anchor : HudModule.VerticalAnchor.values()) {
                float offset = getVerticalAnchorOffset(anchor, size);
                for (float target : targets) {
                    float candidate = Mth.clamp(target - offset, 0.0f, Math.max(0.0f, screenSize - size));
                    if (resolveVerticalAnchor(candidate, size, screenSize) != anchor) continue;

                    float distance = Math.abs(candidate - clampedPosition);
                    if (distance <= SNAP_THRESHOLD && distance < bestDistance) {
                        bestDistance = distance;
                        bestPosition = candidate;
                        bestGuide = target;
                    }
                }
            }
        }

        return new AxisSnap(bestPosition, bestGuide, bestDistance);
    }

    private AxisSnap snapAxisToModules(HudModule module, float renderPosition, float size, int screenSize, boolean horizontal) {
        float clampedPosition = Mth.clamp(renderPosition, 0.0f, Math.max(0.0f, screenSize - size));
        float bestPosition = clampedPosition;
        Float bestGuide = null;
        float bestDistance = SNAP_THRESHOLD + 1.0f;
        float[] selfOffsets = new float[]{0.0f, size / 2.0f, size};

        for (Module otherModule : ModuleManager.INSTANCE.getModules()) {
            if (!(otherModule instanceof HudModule otherHud) || otherHud == module) {
                continue;
            }

            otherHud.updateLayout(null);
            float[] targets = horizontal
                    ? new float[]{otherHud.x, otherHud.x + otherHud.width / 2.0f, otherHud.x + otherHud.width}
                    : new float[]{otherHud.y, otherHud.y + otherHud.height / 2.0f, otherHud.y + otherHud.height};

            for (float selfOffset : selfOffsets) {
                for (float target : targets) {
                    float candidate = Mth.clamp(target - selfOffset, 0.0f, Math.max(0.0f, screenSize - size));
                    float distance = Math.abs(candidate - clampedPosition);
                    if (distance <= SNAP_THRESHOLD && distance < bestDistance) {
                        bestDistance = distance;
                        bestPosition = candidate;
                        bestGuide = target;
                    }
                }
            }
        }

        return new AxisSnap(bestPosition, bestGuide, bestDistance);
    }

    private static AxisSnap pickCloserSnap(AxisSnap primary, AxisSnap secondary) {
        if (secondary.guide() != null && (primary.guide() == null || secondary.distance() < primary.distance())) {
            return secondary;
        }

        return primary;
    }

    private static float getHorizontalAnchorOffset(HudModule.HorizontalAnchor anchor, float width) {
        return switch (anchor) {
            case Left -> 0.0f;
            case Center -> width / 2.0f;
            case Right -> width;
        };
    }

    private static float getVerticalAnchorOffset(HudModule.VerticalAnchor anchor, float height) {
        return switch (anchor) {
            case Top -> 0.0f;
            case Center -> height / 2.0f;
            case Bottom -> height;
        };
    }

    private static HudModule.HorizontalAnchor resolveHorizontalAnchor(float renderX, float width, int screenWidth) {
        float splitLeft = screenWidth / 3.0f;
        float splitRight = splitLeft * 2.0f;
        boolean left = renderX <= splitLeft;
        boolean right = renderX + width >= splitRight;

        if ((left && right) || (!left && !right)) {
            return HudModule.HorizontalAnchor.Center;
        }

        return left ? HudModule.HorizontalAnchor.Left : HudModule.HorizontalAnchor.Right;
    }

    private static HudModule.VerticalAnchor resolveVerticalAnchor(float renderY, float height, int screenHeight) {
        float splitTop = screenHeight / 3.0f;
        float splitBottom = splitTop * 2.0f;
        boolean top = renderY <= splitTop;
        boolean bottom = renderY + height >= splitBottom;

        if ((top && bottom) || (!top && !bottom)) {
            return HudModule.VerticalAnchor.Center;
        }

        return top ? HudModule.VerticalAnchor.Top : HudModule.VerticalAnchor.Bottom;
    }

    private void clearSnapPreview() {
        snapPreviewX = null;
        snapPreviewY = null;
    }

    private record AxisSnap(float renderPosition, Float guide, float distance) {
    }

    private record SnapPosition(float renderX, float renderY, Float guideX, Float guideY) {
    }

}

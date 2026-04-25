package com.github.epsilon.gui.panel.dsl;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.util.PanelContentBuffer;

import java.awt.*;

import java.util.List;

public final class PanelUiCompiler {

    private PanelUiCompiler() {
    }

    public static void render(PanelUiTree tree, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer) {
        render(tree, null, roundRectRenderer, rectRenderer, textRenderer);
    }

    public static void render(PanelUiTree tree, ShadowRenderer shadowRenderer, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer) {
        renderNodes(tree.nodes(), new RenderTarget(shadowRenderer, roundRectRenderer, rectRenderer, textRenderer));
    }

    private static void renderNodes(List<PanelUiTree.UiNode> nodes, RenderTarget target) {
        for (PanelUiTree.UiNode node : nodes) {
            if (node instanceof PanelUiTree.GroupNode(List<PanelUiTree.UiNode> children1)) {
                renderNodes(children1, target);
                continue;
            }
            if (node instanceof PanelUiTree.ShadowNode(
                    float x2, float y2, float width1, float height1, float topLeft, float topRight, float bottomRight,
                    float bottomLeft, float blurRadius, java.awt.Color color2
            )) {
                if (target.shadowRenderer() != null) {
                    target.shadowRenderer().addShadow(
                            x2, y2, width1, height1,
                            topLeft, topRight, bottomRight, bottomLeft,
                            blurRadius, color2
                    );
                }
                continue;
            }
            if (node instanceof PanelUiTree.RoundRectNode(
                    float x1, float y1, float width, float height, float radiusTopLeft, float radiusTopRight,
                    float radiusBottomRight, float radiusBottomLeft, java.awt.Color color1
            )) {
                target.roundRectRenderer().addRoundRect(
                        x1, y1, width, height,
                        radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft,
                        color1
                );
                continue;
            }
            if (node instanceof PanelUiTree.RectNode(
                    float x1, float y1, float width, float height, java.awt.Color color1
            )) {
                target.rectRenderer().addRect(x1, y1, width, height, color1);
                continue;
            }
            if (node instanceof PanelUiTree.TextNode(
                    String text, float x, float y, float scale, java.awt.Color color,
                    com.github.epsilon.graphics.text.ttf.TtfFontLoader fontLoader
            )) {
                if (fontLoader != null) {
                    target.textRenderer().addText(text, x, y, scale, color, fontLoader);
                } else {
                    target.textRenderer().addText(text, x, y, scale, color);
                }
                continue;
            }
            if (node instanceof PanelUiTree.ButtonNode(
                    float x, float y, float width, float height, float radius, Color background,
                    String label, float labelScale, Color labelColor
            )) {
                target.roundRectRenderer().addRoundRect(x, y, width, height, radius, background);
                float labelWidth = target.textRenderer().getWidth(label, labelScale);
                float labelHeight = target.textRenderer().getHeight(labelScale);
                target.textRenderer().addText(label,
                        x + (width - labelWidth) / 2.0f,
                        y + (height - labelHeight) / 2.0f - 1.0f,
                        labelScale,
                        labelColor);
                continue;
            }
            if (node instanceof PanelUiTree.SwitchNode(com.github.epsilon.gui.panel.PanelLayout.Rect bounds, float toggleProgress, float hoverProgress)) {
                Color track = MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER_HIGHEST, MD3Theme.PRIMARY, toggleProgress);
                Color knob = MD3Theme.lerp(MD3Theme.OUTLINE, MD3Theme.ON_PRIMARY, toggleProgress);
                float knobSize = 8.0f + 3.0f * toggleProgress;
                float knobTravel = bounds.width() - 10.0f - knobSize;
                float knobX = bounds.x() + 5.0f + knobTravel * toggleProgress;
                float knobY = bounds.centerY() - knobSize / 2.0f;
                target.roundRectRenderer().addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), bounds.height() / 2.0f, track);
                if (hoverProgress > 0.02f) {
                    float haloSize = 16.0f;
                    float haloX = knobX + knobSize / 2.0f - haloSize / 2.0f;
                    float haloY = bounds.centerY() - haloSize / 2.0f;
                    target.roundRectRenderer().addRoundRect(haloX, haloY, haloSize, haloSize, haloSize / 2.0f,
                            MD3Theme.stateLayer(MD3Theme.TEXT_PRIMARY, hoverProgress, 18));
                }
                target.roundRectRenderer().addRoundRect(knobX, knobY, knobSize, knobSize, knobSize / 2.0f, knob);
                continue;
            }
            if (node instanceof PanelUiTree.FilledFieldNode(com.github.epsilon.gui.panel.PanelLayout.Rect bounds, boolean focused, float hoverProgress)) {
                target.roundRectRenderer().addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.CONTROL_RADIUS,
                        MD3Theme.filledFieldSurface(focused, hoverProgress));
                float indicatorHeight = focused ? 1.5f : 1.0f;
                float indicatorInset = 4.0f;
                target.rectRenderer().addRect(bounds.x() + indicatorInset, bounds.bottom() - indicatorHeight,
                        Math.max(0.0f, bounds.width() - indicatorInset * 2.0f), indicatorHeight,
                        MD3Theme.filledFieldIndicator(focused, hoverProgress));
                continue;
            }
            if (node instanceof PanelUiTree.AssistChipNode(
                    com.github.epsilon.gui.panel.PanelLayout.Rect bounds, String label, float textScale, Color background, Color foreground,
                    String trailingIcon, float trailingIconScale, com.github.epsilon.graphics.text.ttf.TtfFontLoader trailingIconFont
            )) {
                target.roundRectRenderer().addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.CONTROL_RADIUS, background);
                float textY = bounds.y() + (bounds.height() - target.textRenderer().getHeight(textScale)) / 2.0f - 1.0f;
                target.textRenderer().addText(label, bounds.x() + 8.0f, textY, textScale, foreground);
                if (trailingIcon != null && !trailingIcon.isEmpty() && trailingIconFont != null) {
                    float iconWidth = target.textRenderer().getWidth(trailingIcon, trailingIconScale, trailingIconFont);
                    float iconY = bounds.y() + (bounds.height() - target.textRenderer().getHeight(trailingIconScale, trailingIconFont)) / 2.0f - 1.0f;
                    target.textRenderer().addText(trailingIcon, bounds.right() - 8.0f - iconWidth, iconY, trailingIconScale, foreground, trailingIconFont);
                }
                continue;
            }
            if (node instanceof PanelUiTree.SegmentedControlNode(
                    com.github.epsilon.gui.panel.PanelLayout.Rect bounds, String leadingLabel, String trailingLabel, float progress, float hoverProgress
            )) {
                float outerRadius = MD3Theme.CONTROL_RADIUS;
                float shellInset = 1.0f;
                float innerX = bounds.x() + shellInset;
                float innerY = bounds.y() + shellInset;
                float innerWidth = bounds.width() - shellInset * 2.0f;
                float innerHeight = bounds.height() - shellInset * 2.0f;
                float segmentWidth = innerWidth / 2.0f;
                float indicatorInset = 1.5f;
                float indicatorWidth = segmentWidth - indicatorInset * 2.0f;
                float indicatorX = innerX + indicatorInset + segmentWidth * progress;
                float indicatorY = innerY + indicatorInset;
                float indicatorHeight = innerHeight - indicatorInset * 2.0f;
                float indicatorRadius = Math.max(4.0f, outerRadius - 2.0f);
                float labelScale = 0.52f;
                float labelY = innerY + (innerHeight - target.textRenderer().getHeight(labelScale)) / 2.0f - 1.0f;
                Color inactiveLabel = MD3Theme.segmentedControlInactiveLabel();
                Color activeLabel = MD3Theme.segmentedControlActiveLabel();

                target.roundRectRenderer().addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), outerRadius, MD3Theme.OUTLINE_SOFT);
                target.roundRectRenderer().addRoundRect(innerX, innerY, innerWidth, innerHeight, Math.max(outerRadius - shellInset, 1.0f), MD3Theme.segmentedControlSurface());
                if (hoverProgress > 0.01f) {
                    target.roundRectRenderer().addRoundRect(innerX, innerY, innerWidth, innerHeight, Math.max(outerRadius - shellInset, 1.0f),
                            MD3Theme.stateLayer(MD3Theme.TEXT_PRIMARY, hoverProgress, MD3Theme.isLightTheme() ? 10 : 14));
                }
                target.rectRenderer().addRect(innerX + segmentWidth - 0.5f, innerY + 3.0f, 1.0f, innerHeight - 6.0f, MD3Theme.OUTLINE_SOFT);
                target.roundRectRenderer().addRoundRect(indicatorX, indicatorY, indicatorWidth, indicatorHeight, indicatorRadius, MD3Theme.segmentedControlIndicator());
                float leadingWidth = target.textRenderer().getWidth(leadingLabel, labelScale);
                float trailingWidth = target.textRenderer().getWidth(trailingLabel, labelScale);
                target.textRenderer().addText(leadingLabel, innerX + (segmentWidth - leadingWidth) / 2.0f, labelY, labelScale, MD3Theme.lerp(activeLabel, inactiveLabel, progress));
                target.textRenderer().addText(trailingLabel, innerX + segmentWidth + (segmentWidth - trailingWidth) / 2.0f, labelY, labelScale, MD3Theme.lerp(inactiveLabel, activeLabel, progress));
                continue;
            }
            if (node instanceof PanelUiTree.IconButtonNode(com.github.epsilon.gui.panel.PanelLayout.Rect bounds, String label, float scale, Color tone, float hoverProgress)) {
                target.roundRectRenderer().addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), bounds.height() / 2.0f,
                        MD3Theme.stateLayer(tone, hoverProgress, 32));
                Color labelColor = MD3Theme.lerp(MD3Theme.TEXT_MUTED, tone, hoverProgress);
                float labelWidth = target.textRenderer().getWidth(label, scale);
                float labelHeight = target.textRenderer().getHeight(scale);
                target.textRenderer().addText(label,
                        bounds.x() + (bounds.width() - labelWidth) / 2.0f,
                        bounds.y() + (bounds.height() - labelHeight) / 2.0f - 1.0f,
                        scale,
                        labelColor);
                continue;
            }
            if (node instanceof PanelUiTree.ViewportNode(
                    PanelContentBuffer buffer, com.github.epsilon.gui.panel.PanelLayout.Rect viewport, int guiHeight,
                    float scroll, float maxScroll, float contentHeight, List<PanelUiTree.UiNode> children
            )) {
                renderNodes(children, RenderTarget.forContentBuffer(buffer));
                buffer.queueViewport(viewport, guiHeight, scroll, maxScroll, contentHeight);
            }
        }
    }

    private record RenderTarget(ShadowRenderer shadowRenderer, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer) {
        private static RenderTarget forContentBuffer(PanelContentBuffer buffer) {
            return new RenderTarget(buffer.shadowRenderer(), buffer.roundRectRenderer(), buffer.rectRenderer(), buffer.textRenderer());
        }
    }
}



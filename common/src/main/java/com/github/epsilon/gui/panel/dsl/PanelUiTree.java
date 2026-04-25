package com.github.epsilon.gui.panel.dsl;

import com.github.epsilon.graphics.text.ttf.TtfFontLoader;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.util.PanelContentBuffer;
import com.github.epsilon.utils.render.animation.Animation;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class PanelUiTree {

    private static final Map<MemoKey, MemoEntry> MEMO_CACHE = new HashMap<>();

    private final List<UiNode> nodes;
    private final boolean hasActiveAnimations;

    private PanelUiTree(List<UiNode> nodes, boolean hasActiveAnimations) {
        this.nodes = nodes;
        this.hasActiveAnimations = hasActiveAnimations;
    }

    public static PanelUiTree build(Consumer<Scope> content) {
        Scope scope = new Scope();
        content.accept(scope);
        return new PanelUiTree(List.copyOf(scope.nodes), scope.hasActiveAnimations);
    }

    List<UiNode> nodes() {
        return nodes;
    }

    public boolean hasActiveAnimations() {
        return hasActiveAnimations;
    }

    public static final class Scope {

        private List<UiNode> nodes = new ArrayList<>();
        private boolean hasActiveAnimations;

        public void group(Consumer<Scope> content) {
            CaptureResult capture = capture(content);
            hasActiveAnimations = hasActiveAnimations || capture.hasActiveAnimations();
            nodes.add(new GroupNode(capture.nodes()));
        }

        public void memo(Object key, long signature, Consumer<Scope> content) {
            MemoKey memoKey = new MemoKey(key, signature);
            MemoEntry cached = MEMO_CACHE.get(memoKey);
            if (cached != null) {
                nodes.add(new GroupNode(cached.nodes()));
                return;
            }

            CaptureResult capture = capture(content);
            hasActiveAnimations = hasActiveAnimations || capture.hasActiveAnimations();
            nodes.add(new GroupNode(capture.nodes()));
            if (!capture.hasActiveAnimations()) {
                MEMO_CACHE.put(memoKey, new MemoEntry(capture.nodes()));
            }
        }

        public float animate(Animation animation, boolean target) {
            return animate(animation, target ? 1.0f : 0.0f);
        }

        public float animate(Animation animation, float target) {
            animation.run(target);
            hasActiveAnimations = hasActiveAnimations || !animation.isFinished();
            return animation.getValue();
        }

        public void shadow(float x, float y, float width, float height, float radius, float blurRadius, Color color) {
            nodes.add(new ShadowNode(x, y, width, height, radius, radius, radius, radius, blurRadius, color));
        }

        public void shadow(float x, float y, float width, float height,
                           float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft,
                           float blurRadius, Color color) {
            nodes.add(new ShadowNode(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, blurRadius, color));
        }

        public void roundRect(float x, float y, float width, float height, float radius, Color color) {
            nodes.add(new RoundRectNode(x, y, width, height, radius, radius, radius, radius, color));
        }

        public void roundRect(float x, float y, float width, float height,
                              float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft,
                              Color color) {
            nodes.add(new RoundRectNode(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color));
        }

        public void rect(float x, float y, float width, float height, Color color) {
            nodes.add(new RectNode(x, y, width, height, color));
        }

        public void text(String text, float x, float y, float scale, Color color) {
            nodes.add(new TextNode(text, x, y, scale, color, null));
        }

        public void text(String text, float x, float y, float scale, Color color, TtfFontLoader fontLoader) {
            nodes.add(new TextNode(text, x, y, scale, color, fontLoader));
        }

        public void button(float x, float y, float width, float height, float radius, Color background,
                           String label, float labelScale, Color labelColor) {
            button(new ButtonElement(new PanelLayout.Rect(x, y, width, height), radius, background, label, labelScale, labelColor));
        }

        public void button(PanelLayout.Rect bounds, float radius, Color background,
                           String label, float labelScale, Color labelColor) {
            button(new ButtonElement(bounds, radius, background, label, labelScale, labelColor));
        }

        public void button(ButtonElement element) {
            PanelLayout.Rect bounds = element.bounds();
            nodes.add(new ButtonNode(bounds.x(), bounds.y(), bounds.width(), bounds.height(), element.radius(),
                    element.background(), element.label(), element.labelScale(), element.labelColor()));
        }

        public void switchControl(PanelLayout.Rect bounds, float toggleProgress, float hoverProgress) {
            toggleSwitch(new SwitchElement(bounds, toggleProgress, hoverProgress));
        }

        public void toggle(PanelLayout.Rect bounds, float toggleProgress, float hoverProgress) {
            toggleSwitch(new SwitchElement(bounds, toggleProgress, hoverProgress));
        }

        public void toggleSwitch(SwitchElement element) {
            nodes.add(new SwitchNode(element.bounds(), element.toggleProgress(), element.hoverProgress()));
        }

        public void toggle(SwitchElement element) {
            toggleSwitch(element);
        }

        public void filledField(PanelLayout.Rect bounds, boolean focused, float hoverProgress) {
            input(new InputElement(bounds, focused, hoverProgress,
                    0.0f, new Color(0, 0, 0, 0), 0.0f,
                    6.0f, null, 0.0f, new Color(0, 0, 0, 0),
                    null, null,
                    null, null,
                    null, 0.0f, null));
        }

        public void input(InputElement element) {
            nodes.add(new InputNode(element));
        }

        public void input(PanelLayout.Rect bounds, boolean focused, float hoverProgress,
                          float textInset, @Nullable String text, float textScale, Color textColor,
                          @Nullable Integer caretIndex, @Nullable Color caretColor,
                          @Nullable String trailingHint, float trailingHintScale, @Nullable Color trailingHintColor) {
            input(new InputElement(bounds, focused, hoverProgress,
                    0.0f, new Color(0, 0, 0, 0), 0.0f,
                    textInset, text, textScale, textColor,
                    null, null,
                    caretIndex, caretColor,
                    trailingHint, trailingHintScale, trailingHintColor));
        }

        public void input(PanelLayout.Rect bounds, boolean focused, float hoverProgress,
                          float focusRingProgress, Color focusRingColor, float focusRingInset,
                          float textInset, @Nullable String text, float textScale, Color textColor,
                          @Nullable SelectionRange selection, @Nullable Color selectionColor,
                          @Nullable Integer caretIndex, @Nullable Color caretColor,
                          @Nullable String trailingHint, float trailingHintScale, @Nullable Color trailingHintColor) {
            input(new InputElement(bounds, focused, hoverProgress,
                    focusRingProgress, focusRingColor, focusRingInset,
                    textInset, text, textScale, textColor,
                    selection, selectionColor,
                    caretIndex, caretColor,
                    trailingHint, trailingHintScale, trailingHintColor));
        }

        public void assistChip(PanelLayout.Rect bounds, String label, float textScale, Color background, Color foreground,
                               @Nullable String trailingIcon, float trailingIconScale, @Nullable TtfFontLoader trailingIconFont) {
            nodes.add(new AssistChipNode(bounds, label, textScale, background, foreground, trailingIcon, trailingIconScale, trailingIconFont));
        }

        public void chip(PanelLayout.Rect bounds, String label, float textScale, Color background, Color foreground,
                         @Nullable String trailingIcon, float trailingIconScale, @Nullable TtfFontLoader trailingIconFont) {
            assistChip(bounds, label, textScale, background, foreground, trailingIcon, trailingIconScale, trailingIconFont);
        }

        public void segmentedControl(PanelLayout.Rect bounds, String leadingLabel, String trailingLabel,
                                     float progress, float hoverProgress) {
            nodes.add(new SegmentedControlNode(bounds, leadingLabel, trailingLabel, progress, hoverProgress));
        }

        public void segmented(PanelLayout.Rect bounds, String leadingLabel, String trailingLabel,
                              float progress, float hoverProgress) {
            segmentedControl(bounds, leadingLabel, trailingLabel, progress, hoverProgress);
        }

        public void iconButton(PanelLayout.Rect bounds, String label, float scale, Color tone, float hoverProgress) {
            nodes.add(new IconButtonNode(bounds, label, scale, tone, hoverProgress));
        }

        public void popupCard(PanelLayout.Rect bounds, float radius, float blurRadius, Color shadowColor, Color surfaceColor) {
            nodes.add(new PopupCardNode(bounds, radius, blurRadius, shadowColor, surfaceColor));
        }

        public void slider(PanelLayout.Rect bounds, float progress, float trackRadius,
                           Color trackColor, float activeEndInset, float activeMinWidth, Color activeColor,
                           float handleWidth, float handleHeight, float handleRadius, Color handleColor) {
            nodes.add(new SliderNode(bounds, progress, trackRadius, trackColor, activeEndInset, activeMinWidth, activeColor,
                    handleWidth, handleHeight, handleRadius, handleColor));
        }

        public void viewport(PanelContentBuffer buffer, PanelLayout.Rect viewport, int guiHeight,
                             float scroll, float maxScroll, float contentHeight,
                             Consumer<Scope> content) {
            CaptureResult capture = capture(content);
            hasActiveAnimations = hasActiveAnimations || capture.hasActiveAnimations();
            nodes.add(new ViewportNode(buffer, viewport, guiHeight, scroll, maxScroll, contentHeight, capture.nodes()));
        }

        private CaptureResult capture(Consumer<Scope> content) {
            List<UiNode> parent = nodes;
            boolean parentAnimations = hasActiveAnimations;
            nodes = new ArrayList<>();
            hasActiveAnimations = false;
            try {
                content.accept(this);
                return new CaptureResult(List.copyOf(nodes), hasActiveAnimations);
            } finally {
                nodes = parent;
                hasActiveAnimations = parentAnimations;
            }
        }
    }

    private record CaptureResult(List<UiNode> nodes, boolean hasActiveAnimations) {
    }

    private record MemoKey(Object key, long signature) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MemoKey other)) {
                return false;
            }
            return signature == other.signature && Objects.equals(key, other.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, signature);
        }
    }

    private record MemoEntry(List<UiNode> nodes) {
    }

    sealed interface UiNode permits GroupNode, ShadowNode, RoundRectNode, RectNode, TextNode, ButtonNode, SwitchNode, FilledFieldNode, InputNode, AssistChipNode, SegmentedControlNode, IconButtonNode, PopupCardNode, SliderNode, ViewportNode {
    }

    public record ButtonElement(PanelLayout.Rect bounds, float radius, Color background,
                                String label, float labelScale, Color labelColor) {
    }

    public record SwitchElement(PanelLayout.Rect bounds, float toggleProgress, float hoverProgress) {
    }

    public record SelectionRange(int start, int end) {
    }

    public record InputElement(PanelLayout.Rect bounds, boolean focused, float hoverProgress,
                               float focusRingProgress, Color focusRingColor, float focusRingInset,
                               float textInset, @Nullable String text, float textScale, Color textColor,
                               @Nullable SelectionRange selection, @Nullable Color selectionColor,
                               @Nullable Integer caretIndex, @Nullable Color caretColor,
                               @Nullable String trailingHint, float trailingHintScale, @Nullable Color trailingHintColor) {
    }

    record GroupNode(List<UiNode> children) implements UiNode {
    }

    record ShadowNode(float x, float y, float width, float height,
                      float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft,
                      float blurRadius, Color color) implements UiNode {
    }

    record RoundRectNode(float x, float y, float width, float height,
                         float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft,
                         Color color) implements UiNode {
    }

    record RectNode(float x, float y, float width, float height, Color color) implements UiNode {
    }

    record TextNode(String text, float x, float y, float scale, Color color,
                    @Nullable TtfFontLoader fontLoader) implements UiNode {
    }

    record ButtonNode(float x, float y, float width, float height, float radius, Color background,
                      String label, float labelScale, Color labelColor) implements UiNode {
    }

    record SwitchNode(PanelLayout.Rect bounds, float toggleProgress, float hoverProgress) implements UiNode {
    }

    record FilledFieldNode(PanelLayout.Rect bounds, boolean focused, float hoverProgress) implements UiNode {
    }

    record InputNode(InputElement element) implements UiNode {
    }

    record AssistChipNode(PanelLayout.Rect bounds, String label, float textScale, Color background, Color foreground,
                          @Nullable String trailingIcon, float trailingIconScale, @Nullable TtfFontLoader trailingIconFont) implements UiNode {
    }

    record SegmentedControlNode(PanelLayout.Rect bounds, String leadingLabel, String trailingLabel,
                                float progress, float hoverProgress) implements UiNode {
    }

    record IconButtonNode(PanelLayout.Rect bounds, String label, float scale, Color tone, float hoverProgress) implements UiNode {
    }

    record PopupCardNode(PanelLayout.Rect bounds, float radius, float blurRadius, Color shadowColor, Color surfaceColor) implements UiNode {
    }

    record SliderNode(PanelLayout.Rect bounds, float progress, float trackRadius, Color trackColor,
                      float activeEndInset, float activeMinWidth, Color activeColor,
                      float handleWidth, float handleHeight, float handleRadius, Color handleColor) implements UiNode {
    }

    record ViewportNode(PanelContentBuffer buffer, PanelLayout.Rect viewport, int guiHeight,
                        float scroll, float maxScroll, float contentHeight,
                        List<UiNode> children) implements UiNode {
    }
}



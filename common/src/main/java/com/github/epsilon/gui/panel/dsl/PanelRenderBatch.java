package com.github.epsilon.gui.panel.dsl;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;

public final class PanelRenderBatch {

    private final ShadowRenderer shadowRenderer;
    private final RoundRectRenderer roundRectRenderer;
    private final RectRenderer rectRenderer;
    private final TextRenderer textRenderer;

    public PanelRenderBatch() {
        this(new ShadowRenderer(), new RoundRectRenderer(), new RectRenderer(), new TextRenderer());
    }

    public PanelRenderBatch(ShadowRenderer shadowRenderer, RoundRectRenderer roundRectRenderer,
                            RectRenderer rectRenderer, TextRenderer textRenderer) {
        this.shadowRenderer = shadowRenderer;
        this.roundRectRenderer = roundRectRenderer;
        this.rectRenderer = rectRenderer;
        this.textRenderer = textRenderer;
    }

    public ShadowRenderer shadowRenderer() {
        return shadowRenderer;
    }

    public RoundRectRenderer roundRectRenderer() {
        return roundRectRenderer;
    }

    public RectRenderer rectRenderer() {
        return rectRenderer;
    }

    public TextRenderer textRenderer() {
        return textRenderer;
    }

    public void render(PanelUiTree tree) {
        PanelUiCompiler.render(tree, shadowRenderer, roundRectRenderer, rectRenderer, textRenderer);
    }

    public void flush() {
        shadowRenderer.draw();
        roundRectRenderer.draw();
        rectRenderer.draw();
        textRenderer.draw();
    }

    public void clear() {
        shadowRenderer.clear();
        roundRectRenderer.clear();
        rectRenderer.clear();
        textRenderer.clear();
    }

    public void flushAndClear() {
        flush();
        clear();
    }
}


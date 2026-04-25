package com.github.epsilon.gui.panel.dsl;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;

/**
 * 面板 UI 的通用 renderer 批次封装。
 * <p>
 * 一个批次持有阴影、圆角矩形、矩形和文本四类 renderer，负责承接
 * {@link PanelUiCompiler} 的编译输出，并在统一阶段执行 flush 或 clear。
 */
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

    /**
     * 将 UI 树编译进当前批次持有的 renderer。
     *
     * @param tree 待编译的 UI 树
     */
    public void render(PanelUiTree tree) {
        PanelUiCompiler.render(tree, shadowRenderer, roundRectRenderer, rectRenderer, textRenderer);
    }

    /**
     * 将当前批次中的所有 renderer 按固定顺序输出到 GPU。
     * <p>
     * 该方法不会清空批次内容，适用于需要延迟清理或复用状态的场景。
     */
    public void flush() {
        shadowRenderer.draw();
        roundRectRenderer.draw();
        rectRenderer.draw();
        textRenderer.draw();
    }

    /**
     * 清空当前批次持有的所有 renderer 缓冲。
     */
    public void clear() {
        shadowRenderer.clear();
        roundRectRenderer.clear();
        rectRenderer.clear();
        textRenderer.clear();
    }

    /**
     * 先输出当前批次，再立即清空所有 renderer 缓冲。
     */
    public void flushAndClear() {
        flush();
        clear();
    }
}


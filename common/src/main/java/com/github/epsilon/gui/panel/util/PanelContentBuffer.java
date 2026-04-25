package com.github.epsilon.gui.panel.util;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.gui.panel.PanelLayout;

/**
 * 面板视口内容缓冲。
 * <p>
 * 该缓冲用于承接需要裁剪输出的子内容，例如滚动列表、弹窗下拉内容等。
 * 编译阶段先写入缓冲，flush 阶段再统一应用 scissor 并输出滚动条。
 */
public final class PanelContentBuffer {

    private final RoundRectRenderer roundRectRenderer = new RoundRectRenderer();
    private final RectRenderer rectRenderer = new RectRenderer();
    private final ShadowRenderer shadowRenderer = new ShadowRenderer();
    private final TextRenderer textRenderer = new TextRenderer();
    private final RoundRectRenderer scrollBarRenderer = new RoundRectRenderer();

    private boolean pending;

    public RoundRectRenderer roundRectRenderer() {
        return roundRectRenderer;
    }

    public RectRenderer rectRenderer() {
        return rectRenderer;
    }

    public ShadowRenderer shadowRenderer() {
        return shadowRenderer;
    }

    public TextRenderer textRenderer() {
        return textRenderer;
    }

    /**
     * 清空内容缓冲及滚动条缓冲，并重置待输出标记。
     */
    public void clear() {
        clearContent();
        scrollBarRenderer.clear();
        pending = false;
    }

    private void clearContent() {
        shadowRenderer.clear();
        roundRectRenderer.clear();
        rectRenderer.clear();
        textRenderer.clear();
    }

    /**
     * 为当前内容缓冲登记一个视口输出阶段。
     * <p>
     * 该方法会应用裁剪区域并预先构建滚动条，但不会立即绘制。
     *
     * @param viewport 视口区域
     * @param guiHeight 当前 GUI 高度，用于换算 scissor 坐标
     * @param scroll 当前滚动偏移
     * @param maxScroll 最大滚动偏移
     * @param contentHeight 内容总高度
     */
    public void queueViewport(PanelLayout.Rect viewport, int guiHeight, float scroll, float maxScroll, float contentHeight) {
        PanelScissor.apply(viewport, rectRenderer, roundRectRenderer, shadowRenderer, textRenderer, guiHeight);
        scrollBarRenderer.clear();
        ScrollBarUtil.draw(scrollBarRenderer, viewport, scroll, maxScroll, contentHeight);
        pending = true;
    }

    /**
     * 输出当前缓冲中的内容，并在输出后恢复 scissor 状态。
     */
    public void flush() {
        if (!pending) {
            return;
        }
        shadowRenderer.draw();
        roundRectRenderer.draw();
        rectRenderer.draw();
        textRenderer.draw();
        PanelScissor.clear(rectRenderer, roundRectRenderer, shadowRenderer, textRenderer);
        scrollBarRenderer.draw();
        scrollBarRenderer.clear();
        pending = false;
    }

    /**
     * 先输出视口内容，再清空内容缓冲。
     */
    public void flushAndClear() {
        flush();
        clearContent();
    }

}

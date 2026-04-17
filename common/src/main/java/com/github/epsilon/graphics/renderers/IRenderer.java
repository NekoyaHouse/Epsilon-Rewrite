package com.github.epsilon.graphics.renderers;

public interface IRenderer {

    void draw();

    /**
     * 一帧内 在 clear() 之后 不能再进行 draw() / drawAndClear()
     */
    void clear();

    /**
     * 一帧内 在 drawAndClear() 之后 不能再进行 draw() / drawAndClear()
     */
    default void drawAndClear() {
        draw();
        clear();
    }

    void close();

}

package com.github.epsilon.graphics.elements;

import java.awt.*;

public record RectElement(
        float x,
        float y,
        float width,
        float height,
        Color topLeft,
        Color bottomLeft,
        Color bottomRight,
        Color topRight
) {

    public static RectElement solid(float x, float y, float width, float height, Color color) {
        return new RectElement(x, y, width, height, color, color, color, color);
    }

    public static RectElement verticalGradient(float x, float y, float width, float height, Color top, Color bottom) {
        return new RectElement(x, y, width, height, top, bottom, bottom, top);
    }

    public static RectElement horizontalGradient(float x, float y, float width, float height, Color left, Color right) {
        return new RectElement(x, y, width, height, left, left, right, right);
    }
}


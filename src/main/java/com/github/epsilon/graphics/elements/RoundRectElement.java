package com.github.epsilon.graphics.elements;

import java.awt.*;

public record RoundRectElement(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        Color color
) {

    public static RoundRectElement uniform(float x, float y, float width, float height, float radius, Color color) {
        return new RoundRectElement(x, y, width, height, radius, radius, radius, radius, color);
    }
}


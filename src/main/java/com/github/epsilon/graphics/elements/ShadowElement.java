package com.github.epsilon.graphics.elements;

import java.awt.*;

public record ShadowElement(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        float blurRadius,
        Color color
) {

    public static ShadowElement uniform(float x, float y, float width, float height, float radius, float blurRadius, Color color) {
        return new ShadowElement(x, y, width, height, radius, radius, radius, radius, blurRadius, color);
    }
}


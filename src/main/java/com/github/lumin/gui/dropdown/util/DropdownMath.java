package com.github.lumin.gui.dropdown.util;

public final class DropdownMath {

    private DropdownMath() {
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float lerp(float start, float end, float delta) {
        return start + (end - start) * clamp(delta, 0.0f, 1.0f);
    }

    public static boolean contains(double x, double y, float left, float top, float width, float height) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
    }
}

package com.github.lumin.gui.dropdown;

import net.minecraft.util.Mth;

import java.awt.*;

public final class DropdownTheme {

    public static final Color SCRIM = new Color(7, 10, 13, 138);
    public static final Color SHADOW = new Color(0, 0, 0, 72);

    public static final Color SURFACE = new Color(22, 26, 29, 238);
    public static final Color SURFACE_DIM = new Color(18, 22, 25, 232);
    public static final Color SURFACE_CONTAINER = new Color(29, 34, 39, 244);
    public static final Color SURFACE_CONTAINER_HIGH = new Color(35, 41, 47, 248);
    public static final Color SURFACE_CONTAINER_HIGHEST = new Color(41, 48, 55, 252);

    public static final Color OUTLINE = new Color(63, 75, 85, 180);
    public static final Color OUTLINE_SOFT = new Color(63, 75, 85, 96);

    public static final Color PRIMARY = new Color(124, 198, 255);
    public static final Color PRIMARY_CONTAINER = new Color(18, 50, 71, 230);
    public static final Color ON_PRIMARY_CONTAINER = new Color(214, 238, 255);

    public static final Color SECONDARY_CONTAINER = new Color(36, 49, 58, 236);
    public static final Color ON_SECONDARY_CONTAINER = new Color(222, 233, 241);

    public static final Color TEXT_PRIMARY = new Color(230, 237, 243);
    public static final Color TEXT_SECONDARY = new Color(154, 167, 178);
    public static final Color TEXT_MUTED = new Color(122, 134, 144);
    public static final Color SUCCESS = new Color(129, 199, 132);
    public static final Color ERROR = new Color(255, 138, 128);

    public static final int PANEL_RADIUS = 18;
    public static final int SECTION_RADIUS = 14;
    public static final int CARD_RADIUS = 10;
    public static final int CHIP_RADIUS = 999;

    public static final float OUTER_PADDING = 11.0f;
    public static final float SECTION_GAP = 8.0f;
    public static final float INNER_PADDING = 10.0f;
    public static final float ROW_GAP = 5.0f;

    private DropdownTheme() {
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color lerp(Color start, Color end, float delta) {
        float t = Mth.clamp(delta, 0.0f, 1.0f);
        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * t);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * t);
        int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * t);
        int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * t);
        return new Color(r, g, b, a);
    }

}

package com.github.epsilon.utils.render;

import net.minecraft.util.Mth;

import java.awt.*;

public class ColorUtils {

    private static int clamp255(int value) {
        return Mth.clamp(value, 0, 255);
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Mth.clamp(opacity, 0.0f, 1.0f);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static int getRainbowOpaque(int index, float saturation, float brightness, float speed) {
        float hue = (float) ((System.currentTimeMillis() + (long) index) % (long) ((int) speed)) / speed;
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    public static Color interpolateColor(Color color1, Color color2, float fraction) {
        fraction = Mth.clamp(fraction, 0.0f, 1.0f);

        int red = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * fraction);
        int green = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * fraction);
        int blue = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * fraction);
        int alpha = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * fraction);

        return new Color(clamp255(red), clamp255(green), clamp255(blue), clamp255(alpha));
    }

    public static Color colorSwitchToColor(Color firstColor, Color secondColor, float time, int index, long timePerIndex, double speed, int alpha) {
        long timeInLong = (long) time;
        long now = (long) (speed * System.currentTimeMillis() + index * timePerIndex);

        float redDiff = (firstColor.getRed() - secondColor.getRed()) / time;
        float greenDiff = (firstColor.getGreen() - secondColor.getGreen()) / time;
        float blueDiff = (firstColor.getBlue() - secondColor.getBlue()) / time;

        float redInverseDiff = (secondColor.getRed() - firstColor.getRed()) / time;
        float greenInverseDiff = (secondColor.getGreen() - firstColor.getGreen()) / time;
        float blueInverseDiff = (secondColor.getBlue() - firstColor.getBlue()) / time;

        long fullCycle = timeInLong * 2;
        long currentTimeInCycle = now % fullCycle;

        Color resultColor;

        if (currentTimeInCycle < timeInLong) {
            long progressTime = currentTimeInCycle;
            int red = Math.round(firstColor.getRed() + redInverseDiff * progressTime);
            int green = Math.round(firstColor.getGreen() + greenInverseDiff * progressTime);
            int blue = Math.round(firstColor.getBlue() + blueInverseDiff * progressTime);

            resultColor = new Color(
                    Mth.clamp(red, 0, 255),
                    Mth.clamp(green, 0, 255),
                    Mth.clamp(blue, 0, 255),
                    Mth.clamp(alpha, 0, 255)
            );
        } else {
            long progressTime = currentTimeInCycle - timeInLong;
            int red = Math.round(secondColor.getRed() + redDiff * progressTime);
            int green = Math.round(secondColor.getGreen() + greenDiff * progressTime);
            int blue = Math.round(secondColor.getBlue() + blueDiff * progressTime);

            resultColor = new Color(
                    Mth.clamp(red, 0, 255),
                    Mth.clamp(green, 0, 255),
                    Mth.clamp(blue, 0, 255),
                    Mth.clamp(alpha, 0, 255)
            );
        }

        return resultColor;
    }

    public static Color fadeCustomColor(Color color, long time, int index, float speed1) {
        float speed = speed1;
        int tick = (int) (time / (50 / speed)) + index * 15;

        int red = (int) color.getRed();
        int green = (int) color.getGreen();
        int blue = (int) color.getBlue();
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        float baseHue = hsb[0];
        float baseSaturation = hsb[1];
        float baseBrightness = hsb[2];

        float hueVariation = 0.05f;
        float hue = baseHue + (float) Math.sin(tick * 0.02) * hueVariation;

        float saturationVariation = 0.15f;
        float saturation = Math.max(0.1f, Math.min(1.0f,
                baseSaturation + (float) Math.sin(tick * 0.015) * saturationVariation));

        float brightnessVariation = 0.2f;
        float brightness = Math.max(0.3f, Math.min(1.0f, baseBrightness + (float) Math.sin(tick * 0.01) * brightnessVariation));

        return Color.getHSBColor(hue, saturation, brightness);
    }

}

package dev.maru.verify.util;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.util.concurrent.atomic.AtomicReference;

public final class WindowUtil {
    private static final AtomicReference<Long> vgRef = new AtomicReference<>(0L);

    private WindowUtil() {
    }

    public static void setVg(long vg) {
        vgRef.set(vg);
    }

    public static long getVg() {
        return vgRef.get();
    }

    public static void setColor(NVGColor c, int r, int g, int b, int a) {
        NanoVG.nvgRGBA((byte) r, (byte) g, (byte) b, (byte) a, c);
    }

    public static void drawText(int font, float size, float x, float y, int align, int r, int g, int b, int a, String text, MemoryStack stack) {
        long vg = vgRef.get();
        if (vg == 0L) return;
        NanoVG.nvgFontFaceId(vg, font);
        NanoVG.nvgFontSize(vg, size);
        NanoVG.nvgTextAlign(vg, align);
        NVGColor c = NVGColor.malloc(stack);
        setColor(c, r, g, b, a);
        NanoVG.nvgFillColor(vg, c);
        NanoVG.nvgText(vg, x, y, text == null ? "" : text);
    }

    public static float textWidth(int font, float size, String text) {
        long vg = vgRef.get();
        if (vg == 0L) return 0f;
        NanoVG.nvgFontFaceId(vg, font);
        NanoVG.nvgFontSize(vg, size);
        float[] bounds = new float[4];
        return NanoVG.nvgTextBounds(vg, 0, 0, text == null ? "" : text, bounds);
    }

    public static void drawRRect(float x, float y, float w, float h, float r, int cr, int cg, int cb, int ca, MemoryStack stack) {
        long vg = vgRef.get();
        if (vg == 0L) return;
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
        NVGColor c = NVGColor.malloc(stack);
        setColor(c, cr, cg, cb, ca);
        NanoVG.nvgFillColor(vg, c);
        NanoVG.nvgFill(vg);
    }

    public static void drawRRectStroke(float x, float y, float w, float h, float r, float strokeW, int cr, int cg, int cb, int ca, MemoryStack stack) {
        long vg = vgRef.get();
        if (vg == 0L) return;
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
        NVGColor c = NVGColor.malloc(stack);
        setColor(c, cr, cg, cb, ca);
        NanoVG.nvgStrokeColor(vg, c);
        NanoVG.nvgStrokeWidth(vg, strokeW);
        NanoVG.nvgStroke(vg);
    }

    public static void drawCircle(float x, float y, float radius, int cr, int cg, int cb, int ca, MemoryStack stack) {
        long vg = vgRef.get();
        if (vg == 0L) return;
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgCircle(vg, x, y, radius);
        NVGColor c = NVGColor.malloc(stack);
        setColor(c, cr, cg, cb, ca);
        NanoVG.nvgFillColor(vg, c);
        NanoVG.nvgFill(vg);
    }

    public static void drawGradientRect(float x, float y, float w, float h, int r0, int g0, int b0, int a0, int r1, int g1, int b1, int a1, MemoryStack stack) {
        long vg = vgRef.get();
        if (vg == 0L) return;
        NVGColor c0 = NVGColor.malloc(stack);
        NVGColor c1 = NVGColor.malloc(stack);
        setColor(c0, r0, g0, b0, a0);
        setColor(c1, r1, g1, b1, a1);
        NVGPaint paint = NVGPaint.malloc(stack);
        NanoVG.nvgLinearGradient(vg, x, y, x, y + h, c0, c1, paint);
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRect(vg, x, y, w, h);
        NanoVG.nvgFillPaint(vg, paint);
        NanoVG.nvgFill(vg);
    }

    public static void drawGradientRRect(float x, float y, float w, float h, float r, int r0, int g0, int b0, int a0, int r1, int g1, int b1, int a1, MemoryStack stack) {
        long vg = vgRef.get();
        if (vg == 0L) return;
        NVGColor c0 = NVGColor.malloc(stack);
        NVGColor c1 = NVGColor.malloc(stack);
        setColor(c0, r0, g0, b0, a0);
        setColor(c1, r1, g1, b1, a1);
        NVGPaint paint = NVGPaint.malloc(stack);
        NanoVG.nvgLinearGradient(vg, x, y, x, y + h, c0, c1, paint);
        NanoVG.nvgBeginPath(vg);
        NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
        NanoVG.nvgFillPaint(vg, paint);
        NanoVG.nvgFill(vg);
    }

    public static float approach(float current, float target, float speed, float dt) {
        float k = 1f - (float) Math.exp(-speed * dt);
        return current + (target - current) * k;
    }

    public static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public static int mixInt(int a, int b, float t) {
        t = clamp(t, 0f, 1f);
        return (int) (a + (b - a) * t);
    }

    public static float easeOutCubic(float t) {
        t = clamp(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u;
    }
}

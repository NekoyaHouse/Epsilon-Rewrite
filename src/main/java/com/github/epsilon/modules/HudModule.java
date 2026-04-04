package com.github.epsilon.modules;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

public abstract class HudModule extends Module {

    public enum HorizontalAnchor {
        LEFT,
        CENTER,
        RIGHT;

        public HorizontalAnchor next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public enum VerticalAnchor {
        TOP,
        CENTER,
        BOTTOM;

        public VerticalAnchor next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public float x, y, width, height;
    private HorizontalAnchor horizontalAnchor = HorizontalAnchor.LEFT;
    private VerticalAnchor verticalAnchor = VerticalAnchor.TOP;
    private float anchorOffsetX;
    private float anchorOffsetY;

    public HudModule(String name, Category category) {
        this(name, category, 0f, 0f, 20f, 20f);
    }

    public HudModule(String name, Category category, float width, float height) {
        this(name, category, 0f, 0f, width, height);
    }

    public HudModule(String name, Category category, float x, float y, float width, float height) {
        super(name, category);

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        updateAnchorOffsets();
    }

    public void updateBounds(DeltaTracker deltaTracker) {
    }

    protected final void setBounds(float width, float height) {
        this.width = Math.max(0.0f, width);
        this.height = Math.max(0.0f, height);
        applyAnchorPosition();
    }

    public final boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public final void moveTo(float x, float y) {
        this.x = clampToScreenX(x);
        this.y = clampToScreenY(y);
        updateAnchorOffsets();
    }

    public final void moveBy(float deltaX, float deltaY) {
        moveTo(x + deltaX, y + deltaY);
    }

    public final void refreshPosition() {
        applyAnchorPosition();
    }

    public final HorizontalAnchor getHorizontalAnchor() {
        return horizontalAnchor;
    }

    public final VerticalAnchor getVerticalAnchor() {
        return verticalAnchor;
    }

    public final void setHorizontalAnchor(HorizontalAnchor horizontalAnchor) {
        if (horizontalAnchor == null) {
            return;
        }

        this.horizontalAnchor = horizontalAnchor;
        this.x = clampToScreenX(x);
        updateAnchorOffsets();
    }

    public final void setVerticalAnchor(VerticalAnchor verticalAnchor) {
        if (verticalAnchor == null) {
            return;
        }

        this.verticalAnchor = verticalAnchor;
        this.y = clampToScreenY(y);
        updateAnchorOffsets();
    }

    public final void cycleHorizontalAnchor() {
        setHorizontalAnchor(horizontalAnchor.next());
    }

    public final void cycleVerticalAnchor() {
        setVerticalAnchor(verticalAnchor.next());
    }

    public final float getAnchorOffsetX() {
        return anchorOffsetX;
    }

    public final float getAnchorOffsetY() {
        return anchorOffsetY;
    }

    public final void setAnchorState(HorizontalAnchor horizontalAnchor, VerticalAnchor verticalAnchor, float anchorOffsetX, float anchorOffsetY) {
        this.horizontalAnchor = horizontalAnchor != null ? horizontalAnchor : HorizontalAnchor.LEFT;
        this.verticalAnchor = verticalAnchor != null ? verticalAnchor : VerticalAnchor.TOP;
        this.anchorOffsetX = anchorOffsetX;
        this.anchorOffsetY = anchorOffsetY;
        applyAnchorPosition();
    }

    private void updateAnchorOffsets() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        anchorOffsetX = switch (horizontalAnchor) {
            case LEFT -> x;
            case CENTER -> x - (screenWidth - width) / 2.0f;
            case RIGHT -> x - (screenWidth - width);
        };

        anchorOffsetY = switch (verticalAnchor) {
            case TOP -> y;
            case CENTER -> y - (screenHeight - height) / 2.0f;
            case BOTTOM -> y - (screenHeight - height);
        };
    }

    private void applyAnchorPosition() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        x = switch (horizontalAnchor) {
            case LEFT -> anchorOffsetX;
            case CENTER -> (screenWidth - width) / 2.0f + anchorOffsetX;
            case RIGHT -> screenWidth - width + anchorOffsetX;
        };

        y = switch (verticalAnchor) {
            case TOP -> anchorOffsetY;
            case CENTER -> (screenHeight - height) / 2.0f + anchorOffsetY;
            case BOTTOM -> screenHeight - height + anchorOffsetY;
        };

        x = clampToScreenX(x);
        y = clampToScreenY(y);
        updateAnchorOffsets();
    }

    private float clampToScreenX(float value) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return clamp(value, 0.0f, Math.max(0.0f, screenWidth - width));
    }

    private float clampToScreenY(float value) {
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return clamp(value, 0.0f, Math.max(0.0f, screenHeight - height));
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    abstract public void render(DeltaTracker deltaTracker);

}

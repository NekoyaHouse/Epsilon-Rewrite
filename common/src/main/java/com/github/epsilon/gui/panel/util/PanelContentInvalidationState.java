package com.github.epsilon.gui.panel.util;

import com.github.epsilon.gui.panel.PanelLayout;

public final class PanelContentInvalidationState {

    private boolean dirty = true;
    private boolean hasActiveAnimations;
    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;
    private int lastGuiHeight = -1;
    private long lastSignature = Long.MIN_VALUE;
    private PanelLayout.Rect lastBounds;

    public void markDirty() {
        dirty = true;
    }

    public void beginRebuild() {
        hasActiveAnimations = false;
    }

    public void noteAnimation(boolean active) {
        hasActiveAnimations = hasActiveAnimations || active;
    }

    public boolean hasActiveAnimations() {
        return hasActiveAnimations;
    }

    public boolean needsRebuild(PanelLayout.Rect bounds, int mouseX, int mouseY, int guiHeight) {
        return needsRebuild(bounds, mouseX, mouseY, guiHeight, 0L);
    }

    public boolean needsRebuild(PanelLayout.Rect bounds, int mouseX, int mouseY, int guiHeight, long signature) {
        return dirty
                || hasActiveAnimations
                || lastBounds == null
                || !lastBounds.equals(bounds)
                || lastGuiHeight != guiHeight
                || lastMouseX != mouseX
                || lastMouseY != mouseY
                || lastSignature != signature;
    }

    public void rememberSnapshot(PanelLayout.Rect bounds, int mouseX, int mouseY, int guiHeight) {
        rememberSnapshot(bounds, mouseX, mouseY, guiHeight, 0L);
    }

    public void rememberSnapshot(PanelLayout.Rect bounds, int mouseX, int mouseY, int guiHeight, long signature) {
        lastBounds = bounds;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastGuiHeight = guiHeight;
        lastSignature = signature;
        dirty = false;
    }

}

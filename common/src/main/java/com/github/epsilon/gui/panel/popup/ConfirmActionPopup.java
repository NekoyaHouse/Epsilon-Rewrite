package com.github.epsilon.gui.panel.popup;

import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.StaticFontLoader;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.managers.RenderManager;
import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;

public final class ConfirmActionPopup implements PanelPopupHost.Popup {

    private final PanelLayout.Rect bounds;
    private final String title;
    private final String message;
    private final String detail;
    private final String confirmLabel;
    private final String cancelLabel;
    private final Runnable onConfirm;

    private final RoundRectRenderer roundRectRenderer = new RoundRectRenderer();
    private final ShadowRenderer shadowRenderer = new ShadowRenderer();
    private final TextRenderer textRenderer = new TextRenderer();
    private final Animation openAnimation = new Animation(Easing.EASE_OUT_CUBIC, 160L);
    private final Animation confirmHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation cancelHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);

    private boolean closeAfterClick;
    private float animatedY;
    private PanelLayout.Rect confirmButtonBounds;
    private PanelLayout.Rect cancelButtonBounds;

    public ConfirmActionPopup(PanelLayout.Rect bounds, String title, String message, String detail,
                              String confirmLabel, String cancelLabel, Runnable onConfirm) {
        this.bounds = bounds;
        this.title = title;
        this.message = message;
        this.detail = detail;
        this.confirmLabel = confirmLabel;
        this.cancelLabel = cancelLabel;
        this.onConfirm = onConfirm;
        this.openAnimation.setStartValue(0.0f);
        this.confirmHoverAnimation.setStartValue(0.0f);
        this.cancelHoverAnimation.setStartValue(0.0f);
        updateLayout(bounds.y());
    }

    @Override
    public PanelLayout.Rect getBounds() {
        return bounds;
    }

    @Override
    public void extractGui(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        openAnimation.run(1.0f);
        float progress = openAnimation.getValue();
        animatedY = bounds.y() - (1.0f - progress) * 6.0f;
        int alpha = (int) (245 * progress);
        updateLayout(animatedY);

        confirmHoverAnimation.run(confirmButtonBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);
        cancelHoverAnimation.run(cancelButtonBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);

        shadowRenderer.addShadow(bounds.x(), animatedY, bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS,
                POPUP_SHADOW_RADIUS, MD3Theme.withAlpha(MD3Theme.SHADOW, (int) (MD3Theme.POPUP_SHADOW_ALPHA * progress)));
        roundRectRenderer.addRoundRect(bounds.x(), animatedY, bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS,
                MD3Theme.withAlpha(MD3Theme.SURFACE_CONTAINER_LOW, alpha));

        float titleScale = 0.66f;
        float messageScale = 0.56f;
        float detailScale = 0.60f;
        float textX = bounds.x() + 12.0f;
        textRenderer.addText(title, textX, animatedY + 10.0f, titleScale, MD3Theme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);
        textRenderer.addText(message, textX, animatedY + 24.0f, messageScale, MD3Theme.TEXT_SECONDARY);
        if (detail != null && !detail.isBlank()) {
            textRenderer.addText(detail, textX, animatedY + 37.0f, detailScale, MD3Theme.PRIMARY, StaticFontLoader.DUCKSANS);
        }

        renderButton(cancelButtonBounds, cancelLabel, false, cancelHoverAnimation.getValue(), alpha);
        renderButton(confirmButtonBounds, confirmLabel, true, confirmHoverAnimation.getValue(), alpha);

        RenderManager.INSTANCE.applyRender(() -> {
            shadowRenderer.drawAndClear();
            roundRectRenderer.drawAndClear();
            textRenderer.drawAndClear();
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() != 0 || !bounds.contains(event.x(), event.y())) {
            return false;
        }
        closeAfterClick = false;
        if (confirmButtonBounds.contains(event.x(), event.y())) {
            onConfirm.run();
            closeAfterClick = true;
            return true;
        }
        if (cancelButtonBounds.contains(event.x(), event.y())) {
            closeAfterClick = true;
            return true;
        }
        return true;
    }

    @Override
    public boolean shouldCloseAfterClick() {
        return closeAfterClick;
    }

    private void renderButton(PanelLayout.Rect buttonBounds, String label, boolean destructive, float hover, int alpha) {
        Color baseColor = destructive ? MD3Theme.ERROR : MD3Theme.SURFACE_CONTAINER_HIGH;
        Color hoverColor = destructive ? MD3Theme.withAlpha(MD3Theme.ERROR, 220) : MD3Theme.SURFACE_CONTAINER_HIGHEST;
        Color textColor = destructive ? MD3Theme.ON_PRIMARY : MD3Theme.TEXT_PRIMARY;
        roundRectRenderer.addRoundRect(buttonBounds.x(), buttonBounds.y(), buttonBounds.width(), buttonBounds.height(),
                buttonBounds.height() / 2.0f,
                MD3Theme.withAlpha(MD3Theme.lerp(baseColor, hoverColor, hover * 0.35f), alpha));

        float labelScale = 0.56f;
        float labelWidth = textRenderer.getWidth(label, labelScale);
        float labelHeight = textRenderer.getHeight(labelScale);
        textRenderer.addText(label,
                buttonBounds.x() + (buttonBounds.width() - labelWidth) / 2.0f,
                buttonBounds.y() + (buttonBounds.height() - labelHeight) / 2.0f - 1.0f,
                labelScale,
                textColor);
    }

    private void updateLayout(float popupY) {
        float buttonWidth = 68.0f;
        float buttonHeight = 24.0f;
        float gap = 6.0f;
        float cancelX = bounds.right() - buttonWidth * 2.0f - gap - 12.0f;
        float confirmX = bounds.right() - buttonWidth - 12.0f;
        float buttonY = popupY + bounds.height() - buttonHeight - 10.0f;
        cancelButtonBounds = new PanelLayout.Rect(cancelX, buttonY, buttonWidth, buttonHeight);
        confirmButtonBounds = new PanelLayout.Rect(confirmX, buttonY, buttonWidth, buttonHeight);
    }
}


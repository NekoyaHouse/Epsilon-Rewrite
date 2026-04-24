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

public final class MessagePopup implements PanelPopupHost.Popup {

    private final PanelLayout.Rect bounds;
    private final String title;
    private final String message;
    private final String detail;
    private final String buttonLabel;

    private final RoundRectRenderer roundRectRenderer = new RoundRectRenderer();
    private final ShadowRenderer shadowRenderer = new ShadowRenderer();
    private final TextRenderer textRenderer = new TextRenderer();
    private final Animation openAnimation = new Animation(Easing.EASE_OUT_CUBIC, 160L);
    private final Animation buttonHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);

    private boolean closeAfterClick;
    private PanelLayout.Rect buttonBounds;

    public MessagePopup(PanelLayout.Rect bounds, String title, String message, String detail, String buttonLabel) {
        this.bounds = bounds;
        this.title = title;
        this.message = message;
        this.detail = detail;
        this.buttonLabel = buttonLabel;
        this.openAnimation.setStartValue(0.0f);
        this.buttonHoverAnimation.setStartValue(0.0f);
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
        float popupY = bounds.y() - (1.0f - progress) * 6.0f;
        updateLayout(popupY);
        buttonHoverAnimation.run(buttonBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);

        shadowRenderer.addShadow(bounds.x(), popupY, bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS,
                POPUP_SHADOW_RADIUS, MD3Theme.withAlpha(MD3Theme.SHADOW, (int) (MD3Theme.POPUP_SHADOW_ALPHA * progress)));
        roundRectRenderer.addRoundRect(bounds.x(), popupY, bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS,
                MD3Theme.withAlpha(MD3Theme.SURFACE_CONTAINER_LOW, 255));

        float titleScale = 0.66f;
        float messageScale = 0.56f;
        float detailScale = 0.52f;
        float textX = bounds.x() + 12.0f;
        textRenderer.addText(title, textX, popupY + 10.0f, titleScale, MD3Theme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);
        textRenderer.addText(message, textX, popupY + 25.0f, messageScale, MD3Theme.TEXT_SECONDARY);
        if (detail != null && !detail.isBlank()) {
            textRenderer.addText(detail, textX, popupY + 38.0f, detailScale, MD3Theme.TEXT_MUTED);
        }

        float hover = buttonHoverAnimation.getValue();
        roundRectRenderer.addRoundRect(buttonBounds.x(), buttonBounds.y(), buttonBounds.width(), buttonBounds.height(),
                buttonBounds.height() / 2.0f,
                MD3Theme.lerp(MD3Theme.PRIMARY_CONTAINER, MD3Theme.PRIMARY, hover * 0.35f));
        float labelScale = 0.56f;
        float labelWidth = textRenderer.getWidth(buttonLabel, labelScale);
        float labelHeight = textRenderer.getHeight(labelScale);
        textRenderer.addText(buttonLabel,
                buttonBounds.x() + (buttonBounds.width() - labelWidth) / 2.0f,
                buttonBounds.y() + (buttonBounds.height() - labelHeight) / 2.0f - 1.0f,
                labelScale,
                MD3Theme.ON_PRIMARY_CONTAINER);

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
        closeAfterClick = buttonBounds.contains(event.x(), event.y());
        return true;
    }

    @Override
    public boolean shouldCloseAfterClick() {
        return closeAfterClick;
    }

    private void updateLayout(float popupY) {
        float buttonWidth = 68.0f;
        float buttonHeight = 24.0f;
        buttonBounds = new PanelLayout.Rect(
                bounds.x() + bounds.width() - buttonWidth - 12.0f,
                popupY + bounds.height() - buttonHeight - 10.0f,
                buttonWidth,
                buttonHeight
        );
    }
}


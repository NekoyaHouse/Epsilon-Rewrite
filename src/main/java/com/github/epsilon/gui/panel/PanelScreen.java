package com.github.epsilon.gui.panel;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.gui.panel.input.PanelInputRouter;
import com.github.epsilon.gui.panel.panel.CategoryRailPanel;
import com.github.epsilon.gui.panel.panel.ClientSettingPanel;
import com.github.epsilon.gui.panel.panel.ModuleDetailPanel;
import com.github.epsilon.gui.panel.panel.ModuleListPanel;
import com.github.epsilon.gui.panel.popup.PanelPopupHost;
import com.github.epsilon.managers.RenderManager;
import com.github.epsilon.modules.impl.ClientSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class PanelScreen extends Screen {

    public static final PanelScreen INSTANCE = new PanelScreen();
    private static final long OPEN_DURATION_MS = 220L;
    private static final long CLOSE_DURATION_MS = 170L;
    private static final float OPEN_START_SCALE = 0.5f;
    private static final float OPEN_END_SCALE = 1.0f;
    private static final float CLOSE_END_SCALE = 1.25f;

    private final PanelState state = new PanelState();
    private final PanelDirtyState dirtyState = new PanelDirtyState();
    private final TextRenderer textRenderer = new TextRenderer();
    private final RectRenderer rectRenderer = new RectRenderer();
    private final RoundRectRenderer roundRectRenderer = new RoundRectRenderer();
    private final ShadowRenderer shadowRenderer = new ShadowRenderer();
    private final PanelPopupHost popupHost = new PanelPopupHost();
    private final PanelInputRouter inputRouter = new PanelInputRouter();
    private final CategoryRailPanel categoryRailPanel = new CategoryRailPanel(state, rectRenderer, roundRectRenderer, textRenderer);
    private final ModuleListPanel moduleListPanel = new ModuleListPanel(state, roundRectRenderer, rectRenderer, shadowRenderer, textRenderer);
    private final ModuleDetailPanel moduleDetailPanel = new ModuleDetailPanel(state, roundRectRenderer, rectRenderer, shadowRenderer, textRenderer, popupHost);
    private final ClientSettingPanel clientSettingPanel = new ClientSettingPanel(state, roundRectRenderer, rectRenderer, shadowRenderer, textRenderer, popupHost);
    private int lastWidth = -1;
    private int lastHeight = -1;
    private String lastSelectedCategory = "";
    private String lastSelectedModule = "";
    private String lastSearchQuery = "";
    private boolean lastSidebarExpanded;
    private boolean lastClientSettingMode;
    private boolean closing;
    private boolean closeCommitted;
    private long animStartMs;
    private float lastScale = OPEN_END_SCALE;
    private float closeStartScale = OPEN_END_SCALE;

    private PanelScreen() {
        super(Component.literal("PanelGui"));
    }

    @Override
    public void init() {
        super.init();
        closing = false;
        closeCommitted = false;
        animStartMs = System.currentTimeMillis();
        lastScale = OPEN_START_SCALE;
        closeStartScale = OPEN_END_SCALE;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        if (state.updateScrollAnimations()) {
            dirtyState.markAllDirty();
        }

        String currentCategory = state.getSelectedCategory().name();
        String currentModule = state.getSelectedModule() == null ? "" : state.getSelectedModule().getName();
        String currentQuery = state.getSearchQuery();
        boolean sidebarExpanded = state.isSidebarExpanded();
        boolean clientSettingMode = state.isClientSettingMode();
        if (!lastSelectedCategory.equals(currentCategory)
                || !lastSelectedModule.equals(currentModule)
                || !lastSearchQuery.equals(currentQuery)
                || lastSidebarExpanded != sidebarExpanded
                || lastClientSettingMode != clientSettingMode) {
            dirtyState.markAllDirty();
            lastSelectedCategory = currentCategory;
            lastSelectedModule = currentModule;
            lastSearchQuery = currentQuery;
            lastSidebarExpanded = sidebarExpanded;
            lastClientSettingMode = clientSettingMode;
        }

        if (categoryRailPanel.hasActiveAnimations()
                || moduleListPanel.hasActiveAnimations()
                || moduleDetailPanel.hasActiveAnimations()
                || clientSettingPanel.hasActiveAnimations()
                || state.hasActiveScrollAnimations()) {
            dirtyState.markAllDirty();
        }

        if (width != lastWidth || height != lastHeight) {
            dirtyState.markLayoutDirty();
            lastWidth = width;
            lastHeight = height;
        }

        if (dirtyState.consumeModuleListDirty()) {
            moduleListPanel.markDirty();
        }
        if (dirtyState.consumeDetailDirty()) {
            moduleDetailPanel.markDirty();
        }
        if (dirtyState.consumeClientSettingDirty()) {
            clientSettingPanel.markDirty();
        }

        float animProgress = getAnimProgress();
        float openScale;
        float openAlpha;
        if (closing) {
            float scaleEased = easeOutCubic(animProgress);
            float fadeEased = easeOutCubic(animProgress);
            openAlpha = 1.0f - fadeEased;
            openScale = lerp(closeStartScale, CLOSE_END_SCALE, scaleEased);
        } else {
            float eased = easeOutCubic(animProgress);
            openAlpha = eased;
            openScale = lerp(OPEN_START_SCALE, OPEN_END_SCALE, eased);
        }
        lastScale = openScale;
        openAlpha = Math.max(0.0f, Math.min(1.0f, openAlpha));

        shadowRenderer.setGlobalAlpha(openAlpha);
        roundRectRenderer.setGlobalAlpha(openAlpha);
        rectRenderer.setGlobalAlpha(openAlpha);
        textRenderer.setGlobalAlpha(openAlpha);
        categoryRailPanel.setGlobalAlpha(openAlpha);
        moduleListPanel.setGlobalAlpha(openAlpha);
        moduleDetailPanel.setGlobalAlpha(openAlpha);
        clientSettingPanel.setGlobalAlpha(openAlpha);

        if (animProgress < 1.0f) dirtyState.markAllDirty();
        if (closing && animProgress >= 1.0f) {
            commitClose();
            return;
        }

        MD3Theme.syncFromSettings();
        float railWidth = categoryRailPanel.getAnimatedWidth();
        PanelLayout.Layout layout = scaleLayout(PanelLayout.compute(width, height, railWidth), openScale);

        drawChrome(layout);
        categoryRailPanel.render(GuiGraphicsExtractor, layout.rail(), mouseX, mouseY, partialTick);
        if (state.isClientSettingMode()) {
            PanelLayout.Rect clientSettingsBounds = new PanelLayout.Rect(
                    layout.modules().x(), layout.modules().y(),
                    layout.detail().right() - layout.modules().x(),
                    layout.modules().height()
            );
            clientSettingPanel.render(GuiGraphicsExtractor, clientSettingsBounds, mouseX, mouseY, partialTick);
        } else {
            moduleListPanel.render(GuiGraphicsExtractor, layout.modules(), mouseX, mouseY, partialTick);
            moduleDetailPanel.render(GuiGraphicsExtractor, layout.detail(), mouseX, mouseY, partialTick);
        }

        RenderManager.INSTANCE.applyRenderAfterFrame(this::flushQueuedRenderers);

        popupHost.render(GuiGraphicsExtractor, mouseX, mouseY, partialTick);
    }

    private void drawChrome(PanelLayout.Layout layout) {
        shadowRenderer.addShadow(layout.panel().x(), layout.panel().y(), layout.panel().width(), layout.panel().height(), MD3Theme.PANEL_RADIUS, 18.0f, MD3Theme.SHADOW);
        roundRectRenderer.addRoundRect(layout.panel().x(), layout.panel().y(), layout.panel().width(), layout.panel().height(), MD3Theme.PANEL_RADIUS, MD3Theme.SURFACE);

        roundRectRenderer.addRoundRect(layout.rail().x(), layout.rail().y(), layout.rail().width(), layout.rail().height(), MD3Theme.SECTION_RADIUS, MD3Theme.SURFACE_DIM);
        if (state.isClientSettingMode()) {
            float csX = layout.modules().x();
            float csY = layout.modules().y();
            float csW = layout.detail().right() - layout.modules().x();
            float csH = layout.modules().height();
            roundRectRenderer.addRoundRect(csX, csY, csW, csH, MD3Theme.SECTION_RADIUS, MD3Theme.SURFACE_DIM);
        } else {
            roundRectRenderer.addRoundRect(layout.modules().x(), layout.modules().y(), layout.modules().width(), layout.modules().height(), MD3Theme.SECTION_RADIUS, MD3Theme.SURFACE_DIM);
            roundRectRenderer.addRoundRect(layout.detail().x(), layout.detail().y(), layout.detail().width(), layout.detail().height(), MD3Theme.SECTION_RADIUS, MD3Theme.SURFACE_DIM);
        }
    }

    private static PanelLayout.Layout scaleLayout(PanelLayout.Layout layout, float scale) {
        if (Math.abs(scale - 1.0f) <= 0.0001f) return layout;
        float cx = layout.panel().x() + layout.panel().width() / 2;
        float cy = layout.panel().y() + layout.panel().height() / 2;
        return new PanelLayout.Layout(
                scaleRect(layout.panel(), cx, cy, scale),
                scaleRect(layout.rail(), cx, cy, scale),
                scaleRect(layout.modules(), cx, cy, scale),
                scaleRect(layout.detail(), cx, cy, scale)
        );
    }

    private static PanelLayout.Rect scaleRect(PanelLayout.Rect rect, float cx, float cy, float scale) {
        return new PanelLayout.Rect(
                cx + (rect.x() - cx) * scale,
                cy + (rect.y() - cy) * scale,
                rect.width() * scale,
                rect.height() * scale
        );
    }

    private void flushQueuedRenderers() {
        shadowRenderer.drawAndClear();
        roundRectRenderer.drawAndClear();
        rectRenderer.drawAndClear();
        textRenderer.drawAndClear();
        if (state.isClientSettingMode()) {
            clientSettingPanel.flushContent();
        } else {
            moduleListPanel.flushContent();
            moduleDetailPanel.flushContent();
        }
        categoryRailPanel.flushClippedText();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (closing) {
            return true;
        }
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() != 0) {
            return super.mouseClicked(event, isDoubleClick);
        }

        if (popupHost.getActivePopup() != null) {
            return inputRouter.routeMouseClicked(event, isDoubleClick, popupHost, moduleDetailPanel, moduleListPanel, categoryRailPanel, clientSettingPanel, state.isClientSettingMode())
                    || super.mouseClicked(event, isDoubleClick);
        }

        PanelLayout.Layout layout = PanelLayout.compute(width, height, categoryRailPanel.getAnimatedWidth());
        if (!layout.panel().contains(mouseX, mouseY)) {
            if (ClientSetting.INSTANCE.closeOnOutside.getValue()) {
                requestCloseAnimation();
            }
            return true;
        }
        if (!state.isClientSettingMode()) {
            moduleListPanel.handleGlobalClick(mouseX, mouseY);
        }
        boolean handled = inputRouter.routeMouseClicked(event, isDoubleClick, popupHost, moduleDetailPanel, moduleListPanel, categoryRailPanel, clientSettingPanel, state.isClientSettingMode());
        if (handled) {
            dirtyState.markAllDirty();
        }
        return handled || super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (closing) {
            return true;
        }
        if (popupHost.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            dirtyState.markAllDirty();
            return true;
        }
        if (state.isClientSettingMode()) {
            if (clientSettingPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                dirtyState.markClientSettingDirty();
                return true;
            }
        } else {
            if (moduleListPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                dirtyState.markModuleListDirty();
                return true;
            }
            if (moduleDetailPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                dirtyState.markDetailDirty();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (closing) {
            return true;
        }
        if (inputRouter.routeMouseReleased(event, popupHost, moduleDetailPanel, moduleListPanel, clientSettingPanel, state.isClientSettingMode())) {
            dirtyState.markAllDirty();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (closing) {
            return true;
        }
        if (inputRouter.routeMouseDragged(event, mouseX, mouseY, popupHost, moduleDetailPanel, moduleListPanel, clientSettingPanel, state.isClientSettingMode())) {
            dirtyState.markAllDirty();
            return true;
        }
        return super.mouseDragged(event, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            requestCloseAnimation();
            return true;
        }
        if (closing) {
            return true;
        }
        if (inputRouter.routeKeyPressed(event, popupHost, moduleDetailPanel, moduleListPanel, clientSettingPanel, state.isClientSettingMode())) {
            dirtyState.markAllDirty();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (closing) {
            return true;
        }
        if (inputRouter.routeCharTyped(event, popupHost, moduleDetailPanel, moduleListPanel, clientSettingPanel, state.isClientSettingMode())) {
            dirtyState.markAllDirty();
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        if (closeCommitted) {
            super.onClose();
            return;
        }
        requestCloseAnimation();
    }

    private void requestCloseAnimation() {
        if (closing) {
            return;
        }
        closing = true;
        closeStartScale = lastScale;
        animStartMs = System.currentTimeMillis();
        dirtyState.markAllDirty();
    }

    private void commitClose() {
        if (closeCommitted) {
            return;
        }
        closeCommitted = true;
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private float getAnimProgress() {
        long duration = closing ? CLOSE_DURATION_MS : OPEN_DURATION_MS;
        if (duration <= 0L) {
            return 1.0f;
        }
        long elapsed = System.currentTimeMillis() - animStartMs;
        float t = elapsed / (float) duration;
        return Math.max(0.0f, Math.min(1.0f, t));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float easeOutCubic(float t) {
        float p = 1.0f - t;
        return 1.0f - (p * p * p);
    }

}

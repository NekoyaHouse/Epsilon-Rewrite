package com.github.epsilon.gui.panel.panel.clientsettings;

import com.github.epsilon.Epsilon;
import com.github.epsilon.assets.i18n.EpsilonTranslateComponent;
import com.github.epsilon.assets.i18n.TranslateComponent;
import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.PanelState;
import com.github.epsilon.gui.panel.popup.ConfirmActionPopup;
import com.github.epsilon.gui.panel.popup.PanelPopupHost;
import com.github.epsilon.gui.panel.util.PanelContentBuffer;
import com.github.epsilon.gui.panel.util.PanelContentInvalidationState;
import com.github.epsilon.gui.panel.util.ScrollBarDragState;
import com.github.epsilon.gui.panel.util.ScrollBarUtil;
import com.github.epsilon.managers.ConfigManager;
import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ConfigClientSettingTab implements ClientSettingTabView {

    private static final TranslateComponent configNamePlaceholderComponent = EpsilonTranslateComponent.create("gui", "config.name.placeholder");
    private static final TranslateComponent zipPlaceholderComponent = EpsilonTranslateComponent.create("gui", "config.zip.placeholder");
    private static final TranslateComponent currentComponent = EpsilonTranslateComponent.create("gui", "config.current");
    private static final TranslateComponent emptyComponent = EpsilonTranslateComponent.create("gui", "config.empty");
    private static final TranslateComponent saveAsComponent = EpsilonTranslateComponent.create("gui", "config.action.saveas");
    private static final TranslateComponent reloadComponent = EpsilonTranslateComponent.create("gui", "config.action.reload");
    private static final TranslateComponent exportComponent = EpsilonTranslateComponent.create("gui", "config.action.export");
    private static final TranslateComponent importComponent = EpsilonTranslateComponent.create("gui", "config.action.import");
    private static final TranslateComponent deleteConfirmTitleComponent = EpsilonTranslateComponent.create("gui", "config.delete.confirm.title");
    private static final TranslateComponent deleteConfirmMessageComponent = EpsilonTranslateComponent.create("gui", "config.delete.confirm.message");
    private static final TranslateComponent deleteConfirmConfirmComponent = EpsilonTranslateComponent.create("gui", "config.delete.confirm.confirm");
    private static final TranslateComponent deleteConfirmCancelComponent = EpsilonTranslateComponent.create("gui", "config.delete.confirm.cancel");
    private static final float ROW_HEIGHT = 36.0f;
    private static final float FIELD_HEIGHT = 28.0f;
    private static final float BUTTON_HEIGHT = 26.0f;
    private static final float SECTION_GAP = 6.0f;
    private static final float FIELD_SCALE = 0.78f;
    private static final int MAX_CONFIG_NAME_LENGTH = 48;
    private static final int MAX_ZIP_PATH_LENGTH = 200;

    private final PanelState state;
    private final PanelPopupHost popupHost;
    private final RoundRectRenderer roundRectRenderer;
    private final RectRenderer rectRenderer;
    private final TextRenderer textRenderer;
    private final PanelContentBuffer contentBuffer = new PanelContentBuffer();
    private final PanelContentInvalidationState contentState = new PanelContentInvalidationState();
    private final ScrollBarDragState scrollBarDrag = new ScrollBarDragState();
    private final ClientSettingTextField nameField = new ClientSettingTextField(MAX_CONFIG_NAME_LENGTH);
    private final ClientSettingTextField zipField = new ClientSettingTextField(MAX_ZIP_PATH_LENGTH);
    private final Map<String, Animation> rowHoverAnimations = new HashMap<>();
    private final Map<String, Animation> deleteHoverAnimations = new HashMap<>();
    private final Map<String, Animation> buttonHoverAnimations = new HashMap<>();
    private final List<ConfigRowEntry> rowEntries = new ArrayList<>();

    private PanelLayout.Rect bounds;
    private int guiHeight;
    private float lastScroll = Float.NaN;
    private List<String> lastConfigList = List.of();
    private String lastActiveConfig = "";

    public ConfigClientSettingTab(PanelState state, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, PanelPopupHost popupHost) {
        this.state = state;
        this.popupHost = popupHost;
        this.roundRectRenderer = roundRectRenderer;
        this.rectRenderer = rectRenderer;
        this.textRenderer = textRenderer;
    }

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, PanelLayout.Rect bounds, int mouseX, int mouseY, float partialTick) {
        this.bounds = bounds;
        this.guiHeight = guiGraphics.guiHeight();

        List<String> configs = ConfigManager.INSTANCE.listConfigs();
        String activeConfig = ConfigManager.INSTANCE.getActiveConfigName();

        renderInputs(getInputSectionBounds(bounds), mouseX, mouseY);

        PanelLayout.Rect listViewport = getListViewport(bounds);
        float contentHeight = configs.size() * (ROW_HEIGHT + MD3Theme.ROW_GAP);
        state.setMaxConfigScroll(contentHeight - listViewport.height());
        float maxScroll = Math.max(0.0f, contentHeight - listViewport.height());
        boolean hasScrollBar = maxScroll > 0.0f;
        float rowWidth = hasScrollBar ? listViewport.width() - ScrollBarUtil.TOTAL_WIDTH : listViewport.width();

        if (shouldRebuild(listViewport, mouseX, mouseY, configs, activeConfig, guiGraphics.guiHeight())) {
            contentBuffer.clear();
            contentState.beginRebuild();
            rowEntries.clear();
            rowHoverAnimations.keySet().removeIf(name -> !configs.contains(name));
            deleteHoverAnimations.keySet().removeIf(name -> !configs.contains(name));

            float rowY = listViewport.y() - state.getConfigScroll();
            for (String configName : configs) {
                PanelLayout.Rect rowBounds = new PanelLayout.Rect(listViewport.x(), rowY, rowWidth, ROW_HEIGHT);
                PanelLayout.Rect deleteBounds = getDeleteButtonBounds(rowBounds);
                rowEntries.add(new ConfigRowEntry(configName, rowBounds, deleteBounds));

                Animation rowHover = rowHoverAnimations.computeIfAbsent(configName, ignored -> createAnimation());
                Animation deleteHover = deleteHoverAnimations.computeIfAbsent(configName, ignored -> createAnimation());
                rowHover.run(rowBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);
                deleteHover.run(deleteBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);
                contentState.noteAnimation(!rowHover.isFinished() || !deleteHover.isFinished());

                renderConfigRow(configName, activeConfig, rowBounds, deleteBounds, rowHover.getValue(), deleteHover.getValue());
                rowY += ROW_HEIGHT + MD3Theme.ROW_GAP;
            }

            if (configs.isEmpty()) {
                float hintScale = 0.58f;
                String hint = emptyComponent.getTranslatedName();
                float hintWidth = contentBuffer.textRenderer().getWidth(hint, hintScale);
                float hintX = listViewport.x() + (listViewport.width() - hintWidth) / 2.0f;
                float hintY = listViewport.y() + listViewport.height() / 2.0f - contentBuffer.textRenderer().getHeight(hintScale) / 2.0f;
                contentBuffer.textRenderer().addText(hint, hintX, hintY, hintScale, MD3Theme.TEXT_MUTED);
            }

            rememberSnapshot(listViewport, mouseX, mouseY, configs, activeConfig, guiGraphics.guiHeight());
        }

        contentBuffer.queueViewport(listViewport, guiHeight, state.getConfigScroll(), maxScroll, contentHeight);
    }

    @Override
    public void flushContent() {
        contentBuffer.flush();
    }

    @Override
    public void markDirty() {
        contentState.markDirty();
    }

    @Override
    public boolean hasActiveAnimations() {
        boolean rowAnimations = rowHoverAnimations.values().stream().anyMatch(animation -> !animation.isFinished())
                || deleteHoverAnimations.values().stream().anyMatch(animation -> !animation.isFinished());
        boolean buttonAnimations = buttonHoverAnimations.values().stream().anyMatch(animation -> !animation.isFinished());
        return contentState.hasActiveAnimations()
                || rowAnimations
                || buttonAnimations
                || nameField.hasActiveAnimations()
                || zipField.hasActiveAnimations();
    }

    @Override
    public boolean consumesHover(int mouseX, int mouseY) {
        return popupHost.getActivePopup() != null && popupHost.getActivePopup().getBounds().contains(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (bounds == null || event.button() != 0) {
            return false;
        }

        PanelLayout.Rect listViewport = getListViewport(bounds);
        float maxScroll = state.getMaxConfigScroll();
        if (scrollBarDrag.mouseClicked(event.x(), event.y(), listViewport, state.getConfigScroll(), maxScroll)) {
            float newScroll = scrollBarDrag.mouseDragged(event.y(), listViewport, maxScroll);
            if (newScroll >= 0.0f) {
                state.setConfigScroll(newScroll);
            }
            markDirty();
            return true;
        }

        PanelLayout.Rect inputSection = getInputSectionBounds(bounds);
        PanelLayout.Rect nameBounds = getNameFieldBounds(inputSection);
        PanelLayout.Rect zipBounds = getZipFieldBounds(inputSection);
        if (nameBounds.contains(event.x(), event.y())) {
            zipField.blur();
            nameField.focusIfContains(nameBounds, event.x(), event.y());
            markDirty();
            return true;
        }
        if (zipBounds.contains(event.x(), event.y())) {
            nameField.blur();
            zipField.focusIfContains(zipBounds, event.x(), event.y());
            markDirty();
            return true;
        }

        nameField.blur();
        zipField.blur();

        for (ActionButton button : getActionButtons(inputSection)) {
            if (button.bounds().contains(event.x(), event.y())) {
                handleAction(button.type());
                markDirty();
                return true;
            }
        }

        for (ConfigRowEntry entry : rowEntries) {
            if (entry.deleteBounds().contains(event.x(), event.y())) {
                openDeleteConfirmation(entry.name());
                markDirty();
                return true;
            }
            if (entry.rowBounds().contains(event.x(), event.y())) {
                trySwitchConfig(entry.name());
                markDirty();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (scrollBarDrag.mouseReleased()) {
            markDirty();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (!scrollBarDrag.isDragging()) {
            return false;
        }
        PanelLayout.Rect listViewport = getListViewport(bounds);
        float newScroll = scrollBarDrag.mouseDragged(event.y(), listViewport, state.getMaxConfigScroll());
        if (newScroll >= 0.0f) {
            state.setConfigScroll(newScroll);
        }
        markDirty();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (bounds == null) {
            return false;
        }
        PanelLayout.Rect listViewport = getListViewport(bounds);
        if (listViewport.contains(mouseX, mouseY)) {
            state.scrollConfig(-scrollY * 20.0f);
            markDirty();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && (nameField.isFocused() || zipField.isFocused())) {
            nameField.blur();
            zipField.blur();
            markDirty();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (nameField.isFocused()) {
                handleAction(ActionButtonType.SAVE_AS);
                markDirty();
                return true;
            }
            if (zipField.isFocused()) {
                handleAction(ActionButtonType.IMPORT);
                markDirty();
                return true;
            }
        }
        if (nameField.keyPressed(event) || zipField.keyPressed(event)) {
            markDirty();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (nameField.charTyped(event) || zipField.charTyped(event)) {
            markDirty();
            return true;
        }
        return false;
    }

    @Override
    public void onActivated() {
        if (nameField.getText().isBlank()) {
            nameField.setText(ConfigManager.INSTANCE.getActiveConfigName());
            nameField.setCursorToEnd();
        }
        markDirty();
    }

    @Override
    public void onDeactivated() {
        scrollBarDrag.reset();
        nameField.blur();
        zipField.blur();
        markDirty();
    }

    private void renderInputs(PanelLayout.Rect inputBounds, int mouseX, int mouseY) {
        nameField.render(getNameFieldBounds(inputBounds), mouseX, mouseY, roundRectRenderer, rectRenderer, textRenderer,
                configNamePlaceholderComponent.getTranslatedName(), FIELD_SCALE, null);
        zipField.render(getZipFieldBounds(inputBounds), mouseX, mouseY, roundRectRenderer, rectRenderer, textRenderer,
                zipPlaceholderComponent.getTranslatedName(), FIELD_SCALE, "↵");

        for (ActionButton button : getActionButtons(inputBounds)) {
            renderActionButton(button, mouseX, mouseY);
        }

    }

    private void renderActionButton(ActionButton button, int mouseX, int mouseY) {
        Animation hoverAnimation = buttonHoverAnimations.computeIfAbsent(button.type().name(), ignored -> createAnimation());
        boolean hovered = button.bounds().contains(mouseX, mouseY);
        hoverAnimation.run(hovered ? 1.0f : 0.0f);

        float hover = hoverAnimation.getValue();
        Color baseColor = switch (button.type()) {
            case SAVE_AS -> MD3Theme.PRIMARY_CONTAINER;
            case RELOAD -> MD3Theme.SECONDARY_CONTAINER;
            case EXPORT, IMPORT -> MD3Theme.SURFACE_CONTAINER_HIGH;
        };
        Color hoverColor = switch (button.type()) {
            case SAVE_AS -> MD3Theme.PRIMARY;
            case RELOAD -> MD3Theme.SECONDARY;
            case EXPORT, IMPORT -> MD3Theme.SURFACE_CONTAINER_HIGHEST;
        };
        Color textColor = switch (button.type()) {
            case SAVE_AS -> MD3Theme.ON_PRIMARY_CONTAINER;
            case RELOAD -> MD3Theme.ON_SECONDARY_CONTAINER;
            case EXPORT, IMPORT -> MD3Theme.TEXT_PRIMARY;
        };

        roundRectRenderer.addRoundRect(button.bounds().x(), button.bounds().y(), button.bounds().width(), button.bounds().height(),
                button.bounds().height() / 2.0f, MD3Theme.lerp(baseColor, hoverColor, hover * 0.35f));

        float labelScale = 0.56f;
        float labelWidth = textRenderer.getWidth(button.label(), labelScale);
        float labelHeight = textRenderer.getHeight(labelScale);
        textRenderer.addText(button.label(),
                button.bounds().x() + (button.bounds().width() - labelWidth) / 2.0f,
                button.bounds().y() + (button.bounds().height() - labelHeight) / 2.0f - 1.0f,
                labelScale,
                textColor);
    }

    private void renderConfigRow(String configName, String activeConfig, PanelLayout.Rect rowBounds, PanelLayout.Rect deleteBounds, float hover, float deleteHover) {
        boolean active = Objects.equals(configName, activeConfig);
        RoundRectRenderer rowRenderer = contentBuffer.roundRectRenderer();
        TextRenderer rowTextRenderer = contentBuffer.textRenderer();

        Color baseColor = MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER, MD3Theme.SURFACE_CONTAINER_HIGH, hover);
        Color rowColor = active ? MD3Theme.lerp(baseColor, MD3Theme.PRIMARY_CONTAINER, 0.28f) : baseColor;
        rowRenderer.addRoundRect(rowBounds.x(), rowBounds.y(), rowBounds.width(), rowBounds.height(), MD3Theme.CARD_RADIUS, rowColor);

        float nameScale = 0.66f;
        float subScale = 0.52f;
        float textX = rowBounds.x() + MD3Theme.ROW_CONTENT_INSET + 1.0f;
        float nameY = rowBounds.y() + 7.0f;
        rowTextRenderer.addText(trimToWidth(configName, nameScale, rowBounds.width() - 72.0f), textX, nameY, nameScale,
                active ? MD3Theme.ON_PRIMARY_CONTAINER : MD3Theme.TEXT_PRIMARY);

        String subtitle = active ? currentComponent.getTranslatedName() : "Click to switch";
        Color subtitleColor = active ? MD3Theme.ON_PRIMARY_CONTAINER : MD3Theme.TEXT_MUTED;
        rowTextRenderer.addText(subtitle, textX, nameY + 12.0f, subScale, subtitleColor);

        if (active) {
            String chipText = currentComponent.getTranslatedName();
            float chipScale = 0.48f;
            float chipWidth = rowTextRenderer.getWidth(chipText, chipScale) + 10.0f;
            float chipHeight = 14.0f;
            float chipX = deleteBounds.x() - chipWidth - 6.0f;
            float chipY = rowBounds.y() + (rowBounds.height() - chipHeight) / 2.0f;
            rowRenderer.addRoundRect(chipX, chipY, chipWidth, chipHeight, chipHeight / 2.0f, MD3Theme.PRIMARY);
            rowTextRenderer.addText(chipText,
                    chipX + (chipWidth - rowTextRenderer.getWidth(chipText, chipScale)) / 2.0f,
                    chipY + (chipHeight - rowTextRenderer.getHeight(chipScale)) / 2.0f - 1.0f,
                    chipScale,
                    MD3Theme.ON_PRIMARY);
        }

        rowRenderer.addRoundRect(deleteBounds.x(), deleteBounds.y(), deleteBounds.width(), deleteBounds.height(),
                deleteBounds.height() / 2.0f,
                MD3Theme.lerp(MD3Theme.withAlpha(MD3Theme.ERROR, 0), MD3Theme.withAlpha(MD3Theme.ERROR, 32), deleteHover));
        float removeScale = 0.50f;
        String removeIcon = "✕";
        rowTextRenderer.addText(removeIcon,
                deleteBounds.x() + (deleteBounds.width() - rowTextRenderer.getWidth(removeIcon, removeScale)) / 2.0f,
                deleteBounds.y() + (deleteBounds.height() - rowTextRenderer.getHeight(removeScale)) / 2.0f - 1.0f,
                removeScale,
                MD3Theme.lerp(MD3Theme.TEXT_MUTED, MD3Theme.ERROR, deleteHover));
    }

    private void handleAction(ActionButtonType action) {
        switch (action) {
            case SAVE_AS -> trySaveAs();
            case RELOAD -> {
                ConfigManager.INSTANCE.reload();
                markDirty();
            }
            case EXPORT -> tryExport();
            case IMPORT -> tryImport();
        }
    }

    private void trySaveAs() {
        String targetName = nameField.getText().trim();
        if (targetName.isEmpty()) {
            return;
        }
        try {
            String savedName = ConfigManager.INSTANCE.saveAsConfig(targetName);
            nameField.setText(savedName);
            nameField.setCursorToEnd();
            state.setConfigScroll(0.0f);
        } catch (Exception exception) {
            Epsilon.LOGGER.error("保存配置失败", exception);
        }
    }

    private void tryExport() {
        try {
            Path exported = ConfigManager.INSTANCE.exportActiveConfigToZip(zipField.getText());
            zipField.setText(exported.toString());
            zipField.setCursorToEnd();
        } catch (Exception exception) {
            Epsilon.LOGGER.error("导出配置失败", exception);
        }
    }

    private void tryImport() {
        String zipPath = zipField.getText().trim();
        if (zipPath.isEmpty()) {
            return;
        }
        try {
            String importedName = ConfigManager.INSTANCE.importConfigFromZip(zipPath);
            nameField.setText(importedName);
            nameField.setCursorToEnd();
            state.setConfigScroll(0.0f);
        } catch (Exception exception) {
            Epsilon.LOGGER.error("导入配置失败", exception);
        }
    }

    private void trySwitchConfig(String configName) {
        if (Objects.equals(configName, ConfigManager.INSTANCE.getActiveConfigName())) {
            return;
        }
        try {
            ConfigManager.INSTANCE.switchConfig(configName);
            nameField.setText(configName);
            nameField.setCursorToEnd();
        } catch (Exception exception) {
            Epsilon.LOGGER.error("切换配置失败", exception);
        }
    }

    private void tryDeleteConfig(String configName) {
        try {
            if (!ConfigManager.INSTANCE.deleteConfig(configName)) {
                return;
            }
            if (Objects.equals(nameField.getText().trim(), configName)) {
                nameField.setText(ConfigManager.INSTANCE.getActiveConfigName());
                nameField.setCursorToEnd();
            }
        } catch (Exception exception) {
            Epsilon.LOGGER.error("删除配置失败", exception);
        }
    }

    private void openDeleteConfirmation(String configName) {
        if (bounds == null) {
            return;
        }
        float popupWidth = 198.0f;
        float popupHeight = 82.0f;
        PanelLayout.Rect popupBounds = new PanelLayout.Rect(
                bounds.x() + (bounds.width() - popupWidth) / 2.0f,
                bounds.y() + (bounds.height() - popupHeight) / 2.0f,
                popupWidth,
                popupHeight
        );
        popupHost.open(new ConfirmActionPopup(
                popupBounds,
                deleteConfirmTitleComponent.getTranslatedName(),
                deleteConfirmMessageComponent.getTranslatedName(),
                trimToWidth(configName, 0.60f, popupWidth - 24.0f),
                deleteConfirmConfirmComponent.getTranslatedName(),
                deleteConfirmCancelComponent.getTranslatedName(),
                () -> {
                    tryDeleteConfig(configName);
                    markDirty();
                }
        ));
    }

    private boolean shouldRebuild(PanelLayout.Rect listViewport, int mouseX, int mouseY, List<String> configs, String activeConfig, int currentGuiHeight) {
        if (contentState.needsRebuild(listViewport, mouseX, mouseY, currentGuiHeight)) {
            return true;
        }
        if (Float.compare(lastScroll, state.getConfigScroll()) != 0) {
            return true;
        }
        if (!Objects.equals(lastConfigList, configs)) {
            return true;
        }
        return !Objects.equals(lastActiveConfig, activeConfig);
    }

    private void rememberSnapshot(PanelLayout.Rect listViewport, int mouseX, int mouseY, List<String> configs, String activeConfig, int currentGuiHeight) {
        contentState.rememberSnapshot(listViewport, mouseX, mouseY, currentGuiHeight);
        lastScroll = state.getConfigScroll();
        lastConfigList = new ArrayList<>(configs);
        lastActiveConfig = activeConfig;
    }

    private PanelLayout.Rect getInputSectionBounds(PanelLayout.Rect bounds) {
        float inputHeight = FIELD_HEIGHT * 2.0f + BUTTON_HEIGHT + 3.0f * SECTION_GAP;
        return new PanelLayout.Rect(bounds.x(), bounds.bottom() - inputHeight, bounds.width(), inputHeight);
    }

    private PanelLayout.Rect getListViewport(PanelLayout.Rect bounds) {
        PanelLayout.Rect inputBounds = getInputSectionBounds(bounds);
        float y = bounds.y();
        float bottom = inputBounds.y() - SECTION_GAP;
        return new PanelLayout.Rect(bounds.x(), y, bounds.width(), Math.max(0.0f, bottom - y));
    }

    private PanelLayout.Rect getNameFieldBounds(PanelLayout.Rect inputBounds) {
        return new PanelLayout.Rect(inputBounds.x(), inputBounds.y(), inputBounds.width(), FIELD_HEIGHT);
    }

    private PanelLayout.Rect getZipFieldBounds(PanelLayout.Rect inputBounds) {
        return new PanelLayout.Rect(inputBounds.x(), inputBounds.y() + FIELD_HEIGHT + SECTION_GAP, inputBounds.width(), FIELD_HEIGHT);
    }

    private List<ActionButton> getActionButtons(PanelLayout.Rect inputBounds) {
        float y = getZipFieldBounds(inputBounds).bottom() + SECTION_GAP;
        float gap = 4.0f;
        float width = (inputBounds.width() - gap * 3.0f) / 4.0f;
        return List.of(
                new ActionButton(ActionButtonType.SAVE_AS, saveAsComponent.getTranslatedName(), new PanelLayout.Rect(inputBounds.x(), y, width, BUTTON_HEIGHT)),
                new ActionButton(ActionButtonType.RELOAD, reloadComponent.getTranslatedName(), new PanelLayout.Rect(inputBounds.x() + width + gap, y, width, BUTTON_HEIGHT)),
                new ActionButton(ActionButtonType.EXPORT, exportComponent.getTranslatedName(), new PanelLayout.Rect(inputBounds.x() + (width + gap) * 2.0f, y, width, BUTTON_HEIGHT)),
                new ActionButton(ActionButtonType.IMPORT, importComponent.getTranslatedName(), new PanelLayout.Rect(inputBounds.x() + (width + gap) * 3.0f, y, width, BUTTON_HEIGHT))
        );
    }

    private PanelLayout.Rect getDeleteButtonBounds(PanelLayout.Rect rowBounds) {
        float buttonSize = 20.0f;
        return new PanelLayout.Rect(
                rowBounds.right() - MD3Theme.ROW_TRAILING_INSET - buttonSize,
                rowBounds.y() + (rowBounds.height() - buttonSize) / 2.0f,
                buttonSize,
                buttonSize
        );
    }

    private Animation createAnimation() {
        Animation animation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
        animation.setStartValue(0.0f);
        return animation;
    }

    private String trimToWidth(String value, float scale, float width) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (textRenderer.getWidth(value, scale) <= width) {
            return value;
        }
        String ellipsis = "...";
        float ellipsisWidth = textRenderer.getWidth(ellipsis, scale);
        if (ellipsisWidth >= width) {
            return ellipsis;
        }
        for (int length = value.length() - 1; length >= 0; length--) {
            String candidate = value.substring(0, length) + ellipsis;
            if (textRenderer.getWidth(candidate, scale) <= width) {
                return candidate;
            }
        }
        return ellipsis;
    }


    private record ConfigRowEntry(String name, PanelLayout.Rect rowBounds, PanelLayout.Rect deleteBounds) {
    }

    private record ActionButton(ActionButtonType type, String label, PanelLayout.Rect bounds) {
    }

    private enum ActionButtonType {
        SAVE_AS,
        RELOAD,
        EXPORT,
        IMPORT
    }
}



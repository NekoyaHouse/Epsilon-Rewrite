package com.github.epsilon.assets.i18n;

import com.github.epsilon.assets.holders.TranslateHolder;
import net.minecraft.client.resources.language.I18n;

public class DefaultTranslateComponent implements TranslateComponent {

    private final String fullKey;
    private String cachedName;

    private DefaultTranslateComponent(String fullKey) {
        this.fullKey = fullKey;
    }

    public static DefaultTranslateComponent create(String fullKey) {
        DefaultTranslateComponent component = new DefaultTranslateComponent(fullKey);
        TranslateHolder.INSTANCE.registerTranslateComponent(component);
        return component;
    }

    @Override
    public String getFullKey() {
        return fullKey;
    }

    @Override
    public String getTranslatedName() {
        if (cachedName == null) {
            cachedName = I18n.get(fullKey);
        }
        return cachedName;
    }

    @Override
    public void refresh() {
        cachedName = I18n.get(fullKey);
    }

    @Override
    public TranslateComponent createChild(String suffix) {
        return DefaultTranslateComponent.create(fullKey + "." + suffix);
    }

}


package com.github.lumin.assets.holders;

import com.github.lumin.assets.i18n.TranslateComponent;

import java.util.ArrayList;
import java.util.List;

public class TranslateHolder {

    public static TranslateHolder INSTANCE = new TranslateHolder();

    private final List<TranslateComponent> components = new ArrayList<>();

    private TranslateHolder() {
    }

    public void refresh() {
        for (TranslateComponent component : components) {
            component.refresh();
        }
    }

    public void registerTranslateComponent(TranslateComponent component) {
        components.add(component);
    }

}

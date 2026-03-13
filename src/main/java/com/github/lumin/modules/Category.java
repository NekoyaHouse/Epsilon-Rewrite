package com.github.lumin.modules;

import com.github.lumin.assets.i18n.TranslateComponent;

public enum Category {

    COMBAT("b", "combat", "ComBat"),
    PLAYER("5", "player", "Player"),
    RENDER("a", "render", "Render"),
    WORLD("3", "world", "World"),
    CLIENT("7", "client", "Client");

    public final String icon;
    private final String name;
    public final String description;

    private final TranslateComponent translateComponent;

    Category(String icon, String name, String description) {
        this.icon = icon;
        this.name = name;
        this.description = description;

        translateComponent = TranslateComponent.create("categories", name);
    }

    public String getName() {
        return translateComponent.getTranslatedName();
    }

    @Override
    public String toString() {
        return name;
    }
}

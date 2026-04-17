package com.github.epsilon.settings;

import com.github.epsilon.assets.i18n.TranslateComponent;
import com.github.epsilon.modules.Module;

public abstract class Setting<V> {

    protected final String name;
    protected V value;
    protected V defaultValue;
    protected final Dependency dependency;
    protected TranslateComponent translateComponent;

    public Setting(String name, Module module, Dependency dependency) {
        this.name = name;
        this.dependency = dependency;
        // TranslateComponent creation is deferred to initTranslateComponent()
    }

    public void initTranslateComponent(TranslateComponent component) {
        this.translateComponent = component;
    }

    public TranslateComponent getTranslateComponent() {
        return translateComponent;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return translateComponent != null ? translateComponent.getTranslatedName() : name;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public void reset() {
        this.value = this.defaultValue;
    }

    public V getDefaultValue() {
        return defaultValue;
    }

    public boolean isAvailable() {
        return dependency != null && this.dependency.check();
    }

    @FunctionalInterface
    public interface Dependency {
        boolean check();
    }

    public Dependency getDependency() {
        return dependency;
    }
}
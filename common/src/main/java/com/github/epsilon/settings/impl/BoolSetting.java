package com.github.epsilon.settings.impl;

import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.Setting;

public class BoolSetting extends Setting<Boolean> {

    public BoolSetting(String name, Module module, boolean defaultValue, Dependency dependency) {
        super(name, module, dependency);
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public BoolSetting(String name, Module module, boolean defaultValue) {
        this(name, module, defaultValue, () -> true);
    }
}
package com.github.lumin.settings.impl;

import com.github.lumin.modules.Module;
import com.github.lumin.settings.Setting;

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
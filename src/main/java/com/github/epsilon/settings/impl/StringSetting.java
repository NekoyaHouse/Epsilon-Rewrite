package com.github.epsilon.settings.impl;

import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.Setting;

public class StringSetting extends Setting<String> {

    public StringSetting(String name, Module module, String defaultValue) {
        this(name, module, defaultValue, () -> true);
    }

    public StringSetting(String name, Module module, String defaultValue, Dependency dependency) {
        super(name, module, dependency);
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

}
package com.github.epsilon.settings.impl;

import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.Setting;

public class KeybindSetting extends Setting<Integer> {

    public KeybindSetting(String name, Module module, int defaultValue, Dependency dependency) {
        super(name, module, dependency);
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public KeybindSetting(String name, Module module, int defaultValue) {
        this(name, module, defaultValue, () -> true);
    }

    /**
     * Sets the value directly without triggering any overridden setValue logic.
     * Used to sync from Module.setKeyBind without recursion.
     */
    public void setValueDirect(int keyCode) {
        this.value = keyCode;
    }
}



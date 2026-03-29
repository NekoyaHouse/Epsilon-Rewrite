package com.github.epsilon.gui.panel.adapter;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;

public record ModuleViewModel(Module module, String displayName, String description, boolean enabled, Category category,
                              String searchText) {
    public static ModuleViewModel from(Module module) {
        String displayName = module.getTranslatedName();
        String description = module.getDescription();
        String categoryName = module.category != null ? module.category.getName() : "";
        String searchText = (displayName + " " + description + " " + categoryName).toLowerCase();
        return new ModuleViewModel(module, displayName, description, module.isEnabled(), module.category, searchText);
    }
}

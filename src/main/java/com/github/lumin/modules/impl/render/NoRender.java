package com.github.lumin.modules.impl.render;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;

public class NoRender extends Module {

    public static final NoRender INSTANCE = new NoRender();

    private NoRender() {
        super("NoRender", Category.RENDER);
    }

    public final BoolSetting vignette = boolSetting("Vignette", true);
    public final BoolSetting potionEffects = boolSetting("PotionEffects", true);
    public final BoolSetting playerNameTags = boolSetting("PlayerNameTags", true);
    public final BoolSetting blockOverlay = boolSetting("BlockOverlay", true);

    public boolean noVignette() {
        return isEnabled() && vignette.getValue();
    }

    public boolean noPotionEffects() {
        return isEnabled() && potionEffects.getValue();
    }

}

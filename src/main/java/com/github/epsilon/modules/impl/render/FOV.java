package com.github.epsilon.modules.impl.render;

import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.render.ComputeFovEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.IntSetting;

public class FOV extends Module {

    public static final FOV INSTANCE = new FOV();

    private final IntSetting fovModifier = intSetting("FOV Modifier", 120, 0, 358, 1);

    private FOV() {
        super("FOV", Category.RENDER);
    }

    @EventHandler
    private void onComputeFov(ComputeFovEvent event) {
        event.setFOV(fovModifier.getValue());
    }
}

package com.github.epsilon.modules.impl.player;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public class Sprint extends Module {
    public static final Sprint INSTANCE = new Sprint();

    private Sprint() {
        super("Sprint", Category.PLAYER);
    }

    @SubscribeEvent
    private void onClientTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;
        mc.options.keySprint.setDown(true);
    }

    @Override
    protected void onDisable() {
        mc.options.keySprint.setDown(false);
    }
}
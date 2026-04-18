package com.github.epsilon.neoforge.addon;

import com.github.epsilon.Epsilon;
import com.github.epsilon.neoforge.modules.NeoModuleTest;

/**
 * Built-in NeoForge addon for NeoForge-only features.
 */
public class NeoForgePlatformAddon extends com.github.epsilon.addon.EpsilonAddon {

    public NeoForgePlatformAddon() {
        super("epsilon_neoforge");
    }

    @Override
    public void onSetup() {
        registerModule(NeoModuleTest.INSTANCE);
        Epsilon.LOGGER.info("NeoForge platform addon initialized.");
    }

}


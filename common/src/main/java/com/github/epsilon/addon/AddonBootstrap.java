package com.github.epsilon.addon;

import com.github.epsilon.Epsilon;

/**
 * Shared addon bootstrap utility used by multiple loaders.
 */
public final class AddonBootstrap {

    private AddonBootstrap() {
    }

    public static void setupAddons(EpsilonAddonSetupEvent addonEvent) {
        setupAddons(addonEvent.getAddons());
    }

    public static void setupAddons(Iterable<EpsilonAddon> addons) {
        for (EpsilonAddon addon : addons) {
            try {
                addon.onSetup();
                Epsilon.LOGGER.info("Loaded Epsilon addon: {}", addon.addonId);
            } catch (Throwable t) {
                Epsilon.LOGGER.error("Failed to setup Epsilon addon: {}", addon.addonId, t);
            }
        }
    }

}

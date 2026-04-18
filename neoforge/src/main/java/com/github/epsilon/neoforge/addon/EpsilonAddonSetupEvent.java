package com.github.epsilon.neoforge.addon;

import java.util.ArrayList;

public class EpsilonAddonSetupEvent {

    public final ArrayList<EpsilonAddon> addons = new ArrayList<>();

    public void registerAddon(EpsilonAddon addon) {
        addons.add(addon);
    }

}

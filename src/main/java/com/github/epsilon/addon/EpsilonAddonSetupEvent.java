package com.github.epsilon.addon;

import net.neoforged.bus.api.Event;

import java.util.ArrayList;

public class EpsilonAddonSetupEvent extends Event {

    public final ArrayList<EpsilonAddon> addons = new ArrayList<>();

    public void registerAddon(EpsilonAddon addon) {
        addons.add(addon);
    }

}

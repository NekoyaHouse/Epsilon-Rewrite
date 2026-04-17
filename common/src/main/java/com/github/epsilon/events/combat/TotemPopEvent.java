package com.github.epsilon.events.combat;

import net.minecraft.world.entity.player.Player;

public class TotemPopEvent {
    private final Player player;
    private final int pops;

    public TotemPopEvent(Player player, int pops) {
        this.player = player;
        this.pops = pops;
    }

    public Player getPlayer() { return player; }
    public int getPops() { return pops; }
}


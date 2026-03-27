package com.github.epsilon.events;

import net.neoforged.bus.api.Event;

public class SlowdownEvent extends Event {

    private boolean slowdown;

    public SlowdownEvent(boolean slowdown) {
        this.slowdown = slowdown;
    }

    public boolean isSlowdown() {
        return this.slowdown;
    }

    public void setSlowdown(boolean slowdown) {
        this.slowdown = slowdown;
    }

}

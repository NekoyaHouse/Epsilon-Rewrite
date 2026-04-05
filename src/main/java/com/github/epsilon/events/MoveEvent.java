package com.github.epsilon.events;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class MoveEvent extends Event implements ICancellableEvent {

    public boolean modify;

    private final double x;
    private final double y;
    private final double z;

    public MoveEvent(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

}

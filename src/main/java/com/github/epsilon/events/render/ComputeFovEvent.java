package com.github.epsilon.events.render;

/**
 * Fired when FOV is computed. Allows modification.
 */
public class ComputeFovEvent {

    private double fov;

    public ComputeFovEvent(double fov) {
        this.fov = fov;
    }

    public double getFOV() {
        return fov;
    }

    public void setFOV(double fov) {
        this.fov = fov;
    }
}


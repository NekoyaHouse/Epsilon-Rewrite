package com.github.epsilon.events.bus;

/**
 * Base class for cancellable events.
 */
public class Cancellable implements ICancellable {

    private boolean cancelled = false;

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}


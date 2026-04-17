package com.github.epsilon.events.bus;

/**
 * Interface for cancellable events.
 */
public interface ICancellable {

    void setCancelled(boolean cancelled);

    boolean isCancelled();

    default void cancel() {
        setCancelled(true);
    }
}


package com.github.lumin.ducks;

public interface ClientPlayerAccess {
    void sendMovementPacketsWrapper();

    void superTick();

    void resetEvent();

    void resetRotations();
}

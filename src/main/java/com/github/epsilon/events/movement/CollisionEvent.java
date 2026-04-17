package com.github.epsilon.events.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class CollisionEvent {
    private BlockState blockState;
    private final BlockPos blockPos;

    public CollisionEvent(BlockState blockState, BlockPos blockPos) {
        this.blockState = blockState;
        this.blockPos = blockPos;
    }

    public BlockState getState() { return blockState; }
    public BlockPos getPos() { return blockPos; }
    public void setState(BlockState blockState) { this.blockState = blockState; }
}


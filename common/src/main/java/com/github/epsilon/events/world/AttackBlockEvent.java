package com.github.epsilon.events.world;

import com.github.epsilon.events.bus.Cancellable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class AttackBlockEvent extends Cancellable {
    private BlockPos blockPos;
    private Direction direction;

    public AttackBlockEvent(BlockPos blockPos, Direction direction) {
        this.blockPos = blockPos;
        this.direction = direction;
    }

    public BlockPos getBlockPos() { return blockPos; }
    public void setBlockPos(BlockPos blockPos) { this.blockPos = blockPos; }
    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }
}


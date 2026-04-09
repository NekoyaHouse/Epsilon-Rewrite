package com.github.epsilon.events;

import net.minecraft.world.phys.Vec2;
import net.neoforged.bus.api.Event;

public class KeyboardInputEvent extends Event {



    public static class MovementInput{
        private boolean left;
        private boolean forward;
        private boolean jumping;
        private boolean sneaking;
        private boolean sprinting;
        private boolean right;
        private boolean backward;
        private Vec2 movementFactor = new Vec2(0,0);
        public MovementInput(boolean left, boolean right, boolean forward,
                             boolean jumping, boolean sneaking, boolean sprinting, boolean backward, Vec2 movementFactor){
            this.left = left;
            this.right = right;
            this.forward = forward;
            this.jumping = jumping;
            this.sneaking = sneaking;
            this.sprinting = sprinting;
            this.backward = backward;
            this.movementFactor = movementFactor;
        }

        public void setLeft(boolean left) {
            this.left = left;
        }

        public void setRight(boolean right) {
            this.right = right;
        }

        public void setForward(boolean forward) {
            this.forward = forward;
        }

        public void setJumping(boolean jumping) {
            this.jumping = jumping;
        }

        public void setSneaking(boolean sneaking) {
            this.sneaking = sneaking;
        }

        public void setSprinting(boolean sprinting) {
            this.sprinting = sprinting;
        }

        public void setBackward(boolean backward) {
            this.backward = backward;
        }

        public boolean isLeft() {
            return left;
        }

        public boolean isForward() {
            return forward;
        }

        public boolean isJumping() {
            return jumping;
        }

        public boolean isSneaking() {
            return sneaking;
        }

        public boolean isSprinting() {
            return sprinting;
        }

        public boolean isRight() {
            return right;
        }

        public boolean isBackward() {
            return backward;
        }

        public Vec2 getMovementFactor() {
            return movementFactor;
        }

        public void setMovementFactor(Vec2 movementFactor) {
            this.movementFactor = movementFactor;
        }
    }
    MovementInput input;
    public KeyboardInputEvent(MovementInput input) {
        this.input = input;
    }
    public MovementInput setMovementInput() {
        return this.input;
    }

    public MovementInput getInput() {
        return this.input;
    }
}

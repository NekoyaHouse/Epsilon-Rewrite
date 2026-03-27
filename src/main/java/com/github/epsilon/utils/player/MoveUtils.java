package com.github.epsilon.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

public class MoveUtils {

    static Minecraft mc = Minecraft.getInstance();

    public static boolean isMoving() {
        return mc.player.zza != 0 || mc.player.xxa != 0 || mc.options.keyJump.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown() || mc.options.keyUp.isDown() || mc.options.keyDown.isDown();
    }

    public static void fixMovement(MovementInputUpdateEvent event, float yaw) {
        Vec2 moveVector = event.getInput().getMoveVector();
        float forward = moveVector.y;
        float left = moveVector.x;

        if (forward == 0 && left == 0) {
            return;
        }

        float deltaYaw = Mth.wrapDegrees(mc.player.getYRot() - yaw);
        float sin = Mth.sin(deltaYaw * Mth.DEG_TO_RAD);
        float cos = Mth.cos(deltaYaw * Mth.DEG_TO_RAD);

        float fixedLeft = left * cos - forward * sin;
        float fixedForward = forward * cos + left * sin;

        event.getInput().moveVector = new Vec2(fixedLeft, fixedForward);
    }

}

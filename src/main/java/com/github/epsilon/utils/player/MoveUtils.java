package com.github.epsilon.utils.player;

import net.minecraft.client.Minecraft;

public class MoveUtils {

    private static final Minecraft mc = Minecraft.getInstance();

    public static boolean isMoving() {
        return mc.player.zza != 0 || mc.player.xxa != 0 || mc.options.keyJump.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown() || mc.options.keyUp.isDown() || mc.options.keyDown.isDown();
    }

}

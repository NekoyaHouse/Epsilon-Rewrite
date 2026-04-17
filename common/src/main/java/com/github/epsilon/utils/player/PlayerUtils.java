package com.github.epsilon.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;

public class PlayerUtils {

    private static final Minecraft mc = Minecraft.getInstance();

    public static boolean isEating() {
        return (mc.player.getMainHandItem().getComponents().has(DataComponents.FOOD) || mc.player.getOffhandItem().getComponents().has(DataComponents.FOOD)) && mc.player.isUsingItem();
    }

}

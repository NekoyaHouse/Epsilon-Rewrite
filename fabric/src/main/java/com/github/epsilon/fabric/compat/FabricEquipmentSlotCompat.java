package com.github.epsilon.fabric.compat;

import com.github.epsilon.compat.EquipmentSlotCompat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

public class FabricEquipmentSlotCompat implements EquipmentSlotCompat {

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        try {
            Method m = ItemStack.class.getMethod("getEquipmentSlot");
            return (EquipmentSlot) m.invoke(stack);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get equipment slot from ItemStack", e);
        }
    }

}


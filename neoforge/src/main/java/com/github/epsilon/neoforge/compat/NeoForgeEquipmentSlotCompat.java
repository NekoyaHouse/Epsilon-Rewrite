package com.github.epsilon.neoforge.compat;

import com.github.epsilon.compat.EquipmentSlotCompat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class NeoForgeEquipmentSlotCompat implements EquipmentSlotCompat {

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return stack.getEquipmentSlot();
    }

}


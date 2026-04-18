package com.github.epsilon.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Loader abstraction for resolving an item's equipment slot.
 */
public interface EquipmentSlotCompat {

    EquipmentSlot getEquipmentSlot(ItemStack stack);

}


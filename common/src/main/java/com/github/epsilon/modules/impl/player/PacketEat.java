package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.network.PacketEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.item.ItemStack;
import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.tick.TickEvent;

public class PacketEat extends Module {

    public static final PacketEat INSTANCE = new PacketEat();

    private PacketEat() {
        super("Packet Eat", Category.PLAYER);
    }

    private ItemStack item;

    @EventHandler
    private void onClientTickPost(TickEvent.Post event) {
        if (nullCheck()) return;
        if (mc.player.isUsingItem()) {
            item = mc.player.getUseItem();
        }
    }

    @EventHandler
    private void onPacket(PacketEvent.Send event) {
        if (event.getPacket() instanceof ServerboundPlayerActionPacket packet && packet.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
            if (item.get(DataComponents.FOOD).canAlwaysEat()) {
                event.setCancelled(true);
            }
        }
    }

}

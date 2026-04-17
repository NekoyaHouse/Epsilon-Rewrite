package com.github.epsilon.managers;

import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.combat.TotemPopEvent;
import com.github.epsilon.events.network.PacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;

public class SyncManager {

    public static final SyncManager INSTANCE = new SyncManager();

    public static int serverSlot = -1;
    private static final HashMap<String, Integer> popList = new HashMap<>();

    private SyncManager() {
        EpsilonEventBus.INSTANCE.subscribe(this);
    }

    @EventHandler
    private void onSyncUpdateSelectedSlotSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof ServerboundSetCarriedItemPacket packet) {
            serverSlot = packet.getSlot();
        }
    }

    @EventHandler
    private void onSyncUpdateSelectedSlotReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof ClientboundContainerSetSlotPacket packet && packet.getContainerId() == -2) {
            serverSlot = packet.getSlot();
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (event.getPacket() instanceof ClientboundEntityEventPacket packet && packet.getEventId() == EntityEvent.PROTECTED_FROM_DEATH) {
            if (packet.getEntity(mc.level) instanceof Player player) {
                int pops = popList.merge(player.getName().getString(), 1, Integer::sum);
                EpsilonEventBus.INSTANCE.post(new TotemPopEvent(player, pops));
            }
        }
    }
}

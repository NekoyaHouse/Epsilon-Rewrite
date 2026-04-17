package com.github.epsilon.utils.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

import java.util.HashSet;
import java.util.Set;

public class PacketUtils {

    private static final Minecraft mc = Minecraft.getInstance();

    public static final Set<Packet<?>> bypassPackets = new HashSet<>();

    public static void sendSilently(Packet<?> packet) {
        bypassPackets.add(packet);
        mc.getConnection().send(packet);
    }

}

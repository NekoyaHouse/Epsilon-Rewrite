package com.github.epsilon.events;

import net.minecraft.network.protocol.Packet;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class PacketEvent {

    public static class Send extends Event implements ICancellableEvent {

        private Packet<?> packet;

        public Send(Packet<?> packet) {
            this.packet = packet;
        }

        public Packet<?> getPacket() {
            return this.packet;
        }

        public void setPacket(Packet<?> packet) {
            this.packet = packet;
        }

    }

    public static class Receive extends Event implements ICancellableEvent {

        private Packet<?> packet;

        public Receive(Packet<?> packet) {
            this.packet = packet;
        }

        public Packet<?> getPacket() {
            return this.packet;
        }

        public void setPacket(Packet<?> packet) {
            this.packet = packet;
        }

    }

}

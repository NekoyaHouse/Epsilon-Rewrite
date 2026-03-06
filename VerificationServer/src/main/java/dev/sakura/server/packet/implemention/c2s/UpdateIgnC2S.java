package dev.sakura.server.packet.implemention.c2s;

import dev.sakura.server.packet.IRCPacket;
import dev.sakura.server.packet.annotations.ProtocolField;

public class UpdateIgnC2S implements IRCPacket {
    @ProtocolField("n")
    private String name;

    public UpdateIgnC2S() {
    }

    public UpdateIgnC2S(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}


package dev.sakura.server.packet.implemention.c2s;

import dev.sakura.server.packet.IRCPacket;
import dev.sakura.server.packet.annotations.ProtocolField;

public class MessageC2S implements IRCPacket {
    @ProtocolField("m")
    private String message;

    public MessageC2S() {
    }

    public MessageC2S(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}


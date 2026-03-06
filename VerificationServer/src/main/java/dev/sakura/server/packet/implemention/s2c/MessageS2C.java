package dev.sakura.server.packet.implemention.s2c;

import dev.sakura.server.packet.IRCPacket;
import dev.sakura.server.packet.annotations.ProtocolField;

public class MessageS2C implements IRCPacket {
    @ProtocolField("s")
    private String sender;

    @ProtocolField("m")
    private String message;

    public MessageS2C() {
    }

    public MessageS2C(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }
}


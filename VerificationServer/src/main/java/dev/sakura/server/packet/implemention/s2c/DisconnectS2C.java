package dev.sakura.server.packet.implemention.s2c;

import dev.sakura.server.packet.IRCPacket;
import dev.sakura.server.packet.annotations.ProtocolField;

public class DisconnectS2C implements IRCPacket {
    @ProtocolField("r")
    private String reason;

    public DisconnectS2C() {
    }

    public DisconnectS2C(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}


package dev.maru.verify.packet.implemention.s2c;

import dev.maru.verify.packet.IRCPacket;
import dev.maru.verify.packet.annotations.ProtocolField;

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

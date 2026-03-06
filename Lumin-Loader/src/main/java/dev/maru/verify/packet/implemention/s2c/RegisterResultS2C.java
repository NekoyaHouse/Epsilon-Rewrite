package dev.maru.verify.packet.implemention.s2c;

import dev.maru.verify.packet.IRCPacket;
import dev.maru.verify.packet.annotations.ProtocolField;

public class RegisterResultS2C implements IRCPacket {
    @ProtocolField("s")
    private boolean success;

    @ProtocolField("e")
    private long expireAt;

    @ProtocolField("t")
    private long timeWindow;

    @ProtocolField("m")
    private String message;

    public RegisterResultS2C() {
    }

    public RegisterResultS2C(boolean success, long expireAt, long timeWindow) {
        this.success = success;
        this.expireAt = expireAt;
        this.timeWindow = timeWindow;
    }

    public RegisterResultS2C(boolean success, long expireAt, long timeWindow, String message) {
        this.success = success;
        this.expireAt = expireAt;
        this.timeWindow = timeWindow;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public long getTimeWindow() {
        return timeWindow;
    }

    public String getMessage() {
        return message;
    }
}

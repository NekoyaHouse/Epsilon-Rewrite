package dev.sakura.server.packet.implemention.s2c;

import dev.sakura.server.packet.IRCPacket;
import dev.sakura.server.packet.annotations.ProtocolField;

public class LoginResultS2C implements IRCPacket {
    @ProtocolField("s")
    private boolean success;

    @ProtocolField("e")
    private long expireAt;

    @ProtocolField("t")
    private long timeWindow;

    @ProtocolField("m")
    private String message;

    public LoginResultS2C() {
    }

    public LoginResultS2C(boolean success, long expireAt, long timeWindow) {
        this.success = success;
        this.expireAt = expireAt;
        this.timeWindow = timeWindow;
    }

    public LoginResultS2C(boolean success, long expireAt, long timeWindow, String message) {
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


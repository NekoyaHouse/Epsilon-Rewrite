package dev.maru.verify.packet.implemention.s2c;

import dev.maru.verify.packet.IRCPacket;
import dev.maru.verify.packet.annotations.ProtocolField;

import java.util.Map;

public class UpdateUserListS2C implements IRCPacket {
    @ProtocolField("u")
    private Map<String, String> userMap;

    public UpdateUserListS2C() {
    }

    public UpdateUserListS2C(Map<String, String> userMap) {
        this.userMap = userMap;
    }

    public Map<String, String> getUserMap() {
        return userMap;
    }
}

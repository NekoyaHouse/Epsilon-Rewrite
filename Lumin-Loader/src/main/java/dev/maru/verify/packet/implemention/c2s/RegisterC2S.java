package dev.maru.verify.packet.implemention.c2s;

import dev.maru.verify.packet.IRCPacket;
import dev.maru.verify.packet.annotations.ProtocolField;

import java.util.Set;

public class RegisterC2S implements IRCPacket {
    @ProtocolField("u")
    private String username;

    @ProtocolField("p")
    private String password;

    @ProtocolField("h")
    private String hwid;

    @ProtocolField("q")
    private Set<String> qqSet;

    @ProtocolField("ph")
    private String phone;

    @ProtocolField("c")
    private String cardKey;

    public RegisterC2S() {
    }

    public RegisterC2S(String username, String password, String hwid, Set<String> qqSet, String phone, String cardKey) {
        this.username = username;
        this.password = password;
        this.hwid = hwid;
        this.qqSet = qqSet;
        this.phone = phone;
        this.cardKey = cardKey;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHwid() {
        return hwid;
    }

    public Set<String> getQqSet() {
        return qqSet;
    }

    public String getPhone() {
        return phone;
    }

    public String getCardKey() {
        return cardKey;
    }
}

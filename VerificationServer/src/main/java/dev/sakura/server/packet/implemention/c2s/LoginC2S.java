package dev.sakura.server.packet.implemention.c2s;

import dev.sakura.server.packet.IRCPacket;
import dev.sakura.server.packet.annotations.ProtocolField;

import java.util.Set;

public class LoginC2S implements IRCPacket {
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

    public LoginC2S() {
    }

    public LoginC2S(String username, String password, String hwid, Set<String> qqSet, String phone) {
        this.username = username;
        this.password = password;
        this.hwid = hwid;
        this.qqSet = qqSet;
        this.phone = phone;
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
}


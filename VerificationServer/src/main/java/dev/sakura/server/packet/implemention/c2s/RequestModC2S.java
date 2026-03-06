package dev.sakura.server.packet.implemention.c2s;

import dev.sakura.server.packet.IRCPacket;
import dev.sakura.server.packet.annotations.ProtocolField;

public class RequestModC2S implements IRCPacket {
    @ProtocolField("hwid")
    private String hwid;

    @ProtocolField("name")
    private String name;

    @ProtocolField("version")
    private String version;

    public RequestModC2S() {
    }

    public RequestModC2S(String hwid, String name, String version) {
        this.hwid = hwid;
        this.name = name;
        this.version = version;
    }

    public String getHwid() {
        return hwid;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}

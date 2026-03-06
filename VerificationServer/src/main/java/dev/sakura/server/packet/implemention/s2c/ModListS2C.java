package dev.sakura.server.packet.implemention.s2c;

import dev.sakura.server.packet.IRCPacket;
import dev.sakura.server.packet.annotations.ProtocolField;

import java.util.List;

public class ModListS2C implements IRCPacket {
    @ProtocolField("names")
    private List<String> names;

    @ProtocolField("versions")
    private List<String> versions;

    public ModListS2C() {
    }

    public ModListS2C(List<String> names, List<String> versions) {
        this.names = names;
        this.versions = versions;
    }

    public List<String> getNames() {
        return names;
    }

    public List<String> getVersions() {
        return versions;
    }
}

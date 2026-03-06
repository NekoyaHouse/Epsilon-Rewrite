package dev.sakura.server.impl.handler.implemention;

import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.interfaces.PacketHandler;
import dev.sakura.server.impl.management.ModManager;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.implemention.c2s.GetModListC2S;
import dev.sakura.server.packet.implemention.s2c.ModListS2C;

import java.util.List;
import java.util.stream.Collectors;

public class GetModListHandler implements PacketHandler<GetModListC2S> {
    @Override
    public void handle(GetModListC2S packet, Connection connection, UserManager userManager, User user) {
        List<ModManager.ModInfo> mods = ModManager.listMods();
        List<String> names = mods.stream().map(m -> m.name).collect(Collectors.toList());
        List<String> versions = mods.stream().map(m -> m.version).collect(Collectors.toList());
        connection.sendPacket(new ModListS2C(names, versions));
    }

    @Override
    public boolean allowNull() {
        return true;
    }
}

package dev.sakura.server.impl.interfaces;

import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.IRCPacket;

public interface PacketHandler<T extends IRCPacket> {
    void handle(T packet, Connection connection, UserManager userManager, User user);

    default boolean allowNull() {
        return false;
    }
}


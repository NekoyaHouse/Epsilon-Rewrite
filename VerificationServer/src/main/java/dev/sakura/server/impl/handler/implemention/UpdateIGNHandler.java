package dev.sakura.server.impl.handler.implemention;

import dev.sakura.server.impl.IRCServer;
import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.interfaces.PacketHandler;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.implemention.c2s.UpdateIgnC2S;
import org.tinylog.Logger;

public class UpdateIGNHandler implements PacketHandler<UpdateIgnC2S> {
    @Override
    public void handle(UpdateIgnC2S packet, Connection connection, UserManager userManager, User user) {
        if (user == null) {
            return;
        }
        String prevIgn = user.getIgn();
        if (!prevIgn.equals(packet.getName())) {
            user.setIgn(packet.getName());
            IRCServer.getInstance().sendInGameUsername();
            Logger.info("User {} updated in-game-username {} -> {}", user.getUsername(), prevIgn, user.getIgn());
        }
    }
}


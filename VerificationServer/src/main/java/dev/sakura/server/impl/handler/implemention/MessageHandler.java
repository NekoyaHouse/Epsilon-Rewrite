package dev.sakura.server.impl.handler.implemention;

import dev.sakura.server.impl.IRCServer;
import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.interfaces.PacketHandler;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.implemention.c2s.MessageC2S;
import dev.sakura.server.packet.implemention.s2c.MessageS2C;
import org.tinylog.Logger;

public class MessageHandler implements PacketHandler<MessageC2S> {
    @Override
    public void handle(MessageC2S packet, Connection connection, UserManager userManager, User user) {
        if (user == null) {
            return;
        }
        String prefix = user.getPrefix();
        String sender = prefix == null || prefix.isEmpty() ? user.getUsername() : prefix + " " + user.getUsername();
        IRCServer.getInstance().boardCastMessage(new MessageS2C(sender, packet.getMessage()));
        Logger.info("Chat Message: {} >> {}", user.getUsername(), packet.getMessage());
    }
}


package dev.sakura.server.impl.handler.implemention;

import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.interfaces.PacketHandler;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.implemention.c2s.HandshakeC2S;
import dev.sakura.server.packet.implemention.s2c.ConnectedS2C;
import dev.sakura.server.packet.implemention.s2c.DisconnectS2C;
import org.tinylog.Logger;

public class HandshakeHandler implements PacketHandler<HandshakeC2S> {
    private static final String EXPECTED_TOKEN = "SakuraVerifyToken0123456789ABCDE";

    @Override
    public void handle(HandshakeC2S packet, Connection connection, UserManager userManager, User user) {
        Logger.info("User {} start handshake", packet.getUsername());

        if (!EXPECTED_TOKEN.equals(packet.getToken())) {
            connection.sendPacket(new DisconnectS2C("验证失败"));
            return;
        }

        if (userManager.getUser(connection.getSessionId()) != null) {
            connection.sendPacket(new DisconnectS2C("你已经连接到了这个服务器！"));
        } else {
            userManager.putUser(connection.getSessionId(), new User(connection.getSessionId(), packet.getUsername(), packet.getToken()));
            connection.sendPacket(new ConnectedS2C());
            Logger.info("Accepted user {} ({}/{})", connection.getSessionId(), connection.getIPAddress(), packet.getUsername());
        }
    }

    @Override
    public boolean allowNull() {
        return true;
    }
}

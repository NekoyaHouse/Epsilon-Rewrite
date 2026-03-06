package dev.sakura.server.impl.handler.implemention;

import dev.sakura.server.impl.IRCServer;
import dev.sakura.server.impl.auth.AuthService;
import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.interfaces.PacketHandler;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.implemention.c2s.RegisterC2S;
import dev.sakura.server.packet.implemention.s2c.ConnectedS2C;
import dev.sakura.server.packet.implemention.s2c.RegisterResultS2C;
import org.tinylog.Logger;

import java.util.Set;
import java.util.stream.Collectors;

public class RegisterHandler implements PacketHandler<RegisterC2S> {
    @Override
    public void handle(RegisterC2S packet, Connection connection, UserManager userManager, User user) {
        AuthService authService = IRCServer.getInstance().getAuthService();
        AuthService.AuthResult result = authService.register(
                packet.getUsername(),
                packet.getPassword(),
                packet.getHwid(),
                packet.getQqSet(),
                packet.getPhone(),
                packet.getCardKey()
        );
        connection.sendPacket(new RegisterResultS2C(result.success(), result.expireAt(), result.timeWindow(), result.message()));

        if (!result.success()) {
            return;
        }

        userManager.putUser(connection.getSessionId(), new User(connection.getSessionId(), packet.getUsername(), "", result.expireAt(), ""));
        connection.sendPacket(new ConnectedS2C());

        Set<String> qqSet = packet.getQqSet();
        String qq = qqSet == null || qqSet.isEmpty() ? "" : qqSet.stream().collect(Collectors.joining(","));
        String phone = packet.getPhone() == null ? "" : packet.getPhone();
        String hwid = packet.getHwid() == null ? "" : packet.getHwid();
        Logger.info("Register success: user={} ip={} hwid={} qq={} phone={}", packet.getUsername(), connection.getIPAddress(), hwid, qq, phone);
    }

    @Override
    public boolean allowNull() {
        return true;
    }
}


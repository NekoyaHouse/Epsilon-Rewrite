package dev.sakura.server.impl.handler.implemention;

import dev.sakura.server.impl.IRCServer;
import dev.sakura.server.impl.auth.AuthService;
import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.interfaces.PacketHandler;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.implemention.c2s.RechargeC2S;
import dev.sakura.server.packet.implemention.s2c.RechargeResultS2C;

public class RechargeHandler implements PacketHandler<RechargeC2S> {
    @Override
    public void handle(RechargeC2S packet, Connection connection, UserManager userManager, User user) {
        if (user == null || !user.getUsername().equals(packet.getUsername())) {
            connection.sendPacket(new RechargeResultS2C(false, 0, System.currentTimeMillis() / 30000L, "未登录或账号不匹配"));
            return;
        }

        AuthService authService = IRCServer.getInstance().getAuthService();
        AuthService.AuthResult result = authService.recharge(packet.getUsername(), packet.getCardKey());
        connection.sendPacket(new RechargeResultS2C(result.success(), result.expireAt(), result.timeWindow(), result.message()));

        if (result.success()) {
            user.setExpireAt(result.expireAt());
        }
    }
}


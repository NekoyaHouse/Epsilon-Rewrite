package dev.sakura.server.impl.handler.implemention;

import dev.sakura.server.impl.IRCServer;
import dev.sakura.server.impl.auth.AuthService;
import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.interfaces.PacketHandler;
import dev.sakura.server.impl.storage.UserRepository;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.implemention.c2s.LoginC2S;
import dev.sakura.server.packet.implemention.s2c.ConnectedS2C;
import dev.sakura.server.packet.implemention.s2c.LoginResultS2C;
import org.tinylog.Logger;

import java.util.Set;
import java.util.stream.Collectors;

public class LoginHandler implements PacketHandler<LoginC2S> {
    @Override
    public void handle(LoginC2S packet, Connection connection, UserManager userManager, User user) {
        AuthService authService = IRCServer.getInstance().getAuthService();
        AuthService.AuthResult result = authService.login(packet.getUsername(), packet.getPassword(), packet.getHwid(), packet.getQqSet(), packet.getPhone());
        connection.sendPacket(new LoginResultS2C(result.success(), result.expireAt(), result.timeWindow(), result.message()));

        if (!result.success()) {
            return;
        }

        String prefix = "";
        Set<String> qqSet = null;
        String phone = "";
        String hwid = packet.getHwid() == null ? "" : packet.getHwid();
        try (java.sql.Connection db = IRCServer.getInstance().getDatabase().openConnection()) {
            UserRepository.UserRow row = IRCServer.getInstance().getUserRepository().findByUsername(db, packet.getUsername());
            if (row != null) {
                prefix = row.prefix() == null ? "" : row.prefix();
                qqSet = row.qqSet();
                phone = row.phone() == null ? "" : row.phone();
                hwid = row.hwid() == null ? hwid : row.hwid();
            }
        } catch (Exception ignored) {
        }

        userManager.putUser(connection.getSessionId(), new User(connection.getSessionId(), packet.getUsername(), "", result.expireAt(), prefix));
        connection.sendPacket(new ConnectedS2C());

        String qq = qqSet == null || qqSet.isEmpty() ? "" : qqSet.stream().collect(Collectors.joining(","));
        Logger.info("Login success: user={} ip={} hwid={} qq={} phone={}", packet.getUsername(), connection.getIPAddress(), hwid, qq, phone);
    }

    @Override
    public boolean allowNull() {
        return true;
    }
}

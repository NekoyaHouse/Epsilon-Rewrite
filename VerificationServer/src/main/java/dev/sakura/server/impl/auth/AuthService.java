package dev.sakura.server.impl.auth;

import dev.sakura.server.impl.storage.CardRepository;
import dev.sakura.server.impl.storage.SqliteDatabase;
import dev.sakura.server.impl.storage.UserRepository;
import dev.sakura.server.util.CryptoUtil;

import java.security.SecureRandom;
import java.sql.Connection;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

public final class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    private final SqliteDatabase database;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public record AuthResult(boolean success, long expireAt, long timeWindow, String message) {
    }

    public AuthService(SqliteDatabase database, UserRepository userRepository, CardRepository cardRepository) {
        this.database = database;
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
    }

    public AuthResult register(String username, String password, String hwid, Set<String> qqSet, String phone, String cardKey) {
        long now = Instant.now().toEpochMilli();
        long timeWindow = now / 30000L;

        if (!isValidUsername(username)) {
            return new AuthResult(false, 0, timeWindow, "用户名不合法");
        }
        if (password == null || password.isEmpty()) {
            return new AuthResult(false, 0, timeWindow, "密码不能为空");
        }
        if (hwid == null || hwid.isEmpty()) {
            return new AuthResult(false, 0, timeWindow, "机器码不能为空");
        }
        if (cardKey == null || cardKey.isEmpty()) {
            return new AuthResult(false, 0, timeWindow, "卡密不能为空");
        }

        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);

            if (userRepository.existsUsername(connection, username)) {
                connection.rollback();
                return new AuthResult(false, 0, timeWindow, "用户名已存在");
            }

            CardRepository.CardRow card = cardRepository.findUnused(connection, cardKey);
            if (card == null) {
                connection.rollback();
                return new AuthResult(false, 0, timeWindow, "卡密无效或已被使用");
            }

            String salt = generateSalt();
            String hash = hashPassword(salt, password);
            long expireAt = now + card.durationMs();

            boolean inserted = userRepository.insertUser(connection, username, salt, hash, hwid, qqSet, phone, expireAt, true);
            if (!inserted) {
                connection.rollback();
                return new AuthResult(false, 0, timeWindow, "注册失败");
            }

            boolean used = cardRepository.markUsed(connection, cardKey, username);
            if (!used) {
                connection.rollback();
                return new AuthResult(false, 0, timeWindow, "卡密使用失败");
            }

            connection.commit();
            return new AuthResult(true, expireAt, timeWindow, "注册成功");
        } catch (Exception e) {
            return new AuthResult(false, 0, timeWindow, "注册失败");
        }
    }

    public AuthResult login(String username, String password, String hwid, Set<String> qqSet, String phone) {
        long now = Instant.now().toEpochMilli();
        long timeWindow = now / 30000L;

        if (username == null || username.isEmpty() || password == null || password.isEmpty() || hwid == null || hwid.isEmpty()) {
            return new AuthResult(false, 0, timeWindow, "参数错误");
        }

        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);

            UserRepository.UserRow user = userRepository.findByUsername(connection, username);
            if (user == null) {
                connection.rollback();
                return new AuthResult(false, 0, timeWindow, "账号不存在");
            }

            if (user.expireAt() <= now) {
                connection.rollback();
                return new AuthResult(false, user.expireAt(), timeWindow, "账号已到期");
            }

            String hash = hashPassword(user.passwordSalt(), password);
            if (!hash.equals(user.passwordHash())) {
                connection.rollback();
                return new AuthResult(false, user.expireAt(), timeWindow, "密码错误");
            }

            if (user.hwid() != null && !user.hwid().isEmpty() && !user.hwid().equals(hwid)) {
                connection.rollback();
                return new AuthResult(false, user.expireAt(), timeWindow, "机器码不匹配");
            }

            if (user.online()) {
                connection.rollback();
                return new AuthResult(false, user.expireAt(), timeWindow, "账号已在线");
            }

            if (!userRepository.setOnlineIfOffline(connection, username)) {
                connection.rollback();
                return new AuthResult(false, user.expireAt(), timeWindow, "账号已在线");
            }

            userRepository.bindHwidIfEmpty(connection, username, hwid);
            userRepository.mergeQqAndPhone(connection, username, qqSet, phone);

            connection.commit();
            return new AuthResult(true, user.expireAt(), timeWindow, "登录成功");
        } catch (Exception e) {
            return new AuthResult(false, 0, timeWindow, "登录失败");
        }
    }

    public AuthResult recharge(String username, String cardKey) {
        long now = Instant.now().toEpochMilli();
        long timeWindow = now / 30000L;

        if (username == null || username.isEmpty() || cardKey == null || cardKey.isEmpty()) {
            return new AuthResult(false, 0, timeWindow, "参数错误");
        }

        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);

            UserRepository.UserRow user = userRepository.findByUsername(connection, username);
            if (user == null) {
                connection.rollback();
                return new AuthResult(false, 0, timeWindow, "账号不存在");
            }

            CardRepository.CardRow card = cardRepository.findUnused(connection, cardKey);
            if (card == null) {
                connection.rollback();
                return new AuthResult(false, user.expireAt(), timeWindow, "卡密无效或已被使用");
            }

            long newExpire = user.expireAt() + card.durationMs();
            if (!userRepository.updateExpireAt(connection, username, newExpire)) {
                connection.rollback();
                return new AuthResult(false, user.expireAt(), timeWindow, "充值失败");
            }

            if (!cardRepository.markUsed(connection, cardKey, username)) {
                connection.rollback();
                return new AuthResult(false, user.expireAt(), timeWindow, "卡密使用失败");
            }

            connection.commit();
            return new AuthResult(true, newExpire, timeWindow, "充值成功");
        } catch (Exception e) {
            return new AuthResult(false, 0, timeWindow, "充值失败");
        }
    }

    public boolean setUserOnline(String username, boolean online) {
        try (Connection connection = database.openConnection()) {
            return userRepository.setOnline(connection, username, online);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean changePassword(String username, String newPassword) {
        if (username == null || username.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            return false;
        }
        try (Connection connection = database.openConnection()) {
            String salt = generateSalt();
            String hash = hashPassword(salt, newPassword);
            return userRepository.updatePassword(connection, username, salt, hash);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        for (int i = 0; i < username.length(); i++) {
            char c = username.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static String generateSalt() {
        byte[] buf = new byte[16];
        RANDOM.nextBytes(buf);
        return B64.encodeToString(buf);
    }

    private static String hashPassword(String salt, String password) {
        return CryptoUtil.sha256Base64UrlNoPaddingUtf8(salt, ":", password);
    }
}

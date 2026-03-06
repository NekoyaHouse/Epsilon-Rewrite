package dev.sakura.server.impl.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class UserRepository {
    private static final Gson GSON = new Gson();
    private static final int DEFAULT_MAX_CLOUD_CONFIGS = 3;

    public record UserRow(
            String username,
            String cardGroup,
            String passwordSalt,
            String passwordHash,
            String hwid,
            Set<String> qqSet,
            String phone,
            String prefix,
            long expireAt,
            boolean online,
            boolean banned,
            long createdAt,
            long updatedAt
    ) {
    }

    public record CloudConfigIndexRow(
            String ownerUsername,
            String configName,
            int size,
            long createdAt,
            long updatedAt
    ) {
    }

    public UserRow findByUsername(Connection connection, String username) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT " +
                        "u.username,u.password_salt,u.password_hash,u.hwid,u.qq_json,u.phone,u.prefix,u.expire_at,u.online,u.banned,u.created_at,u.updated_at," +
                        "(SELECT c.grp FROM cards c WHERE c.used_by = u.username ORDER BY c.used_at DESC LIMIT 1) AS card_grp " +
                        "FROM users u WHERE u.username = ?"
        )) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapUserRow(rs);
            }
        }
    }

    public List<UserRow> findByQq(Connection connection, String qq) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT " +
                        "u.username,u.password_salt,u.password_hash,u.hwid,u.qq_json,u.phone,u.prefix,u.expire_at,u.online,u.banned,u.created_at,u.updated_at," +
                        "(SELECT c.grp FROM cards c WHERE c.used_by = u.username ORDER BY c.used_at DESC LIMIT 1) AS card_grp " +
                        "FROM users u JOIN user_qq q ON u.username = q.username WHERE q.qq = ?"
        )) {
            ps.setString(1, qq);
            try (ResultSet rs = ps.executeQuery()) {
                List<UserRow> out = new java.util.ArrayList<>();
                while (rs.next()) {
                    out.add(mapUserRow(rs));
                }
                return out;
            }
        }
    }

    public List<UserRow> listOnlineUsers(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT " +
                        "u.username,u.password_salt,u.password_hash,u.hwid,u.qq_json,u.phone,u.prefix,u.expire_at,u.online,u.banned,u.created_at,u.updated_at," +
                        "(SELECT c.grp FROM cards c WHERE c.used_by = u.username ORDER BY c.used_at DESC LIMIT 1) AS card_grp " +
                        "FROM users u WHERE u.online != 0"
        )) {
            try (ResultSet rs = ps.executeQuery()) {
                List<UserRow> out = new java.util.ArrayList<>();
                while (rs.next()) {
                    out.add(mapUserRow(rs));
                }
                return out;
            }
        }
    }

    public List<UserRow> listAllUsers(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT " +
                        "u.username,u.password_salt,u.password_hash,u.hwid,u.qq_json,u.phone,u.prefix,u.expire_at,u.online,u.banned,u.created_at,u.updated_at," +
                        "(SELECT c.grp FROM cards c WHERE c.used_by = u.username ORDER BY c.used_at DESC LIMIT 1) AS card_grp " +
                        "FROM users u"
        )) {
            try (ResultSet rs = ps.executeQuery()) {
                List<UserRow> out = new java.util.ArrayList<>();
                while (rs.next()) {
                    out.add(mapUserRow(rs));
                }
                return out;
            }
        }
    }

    public boolean insertUser(
            Connection connection,
            String username,
            String passwordSalt,
            String passwordHash,
            String hwid,
            Set<String> qqSet,
            String phone,
            long expireAt,
            boolean online
    ) throws SQLException {
        long now = Instant.now().toEpochMilli();
        Set<String> safeQq = qqSet == null ? Collections.emptySet() : qqSet;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO users(username,password_salt,password_hash,hwid,qq_json,phone,prefix,expire_at,online,banned,created_at,updated_at) " +
                        "VALUES(?,?,?,?,?,?,?,?,?,0,?,?)"
        )) {
            ps.setString(1, username);
            ps.setString(2, passwordSalt);
            ps.setString(3, passwordHash);
            ps.setString(4, hwid);
            ps.setString(5, GSON.toJson(safeQq));
            ps.setString(6, phone == null ? "" : phone);
            ps.setString(7, "");
            ps.setLong(8, expireAt);
            ps.setInt(9, online ? 1 : 0);
            ps.setLong(10, now);
            ps.setLong(11, now);
            boolean ok = ps.executeUpdate() == 1;
            if (!ok) {
                return false;
            }
            if (!safeQq.isEmpty()) {
                try (PreparedStatement qqPs = connection.prepareStatement("INSERT OR IGNORE INTO user_qq(username, qq) VALUES(?,?)")) {
                    for (String qq : safeQq) {
                        qqPs.setString(1, username);
                        qqPs.setString(2, qq);
                        qqPs.addBatch();
                    }
                    qqPs.executeBatch();
                }
            }
            return true;
        }
    }

    public boolean updatePassword(Connection connection, String username, String passwordSalt, String passwordHash) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET password_salt = ?, password_hash = ?, updated_at = ? WHERE username = ?"
        )) {
            ps.setString(1, passwordSalt);
            ps.setString(2, passwordHash);
            ps.setLong(3, now);
            ps.setString(4, username);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updatePrefix(Connection connection, String username, String prefix) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET prefix = ?, updated_at = ? WHERE username = ?"
        )) {
            ps.setString(1, prefix == null ? "" : prefix);
            ps.setLong(2, now);
            ps.setString(3, username);
            return ps.executeUpdate() == 1;
        }
    }

    public int backfillQqIndex(Connection connection) throws SQLException {
        int inserted = 0;
        try (PreparedStatement ps = connection.prepareStatement("SELECT username, qq_json FROM users")) {
            try (ResultSet rs = ps.executeQuery()) {
                try (PreparedStatement ins = connection.prepareStatement("INSERT OR IGNORE INTO user_qq(username, qq) VALUES(?,?)")) {
                    while (rs.next()) {
                        String username = rs.getString("username");
                        String qqJson = rs.getString("qq_json");
                        Set<String> qqSet;
                        try {
                            qqSet = qqJson == null || qqJson.isEmpty()
                                    ? Collections.emptySet()
                                    : GSON.fromJson(qqJson, TypeToken.getParameterized(Set.class, String.class).getType());
                        } catch (Exception ignored) {
                            qqSet = Collections.emptySet();
                        }

                        for (String qq : qqSet) {
                            ins.setString(1, username);
                            ins.setString(2, qq);
                            ins.addBatch();
                        }
                    }
                    int[] res = ins.executeBatch();
                    for (int r : res) {
                        if (r > 0) {
                            inserted += r;
                        }
                    }
                }
            }
        }
        return inserted;
    }

    public boolean setOnline(Connection connection, String username, boolean online) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET online = ?, updated_at = ? WHERE username = ?"
        )) {
            ps.setInt(1, online ? 1 : 0);
            ps.setLong(2, now);
            ps.setString(3, username);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean setOnlineIfOffline(Connection connection, String username) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET online = 1, updated_at = ? WHERE username = ? AND online = 0"
        )) {
            ps.setLong(1, now);
            ps.setString(2, username);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean bindHwidIfEmpty(Connection connection, String username, String hwid) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET hwid = ?, updated_at = ? WHERE username = ? AND (hwid IS NULL OR hwid = '')"
        )) {
            ps.setString(1, hwid);
            ps.setLong(2, now);
            ps.setString(3, username);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean mergeQqAndPhone(Connection connection, String username, Set<String> qqSet, String phone) throws SQLException {
        if ((qqSet == null || qqSet.isEmpty()) && (phone == null || phone.isBlank())) {
            return true;
        }
        long now = Instant.now().toEpochMilli();

        UserRow existing = findByUsername(connection, username);
        if (existing == null) {
            return false;
        }

        Set<String> merged = new HashSet<>(existing.qqSet() == null ? Collections.emptySet() : existing.qqSet());
        if (qqSet != null) {
            merged.addAll(qqSet);
        }
        String mergedJson = GSON.toJson(merged);

        boolean updatePhone = (existing.phone() == null || existing.phone().isBlank()) && phone != null && !phone.isBlank();
        String safePhone = phone == null ? "" : phone;

        try (PreparedStatement ps = connection.prepareStatement(
                updatePhone
                        ? "UPDATE users SET qq_json = ?, phone = ?, updated_at = ? WHERE username = ?"
                        : "UPDATE users SET qq_json = ?, updated_at = ? WHERE username = ?"
        )) {
            if (updatePhone) {
                ps.setString(1, mergedJson);
                ps.setString(2, safePhone);
                ps.setLong(3, now);
                ps.setString(4, username);
            } else {
                ps.setString(1, mergedJson);
                ps.setLong(2, now);
                ps.setString(3, username);
            }
            ps.executeUpdate();
        }

        if (!merged.isEmpty()) {
            try (PreparedStatement qqPs = connection.prepareStatement("INSERT OR IGNORE INTO user_qq(username, qq) VALUES(?,?)")) {
                for (String qq : merged) {
                    qqPs.setString(1, username);
                    qqPs.setString(2, qq);
                    qqPs.addBatch();
                }
                qqPs.executeBatch();
            }
        }
        return true;
    }

    public boolean resetHwid(Connection connection, String username) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET hwid = NULL, updated_at = ? WHERE username = ?"
        )) {
            ps.setLong(1, now);
            ps.setString(2, username);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteUser(Connection connection, String username) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM users WHERE username = ?")) {
            ps.setString(1, username);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateExpireAt(Connection connection, String username, long expireAt) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET expire_at = ?, updated_at = ? WHERE username = ?"
        )) {
            ps.setLong(1, expireAt);
            ps.setLong(2, now);
            ps.setString(3, username);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean existsUsername(Connection connection, String username) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int getMaxCloudConfigs(Connection connection, String username) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT max_cloud_configs FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return DEFAULT_MAX_CLOUD_CONFIGS;
                }
                int v = rs.getInt(1);
                if (rs.wasNull() || v <= 0) {
                    return DEFAULT_MAX_CLOUD_CONFIGS;
                }
                return v;
            }
        } catch (SQLException e) {
            return DEFAULT_MAX_CLOUD_CONFIGS;
        }
    }

    public boolean updateMaxCloudConfigs(Connection connection, String username, int max) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE users SET max_cloud_configs = ?, updated_at = ? WHERE username = ?"
        )) {
            ps.setInt(1, Math.max(1, max));
            ps.setLong(2, now);
            ps.setString(3, username);
            return ps.executeUpdate() == 1;
        }
    }

    public int countCloudConfigs(Connection connection, String ownerUsername) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM cloud_configs WHERE owner_username = ?"
        )) {
            ps.setString(1, ownerUsername);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return 0;
                }
                return rs.getInt(1);
            }
        }
    }

    public boolean cloudConfigExists(Connection connection, String ownerUsername, String configName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM cloud_configs WHERE owner_username = ? AND config_name = ?"
        )) {
            ps.setString(1, ownerUsername);
            ps.setString(2, configName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean insertCloudConfig(Connection connection, String ownerUsername, String configName, String content) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO cloud_configs(owner_username, config_name, content, created_at, updated_at) VALUES(?,?,?,?,?)"
        )) {
            ps.setString(1, ownerUsername);
            ps.setString(2, configName);
            ps.setString(3, content == null ? "" : content);
            ps.setLong(4, now);
            ps.setLong(5, now);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateCloudConfig(Connection connection, String ownerUsername, String configName, String content) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE cloud_configs SET content = ?, updated_at = ? WHERE owner_username = ? AND config_name = ?"
        )) {
            ps.setString(1, content == null ? "" : content);
            ps.setLong(2, now);
            ps.setString(3, ownerUsername);
            ps.setString(4, configName);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteCloudConfig(Connection connection, String ownerUsername, String configName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM cloud_configs WHERE owner_username = ? AND config_name = ?"
        )) {
            ps.setString(1, ownerUsername);
            ps.setString(2, configName);
            return ps.executeUpdate() == 1;
        }
    }

    public String getCloudConfigContent(Connection connection, String ownerUsername, String configName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT content FROM cloud_configs WHERE owner_username = ? AND config_name = ?"
        )) {
            ps.setString(1, ownerUsername);
            ps.setString(2, configName);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString(1);
            }
        }
    }

    public List<String> listCloudConfigNames(Connection connection, String ownerUsername) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT config_name FROM cloud_configs WHERE owner_username = ? ORDER BY config_name ASC"
        )) {
            ps.setString(1, ownerUsername);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> out = new java.util.ArrayList<>();
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
                return out;
            }
        }
    }

    public List<CloudConfigIndexRow> listAllCloudConfigs(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT owner_username,config_name,length(content) AS size,created_at,updated_at FROM cloud_configs ORDER BY updated_at DESC"
        )) {
            try (ResultSet rs = ps.executeQuery()) {
                List<CloudConfigIndexRow> out = new java.util.ArrayList<>();
                while (rs.next()) {
                    out.add(new CloudConfigIndexRow(
                            rs.getString("owner_username"),
                            rs.getString("config_name"),
                            rs.getInt("size"),
                            rs.getLong("created_at"),
                            rs.getLong("updated_at")
                    ));
                }
                return out;
            }
        }
    }

    private static UserRow mapUserRow(ResultSet rs) throws SQLException {
        String qqJson = rs.getString("qq_json");
        Set<String> qqSet;
        try {
            qqSet = qqJson == null || qqJson.isEmpty()
                    ? Collections.emptySet()
                    : GSON.fromJson(qqJson, TypeToken.getParameterized(Set.class, String.class).getType());
        } catch (Exception ignored) {
            qqSet = Collections.emptySet();
        }

        String cardGroup = null;
        try {
            cardGroup = rs.getString("card_grp");
        } catch (SQLException ignored) {
        }

        return new UserRow(
                rs.getString("username"),
                cardGroup == null ? "" : cardGroup,
                rs.getString("password_salt"),
                rs.getString("password_hash"),
                rs.getString("hwid"),
                qqSet,
                rs.getString("phone"),
                rs.getString("prefix"),
                rs.getLong("expire_at"),
                rs.getInt("online") != 0,
                rs.getInt("banned") != 0,
                rs.getLong("created_at"),
                rs.getLong("updated_at")
        );
    }
}

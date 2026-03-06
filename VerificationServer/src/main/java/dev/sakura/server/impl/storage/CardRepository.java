package dev.sakura.server.impl.storage;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class CardRepository {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CARD_KEY_LENGTH = 24;
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    public record CardRow(
            String cardKey,
            String group,
            long durationMs,
            long createdAt,
            String usedBy,
            Long usedAt
    ) {
    }

    public String createCard(Connection connection, String group, long durationMs) throws SQLException {
        for (int i = 0; i < 20; i++) {
            String key = generateKey(durationMs);
            long now = Instant.now().toEpochMilli();
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO cards(card_key,grp,duration_ms,created_at,used_by,used_at) VALUES(?,?,?,?,NULL,NULL)"
            )) {
                ps.setString(1, key);
                ps.setString(2, group);
                ps.setLong(3, durationMs);
                ps.setLong(4, now);
                ps.executeUpdate();
                return key;
            } catch (SQLException e) {
                if (!isConstraintViolation(e)) {
                    throw e;
                }
            }
        }

        throw new SQLException("Failed to generate unique card key");
    }

    public boolean deleteCard(Connection connection, String cardKey) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM cards WHERE card_key = ?")) {
            ps.setString(1, cardKey);
            return ps.executeUpdate() == 1;
        }
    }

    public int deleteGroup(Connection connection, String group) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM cards WHERE grp = ?")) {
            ps.setString(1, group);
            return ps.executeUpdate();
        }
    }

    public String findUsedBy(Connection connection, String cardKey) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT used_by FROM cards WHERE card_key = ?")) {
            ps.setString(1, cardKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String usedBy = rs.getString(1);
                return usedBy == null || usedBy.isBlank() ? null : usedBy;
            }
        }
    }

    public List<String> listUsedByGroup(Connection connection, String group) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT DISTINCT used_by FROM cards WHERE grp = ? AND used_by IS NOT NULL AND used_by != ''"
        )) {
            ps.setString(1, group);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) {
                    String usedBy = rs.getString(1);
                    if (usedBy != null && !usedBy.isBlank()) {
                        out.add(usedBy);
                    }
                }
                return out;
            }
        }
    }

    public CardRow findUnused(Connection connection, String cardKey) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT card_key,grp,duration_ms,created_at,used_by,used_at FROM cards WHERE card_key = ? AND used_by IS NULL"
        )) {
            ps.setString(1, cardKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapRow(rs);
            }
        }
    }

    public boolean markUsed(Connection connection, String cardKey, String usedBy) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE cards SET used_by = ?, used_at = ? WHERE card_key = ? AND used_by IS NULL"
        )) {
            ps.setString(1, usedBy);
            ps.setLong(2, now);
            ps.setString(3, cardKey);
            return ps.executeUpdate() == 1;
        }
    }

    private static CardRow mapRow(ResultSet rs) throws SQLException {
        Long usedAt = rs.getObject("used_at") == null ? null : rs.getLong("used_at");
        return new CardRow(
                rs.getString("card_key"),
                rs.getString("grp"),
                rs.getLong("duration_ms"),
                rs.getLong("created_at"),
                rs.getString("used_by"),
                usedAt
        );
    }

    private static String generateKey(long durationMs) {
        long days = durationMs <= 0 ? 0 : Math.max(1L, durationMs / DAY_MS);
        String prefix = Long.toString(days) + "D-";
        int randomLen = CARD_KEY_LENGTH - prefix.length();
        if (randomLen < 8) {
            randomLen = 8;
        }
        String token = generateRandomToken(randomLen);
        String key = prefix + token;
        return key.length() <= CARD_KEY_LENGTH ? key : key.substring(0, CARD_KEY_LENGTH);
    }

    private static String generateRandomToken(int length) {
        int bytes = (int) Math.ceil(length * 0.75d);
        byte[] data = new byte[Math.max(16, bytes)];
        RANDOM.nextBytes(data);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(data);
        return token.length() <= length ? token : token.substring(0, length);
    }

    private static boolean isConstraintViolation(SQLException e) {
        String message = e.getMessage();
        return message != null && message.toLowerCase().contains("constraint");
    }
}

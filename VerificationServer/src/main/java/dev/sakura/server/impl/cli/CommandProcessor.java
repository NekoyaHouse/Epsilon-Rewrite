package dev.sakura.server.impl.cli;

import dev.sakura.server.impl.IRCServer;
import dev.sakura.server.impl.storage.CardRepository;
import dev.sakura.server.impl.storage.SqliteDatabase;
import dev.sakura.server.impl.storage.UserRepository;
import dev.sakura.server.impl.util.CardExportUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CommandProcessor {
    public record CommandResult(boolean shouldExit, List<String> lines) {
    }

    private final SqliteDatabase database;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public CommandProcessor(SqliteDatabase database, UserRepository userRepository, CardRepository cardRepository) {
        this.database = database;
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
    }

    public CommandResult execute(String line) throws Exception {
        String[] parts = line.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return new CommandResult(false, List.of());
        }
        String root = parts[0].toLowerCase(Locale.ROOT);
        if (root.equals("exit") || root.equals("quit")) {
            return new CommandResult(true, List.of("Bye"));
        }
        if (root.equals("help")) {
            return new CommandResult(false, helpLines());
        }
        if (root.equals("card")) {
            return new CommandResult(false, handleCard(parts));
        }
        if (root.equals("user")) {
            return new CommandResult(false, handleUser(parts));
        }
        if (root.equals("online")) {
            return new CommandResult(false, handleOnline(parts));
        }
        return new CommandResult(false, List.of("Unknown command. Use 'help'."));
    }

    private List<String> handleCard(String[] parts) throws Exception {
        if (parts.length < 2) {
            return List.of("Usage: card create|create-batch|delete|delete-group ...");
        }

        String sub = parts[1].toLowerCase(Locale.ROOT);
        if (sub.equals("create")) {
            if (parts.length < 4) {
                return List.of("Usage: card create <group> <days>");
            }
            String group = parts[2];
            long days = parseDays(parts[3]);
            long durationMs = daysToMillis(days);

            try (Connection connection = database.openConnection()) {
                String key = cardRepository.createCard(connection, group, durationMs);
                Path file = CardExportUtil.writeKeys(group, days, List.of(key));
                return List.of("Created card: " + key, "Output: " + file.toAbsolutePath());
            }
        }

        if (sub.equals("create-batch")) {
            if (parts.length < 5) {
                return List.of("Usage: card create-batch <group> <days> <count>");
            }

            String group = parts[2];
            long days = parseDays(parts[3]);
            long durationMs = daysToMillis(days);
            int count = Integer.parseInt(parts[4]);

            if (count <= 0) {
                return List.of("Count must be > 0");
            }

            List<String> keys = new ArrayList<>(count);
            try (Connection connection = database.openConnection()) {
                connection.setAutoCommit(false);
                try {
                    for (int i = 0; i < count; i++) {
                        keys.add(cardRepository.createCard(connection, group, durationMs));
                    }
                    connection.commit();
                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                }
            }

            Path file = CardExportUtil.writeKeys(group, days, keys);
            return List.of("Generated " + keys.size() + " cards -> " + file.toAbsolutePath());
        }

        if (sub.equals("delete")) {
            if (parts.length < 3) {
                return List.of("Usage: card delete <cardKey>");
            }
            try (Connection connection = database.openConnection()) {
                connection.setAutoCommit(false);
                try {
                    String usedBy = cardRepository.findUsedBy(connection, parts[2]);
                    if (usedBy != null) {
                        userRepository.deleteUser(connection, usedBy);
                    }
                    boolean ok = cardRepository.deleteCard(connection, parts[2]);
                    connection.commit();
                    return List.of(ok ? "Deleted." : "Not found.");
                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                }
            }
        }

        if (sub.equals("delete-group")) {
            if (parts.length < 3) {
                return List.of("Usage: card delete-group <group>");
            }
            try (Connection connection = database.openConnection()) {
                connection.setAutoCommit(false);
                try {
                    List<String> usedByList = cardRepository.listUsedByGroup(connection, parts[2]);
                    for (String usedBy : usedByList) {
                        userRepository.deleteUser(connection, usedBy);
                    }
                    int count = cardRepository.deleteGroup(connection, parts[2]);
                    connection.commit();
                    return List.of("Deleted " + count + " cards.");
                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                }
            }
        }

        return List.of("Usage: card create|create-batch|delete|delete-group ...");
    }

    private List<String> handleUser(String[] parts) throws Exception {
        if (parts.length < 2) {
            return List.of("Usage: user list|set-password|set-prefix|set-config-max|reset-hwid|find-qq|info|delete|online ...");
        }

        String sub = parts[1].toLowerCase(Locale.ROOT);
        if (sub.equals("list")) {
            try (Connection connection = database.openConnection()) {
                List<UserRepository.UserRow> users = userRepository.listAllUsers(connection);
                if (users.isEmpty()) {
                    return List.of("No user.");
                }
                List<String> out = new ArrayList<>(users.size());
                for (UserRepository.UserRow row : users) {
                    out.add(row.username() + " (online=" + row.online() + ", expireAt=" + row.expireAt() + ")");
                }
                return out;
            }
        }

        if (sub.equals("set-password")) {
            if (parts.length < 4) {
                return List.of("Usage: user set-password <username> <newPassword>");
            }
            boolean ok = IRCServer.getInstance().getAuthService().changePassword(parts[2], parts[3]);
            return List.of(ok ? "OK" : "Not found");
        }

        if (sub.equals("set-prefix")) {
            if (parts.length < 3) {
                return List.of("Usage: user set-prefix <username> <prefix>");
            }

            String username = parts[2];
            String prefix = parts.length >= 4 ? String.join(" ", java.util.Arrays.copyOfRange(parts, 3, parts.length)) : "";
            try (Connection connection = database.openConnection()) {
                boolean ok = userRepository.updatePrefix(connection, username, prefix);
                if (ok) {
                    IRCServer.getInstance().getUserManager().updatePrefix(username, prefix);
                }
                return List.of(ok ? "OK" : "Not found");
            }
        }

        if (sub.equals("set-config-max")) {
            if (parts.length < 4) {
                return List.of("Usage: user set-config-max <username> <max>");
            }

            String username = parts[2];
            int max = Integer.parseInt(parts[3]);
            try (Connection connection = database.openConnection()) {
                boolean ok = userRepository.updateMaxCloudConfigs(connection, username, max);
                return List.of(ok ? "OK" : "Not found");
            }
        }

        if (sub.equals("reset-hwid")) {
            if (parts.length < 3) {
                return List.of("Usage: user reset-hwid <username>");
            }
            try (Connection connection = database.openConnection()) {
                boolean ok = userRepository.resetHwid(connection, parts[2]);
                return List.of(ok ? "OK" : "Not found");
            }
        }

        if (sub.equals("find-qq")) {
            if (parts.length < 3) {
                return List.of("Usage: user find-qq <qq>");
            }
            try (Connection connection = database.openConnection()) {
                List<UserRepository.UserRow> users = userRepository.findByQq(connection, parts[2]);
                if (users.isEmpty()) {
                    return List.of("No user found.");
                }
                List<String> out = new ArrayList<>(users.size());
                for (UserRepository.UserRow row : users) {
                    out.add(row.username() + " (online=" + row.online() + ", expireAt=" + row.expireAt() + ")");
                }
                return out;
            }
        }

        if (sub.equals("info")) {
            if (parts.length < 3) {
                return List.of("Usage: user info <username>");
            }
            try (Connection connection = database.openConnection()) {
                UserRepository.UserRow row = userRepository.findByUsername(connection, parts[2]);
                if (row == null) {
                    return List.of("Not found");
                }
                return List.of(
                        "username=" + row.username(),
                        "expireAt=" + row.expireAt(),
                        "online=" + row.online(),
                        "hwid=" + row.hwid(),
                        "qqSet=" + row.qqSet(),
                        "phone=" + row.phone(),
                        "prefix=" + row.prefix()
                );
            }
        }

        if (sub.equals("delete")) {
            if (parts.length < 3) {
                return List.of("Usage: user delete <username>");
            }
            try (Connection connection = database.openConnection()) {
                boolean ok = userRepository.deleteUser(connection, parts[2]);
                try {
                    IRCServer srv = IRCServer.getInstance();
                    if (ok && srv != null) {
                        srv.disconnectUser(parts[2], "账号已删除");
                    }
                } catch (Exception ignored) {
                }
                return List.of(ok ? "Deleted" : "Not found");
            }
        }

        if (sub.equals("online")) {
            if (parts.length < 3) {
                return List.of("Usage: user online <username>");
            }
            try (Connection connection = database.openConnection()) {
                UserRepository.UserRow row = userRepository.findByUsername(connection, parts[2]);
                return List.of(row != null && row.online() ? "ONLINE" : "OFFLINE");
            }
        }

        return List.of("Usage: user list|set-password|set-prefix|set-config-max|reset-hwid|find-qq|info|delete|online ...");
    }

    private List<String> handleOnline(String[] parts) throws Exception {
        if (parts.length < 2 || !parts[1].equalsIgnoreCase("list")) {
            return List.of("Usage: online list");
        }

        try (Connection connection = database.openConnection()) {
            List<UserRepository.UserRow> users = userRepository.listOnlineUsers(connection);
            if (users.isEmpty()) {
                return List.of("No online user.");
            }
            List<String> out = new ArrayList<>(users.size());
            for (UserRepository.UserRow row : users) {
                out.add(row.username() + " (expireAt=" + row.expireAt() + ")");
            }
            return out;
        }
    }

    private static List<String> helpLines() {
        return List.of(
                "card create <group> <days>",
                "card create-batch <group> <days> <count>",
                "card delete <cardKey>",
                "card delete-group <group>",
                "user list",
                "user set-password <username> <newPassword>",
                "user set-prefix <username> <prefix>",
                "user set-config-max <username> <max>",
                "user reset-hwid <username>",
                "user find-qq <qq>",
                "user info <username>",
                "user delete <username>",
                "user online <username>",
                "online list",
                "exit"
        );
    }

    private static long parseDays(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.endsWith("d")) {
            s = s.substring(0, s.length() - 1);
        }
        long days = Long.parseLong(s);
        return Math.max(0L, days);
    }

    private static long daysToMillis(long days) {
        if (days <= 0) {
            return 0;
        }
        return days * 24L * 60L * 60L * 1000L;
    }
}

package dev.sakura.server.impl.user;

import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.packet.implemention.s2c.UpdateUserListS2C;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private final Map<String, User> sessionToUserMap = new ConcurrentHashMap<>();

    public Iterable<Map.Entry<String, User>> sessionToUserMapEntrySet() {
        return sessionToUserMap.entrySet();
    }

    public void removeUser(String session) {
        sessionToUserMap.remove(session);
    }

    public User getUser(String session) {
        return sessionToUserMap.get(session);
    }

    public void putUser(String session, User user) {
        sessionToUserMap.put(session, user);
    }

    public void updatePrefix(String username, String prefix) {
        if (username == null || username.isEmpty()) {
            return;
        }
        for (User user : sessionToUserMap.values()) {
            if (user != null && username.equals(user.getUsername())) {
                user.setPrefix(prefix);
            }
        }
    }

    public void syncUserList(Connection connection) {
        Map<String, String> userList = new LinkedHashMap<>();
        for (User value : sessionToUserMap.values()) {
            userList.put(value.getUsername(), value.getIgn());
        }
        UpdateUserListS2C packet = new UpdateUserListS2C(userList);
        connection.sendPacket(packet);
    }
}


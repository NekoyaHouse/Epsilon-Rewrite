package dev.sakura.server.impl.interfaces;

import dev.sakura.server.packet.IRCPacket;

public interface Connection {
    void sendPacket(IRCPacket packet);

    String getIPAddress();

    String getSessionId();

    void disconnect(String message);

    void disconnect();
}


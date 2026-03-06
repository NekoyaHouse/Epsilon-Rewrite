package dev.sakura.server.impl.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import dev.sakura.server.impl.IRCServer;

import javax.swing.*;

public final class AdminApp {
    private AdminApp() {
    }

    public static void start(IRCServer server) {
        GuiLogBuffer.install();
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new AdminFrame(server).setVisible(true));
    }
}

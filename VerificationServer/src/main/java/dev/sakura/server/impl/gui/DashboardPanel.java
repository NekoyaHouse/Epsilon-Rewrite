package dev.sakura.server.impl.gui;

import dev.sakura.server.impl.IRCServer;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DashboardPanel extends JPanel {
    private final IRCServer server;
    private final JLabel summary = new JLabel();
    private final JTextArea info = new JTextArea();

    public DashboardPanel(IRCServer server) {
        super(new BorderLayout(10, 10));
        this.server = server;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(summary, BorderLayout.NORTH);

        info.setEditable(false);
        info.setLineWrap(false);
        JScrollPane scroll = new JScrollPane(info);
        add(scroll, BorderLayout.CENTER);

        Timer timer = new Timer(1000, e -> refresh());
        timer.setRepeats(true);
        timer.start();
        refresh();
    }

    private void refresh() {
        summary.setText("连接数: " + server.getConnectionCount() + "    端口: " + server.getPort());

        LocalDateTime startedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(server.getStartedAt()), ZoneId.systemDefault());
        String startedText = startedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder sb = new StringBuilder();
        sb.append("Started At: ").append(startedText).append('\n');
        sb.append("Database: ").append(System.getProperty("sakura.verify.db", "verify.sqlite")).append('\n');
        sb.append("Java: ").append(System.getProperty("java.version")).append('\n');
        sb.append("OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append('\n');
        sb.append('\n');
        sb.append("Connections:\n");
        server.getConnectionSnapshot().forEach((sid, conn) -> sb.append(" - ").append(sid).append(" @ ").append(conn.getIPAddress()).append('\n'));
        info.setText(sb.toString());
        info.setCaretPosition(0);
    }
}


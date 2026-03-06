package dev.sakura.server.impl.gui;

import dev.sakura.server.impl.IRCServer;
import dev.sakura.server.impl.service.AdminService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class SessionsPanel extends JPanel {
    private final AdminService service;
    private final SessionsTableModel model = new SessionsTableModel();
    private final JTable table = new JTable(model);
    private final JButton refreshBtn = new JButton("刷新");
    private final JButton kickBtn = new JButton("断开");
    private final JButton kickByUserBtn = new JButton("按用户名断开");
    private final Timer timer;

    public SessionsPanel(IRCServer server) {
        super(new BorderLayout());
        this.service = new AdminService(server, server.getDatabase(), server.getUserRepository(), server.getCardRepository());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(new JLabel("当前连接会话"));
        top.add(refreshBtn);
        top.add(kickBtn);
        top.add(kickByUserBtn);
        add(top, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> refresh());
        kickBtn.addActionListener(e -> kickSelected());
        kickByUserBtn.addActionListener(e -> kickByUsername());

        timer = new Timer(2000, e -> refresh());
        timer.setRepeats(true);
        timer.start();

        refresh();
    }

    private void refresh() {
        refreshBtn.setEnabled(false);
        new SwingWorker<List<AdminService.SessionRow>, Void>() {
            @Override
            protected List<AdminService.SessionRow> doInBackground() {
                return service.listSessions();
            }

            @Override
            protected void done() {
                try {
                    model.setRows(get());
                } catch (Exception ignored) {
                } finally {
                    refreshBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void kickSelected() {
        int r = table.getSelectedRow();
        if (r < 0) {
            return;
        }
        AdminService.SessionRow row = model.getRow(r);
        if (row == null) {
            return;
        }
        if (row.username() == null || row.username().isEmpty()) {
            JOptionPane.showMessageDialog(this, "该会话未登录（缺少用户名），请使用按用户名断开或等待登录。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String reason = JOptionPane.showInputDialog(this, "原因（可空）", "断开用户: " + row.username(), JOptionPane.QUESTION_MESSAGE);
        if (reason == null) {
            return;
        }
        service.disconnectUser(row.username(), reason.isBlank() ? "Disconnected" : reason.trim());
    }

    private void kickByUsername() {
        String username = JOptionPane.showInputDialog(this, "用户名", "按用户名断开", JOptionPane.QUESTION_MESSAGE);
        if (username == null) {
            return;
        }
        String u = username.trim();
        if (u.isEmpty()) {
            return;
        }
        String reason = JOptionPane.showInputDialog(this, "原因（可空）", "断开用户: " + u, JOptionPane.QUESTION_MESSAGE);
        if (reason == null) {
            return;
        }
        service.disconnectUser(u, reason.isBlank() ? "Disconnected" : reason.trim());
    }

    private static final class SessionsTableModel extends AbstractTableModel {
        private static final String[] COLS = new String[]{"SessionId", "IP", "用户名", "IGN"};
        private final List<AdminService.SessionRow> rows = new ArrayList<>();

        public void setRows(List<AdminService.SessionRow> newRows) {
            rows.clear();
            if (newRows != null) {
                rows.addAll(newRows);
            }
            fireTableDataChanged();
        }

        public AdminService.SessionRow getRow(int viewRow) {
            if (viewRow < 0 || viewRow >= rows.size()) {
                return null;
            }
            return rows.get(viewRow);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AdminService.SessionRow r = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> r.sessionId();
                case 1 -> r.ip();
                case 2 -> r.username();
                case 3 -> r.ign();
                default -> "";
            };
        }
    }
}

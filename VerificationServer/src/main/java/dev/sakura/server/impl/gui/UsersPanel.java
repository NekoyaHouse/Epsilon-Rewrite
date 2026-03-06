package dev.sakura.server.impl.gui;

import dev.sakura.server.impl.IRCServer;
import dev.sakura.server.impl.service.AdminService;
import dev.sakura.server.impl.storage.UserRepository;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class UsersPanel extends JPanel {
    private final IRCServer server;
    private final AdminService service;
    private final UsersTableModel model = new UsersTableModel();
    private final JTable table = new JTable(model);

    private final JComboBox<String> modeBox = new JComboBox<>(new String[]{"全部", "用户名", "QQ", "在线"});
    private final JTextField queryField = new JTextField(24);
    private final JButton refreshBtn = new JButton("刷新");
    private final JButton deleteBtn = new JButton("删除用户");
    private final JButton kickBtn = new JButton("踢下线");
    private final JButton resetHwidBtn = new JButton("重置HWID");
    private final JButton setPwdBtn = new JButton("改密码");
    private final JButton setMaxBtn = new JButton("云配置上限");

    public UsersPanel(IRCServer server) {
        super(new BorderLayout());
        this.server = server;
        this.service = new AdminService(server, server.getDatabase(), server.getUserRepository(), server.getCardRepository());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(new JLabel("查询："));
        top.add(modeBox);
        top.add(queryField);
        top.add(refreshBtn);
        add(top, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(deleteBtn);
        actions.add(kickBtn);
        actions.add(resetHwidBtn);
        actions.add(setPwdBtn);
        actions.add(setMaxBtn);
        add(actions, BorderLayout.SOUTH);

        refreshBtn.addActionListener(e -> refresh());
        deleteBtn.addActionListener(e -> deleteUser());
        kickBtn.addActionListener(e -> kick());
        resetHwidBtn.addActionListener(e -> resetHwid());
        setPwdBtn.addActionListener(e -> setPassword());
        setMaxBtn.addActionListener(e -> setConfigMax());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2 || table.getSelectedRow() < 0) {
                    return;
                }
                int viewRow = table.rowAtPoint(e.getPoint());
                int viewCol = table.columnAtPoint(e.getPoint());
                if (viewRow < 0 || viewCol != 2) {
                    return;
                }
                UserRepository.UserRow row = model.getRow(viewRow);
                if (row == null || row.qqSet() == null || row.qqSet().isEmpty()) {
                    JOptionPane.showMessageDialog(UsersPanel.this, "无QQ信息", "QQ", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                String content = String.join("\n", row.qqSet());
                JOptionPane.showMessageDialog(UsersPanel.this, content, "QQ - " + row.username(), JOptionPane.INFORMATION_MESSAGE);
            }
        });

        refresh();
    }

    private void refresh() {
        String mode = String.valueOf(modeBox.getSelectedItem());
        String q = queryField.getText() == null ? "" : queryField.getText().trim();
        refreshBtn.setEnabled(false);

        new SwingWorker<List<UserRepository.UserRow>, Void>() {
            @Override
            protected List<UserRepository.UserRow> doInBackground() throws Exception {
                if (mode.equals("在线")) {
                    return service.listOnlineUsers();
                }
                if (q.isEmpty() || mode.equals("全部")) {
                    return service.listUsers();
                }
                if (mode.equals("用户名")) {
                    UserRepository.UserRow row = service.findUser(q);
                    if (row == null) {
                        return List.of();
                    }
                    return List.of(row);
                }
                if (mode.equals("QQ")) {
                    return service.findUsersByQq(q);
                }
                return service.listUsers();
            }

            @Override
            protected void done() {
                try {
                    model.setRows(get());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UsersPanel.this, "刷新失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                } finally {
                    refreshBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private String getSelectedUsername() {
        int r = table.getSelectedRow();
        if (r < 0) {
            return null;
        }
        UserRepository.UserRow row = model.getRow(r);
        return row == null ? null : row.username();
    }

    private void deleteUser() {
        String username = getSelectedUsername();
        if (username == null || username.isEmpty()) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "确定要删除用户: " + username + " ?\n该操作不可恢复。", "确认", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return service.deleteUser(username);
            }

            @Override
            protected void done() {
                try {
                    if (!get()) {
                        JOptionPane.showMessageDialog(UsersPanel.this, "删除失败", "提示", JOptionPane.WARNING_MESSAGE);
                    }
                    refresh();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UsersPanel.this, "删除失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void kick() {
        String username = getSelectedUsername();
        if (username == null || username.isEmpty()) {
            return;
        }
        String reason = JOptionPane.showInputDialog(this, "踢下线原因（可空）", "踢下线", JOptionPane.QUESTION_MESSAGE);
        if (reason == null) {
            return;
        }
        service.disconnectUser(username, reason.isBlank() ? "Disconnected" : reason.trim());
    }

    private void resetHwid() {
        String username = getSelectedUsername();
        if (username == null || username.isEmpty()) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "确定重置 HWID: " + username + " ?", "确认", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return service.resetHwid(username);
            }

            @Override
            protected void done() {
                try {
                    if (!get()) {
                        JOptionPane.showMessageDialog(UsersPanel.this, "未找到用户或重置失败", "提示", JOptionPane.WARNING_MESSAGE);
                    }
                    refresh();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UsersPanel.this, "重置失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setPassword() {
        String username = getSelectedUsername();
        if (username == null || username.isEmpty()) {
            return;
        }
        String pwd = JOptionPane.showInputDialog(this, "新密码", "改密码 - " + username, JOptionPane.QUESTION_MESSAGE);
        if (pwd == null) {
            return;
        }
        boolean ok = service.setPassword(username, pwd);
        JOptionPane.showMessageDialog(this, ok ? "OK" : "失败", "结果", ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    private void setConfigMax() {
        String username = getSelectedUsername();
        if (username == null || username.isEmpty()) {
            return;
        }
        String v = JOptionPane.showInputDialog(this, "最大云配置数量", "云配置上限 - " + username, JOptionPane.QUESTION_MESSAGE);
        if (v == null) {
            return;
        }
        int max;
        try {
            max = Integer.parseInt(v.trim());
        } catch (Exception e) {
            return;
        }
        int finalMax = max;
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return service.setCloudConfigMax(username, finalMax);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    JOptionPane.showMessageDialog(UsersPanel.this, ok ? "OK" : "失败", "结果", ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                    refresh();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UsersPanel.this, "失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private static final class UsersTableModel extends AbstractTableModel {
        private static final String[] COLS = new String[]{"用户名", "卡密组", "QQ", "到期时间", "在线", "Phone"};
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private final List<UserRepository.UserRow> rows = new ArrayList<>();

        public void setRows(List<UserRepository.UserRow> newRows) {
            rows.clear();
            if (newRows != null) {
                rows.addAll(newRows);
            }
            fireTableDataChanged();
        }

        public UserRepository.UserRow getRow(int viewRow) {
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
            UserRepository.UserRow r = rows.get(rowIndex);
            String qq = r.qqSet() == null || r.qqSet().isEmpty() ? "" : String.join(",", r.qqSet());
            return switch (columnIndex) {
                case 0 -> r.username();
                case 1 -> r.cardGroup();
                case 2 -> qq;
                case 3 -> formatExpireAt(r.expireAt());
                case 4 -> r.online();
                case 5 -> r.phone();
                default -> "";
            };
        }

        private static String formatExpireAt(long expireAt) {
            if (expireAt <= 0) {
                return "";
            }
            LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(expireAt), ZoneId.systemDefault());
            return dt.format(FMT);
        }
    }
}

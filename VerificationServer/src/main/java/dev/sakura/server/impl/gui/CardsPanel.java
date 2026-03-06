package dev.sakura.server.impl.gui;

import dev.sakura.server.impl.IRCServer;
import dev.sakura.server.impl.service.AdminService;
import dev.sakura.server.impl.storage.CardRepository;
import dev.sakura.server.impl.util.CardExportUtil;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class CardsPanel extends JPanel {
    private final AdminService service;
    private final CardsTableModel model = new CardsTableModel();
    private final JTable table = new JTable(model);

    private final JTextField groupField = new JTextField(10);
    private final JTextField daysField = new JTextField(6);
    private final JTextField countField = new JTextField(4);
    private final JButton createBtn = new JButton("创建");
    private final JButton batchBtn = new JButton("批量创建");
    private final JButton deleteBtn = new JButton("删除卡密");
    private final JButton deleteGroupBtn = new JButton("删组");
    private final JButton refreshBtn = new JButton("刷新");
    private final JComboBox<String> filterUsed = new JComboBox<>(new String[]{"全部", "未使用", "已使用"});

    public CardsPanel(IRCServer server) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        this.service = new AdminService(server, server.getDatabase(), server.getUserRepository(), server.getCardRepository());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(new JLabel("组:"));
        top.add(groupField);
        top.add(new JLabel("天数:"));
        daysField.setText("30");
        top.add(daysField);
        top.add(new JLabel("数量:"));
        countField.setText("10");
        top.add(countField);
        top.add(createBtn);
        top.add(batchBtn);
        top.add(deleteBtn);
        top.add(deleteGroupBtn);
        top.add(new JLabel("筛选:"));
        top.add(filterUsed);
        top.add(refreshBtn);
        add(top, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                CardRepository.CardRow r = model.getRow(row);
                if (r == null || r.cardKey() == null || r.cardKey().isEmpty()) {
                    return;
                }
                try {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(r.cardKey()), null);
                } catch (Exception ignored) {
                }
                JOptionPane.showMessageDialog(CardsPanel.this, "卡密已复制！", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        createBtn.addActionListener(e -> createOne());
        batchBtn.addActionListener(e -> createBatch());
        deleteBtn.addActionListener(e -> deleteOne());
        deleteGroupBtn.addActionListener(e -> deleteGroup());
        refreshBtn.addActionListener(e -> refresh());
        filterUsed.addActionListener(e -> refresh());

        refresh();
    }

    private void refresh() {
        String group = groupField.getText() == null ? "" : groupField.getText().trim();
        Boolean used = switch (String.valueOf(filterUsed.getSelectedItem())) {
            case "未使用" -> Boolean.FALSE;
            case "已使用" -> Boolean.TRUE;
            default -> null;
        };
        refreshBtn.setEnabled(false);
        new SwingWorker<List<CardRepository.CardRow>, Void>() {
            @Override
            protected List<CardRepository.CardRow> doInBackground() throws Exception {
                return service.listCards(group, used);
            }

            @Override
            protected void done() {
                try {
                    model.setRows(get());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CardsPanel.this, "刷新失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                } finally {
                    refreshBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void createOne() {
        String group = groupField.getText() == null ? "" : groupField.getText().trim();
        if (group.isEmpty()) {
            return;
        }
        int days = parseInt(daysField.getText(), 0);
        if (days <= 0) {
            return;
        }
        long durationMs = daysToMillis(days);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String key = service.createCard(group, durationMs);
                CardExportUtil.writeKeys(group, days, List.of(key));
                return key;
            }

            @Override
            protected void done() {
                try {
                    String key = get();
                    JOptionPane.showMessageDialog(CardsPanel.this, key, "创建成功", JOptionPane.INFORMATION_MESSAGE);
                    refresh();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CardsPanel.this, "创建失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void createBatch() {
        String group = groupField.getText() == null ? "" : groupField.getText().trim();
        if (group.isEmpty()) {
            return;
        }
        int days = parseInt(daysField.getText(), 0);
        if (days <= 0) {
            return;
        }
        int count = parseInt(countField.getText(), 10);
        if (count <= 0) {
            return;
        }
        long durationMs = daysToMillis(days);

        new SwingWorker<Path, Void>() {
            @Override
            protected Path doInBackground() throws Exception {
                List<String> keys = service.createCardsBatch(group, durationMs, count);
                return CardExportUtil.writeKeys(group, days, keys);
            }

            @Override
            protected void done() {
                try {
                    Path file = get();
                    JOptionPane.showMessageDialog(CardsPanel.this, "已输出到: " + file.toAbsolutePath(), "完成", JOptionPane.INFORMATION_MESSAGE);
                    refresh();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CardsPanel.this, "批量创建失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void deleteOne() {
        int r = table.getSelectedRow();
        if (r < 0) {
            return;
        }
        CardRepository.CardRow row = model.getRow(r);
        if (row == null) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "删除卡密: " + row.cardKey() + " ?", "确认", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return service.deleteCard(row.cardKey());
            }

            @Override
            protected void done() {
                try {
                    refresh();
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void deleteGroup() {
        String group = groupField.getText() == null ? "" : groupField.getText().trim();
        if (group.isEmpty()) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "删除组内所有卡密: " + group + " ?", "确认", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return service.deleteCardGroup(group);
            }

            @Override
            protected void done() {
                try {
                    JOptionPane.showMessageDialog(CardsPanel.this, "已删除 " + get() + " 张", "完成", JOptionPane.INFORMATION_MESSAGE);
                    refresh();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CardsPanel.this, "删除失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static long daysToMillis(int days) {
        return Math.max(0L, (long) days) * 24L * 60L * 60L * 1000L;
    }

    private static final class CardsTableModel extends AbstractTableModel {
        private static final String[] COLS = new String[]{"卡密", "组", "天数", "创建时间", "使用者", "使用时间"};
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private final List<CardRepository.CardRow> rows = new ArrayList<>();

        public void setRows(List<CardRepository.CardRow> newRows) {
            rows.clear();
            if (newRows != null) {
                rows.addAll(newRows);
            }
            fireTableDataChanged();
        }

        public CardRepository.CardRow getRow(int viewRow) {
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
            CardRepository.CardRow r = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> r.cardKey();
                case 1 -> r.group();
                case 2 -> millisToDays(r.durationMs());
                case 3 -> formatTime(r.createdAt());
                case 4 -> r.usedBy();
                case 5 -> r.usedAt() == null ? "" : formatTime(r.usedAt());
                default -> "";
            };
        }

        private static long millisToDays(long ms) {
            if (ms <= 0) {
                return 0;
            }
            return ms / (24L * 60L * 60L * 1000L);
        }

        private static String formatTime(long epochMillis) {
            LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
            return dt.format(FMT);
        }
    }

}

package dev.sakura.server.impl.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.sakura.server.impl.IRCServer;
import dev.sakura.server.impl.service.AdminService;
import dev.sakura.server.impl.storage.UserRepository;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CloudConfigPanel extends JPanel {
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    private final AdminService service;
    private final JButton refreshBtn = new JButton("刷新");
    private final JButton saveBtn = new JButton("保存");
    private final JButton deleteBtn = new JButton("删除");
    private final JButton importBtn = new JButton("导入");
    private final JButton exportBtn = new JButton("导出");

    private final CloudConfigTableModel model = new CloudConfigTableModel();
    private final JTable table = new JTable(model);
    private final JTextArea editor = new JTextArea();
    private volatile String currentLoadedName = "";
    private volatile String currentLoadedOwner = "";

    public CloudConfigPanel(IRCServer server) {
        super(new BorderLayout());
        this.service = new AdminService(server, server.getDatabase(), server.getUserRepository(), server.getCardRepository());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(refreshBtn);
        top.add(saveBtn);
        top.add(deleteBtn);
        top.add(importBtn);
        top.add(exportBtn);
        add(top, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        editor.setTabSize(4);

        JScrollPane left = new JScrollPane(table);
        JScrollPane right = new JScrollPane(editor);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.45);
        add(split, BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> refreshList());
        saveBtn.addActionListener(e -> saveCurrent());
        deleteBtn.addActionListener(e -> deleteCurrent());
        importBtn.addActionListener(e -> importFromFile());
        exportBtn.addActionListener(e -> exportToFile());

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() != 2 || table.getSelectedRow() < 0) {
                    return;
                }
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow < 0) {
                    return;
                }
                UserRepository.CloudConfigIndexRow row = model.getRow(viewRow);
                if (row == null) {
                    return;
                }
                loadConfig(row.ownerUsername(), row.configName());
            }
        });

        refreshList();
    }

    private void refreshList() {
        refreshBtn.setEnabled(false);
        new SwingWorker<List<UserRepository.CloudConfigIndexRow>, Void>() {
            @Override
            protected List<UserRepository.CloudConfigIndexRow> doInBackground() throws Exception {
                return service.listAllCloudConfigs();
            }

            @Override
            protected void done() {
                try {
                    model.setRows(get());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CloudConfigPanel.this, "获取列表失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                } finally {
                    refreshBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void loadConfig(String owner, String name) {
        if (owner == null || owner.isBlank() || name == null || name.isBlank()) {
            return;
        }
        refreshBtn.setEnabled(false);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return service.getCloudConfigContent(owner, name);
            }

            @Override
            protected void done() {
                try {
                    String content = get();
                    if (content == null) {
                        JOptionPane.showMessageDialog(CloudConfigPanel.this, "配置不存在", "提示", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    currentLoadedOwner = owner;
                    currentLoadedName = name;
                    editor.setText(prettyJsonOrRaw(content));
                    editor.setCaretPosition(0);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CloudConfigPanel.this, "加载失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                } finally {
                    refreshBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void saveCurrent() {
        String owner = currentLoadedOwner;
        String name = currentLoadedName;
        if (owner.isEmpty() || name.isEmpty()) {
            return;
        }
        String content = editor.getText();
        saveBtn.setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return service.saveCloudConfig(owner, name, content == null ? "" : content);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (!ok) {
                        int max = service.getCloudConfigMax(owner);
                        JOptionPane.showMessageDialog(CloudConfigPanel.this, "保存失败（可能已达到上限: " + max + "）", "提示", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    refreshList();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CloudConfigPanel.this, "保存失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                } finally {
                    saveBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void deleteCurrent() {
        String owner = currentLoadedOwner;
        String name = currentLoadedName;
        if (owner.isEmpty() || name.isEmpty()) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "确定删除 " + owner + "/" + name + " ?", "确认", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        deleteBtn.setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return service.deleteCloudConfig(owner, name);
            }

            @Override
            protected void done() {
                try {
                    boolean deleted = get();
                    if (!deleted) {
                        JOptionPane.showMessageDialog(CloudConfigPanel.this, "配置不存在", "提示", JOptionPane.WARNING_MESSAGE);
                    }
                    currentLoadedOwner = "";
                    currentLoadedName = "";
                    editor.setText("");
                    refreshList();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CloudConfigPanel.this, "删除失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                } finally {
                    deleteBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void importFromFile() {
        try {
            JFileChooser chooser = new JFileChooser();
            int r = chooser.showOpenDialog(this);
            if (r != JFileChooser.APPROVE_OPTION) {
                return;
            }
            Path p = chooser.getSelectedFile().toPath();
            editor.setText(Files.readString(p, StandardCharsets.UTF_8));
            editor.setCaretPosition(0);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "导入失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportToFile() {
        try {
            JFileChooser chooser = new JFileChooser();
            int r = chooser.showSaveDialog(this);
            if (r != JFileChooser.APPROVE_OPTION) {
                return;
            }
            Path p = chooser.getSelectedFile().toPath();
            Files.writeString(p, editor.getText() == null ? "" : editor.getText(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "导出失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String prettyJsonOrRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            JsonElement el = JsonParser.parseString(raw);
            return PRETTY_GSON.toJson(el);
        } catch (Exception ignored) {
            return raw;
        }
    }

    private static final class CloudConfigTableModel extends AbstractTableModel {
        private static final String[] COLS = new String[]{"Owner", "Name", "Size", "UpdatedAt"};
        private final java.util.List<UserRepository.CloudConfigIndexRow> rows = new java.util.ArrayList<>();

        public void setRows(java.util.List<UserRepository.CloudConfigIndexRow> newRows) {
            rows.clear();
            if (newRows != null) {
                rows.addAll(newRows);
            }
            fireTableDataChanged();
        }

        public UserRepository.CloudConfigIndexRow getRow(int viewRow) {
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
            UserRepository.CloudConfigIndexRow r = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> r.ownerUsername();
                case 1 -> r.configName();
                case 2 -> r.size();
                case 3 -> AdminService.formatEpochMillis(r.updatedAt());
                default -> "";
            };
        }
    }
}


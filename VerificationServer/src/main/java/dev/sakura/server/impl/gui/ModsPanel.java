package dev.sakura.server.impl.gui;

import dev.sakura.server.impl.IRCServer;
import dev.sakura.server.impl.management.ModManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;

public class ModsPanel extends JPanel {
    private final IRCServer server;
    private final DefaultListModel<ModManager.ModInfo> listModel = new DefaultListModel<>();
    private final JList<ModManager.ModInfo> modList = new JList<>(listModel);

    public ModsPanel(IRCServer server) {
        this.server = server;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(buildListPanel());
        splitPane.setRightComponent(buildUploadPanel());
        splitPane.setResizeWeight(0.4);
        add(splitPane, BorderLayout.CENTER);

        refreshList();
    }

    private JPanel buildListPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("已安装的 Mod"));

        modList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p.add(new JScrollPane(modList), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshBtn = new JButton("刷新");
        refreshBtn.addActionListener(e -> refreshList());

        JButton deleteBtn = new JButton("删除选中");
        deleteBtn.addActionListener(e -> {
            ModManager.ModInfo selected = modList.getSelectedValue();
            if (selected != null) {
                int confirm = JOptionPane.showConfirmDialog(this, "确定删除 " + selected + " 吗？", "确认删除", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    ModManager.deleteMod(selected.name, selected.version);
                    refreshList();
                }
            }
        });

        btnPanel.add(refreshBtn);
        btnPanel.add(deleteBtn);
        p.add(btnPanel, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildUploadPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createTitledBorder("上传新版本"));

        JPanel form = new JPanel(new GridLayout(3, 2, 5, 5));
        JTextField nameField = new JTextField();
        JTextField versionField = new JTextField();

        form.add(new JLabel("Mod 名称:"));
        form.add(nameField);
        form.add(new JLabel("版本号:"));
        form.add(versionField);

        p.add(form, BorderLayout.NORTH);

        JLabel dropLabel = new JLabel("拖拽 .jar 文件到此处", SwingConstants.CENTER);
        dropLabel.setBorder(BorderFactory.createDashedBorder(Color.GRAY, 2, 5));
        dropLabel.setOpaque(true);
        dropLabel.setBackground(new Color(240, 240, 240));
        dropLabel.setPreferredSize(new Dimension(200, 200));

        new DropTarget(dropLabel, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!droppedFiles.isEmpty()) {
                        File file = droppedFiles.get(0);
                        if (file.getName().endsWith(".jar")) {
                            String name = nameField.getText().trim();
                            String version = versionField.getText().trim();

                            if (name.isEmpty() || version.isEmpty()) {
                                JOptionPane.showMessageDialog(ModsPanel.this, "请先输入 Mod 名称和版本号！");
                                return;
                            }

                            ModManager.addMod(file, name, version);
                            refreshList();
                            JOptionPane.showMessageDialog(ModsPanel.this, "上传成功！");
                        } else {
                            JOptionPane.showMessageDialog(ModsPanel.this, "只支持 .jar 文件");
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ModsPanel.this, "上传失败: " + ex.getMessage());
                }
            }
        });

        p.add(dropLabel, BorderLayout.CENTER);

        JTextArea tip = new JTextArea("提示：请先在上方输入名称和版本，然后拖入文件。\n重复的名称和版本将覆盖旧文件。");
        tip.setEditable(false);
        tip.setLineWrap(true);
        tip.setOpaque(false);
        tip.setFont(tip.getFont().deriveFont(11f));
        p.add(tip, BorderLayout.SOUTH);

        return p;
    }

    private void refreshList() {
        listModel.clear();
        for (ModManager.ModInfo info : ModManager.listMods()) {
            listModel.addElement(info);
        }
    }
}

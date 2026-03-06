package dev.sakura.server.impl.gui;

import dev.sakura.server.impl.IRCServer;
import dev.sakura.server.impl.cli.CommandProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public final class ConsolePanel extends JPanel {
    private final CommandProcessor processor;
    private final JTextArea output = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton sendBtn = new JButton("执行");
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;

    private final JTextArea logs = new JTextArea();
    private final Timer logTimer;

    public ConsolePanel(IRCServer server) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        this.processor = new CommandProcessor(server.getDatabase(), server.getUserRepository(), server.getCardRepository());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("命令", buildCommandTab());
        tabs.addTab("日志", buildLogsTab());
        add(tabs, BorderLayout.CENTER);

        logTimer = new Timer(800, e -> refreshLogs());
        logTimer.setRepeats(true);
        logTimer.start();
    }

    private JPanel buildCommandTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));

        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        output.setLineWrap(false);
        p.add(new JScrollPane(output), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.add(new JLabel("Command:"), BorderLayout.WEST);
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);
        p.add(bottom, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> submit());
        input.addActionListener(e -> submit());
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (history.isEmpty()) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (historyIndex < 0) {
                        historyIndex = history.size() - 1;
                    } else {
                        historyIndex = Math.max(0, historyIndex - 1);
                    }
                    input.setText(history.get(historyIndex));
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (historyIndex < 0) {
                        return;
                    }
                    historyIndex = Math.min(history.size() - 1, historyIndex + 1);
                    input.setText(history.get(historyIndex));
                }
            }
        });

        appendLine("Type 'help' to list commands.");
        return p;
    }

    private JPanel buildLogsTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        logs.setEditable(false);
        logs.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logs.setLineWrap(false);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(new JLabel("当前进程输出（捕获 System.out/System.err）"));
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(logs), BorderLayout.CENTER);
        return p;
    }

    private void submit() {
        String line = input.getText();
        if (line == null) {
            return;
        }
        String cmd = line.trim();
        if (cmd.isEmpty()) {
            return;
        }
        history.add(cmd);
        historyIndex = -1;
        input.setText("");
        sendBtn.setEnabled(false);

        appendLine("> " + cmd);
        new SwingWorker<CommandProcessor.CommandResult, Void>() {
            @Override
            protected CommandProcessor.CommandResult doInBackground() throws Exception {
                return processor.execute(cmd);
            }

            @Override
            protected void done() {
                try {
                    CommandProcessor.CommandResult res = get();
                    for (String l : res.lines()) {
                        appendLine(l);
                    }
                } catch (Exception e) {
                    appendLine("ERROR: " + e.getMessage());
                } finally {
                    sendBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void appendLine(String s) {
        output.append((s == null ? "" : s) + "\n");
        output.setCaretPosition(output.getDocument().getLength());
    }

    private void refreshLogs() {
        List<String> snap = GuiLogBuffer.snapshot();
        StringBuilder sb = new StringBuilder();
        for (String l : snap) {
            sb.append(l).append('\n');
        }
        logs.setText(sb.toString());
        logs.setCaretPosition(logs.getDocument().getLength());
    }
}

package dev.sakura.server.impl.gui;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GuiLogBuffer {
    private static final int MAX_LINES = 2000;
    private static final Object lock = new Object();
    private static final ArrayList<String> lines = new ArrayList<>(MAX_LINES);
    private static final StringBuilder currentLine = new StringBuilder();
    private static volatile boolean installed;

    private GuiLogBuffer() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        PrintStream out = System.out;
        PrintStream err = System.err;
        System.setOut(new PrintStream(new TeeOutputStream(out, new BufferOutputStream()), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new TeeOutputStream(err, new BufferOutputStream()), true, StandardCharsets.UTF_8));
    }

    public static List<String> snapshot() {
        synchronized (lock) {
            return new ArrayList<>(lines);
        }
    }

    private static void appendBytes(byte[] b, int off, int len) {
        String s = new String(b, off, len, StandardCharsets.UTF_8);
        synchronized (lock) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '\r') {
                    continue;
                }
                if (c == '\n') {
                    flushLineLocked();
                } else {
                    currentLine.append(c);
                    if (currentLine.length() > 8192) {
                        flushLineLocked();
                    }
                }
            }
        }
    }

    private static void flushLineLocked() {
        String line = currentLine.toString();
        currentLine.setLength(0);
        lines.add(line);
        if (lines.size() > MAX_LINES) {
            lines.remove(0);
        }
    }

    private static final class BufferOutputStream extends OutputStream {
        @Override
        public void write(int b) {
            appendBytes(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            if (b == null || len <= 0) {
                return;
            }
            appendBytes(b, off, len);
        }
    }

    private static final class TeeOutputStream extends OutputStream {
        private final OutputStream a;
        private final OutputStream b;

        private TeeOutputStream(OutputStream a, OutputStream b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void write(int v) {
            try {
                a.write(v);
            } catch (Exception ignored) {
            }
            try {
                b.write(v);
            } catch (Exception ignored) {
            }
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            try {
                a.write(buf, off, len);
            } catch (Exception ignored) {
            }
            try {
                b.write(buf, off, len);
            } catch (Exception ignored) {
            }
        }

        @Override
        public void flush() {
            try {
                a.flush();
            } catch (Exception ignored) {
            }
            try {
                b.flush();
            } catch (Exception ignored) {
            }
        }
    }
}


package dev.maru.loader.security;

import niurendeobf.ZKMIndy;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ZKMIndy
public final class AntiDebug {
    private static final AntiDebug INSTANCE = new AntiDebug();
    
    private static final Pattern DEBUG_ARG_PATTERN = Pattern.compile("-agentlib:jdwp(=|$)");
    private static final Pattern XDEBUG_PATTERN = Pattern.compile("-Xrunjdwp(:|$)");
    private static final Pattern JAVA_TOOL_OPTIONS = Pattern.compile("-agentlib:jdwp", Pattern.CASE_INSENSITIVE);
    
    private final AtomicBoolean detected = new AtomicBoolean(false);
    private final AtomicInteger detectionCount = new AtomicInteger(0);
    private volatile Thread monitorThread = null;
    private volatile boolean monitoring = false;

    private AntiDebug() {
    }

    public static AntiDebug getInstance() {
        return INSTANCE;
    }

    public static class DebugDetectionResult {
        private final boolean debuggerDetected;
        private final boolean agentDetected;
        private final boolean suspiciousProcess;
        private final boolean timingAttack;
        private final String details;

        private DebugDetectionResult(boolean debuggerDetected, boolean agentDetected, 
                                      boolean suspiciousProcess, boolean timingAttack, String details) {
            this.debuggerDetected = debuggerDetected;
            this.agentDetected = agentDetected;
            this.suspiciousProcess = suspiciousProcess;
            this.timingAttack = timingAttack;
            this.details = details;
        }

        public boolean isDebuggerDetected() { return debuggerDetected; }
        public boolean isAgentDetected() { return agentDetected; }
        public boolean isSuspiciousProcess() { return suspiciousProcess; }
        public boolean isTimingAttack() { return timingAttack; }
        public String getDetails() { return details; }
        
        public boolean isSafe() {
            return !debuggerDetected && !agentDetected && !suspiciousProcess && !timingAttack;
        }
    }

    public DebugDetectionResult performCheck() {
        StringBuilder details = new StringBuilder();
        boolean debugger = checkDebugger();
        boolean agent = checkAgent();
        boolean process = checkSuspiciousProcesses();
        boolean timing = checkTiming();
        
        if (debugger) details.append("Debugger detected. ");
        if (agent) details.append("Agent detected. ");
        if (process) details.append("Suspicious process. ");
        if (timing) details.append("Timing anomaly. ");
        
        boolean anyDetected = debugger || agent || process || timing;
        if (anyDetected) {
            detected.set(true);
            detectionCount.incrementAndGet();
        }
        
        return new DebugDetectionResult(debugger, agent, process, timing, details.toString().trim());
    }

    private boolean checkDebugger() {
        try {
            List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
            for (String arg : args) {
                if (DEBUG_ARG_PATTERN.matcher(arg).find() || XDEBUG_PATTERN.matcher(arg).find()) {
                    return true;
                }
            }
            
            String toolOptions = System.getenv("JAVA_TOOL_OPTIONS");
            if (toolOptions != null && JAVA_TOOL_OPTIONS.matcher(toolOptions).find()) {
                return true;
            }
            
            String jdkOptions = System.getenv("JDK_JAVA_OPTIONS");
            if (jdkOptions != null && JAVA_TOOL_OPTIONS.matcher(jdkOptions).find()) {
                return true;
            }
        } catch (Exception ignored) {
        }
        
        return false;
    }

    private boolean checkAgent() {
        try {
            Class<?> instrumentationClass = Class.forName("java.lang.instrument.Instrumentation");
            
            try {
                Class<?> byteBuddyAgent = Class.forName("net.bytebuddy.agent.ByteBuddyAgent");
                return true;
            } catch (ClassNotFoundException ignored) {
            }
            
            try {
                for (Class<?> clazz : instrumentationClass.getClassLoader().loadClass("java.lang.Class").getClasses()) {
                    if (clazz.getName().contains("Instrumentation")) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
            }
            
        } catch (ClassNotFoundException ignored) {
        }
        
        try {
            Thread[] threads = new Thread[Thread.activeCount() * 2];
            int count = Thread.enumerate(threads);
            for (int i = 0; i < count; i++) {
                Thread t = threads[i];
                if (t != null) {
                    String name = t.getName().toLowerCase();
                    if (name.contains("debug") || name.contains("jdi") || 
                        name.contains("jdwp") || name.contains("attach")) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        
        return false;
    }

    private boolean checkSuspiciousProcesses() {
        String[] suspiciousProcesses = {
            "jdb", "jconsole", "jvisualvm", "jmc",
            "idea", "eclipse", "netbeans", "jprofiler",
            "yourkit", "jstack", "jmap", "visualvm",
            "rejava", "bytecode-viewer", "jadx", "jd-gui",
            "fernflower", "procyon", "cfr", "krakatau",
            "processhacker", "procmon", "procexp"
        };
        
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            ProcessBuilder pb;
            
            if (os.contains("win")) {
                pb = new ProcessBuilder("tasklist");
            } else {
                pb = new ProcessBuilder("ps", "-e");
            }
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            
            String line;
            while ((line = reader.readLine()) != null) {
                String lowerLine = line.toLowerCase();
                for (String suspicious : suspiciousProcesses) {
                    if (lowerLine.contains(suspicious.toLowerCase())) {
                        process.destroy();
                        return true;
                    }
                }
            }
            
            process.destroy();
        } catch (Exception ignored) {
        }
        
        return false;
    }

    private boolean checkTiming() {
        try {
            long start = System.nanoTime();
            
            for (int i = 0; i < 1000; i++) {
                Math.sqrt(i);
            }
            
            long elapsed = System.nanoTime() - start;
            
            if (elapsed > 100_000_000) {
                return true;
            }
            
            long start2 = System.nanoTime();
            Thread.sleep(10);
            long elapsed2 = System.nanoTime() - start2;
            
            if (elapsed2 > 100_000_000) {
                return true;
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        } catch (Exception e) {
            return true;
        }
        
        return false;
    }

    public void startMonitoring(long intervalMs, DebugCallback callback) {
        if (monitoring) {
            return;
        }
        
        monitoring = true;
        monitorThread = new Thread(() -> {
            while (monitoring) {
                try {
                    DebugDetectionResult result = performCheck();
                    if (!result.isSafe() && callback != null) {
                        callback.onDebugDetected(result);
                    }
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Lumin-AntiDebug-Monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public void stopMonitoring() {
        monitoring = false;
        if (monitorThread != null) {
            monitorThread.interrupt();
            monitorThread = null;
        }
    }

    public boolean isDetected() {
        return detected.get();
    }

    public int getDetectionCount() {
        return detectionCount.get();
    }

    public interface DebugCallback {
        void onDebugDetected(DebugDetectionResult result);
    }

    public static void exitIfDebugged() {
        DebugDetectionResult result = getInstance().performCheck();
        if (!result.isSafe()) {
            System.exit(-1);
        }
    }

    public static void exitIfDebugged(DebugCallback callback) {
        DebugDetectionResult result = getInstance().performCheck();
        if (!result.isSafe()) {
            if (callback != null) {
                callback.onDebugDetected(result);
            }
            System.exit(-1);
        }
    }
}

package dev.maru.loader.security;

import dev.maru.verify.util.CryptoUtil;
import niurendeobf.ZKMIndy;
import org.objectweb.asm.ClassReader;

import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@ZKMIndy
public final class IntegrityChecker {
    private static final IntegrityChecker INSTANCE = new IntegrityChecker();
    
    private final Map<String, byte[]> classHashes = new ConcurrentHashMap<>();
    private final Map<String, byte[]> originalBytecode = new ConcurrentHashMap<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicReference<Instrumentation> instrumentation = new AtomicReference<>(null);
    
    private volatile Thread checkerThread = null;
    private volatile boolean checking = false;

    private IntegrityChecker() {
    }

    public static IntegrityChecker getInstance() {
        return INSTANCE;
    }

    public void registerClass(String className, byte[] bytecode) {
        if (className == null || bytecode == null) return;
        
        byte[] hash = CryptoUtil.sha256(bytecode);
        classHashes.put(className, hash);
        originalBytecode.put(className, Arrays.copyOf(bytecode, bytecode.length));
    }

    public void registerClasses(Map<String, byte[]> classes) {
        if (classes == null) return;
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            registerClass(entry.getKey(), entry.getValue());
        }
    }

    public boolean verifyClass(String className) {
        byte[] expectedHash = classHashes.get(className);
        if (expectedHash == null) {
            return true;
        }
        
        try {
            String resourceName = className.replace('.', '/') + ".class";
            InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName);
            if (is == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
            }
            if (is == null) {
                return false;
            }
            
            byte[] currentBytecode = is.readAllBytes();
            is.close();
            
            byte[] currentHash = CryptoUtil.sha256(currentBytecode);
            return MessageDigest.isEqual(expectedHash, currentHash);
            
        } catch (Exception e) {
            return false;
        }
    }

    public boolean verifyClassByInstrumentation(String className) {
        Instrumentation inst = instrumentation.get();
        if (inst == null) {
            return verifyClass(className);
        }
        
        try {
            Class<?>[] loadedClasses = inst.getAllLoadedClasses();
            for (Class<?> clazz : loadedClasses) {
                if (clazz.getName().equals(className)) {
                    byte[] expectedHash = classHashes.get(className);
                    if (expectedHash == null) {
                        return true;
                    }
                    
                    byte[] currentBytecode = getClassBytecode(clazz);
                    if (currentBytecode == null) {
                        return false;
                    }
                    
                    byte[] currentHash = CryptoUtil.sha256(currentBytecode);
                    return MessageDigest.isEqual(expectedHash, currentHash);
                }
            }
        } catch (Exception e) {
            return false;
        }
        
        return true;
    }

    private byte[] getClassBytecode(Class<?> clazz) {
        try {
            String resourceName = clazz.getName().replace('.', '/') + ".class";
            InputStream is = clazz.getClassLoader().getResourceAsStream(resourceName);
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public IntegrityReport verifyAll() {
        IntegrityReport report = new IntegrityReport();
        
        for (String className : classHashes.keySet()) {
            boolean valid = verifyClass(className);
            if (!valid) {
                report.addViolation(className, "Hash mismatch");
            }
        }
        
        return report;
    }

    public void setInstrumentation(Instrumentation inst) {
        instrumentation.set(inst);
    }

    public void startPeriodicCheck(long intervalMs, IntegrityCallback callback) {
        if (checking) return;
        
        checking = true;
        checkerThread = new Thread(() -> {
            while (checking) {
                try {
                    IntegrityReport report = verifyAll();
                    if (!report.isClean() && callback != null) {
                        callback.onViolation(report);
                    }
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Lumin-Integrity-Checker");
        checkerThread.setDaemon(true);
        checkerThread.start();
    }

    public void stopPeriodicCheck() {
        checking = false;
        if (checkerThread != null) {
            checkerThread.interrupt();
            checkerThread = null;
        }
    }

    public void markInitialized() {
        initialized.set(true);
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public void clear() {
        classHashes.clear();
        for (byte[] bytes : originalBytecode.values()) {
            Arrays.fill(bytes, (byte) 0);
        }
        originalBytecode.clear();
        initialized.set(false);
    }

    public static class IntegrityReport {
        private final Map<String, String> violations = new ConcurrentHashMap<>();
        
        public void addViolation(String className, String reason) {
            violations.put(className, reason);
        }
        
        public Map<String, String> getViolations() {
            return violations;
        }
        
        public boolean isClean() {
            return violations.isEmpty();
        }
        
        public int getViolationCount() {
            return violations.size();
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("IntegrityReport{violations=").append(violations.size()).append("}");
            if (!violations.isEmpty()) {
                sb.append("\n");
                for (Map.Entry<String, String> entry : violations.entrySet()) {
                    sb.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
            return sb.toString();
        }
    }

    public interface IntegrityCallback {
        void onViolation(IntegrityReport report);
    }

    public static void exitOnViolation() {
        IntegrityReport report = getInstance().verifyAll();
        if (!report.isClean()) {
            System.exit(-1);
        }
    }

    public static void exitOnViolation(IntegrityCallback callback) {
        IntegrityReport report = getInstance().verifyAll();
        if (!report.isClean()) {
            if (callback != null) {
                callback.onViolation(report);
            }
            System.exit(-1);
        }
    }
}

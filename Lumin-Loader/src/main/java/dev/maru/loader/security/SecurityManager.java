package dev.maru.loader.security;

import dev.maru.loader.CloudJarManager;
import dev.maru.loader.MemoryJarContents;
import niurendeobf.ZKMIndy;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@ZKMIndy
public final class SecurityManager {
    private static final SecurityManager INSTANCE = new SecurityManager();
    
    private static final long DEFAULT_CHECK_INTERVAL = 5000L;
    
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicReference<SecurityConfig> config = new AtomicReference<>(new SecurityConfig());
    
    private volatile AntiDebug.DebugCallback debugCallback = null;
    private volatile IntegrityChecker.IntegrityCallback integrityCallback = null;

    private SecurityManager() {
    }

    public static SecurityManager getInstance() {
        return INSTANCE;
    }

    public static class SecurityConfig {
        private boolean enableAntiDebug = true;
        private boolean enableEncryptedStorage = true;
        private boolean enableIntegrityCheck = true;
        private boolean enableClassLoadHook = true;
        private boolean exitOnViolation = true;
        private long checkIntervalMs = DEFAULT_CHECK_INTERVAL;
        
        public SecurityConfig enableAntiDebug(boolean enable) {
            this.enableAntiDebug = enable;
            return this;
        }
        
        public SecurityConfig enableEncryptedStorage(boolean enable) {
            this.enableEncryptedStorage = enable;
            return this;
        }
        
        public SecurityConfig enableIntegrityCheck(boolean enable) {
            this.enableIntegrityCheck = enable;
            return this;
        }
        
        public SecurityConfig enableClassLoadHook(boolean enable) {
            this.enableClassLoadHook = enable;
            return this;
        }
        
        public SecurityConfig exitOnViolation(boolean exit) {
            this.exitOnViolation = exit;
            return this;
        }
        
        public SecurityConfig checkIntervalMs(long ms) {
            this.checkIntervalMs = ms;
            return this;
        }
    }

    public void initialize(SecurityConfig config) {
        if (initialized.compareAndSet(false, true)) {
            this.config.set(config);
            
            if (config.enableAntiDebug) {
                initializeAntiDebug();
            }
            
            if (config.enableClassLoadHook) {
                initializeClassLoadHook();
            }
            
            if (config.enableIntegrityCheck) {
                initializeIntegrityCheck();
            }
        }
    }

    public void initialize() {
        initialize(new SecurityConfig());
    }

    private void initializeAntiDebug() {
        AntiDebug antiDebug = AntiDebug.getInstance();
        
        AntiDebug.DebugDetectionResult result = antiDebug.performCheck();
        if (!result.isSafe()) {
            handleSecurityViolation("Debug detected: " + result.getDetails());
            return;
        }
        
        debugCallback = debugResult -> {
            handleSecurityViolation("Debug detected during monitoring: " + debugResult.getDetails());
        };
        
        antiDebug.startMonitoring(config.get().checkIntervalMs, debugCallback);
    }

    private void initializeClassLoadHook() {
        try {
            ClassLoadHook hook = ClassLoadHook.getInstance();
            hook.installAgent();
        } catch (Exception e) {
            handleSecurityViolation("Failed to install class load hook: " + e.getMessage());
        }
    }

    private void initializeIntegrityCheck() {
        IntegrityChecker checker = IntegrityChecker.getInstance();
        
        integrityCallback = report -> {
            handleSecurityViolation("Integrity violation detected: " + report.getViolationCount() + " classes modified");
        };
        
        checker.startPeriodicCheck(config.get().checkIntervalMs, integrityCallback);
    }

    public void registerCloudJar(byte[] jarBytes, String identifier, Map<String, byte[]> classBytes) {
        SecurityConfig cfg = config.get();
        
        if (cfg.enableEncryptedStorage) {
            EncryptedMemoryStorage storage = EncryptedMemoryStorage.getInstance();
            storage.storeEncrypted(jarBytes, identifier);
        } else {
            CloudJarManager.getInstance().setJarBytes(jarBytes, identifier);
        }
        
        if (cfg.enableIntegrityCheck && classBytes != null) {
            IntegrityChecker.getInstance().registerClasses(classBytes);
        }
        
        if (cfg.enableClassLoadHook && classBytes != null) {
            ClassLoadHook.getInstance().registerCloudClasses(classBytes);
        }
    }

    public byte[] getDecryptedJarBytes() {
        SecurityConfig cfg = config.get();
        
        if (cfg.enableEncryptedStorage) {
            return EncryptedMemoryStorage.getInstance().retrieveDecrypted();
        } else {
            return CloudJarManager.getInstance().getJarBytes();
        }
    }

    public MemoryJarContents createSecureJarContents() {
        byte[] jarBytes = getDecryptedJarBytes();
        String identifier;
        
        SecurityConfig cfg = config.get();
        if (cfg.enableEncryptedStorage) {
            identifier = EncryptedMemoryStorage.getInstance().getIdentifier();
        } else {
            identifier = CloudJarManager.getInstance().getJarIdentifier();
        }
        
        if (jarBytes == null || jarBytes.length == 0) {
            return null;
        }
        
        return new MemoryJarContents(jarBytes, identifier != null ? identifier : "secure-jar");
    }

    private void handleSecurityViolation(String message) {
        SecurityConfig cfg = config.get();
        
        System.err.println("[SecurityManager] " + message);
        
        if (cfg.exitOnViolation) {
            cleanup();
            System.exit(-1);
        }
    }

    public void cleanup() {
        AntiDebug antiDebug = AntiDebug.getInstance();
        antiDebug.stopMonitoring();
        
        IntegrityChecker checker = IntegrityChecker.getInstance();
        checker.stopPeriodicCheck();
        checker.clear();
        
        EncryptedMemoryStorage storage = EncryptedMemoryStorage.getInstance();
        storage.clearAll();
        
        ClassLoadHook hook = ClassLoadHook.getInstance();
        hook.clear();
        
        CloudJarManager manager = CloudJarManager.getInstance();
        manager.clear();
        
        initialized.set(false);
    }

    public boolean performSecurityCheck() {
        SecurityConfig cfg = config.get();
        
        if (cfg.enableAntiDebug) {
            AntiDebug.DebugDetectionResult result = AntiDebug.getInstance().performCheck();
            if (!result.isSafe()) {
                return false;
            }
        }
        
        if (cfg.enableIntegrityCheck) {
            IntegrityChecker.IntegrityReport report = IntegrityChecker.getInstance().verifyAll();
            if (!report.isClean()) {
                return false;
            }
        }
        
        return true;
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public static void quickInit() {
        getInstance().initialize();
    }

    public static void quickInit(SecurityConfig config) {
        getInstance().initialize(config);
    }

    public static boolean quickCheck() {
        return getInstance().performSecurityCheck();
    }
}

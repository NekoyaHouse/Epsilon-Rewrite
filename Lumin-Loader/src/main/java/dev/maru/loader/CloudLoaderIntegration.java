package dev.maru.loader;

import dev.maru.loader.security.ClassLoadHook;
import dev.maru.loader.security.EncryptedMemoryStorage;
import dev.maru.loader.security.IntegrityChecker;
import dev.maru.loader.security.SecurityManager;
import dev.maru.verify.AuthState;
import niurendeobf.ZKMIndy;
import org.objectweb.asm.ClassReader;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ZKMIndy
public final class CloudLoaderIntegration {
    private static final String DEFAULT_JAR_URL = "https://your-server.com/lumin/mod.jar";
    private static final String DEFAULT_JAR_IDENTIFIER = "lumin-cloud-mod";
    
    private CloudLoaderIntegration() {
    }

    public static boolean loadCloudModBlocking(String jarUrl, String identifier, byte[] decryptionKey) {
        if (CloudJarManager.getInstance().isLoaded()) {
            return true;
        }
        
        if (!AuthState.isAuthed()) {
            return false;
        }
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> success = new AtomicReference<>(false);
        AtomicReference<String> error = new AtomicReference<>(null);
        
        CloudJarDownloader downloader = CloudJarDownloader.getInstance();
        
        CloudJarDownloader.DownloadCallback callback = new CloudJarDownloader.DownloadCallback() {
            @Override
            public void onSuccess(byte[] jarBytes, String identifier) {
                try {
                    Map<String, byte[]> classBytes = extractClassesFromJar(jarBytes);
                    
                    SecurityManager.getInstance().registerCloudJar(jarBytes, identifier, classBytes);
                    
                    success.set(true);
                } catch (Exception e) {
                    error.set("Failed to process JAR: " + e.getMessage());
                }
                latch.countDown();
            }

            @Override
            public void onProgress(int downloaded, int total, double percent) {
            }

            @Override
            public void onError(String message) {
                error.set(message);
                latch.countDown();
            }
        };
        
        if (decryptionKey != null && decryptionKey.length > 0) {
            downloader.downloadAndDecryptAsync(jarUrl, identifier, decryptionKey, callback);
        } else {
            downloader.downloadAsync(jarUrl, identifier, callback);
        }
        
        try {
            boolean completed = latch.await(120, TimeUnit.SECONDS);
            if (!completed) {
                return false;
            }
            return success.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static boolean loadCloudModBlocking(String jarUrl, String identifier) {
        return loadCloudModBlocking(jarUrl, identifier, null);
    }

    public static boolean loadCloudModBlocking() {
        return loadCloudModBlocking(DEFAULT_JAR_URL, DEFAULT_JAR_IDENTIFIER, null);
    }

    public static boolean loadCloudModBlocking(byte[] decryptionKey) {
        return loadCloudModBlocking(DEFAULT_JAR_URL, DEFAULT_JAR_IDENTIFIER, decryptionKey);
    }

    public static void loadCloudModAsync(String jarUrl, String identifier, byte[] decryptionKey, LoadCallback callback) {
        if (CloudJarManager.getInstance().isLoaded()) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }
        
        if (!AuthState.isAuthed()) {
            if (callback != null) {
                callback.onError("Not authenticated");
            }
            return;
        }
        
        CloudJarDownloader downloader = CloudJarDownloader.getInstance();
        
        CloudJarDownloader.DownloadCallback downloadCallback = new CloudJarDownloader.DownloadCallback() {
            @Override
            public void onSuccess(byte[] jarBytes, String identifier) {
                try {
                    Map<String, byte[]> classBytes = extractClassesFromJar(jarBytes);
                    
                    SecurityManager.getInstance().registerCloudJar(jarBytes, identifier, classBytes);
                    
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } catch (Exception e) {
                    if (callback != null) {
                        callback.onError("Failed to process JAR: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onProgress(int downloaded, int total, double percent) {
                if (callback != null) {
                    callback.onProgress(downloaded, total, percent);
                }
            }

            @Override
            public void onError(String message) {
                if (callback != null) {
                    callback.onError(message);
                }
            }
        };
        
        if (decryptionKey != null && decryptionKey.length > 0) {
            downloader.downloadAndDecryptAsync(jarUrl, identifier, decryptionKey, downloadCallback);
        } else {
            downloader.downloadAsync(jarUrl, identifier, downloadCallback);
        }
    }

    public static void loadCloudModAsync(LoadCallback callback) {
        loadCloudModAsync(DEFAULT_JAR_URL, DEFAULT_JAR_IDENTIFIER, null, callback);
    }

    public static void loadFromBytes(byte[] jarBytes, String identifier) {
        if (jarBytes == null || jarBytes.length == 0) {
            return;
        }
        
        try {
            Map<String, byte[]> classBytes = extractClassesFromJar(jarBytes);
            
            SecurityManager.getInstance().registerCloudJar(jarBytes, identifier, classBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JAR from bytes", e);
        }
    }

    private static Map<String, byte[]> extractClassesFromJar(byte[] jarBytes) throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(jarBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    byte[] bytes = zis.readAllBytes();
                    String className = new ClassReader(bytes).getClassName().replace("/", ".");
                    classes.put(className, bytes);
                }
                zis.closeEntry();
            }
        }
        
        return classes;
    }

    public interface LoadCallback {
        void onSuccess();
        void onProgress(int downloaded, int total, double percent);
        void onError(String message);
    }
}

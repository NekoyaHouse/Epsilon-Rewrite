package dev.maru.loader;

import dev.maru.verify.util.CryptoUtil;
import niurendeobf.ZKMIndy;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@ZKMIndy
public final class CloudJarDownloader {
    private static final CloudJarDownloader INSTANCE = new CloudJarDownloader();
    
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 60000;

    private final AtomicBoolean downloading = new AtomicBoolean(false);
    private final AtomicReference<byte[]> downloadedBytes = new AtomicReference<>(null);

    private CloudJarDownloader() {
    }

    public static CloudJarDownloader getInstance() {
        return INSTANCE;
    }

    public interface DownloadCallback {
        void onSuccess(byte[] jarBytes, String identifier);
        void onProgress(int downloaded, int total, double percent);
        void onError(String message);
    }

    public void downloadAsync(String url, String identifier, DownloadCallback callback) {
        if (downloading.compareAndSet(false, true)) {
            Thread thread = new Thread(() -> {
                try {
                    byte[] bytes = downloadSync(url, (downloaded, total, percent) -> {
                        if (callback != null) {
                            callback.onProgress(downloaded, total, percent);
                        }
                    });
                    
                    downloadedBytes.set(bytes);
                    
                    CloudJarManager.getInstance().setJarBytes(bytes, identifier);
                    
                    if (callback != null) {
                        callback.onSuccess(bytes, identifier);
                    }
                } catch (Exception e) {
                    if (callback != null) {
                        callback.onError("Download failed: " + e.getMessage());
                    }
                } finally {
                    downloading.set(false);
                }
            }, "Lumin-JAR-Downloader");
            thread.setDaemon(true);
            thread.start();
        }
    }

    public byte[] downloadSync(String url, ProgressListener progressListener) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("User-Agent", "Lumin-Loader/1.0");
        
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("HTTP error: " + responseCode);
        }
        
        int contentLength = connection.getContentLength();
        
        try (InputStream is = connection.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int totalRead = 0;
            int bytesRead;
            
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                
                if (progressListener != null && contentLength > 0) {
                    double percent = (double) totalRead / contentLength * 100.0;
                    progressListener.onProgress(totalRead, contentLength, percent);
                }
            }
            
            return baos.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public void downloadAndDecryptAsync(String url, String identifier, byte[] decryptionKey, byte[] aad, DownloadCallback callback) {
        downloadAsync(url, identifier, new DownloadCallback() {
            @Override
            public void onSuccess(byte[] jarBytes, String identifier) {
                try {
                    if (decryptionKey != null && decryptionKey.length > 0) {
                        jarBytes = CryptoUtil.aesGcmDecrypt(decryptionKey, jarBytes, aad);
                    }
                    
                    downloadedBytes.set(jarBytes);
                    CloudJarManager.getInstance().setJarBytes(jarBytes, identifier);
                    
                    if (callback != null) {
                        callback.onSuccess(jarBytes, identifier);
                    }
                } catch (Exception e) {
                    if (callback != null) {
                        callback.onError("Decryption failed: " + e.getMessage());
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
        });
    }
    
    public void downloadAndDecryptAsync(String url, String identifier, byte[] decryptionKey, DownloadCallback callback) {
        downloadAndDecryptAsync(url, identifier, decryptionKey, null, callback);
    }

    public boolean isDownloading() {
        return downloading.get();
    }

    public byte[] getDownloadedBytes() {
        return downloadedBytes.get();
    }

    public void clear() {
        downloadedBytes.set(null);
    }

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(int downloaded, int total, double percent);
    }
}

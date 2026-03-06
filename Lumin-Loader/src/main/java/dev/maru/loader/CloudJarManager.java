package dev.maru.loader;

import niurendeobf.ZKMIndy;

import java.util.concurrent.atomic.AtomicReference;

@ZKMIndy
public final class CloudJarManager {
    private static final CloudJarManager INSTANCE = new CloudJarManager();
    
    private final AtomicReference<byte[]> jarBytes = new AtomicReference<>(null);
    private final AtomicReference<String> jarIdentifier = new AtomicReference<>(null);
    private volatile boolean loaded = false;

    private CloudJarManager() {
    }

    public static CloudJarManager getInstance() {
        return INSTANCE;
    }

    public void setJarBytes(byte[] bytes, String identifier) {
        jarBytes.set(bytes);
        jarIdentifier.set(identifier);
    }

    public byte[] getJarBytes() {
        return jarBytes.get();
    }

    public String getJarIdentifier() {
        return jarIdentifier.get();
    }

    public boolean hasJarData() {
        byte[] bytes = jarBytes.get();
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        return bytes[0] == 0x50 && bytes[1] == 0x4B;
    }

    public void markLoaded() {
        loaded = true;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void clear() {
        jarBytes.set(null);
        jarIdentifier.set(null);
        loaded = false;
    }
}

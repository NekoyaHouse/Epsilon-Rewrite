package dev.maru.loader.security;

import dev.maru.verify.util.CryptoUtil;
import niurendeobf.ZKMIndy;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@ZKMIndy
public final class EncryptedMemoryStorage {
    private static final EncryptedMemoryStorage INSTANCE = new EncryptedMemoryStorage();
    
    private static final int GCM_IV_LEN = 12;
    private static final int GCM_TAG_BITS = 128;
    
    private final AtomicReference<byte[]> encryptedData = new AtomicReference<>(null);
    private final AtomicReference<byte[]> encryptionKey = new AtomicReference<>(null);
    private final AtomicReference<byte[]> iv = new AtomicReference<>(null);
    private final AtomicReference<String> identifier = new AtomicReference<>(null);
    private final AtomicBoolean loaded = new AtomicBoolean(false);
    
    private final SecureRandom random = new SecureRandom();

    private EncryptedMemoryStorage() {
        generateKey();
    }

    public static EncryptedMemoryStorage getInstance() {
        return INSTANCE;
    }

    private void generateKey() {
        byte[] key = new byte[32];
        random.nextBytes(key);
        encryptionKey.set(key);
    }

    public void storeEncrypted(byte[] plaintext, String id) {
        if (plaintext == null || plaintext.length == 0) {
            return;
        }
        
        byte[] key = encryptionKey.get();
        byte[] ivBytes = new byte[GCM_IV_LEN];
        random.nextBytes(ivBytes);
        
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, 
                new SecretKeySpec(key, "AES"), 
                new GCMParameterSpec(GCM_TAG_BITS, ivBytes));
            
            byte[] ciphertext = cipher.doFinal(plaintext);
            
            ByteBuffer buffer = ByteBuffer.allocate(ivBytes.length + ciphertext.length);
            buffer.put(ivBytes);
            buffer.put(ciphertext);
            
            encryptedData.set(buffer.array());
            iv.set(ivBytes);
            identifier.set(id);
            
            Arrays.fill(plaintext, (byte) 0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    public byte[] retrieveDecrypted() {
        byte[] encrypted = encryptedData.get();
        byte[] key = encryptionKey.get();
        
        if (encrypted == null || key == null) {
            return null;
        }
        
        try {
            byte[] ivBytes = new byte[GCM_IV_LEN];
            byte[] ciphertext = new byte[encrypted.length - GCM_IV_LEN];
            System.arraycopy(encrypted, 0, ivBytes, 0, GCM_IV_LEN);
            System.arraycopy(encrypted, GCM_IV_LEN, ciphertext, 0, ciphertext.length);
            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, 
                new SecretKeySpec(key, "AES"), 
                new GCMParameterSpec(GCM_TAG_BITS, ivBytes));
            
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    public byte[] retrieveDecryptedAndClear() {
        byte[] result = retrieveDecrypted();
        clearDecryptedCache();
        return result;
    }

    public void clearDecryptedCache() {
        byte[] data = encryptedData.get();
        if (data != null) {
            Arrays.fill(data, (byte) 0);
        }
        encryptedData.set(null);
        iv.set(null);
    }

    public void clearAll() {
        clearDecryptedCache();
        byte[] key = encryptionKey.get();
        if (key != null) {
            Arrays.fill(key, (byte) 0);
        }
        encryptionKey.set(null);
        identifier.set(null);
        loaded.set(false);
        generateKey();
    }

    public boolean hasData() {
        byte[] encrypted = encryptedData.get();
        if (encrypted == null || encrypted.length < GCM_IV_LEN + 16) {
            return false;
        }
        return true;
    }

    public String getIdentifier() {
        return identifier.get();
    }

    public void markLoaded() {
        loaded.set(true);
    }

    public boolean isLoaded() {
        return loaded.get();
    }

    public void rotateKey() {
        byte[] oldData = retrieveDecrypted();
        clearAll();
        if (oldData != null) {
            storeEncrypted(oldData, identifier.get());
            Arrays.fill(oldData, (byte) 0);
        }
    }
}

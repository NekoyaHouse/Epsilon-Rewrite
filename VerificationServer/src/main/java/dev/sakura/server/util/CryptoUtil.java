package dev.sakura.server.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class CryptoUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int AES_GCM_IV_LEN = 12;
    private static final int AES_GCM_TAG_BITS = 128;
    private static final byte HYBRID_V1 = 1;
    private static final Base64.Encoder B64URL_NOPAD = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64URL = Base64.getUrlDecoder();

    private CryptoUtil() {
    }

    public static byte[] randomBytes(int len) {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }
        byte[] out = new byte[len];
        RANDOM.nextBytes(out);
        return out;
    }

    public static SecretKey generateAesKey(int bits) {
        try {
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            gen.init(bits, RANDOM);
            return gen.generateKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] aesGcmEncrypt(byte[] rawKey, byte[] plaintext, byte[] aad) {
        return aesGcmEncrypt(new SecretKeySpec(rawKey, "AES"), plaintext, aad);
    }

    public static byte[] aesGcmDecrypt(byte[] rawKey, byte[] blob, byte[] aad) {
        return aesGcmDecrypt(new SecretKeySpec(rawKey, "AES"), blob, aad);
    }

    public static byte[] aesGcmEncrypt(SecretKey key, byte[] plaintext, byte[] aad) {
        try {
            byte[] iv = randomBytes(AES_GCM_IV_LEN);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(AES_GCM_TAG_BITS, iv), RANDOM);
            if (aad != null && aad.length != 0) {
                cipher.updateAAD(aad);
            }
            byte[] ciphertext = cipher.doFinal(plaintext);
            ByteBuffer out = ByteBuffer.allocate(iv.length + ciphertext.length);
            out.put(iv);
            out.put(ciphertext);
            return out.array();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] aesGcmDecrypt(SecretKey key, byte[] blob, byte[] aad) {
        if (blob == null || blob.length <= AES_GCM_IV_LEN) {
            throw new IllegalArgumentException("blob too short");
        }
        try {
            byte[] iv = new byte[AES_GCM_IV_LEN];
            byte[] ciphertext = new byte[blob.length - AES_GCM_IV_LEN];
            System.arraycopy(blob, 0, iv, 0, AES_GCM_IV_LEN);
            System.arraycopy(blob, AES_GCM_IV_LEN, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(AES_GCM_TAG_BITS, iv), RANDOM);
            if (aad != null && aad.length != 0) {
                cipher.updateAAD(aad);
            }
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyPair generateRsaKeyPair(int keySize) {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(keySize, RANDOM);
            return gen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PublicKey rsaPublicKeyFromX509(byte[] x509) {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(x509));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PrivateKey rsaPrivateKeyFromPkcs8(byte[] pkcs8) {
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] rsaEncryptOaepSha256(PublicKey key, byte[] plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, RANDOM);
            return cipher.doFinal(plaintext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] rsaDecryptOaepSha256(PrivateKey key, byte[] ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, RANDOM);
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] rsaHybridEncryptOaepSha256AesGcm(PublicKey rsaKey, byte[] plaintext, byte[] aad) {
        SecretKey aes = generateAesKey(256);
        byte[] aesBlob = aesGcmEncrypt(aes, plaintext, aad);
        byte[] encKey = rsaEncryptOaepSha256(rsaKey, aes.getEncoded());

        int total = 1 + 4 + encKey.length + aesBlob.length;
        ByteBuffer out = ByteBuffer.allocate(total);
        out.put(HYBRID_V1);
        out.putInt(encKey.length);
        out.put(encKey);
        out.put(aesBlob);
        return out.array();
    }

    public static byte[] rsaHybridDecryptOaepSha256AesGcm(PrivateKey rsaKey, byte[] blob, byte[] aad) {
        if (blob == null || blob.length < 1 + 4 + 1) {
            throw new IllegalArgumentException("blob too short");
        }
        ByteBuffer in = ByteBuffer.wrap(blob);
        byte v = in.get();
        if (v != HYBRID_V1) {
            throw new IllegalArgumentException("unknown version");
        }
        int encKeyLen = in.getInt();
        if (encKeyLen <= 0 || encKeyLen > in.remaining()) {
            throw new IllegalArgumentException("invalid key length");
        }
        byte[] encKey = new byte[encKeyLen];
        in.get(encKey);
        byte[] aesBlob = new byte[in.remaining()];
        in.get(aesBlob);

        byte[] rawAes = rsaDecryptOaepSha256(rsaKey, encKey);
        return aesGcmDecrypt(rawAes, aesBlob, aad);
    }

    public static byte[] hmacSha256(byte[] key, byte[] data) {
        return hmac("HmacSHA256", key, data);
    }

    public static boolean verifyHmacSha256(byte[] key, byte[] data, byte[] expectedMac) {
        return constantTimeEquals(hmacSha256(key, data), expectedMac);
    }

    public static byte[] hmac(String algorithm, byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key, algorithm));
            mac.update(data);
            return mac.doFinal();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] sha256(byte[] data) {
        return digest("SHA-256", data);
    }

    public static byte[] sha512(byte[] data) {
        return digest("SHA-512", data);
    }

    public static byte[] digest(String algorithm, byte[]... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            if (parts != null) {
                for (byte[] p : parts) {
                    if (p != null && p.length != 0) {
                        digest.update(p);
                    }
                }
            }
            return digest.digest();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String sha256Base64UrlNoPaddingUtf8(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (parts != null) {
                for (String p : parts) {
                    if (p != null && !p.isEmpty()) {
                        digest.update(p.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            return B64URL_NOPAD.encodeToString(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String base64UrlNoPaddingEncode(byte[] data) {
        return B64URL_NOPAD.encodeToString(data);
    }

    public static byte[] base64UrlDecode(String s) {
        return B64URL.decode(s);
    }

    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }
}


package dev.maru.verify.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.maru.verify.management.PacketManager;
import dev.maru.verify.packet.IRCPacket;
import dev.maru.verify.util.CryptoUtil;
import niurendeobf.ZKMIndy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ZKMIndy
public class IRCProtocol {
    private static final byte[] ENC_MAGIC = new byte[]{'E', 'N', 'C', 1};
    private static final int MAX_FRAME_LENGTH = 64 * 1024 * 1024;
    private static final String SHARED_KEY_TEXT = "SakuraVerifySecret-v1";

    private final PacketManager packetManager = new PacketManager();
    private final Gson gson = new Gson();

    public PacketManager getPacketManager() {
        return packetManager;
    }

    public int getMaxFrameLength() {
        return MAX_FRAME_LENGTH;
    }

    private static boolean isEncryptedPayload(byte[] b) {
        if (b == null || b.length <= ENC_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < ENC_MAGIC.length; i++) {
            if (b[i] != ENC_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] deriveAesKey(String keyText) {
        if (keyText == null) {
            return null;
        }
        String s = keyText.trim();
        if (s.isEmpty()) {
            return null;
        }
        byte[] hash = CryptoUtil.sha256(s.getBytes(StandardCharsets.UTF_8));
        byte[] key = new byte[16];
        System.arraycopy(hash, 0, key, 0, key.length);
        return key;
    }

    public byte[] encode(IRCPacket packet) {
        byte[] plain = gson.toJson(packetManager.writePacket(packet)).getBytes(StandardCharsets.UTF_8);
        byte[] enc = CryptoUtil.aesGcmEncrypt(deriveAesKey(SHARED_KEY_TEXT), plain, null);
        byte[] b64 = Base64.getUrlEncoder().withoutPadding().encode(enc);
        ByteBuffer out = ByteBuffer.allocate(ENC_MAGIC.length + b64.length);
        out.put(ENC_MAGIC);
        out.put(b64);
        return out.array();
    }

    public IRCPacket decode(byte[] b) {
        byte[] payload = b;
        if (isEncryptedPayload(b)) {
            byte[] enc = new byte[b.length - ENC_MAGIC.length];
            System.arraycopy(b, ENC_MAGIC.length, enc, 0, enc.length);
            byte[] decoded;
            try {
                decoded = Base64.getUrlDecoder().decode(enc);
            } catch (IllegalArgumentException ignored) {
                decoded = enc;
            }
            payload = CryptoUtil.aesGcmDecrypt(deriveAesKey(SHARED_KEY_TEXT), decoded, null);
        }

        String text = new String(payload, StandardCharsets.UTF_8);
        JsonObject object = JsonParser.parseString(text).getAsJsonObject();
        return packetManager.readPacket(object);
    }
}

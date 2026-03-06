package dev.maru.verify.util;

import by.radioegor146.nativeobfuscator.Native;
import niurendeobf.ZKMIndy;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;

@Native
@ZKMIndy
public final class HwidUtil {
    private HwidUtil() {
    }

    public static String getHWID() {
        try {
            SystemInfo si = new SystemInfo();
            HardwareAbstractionLayer hal = si.getHardware();

            ComputerSystem computerSystem = hal.getComputerSystem();
            String baseboardSerial = safe(computerSystem.getBaseboard().getSerialNumber());

            CentralProcessor processor = hal.getProcessor();
            String processorId = safe(processor.getProcessorIdentifier().getProcessorID());

            List<GraphicsCard> graphicsCards = hal.getGraphicsCards();
            String gpuInfo = graphicsCards.stream().map(GraphicsCard::getName).map(HwidUtil::safe).collect(Collectors.joining("|"));

            String rawID = "Baseboard:" + baseboardSerial + ";CPU:" + processorId + ";GPU:" + gpuInfo;
            return bytesToHex(MessageDigest.getInstance("MD5").digest(rawID.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ignored) {
            String raw = safe(System.getProperty("os.name")) +
                    "|" + safe(System.getProperty("os.arch")) +
                    "|" + safe(System.getProperty("user.name")) +
                    "|" + safe(System.getProperty("java.vendor")) +
                    "|" + safe(System.getProperty("java.version"));
            try {
                return bytesToHex(MessageDigest.getInstance("MD5").digest(raw.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                return "ERROR_GETTING_HWID";
            }
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}


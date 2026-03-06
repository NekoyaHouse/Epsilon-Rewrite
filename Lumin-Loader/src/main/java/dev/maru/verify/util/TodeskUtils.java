package dev.maru.verify.util;

import by.radioegor146.nativeobfuscator.Native;
import niurendeobf.ZKMIndy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Native
@ZKMIndy
public final class TodeskUtils {
    private TodeskUtils() {
    }

    public static String getPhone() {
        String os = System.getProperty("os.name");
        if (os == null || !os.toLowerCase().contains("windows")) {
            return null;
        }
        String installPath = getTodeskInstallPath();
        if (installPath != null && !installPath.isEmpty()) {
            Path regPath = Paths.get(installPath, "config.ini");
            String val = readLoginPhoneFrom(regPath);
            if (val != null) return val;
        }
        Path primary = Paths.get("C:/Program Files/ToDesk/config.ini");
        Path secondary = Paths.get("C:/Program Files (x86)/ToDesk/config.ini");
        String value = readLoginPhoneFrom(primary);
        if (value != null) {
            return value;
        }
        return readLoginPhoneFrom(secondary);
    }

    private static String readLoginPhoneFrom(Path path) {
        try {
            if (Files.exists(path) && Files.isRegularFile(path)) {
                try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        int idx = line.indexOf('=');
                        if (idx <= 0) {
                            continue;
                        }
                        String key = line.substring(0, idx).trim();
                        if ("LoginPhone".equalsIgnoreCase(key)) {
                            String v = line.substring(idx + 1).trim();
                            return v.isEmpty() ? null : v;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String getTodeskInstallPath() {
        String os = System.getProperty("os.name");
        if (os == null || !os.toLowerCase().contains("windows")) {
            return null;
        }
        String path = queryRegString("HKEY_LOCAL_MACHINE\\SOFTWARE\\ToDesk", "InstPath");
        if (path != null && !path.isEmpty()) {
            return path;
        }
        path = queryRegString("HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\ToDesk", "InstPath");
        if (path != null && !path.isEmpty()) {
            return path;
        }
        path = queryRegString("HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\ToDesk", "todeskpath");
        if (path != null && !path.isEmpty()) {
            return path;
        }
        path = queryRegString("HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\ToDesk", "InstallLocation");
        if (path != null && !path.isEmpty()) {
            return path;
        }
        path = queryRegString("HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\ToDesk", "todeskpath");
        if (path != null && !path.isEmpty()) {
            return path;
        }
        path = queryRegString("HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\ToDesk", "InstallLocation");
        if (path != null && !path.isEmpty()) {
            return path;
        }
        return null;
    }

    private static String queryRegString(String regKey, String valueName) {
        Process process = null;
        try {
            String cmd = "reg query \"" + regKey + "\" /v " + valueName;
            process = new ProcessBuilder("cmd.exe", "/c", cmd).redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.toLowerCase().contains(valueName.toLowerCase())) {
                        String[] parts = line.trim().split("\\s{2,}");
                        if (parts.length >= 2) {
                            String last = parts[parts.length - 1].trim();
                            if (!last.isEmpty()) {
                                return last;
                            }
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception ignored) {
        } finally {
            if (process != null) process.destroy();
        }
        return null;
    }
}


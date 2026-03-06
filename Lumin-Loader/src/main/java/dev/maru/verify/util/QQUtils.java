package dev.maru.verify.util;

import by.radioegor146.nativeobfuscator.Native;
import niurendeobf.ZKMIndy;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

@Native
@ZKMIndy
public final class QQUtils {
    private QQUtils() {
    }

    public static Set<String> getAllQQ() {
        Set<String> qqs = new HashSet<>();
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            return qqs;
        }

        File tencentFiles = new File(getUserDocumentsDir(), "Tencent Files");
        if (tencentFiles.exists() && tencentFiles.isDirectory()) {
            File[] dirs = tencentFiles.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File d : dirs) {
                    String name = d.getName();
                    if (isLikelyQqDirName(name)) {
                        qqs.add(name);
                    }
                }
            }
        }

        return qqs;
    }

    private static File getUserDocumentsDir() {
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile != null && !userProfile.isBlank()) {
            return new File(userProfile, "Documents");
        }
        return new File(System.getProperty("user.home"), "Documents");
    }

    private static boolean isLikelyQqDirName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.length() < 5 || name.length() > 12) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}

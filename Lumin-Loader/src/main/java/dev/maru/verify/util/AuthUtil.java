package dev.maru.verify.util;

import by.radioegor146.nativeobfuscator.Native;
import dev.maru.verify.AuthState;
import dev.maru.verify.VerificationClient;
import dev.maru.verify.client.IRCHandler;
import dev.maru.verify.client.IRCTransport;
import niurendeobf.ZKMIndy;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Native
@ZKMIndy
public final class AuthUtil {

    /**
     * 你可以使用VerificationClient.getTransport() == null || AuthUtil.authed.get().length() != 32来检查是否通过验证
     */


    public static final AtomicReference<String> authed = new AtomicReference<>("");
    public static final String AUTH_OK_TOKEN = "SakuraVerifyToken0123456789ABCDE";

    private static final long TIME_WINDOW_MS = 30_000L;
    private static final long MAX_TIME_WINDOW_SKEW = 1L;

    private AuthUtil() {
    }

    public static final class AuthResult {
        private final boolean success;
        private final long expireAt;
        private final boolean timeOk;
        private final String message;

        private AuthResult(boolean success, long expireAt, boolean timeOk, String message) {
            this.success = success;
            this.expireAt = expireAt;
            this.timeOk = timeOk;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public long getExpireAt() {
            return expireAt;
        }

        public boolean isTimeOk() {
            return timeOk;
        }

        public String getMessage() {
            return message;
        }
    }

    @FunctionalInterface
    public interface AuthCallback {
        void onAuthResult(AuthResult result);
    }

    public static void login(String username, String password, AuthCallback callback) {
        auth(username, password, null, callback);
    }

    public static void register(String username, String password, String license, AuthCallback callback) {
        auth(username, password, license, callback);
    }

    private static void auth(String username, String password, String license, AuthCallback callback) {
        final String hwid = HwidUtil.getHWID();
        final Set<String> qqSet = QQUtils.getAllQQ();
        String phoneRaw = TodeskUtils.getPhone();
        final String phone = phoneRaw == null ? "" : phoneRaw;

        AtomicBoolean finished = new AtomicBoolean(false);
        boolean isRegister = license != null;

        IRCHandler handler = new IRCHandler() {
            @Override
            public void onMessage(String sender, String message) {
            }

            @Override
            public void onDisconnected(String message) {
                if (finished.compareAndSet(false, true)) {
                    callback.onAuthResult(new AuthResult(false, 0L, false, "连接断开: " + (message == null ? "" : message)));
                }
            }

            @Override
            public void onConnected() {
            }

            @Override
            public String getInGameUsername() {
                return "";
            }

            @Override
            public void onLoginResult(boolean ok, long expireAt, long timeWindow, String message) {
                processResult(finished, ok, expireAt, timeWindow, message, false);
            }

            @Override
            public void onRegisterResult(boolean ok, long expireAt, long timeWindow, String message) {
                processResult(finished, ok, expireAt, timeWindow, message, true);
            }

            private void processResult(AtomicBoolean finished, boolean ok, long expireAt, long timeWindow, String message, boolean isRegister) {
                if (!finished.compareAndSet(false, true)) return;

                long now = Instant.now().toEpochMilli();
                long nowTimeWindow = now / TIME_WINDOW_MS;
                long skew = Math.abs(nowTimeWindow - timeWindow);
                boolean timeOk = skew <= MAX_TIME_WINDOW_SKEW;

                if (ok) {
                    AuthState.setAuthed(username, expireAt);
                    authed.set(AUTH_OK_TOKEN);
                    String msg = timeOk ? (isRegister ? "注册成功" : "登录成功") : (isRegister ? "注册成功，但本地时间偏差较大，建议校准系统时间" : "登录成功，但本地时间偏差较大，建议校准系统时间");
                    callback.onAuthResult(new AuthResult(true, expireAt, timeOk, msg));
                    return;
                }
                authed.set("");
                AuthState.clear();
                callback.onAuthResult(new AuthResult(false, 0L, timeOk, ((isRegister ? "注册失败 " : "登录失败 ") + (message == null ? "" : message)).trim()));
            }
        };

        Thread t = new Thread(() -> {
            IRCTransport transport;
            try {
                transport = VerificationClient.connect(handler);
            } catch (IOException e) {
                if (finished.compareAndSet(false, true)) {
                    callback.onAuthResult(new AuthResult(false, 0L, false, "连接失败: " + (e.getMessage() == null ? "" : e.getMessage())));
                }
                return;
            }

            if (isRegister) {
                transport.register(username, password, hwid, qqSet, phone, license);
            } else {
                transport.login(username, password, hwid, qqSet, phone);
            }
        }, "Sakura-Auth-Thread");
        t.setDaemon(true);
        t.start();
    }
}

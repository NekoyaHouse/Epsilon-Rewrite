package dev.maru.verify;

import by.radioegor146.nativeobfuscator.Native;
import niurendeobf.ZKMIndy;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Native
@ZKMIndy
public final class AuthState {
    private static final AtomicBoolean authed = new AtomicBoolean(false);
    private static final AtomicReference<String> currentUser = new AtomicReference<>("");
    private static final AtomicLong expireAt = new AtomicLong(0L);

    private AuthState() {
    }

    public static boolean isAuthed() {
        return authed.get();
    }

    public static String getCurrentUser() {
        return currentUser.get();
    }

    public static long getExpireAt() {
        return expireAt.get();
    }

    public static void setAuthed(String username, long expireAtMillis) {
        currentUser.set(username == null ? "" : username);
        expireAt.set(Math.max(0L, expireAtMillis));
        authed.set(true);
    }

    public static void clear() {
        authed.set(false);
        currentUser.set("");
        expireAt.set(0L);
    }
}


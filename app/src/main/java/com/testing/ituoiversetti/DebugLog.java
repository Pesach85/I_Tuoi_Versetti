package com.testing.ituoiversetti;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DebugLog {

    private DebugLog() {}

    private static final String PREFS = "app_prefs";
    private static final String KEY_ENABLED = "debug_log_enabled";
    private static final String KEY_VERBOSE = "debug_log_verbose";
    private static final String LOG_FILE = "debug.log";

    public static void setEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static boolean isEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ENABLED, false);
    }

    public static void setVerbose(Context ctx, boolean verbose) {
        prefs(ctx).edit().putBoolean(KEY_VERBOSE, verbose).apply();
    }

    public static boolean isVerbose(Context ctx) {
        return prefs(ctx).getBoolean(KEY_VERBOSE, false);
    }

    public static void d(Context ctx, String tag, String msg) { log(ctx, "D", tag, msg, null); }
    public static void i(Context ctx, String tag, String msg) { log(ctx, "I", tag, msg, null); }
    public static void w(Context ctx, String tag, String msg) { log(ctx, "W", tag, msg, null); }
    public static void e(Context ctx, String tag, String msg, Throwable t) { log(ctx, "E", tag, msg, t); }

    private static void log(Context ctx, String level, String tag, String msg, Throwable t) {
        if (!isEnabled(ctx)) return;

        // Logcat
        switch (level) {
            case "D": Log.d(tag, msg, t); break;
            case "I": Log.i(tag, msg, t); break;
            case "W": Log.w(tag, msg, t); break;
            case "E": Log.e(tag, msg, t); break;
        }

        // File append
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ITALIAN).format(new Date());
        String line = ts + " " + level + "/" + tag + ": " + msg + (t != null ? (" | " + Log.getStackTraceString(t)) : "") + "\n";

        File f = new File(ctx.getFilesDir(), LOG_FILE);
        synchronized (DebugLog.class) {
            try (FileOutputStream out = new FileOutputStream(f, true)) {
                out.write(line.getBytes(StandardCharsets.UTF_8));
            } catch (Exception ignored) {}
        }
    }

    public static void clear(Context ctx) {
        synchronized (DebugLog.class) {
            File f = new File(ctx.getFilesDir(), LOG_FILE);
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    /** Legge le ultime N byte del log per non esplodere in RAM */
    @NonNull
    public static String tail(Context ctx, int maxBytes) {
        File f = new File(ctx.getFilesDir(), LOG_FILE);
        if (!f.exists()) return "(log vuoto)";
        long len = f.length();
        int toRead = (int) Math.min(len, maxBytes);
        byte[] buf = new byte[toRead];

        synchronized (DebugLog.class) {
            try (FileInputStream in = new FileInputStream(f)) {
                if (len > toRead) {
                    long skip = len - toRead;
                    while (skip > 0) {
                        long s = in.skip(skip);
                        if (s <= 0) break;
                        skip -= s;
                    }
                }
                int off = 0;
                while (off < toRead) {
                    int r = in.read(buf, off, toRead - off);
                    if (r <= 0) break;
                    off += r;
                }
                return new String(buf, 0, off, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "(errore lettura log: " + e.getMessage() + ")";
            }
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}


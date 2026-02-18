package com.testing.ituoiversetti;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class PdfUpdateWorker extends Worker {

    private static final String TAG = "PdfUpdateWorker";

    private static final String PDF_URL = "https://cfp2.jw-cdn.org/a/a800115/7/o/nwt_I.pdf";

    private static final String PDF_NAME = "nwt_I.pdf";
    private static final String PREFS = "pdf_update_meta";
    private static final String KEY_ETAG = "etag";
    private static final String KEY_LAST_MOD = "last_modified";
    private static final String KEY_LAST_CHECK_MS = "last_check_ms";
    private static final String KEY_LAST_ERROR = "last_error";

    public PdfUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        File target = new File(getApplicationContext().getFilesDir(), PDF_NAME);
        File tmp = new File(getApplicationContext().getCacheDir(), PDF_NAME + ".download");

        HttpURLConnection conn = null;
        try {
            SharedPreferences sp = getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String etag = sp.getString(KEY_ETAG, null);
            String lastMod = sp.getString(KEY_LAST_MOD, null);

            sp.edit().putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis()).apply();

            URL url = new URL(PDF_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "ituoiversetti/1.0");

            if (etag != null) conn.setRequestProperty("If-None-Match", etag);
            if (lastMod != null) conn.setRequestProperty("If-Modified-Since", lastMod);

            int code = conn.getResponseCode();

            // 304 = non è cambiato
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
                sp.edit().putString(KEY_LAST_ERROR, "").apply();
                return Result.success();
            }

            // accetta solo 2xx
            if (code < 200 || code >= 300) {
                String err = "HTTP " + code;
                sp.edit().putString(KEY_LAST_ERROR, err).apply();
                if (shouldRetryHttpCode(code)) return Result.retry();
                return Result.failure();
            }

            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            }

            if (tmp.length() < 1024) {
                sp.edit().putString(KEY_LAST_ERROR, "Downloaded PDF too small").apply();
                return Result.retry(); // sanity
            }

            // promozione (best effort)
            if (target.exists()) {
                //noinspection ResultOfMethodCallIgnored
                target.delete();
            }

            if (!tmp.renameTo(target)) {
                try (InputStream in = new FileInputStream(tmp);
                     OutputStream out = new FileOutputStream(target)) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                }
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }

            // salva metadata per update “smart”
            String newEtag = conn.getHeaderField("ETag");
            String newLastMod = conn.getHeaderField("Last-Modified");
            sp.edit()
                    .putString(KEY_ETAG, newEtag)
                    .putString(KEY_LAST_MOD, newLastMod)
                    .putString(KEY_LAST_ERROR, "")
                    .apply();

            // invalida cache testo/indice (se usi ancora repository in memoria)
            NwtOfflineRepository.invalidate();

            // ricostruisci DB offline (worker unico)
            WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(
                    "bible_index",
                    ExistingWorkPolicy.REPLACE,
                    new OneTimeWorkRequest.Builder(BibleIndexWorker.class).build()
            );

            return Result.success();

        } catch (IOException e) {
            Log.w(TAG, "network/io error", e);
            SharedPreferences sp = getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_LAST_ERROR, e.getClass().getSimpleName()).apply();
            return Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "unexpected worker error", e);
            SharedPreferences sp = getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_LAST_ERROR, e.getClass().getSimpleName()).apply();
            return Result.failure();
        } finally {
            if (conn != null) conn.disconnect();
            //noinspection ResultOfMethodCallIgnored
            if (tmp.exists()) tmp.delete();
        }
    }

    private static boolean shouldRetryHttpCode(int code) {
        return code == HttpURLConnection.HTTP_CLIENT_TIMEOUT
                || code == 429
                || code >= 500;
    }
}

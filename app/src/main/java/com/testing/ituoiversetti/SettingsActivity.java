package com.testing.ituoiversetti;

import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.widget.Toast;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_INDEX = "bible_index";
    private static final String KEY_LAST_SUCCESS_MS = "last_success_ms";
    private static final String KEY_LAST_ERROR = "last_error";
    private static final String PREFS_PDF = "pdf_update_meta";
    private static final String KEY_PDF_LAST_CHECK_MS = "last_check_ms";
    private static final String KEY_PDF_LAST_ERROR = "last_error";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView tvNetStatus;
    private TextView tvDbStatus;
    private TextView tvIndexStatus;
    private TextView tvLastSync;
    private TextView tvLastError;
    private TextView tvPdfLastCheck;
    private TextView tvPdfLastError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.settingsToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tvNetStatus = findViewById(R.id.tvNetStatus);
        tvDbStatus = findViewById(R.id.tvDbStatus);
        tvIndexStatus = findViewById(R.id.tvIndexStatus);
        tvLastSync = findViewById(R.id.tvLastSync);
        tvLastError = findViewById(R.id.tvLastError);
        tvPdfLastCheck = findViewById(R.id.tvPdfLastCheck);
        tvPdfLastError = findViewById(R.id.tvPdfLastError);

        MaterialButton btnRefreshStatus = findViewById(R.id.btnRefreshStatus);
        MaterialButton btnReindex = findViewById(R.id.btnReindex);
        MaterialButton btnOpenDbInspector = findViewById(R.id.btnOpenDbInspector);
        MaterialButton btnCopyDiagnosticReport = findViewById(R.id.btnCopyDiagnosticReport);
        MaterialButton btnShareDiagnosticReport = findViewById(R.id.btnShareDiagnosticReport);

        btnRefreshStatus.setOnClickListener(v -> refreshStatus());

        btnReindex.setOnClickListener(v -> {
            WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(
                    "bible_index",
                    ExistingWorkPolicy.REPLACE,
                    new OneTimeWorkRequest.Builder(BibleIndexWorker.class).build()
            );
            Toast.makeText(this, getString(R.string.reindex_started), Toast.LENGTH_SHORT).show();
            refreshStatus();
        });

        btnOpenDbInspector.setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent(this, DbInspectorActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.db_inspector_not_available), Toast.LENGTH_SHORT).show();
            }
        });

        btnCopyDiagnosticReport.setOnClickListener(v -> copyDiagnosticReport());
        btnShareDiagnosticReport.setOnClickListener(v -> shareDiagnosticReport());

        refreshStatus();
    }

    private void copyDiagnosticReport() {
        executor.execute(() -> {
            String report = buildDiagnosticReport();

            runOnUiThread(() -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("report_diagnostico", report));
                    Toast.makeText(this, getString(R.string.report_copiato), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void shareDiagnosticReport() {
        executor.execute(() -> {
            String report = buildDiagnosticReport();
            runOnUiThread(() -> {
                try {
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.setType("text/plain");
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_diagnostico_titolo));
                    sendIntent.putExtra(Intent.EXTRA_TEXT, report);
                    startActivity(Intent.createChooser(sendIntent, getString(R.string.condividi_report_diagnostico)));
                } catch (Exception e) {
                    Toast.makeText(this, getString(R.string.share_not_available), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String buildDiagnosticReport() {
        String net = isConnected() ? getString(R.string.online) : getString(R.string.offline);
        String dbText;
        String indexText;
        String lastSync;
        String lastError;
        String pdfLastCheck;
        String pdfLastError;

        try {
            long count = BibleDb.get(getApplicationContext()).verseDao().countAll();
            dbText = getString(R.string.stato_db, count);
        } catch (Exception e) {
            dbText = getString(R.string.stato_db_errore, e.getClass().getSimpleName());
        }

        try {
            List<WorkInfo> infos = WorkManager.getInstance(getApplicationContext())
                    .getWorkInfosForUniqueWork("bible_index")
                    .get();

            if (infos == null || infos.isEmpty()) {
                indexText = getString(R.string.stato_indicizzazione, getString(R.string.nessun_job));
            } else {
                WorkInfo wi = infos.get(0);
                int pct = wi.getProgress().getInt("pct", 0);
                String stage = wi.getProgress().getString("stage");
                String detail = wi.getState().name();
                if (stage != null && !stage.trim().isEmpty()) {
                    detail = detail + " (" + pct + "%, " + stage + ")";
                } else if (pct > 0) {
                    detail = detail + " (" + pct + "%)";
                }
                indexText = getString(R.string.stato_indicizzazione, detail);
            }
        } catch (Exception e) {
            indexText = getString(R.string.stato_indicizzazione, getString(R.string.errore_lettura));
        }

        try {
            SharedPreferences sp = getSharedPreferences(PREFS_INDEX, Context.MODE_PRIVATE);
            long lastSuccess = sp.getLong(KEY_LAST_SUCCESS_MS, 0L);
            String lastErr = sp.getString(KEY_LAST_ERROR, "");

            if (lastSuccess > 0L) {
                String when = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(new Date(lastSuccess));
                lastSync = getString(R.string.ultimo_aggiornamento, when);
            } else {
                lastSync = getString(R.string.ultimo_aggiornamento, getString(R.string.mai));
            }

            if (lastErr == null || lastErr.trim().isEmpty()) {
                lastError = getString(R.string.ultimo_errore, getString(R.string.nessuno));
            } else {
                lastError = getString(R.string.ultimo_errore, lastErr);
            }
        } catch (Exception e) {
            lastSync = getString(R.string.ultimo_aggiornamento, getString(R.string.errore_lettura));
            lastError = getString(R.string.ultimo_errore, getString(R.string.errore_lettura));
        }

        try {
            SharedPreferences spPdf = getSharedPreferences(PREFS_PDF, Context.MODE_PRIVATE);
            long lastCheck = spPdf.getLong(KEY_PDF_LAST_CHECK_MS, 0L);
            String lastErr = spPdf.getString(KEY_PDF_LAST_ERROR, "");

            if (lastCheck > 0L) {
                String when = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(new Date(lastCheck));
                pdfLastCheck = getString(R.string.pdf_ultimo_check, when);
            } else {
                pdfLastCheck = getString(R.string.pdf_ultimo_check, getString(R.string.mai));
            }

            if (lastErr == null || lastErr.trim().isEmpty()) {
                pdfLastError = getString(R.string.pdf_ultimo_errore, getString(R.string.nessuno));
            } else {
                pdfLastError = getString(R.string.pdf_ultimo_errore, lastErr);
            }
        } catch (Exception e) {
            pdfLastCheck = getString(R.string.pdf_ultimo_check, getString(R.string.errore_lettura));
            pdfLastError = getString(R.string.pdf_ultimo_errore, getString(R.string.errore_lettura));
        }

        String generatedAt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date());

        return "I Tuoi Versetti - Report diagnostico\n"
                + "Generato: " + generatedAt + "\n"
                + getString(R.string.stato_rete, net) + "\n"
                + dbText + "\n"
                + indexText + "\n"
                + lastSync + "\n"
                + lastError + "\n"
                + pdfLastCheck + "\n"
                + pdfLastError;
    }

    private void refreshStatus() {
        tvNetStatus.setText(getString(R.string.stato_rete, isConnected() ? getString(R.string.online) : getString(R.string.offline)));

        executor.execute(() -> {
            String dbText;
            String indexText;
            String lastSyncText;
            String lastErrorText;
            String pdfLastCheckText;
            String pdfLastErrorText;

            try {
                long count = BibleDb.get(getApplicationContext()).verseDao().countAll();
                dbText = getString(R.string.stato_db, count);
            } catch (Exception e) {
                dbText = getString(R.string.stato_db_errore, e.getClass().getSimpleName());
            }

            try {
                List<WorkInfo> infos = WorkManager.getInstance(getApplicationContext())
                        .getWorkInfosForUniqueWork("bible_index")
                        .get();

                if (infos == null || infos.isEmpty()) {
                    indexText = getString(R.string.stato_indicizzazione, getString(R.string.nessun_job));
                } else {
                    WorkInfo wi = infos.get(0);
                    int pct = wi.getProgress().getInt("pct", 0);
                    String stage = wi.getProgress().getString("stage");
                    String detail = wi.getState().name();
                    if (stage != null && !stage.trim().isEmpty()) {
                        detail = detail + " (" + pct + "%, " + stage + ")";
                    } else if (pct > 0) {
                        detail = detail + " (" + pct + "%)";
                    }
                    indexText = getString(R.string.stato_indicizzazione, detail);
                }
            } catch (Exception e) {
                indexText = getString(R.string.stato_indicizzazione, getString(R.string.errore_lettura));
            }

            try {
                SharedPreferences sp = getSharedPreferences(PREFS_INDEX, Context.MODE_PRIVATE);
                long lastSuccess = sp.getLong(KEY_LAST_SUCCESS_MS, 0L);
                String lastError = sp.getString(KEY_LAST_ERROR, "");

                if (lastSuccess > 0L) {
                    String when = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(new Date(lastSuccess));
                    lastSyncText = getString(R.string.ultimo_aggiornamento, when);
                } else {
                    lastSyncText = getString(R.string.ultimo_aggiornamento, getString(R.string.mai));
                }

                if (lastError == null || lastError.trim().isEmpty()) {
                    lastErrorText = getString(R.string.ultimo_errore, getString(R.string.nessuno));
                } else {
                    lastErrorText = getString(R.string.ultimo_errore, lastError);
                }
            } catch (Exception e) {
                lastSyncText = getString(R.string.ultimo_aggiornamento, getString(R.string.errore_lettura));
                lastErrorText = getString(R.string.ultimo_errore, getString(R.string.errore_lettura));
            }

            try {
                SharedPreferences spPdf = getSharedPreferences(PREFS_PDF, Context.MODE_PRIVATE);
                long lastCheck = spPdf.getLong(KEY_PDF_LAST_CHECK_MS, 0L);
                String lastErr = spPdf.getString(KEY_PDF_LAST_ERROR, "");

                if (lastCheck > 0L) {
                    String when = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(new Date(lastCheck));
                    pdfLastCheckText = getString(R.string.pdf_ultimo_check, when);
                } else {
                    pdfLastCheckText = getString(R.string.pdf_ultimo_check, getString(R.string.mai));
                }

                if (lastErr == null || lastErr.trim().isEmpty()) {
                    pdfLastErrorText = getString(R.string.pdf_ultimo_errore, getString(R.string.nessuno));
                } else {
                    pdfLastErrorText = getString(R.string.pdf_ultimo_errore, lastErr);
                }
            } catch (Exception e) {
                pdfLastCheckText = getString(R.string.pdf_ultimo_check, getString(R.string.errore_lettura));
                pdfLastErrorText = getString(R.string.pdf_ultimo_errore, getString(R.string.errore_lettura));
            }

            String finalDbText = dbText;
            String finalIndexText = indexText;
            String finalLastSyncText = lastSyncText;
            String finalLastErrorText = lastErrorText;
            String finalPdfLastCheckText = pdfLastCheckText;
            String finalPdfLastErrorText = pdfLastErrorText;
            runOnUiThread(() -> {
                tvDbStatus.setText(finalDbText);
                tvIndexStatus.setText(finalIndexText);
                tvLastSync.setText(finalLastSyncText);
                tvLastError.setText(finalLastErrorText);
                tvPdfLastCheck.setText(finalPdfLastCheckText);
                tvPdfLastError.setText(finalPdfLastErrorText);
            });
        });
    }

    private boolean isConnected() {
        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        android.net.Network n = cm.getActiveNetwork();
        if (n == null) return false;

        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(n);
        return caps != null
                && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}

package com.testing.ituoiversetti;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class BatchVerseFetchActivity extends AppCompatActivity {

    private AutoCompleteTextView libroView;
    private EditText capitoloView, versettoInView, versettoFinView, iterazioniView;
    private Button startBtn, stopBtn;
    private TextView outputView;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_verse_fetch);

        libroView          = findViewById(R.id.batchLibro);
        capitoloView       = findViewById(R.id.batchCapitolo);
        versettoInView     = findViewById(R.id.batchVersettoIn);
        versettoFinView    = findViewById(R.id.batchVersettoFin);
        iterazioniView     = findViewById(R.id.batchIterazioni);
        startBtn           = findViewById(R.id.batchStartBtn);
        stopBtn            = findViewById(R.id.batchStopBtn);
        outputView         = findViewById(R.id.batchOutput);

        stopBtn.setEnabled(false);

        setupBookSuggestions();

        startBtn.setOnClickListener(v -> onStartClicked());
        stopBtn .setOnClickListener(v -> onStopClicked());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRequested.set(true);
        executor.shutdown();
    }

    // -------------------------------------------------------------------------
    // Button handlers
    // -------------------------------------------------------------------------

    private void onStartClicked() {
        String libro = libroView.getText().toString().trim();
        int capitolo = parseInt(capitoloView);
        int vIn      = parseInt(versettoInView);
        int vFin     = parseInt(versettoFinView);
        int iter     = parseInt(iterazioniView);

        if (libro.isEmpty() || capitolo <= 0 || vIn <= 0 || vFin < vIn || iter <= 0) {
            outputView.setText("Dati non validi. Controlla tutti i campi.");
            return;
        }

        stopRequested.set(false);
        startBtn.setEnabled(false);
        stopBtn .setEnabled(true);
        outputView.setText("Inizio batch...\n");

        batchFetch(libro, capitolo, vIn, vFin, iter);
    }

    private void onStopClicked() {
        stopRequested.set(true);
        stopBtn.setEnabled(false);
        outputView.append("\nInterruzione richiesta...");
    }

    // -------------------------------------------------------------------------
    // Autocomplete libri
    // -------------------------------------------------------------------------

    private void setupBookSuggestions() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new Bibbia().composeBibbia());
        libroView.setAdapter(adapter);
    }

    // -------------------------------------------------------------------------
    // Autocomplete capitoli
    // -------------------------------------------------------------------------

    private void setupChapterSuggestions() {
        WeakReference<BatchVerseFetchActivity> ref = new WeakReference<>(this);
        executor.execute(() -> {
            List<String> chapters = new ArrayList<>();
            String error = null;
            try {
                BibleDb db = BibleDb.get(getApplicationContext());
                androidx.sqlite.db.SupportSQLiteDatabase readableDb =
                        db.getOpenHelper().getReadableDatabase();
                try (android.database.Cursor c = readableDb.query(
                        "SELECT DISTINCT chapter_name FROM verses ORDER BY chapter_name")) {
                    while (c.moveToNext()) {
                        String value = c.getString(0);
                        if (value != null && !value.isBlank()) chapters.add(value);
                    }
                }
            } catch (Exception e) {
                error = "Errore suggerimenti capitolo: " + e.getMessage();
            }
            final List<String> finalChapters = chapters;
            final String finalError = error;
            handler.post(() -> {
                BatchVerseFetchActivity a = ref.get();
                if (a == null || a.isDestroyed() || a.isFinishing()) return;
                if (finalError != null) {
                    a.outputView.setText(finalError);
                } else {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            a,
                            android.R.layout.simple_dropdown_item_1line,
                            finalChapters);
                }
            });
        });
    }

    // -------------------------------------------------------------------------
    // Batch logic
    // -------------------------------------------------------------------------

    private void batchFetch(String libro, int capitoloInizio, int vIn, int vFin, int totalIter) {
        WeakReference<BatchVerseFetchActivity> ref = new WeakReference<>(this);
        scheduleIteration(ref, libro, capitoloInizio, vIn, vFin, totalIter, 0, 0);
    }

    private void scheduleIteration(
            WeakReference<BatchVerseFetchActivity> ref,
            String libro, int capitoloInizio,
            int vIn, int vFin,
            int totalIter, int currentIter, int capOffset) {

        long delay = (currentIter == 0) ? 0L : 5000L;

        handler.postDelayed(() -> {
            BatchVerseFetchActivity a = ref.get();
            if (a == null || a.isDestroyed() || a.isFinishing()) return;

            if (currentIter >= totalIter) {
                a.outputView.append("\nBatch completato.");
                a.startBtn.setEnabled(true);
                a.stopBtn .setEnabled(false);
                return;
            }

            if (a.stopRequested.get()) {
                a.outputView.append("\nBatch interrotto.");
                a.startBtn.setEnabled(true);
                a.stopBtn .setEnabled(false);
                return;
            }

            int cap = capitoloInizio + capOffset;
            a.outputView.append("Ricerca " + (currentIter + 1) + "/" + totalIter
                    + ": " + libro + " " + cap + ":" + vIn + "-" + vFin + "\n");

            a.executor.execute(() -> {
                String result;
                boolean success = true;
                try {
                    result = MainActivity.searchDbThenWebAndCacheStatic(
                            a.getApplicationContext(), libro, cap, vIn, vFin);
                } catch (Exception e) {
                    result = "ERRORE: " + e.getClass().getSimpleName() + " - " + e.getMessage();
                    success = false;
                    writeErrorLog(a, e);
                }

                final String finalResult = result;
                final boolean finalSuccess = success;
                handler.post(() -> {
                    BatchVerseFetchActivity act = ref.get();
                    if (act == null || act.isDestroyed() || act.isFinishing()) return;
                    act.outputView.append((finalSuccess ? "OK: " : "") + finalResult + "\n\n");

                    scheduleIteration(ref, libro, capitoloInizio,
                            vIn, vFin, totalIter, currentIter + 1, capOffset + 1);
                });
            });

        }, delay);
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private void writeErrorLog(BatchVerseFetchActivity activity, Exception e) {
        try {
            java.io.File logFile = new java.io.File(
                    activity.getExternalFilesDir(null), "crash_log.txt");
            try (FileWriter fw = new FileWriter(logFile, true)) {
                fw.write(new java.util.Date() + " - " + e + "\n");
            }
        } catch (Exception ignored) {}
    }

    private int parseInt(EditText v) {
        try {
            return Integer.parseInt(v.getText().toString().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
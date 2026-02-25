package com.testing.ituoiversetti;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.io.FileWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class BatchVerseFetchActivity extends AppCompatActivity {

    private EditText libroView, capitoloView, versettoInView, versettoFinView, iterazioniView;
    private MaterialAutoCompleteTextView chapterSuggestView;
    private Button startBtn, stopBtn;
    private TextView outputView;

    // Executor dedicato a questo Activity, chiuso in onDestroy
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Flag per interrompere il batch dall'esterno
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_verse_fetch);

        libroView       = findViewById(R.id.batchLibro);
        chapterSuggestView = findViewById(R.id.chapterSuggestView);
        capitoloView    = findViewById(R.id.batchCapitolo);
        versettoInView  = findViewById(R.id.batchVersettoIn);
        versettoFinView = findViewById(R.id.batchVersettoFin);
        iterazioniView  = findViewById(R.id.batchIterazioni);
        startBtn        = findViewById(R.id.batchStartBtn);
        stopBtn         = findViewById(R.id.batchStopBtn);
        outputView      = findViewById(R.id.batchOutput);

        stopBtn.setEnabled(false);

        setupChapterSuggestions();

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
        String libro  = libroView.getText().toString().trim();
        int capitolo  = parseInt(capitoloView);
        int vIn       = parseInt(versettoInView);
        int vFin      = parseInt(versettoFinView);
        int iter      = parseInt(iterazioniView);

        if (libro.isEmpty() || capitolo <= 0 || vIn <= 0 || vFin < vIn || iter <= 0) {
            outputView.setText("Dati non validi");
            return;
        }

        stopRequested.set(false);
        startBtn.setEnabled(false);
        stopBtn .setEnabled(true);
        outputView.setText("Inizio batch...");

        batchFetch(libro, capitolo, vIn, vFin, iter);
    }

    private void onStopClicked() {
        stopRequested.set(true);
        stopBtn.setEnabled(false);
        outputView.append("\nInterruzione richiesta...");
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
                error = "Errore suggerimenti: " + e.getMessage();
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
                    a.chapterSuggestView.setAdapter(adapter);
                }
            });
        });
    }

    // -------------------------------------------------------------------------
    // Batch logic
    // -------------------------------------------------------------------------

    /**
     * Avvia il batch in modo ricorsivo-asincrono.
     * Ogni iterazione viene eseguita su executor (thread unico),
     * con un ritardo di 5 secondi tra un'iterazione e la successiva.
     * Usa WeakReference per non tenere in vita l'Activity.
     */
    private void batchFetch(String libro, int capitoloInizio, int vIn, int vFin, int totalIter) {
        WeakReference<BatchVerseFetchActivity> ref = new WeakReference<>(this);
        scheduleIteration(ref, libro, capitoloInizio, vIn, vFin, totalIter, 0, 0);
    }

    private void scheduleIteration(
            WeakReference<BatchVerseFetchActivity> ref,
            String libro, int capitoloInizio,
            int vIn, int vFin,
            int totalIter, int currentIter, int capOffset) {

        // Ritardo 0 per la prima iterazione, 5000ms per le successive
        long delay = (currentIter == 0) ? 0L : 5000L;

        handler.postDelayed(() -> {
            BatchVerseFetchActivity a = ref.get();
            if (a == null || a.isDestroyed() || a.isFinishing()) return;

            // Batch completato
            if (currentIter >= totalIter) {
                a.outputView.append("\nBatch completato.");
                a.startBtn.setEnabled(true);
                a.stopBtn .setEnabled(false);
                return;
            }

            // Stop richiesto dall'utente
            if (a.stopRequested.get()) {
                a.outputView.append("\nBatch interrotto.");
                a.startBtn.setEnabled(true);
                a.stopBtn .setEnabled(false);
                return;
            }

            int cap = capitoloInizio + capOffset;
            a.outputView.append("\nRicerca " + (currentIter + 1) + "/" + totalIter
                    + ": " + libro + " " + cap + ": " + vIn + "-" + vFin);

            // Esegue la ricerca sul thread dell'executor (non crea un nuovo Thread ogni volta)
            a.executor.execute(() -> {
                String result;
                boolean success = true;
                try {
                    result = MainActivity.searchDbThenWebAndCacheStatic(
                            a.getApplicationContext(), libro, cap, vIn, vFin);
                } catch (Exception e) {
                    result = "ERRORE: " + e.getClass().getSimpleName() + " - " + e.getMessage();
                    success = false;
                    // Scrittura log su thread background — corretto
                    writeErrorLog(a, e);
                }

                final String finalResult = result;
                final boolean finalSuccess = success;
                handler.post(() -> {
                    BatchVerseFetchActivity act = ref.get();
                    if (act == null || act.isDestroyed() || act.isFinishing()) return;
                    act.outputView.append("\n" + (finalSuccess ? "Risultato: " : "") + finalResult);

                    // Pianifica l'iterazione successiva
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
        } catch (Exception ignored) {
            // Se non riesce a scrivere il log, non blocchiamo il batch
        }
    }

    private int parseInt(EditText v) {
        try {
            return Integer.parseInt(v.getText().toString().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
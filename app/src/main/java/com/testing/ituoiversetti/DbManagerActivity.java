package com.testing.ituoiversetti;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DbManagerActivity extends AppCompatActivity {

    private TextView dbInfoView;
    private Button btnDeleteDb, btnExportDb, btnImportDb;
    private TextView sqlInputView;
    private Button btnExecSql;

    private static final int MAX_SQL_ROWS_DISPLAY = 100;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_manager);

        dbInfoView        = findViewById(R.id.dbInfoView);
        btnDeleteDb       = findViewById(R.id.btnDeleteDb);
        btnExportDb       = findViewById(R.id.btnExportDb);
        btnImportDb       = findViewById(R.id.btnImportDb);
        sqlInputView      = findViewById(R.id.sqlInputView);
        btnExecSql        = findViewById(R.id.btnExecSql);

        refreshDbInfo();

        btnDeleteDb.setOnClickListener(v -> deleteDb());
        btnExportDb.setOnClickListener(v -> exportDb());
        btnImportDb.setOnClickListener(v -> importDb());
        btnExecSql .setOnClickListener(v -> execSql());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // -------------------------------------------------------------------------
    // UI helper — posts to main thread only if Activity is still alive
    // -------------------------------------------------------------------------

    /** Aggiorna dbInfoView in modo sicuro (Activity potrebbe essere già distrutta). */
    private void postSafe(String text) {
        WeakReference<DbManagerActivity> ref = new WeakReference<>(this);
        handler.post(() -> {
            DbManagerActivity a = ref.get();
            if (a != null && !a.isDestroyed() && !a.isFinishing()) {
                a.dbInfoView.setText(text);
            }
        });
    }

    // -------------------------------------------------------------------------
    // DB info — logica sincrona separata, richiamabile dentro executor
    // -------------------------------------------------------------------------

    /**
     * Legge le informazioni dal DB in modo sincrono.
     * DEVE essere chiamata già all'interno di executor.execute(...).
     */
    private String readDbInfoSync() {
        try {
            BibleDb db = BibleDb.get(getApplicationContext());
            long count = db.verseDao().countAll();
            java.io.File dbFile = getApplicationContext().getDatabasePath("bible.db");
            long size = dbFile.exists() ? dbFile.length() : 0;
            String sizeStr = android.text.format.Formatter.formatFileSize(this, size);
            return "Versetti nel DB: " + count + "\nDimensione file: " + sizeStr;
        } catch (Exception e) {
            return "Errore lettura info DB: " + e.getMessage();
        }
    }

    /** Avvia una lettura asincrona delle info DB e aggiorna la UI. */
    private void refreshDbInfo() {
        executor.execute(() -> postSafe(readDbInfoSync()));
    }

    // -------------------------------------------------------------------------
    // Chapter suggestions
    // -------------------------------------------------------------------------

    private void setupChapterSuggestions() {
        WeakReference<DbManagerActivity> ref = new WeakReference<>(this);
        executor.execute(() -> {
            List<String> chapters = new ArrayList<>();
            String error = null;
            try {
                BibleDb db = BibleDb.get(getApplicationContext());
                SupportSQLiteDatabase readableDb = db.getOpenHelper().getReadableDatabase();
                try (Cursor c = readableDb.query(
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
                DbManagerActivity a = ref.get();
                if (a == null || a.isDestroyed() || a.isFinishing()) return;
                if (finalError != null) {
                    a.dbInfoView.setText(finalError);
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
    // SQL execution
    // -------------------------------------------------------------------------

    private void execSql() {
        String sql = sqlInputView.getText().toString().trim();
        if (sql.isEmpty()) {
            dbInfoView.setText("Inserisci una query SQL");
            return;
        }
        executor.execute(() -> {
            try {
                BibleDb db = BibleDb.get(getApplicationContext());
                SupportSQLiteDatabase writableDb = db.getOpenHelper().getWritableDatabase();

                if (returnsResultSet(sql)) {
                    try (Cursor cursor = writableDb.query(sql)) {
                        String result = formatCursorResults(cursor);
                        postSafe("Risultato query:\n" + result);
                    }
                } else {
                    writableDb.execSQL(sql);
                    // FIX: chiamata sincrona a readDbInfoSync() — nessun executor annidato
                    String info = readDbInfoSync();
                    postSafe("Query eseguita con successo\n" + info);
                }
            } catch (Exception e) {
                postSafe("Errore SQL: " + e.getMessage());
            }
        });
    }

    private boolean returnsResultSet(String sql) {
        String trimmed = sql.trim().toLowerCase(Locale.ROOT);
        return trimmed.startsWith("select")
                || trimmed.startsWith("pragma")
                || trimmed.startsWith("with");
    }

    private String formatCursorResults(Cursor cursor) {
        String[] columns = cursor.getColumnNames();
        StringBuilder builder = new StringBuilder();
        builder.append("Colonne: ").append(String.join(", ", columns)).append("\n");

        int rowIndex = 0;
        int totalRows = 0; // contiamo durante l'iterazione, evitiamo getCount()
        while (cursor.moveToNext()) {
            totalRows++;
            if (rowIndex < MAX_SQL_ROWS_DISPLAY) {
                if (rowIndex > 0) builder.append("\n");
                builder.append("Riga ").append(rowIndex + 1).append(": ");
                for (int i = 0; i < columns.length; i++) {
                    if (i > 0) builder.append(", ");
                    builder.append(columns[i]).append("=");
                    builder.append(cursor.isNull(i) ? "null" : cursor.getString(i));
                }
                rowIndex++;
            }
        }

        int omitted = totalRows - MAX_SQL_ROWS_DISPLAY;
        if (omitted > 0) {
            builder.append("\n... ").append(omitted).append(" righe omesse");
        }
        if (totalRows == 0) {
            builder.append("Nessuna riga restituita");
        }
        return builder.toString();
    }

    // -------------------------------------------------------------------------
    // Delete DB
    // -------------------------------------------------------------------------

    private void deleteDb() {
        executor.execute(() -> {
            try {
                // Chiudiamo il DB prima di cancellare il file
                BibleDb db = BibleDb.get(getApplicationContext());
                db.close();

                java.io.File dbFile = getApplicationContext().getDatabasePath("bible.db");
                boolean deleted = dbFile.delete();
                postSafe(deleted
                        ? "DB cancellato con successo"
                        : "Impossibile cancellare il DB (forse già cancellato)");
            } catch (Exception e) {
                postSafe("Errore cancellazione DB: " + e.getMessage());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Export DB
    // -------------------------------------------------------------------------

    private void exportDb() {
        executor.execute(() -> {
            try {
                BibleDb db = BibleDb.get(getApplicationContext());
                SupportSQLiteDatabase readableDb = db.getOpenHelper().getReadableDatabase();

                // FIX: forziamo il checkpoint WAL prima di copiare il file
                try (Cursor ignored = readableDb.query("PRAGMA wal_checkpoint(FULL)")) {
                    // eseguiamo solo per effetto collaterale
                }

                java.io.File dbFile = getApplicationContext().getDatabasePath("bible.db");
                if (!dbFile.exists()) {
                    postSafe("DB non trovato");
                    return;
                }

                java.io.File exportDir = getApplicationContext().getExternalFilesDir(null);
                if (exportDir == null) {
                    postSafe("Cartella esterna non disponibile");
                    return;
                }

                String exportName = "bible_export_" + System.currentTimeMillis() + ".db";
                java.io.File exportFile = new java.io.File(exportDir, exportName);
                java.nio.file.Files.copy(dbFile.toPath(), exportFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                postSafe("DB esportato in:\n" + exportFile.getAbsolutePath());
            } catch (Exception e) {
                postSafe("Errore esportazione DB: " + e.getMessage());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Import DB
    // -------------------------------------------------------------------------

    private void importDb() {
        executor.execute(() -> {
            try {
                java.io.File importDir = getApplicationContext().getExternalFilesDir(null);
                if (importDir == null) {
                    postSafe("Cartella esterna non disponibile");
                    return;
                }

                java.io.File[] files = importDir.listFiles(
                        (dir, name) -> name.startsWith("bible_export_") && name.endsWith(".db"));
                if (files == null || files.length == 0) {
                    postSafe("Nessun file di backup trovato");
                    return;
                }

                // Selezioniamo il file più recente
                java.io.File latest = files[0];
                for (java.io.File f : files) {
                    if (f.lastModified() > latest.lastModified()) latest = f;
                }

                // Chiudiamo il DB prima di sovrascrivere il file
                BibleDb.get(getApplicationContext()).close();

                java.io.File dbFile = getApplicationContext().getDatabasePath("bible.db");
                java.nio.file.Files.copy(latest.toPath(), dbFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                postSafe("DB importato da: " + latest.getName());
            } catch (Exception e) {
                postSafe("Errore importazione DB: " + e.getMessage());
            }
        });
    }
}
package com.testing.ituoiversetti;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class DbManagerActivity extends AppCompatActivity {
    private TextView dbInfoView;
    private Button btnDeleteDb, btnExportDb, btnImportDb;
    private TextView sqlInputView;
    private Button btnExecSql;

    // Executor condiviso, chiuso in onDestroy
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_manager);

        dbInfoView = findViewById(R.id.dbInfoView);
        btnDeleteDb = findViewById(R.id.btnDeleteDb);
        btnExportDb = findViewById(R.id.btnExportDb);
        btnImportDb = findViewById(R.id.btnImportDb);
        sqlInputView = findViewById(R.id.sqlInputView);
        btnExecSql = findViewById(R.id.btnExecSql);

        getDbInfo(info -> dbInfoView.setText(info));

        btnDeleteDb.setOnClickListener(v -> deleteDb());
        btnExportDb.setOnClickListener(v -> exportDb());
        btnImportDb.setOnClickListener(v -> importDb());
        btnExecSql.setOnClickListener(v -> execSql());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void getDbInfo(Consumer<String> callback) {
        executor.execute(() -> {
            String result;
            try {
                BibleDb db = BibleDb.get(getApplicationContext());
                long count = db.verseDao().countAll();
                java.io.File dbFile = getApplicationContext().getDatabasePath("bible.db");
                long size = dbFile.exists() ? dbFile.length() : 0;
                String sizeStr = android.text.format.Formatter.formatFileSize(this, size);
                result = "Versetti nel DB: " + count + "\nDimensione file: " + sizeStr;
            } catch (Exception e) {
                result = "Errore lettura info DB: " + e.getMessage();
            }
            String finalResult = result;
            handler.post(() -> callback.accept(finalResult));
        });
    }

    private void execSql() {
        String sql = sqlInputView.getText().toString().trim();
        if (sql.isEmpty()) {
            dbInfoView.setText("Inserisci una query SQL");
            return;
        }
        executor.execute(() -> {
            try {
                BibleDb db = BibleDb.get(getApplicationContext());
                db.getOpenHelper().getWritableDatabase().execSQL(sql);
                getDbInfo(info -> dbInfoView.setText("Query eseguita con successo\n" + info));
            } catch (Exception e) {
                handler.post(() -> dbInfoView.setText("Errore SQL: " + e.getMessage()));
            }
        });
    }

    private void deleteDb() {
        executor.execute(() -> {
            try {
                BibleDb db = BibleDb.get(getApplicationContext());
                db.close();
                java.io.File dbFile = getApplicationContext().getDatabasePath("bible.db");
                boolean deleted = dbFile.delete();
                handler.post(() -> dbInfoView.setText(
                    deleted ? "DB cancellato con successo" : "Impossibile cancellare il DB (forse già cancellato)"
                ));
            } catch (Exception e) {
                handler.post(() -> dbInfoView.setText("Errore cancellazione DB: " + e.getMessage()));
            }
        });
    }

    private void exportDb() {
        executor.execute(() -> {
            try {
                java.io.File dbFile = getApplicationContext().getDatabasePath("bible.db");
                if (!dbFile.exists()) {
                    handler.post(() -> dbInfoView.setText("DB non trovato"));
                    return;
                }
                java.io.File exportDir = getApplicationContext().getExternalFilesDir(null);
                if (exportDir == null) {
                    handler.post(() -> dbInfoView.setText("Cartella esterna non disponibile"));
                    return;
                }
                String exportName = "bible_export_" + System.currentTimeMillis() + ".db";
                java.io.File exportFile = new java.io.File(exportDir, exportName);
                java.nio.file.Files.copy(dbFile.toPath(), exportFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                handler.post(() -> dbInfoView.setText("DB esportato in:\n" + exportFile.getAbsolutePath()));
            } catch (Exception e) {
                handler.post(() -> dbInfoView.setText("Errore esportazione DB: " + e.getMessage()));
            }
        });
    }

    private void importDb() {
        executor.execute(() -> {
            try {
                java.io.File importDir = getApplicationContext().getExternalFilesDir(null);
                if (importDir == null) {
                    handler.post(() -> dbInfoView.setText("Cartella esterna non disponibile"));
                    return;
                }
                java.io.File[] files = importDir.listFiles(
                    (dir, name) -> name.startsWith("bible_export_") && name.endsWith(".db")
                );
                if (files == null || files.length == 0) {
                    handler.post(() -> dbInfoView.setText("Nessun file di backup trovato"));
                    return;
                }
                java.io.File latest = files[0];
                for (java.io.File f : files) {
                    if (f.lastModified() > latest.lastModified()) latest = f;
                }
                BibleDb.get(getApplicationContext()).close();
                java.io.File dbFile = getApplicationContext().getDatabasePath("bible.db");
                java.nio.file.Files.copy(latest.toPath(), dbFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                java.io.File finalLatest = latest;
                handler.post(() -> dbInfoView.setText("DB importato da: " + finalLatest.getName()));
            } catch (Exception e) {
                handler.post(() -> dbInfoView.setText("Errore importazione DB: " + e.getMessage()));
            }
        });
    }
}
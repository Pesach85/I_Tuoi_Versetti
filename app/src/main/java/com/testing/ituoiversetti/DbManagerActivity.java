package com.testing.ituoiversetti;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DbManagerActivity extends AppCompatActivity {

    private ScrollView dbInfoScroll;
    private TextView dbInfoView;
    private Button btnDeleteDb, btnExportDb, btnImportDb;
    private EditText sqlInputView;
    private Button btnExecSql;

    private AutoCompleteTextView quickBookView;
    private AutoCompleteTextView quickChapterView;
    private AutoCompleteTextView quickVerseView;
    private EditText quickTextView;
    private Button btnQuickLoad, btnQuickSave, btnPresetSelect, btnPresetUpdate;

    private static final int MAX_SQL_ROWS_DISPLAY = 100;
    private static final String EXPORT_PREFIX = "bible_export_";
    private static final String EXPORT_SUFFIX = ".db";
    private static final String EXPORT_SUBDIR = "I_Tuoi_Versetti";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final InputCheck inputCheck = new InputCheck();
    private PopupWindow activeSuggestionPopup;
    private List<String> allBooks = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_manager);

        dbInfoScroll      = findViewById(R.id.dbInfoScroll);
        dbInfoView        = findViewById(R.id.dbInfoView);
        btnDeleteDb       = findViewById(R.id.btnDeleteDb);
        btnExportDb       = findViewById(R.id.btnExportDb);
        btnImportDb       = findViewById(R.id.btnImportDb);
        sqlInputView      = findViewById(R.id.sqlInputView);
        btnExecSql        = findViewById(R.id.btnExecSql);
        quickBookView     = findViewById(R.id.quickBookView);
        quickChapterView  = findViewById(R.id.quickChapterView);
        quickVerseView    = findViewById(R.id.quickVerseView);
        quickTextView     = findViewById(R.id.quickTextView);
        btnQuickLoad      = findViewById(R.id.btnQuickLoad);
        btnQuickSave      = findViewById(R.id.btnQuickSave);
        btnPresetSelect   = findViewById(R.id.btnPresetSelect);
        btnPresetUpdate   = findViewById(R.id.btnPresetUpdate);

        setupQuickEditors();

        refreshDbInfo();

        btnDeleteDb.setOnClickListener(v -> deleteDb());
        btnExportDb.setOnClickListener(v -> exportDb());
        btnImportDb.setOnClickListener(v -> importDb());
        btnExecSql .setOnClickListener(v -> execSql());
        btnQuickLoad.setOnClickListener(v -> loadQuickVerse());
        btnQuickSave.setOnClickListener(v -> saveQuickVerse());
        btnPresetSelect.setOnClickListener(v -> fillPresetSelectSql());
        btnPresetUpdate.setOnClickListener(v -> fillPresetUpdateSql());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeSuggestionPopup != null) activeSuggestionPopup.dismiss();
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
                if (a.dbInfoScroll != null) {
                    a.dbInfoScroll.post(() -> a.dbInfoScroll.fullScroll(android.view.View.FOCUS_DOWN));
                }
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
    // Quick row editor
    // -------------------------------------------------------------------------

    private void setupQuickEditors() {
        allBooks = new Bibbia().composeBibbia();

        quickBookView.setOnClickListener(v -> showGridPopup(
                quickBookView,
                allBooks,
                4,
                picked -> {
                    quickBookView.setText(picked, false);
                    quickChapterView.setText("");
                    quickVerseView.setText("1", false);
                }));

        quickBookView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && safeText(quickBookView).isEmpty()) {
                showGridPopup(quickBookView, allBooks, 4, picked -> {
                    quickBookView.setText(picked, false);
                    quickChapterView.setText("");
                    quickVerseView.setText("1", false);
                });
            }
        });

        quickChapterView.setOnClickListener(v -> {
            String bookInput = safeText(quickBookView);
            if (bookInput.isEmpty()) return;
            List<String> chapters = buildChapterSuggestions(bookInput);
            if (chapters.isEmpty()) return;
            showGridPopup(quickChapterView, chapters, 8,
                    picked -> quickChapterView.setText(picked, false));
        });

        quickVerseView.setOnClickListener(v -> {
            List<String> verses = new ArrayList<>();
            for (int i = 1; i <= 200; i++) verses.add(String.valueOf(i));
            showGridPopup(quickVerseView, verses, 8,
                    picked -> quickVerseView.setText(picked, false));
        });
    }

    private List<String> buildChapterSuggestions(String bookInput) {
        try {
            String normalized = normalizeBookInput(bookInput);
            if (normalized.isEmpty()) return Collections.emptyList();
            NumCapitoli caps = new NumCapitoli();
            caps.selectCapN(normalized);
            return new ArrayList<>(caps.caps);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private void loadQuickVerse() {
        String book = normalizeBookInput(safeText(quickBookView));
        Integer chapter = parseIntOrNull(quickChapterView);
        Integer verse = parseIntOrNull(quickVerseView);

        if (book.isEmpty() || chapter == null || verse == null) {
            toast("Inserisci libro, capitolo e versetto");
            return;
        }

        executor.execute(() -> {
            try {
                BibleDb db = BibleDb.get(getApplicationContext());
                List<String> keys = BookNameUtil.candidateKeys(book);
                VerseEntity row = db.verseDao().findOne(keys, chapter, verse);
                if (row == null || row.text == null) {
                    postSafe("Riga non trovata nel DB per " + book + " " + chapter + ":" + verse);
                    handler.post(() -> quickTextView.setText(""));
                    return;
                }
                final String text = row.text;
                final String foundBook = row.bookKey;
                postSafe("Riga caricata: " + foundBook + " " + chapter + ":" + verse);
                handler.post(() -> quickTextView.setText(text));
            } catch (Exception e) {
                postSafe("Errore lettura riga: " + e.getMessage());
            }
        });
    }

    private void saveQuickVerse() {
        String book = normalizeBookInput(safeText(quickBookView));
        Integer chapter = parseIntOrNull(quickChapterView);
        Integer verse = parseIntOrNull(quickVerseView);
        String newText = safeText(quickTextView);

        if (book.isEmpty() || chapter == null || verse == null) {
            toast("Inserisci libro, capitolo e versetto");
            return;
        }
        if (newText.isEmpty()) {
            toast("Inserisci il nuovo testo del versetto");
            return;
        }

        executor.execute(() -> {
            try {
                BibleDb db = BibleDb.get(getApplicationContext());
                List<String> keys = BookNameUtil.candidateKeys(book);
                VerseEntity row = db.verseDao().findOne(keys, chapter, verse);
                if (row == null || row.bookKey == null || row.bookKey.trim().isEmpty()) {
                    postSafe("Impossibile aggiornare: riga non trovata");
                    return;
                }
                int updated = db.verseDao().updateVerseText(row.bookKey, chapter, verse, newText);
                String info = readDbInfoSync();
                postSafe((updated > 0 ? "Riga aggiornata con successo" : "Nessuna riga aggiornata")
                        + "\n" + info);
            } catch (Exception e) {
                postSafe("Errore aggiornamento riga: " + e.getMessage());
            }
        });
    }

    private void fillPresetSelectSql() {
        String book = normalizeBookInput(safeText(quickBookView));
        Integer chapter = parseIntOrNull(quickChapterView);
        Integer verse = parseIntOrNull(quickVerseView);
        if (book.isEmpty() || chapter == null || verse == null) {
            toast("Compila libro/capitolo/versetto per query preimpostata");
            return;
        }
        String bookKey = BookNameUtil.key(book);
        String sql = "SELECT bookKey, chapter, verse, text FROM verses WHERE bookKey = '" + bookKey
                + "' AND chapter = " + chapter + " AND verse = " + verse + ";";
        sqlInputView.setText(sql);
    }

    private void fillPresetUpdateSql() {
        String book = normalizeBookInput(safeText(quickBookView));
        Integer chapter = parseIntOrNull(quickChapterView);
        Integer verse = parseIntOrNull(quickVerseView);
        String newText = safeText(quickTextView);
        if (book.isEmpty() || chapter == null || verse == null || newText.isEmpty()) {
            toast("Compila libro/capitolo/versetto e testo per UPDATE preimpostata");
            return;
        }
        String bookKey = BookNameUtil.key(book);
        String escaped = newText.replace("'", "''");
        String sql = "UPDATE verses SET text = '" + escaped + "' WHERE bookKey = '"
                + bookKey + "' AND chapter = " + chapter + " AND verse = " + verse + ";";
        sqlInputView.setText(sql);
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

                File dbFile = getApplicationContext().getDatabasePath("bible.db");
                if (!dbFile.exists()) {
                    postSafe("DB non trovato");
                    return;
                }

                String exportName = EXPORT_PREFIX + System.currentTimeMillis() + EXPORT_SUFFIX;
                String exportPath = exportDbToUserFolder(dbFile, exportName);
                postSafe("DB esportato in:\n" + exportPath);
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
                // Chiudiamo il DB prima di sovrascrivere il file
                BibleDb.get(getApplicationContext()).close();

                File dbFile = getApplicationContext().getDatabasePath("bible.db");
                String sourceName = importLatestBackupInto(dbFile);
                if (sourceName == null) {
                    postSafe("Nessun file di backup trovato (Download/Documenti/app)");
                    return;
                }

                postSafe("DB importato da: " + sourceName);
            } catch (Exception e) {
                postSafe("Errore importazione DB: " + e.getMessage());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Export/import helpers
    // -------------------------------------------------------------------------

    private String exportDbToUserFolder(File sourceDbFile, String exportName) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String inDownloads = exportViaMediaStore(sourceDbFile, exportName,
                    Environment.DIRECTORY_DOWNLOADS);
            if (inDownloads != null) return inDownloads;

            String inDocs = exportViaMediaStore(sourceDbFile, exportName,
                    Environment.DIRECTORY_DOCUMENTS);
            if (inDocs != null) return inDocs;
        }

        String legacy = exportToLegacyPublicFolder(sourceDbFile, exportName,
                Environment.DIRECTORY_DOWNLOADS);
        if (legacy != null) return legacy;

        String legacyDocs = exportToLegacyPublicFolder(sourceDbFile, exportName,
                Environment.DIRECTORY_DOCUMENTS);
        if (legacyDocs != null) return legacyDocs;

        throw new IOException("Nessuna cartella pubblica disponibile");
    }

    private String exportViaMediaStore(File sourceDbFile,
                                       String exportName,
                                       String relativeRoot) {
        try {
            ContentResolver resolver = getContentResolver();
            Uri collection = MediaStore.Files.getContentUri("external");
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, exportName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeRoot + "/" + EXPORT_SUBDIR);

            Uri itemUri = resolver.insert(collection, values);
            if (itemUri == null) return null;

            try (InputStream in = new FileInputStream(sourceDbFile);
                 OutputStream out = resolver.openOutputStream(itemUri, "w")) {
                if (out == null) return null;
                copyStream(in, out);
            }
            return itemUri.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String exportToLegacyPublicFolder(File sourceDbFile,
                                              String exportName,
                                              String directoryType) {
        try {
            File root = Environment.getExternalStoragePublicDirectory(directoryType);
            if (root == null) return null;
            File dir = new File(root, EXPORT_SUBDIR);
            if (!dir.exists() && !dir.mkdirs()) return null;
            File target = new File(dir, exportName);
            try (InputStream in = new FileInputStream(sourceDbFile);
                 OutputStream out = new FileOutputStream(target)) {
                copyStream(in, out);
            }
            return target.getAbsolutePath();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String importLatestBackupInto(File targetDbFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String imported = importLatestFromMediaStore(targetDbFile);
            if (imported != null) return imported;
        }

        String legacyDownloads = importLatestFromLegacyFolder(targetDbFile,
                Environment.DIRECTORY_DOWNLOADS);
        if (legacyDownloads != null) return legacyDownloads;

        String legacyDocuments = importLatestFromLegacyFolder(targetDbFile,
                Environment.DIRECTORY_DOCUMENTS);
        if (legacyDocuments != null) return legacyDocuments;

        return importLatestFromAppExternal(targetDbFile);
    }

    private String importLatestFromMediaStore(File targetDbFile) {
        ContentResolver resolver = getContentResolver();
        Uri collection = MediaStore.Files.getContentUri("external");
        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME
        };
        String selection = MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ?";
        String[] selectionArgs = {EXPORT_PREFIX + "%" + EXPORT_SUFFIX};
        String order = MediaStore.MediaColumns.DATE_MODIFIED + " DESC";

        try (Cursor cursor = resolver.query(collection, projection, selection, selectionArgs, order)) {
            if (cursor == null || !cursor.moveToFirst()) return null;

            int idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int nameIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
            long id = cursor.getLong(idIdx);
            String displayName = cursor.getString(nameIdx);
            Uri itemUri = ContentUris.withAppendedId(collection, id);

            try (InputStream in = resolver.openInputStream(itemUri);
                 OutputStream out = new FileOutputStream(targetDbFile, false)) {
                if (in == null) return null;
                copyStream(in, out);
            }
            return displayName != null ? displayName : itemUri.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String importLatestFromLegacyFolder(File targetDbFile, String directoryType) {
        try {
            File root = Environment.getExternalStoragePublicDirectory(directoryType);
            if (root == null) return null;
            File dir = new File(root, EXPORT_SUBDIR);
            File[] files = dir.listFiles((d, name) ->
                    name != null && name.startsWith(EXPORT_PREFIX) && name.endsWith(EXPORT_SUFFIX));
            if (files == null || files.length == 0) return null;

            File latest = files[0];
            for (File f : files) if (f.lastModified() > latest.lastModified()) latest = f;

            try (InputStream in = new FileInputStream(latest);
                 OutputStream out = new FileOutputStream(targetDbFile, false)) {
                copyStream(in, out);
            }
            return latest.getName();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String importLatestFromAppExternal(File targetDbFile) {
        try {
            File importDir = getApplicationContext().getExternalFilesDir(null);
            if (importDir == null) return null;
            File[] files = importDir.listFiles((dir, name) ->
                    name != null && name.startsWith(EXPORT_PREFIX) && name.endsWith(EXPORT_SUFFIX));
            if (files == null || files.length == 0) return null;

            File latest = files[0];
            for (File f : files) if (f.lastModified() > latest.lastModified()) latest = f;
            try (InputStream in = new FileInputStream(latest);
                 OutputStream out = new FileOutputStream(targetDbFile, false)) {
                copyStream(in, out);
            }
            return latest.getName();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
        out.flush();
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private static String safeText(TextView view) {
        if (view == null || view.getText() == null) return "";
        return view.getText().toString().trim();
    }

    private static Integer parseIntOrNull(TextView view) {
        String value = safeText(view);
        if (value.isEmpty()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeBookInput(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return "";
        try {
            String fixed = inputCheck.setTitoloCorrected(trimmed);
            return fixed == null ? "" : fixed.trim();
        } catch (Exception ignored) {
            return trimmed;
        }
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    // -------------------------------------------------------------------------
    // Suggestion popup grid
    // -------------------------------------------------------------------------

    private void showGridPopup(AutoCompleteTextView anchor,
                               List<String> items,
                               int columns,
                               SuggestionPicker picker) {
        if (anchor == null || items == null || items.isEmpty()) return;
        if (activeSuggestionPopup != null && activeSuggestionPopup.isShowing()) {
            activeSuggestionPopup.dismiss();
        }

        GridView grid = new GridView(this);
        grid.setNumColumns(columns);
        grid.setHorizontalSpacing(dp(8));
        grid.setVerticalSpacing(dp(8));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setPadding(dp(8), dp(8), dp(8), dp(8));
        grid.setClipToPadding(false);
        grid.setAdapter(new SuggestionGridAdapter(items));
        grid.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            picker.onPick(items.get(position));
            if (activeSuggestionPopup != null) activeSuggestionPopup.dismiss();
        });

        int popupWidth = Math.max(anchor.getWidth(), dp(280));
        int popupHeight = columns >= 8 ? dp(280) : dp(340);
        PopupWindow popup = new PopupWindow(grid, popupWidth, popupHeight, true);
        popup.setOutsideTouchable(true);
        popup.setElevation(dp(6));
        popup.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_suggestion_popup));

        activeSuggestionPopup = popup;
        popup.showAsDropDown(anchor, 0, dp(6));
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private interface SuggestionPicker {
        void onPick(String value);
    }

    private final class SuggestionGridAdapter extends ArrayAdapter<String> {
        SuggestionGridAdapter(List<String> values) {
            super(DbManagerActivity.this, 0, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv;
            if (convertView instanceof TextView) {
                tv = (TextView) convertView;
            } else {
                tv = new TextView(DbManagerActivity.this);
                int pH = dp(12);
                int pV = dp(10);
                tv.setPadding(pH, pV, pH, pV);
                tv.setBackgroundResource(R.drawable.bg_suggestion_tile);
                tv.setTextColor(ContextCompat.getColor(DbManagerActivity.this, R.color.on_surface));
                tv.setTextSize(16f);
                tv.setGravity(android.view.Gravity.CENTER);
            }
            String value = getItem(position);
            tv.setText(value == null ? "" : value);
            return tv;
        }
    }
}
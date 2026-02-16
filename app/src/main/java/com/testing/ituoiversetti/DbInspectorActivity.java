package com.testing.ituoiversetti;

public class DbInspectorActivity extends androidx.appcompat.app.AppCompatActivity {

    private final java.util.concurrent.ExecutorService ex =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    private android.widget.TextView tv;

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_inspector);
        tv = findViewById(R.id.dbText);
        findViewById(R.id.btnRefresh).setOnClickListener(v -> refresh());
        refresh();
    }

    private void refresh() {
        tv.setText("Leggo DB...");
        ex.execute(() -> {
            BibleDb db = BibleDb.get(getApplicationContext());
            VerseDao dao = db.verseDao();

            long total = dao.countAll();
            java.util.List<BookCount> books = dao.listBooks();
            java.util.List<VerseDump> dump = dao.dump(40);

            StringBuilder sb = new StringBuilder();
            sb.append("TOTAL VERSES = ").append(total).append("\n\n");

            sb.append("BOOK KEYS:\n");
            for (BookCount bc : books) {
                sb.append(" - ").append(bc.bookKey).append(" -> ").append(bc.cnt).append("\n");
            }

            sb.append("\nSAMPLE (40):\n");
            for (VerseDump d : dump) {
                sb.append(d.bookKey).append(" ")
                        .append(d.chapter).append(":").append(d.verse)
                        .append("  ").append(d.text).append("\n");
            }

            runOnUiThread(() -> tv.setText(sb.toString()));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ex.shutdownNow();
    }
}

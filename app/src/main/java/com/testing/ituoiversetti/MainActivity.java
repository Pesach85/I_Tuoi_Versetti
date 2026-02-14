package com.testing.ituoiversetti;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    AutoCompleteTextView[] mEdit = new AutoCompleteTextView[5];
    ArrayAdapter<String> arrayAdapter;
    String libro = "";
    Integer capitolo = 0;
    Integer versetto_in = 0;
    Integer versetto_final = 0;
    Bibbia bibbia = new Bibbia();
    String testo = "";
    InputCheck ok = new InputCheck();

    // ✅ Thread pool per fare ricerca senza bloccare UI
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Se non l’hai già fatto altrove (meglio in Application.onCreate):
        // PdfParser.init(getApplicationContext());

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bibbia.composeBibbia());

        mEdit[0] = findViewById(R.id.autoCompleteTextView);   // Libro
        mEdit[1] = findViewById(R.id.autoCompleteTextView2);  // Capitolo
        mEdit[2] = findViewById(R.id.autoCompleteTextView3);  // Versetto IN
        mEdit[3] = findViewById(R.id.autoCompleteTextView4);  // Versetto FIN (opz.)
        mEdit[4] = findViewById(R.id.multiAutoCompleteTextView); // Output

        mEdit[0].setAdapter(arrayAdapter);

        final Button button = findViewById(R.id.button);

        button.setOnClickListener(v -> {
            // 1) Leggi input e valida (NO crash)
            String libroIn = safeText(mEdit[0]);
            libroIn = ok.setTitoloCorrected(libroIn);

            Integer cap = parseIntOrNull(mEdit[1]);
            Integer vIn = parseIntOrNull(mEdit[2]);
            Integer vFin = parseIntOrNull(mEdit[3]); // può essere null

            if (libroIn.isEmpty()) { toast("Inserisci il libro"); return; }
            if (cap == null)       { toast("Inserisci il capitolo"); return; }
            if (vIn == null)       { toast("Inserisci almeno un versetto"); return; }

            // 2) Imposta stato UI (facoltativo ma utile)
            button.setEnabled(false);
            mEdit[4].setText("Ricerca in corso...");

            // 3) Esegui ricerca in background
            final String finalLibro = libroIn;
            final int finalCap = cap;
            final int finalVIn = vIn;
            final Integer finalVFin = vFin;

            executor.execute(() -> {
                String result;
                try {
                    int capCorr = ok.setCapitoloCorrected(finalCap);
                    int vFinCorr = (finalVFin == null) ? finalVIn : finalVFin;

                    // ✅ qui chiamiamo un metodo che fa WEB e se fallisce fa OFFLINE PDF
                    result = searchVerseText(finalLibro, capCorr, finalVIn, vFinCorr);

                    final String show = finalLibro + " " + capCorr + ": " + result;

                    runOnUiThread(() -> {
                        mEdit[4].setText(show);
                        button.setEnabled(true);
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        mEdit[4].setText("Errore ricerca");
                        button.setEnabled(true);
                    });
                }
            });
        });

        final ImageButton button1 = findViewById(R.id.imageButton);
        button1.setOnClickListener(v -> {
            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setType("text/plain");
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, mEdit[4].getText().toString());
            try {
                MainActivity.this.startActivity(whatsappIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp")));
            }
        });
    }

    // ✅ Ricerca: WEB se possibile, altrimenti fallback PDF
    private String searchVerseText(String libro, int capitolo, int versettoIn, int versettoFin) throws IOException {
        // 1) DB first (offline fast)
        BibleDb db = BibleDb.get(this);
        long n = db.verseDao().countAll();
        if (n == 0) {
            return "Offline: indicizzazione in corso. Riprova tra poco.";
        }
    
        List<String> keys = BookNameUtil.candidateKeys(libro);
        List<VerseRow> rows = db.verseDao().getRange(keys, capitolo, versettoIn, versettoFin);
    
        if (rows != null && !rows.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (VerseRow r : rows) sb.append(r.verse).append(" ").append(r.text).append(" ");
            return sb.toString().trim();
        }
    
        // 2) Se online, fai web e (opzionale) salvi nel DB
        if (isConnected()) {
            Bibbia bibbia2 = new Bibbia();
            bibbia2.getWebContent(libro, capitolo, versettoIn, versettoFin);
            if (bibbia2.search && bibbia2.src != null && !bibbia2.src.trim().isEmpty()) {
                // opzionale: qui potresti “splittare” bibbia2.src e salvarla in DB,
                // ma per ora almeno la ritorni.
                return bibbia2.src;
            }
        }
    
        return "Non trovato offline";
    }


    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network n = cm.getActiveNetwork();
        if (n == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(n);
        return caps != null;
    }

    private static String safeText(AutoCompleteTextView v) {
        if (v == null || v.getText() == null) return "";
        return v.getText().toString().trim();
    }

    private static Integer parseIntOrNull(AutoCompleteTextView v) {
        String s = safeText(v);
        if (s.isEmpty()) return null;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return null; }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) return true;
        return super.onOptionsItemSelected(item);
    }
}

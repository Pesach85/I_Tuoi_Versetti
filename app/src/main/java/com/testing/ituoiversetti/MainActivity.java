package com.testing.ituoiversetti;

import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends AppCompatActivity {

    // UI
    private AutoCompleteTextView bookView;
    private AutoCompleteTextView chapterView;
    private AutoCompleteTextView verseFromView;
    private AutoCompleteTextView verseToView;
    private MultiAutoCompleteTextView outputView;

    private ArrayAdapter<String> arrayAdapter;

    // Logic
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final InputCheck ok = new InputCheck();

    // Stato UI: evita che il progresso sovrascriva un risultato “finale”
    private volatile boolean userSearchInProgress = false;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar tb = findViewById(R.id.toolbar);
        if (tb != null) setSupportActionBar(tb);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("I Tuoi Versetti");
}


        bookView      = findViewById(R.id.autoCompleteTextView);
        chapterView   = findViewById(R.id.autoCompleteTextView2);
        verseFromView = findViewById(R.id.autoCompleteTextView3);
        verseToView   = findViewById(R.id.autoCompleteTextView4);
        outputView    = findViewById(R.id.multiAutoCompleteTextView);

        // Lista libri: puoi usare Bibbia.composeBibbia() senza istanziare Bibbia come campo
        arrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                new Bibbia().composeBibbia());
        bookView.setAdapter(arrayAdapter);

        Button searchBtn = findViewById(R.id.button);
        ImageButton whatsappBtn = findViewById(R.id.imageButton);

        // Se DB è vuoto, fai partire subito l’indicizzazione (senza aspettare il click)
        executor.execute(() -> {
            long c = BibleDb.get(getApplicationContext()).verseDao().countAll();
            if (c == 0) {
                ensureIndexWorkEnqueued();
                runOnUiThread(() -> outputView.setText("Offline: indicizzazione in corso..."));
            }
        });

        // Observer progresso indicizzazione DB
        WorkManager.getInstance(this)
            .getWorkInfosForUniqueWorkLiveData("bible_index")
            .observe(this, infos -> {
                if (infos == null || infos.isEmpty()) return;
                if (userSearchInProgress) return;   // <--- IMPORTANTISSIMO
            
                WorkInfo wi = infos.get(0);
            
                String stage = wi.getProgress().getString("stage");
                int pct = wi.getProgress().getInt("pct", 0);
            
                if (wi.getState() == WorkInfo.State.ENQUEUED) {
                    outputView.setText("Offline: indicizzazione in coda...");
                } else if (wi.getState() == WorkInfo.State.RUNNING) {
                    outputView.setText("Offline: indicizzazione... " + pct + "%\n" + (stage != null ? stage : ""));
                } else if (wi.getState() == WorkInfo.State.FAILED) {
                    String err = wi.getOutputData().getString("err");
                    outputView.setText("Offline: indicizzazione FALLITA ❌\n" + (err != null ? err : "Errore sconosciuto"));
                } else if (wi.getState() == WorkInfo.State.SUCCEEDED) {
                    outputView.setText("Offline: pronto ✅");
                }
            });


        searchBtn.setOnClickListener(v -> {
        
            // 1) leggi input
            String libroInput = safeText(bookView);
            Integer cap = parseIntOrNull(chapterView);
            Integer vIn = parseIntOrNull(verseFromView);
            Integer vFin = parseIntOrNull(verseToView);
        
            // 2) valida PRIMA di qualsiasi correzione
            if (libroInput.isEmpty()) {
                toast("Inserisci il libro");
                bookView.requestFocus();
                return;
            }
            if (cap == null) {
                toast("Inserisci il capitolo");
                chapterView.requestFocus();
                return;
            }
            if (vIn == null) {
                toast("Inserisci almeno un versetto");
                verseFromView.requestFocus();
                return;
            }
        
            // 3) ora puoi correggere il titolo in sicurezza
            String libro;
            try {
                libro = ok.setTitoloCorrected(libroInput);
                if (libro == null || libro.trim().isEmpty()) {
                    toast("Libro non valido");
                    bookView.requestFocus();
                    return;
                }
            } catch (Exception e) {
                toast("Libro non valido");
                bookView.requestFocus();
                return;
            }
        
            int capitolo;
            try {
                capitolo = ok.setCapitoloCorrected(cap);
            } catch (IOException e) {
                toast("Capitolo non valido");
                chapterView.requestFocus();
                return;
            }
        
            int versettoIn = vIn;
            int versettoFin = (vFin == null) ? vIn : vFin;
            if (versettoFin < versettoIn) versettoFin = versettoIn;
        
            
            // Opzionale: auto suggerimenti capitoli (non blocca più la UI)
            try { new NumCapitoli().selectCapN(libro); } catch (IOException ignored) {}

            // UI state
            userSearchInProgress = true;
            searchBtn.setEnabled(false);
            outputView.setText("Ricerca in corso...");

            final String fLibro = libro;
            final int fCap = capitolo;
            final int fVIn = versettoIn;
            final int fVFin = versettoFin;

            executor.execute(() -> {
                String result;
                try {
                    result = searchDbThenWebAndCache(fLibro, fCap, fVIn, fVFin);
                } catch (Exception e) {
                    result = "Errore ricerca";
                }

                final String show = fLibro + " " + fCap + ": " + result;
                runOnUiThread(() -> {
                    outputView.setText(show);
                    searchBtn.setEnabled(true);
                    userSearchInProgress = false;
                });
            });
        });

        whatsappBtn.setOnClickListener(v -> {
            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setType("text/plain");
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, safeText(outputView));
            try {
                startActivity(whatsappIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp")));
            }
        });
    }

    /** DB-only */
    private String searchDbThenWebAndCache(String libro, int capitolo, int vIn, int vFin) {
        BibleDb db = BibleDb.get(getApplicationContext());

        List<String> keys = BookNameUtil.candidateKeys(libro);
        List<VerseRow> rows = db.verseDao().getRange(keys, capitolo, vIn, vFin);
        if (rows != null && !rows.isEmpty()) return join(rows);

        // se non c'è rete: fine
        if (!isConnected()) return "Non trovato nel DB (offline)";

        // fallback online + cache su DB
        try {
            List<VerseEntity> fetched = WolVerseFetcher.fetchRange(getApplicationContext(), libro, capitolo, vIn, vFin);
            if (fetched == null || fetched.isEmpty()) return "Non trovato (online)";

            db.verseDao().upsertAll(fetched);

            // rileggi dal DB (così sei coerente col join/format)
            rows = db.verseDao().getRange(keys, capitolo, vIn, vFin);
            if (rows != null && !rows.isEmpty()) return join(rows);

            return "Salvato, ma non rileggibile (chiavi?)";

        } catch (Exception e) {
            return "Errore online: " + e.getClass().getSimpleName();
        }
    }

    private static String join(List<VerseRow> rows) {
        StringBuilder sb = new StringBuilder();
        for (VerseRow r : rows) sb.append(r.verse).append(" ").append(r.text).append(" ");
        return sb.toString().trim();
    }

    private void ensureIndexWorkEnqueued() {
        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(
                "bible_index",
                ExistingWorkPolicy.KEEP,
                new OneTimeWorkRequest.Builder(BibleIndexWorker.class).build()
        );
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
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            try {
                startActivity(new android.content.Intent(this, SettingsActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Impostazioni non disponibili", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}

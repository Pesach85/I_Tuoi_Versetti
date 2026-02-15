package com.testing.ituoiversetti;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


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
                    WorkInfo wi = infos.get(0);

                    if (wi.getState() == WorkInfo.State.RUNNING) {
                        int pct = wi.getProgress().getInt("pct", 0);

                        // NON sovrascrivere mentre l’utente sta cercando o dopo un risultato finale
                        if (!userSearchInProgress) {
                            String current = safeText(outputView);
                            if (current.isEmpty() ||
                                current.startsWith("Offline: indicizzazione") ||
                                current.startsWith("Indicizzazione")) {
                                outputView.setText("Offline: indicizzazione in corso... " + pct + "%");
                            }
                        }
                    }

                    // Quando finisce, se la vista mostra ancora “indicizzazione”, aggiorna messaggio
                    if (wi.getState() == WorkInfo.State.SUCCEEDED && !userSearchInProgress) {
                        String current = safeText(outputView);
                        if (current.startsWith("Offline: indicizzazione") || current.startsWith("Indicizzazione")) {
                            outputView.setText("Offline: pronto ✅");
                        }
                    }
                });

        searchBtn.setOnClickListener(v -> {
            // Validazione input
            String libro = safeText(bookView);
            libro = ok.setTitoloCorrected(libro);

            Integer cap = parseIntOrNull(chapterView);
            Integer vIn = parseIntOrNull(verseFromView);
            Integer vFin = parseIntOrNull(verseToView);

            if (libro.isEmpty()) { toast("Inserisci il libro"); return; }
            if (cap == null)     { toast("Inserisci il capitolo"); return; }
            if (vIn == null)     { toast("Inserisci almeno un versetto"); return; }

            int capitolo;
            try {
                capitolo = ok.setCapitoloCorrected(cap);
            } catch (IOException e) {
                toast("Capitolo non valido");
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
                    result = searchDbOnly(fLibro, fCap, fVIn, fVFin);
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
    private String searchDbOnly(String libro, int capitolo, int vIn, int vFin) {
        BibleDb db = BibleDb.get(getApplicationContext());

        long count = db.verseDao().countAll();
        if (count == 0) {
            ensureIndexWorkEnqueued();
            return "Offline: indicizzazione in corso. Riprova tra poco.";
        }

        List<String> keys = BookNameUtil.candidateKeys(libro);
        List<VerseRow> rows = db.verseDao().getRange(keys, capitolo, vIn, vFin);

        if (rows == null || rows.isEmpty()) {
            return "Non trovato nel DB";
        }

        StringBuilder sb = new StringBuilder();
        for (VerseRow r : rows) {
            sb.append(r.verse).append(" ").append(r.text).append(" ");
        }
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
            Toast.makeText(this, "Impostazioni (TODO)", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

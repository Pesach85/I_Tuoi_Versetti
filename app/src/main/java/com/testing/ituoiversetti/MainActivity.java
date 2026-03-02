package com.testing.ituoiversetti;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    // -------------------------------------------------------------------------
    // Costanti
    // -------------------------------------------------------------------------
    private static final String PREFS_DB_FIX = "db_fixes";
    private static final String KEY_VERSE_PREFIX_FIX_DONE = "verse_prefix_fix_done";
    private static final String KEY_VERSE_ONE_CHAPTER_PREFIX_FIX_DONE = "verse_one_chapter_prefix_fix_done";
    private static final String PREFS_PDF = "pdf_update_meta";
    private static final String KEY_AUTO_PARSE_ON_STARTUP = "auto_parse_on_startup";
    private static final int SANITIZE_PAGE_SIZE = 500;
    private static final int TOPIC_RESULTS_LIMIT = 25;
    private static final int TOPIC_CANDIDATE_LIMIT = 220;
    private static final int TOPIC_MAX_SEMANTIC_TERMS = 10;
    private static final Pattern CHAPTER_PREFIX_FOR_VERSE_ONE = Pattern.compile("^\\s*\\d{1,3}\\s+");

    // -------------------------------------------------------------------------
    // UI
    // -------------------------------------------------------------------------
    private AutoCompleteTextView bookView;
    private AutoCompleteTextView topicView;
    private SwitchCompat semanticSwitch;
    private SwitchCompat onlineSwitch;
    private AutoCompleteTextView chapterView;
    private AutoCompleteTextView verseFromView;
    private AutoCompleteTextView verseToView;
    private TextInputLayout verseToLayout;
    private MultiAutoCompleteTextView outputView;
    private ArrayAdapter<String> arrayAdapter;
    private MaterialButtonToggleGroup verseModeToggle;
    private ChipGroup chipVerseStartGroup;
    private ChipGroup chipVerseRangeGroup;

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final InputCheck ok = new InputCheck();
    private volatile boolean userSearchInProgress = false;
    private PopupWindow activeSuggestionPopup;
    private List<String> allBooks = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Connettività
    // -------------------------------------------------------------------------
    private boolean isConnected() {
        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network n = cm.getActiveNetwork();
        if (n == null) return false;
        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(n);
        return caps != null &&
                caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private static boolean isConnectedStatic(Context ctx) {
        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network n = cm.getActiveNetwork();
        if (n == null) return false;
        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(n);
        return caps != null &&
                caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar tb = findViewById(R.id.toolbar);
        if (tb != null) setSupportActionBar(tb);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("I Tuoi Versetti");

        bookView       = findViewById(R.id.autoCompleteTextView);
        topicView      = findViewById(R.id.autoCompleteTextViewTopic);
        semanticSwitch = findViewById(R.id.switchSemanticSearch);
        onlineSwitch   = findViewById(R.id.switchOnlineMode);
        chapterView    = findViewById(R.id.autoCompleteTextView2);
        verseFromView  = findViewById(R.id.autoCompleteTextView3);
        verseToView    = findViewById(R.id.autoCompleteTextView4);
        verseToLayout  = findViewById(R.id.verseToLayout);
        verseModeToggle = findViewById(R.id.verseModeToggle);
        chipVerseStartGroup = findViewById(R.id.chipVerseStartGroup);
        chipVerseRangeGroup = findViewById(R.id.chipVerseRangeGroup);
        outputView     = findViewById(R.id.multiAutoCompleteTextView);

        allBooks = new Bibbia().composeBibbia();
        arrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
            allBooks);
        bookView.setAdapter(arrayAdapter);
        setupBookGridSuggestions();
        setupChapterGridSuggestions();
        setupVerseInputAssistant();

        Button searchBtn   = findViewById(R.id.button);

        executor.execute(() -> {
            sanitizeVersePrefixOnce();
            sanitizeVerseOneChapterPrefixOnce();
            long c = BibleDb.get(getApplicationContext()).verseDao().countAll();
            if (c == 0) {
                SharedPreferences spPdf = getSharedPreferences(PREFS_PDF, MODE_PRIVATE);
                boolean autoParseEnabled = spPdf.getBoolean(KEY_AUTO_PARSE_ON_STARTUP, true);
                if (autoParseEnabled) {
                    ensureIndexWorkEnqueued();
                    runOnUiThread(() -> outputView.setText("Offline: indicizzazione in corso..."));
                } else {
                    runOnUiThread(() -> outputView.setText("Offline: indicizzazione automatica disattivata"));
                }
            }
        });

        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData("bible_index")
                .observe(this, infos -> {
                    if (infos == null || infos.isEmpty()) return;
                    if (userSearchInProgress) return;
                    WorkInfo wi = infos.get(0);
                    String stage = wi.getProgress().getString("stage");
                    int pct = wi.getProgress().getInt("pct", 0);
                    if (wi.getState() == WorkInfo.State.ENQUEUED) {
                        outputView.setText("Offline: indicizzazione in coda...");
                    } else if (wi.getState() == WorkInfo.State.RUNNING) {
                        outputView.setText("Offline: indicizzazione... " + pct + "%\n"
                                + (stage != null ? stage : ""));
                    } else if (wi.getState() == WorkInfo.State.FAILED) {
                        String err = wi.getOutputData().getString("err");
                        outputView.setText("Offline: indicizzazione FALLITA ❌\n"
                                + (err != null ? err : "Errore sconosciuto"));
                    } else if (wi.getState() == WorkInfo.State.SUCCEEDED) {
                        outputView.setText("Offline: pronto ✅");
                    }
                });

        searchBtn.setOnClickListener(v -> {
            String topicInput = safeText(topicView);
            String libroInput = safeText(bookView);
            Integer cap  = parseIntOrNull(chapterView);
            Integer vIn  = parseIntOrNull(verseFromView);
            Integer vFin = parseIntOrNull(verseToView);
                boolean singleVerseMode = verseModeToggle != null
                    && verseModeToggle.getCheckedButtonId() == R.id.btnVerseModeSingle;

            boolean hasTopic    = !topicInput.isEmpty();
            boolean hasRefInput = !libroInput.isEmpty() || cap != null
                    || vIn != null || vFin != null;

            if (hasTopic && hasRefInput) {
                toast(getString(R.string.argomento_e_riferimento_conflitto));
                return;
            }

            if (hasTopic) {
                boolean semanticEnabled = semanticSwitch == null || semanticSwitch.isChecked();
                userSearchInProgress = true;
                searchBtn.setEnabled(false);
                outputView.setText(getString(R.string.ricerca_argomento_in_corso));

                executor.execute(() -> {
                    String result;
                    try {
                        result = searchTopicInDb(topicInput, semanticEnabled);
                    } catch (Exception e) {
                        result = "Errore ricerca";
                    }
                    final String modeName = semanticEnabled
                            ? getString(R.string.modalita_ai)
                            : getString(R.string.modalita_semplice);
                    final String show = "Argomento \"" + topicInput
                            + "\" (" + modeName + "):\n" + result;
                    runOnUiThread(() -> {
                        outputView.setText(show);
                        searchBtn.setEnabled(true);
                        userSearchInProgress = false;
                    });
                });
                return;
            }

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

            int versettoIn  = vIn;
            int versettoFin = singleVerseMode ? vIn : ((vFin == null) ? vIn : vFin);
            if (versettoFin < versettoIn) versettoFin = versettoIn;

            try { new NumCapitoli().selectCapN(libro); } catch (IOException ignored) {}

            userSearchInProgress = true;
            searchBtn.setEnabled(false);
            outputView.setText("Ricerca in corso...");

            final String fLibro   = libro;
            final int    fCap     = capitolo;
            final int    fVIn     = versettoIn;
            final int    fVFin    = versettoFin;
            final boolean allowOnlineFetch = onlineSwitch == null || onlineSwitch.isChecked();

            executor.execute(() -> {
                String result;
                try {
                    result = searchDbThenWebAndCache(fLibro, fCap, fVIn, fVFin, allowOnlineFetch);
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
        // Copia testo
        findViewById(R.id.btnCopy).setOnClickListener(v -> {
            String testo = safeText(outputView);
            if (testo.isEmpty()) return;
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText("versetto", testo));
            toast("Testo copiato");
        });
        
        // Condividi generico
        findViewById(R.id.btnShare).setOnClickListener(v -> {
            String testo = safeText(outputView);
            if (testo.isEmpty()) return;
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, testo);
            startActivity(Intent.createChooser(shareIntent, "Condividi versetto"));
        });
        
        // Genera immagine artistica
        findViewById(R.id.btnGenerateImage).setOnClickListener(v -> {
            String testo = safeText(outputView);
            if (testo.isEmpty()) { toast("Nessun versetto da illustrare"); return; }
            Intent intent = new Intent(this, VerseImageActivity.class);
            intent.putExtra("verse_text", testo);
            startActivity(intent);
        });
        
        // Salva preferiti
        findViewById(R.id.btnFavorite).setOnClickListener(v -> {
            String testo = safeText(outputView);
            if (testo.isEmpty()) return;
            android.content.SharedPreferences prefs =
                    getSharedPreferences("favorites", Context.MODE_PRIVATE);
            String key = "fav_" + System.currentTimeMillis();
            prefs.edit().putString(key, testo).apply();
            toast("Salvato nei preferiti ✓");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeSuggestionPopup != null) activeSuggestionPopup.dismiss();
        executor.shutdownNow();
    }

    // -------------------------------------------------------------------------
    // Ricerca versetti — logica con precedenza web + confronto testo
    // -------------------------------------------------------------------------

    /**
     * Istanza — usata da MainActivity.
     * Jsoup ha precedenza: fetch sempre se online, upsert solo se il testo è diverso o mancante.
     */
    private String searchDbThenWebAndCache(String libro, int capitolo, int vIn, int vFin,
                                           boolean allowOnlineFetch) {
        BibleDb db = BibleDb.get(getApplicationContext());
        List<String> keys = BookNameUtil.candidateKeys(libro);
        boolean connected = isConnected();
        boolean canUseOnline = allowOnlineFetch && connected;

        if (canUseOnline) {
            // Check preventivo: se tutti i versetti esistono nel DB, salta la rete
            List<VerseRow> existing = db.verseDao().getRange(keys, capitolo, vIn, vFin);
            int expectedCount = vFin - vIn + 1;

            if (existing == null || existing.size() < expectedCount) {
                // Versetti mancanti: fetch obbligatorio
                fetchAndUpdateIfDifferent(db, keys, libro, capitolo, vIn, vFin);
            } else {
                // Tutti presenti: fetch e confronta testo, upsert solo se diverso
                fetchAndUpdateIfDifferent(db, keys, libro, capitolo, vIn, vFin);
            }
        }

        List<VerseRow> rows = db.verseDao().getRange(keys, capitolo, vIn, vFin);
        if (rows != null && !rows.isEmpty()) return join(rows);
        if (!allowOnlineFetch) return "Non trovato nel DB (offline forzato)";
        return connected ? "Non trovato (online)" : "Non trovato nel DB (offline)";
    }

    /**
     * Statica — usata dal batch.
     */
    public static String searchDbThenWebAndCacheStatic(Context ctx,
            String libro, int capitolo, int vIn, int vFin) {
        BibleDb db = BibleDb.get(ctx);
        List<String> keys = BookNameUtil.candidateKeys(libro);
        boolean connected = isConnectedStatic(ctx);

        if (connected) {
            fetchAndUpdateIfDifferentStatic(ctx, db, keys, libro, capitolo, vIn, vFin);
        }

        List<VerseRow> rows = db.verseDao().getRange(keys, capitolo, vIn, vFin);
        if (rows != null && !rows.isEmpty()) return joinStatic(rows);
        return connected ? "Non trovato (online)" : "Non trovato nel DB (offline)";
    }

    /**
     * Fetch Jsoup + upsert solo se testo diverso o versetto mancante.
     * Versione istanza.
     */
    private void fetchAndUpdateIfDifferent(BibleDb db, List<String> keys,
            String libro, int capitolo, int vIn, int vFin) {
        try {
            List<VerseEntity> fetched = WolVerseFetcher.fetchRange(
                    getApplicationContext(), libro, capitolo, vIn, vFin);
            if (fetched == null || fetched.isEmpty()) return;

            List<VerseEntity> toUpsert = new ArrayList<>();
            for (VerseEntity incoming : fetched) {
                String incomingText = normalizeIncomingVerseText(incoming.text, incoming.verse);
                incoming.text = incomingText;

                List<VerseRow> existing = db.verseDao()
                        .getRange(keys, capitolo, incoming.verse, incoming.verse);

                boolean missing  = (existing == null || existing.isEmpty());
                boolean different = !missing &&
                        !incomingText.equals(
                                existing.get(0).text == null
                                        ? "" : existing.get(0).text.trim());

                if (missing || different) toUpsert.add(incoming);
            }

            if (!toUpsert.isEmpty()) db.verseDao().upsertAll(toUpsert);

        } catch (Exception ignored) {}
    }

    /**
     * Fetch Jsoup + upsert solo se testo diverso o versetto mancante.
     * Versione statica (batch).
     */
    private static void fetchAndUpdateIfDifferentStatic(Context ctx, BibleDb db,
            List<String> keys, String libro, int capitolo, int vIn, int vFin) {
        try {
            List<VerseEntity> fetched = WolVerseFetcher.fetchRange(ctx, libro, capitolo, vIn, vFin);
            if (fetched == null || fetched.isEmpty()) return;

            List<VerseEntity> toUpsert = new ArrayList<>();
            for (VerseEntity incoming : fetched) {
                String incomingText = normalizeIncomingVerseText(incoming.text, incoming.verse);
                incoming.text = incomingText;

                List<VerseRow> existing = db.verseDao()
                        .getRange(keys, capitolo, incoming.verse, incoming.verse);

                boolean missing   = (existing == null || existing.isEmpty());
                boolean different = !missing &&
                        !incomingText.equals(
                                existing.get(0).text == null
                                        ? "" : existing.get(0).text.trim());

                if (missing || different) toUpsert.add(incoming);
            }

            if (!toUpsert.isEmpty()) db.verseDao().upsertAll(toUpsert);

        } catch (Exception ignored) {}
    }

    // -------------------------------------------------------------------------
    // Join rows → stringa
    // -------------------------------------------------------------------------

    private static String join(List<VerseRow> rows) {
        StringBuilder sb = new StringBuilder();
        for (VerseRow r : rows) {
            String text = normalizeIncomingVerseText(r.text, r.verse);
            if (startsWithVersePrefix(text, r.verse)) sb.append(text).append(" ");
            else sb.append(r.verse).append(" ").append(text).append(" ");
        }
        return sb.toString().trim();
    }

    private static String joinStatic(List<VerseRow> rows) {
        StringBuilder sb = new StringBuilder();
        for (VerseRow r : rows) {
            String text = normalizeIncomingVerseText(r.text, r.verse);
            if (startsWithVersePrefix(text, r.verse)) sb.append(text).append(" ");
            else sb.append(r.verse).append(" ").append(text).append(" ");
        }
        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Work manager
    // -------------------------------------------------------------------------

    private void ensureIndexWorkEnqueued() {
        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(
                "bible_index",
                ExistingWorkPolicy.KEEP,
                new OneTimeWorkRequest.Builder(BibleIndexWorker.class).build());
    }

    // -------------------------------------------------------------------------
    // Ricerca per argomento
    // -------------------------------------------------------------------------

    private String searchTopicInDb(String topicInput, boolean semanticEnabled) {
        if (!semanticEnabled) return searchTopicInDbSimple(topicInput);

        List<String> coreTerms = parseTopicTerms(topicInput);
        if (coreTerms.isEmpty()) return getString(R.string.argomento_non_trovato);

        String normalizedTopic = normalizeSemantic(topicInput);
        List<String> semanticTerms = buildSemanticTerms(coreTerms);

        BibleDb db = BibleDb.get(getApplicationContext());
        List<TopicVerseRow> candidates = collectTopicCandidates(db, semanticTerms);
        if (candidates.isEmpty()) return getString(R.string.argomento_non_trovato);

        List<TopicHit> hits = new ArrayList<>();
        for (TopicVerseRow row : candidates) {
            if (row == null || row.text == null) continue;
            if (!containsAnySemanticTerm(row.text, semanticTerms)) continue;
            int score = scoreTopicMatch(row.text, coreTerms, semanticTerms, normalizedTopic);
            if (score <= 0) continue;
            hits.add(new TopicHit(row, score));
        }

        if (hits.isEmpty()) return getString(R.string.argomento_non_trovato);

        Collections.sort(hits, Comparator
                .comparingInt((TopicHit h) -> h.score).reversed()
                .thenComparing(h -> h.row.bookKey == null ? "" : h.row.bookKey)
                .thenComparingInt(h -> h.row.chapter)
                .thenComparingInt(h -> h.row.verse));

        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (TopicHit hit : hits) {
            TopicVerseRow row = hit.row;
            sb.append(prettyBookKey(row.bookKey))
                    .append(" ").append(row.chapter)
                    .append(":").append(row.verse)
                    .append(" ")
                    .append(highlightTerms(row.text.trim(), coreTerms))
                    .append("\n\n");
            shown++;
            if (shown >= TOPIC_RESULTS_LIMIT) break;
        }

        return shown == 0 ? getString(R.string.argomento_non_trovato) : sb.toString().trim();
    }

    private String searchTopicInDbSimple(String topicInput) {
        List<String> terms = parseTopicTerms(topicInput);
        if (terms.isEmpty()) return getString(R.string.argomento_non_trovato);

        BibleDb db = BibleDb.get(getApplicationContext());
        List<TopicVerseRow> candidates = db.verseDao().searchByTopicTerm(terms.get(0), 350);
        if (candidates == null || candidates.isEmpty())
            return getString(R.string.argomento_non_trovato);

        List<TopicHit> hits = new ArrayList<>();
        for (TopicVerseRow row : candidates) {
            if (row == null || row.text == null) continue;
            if (!containsAllTerms(row.text, terms)) continue;
            int score = 0;
            String textNorm = normalizeSemantic(row.text);
            for (String t : terms) score += countOccurrences(textNorm, t) * 20;
            if (score > 0) hits.add(new TopicHit(row, score));
        }

        if (hits.isEmpty()) return getString(R.string.argomento_non_trovato);

        Collections.sort(hits, Comparator
                .comparingInt((TopicHit h) -> h.score).reversed()
                .thenComparing(h -> h.row.bookKey == null ? "" : h.row.bookKey)
                .thenComparingInt(h -> h.row.chapter)
                .thenComparingInt(h -> h.row.verse));

        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (TopicHit hit : hits) {
            TopicVerseRow row = hit.row;
            sb.append(prettyBookKey(row.bookKey))
                    .append(" ").append(row.chapter)
                    .append(":").append(row.verse)
                    .append(" ")
                    .append(highlightTerms(row.text.trim(), terms))
                    .append("\n\n");
            shown++;
            if (shown >= TOPIC_RESULTS_LIMIT) break;
        }

        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Topic helpers
    // -------------------------------------------------------------------------

    private static List<String> parseTopicTerms(String topicInput) {
        String normalized = normalizeSemantic(topicInput);
        if (normalized.isEmpty()) return new ArrayList<>();
        String[] raw = normalized.split(" ");
        List<String> out = new ArrayList<>();
        for (String t : raw) if (t.length() >= 2) out.add(t);
        return out;
    }

    private static String normalizeSemantic(String topicInput) {
        String n = topicInput == null ? "" : topicInput.toLowerCase(Locale.ITALIAN).trim();
        n = Normalizer.normalize(n, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        n = n.replaceAll("[^a-z0-9\\s]", " ");
        return n.replaceAll("\\s+", " ").trim();
    }

    private static boolean containsAllTerms(String text, List<String> terms) {
        String lower = normalizeSemantic(text);
        for (String t : terms) if (!lower.contains(t)) return false;
        return true;
    }

    private static boolean containsAnySemanticTerm(String text, List<String> semanticTerms) {
        String normalized = normalizeSemantic(text);
        for (String t : semanticTerms) if (normalized.contains(t)) return true;
        return false;
    }

    private List<TopicVerseRow> collectTopicCandidates(BibleDb db, List<String> semanticTerms) {
        Map<String, TopicVerseRow> uniq = new LinkedHashMap<>();
        VerseDao dao = db.verseDao();
        for (String term : semanticTerms) {
            List<TopicVerseRow> rows = dao.searchByTopicTerm(term, TOPIC_CANDIDATE_LIMIT);
            if (rows == null || rows.isEmpty()) continue;
            for (TopicVerseRow r : rows) {
                if (r == null) continue;
                String key = (r.bookKey == null ? "" : r.bookKey)
                        + "|" + r.chapter + "|" + r.verse;
                if (!uniq.containsKey(key)) uniq.put(key, r);
            }
        }
        return new ArrayList<>(uniq.values());
    }

    private static List<String> buildSemanticTerms(List<String> coreTerms) {
        LinkedHashSet<String> terms = new LinkedHashSet<>(coreTerms);
        for (String core : coreTerms) {
            for (String syn : semanticSynonyms(core))
                if (syn != null && syn.length() >= 2) terms.add(syn);
            String stem = stemItalian(core);
            if (stem.length() >= 3) terms.add(stem);
        }
        List<String> out = new ArrayList<>(terms);
        if (out.size() > TOPIC_MAX_SEMANTIC_TERMS)
            return new ArrayList<>(out.subList(0, TOPIC_MAX_SEMANTIC_TERMS));
        return out;
    }

    private static List<String> semanticSynonyms(String term) {
        switch (term) {
            case "fede":       return Arrays.asList("fiducia","credere","creduto","credenti","fermezza");
            case "speranza":   return Arrays.asList("attesa","futuro","consolazione","promessa");
            case "amore":      return Arrays.asList("carita","benevolenza","affetto","misericordia");
            case "pace":       return Arrays.asList("serenita","tranquillita","calma","concordia");
            case "gioia":      return Arrays.asList("rallegrarsi","felicita","esultare","letizia");
            case "preghiera":  return Arrays.asList("pregare","invocare","supplica","orazione");
            case "perdono":    return Arrays.asList("perdonare","misericordia","colpa","colpe");
            case "timore":     return Arrays.asList("paura","riverenza","rispetto","timore di dio");
            case "ubbidienza": return Arrays.asList("obbedienza","ubbidire","osservare","comandamenti");
            case "salvezza":   return Arrays.asList("salvare","riscatto","redenzione","liberazione");
            case "grazia":     return Arrays.asList("favore","benevolenza","dono","immeritata");
            case "regno":      return Arrays.asList("regnare","governo","messianico","re");
            case "spirito":    return Arrays.asList("spirito santo","forza","potenza","soffio");
            case "sapienza":   return Arrays.asList("saggezza","discernimento","intelligenza","conoscenza");
            default:           return Collections.emptyList();
        }
    }

    private static String stemItalian(String term) {
        if (term == null) return "";
        String t = term.trim();
        if (t.length() <= 4) return t;
        String[] endings = {"mente","zioni","zione","amenti","amento","atori","atore",
                "anti","ante","are","ere","ire","ita","ivo","iva","osi","oso","osa","i","e","a","o"};
        for (String end : endings) {
            if (t.endsWith(end) && t.length() - end.length() >= 3)
                return t.substring(0, t.length() - end.length());
        }
        return t;
    }

    private static int scoreTopicMatch(String text, List<String> coreTerms,
            List<String> semanticTerms, String normalizedTopic) {
        String lower = normalizeSemantic(text);
        int score = 0;
        if (!normalizedTopic.isEmpty() && lower.contains(normalizedTopic)) score += 160;

        int coreHits = 0;
        for (String term : coreTerms) {
            int count = countOccurrences(lower, term);
            if (count > 0) coreHits++;
            score += count * 30;
            int firstIdx = lower.indexOf(term);
            if (firstIdx >= 0) score += Math.max(0, 30 - Math.min(30, firstIdx / 4));
        }

        for (String term : semanticTerms) {
            if (coreTerms.contains(term)) continue;
            score += countOccurrences(lower, term) * 10;
        }

        if (coreHits == coreTerms.size()) score += 80;
        score += proximityBonus(lower, coreTerms);
        if (text != null && text.length() <= 160) score += 5;
        return score;
    }

    private static int proximityBonus(String normalizedText, List<String> coreTerms) {
        if (coreTerms == null || coreTerms.size() < 2) return 0;
        int minPos = Integer.MAX_VALUE, maxPos = -1;
        for (String t : coreTerms) {
            int p = normalizedText.indexOf(t);
            if (p < 0) return 0;
            if (p < minPos) minPos = p;
            if (p > maxPos) maxPos = p;
        }
        int span = maxPos - minPos;
        if (span <= 25) return 45;
        if (span <= 60) return 25;
        if (span <= 100) return 10;
        return 0;
    }

    private static int countOccurrences(String text, String term) {
        if (text == null || term == null || term.isEmpty()) return 0;
        int count = 0, from = 0;
        while (true) {
            int idx = text.indexOf(term, from);
            if (idx < 0) break;
            count++;
            from = idx + term.length();
        }
        return count;
    }

    private static String highlightTerms(String text, List<String> terms) {
        if (text == null || text.isEmpty() || terms == null || terms.isEmpty()) return text;
        List<String> sorted = new ArrayList<>(terms);
        Collections.sort(sorted, (a, b) -> Integer.compare(b.length(), a.length()));
        String out = text;
        for (String term : sorted) {
            if (term == null || term.trim().isEmpty()) continue;
            Pattern p = Pattern.compile("(?i)" + Pattern.quote(term));
            Matcher m = p.matcher(out);
            StringBuffer sb = new StringBuffer();
            while (m.find())
                m.appendReplacement(sb, Matcher.quoteReplacement("«" + m.group() + "»"));
            m.appendTail(sb);
            out = sb.toString();
        }
        return out;
    }

    private static String prettyBookKey(String key) {
        if (key == null || key.trim().isEmpty()) return "Libro";
        String[] parts = key.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            if (i > 0) out.append(' ');
            if (p.length() == 1) out.append(p.toUpperCase(Locale.ITALIAN));
            else out.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return out.toString();
    }

    // -------------------------------------------------------------------------
    // Sanitize DB
    // -------------------------------------------------------------------------

    private void sanitizeVersePrefixOnce() {
        android.content.SharedPreferences sp =
                getSharedPreferences(PREFS_DB_FIX, Context.MODE_PRIVATE);
        if (sp.getBoolean(KEY_VERSE_PREFIX_FIX_DONE, false)) return;
        try {
            BibleDb db = BibleDb.get(getApplicationContext());
            VerseDao dao = db.verseDao();
            int offset = 0;
            while (true) {
                List<VerseEntity> page = dao.pageAll(SANITIZE_PAGE_SIZE, offset);
                if (page == null || page.isEmpty()) break;
                boolean changed = false;
                for (VerseEntity row : page) {
                    if (row == null || row.text == null) continue;
                    String cleaned = normalizeIncomingVerseText(row.text, row.verse);
                    if (!cleaned.equals(row.text)) { row.text = cleaned; changed = true; }
                }
                if (changed) dao.upsertAll(page);
                if (page.size() < SANITIZE_PAGE_SIZE) break;
                offset += page.size();
            }
            sp.edit().putBoolean(KEY_VERSE_PREFIX_FIX_DONE, true).apply();
        } catch (Exception ignored) {}
    }

    private void sanitizeVerseOneChapterPrefixOnce() {
        android.content.SharedPreferences sp =
                getSharedPreferences(PREFS_DB_FIX, Context.MODE_PRIVATE);
        if (sp.getBoolean(KEY_VERSE_ONE_CHAPTER_PREFIX_FIX_DONE, false)) return;
        try {
            BibleDb db = BibleDb.get(getApplicationContext());
            VerseDao dao = db.verseDao();
            int offset = 0;
            while (true) {
                List<VerseEntity> page = dao.pageAll(SANITIZE_PAGE_SIZE, offset);
                if (page == null || page.isEmpty()) break;
                boolean changed = false;
                for (VerseEntity row : page) {
                    if (row == null || row.text == null || row.verse != 1) continue;
                    String cleaned = stripChapterNumberPrefixForVerseOne(row.text, row.verse);
                    if (!cleaned.equals(row.text)) { row.text = cleaned; changed = true; }
                }
                if (changed) dao.upsertAll(page);
                if (page.size() < SANITIZE_PAGE_SIZE) break;
                offset += page.size();
            }
            sp.edit().putBoolean(KEY_VERSE_ONE_CHAPTER_PREFIX_FIX_DONE, true).apply();
        } catch (Exception ignored) {}
    }

    private static String normalizeIncomingVerseText(String text, int verse) {
        String cleaned = stripLeadingVerseNumber(text, verse);
        return stripChapterNumberPrefixForVerseOne(cleaned, verse);
    }

    private static String stripChapterNumberPrefixForVerseOne(String text, int verse) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (verse != 1 || trimmed.isEmpty()) return trimmed;
        return CHAPTER_PREFIX_FOR_VERSE_ONE.matcher(trimmed).replaceFirst("").trim();
    }

    private static String stripLeadingVerseNumber(String text, int verse) {
        if (text == null) return "";
        int idx = 0, n = text.length();
        while (idx < n && Character.isWhitespace(text.charAt(idx))) idx++;
        String verseStr = String.valueOf(verse);
        int end = idx + verseStr.length();
        if (end > n || !text.regionMatches(idx, verseStr, 0, verseStr.length()))
            return text.trim();
        if (end < n && !Character.isWhitespace(text.charAt(end))) return text.trim();
        while (end < n && Character.isWhitespace(text.charAt(end))) end++;
        return text.substring(end).trim();
    }

    private static boolean startsWithVersePrefix(String text, int verse) {
        if (text == null || text.isEmpty()) return false;
        int idx = 0, n = text.length();
        while (idx < n && Character.isWhitespace(text.charAt(idx))) idx++;
        String verseStr = String.valueOf(verse);
        int end = idx + verseStr.length();
        if (end > n || !text.regionMatches(idx, verseStr, 0, verseStr.length())) return false;
        if (end == n) return true;
        return Character.isWhitespace(text.charAt(end));
    }

    // -------------------------------------------------------------------------
    // Grid suggestions
    // -------------------------------------------------------------------------

    private void setupBookGridSuggestions() {
        View.OnClickListener showBooks = v -> showGridPopup(
                bookView,
                allBooks,
                4,
                picked -> {
                    bookView.setText(picked, false);
                    chapterView.setText("");
                });

        bookView.setOnClickListener(showBooks);
        bookView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && safeText(bookView).isEmpty()) showBooks.onClick(bookView);
        });
        bookView.setOnItemClickListener((parent, view, position, id) -> chapterView.setText(""));
    }

    private void setupChapterGridSuggestions() {
        View.OnClickListener showChapters = v -> {
            String bookInput = safeText(bookView);
            if (bookInput.isEmpty()) {
                toast("Seleziona prima un libro");
                bookView.requestFocus();
                return;
            }
            List<String> chapters = buildChaptersForBook(bookInput);
            if (chapters.isEmpty()) {
                toast("Capitoli non disponibili per il libro selezionato");
                return;
            }
            showGridPopup(chapterView, chapters, 8, picked -> chapterView.setText(picked, false));
        };

        chapterView.setOnClickListener(showChapters);
        chapterView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && safeText(chapterView).isEmpty()) showChapters.onClick(chapterView);
        });
    }

    private void setupVerseInputAssistant() {
        if (verseModeToggle != null) {
            verseModeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                boolean single = checkedId == R.id.btnVerseModeSingle;
                if (verseToLayout != null) verseToLayout.setVisibility(single ? View.GONE : View.VISIBLE);
                if (single) verseToView.setText("");
            });
            boolean single = verseModeToggle.getCheckedButtonId() == R.id.btnVerseModeSingle;
            if (verseToLayout != null) verseToLayout.setVisibility(single ? View.GONE : View.VISIBLE);
        }

        if (chipVerseStartGroup != null) {
            wireVerseStartChip(R.id.chipStart1, 1);
            wireVerseStartChip(R.id.chipStart5, 5);
            wireVerseStartChip(R.id.chipStart10, 10);
            wireVerseStartChip(R.id.chipStart20, 20);
            wireVerseStartChip(R.id.chipStart30, 30);
        }

        if (chipVerseRangeGroup != null) {
            wireVerseRangeChip(R.id.chipRange0, 0);
            wireVerseRangeChip(R.id.chipRange4, 4);
            wireVerseRangeChip(R.id.chipRange9, 9);
            wireVerseRangeChip(R.id.chipRange19, 19);
        }
    }

    private void wireVerseStartChip(int chipId, int value) {
        Chip chip = findViewById(chipId);
        if (chip == null) return;
        chip.setOnClickListener(v -> {
            verseFromView.setText(String.valueOf(value), false);
            if (verseModeToggle != null
                    && verseModeToggle.getCheckedButtonId() == R.id.btnVerseModeSingle) {
                verseToView.setText("");
            }
        });
    }

    private void wireVerseRangeChip(int chipId, int delta) {
        Chip chip = findViewById(chipId);
        if (chip == null) return;
        chip.setOnClickListener(v -> {
            Integer start = parseIntOrNull(verseFromView);
            if (start == null) {
                toast("Inserisci prima il versetto iniziale");
                verseFromView.requestFocus();
                return;
            }
            if (delta > 0 && verseModeToggle != null) {
                verseModeToggle.check(R.id.btnVerseModeRange);
            }
            int end = Math.max(start, start + delta);
            if (delta == 0 && verseModeToggle != null
                    && verseModeToggle.getCheckedButtonId() == R.id.btnVerseModeSingle) {
                verseToView.setText("");
            } else {
                verseToView.setText(String.valueOf(end), false);
            }
        });
    }

    private List<String> buildChaptersForBook(String bookInput) {
        try {
            String normalizedBook = ok.setTitoloCorrected(bookInput);
            if (normalizedBook == null || normalizedBook.trim().isEmpty()) return Collections.emptyList();
            NumCapitoli cap = new NumCapitoli();
            cap.selectCapN(normalizedBook);
            return new ArrayList<>(cap.caps);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

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
            super(MainActivity.this, 0, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv;
            if (convertView instanceof TextView) {
                tv = (TextView) convertView;
            } else {
                tv = new TextView(MainActivity.this);
                int pH = dp(12);
                int pV = dp(10);
                tv.setPadding(pH, pV, pH, pV);
                tv.setBackgroundResource(R.drawable.bg_suggestion_tile);
                tv.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.on_surface));
                tv.setTextSize(16f);
                tv.setGravity(android.view.Gravity.CENTER);
            }
            String value = getItem(position);
            tv.setText(value == null ? "" : value);
            return tv;
        }
    }

    // -------------------------------------------------------------------------
    // Utility UI
    // -------------------------------------------------------------------------

    private static String safeText(AutoCompleteTextView v) {
        if (v == null || v.getText() == null) return "";
        return v.getText().toString().trim();
    }

    private static String safeText(MultiAutoCompleteTextView v) {
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

    // -------------------------------------------------------------------------
    // Menu
    // -------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            try { startActivity(new Intent(this, SettingsActivity.class)); }
            catch (Exception e) { toast("Impostazioni non disponibili"); }
            return true;
        } else if (id == R.id.action_db_manager) {
            try { startActivity(new Intent(this, DbManagerActivity.class)); }
            catch (Exception e) { toast("Gestione DB non disponibile"); }
            return true;
        } else if (id == R.id.action_study_assistant) {
            try { startActivity(new Intent(this, StudyAssistantActivity.class)); }
            catch (Exception e) { toast("Assistente studio non disponibile"); }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // -------------------------------------------------------------------------
    // Inner class
    // -------------------------------------------------------------------------

    private static final class TopicHit {
        final TopicVerseRow row;
        final int score;
        TopicHit(TopicVerseRow row, int score) { this.row = row; this.score = score; }
    }
}
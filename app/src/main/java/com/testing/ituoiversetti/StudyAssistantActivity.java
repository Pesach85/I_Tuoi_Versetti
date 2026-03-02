package com.testing.ituoiversetti;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.URLEncoder;

public class StudyAssistantActivity extends AppCompatActivity {

    private static final int STUDY_RESULTS_LIMIT = 10;
    private static final int STUDY_CANDIDATE_LIMIT = 200;
    private static final String JW_GLOSSARY_BASE = "https://www.jw.org/it/biblioteca-digitale/libri/dizionario-biblico-glossario/";
    private static final String PREFS_GLOSSARY_CACHE = "jw_glossary_cache";
    private static final String KEY_INDEX_JSON = "index_json";
    private static final String PREFS_STUDY_SETTINGS = "study_settings";
    private static final String KEY_ALLOW_NETWORK_SOURCES = "allow_network_sources";
    private static final String WOL_RESEARCH_GUIDE_URL = "https://wol.jw.org/it/wol/publication/r6/lp-i/rsg19/3";
    private static final String RESEARCH_GUIDE_CACHE_FILE = "research_guide_cache.txt";

    private EditText questionInput;
    private TextView answerView;
    private EditText glossaryInput;
    private TextView glossaryView;
    private TextView researchGuideView;
    private TextView studyPathView;
    private EditText noteTitleInput;
    private EditText noteBodyInput;
    private MaterialButtonToggleGroup durationToggle;
    private SwitchMaterial networkSourcesSwitch;
    private boolean allowNetworkSources = true;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_assistant);

        MaterialToolbar toolbar = findViewById(R.id.studyToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        questionInput = findViewById(R.id.studyQuestionInput);
        answerView = findViewById(R.id.studyAnswerView);
        glossaryInput = findViewById(R.id.studyGlossaryInput);
        glossaryView = findViewById(R.id.studyGlossaryView);
        researchGuideView = findViewById(R.id.studyResearchGuideView);
        studyPathView = findViewById(R.id.studyPathView);
        noteTitleInput = findViewById(R.id.studyNoteTitleInput);
        noteBodyInput = findViewById(R.id.studyNoteBodyInput);
        durationToggle = findViewById(R.id.studyDurationToggle);
        networkSourcesSwitch = findViewById(R.id.switchStudyNetworkSources);

        allowNetworkSources = getSharedPreferences(PREFS_STUDY_SETTINGS, MODE_PRIVATE)
            .getBoolean(KEY_ALLOW_NETWORK_SOURCES, true);
        if (networkSourcesSwitch != null) {
            networkSourcesSwitch.setChecked(allowNetworkSources);
            networkSourcesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                allowNetworkSources = isChecked;
                getSharedPreferences(PREFS_STUDY_SETTINGS, MODE_PRIVATE)
                    .edit().putBoolean(KEY_ALLOW_NETWORK_SOURCES, isChecked).apply();
            });
        }

        MaterialButton btnAnalyze = findViewById(R.id.btnStudyAnalyze);
        MaterialButton btnLookupGlossary = findViewById(R.id.btnLookupGlossary);
        MaterialButton btnInsertGlossary = findViewById(R.id.btnInsertGlossary);
        MaterialButton btnWarmGlossaryCache = findViewById(R.id.btnWarmGlossaryCache);
        MaterialButton btnLookupResearchGuide = findViewById(R.id.btnLookupResearchGuide);
        MaterialButton btnOpenResearchGuide = findViewById(R.id.btnOpenResearchGuide);
        MaterialButton btnInsertResearchGuide = findViewById(R.id.btnInsertResearchGuide);
        MaterialButton btnGeneratePath = findViewById(R.id.btnGeneratePath);
        MaterialButton btnInsertAnswer = findViewById(R.id.btnInsertAnswer);
        MaterialButton btnInsertPath = findViewById(R.id.btnInsertPath);
        MaterialButton btnSaveLocal = findViewById(R.id.btnSaveNoteLocal);
        MaterialButton btnExportTxt = findViewById(R.id.btnExportTxt);
        MaterialButton btnExportMd = findViewById(R.id.btnExportMd);
        MaterialButton btnShare = findViewById(R.id.btnShareNote);

        wirePromptChip(R.id.chipPromptFaith, "Come posso rafforzare la fede nelle prove?");
        wirePromptChip(R.id.chipPromptHope, "Quali versetti alimentano la speranza cristiana?");
        wirePromptChip(R.id.chipPromptPrayer, "Come rendere più profonda la preghiera?");
        wirePromptChip(R.id.chipPromptFamily, "Quali principi biblici aiutano la famiglia?");
        wirePromptChip(R.id.chipPromptPeace, "Come trovare pace e calma interiore?");

        btnAnalyze.setOnClickListener(v -> analyzeQuestion());
        btnLookupGlossary.setOnClickListener(v -> lookupGlossary());
        btnInsertGlossary.setOnClickListener(v -> insertGlossaryIntoNote());
        btnWarmGlossaryCache.setOnClickListener(v -> warmGlossaryCacheAZ());
        btnLookupResearchGuide.setOnClickListener(v -> lookupResearchGuide());
        btnOpenResearchGuide.setOnClickListener(v -> openResearchGuideSource());
        btnInsertResearchGuide.setOnClickListener(v -> insertResearchGuideIntoNote());
        btnGeneratePath.setOnClickListener(v -> generateStudyPath());
        btnInsertAnswer.setOnClickListener(v -> insertAnswerIntoNote());
        btnInsertPath.setOnClickListener(v -> insertPathIntoNote());
        btnSaveLocal.setOnClickListener(v -> saveLocalNote());
        btnExportTxt.setOnClickListener(v -> exportNote(false));
        btnExportMd.setOnClickListener(v -> exportNote(true));
        btnShare.setOnClickListener(v -> shareNote());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void wirePromptChip(int chipId, String prompt) {
        Chip chip = findViewById(chipId);
        if (chip == null) return;
        chip.setOnClickListener(v -> {
            questionInput.setText(prompt);
            analyzeQuestion();
        });
    }

    private void analyzeQuestion() {
        String question = safeText(questionInput);
        if (question.isEmpty()) {
            toast(getString(R.string.study_question_required));
            return;
        }

        answerView.setText(getString(R.string.study_generating));
        executor.execute(() -> {
            String answer;
            try {
                answer = buildLocalStudyAnswer(question);
            } catch (Exception e) {
                answer = getString(R.string.study_error) + ": " + e.getMessage();
            }
            final String finalAnswer = answer;
            runOnUiThread(() -> answerView.setText(finalAnswer));
        });
    }

    private void lookupGlossary() {
        String term = safeText(glossaryInput);
        if (term.isEmpty()) {
            String question = safeText(questionInput);
            List<String> terms = parseTerms(question);
            if (!terms.isEmpty()) {
                term = terms.get(0);
                glossaryInput.setText(term);
            }
        }
        if (term.isEmpty()) {
            toast(getString(R.string.study_glossary_term_required));
            return;
        }

        final String finalTerm = term;
        glossaryView.setText(getString(R.string.study_glossary_loading));
        executor.execute(() -> {
            String result;
            try {
                result = getGlossaryDefinition(finalTerm);
            } catch (Exception e) {
                result = getString(R.string.study_error) + ": " + e.getMessage();
            }
            String finalResult = result;
            runOnUiThread(() -> glossaryView.setText(finalResult));
        });
    }

    private void warmGlossaryCacheAZ() {
        if (!canUseNetworkSources()) {
            toast(getString(R.string.study_cache_only_mode));
            return;
        }
        glossaryView.setText(getString(R.string.study_glossary_warmup_running));
        executor.execute(() -> {
            try {
                Map<String, String> index = loadGlossaryIndex();
                if (index.isEmpty()) {
                    runOnUiThread(() -> glossaryView.setText(getString(R.string.study_glossary_not_found)));
                    return;
                }

                int total = index.size();
                int alreadyCached = 0;
                int fetched = 0;
                int failed = 0;
                int current = 0;

                for (Map.Entry<String, String> e : index.entrySet()) {
                    current++;
                    String key = e.getKey();
                    String url = e.getValue();

                    String cached = getGlossaryDefinitionCachedOnly(key);
                    if (!cached.isEmpty()) {
                        alreadyCached++;
                    } else {
                        GlossaryEntry entry = fetchGlossaryEntry(url);
                        if (entry != null && !entry.definition.isEmpty()) {
                            String formatted = entry.title + "\n\n" + entry.definition + "\n\nFonte: " + entry.url;
                            saveGlossaryDefinition(normalize(key), formatted);
                            if (!entry.title.isEmpty()) saveGlossaryDefinition(normalize(entry.title), formatted);
                            fetched++;
                        } else {
                            failed++;
                        }
                    }

                    if (current % 20 == 0 || current == total) {
                        final String progress = getString(R.string.study_glossary_warmup_running)
                                + "\n" + current + "/" + total
                                + " | cache=" + alreadyCached
                                + " | nuovi=" + fetched
                                + " | fail=" + failed;
                        runOnUiThread(() -> glossaryView.setText(progress));
                    }
                }

                final String done = getString(R.string.study_glossary_warmup_done)
                        + "\nTotale voci: " + total
                        + "\nGià in cache: " + alreadyCached
                        + "\nNuove scaricate: " + fetched
                        + "\nFallite: " + failed;
                runOnUiThread(() -> glossaryView.setText(done));
            } catch (Exception e) {
                runOnUiThread(() -> glossaryView.setText(getString(R.string.study_error) + ": " + e.getMessage()));
            }
        });
    }

    private void lookupResearchGuide() {
        String term = safeText(glossaryInput);
        if (term.isEmpty()) {
            String question = safeText(questionInput);
            List<String> terms = parseTerms(question);
            if (!terms.isEmpty()) {
                term = terms.get(0);
                glossaryInput.setText(term);
            }
        }
        if (term.isEmpty()) {
            toast(getString(R.string.study_glossary_term_required));
            return;
        }

        final String finalTerm = term;
        researchGuideView.setText(getString(R.string.study_research_loading));
        executor.execute(() -> {
            String result;
            try {
                result = searchResearchGuide(finalTerm);
            } catch (Exception e) {
                result = getString(R.string.study_error) + ": " + e.getMessage();
            }
            String finalResult = result;
            runOnUiThread(() -> researchGuideView.setText(finalResult));
        });
    }

    private void openResearchGuideSource() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(WOL_RESEARCH_GUIDE_URL));
            startActivity(i);
        } catch (Exception e) {
            toast(getString(R.string.study_research_open_error));
        }
    }

    private void generateStudyPath() {
        String question = safeText(questionInput);
        if (question.isEmpty()) {
            toast(getString(R.string.study_question_required));
            return;
        }

        final int days = selectedDurationDays();
        studyPathView.setText(getString(R.string.study_path_generating));
        executor.execute(() -> {
            String plan;
            try {
                plan = buildStudyPath(question, days);
            } catch (Exception e) {
                plan = getString(R.string.study_error) + ": " + e.getMessage();
            }
            String finalPlan = plan;
            runOnUiThread(() -> studyPathView.setText(finalPlan));
        });
    }

    private int selectedDurationDays() {
        if (durationToggle == null) return 7;
        int checked = durationToggle.getCheckedButtonId();
        if (checked == R.id.btnDuration3) return 3;
        if (checked == R.id.btnDuration14) return 14;
        if (checked == R.id.btnDuration30) return 30;
        return 7;
    }

    private String buildLocalStudyAnswer(String question) {
        List<String> coreTerms = parseTerms(question);
        if (coreTerms.isEmpty()) return getString(R.string.study_no_terms);

        List<StudyHit> hits = findStudyHits(question, coreTerms);

        if (hits.isEmpty()) return getString(R.string.study_no_results);

        Collections.sort(hits, Comparator
                .comparingInt((StudyHit h) -> h.score).reversed()
                .thenComparing(h -> h.row.bookKey == null ? "" : h.row.bookKey)
                .thenComparingInt(h -> h.row.chapter)
                .thenComparingInt(h -> h.row.verse));

        String principle = inferPrinciple(coreTerms);
        StringBuilder sb = new StringBuilder();
        sb.append("Domanda di studio:\n")
                .append(question.trim())
                .append("\n\n");

        sb.append("Sintesi locale (on-device):\n")
                .append(principle)
                .append("\n\n");

        sb.append("Versetti guida:\n");
        int shown = 0;
        for (StudyHit hit : hits) {
            if (shown >= STUDY_RESULTS_LIMIT) break;
            TopicVerseRow row = hit.row;
            sb.append("- ")
                    .append(prettyBookKey(row.bookKey))
                    .append(" ")
                    .append(row.chapter)
                    .append(":")
                    .append(row.verse)
                    .append(" — ")
                    .append(row.text == null ? "" : row.text.trim())
                    .append("\n");
            shown++;
        }

        sb.append("\nDomande per meditazione personale:\n")
                .append("1) Cosa rivela questo tema sul modo di pensare di Geova?\n")
                .append("2) Quale passo pratico posso applicare oggi?\n")
                .append("3) Con chi potrei condividere questo incoraggiamento?\n")
                .append("\nNota: elaborazione 100% locale su DB biblico e regole semantiche interne.");

        String glossary = glossaryForCoreTerms(coreTerms);
        if (!glossary.isEmpty()) {
            sb.append("\n\nApprofondimento glossario JW.org:\n").append(glossary);
        }

        String researchGuide = researchGuideForCoreTerms(coreTerms);
        if (!researchGuide.isEmpty()) {
            sb.append("\n\nApprofondimento Guida alle ricerche (WOL):\n").append(researchGuide);
        }

        return sb.toString();
    }

    private String glossaryForCoreTerms(List<String> coreTerms) {
        if (coreTerms == null || coreTerms.isEmpty()) return "";
        for (String term : coreTerms) {
            if (term == null || term.trim().isEmpty()) continue;
            String fromCache = getGlossaryDefinitionCachedOnly(term);
            if (!fromCache.isEmpty()) return fromCache;
        }
        if (!canUseNetworkSources()) return "";
        for (String term : coreTerms) {
            if (term == null || term.trim().isEmpty()) continue;
            String fetched = getGlossaryDefinition(term);
            if (!fetched.isEmpty() && !fetched.equals(getString(R.string.study_glossary_not_found))) {
                return fetched;
            }
        }
        return "";
    }

    private String researchGuideForCoreTerms(List<String> coreTerms) {
        if (coreTerms == null || coreTerms.isEmpty()) return "";
        String cache = loadResearchGuideCacheText();
        if (!cache.isEmpty()) {
            for (String term : coreTerms) {
                String result = searchResearchGuideInText(cache, term);
                if (!result.isEmpty()) return result;
            }
        }
        if (!canUseNetworkSources()) return "";
        String refreshed = refreshResearchGuideCache();
        if (refreshed.isEmpty()) return "";
        for (String term : coreTerms) {
            String result = searchResearchGuideInText(refreshed, term);
            if (!result.isEmpty()) return result;
        }
        return "";
    }

    private String buildStudyPath(String question, int days) {
        List<String> coreTerms = parseTerms(question);
        if (coreTerms.isEmpty()) return getString(R.string.study_no_terms);

        List<StudyHit> hits = findStudyHits(question, coreTerms);
        if (hits.isEmpty()) return getString(R.string.study_no_results);

        int limit = Math.min(STUDY_RESULTS_LIMIT, hits.size());
        List<StudyHit> top = new ArrayList<>(hits.subList(0, limit));

        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.study_path_title_fallback))
                .append(" (").append(days).append(" giorni)")
                .append("\n\n");

        sb.append("Tema: ").append(question.trim()).append("\n");
        sb.append("Metodo didattico: ispirato a pratiche di studio personale presenti su JW.org")
                .append("\n\n")
                .append("Schema giornaliero:\n")
                .append("1) Preghiera iniziale\n")
                .append("2) Lettura del versetto nel suo contesto\n")
                .append("3) Meditazione: cosa rivela su Geova\n")
                .append("4) Applicazione pratica del giorno\n")
                .append("5) Condivisione dell’incoraggiamento\n\n");

        for (int day = 1; day <= days; day++) {
            StudyHit hit = top.get((day - 1) % top.size());
            TopicVerseRow row = hit.row;
            String ref = prettyBookKey(row.bookKey) + " " + row.chapter + ":" + row.verse;
            String focus = pickDailyFocus(day);
            String text = row.text == null ? "" : row.text.trim();

            sb.append("Giorno ").append(day).append(" — ").append(ref).append("\n");
            sb.append("Focus: ").append(focus).append("\n");
            sb.append("Testo: ").append(text).append("\n");
            sb.append("Azione: Scrivi 2 righe su come applicarlo oggi.\n\n");
        }

        sb.append("Revisione finale:\n")
                .append("- Rileggi gli appunti\n")
                .append("- Evidenzia i progressi spirituali\n")
                .append("- Definisci il prossimo tema di studio\n");

        return sb.toString().trim();
    }

    private List<StudyHit> findStudyHits(String question, List<String> coreTerms) {
        List<String> semanticTerms = buildSemanticTerms(coreTerms);
        BibleDb db = BibleDb.get(getApplicationContext());
        VerseDao dao = db.verseDao();

        Map<String, TopicVerseRow> uniq = new LinkedHashMap<>();
        for (String t : semanticTerms) {
            List<TopicVerseRow> rows = dao.searchByTopicTerm(t, STUDY_CANDIDATE_LIMIT);
            if (rows == null || rows.isEmpty()) continue;
            for (TopicVerseRow r : rows) {
                if (r == null || r.text == null) continue;
                String key = (r.bookKey == null ? "" : r.bookKey)
                        + "|" + r.chapter + "|" + r.verse;
                if (!uniq.containsKey(key)) uniq.put(key, r);
            }
        }

        if (uniq.isEmpty()) return Collections.emptyList();

        String questionNorm = normalize(question);
        List<StudyHit> hits = new ArrayList<>();
        for (TopicVerseRow row : uniq.values()) {
            String norm = normalize(row.text);
            int score = 0;
            for (String t : coreTerms) score += countOccurrences(norm, t) * 25;
            for (String t : semanticTerms) {
                if (!coreTerms.contains(t)) score += countOccurrences(norm, t) * 10;
            }
            if (!questionNorm.isEmpty() && norm.contains(questionNorm)) score += 40;
            if (score > 0) hits.add(new StudyHit(row, score));
        }

        Collections.sort(hits, Comparator
                .comparingInt((StudyHit h) -> h.score).reversed()
                .thenComparing(h -> h.row.bookKey == null ? "" : h.row.bookKey)
                .thenComparingInt(h -> h.row.chapter)
                .thenComparingInt(h -> h.row.verse));

        return hits;
    }

    private String pickDailyFocus(int day) {
        String[] focuses = {
                "Qualità di Geova", "Promesse bibliche", "Decisioni pratiche",
                "Preghiera e fiducia", "Relazioni e amore", "Perseveranza",
                "Condivisione della speranza"
        };
        return focuses[(day - 1) % focuses.length];
    }

    private void insertAnswerIntoNote() {
        String answer = safeText(answerView);
        if (answer.isEmpty() || answer.equals(getString(R.string.study_answer_placeholder))) {
            toast(getString(R.string.study_no_answer_to_insert));
            return;
        }

        String current = safeText(noteBodyInput);
        String merged = current.isEmpty() ? answer : current + "\n\n---\n\n" + answer;
        noteBodyInput.setText(merged);
        if (safeText(noteTitleInput).isEmpty()) {
            noteTitleInput.setText("Studio biblico " + DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT).format(new Date()));
        }
    }

    private void insertPathIntoNote() {
        String path = safeText(studyPathView);
        if (path.isEmpty() || path.equals(getString(R.string.study_path_placeholder))) {
            toast(getString(R.string.study_no_path_to_insert));
            return;
        }
        String current = safeText(noteBodyInput);
        String merged = current.isEmpty() ? path : current + "\n\n---\n\n" + path;
        noteBodyInput.setText(merged);
        if (safeText(noteTitleInput).isEmpty()) {
            noteTitleInput.setText(getString(R.string.study_path_title_fallback));
        }
    }

    private void insertGlossaryIntoNote() {
        String glossary = safeText(glossaryView);
        if (glossary.isEmpty() || glossary.equals(getString(R.string.study_glossary_placeholder))) {
            toast(getString(R.string.study_no_glossary_to_insert));
            return;
        }
        String current = safeText(noteBodyInput);
        String merged = current.isEmpty() ? glossary : current + "\n\n---\n\n" + glossary;
        noteBodyInput.setText(merged);
    }

    private void insertResearchGuideIntoNote() {
        String guide = safeText(researchGuideView);
        if (guide.isEmpty() || guide.equals(getString(R.string.study_research_placeholder))) {
            toast(getString(R.string.study_no_research_to_insert));
            return;
        }
        String current = safeText(noteBodyInput);
        String merged = current.isEmpty() ? guide : current + "\n\n---\n\n" + guide;
        noteBodyInput.setText(merged);
    }

    private String searchResearchGuide(String term) {
        String normalized = normalize(term);
        if (normalized.isEmpty()) return getString(R.string.study_research_not_found);

        String cacheText = loadResearchGuideCacheText();
        if (cacheText.isEmpty() && canUseNetworkSources()) {
            cacheText = refreshResearchGuideCache();
        }
        if (cacheText.isEmpty()) return getString(R.string.study_research_not_found);

        String result = searchResearchGuideInText(cacheText, normalized);
        if (!result.isEmpty()) return result;

        return getString(R.string.study_research_not_found)
                + "\n\nFonte: " + WOL_RESEARCH_GUIDE_URL;
    }

    private String refreshResearchGuideCache() {
        try {
            Document doc = Jsoup.connect(WOL_RESEARCH_GUIDE_URL)
                    .userAgent(defaultUserAgent())
                    .referrer("https://wol.jw.org/it/wol/h/r6/lp-i")
                    .timeout(20000)
                    .maxBodySize(0)
                    .followRedirects(true)
                    .get();

            LinkedHashMap<String, String> topics = new LinkedHashMap<>();
            Elements links = doc.select("a[href]");
            for (Element a : links) {
                String href = a.absUrl("href");
                String text = a.text() == null ? "" : a.text().trim();
                if (text.isEmpty() || href.isEmpty()) continue;
                String lower = text.toLowerCase(Locale.ITALIAN);
                if (lower.contains("condividi")
                        || lower.contains("impostazioni")
                        || lower.contains("copyright")
                        || lower.equals("italiano")) {
                    continue;
                }
                if (href.contains("/wol/d/r6/lp-i/") || href.contains("/wol/publication/r6/lp-i/rsg19/")) {
                    topics.put(text, href);
                }
            }

            if (topics.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            sb.append("Guida alle ricerche per i Testimoni di Geova — Edizione 2019\n");
            sb.append("Fonte: ").append(WOL_RESEARCH_GUIDE_URL).append("\n\n");
            sb.append("Argomenti consultabili:\n");
            for (Map.Entry<String, String> e : topics.entrySet()) {
                sb.append("- ").append(e.getKey()).append(" | ").append(e.getValue()).append("\n");
            }

            String built = sb.toString().trim();
            saveResearchGuideCacheText(built);
            return built;
        } catch (Exception ignored) {
            return "";
        }
    }

    private String searchResearchGuideInText(String cacheText, String term) {
        if (cacheText == null || cacheText.trim().isEmpty()) return "";
        String normalizedTerm = normalize(term);
        if (normalizedTerm.isEmpty()) return "";

        String[] lines = cacheText.split("\\n");
        List<String> matches = new ArrayList<>();
        for (String line : lines) {
            String l = line == null ? "" : line.trim();
            if (l.isEmpty()) continue;
            String normalizedLine = normalize(l);
            if (normalizedLine.contains(normalizedTerm)) {
                matches.add(l);
            }
            if (matches.size() >= 12) break;
        }

        if (matches.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Risultati Guida alle ricerche per: ").append(term).append("\n\n");
        for (String m : matches) sb.append(m).append("\n");
        sb.append("\nFonte: ").append(WOL_RESEARCH_GUIDE_URL);
        return sb.toString().trim();
    }

    private void saveResearchGuideCacheText(String text) {
        if (text == null || text.trim().isEmpty()) return;
        File target = new File(getFilesDir(), RESEARCH_GUIDE_CACHE_FILE);
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(target, false), StandardCharsets.UTF_8)) {
            writer.write(text);
        } catch (Exception ignored) {}
    }

    private String loadResearchGuideCacheText() {
        File target = new File(getFilesDir(), RESEARCH_GUIDE_CACHE_FILE);
        if (!target.exists()) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(target), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!TextUtils.isEmpty(line)) sb.append(line).append("\n");
            }
        } catch (Exception ignored) {
            return "";
        }
        return sb.toString().trim();
    }

    private String getGlossaryDefinition(String term) {
        String normalized = normalize(term);
        if (normalized.isEmpty()) return getString(R.string.study_glossary_not_found);

        String cached = getGlossaryDefinitionCachedOnly(normalized);
        if (!cached.isEmpty()) return cached;

        if (!canUseNetworkSources()) return getString(R.string.study_glossary_not_found);

        Map<String, String> index = loadGlossaryIndex();
        String url = findGlossaryUrl(index, normalized);

        if (url == null) {
            String slug = buildSlug(term);
            if (!slug.isEmpty()) url = JW_GLOSSARY_BASE + slug + "/";
        }

        if (url == null) return getString(R.string.study_glossary_not_found);

        GlossaryEntry entry = fetchGlossaryEntry(url);
        if (entry == null || entry.definition.isEmpty()) {
            return getString(R.string.study_glossary_not_found);
        }

        String formatted = entry.title + "\n\n" + entry.definition + "\n\nFonte: " + entry.url;
        saveGlossaryDefinition(normalized, formatted);
        if (!entry.title.isEmpty()) saveGlossaryDefinition(normalize(entry.title), formatted);
        return formatted;
    }

    private String getGlossaryDefinitionCachedOnly(String term) {
        SharedPreferences sp = getSharedPreferences(PREFS_GLOSSARY_CACHE, MODE_PRIVATE);
        return sp.getString("def_" + normalize(term), "");
    }

    private void saveGlossaryDefinition(String normalizedTerm, String content) {
        SharedPreferences sp = getSharedPreferences(PREFS_GLOSSARY_CACHE, MODE_PRIVATE);
        sp.edit().putString("def_" + normalizedTerm, content).apply();
    }

    private Map<String, String> loadGlossaryIndex() {
        SharedPreferences sp = getSharedPreferences(PREFS_GLOSSARY_CACHE, MODE_PRIVATE);
        String json = sp.getString(KEY_INDEX_JSON, "");
        Map<String, String> index = parseIndexJson(json);
        if (!index.isEmpty()) return index;
        if (!canUseNetworkSources()) return index;

        Map<String, String> fetched = fetchGlossaryIndexFromWeb();
        if (!fetched.isEmpty()) {
            sp.edit().putString(KEY_INDEX_JSON, toIndexJson(fetched)).apply();
        }
        return fetched;
    }

    private Map<String, String> fetchGlossaryIndexFromWeb() {
        Map<String, String> out = new LinkedHashMap<>();
        try {
            Document doc = Jsoup.connect(JW_GLOSSARY_BASE)
                    .userAgent(defaultUserAgent())
                    .referrer("https://www.jw.org/it/")
                    .timeout(20000)
                    .maxBodySize(0)
                    .followRedirects(true)
                    .get();

            Elements links = doc.select("a[href]");
            for (Element a : links) {
                String href = a.absUrl("href");
                String label = a.text() == null ? "" : a.text().trim();
                if (href.isEmpty() || label.isEmpty()) continue;
                if (!href.startsWith(JW_GLOSSARY_BASE)) continue;
                if (href.equals(JW_GLOSSARY_BASE)) continue;
                if (!href.endsWith("/")) continue;
                out.put(normalize(label), href);
            }
        } catch (Exception ignored) {}
        return out;
    }

    private String findGlossaryUrl(Map<String, String> index, String normalizedTerm) {
        if (index == null || index.isEmpty()) return null;
        String direct = index.get(normalizedTerm);
        if (direct != null) return direct;

        for (Map.Entry<String, String> e : index.entrySet()) {
            String key = e.getKey();
            if (key == null) continue;
            if (key.equals(normalizedTerm)
                    || key.startsWith(normalizedTerm + " ")
                    || key.contains(normalizedTerm)) {
                return e.getValue();
            }
        }
        return null;
    }

    private GlossaryEntry fetchGlossaryEntry(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(defaultUserAgent())
                    .referrer(JW_GLOSSARY_BASE)
                    .timeout(20000)
                    .maxBodySize(0)
                    .followRedirects(true)
                    .get();

            String title = "";
            Element h1 = doc.selectFirst("h1");
            if (h1 != null) title = h1.text().trim();

            String definition = "";
            for (Element p : doc.select("p")) {
                String text = p.text() == null ? "" : p.text().trim();
                if (text.length() < 35) continue;
                String lower = text.toLowerCase(Locale.ITALIAN);
                if (lower.contains("cookie")
                        || lower.contains("impostazioni privacy")
                        || lower.contains("jw.org")
                        || lower.contains("formati per il download")) {
                    continue;
                }
                definition = text;
                break;
            }
            if (definition.isEmpty()) return null;
            return new GlossaryEntry(title, definition, url);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildSlug(String input) {
        String n = normalize(input);
        if (n.isEmpty()) return "";
        n = n.replace(' ', '-');
        try {
            String encoded = URLEncoder.encode(n, "UTF-8");
            return encoded.replace("+", "-");
        } catch (Exception e) {
            return n;
        }
    }

    private Map<String, String> parseIndexJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || json.trim().isEmpty()) return map;
        try {
            JSONObject obj = new JSONObject(json);
            java.util.Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = obj.optString(key, "");
                if (!value.isEmpty()) map.put(key, value);
            }
        } catch (Exception ignored) {}
        return map;
    }

    private String toIndexJson(Map<String, String> map) {
        JSONObject obj = new JSONObject();
        try {
            for (Map.Entry<String, String> e : map.entrySet()) {
                obj.put(e.getKey(), e.getValue());
            }
        } catch (Exception ignored) {}
        return obj.toString();
    }

    private String defaultUserAgent() {
        String ua = System.getProperty("http.agent");
        if (ua == null || ua.trim().isEmpty()) return "ituoiversetti/1.0";
        return ua;
    }

    private boolean isConnected() {
        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;
        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private boolean canUseNetworkSources() {
        return allowNetworkSources && isConnected();
    }

    private void saveLocalNote() {
        String content = buildNoteContent(false);
        if (content.isEmpty()) {
            toast(getString(R.string.study_note_empty));
            return;
        }

        executor.execute(() -> {
            try {
                File folder = new File(getFilesDir(), "study_notes");
                if (!folder.exists() && !folder.mkdirs()) {
                    throw new IllegalStateException("Cartella note non disponibile");
                }
                String fileName = safeFileName(buildNoteTitle()) + "_" + System.currentTimeMillis() + ".txt";
                File target = new File(folder, fileName);
                try (OutputStreamWriter writer = new OutputStreamWriter(
                        new FileOutputStream(target), StandardCharsets.UTF_8)) {
                    writer.write(content);
                }
                runOnUiThread(() -> toast(getString(R.string.study_saved_local) + "\n" + target.getAbsolutePath()));
            } catch (Exception e) {
                runOnUiThread(() -> toast(getString(R.string.study_error) + ": " + e.getMessage()));
            }
        });
    }

    private void exportNote(boolean markdown) {
        String content = buildNoteContent(markdown);
        if (content.isEmpty()) {
            toast(getString(R.string.study_note_empty));
            return;
        }

        executor.execute(() -> {
            try {
                String ext = markdown ? ".md" : ".txt";
                String mime = markdown ? "text/markdown" : "text/plain";
                String name = safeFileName(buildNoteTitle()) + "_" + System.currentTimeMillis() + ext;
                String exported = exportTextFile(content, name, mime);
                runOnUiThread(() -> toast(getString(R.string.study_export_ok) + "\n" + exported));
            } catch (Exception e) {
                runOnUiThread(() -> toast(getString(R.string.study_error) + ": " + e.getMessage()));
            }
        });
    }

    private void shareNote() {
        String content = buildNoteContent(false);
        if (content.isEmpty()) {
            toast(getString(R.string.study_note_empty));
            return;
        }
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, buildNoteTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, content);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.study_share_note)));
        } catch (Exception e) {
            toast(getString(R.string.study_error) + ": " + e.getMessage());
        }
    }

    private String exportTextFile(String content, String fileName, String mimeType) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String path = exportViaMediaStore(content, fileName, mimeType, Environment.DIRECTORY_DOCUMENTS);
            if (path != null) return path;
            path = exportViaMediaStore(content, fileName, mimeType, Environment.DIRECTORY_DOWNLOADS);
            if (path != null) return path;
        }

        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        if (root == null) root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (root == null) throw new IllegalStateException("Cartella pubblica non disponibile");

        File folder = new File(root, "I_Tuoi_Versetti");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Impossibile creare cartella export");
        }

        File target = new File(folder, fileName);
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(target), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
        return target.getAbsolutePath();
    }

    private String exportViaMediaStore(String content,
                                       String fileName,
                                       String mimeType,
                                       String rootDir) {
        try {
            ContentResolver resolver = getContentResolver();
            Uri collection = MediaStore.Files.getContentUri("external");
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, rootDir + "/I_Tuoi_Versetti");

            Uri uri = resolver.insert(collection, values);
            if (uri == null) return null;

            try (OutputStream out = resolver.openOutputStream(uri, "w")) {
                if (out == null) return null;
                out.write(content.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            return uri.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildNoteContent(boolean markdown) {
        String title = buildNoteTitle();
        String body = safeText(noteBodyInput);
        String answer = safeText(answerView);
        String question = safeText(questionInput);

        if (body.isEmpty() && (answer.isEmpty() || answer.equals(getString(R.string.study_answer_placeholder)))) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        if (markdown) {
            sb.append("# ").append(title).append("\n\n");
            if (!question.isEmpty()) {
                sb.append("## Domanda\n").append(question).append("\n\n");
            }
            if (!body.isEmpty()) {
                sb.append("## Appunti\n").append(body).append("\n\n");
            }
            if (!answer.isEmpty() && !answer.equals(getString(R.string.study_answer_placeholder))) {
                sb.append("## Assistente (locale)\n").append(answer).append("\n");
            }
        } else {
            sb.append(title).append("\n");
            sb.append("Generato: ").append(DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT).format(new Date())).append("\n\n");
            if (!question.isEmpty()) {
                sb.append("Domanda:\n").append(question).append("\n\n");
            }
            if (!body.isEmpty()) {
                sb.append("Appunti:\n").append(body).append("\n\n");
            }
            if (!answer.isEmpty() && !answer.equals(getString(R.string.study_answer_placeholder))) {
                sb.append("Assistente (locale):\n").append(answer).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String buildNoteTitle() {
        String title = safeText(noteTitleInput);
        if (!title.isEmpty()) return title;
        String question = safeText(questionInput);
        if (!question.isEmpty()) {
            return "Studio - " + question.substring(0, Math.min(40, question.length()));
        }
        return "Studio biblico";
    }

    private static String safeFileName(String input) {
        String clean = input == null ? "studio" : input.trim();
        if (clean.isEmpty()) clean = "studio";
        clean = clean.replaceAll("[^a-zA-Z0-9 _-]", "").trim();
        clean = clean.replaceAll("\\s+", "_");
        return clean.isEmpty() ? "studio" : clean;
    }

    private static String safeText(TextView view) {
        if (view == null || view.getText() == null) return "";
        return view.getText().toString().trim();
    }

    private List<String> parseTerms(String input) {
        String normalized = normalize(input);
        if (normalized.isEmpty()) return Collections.emptyList();
        String[] parts = normalized.split(" ");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            if (part.length() >= 2) out.add(part);
        }
        return out;
    }

    private String normalize(String input) {
        String n = input == null ? "" : input.toLowerCase(Locale.ITALIAN).trim();
        n = Normalizer.normalize(n, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        n = n.replaceAll("[^a-z0-9\\s]", " ");
        return n.replaceAll("\\s+", " ").trim();
    }

    private List<String> buildSemanticTerms(List<String> coreTerms) {
        LinkedHashSet<String> out = new LinkedHashSet<>(coreTerms);
        for (String term : coreTerms) {
            out.addAll(semanticSynonyms(term));
            String stem = stemItalian(term);
            if (stem.length() >= 3) out.add(stem);
        }
        return new ArrayList<>(out);
    }

    private List<String> semanticSynonyms(String term) {
        switch (term) {
            case "fede": return Arrays.asList("fiducia", "credere", "fedelta", "credenti");
            case "speranza": return Arrays.asList("attesa", "promessa", "futuro", "consolazione");
            case "amore": return Arrays.asList("carita", "misericordia", "affetto", "benevolenza");
            case "preghiera": return Arrays.asList("pregare", "invocare", "supplica", "orazione");
            case "pace": return Arrays.asList("serenita", "calma", "tranquillita", "concordia");
            case "famiglia": return Arrays.asList("casa", "genitori", "figli", "matrimonio");
            case "perdono": return Arrays.asList("perdonare", "misericordia", "colpa", "colpe");
            default: return Collections.emptyList();
        }
    }

    private String stemItalian(String term) {
        if (term == null) return "";
        String t = term.trim();
        if (t.length() <= 4) return t;
        String[] endings = {"mente", "zioni", "zione", "amenti", "amento", "atori", "atore",
                "anti", "ante", "are", "ere", "ire", "ita", "ivo", "iva", "osi", "oso", "osa", "i", "e", "a", "o"};
        for (String end : endings) {
            if (t.endsWith(end) && t.length() - end.length() >= 3) {
                return t.substring(0, t.length() - end.length());
            }
        }
        return t;
    }

    private int countOccurrences(String text, String term) {
        if (text == null || term == null || term.isEmpty()) return 0;
        int count = 0;
        int from = 0;
        while (true) {
            int idx = text.indexOf(term, from);
            if (idx < 0) break;
            count++;
            from = idx + term.length();
        }
        return count;
    }

    private String inferPrinciple(List<String> coreTerms) {
        if (coreTerms.contains("fede")) {
            return "Geova invita a sviluppare fiducia in Lui anche nelle prove, con perseveranza e preghiera.";
        }
        if (coreTerms.contains("speranza")) {
            return "La Bibbia orienta la mente alle promesse future di Geova, generando stabilita e conforto.";
        }
        if (coreTerms.contains("preghiera")) {
            return "La preghiera sincera rafforza il legame personale con Geova e guida le decisioni quotidiane.";
        }
        if (coreTerms.contains("amore")) {
            return "L’amore biblico e pratico riflette il pensiero di Geova e costruisce relazioni sane e leali.";
        }
        if (coreTerms.contains("pace")) {
            return "La pace interiore cresce quando la mente si allinea ai principi biblici e alla fiducia in Geova.";
        }
        return "I versetti suggeriti mostrano principi coerenti con il modo di pensare di Geova: verita, amore, santita e giustizia.";
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

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private static final class StudyHit {
        final TopicVerseRow row;
        final int score;

        StudyHit(TopicVerseRow row, int score) {
            this.row = row;
            this.score = score;
        }
    }

    private static final class GlossaryEntry {
        final String title;
        final String definition;
        final String url;

        GlossaryEntry(String title, String definition, String url) {
            this.title = title == null ? "" : title.trim();
            this.definition = definition == null ? "" : definition.trim();
            this.url = url == null ? "" : url.trim();
        }
    }
}

package com.testing.ituoiversetti;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.io.File;

public class BibleIndexWorker extends Worker {

    private static final String TAG = "BibleIndexWorker";

    private static final String PREFS = "bible_index";
    private static final String KEY_FP = "pdf_fp";
    private static final String KEY_LAST_SUCCESS_MS = "last_success_ms";
    private static final String KEY_LAST_ERROR = "last_error";


    // Libri standard (ordine Bibbia)
    private static final List<String> STD_BOOKS = new Bibbia().composeBibbia();

    // key(qualsiasi variante) -> nome standard (es. "1 Corinti")
    private static final Map<String, String> KEY_TO_STD_BOOK =
            BookNameUtil.buildKeyToStandardMap(STD_BOOKS);

    // key(nome standard) -> indice libro (0..65)
    private static final Map<String, Integer> STD_KEY_TO_INDEX = buildStdKeyToIndex();

    // max capitoli per indice libro (0..65), preso da Capitolo
    private static final List<Integer> MAX_CHAPS = initMaxChaps();

    private static Map<String, Integer> buildStdKeyToIndex() {
        HashMap<String, Integer> out = new HashMap<>();
        for (int i = 0; i < STD_BOOKS.size(); i++) {
            out.put(BookNameUtil.key(STD_BOOKS.get(i)), i);
        }
        return Collections.unmodifiableMap(out);
    }

    // Voci di Sommario: "43 Giuseppe ... (15-23)" (anche su 2 righe)
    private static final Pattern SUMMARY_ENTRY_BLOCK = Pattern.compile(
            "(?im)^\\s*\\d{1,3}\\s+[^\\n]{0,120}(?:\\n\\s+[^\\n]{0,120})?\\(\\s*\\d{1,3}[a-z]?(?:\\s*[\\-–—,]\\s*\\d{1,3}[a-z]?)*\\s*\\)\\s*$"
    );
    
    // Riga “solo cap:vers” tipica colonna centrale/note
    private static final Pattern BARE_CHAP_VER_LINE = Pattern.compile(
            "(?im)^\\s*(?:[a-z]\\s+)?\\d{1,3}:\\d{1,3}(?:\\s*[\\-–—]\\s*\\d{1,3}(?::\\d{1,3})?)?\\s*$"
    );


    private static List<Integer> initMaxChaps() {
        try {
            Capitolo c = new Capitolo();
            return new ArrayList<>(c.getCapitoli());
        } catch (Exception e) {
            return Collections.emptyList(); // fallback: niente validazione range
        }
    }

    private static boolean isValidChapter(String stdBookKey, int chap) {
        if (chap < 1) return false;
        if (MAX_CHAPS.isEmpty()) return true; // fallback
        Integer idx = STD_KEY_TO_INDEX.get(stdBookKey);
        if (idx == null || idx < 0 || idx >= MAX_CHAPS.size()) return true;
        int max = MAX_CHAPS.get(idx);
        return chap <= max;
    }

    // Header: "Libro" + "cap:inizio-fine"
    private static final Pattern HEADER = Pattern.compile(
            "(?m)^\\s*(\\S.*\\S)\\s*(?:\\R|\\s+)\\s*(\\d+):(\\d+)[\\-–—](\\d+)\\s*$"
    );

    private static final String SOMMARIO_RX =
        "(?:SOMMARIO|S\\s*O\\s*M\\s*M\\s*A\\s*R\\s*I\\s*O)";

    // Marker versetto: start riga o punteggiatura forte prima del numero
    private static final Pattern VERSE_MARK = Pattern.compile(
            "(?m)(^|[\\n\\r\\.\\!\\?\\+;:”“\"»«])\\s*([1-9]\\d{0,2})\\s+(?![º°])"
    );

        // Rumore tipico PDF a 2 colonne: header pagina/capitolo (es. "1659 RIVELAZIONE 20:5-21:8")
        private static final Pattern PAGE_HEADER_LINE = Pattern.compile(
                "(?im)^\\s*(?:\\d{1,4}\\s+)?[A-ZÀ-Ü][A-ZÀ-Ü\\s'’]{2,}\\s+(?:" + SOMMARIO_RX + "\\s+)?\\d{1,3}:\\d{1,3}(?:\\s*[\\-–—]\\s*\\d{1,3}:\\d{1,3})?\\s*(?:\\d{1,4})?\\s*$"
        );

            // Header con sommario che termina nel capitolo (es. "EBREI Sommario - 2:3")
            private static final Pattern PAGE_HEADER_SUMMARY_TO = Pattern.compile(
                    "(?im)^\\s*(?:\\d{1,4}\\s+)?(?:[1-3]\\s+)?[A-ZÀ-Ü][A-ZÀ-Ü\\s'’]{1,40}\\s+" + SOMMARIO_RX + "\\s*[\\-–—]\\s*\\d{1,3}:\\d{1,3}\\s*(?:\\d{1,4})?\\s*$"
            );

                // Header ponte tra libri (es. "FILEMONE 21 - EBREI SOMMARIO")
                private static final Pattern PAGE_HEADER_BOOK_TO_SUMMARY = Pattern.compile(
                        "(?im)^\\s*(?:\\d{1,4}\\s+)?(?:[1-3]\\s+)?[A-ZÀ-Ü][A-ZÀ-Ü\\s'’]{1,40}\\s+\\d{1,3}\\s*[\\-–—]\\s*(?:[1-3]\\s+)?[A-ZÀ-Ü][A-ZÀ-Ü\\s'’]{1,40}\\s+" + SOMMARIO_RX + "\\s*(?:\\d{1,4})?\\s*$"
                );

            // Header raro cross-libro senza ":" (es. "3 GIOVANNI 11 - GIUDA 5")
            private static final Pattern PAGE_HEADER_CROSS_BOOK = Pattern.compile(
                    "(?im)^\\s*(?:\\d{1,4}\\s+)?(?:[1-3]\\s+)?[A-ZÀ-Ü][A-ZÀ-Ü\\s'’]{1,40}\\s+\\d{1,3}(?::\\d{1,3})?\\s*[\\-–—]\\s*(?:[1-3]\\s+)?[A-ZÀ-Ü][A-ZÀ-Ü\\s'’]{1,40}\\s+\\d{1,3}(?::\\d{1,3})?\\s*(?:\\d{1,4})?\\s*$"
            );

        // Riferimenti laterali/infracolonna (es. "Gen 21:1, 2", "Rivelazione 21:4 [Pagina 1659]")
        private static final Pattern SIDE_REFERENCE = Pattern.compile(
            "\\b(?:[1-3]\\s*)?[A-ZÀ-Ü][a-zà-ù]{1,16}\\s+\\d{1,3}:\\d{1,3}(?:\\s*,\\s*\\d{1,3})?(?:\\s*[\\-–—]\\s*\\d{1,3})?\\b"
        );

        private static final Pattern PAGE_NOTE = Pattern.compile("\\[\\s*Pagina\\s+\\d{1,4}\\s*]", Pattern.CASE_INSENSITIVE);

            // Blocco ponte tra capitoli (centrale): "SOMMARIO ..." da scartare
            private static final Pattern SUMMARY_BLOCK = Pattern.compile(
                    "(?is)(?:\\bSOMMARIO\\b|\\bS\\s*O\\s*M\\s*M\\s*A\\s*R\\s*I\\s*O\\b).*?(?=(?:\\n\\s*[1-9]\\d{0,2}\\s+)|$)"
            );

                // Frontespizi/marker capitolo e numero pagina centrato (rumore da PDF)
                private static final Pattern FRONT_MATTER_LINE = Pattern.compile(
                    "(?im)^\\s*(?:PRIMA|SECONDA|TERZA)\\s+LETTERA\\s+DI\\s*$|^\\s*[A-ZÀ-Ü]{3,}(?:\\s+[A-ZÀ-Ü]{2,}){0,5}\\s*$"
                );
                private static final Pattern CAP_MARKER = Pattern.compile("(?im)\\bCAP\\.?\\s*\\d{1,3}\\b");
                private static final Pattern PAGE_NUMBER_LINE = Pattern.compile("(?m)^\\s*\\d{3,4}\\s*$");

                    // Marker colonna centrale (es. "2° col.")
                    private static final Pattern COLUMN_MARKER_LINE = Pattern.compile("(?im)^\\s*\\d+\\s*[°º]\\s*col\\.?\\s*$");

                    // Riga quasi solo di riferimenti (tipica colonna centrale/nota): "a Gv 3:16", "b 1Re 2:1-3"...
                    private static final Pattern DENSE_REFERENCE_LINE = Pattern.compile(
                        "(?im)^\\s*(?:[a-z]\\s+)?(?:[1-3]?\\s*[A-ZÀ-Ü][a-zà-ù]{1,10}\\s+\\d{1,3}:\\d{1,3}(?:\\s*,\\s*\\d{1,3})?(?:\\s*[\\-–—]\\s*\\d{1,3})?\\s*){1,3}$"
                    );

                        // Riga con singolo riferimento secco (es. "Isa 45:18", "v Eso 30:1")
                        private static final Pattern SINGLE_REFERENCE_LINE = Pattern.compile(
                            "(?im)^\\s*(?:[a-z]\\s+)?(?:[1-3]\\s*)?[A-ZÀ-Ü][a-zà-ù]{1,16}\\s+\\d{1,3}:\\d{1,3}(?:\\s*,\\s*\\d{1,3})?(?:\\s*[\\-–—]\\s*\\d{1,3})?\\s*$"
                        );

                        // Riga indice/sommario (es. "Giuseppe (13-26)")
                        private static final Pattern TOC_ITEM_LINE = Pattern.compile(
                            "(?im)^\\s*[A-ZÀ-Ü][^\\n]{0,120}\\(\\d{1,3}[a-z]?(?:[\\-–—]\\d{1,3}[a-z]?)?\\)\\s*$"
                        );
                            private static final Pattern NUMBERED_TOC_ITEM_LINE = Pattern.compile(
                                "(?im)^\\s*\\d{1,3}\\s+[A-ZÀ-Ü][^\\n]{0,120}\\(\\d{1,3}[a-z]?(?:[\\-–—]\\d{1,3}[a-z]?)?\\)\\s*$"
                            );

                    // Note lessicali in basso (es. "2:1 'o avvocato' ...")
                    private static final Pattern FOOTNOTE_GLOSS = Pattern.compile(
                        "(?is)\\b\\d{1,3}:\\d{1,3}\\s+[“\"']?o\\s+[^\\n]{1,120}(?:[.;]|$)"
                    );

                        // Riga nota a fine pagina (sotto linea), tipicamente non biblica
                        private static final Pattern FOOTNOTE_REFERENCE_LINE = Pattern.compile(
                            "(?im)^\\s*(?:[1-3]\\s*)?\\d{1,3}:\\d{1,3}(?:\\s*[\\-–—]\\s*\\d{1,3})?\\s+(?:Lett\\.|Cioè|O|Oppure|Vedi|In riferimento)\\b.*$"
                        );

                        // Cluster di commenti con più riferimenti sulla stessa riga (es. 4:1 ... 4:7 ... 4:48 ...)
                        private static final Pattern FOOTNOTE_CLUSTER_LINE = Pattern.compile(
                            "(?im)^\\s*(?:[1-3]\\s*)?\\d{1,3}:\\d{1,3}[^\\n]*\\d{1,3}:\\d{1,3}[^\\n]*$"
                        );

                            private static final Pattern NOISE_SYMBOL_LINE = Pattern.compile("(?m)^\\s*[`´^~_=*•·\\-]{2,}\\s*$");

                                // Coda riga da scartare quando inizia la colonna riferimenti (es. "... f Eso 27:9 ...")
                                private static final Pattern INLINE_REFERENCE_TAIL = Pattern.compile(
                                    "\\s+[a-z]\\s+(?:[1-3]\\s*)?[A-ZÀ-Ü][a-zà-ù]{1,16}\\s+\\d{1,3}:\\d{1,3}.*$"
                                );

    public BibleIndexWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        // progress + log start
        setProgressAsync(new Data.Builder().putInt("pct", 1).putString("stage", "Apro PDF").build());
        Log.i(TAG, "start. ctx=" + ctx);

        try {
            // fingerprint per non reindicizzare inutilmente
            File pdf = PdfParser.getReadablePdfFile(ctx);
            String fp = pdf.length() + ":" + pdf.lastModified();

            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String done = sp.getString(KEY_FP, "");

            Log.i(TAG, "pdf=" + pdf.getAbsolutePath() + " size=" + pdf.length() + " lastMod=" + pdf.lastModified());
            Log.i(TAG, "fp=" + fp + " done=" + done);

            if (fp.equals(done)) {
                Log.i(TAG, "skip: fingerprint unchanged");
                sp.edit()
                        .putLong(KEY_LAST_SUCCESS_MS, System.currentTimeMillis())
                        .putString(KEY_LAST_ERROR, "")
                        .apply();
                setProgressAsync(new Data.Builder().putInt("pct", 100).putString("stage", "Già indicizzato").build());
                return Result.success();
            }

            // estrai testo (pesante ma una tantum)
            setProgressAsync(new Data.Builder().putInt("pct", 5).putString("stage", "Estraggo testo PDF (può essere lento)").build());
            long t0 = System.currentTimeMillis();
            String text = PdfParser.extractAllText(ctx);
            long t1 = System.currentTimeMillis();
            if (text == null) text = "";
            Log.i(TAG, "extractAllText: chars=" + text.length() + " ms=" + (t1 - t0));

            text = normalize(text);

            // trova headers
            setProgressAsync(new Data.Builder().putInt("pct", 15).putString("stage", "Cerco intestazioni (Libro + cap:da-a)").build());
            List<Hdr> headers = new ArrayList<>();
            Matcher hm = HEADER.matcher(text);

            int headerSamples = 0;
            while (hm.find()) {
                String bookRaw = hm.group(1).trim();
                int chap = Integer.parseInt(hm.group(2));
            
                // se davanti c'è un numero pagina, toglilo (succede spesso)
                String bookCandidate = bookRaw.replaceFirst("^\\s*\\d{1,4}\\s+", "").trim();
            
                // standardizza: breve/esteso -> "Nome standard" (es. "1 Corinti")
                String stdBook = BookNameUtil.toStandardBookName(bookCandidate, KEY_TO_STD_BOOK);
                if (stdBook == null) stdBook = BookNameUtil.toStandardBookName(bookRaw, KEY_TO_STD_BOOK);
            
                if (stdBook == null) {
                    Log.w(TAG, "HDR skip: unmapped bookRaw='" + bookRaw + "'");
                    continue;
                }
            
                String stdBookKey = BookNameUtil.key(stdBook);
            
                // valida capitolo usando Capitolo (range max capitoli per quel libro)
                if (!isValidChapter(stdBookKey, chap)) {
                    Integer idx = STD_KEY_TO_INDEX.get(stdBookKey);
                    int max = (idx != null && !MAX_CHAPS.isEmpty() && idx < MAX_CHAPS.size()) ? MAX_CHAPS.get(idx) : -1;
                    Log.w(TAG, "HDR skip: invalid chapter book='" + stdBook + "' chap=" + chap + " max=" + max);
                    continue;
                }
            
                headers.add(new Hdr(stdBookKey, stdBook, bookRaw, chap, hm.end(), hm.start()));
            
                if (headerSamples < 10) {
                    Log.i(TAG, "HDR sample: raw='" + bookRaw + "' std='" + stdBook + "' chap=" + chap + " pos=" + hm.start() + "-" + hm.end());
                    headerSamples++;
                }
            }
            Log.i(TAG, "headers found=" + headers.size());

            if (headers.isEmpty()) {
                String snippet = text.length() > 500 ? text.substring(0, 500) : text;
                Log.e(TAG, "HEADER not found. text head snippet:\n" + snippet);
                sp.edit().putString(KEY_LAST_ERROR, "HEADER non trovato nel testo estratto dal PDF").apply();

                Data out = new Data.Builder()
                        .putString("err", "HEADER non trovato nel testo estratto dal PDF (regex/estrazione).")
                        .build();
                return Result.failure(out);
            }

            // end = start del prossimo header
            for (int i = 0; i < headers.size(); i++) {
                int end = (i + 1 < headers.size()) ? headers.get(i + 1).headerStart : text.length();
                headers.get(i).end = end;
            }

            // DB
            setProgressAsync(new Data.Builder().putInt("pct", 20).putString("stage", "Creo DB (clear + insert)").build());
            BibleDb db = BibleDb.get(ctx);
            VerseDao dao = db.verseDao();

            // rebuild completo
            dao.clearAll();
            Log.i(TAG, "db cleared");

            List<VerseEntity> batch = new ArrayList<>(600);
            long inserted = 0;

            for (int i = 0; i < headers.size(); i++) {
                Hdr h = headers.get(i);

                int pct = 20 + (int) (((i + 1) * 75L) / headers.size()); // 20..95
                setProgressAsync(new Data.Builder().putInt("pct", pct).putString("stage", "Indicizzo: " + (i + 1) + "/" + headers.size()).build());

                // Usa tutte le candidateKeys per mappare il nome libro (breve/esteso)
                List<String> candidateKeys = BookNameUtil.candidateKeys(h.bookKey);
                String bookKey = null;
                for (String cand : candidateKeys) {
                    // Qui puoi aggiungere un controllo su una lista di libri validi se necessario
                    if (!cand.isEmpty()) {
                        bookKey = cand;
                        break;
                    }
                }
                if (bookKey == null || bookKey.isEmpty()) {
                    // salta header “strano”
                    continue;
                }

                // Validazione capitolo: opzionale, aggiungi qui se vuoi confrontare con Capitolo.createNumCap()

                String chunk = text.substring(h.contentStart, h.end);
                int before = batch.size();
                parseChapterInto(batch, bookKey, h.chapter, chunk);
                int added = batch.size() - before;

                // log: ogni 200 header o se added=0
                if ((i % 200) == 0 || added == 0) {
                    Log.i(TAG, "hdr#" + (i + 1) + " bookKey=" + bookKey + " chap=" + h.chapter + " added=" + added);
                }

                if (batch.size() >= 600) {
                    dao.upsertAll(batch);
                    inserted += batch.size();
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                dao.upsertAll(batch);
                inserted += batch.size();
                batch.clear();
            }

            long total = dao.countAll();
            Log.i(TAG, "inserted(approx)=" + inserted + " total(countAll)=" + total);

            // salva fingerprint
                sp.edit()
                    .putString(KEY_FP, fp)
                    .putLong(KEY_LAST_SUCCESS_MS, System.currentTimeMillis())
                    .putString(KEY_LAST_ERROR, "")
                    .apply();

            // invalida cache testo/indice se la usi ancora
            NwtOfflineRepository.invalidate();

            setProgressAsync(new Data.Builder().putInt("pct", 100).putString("stage", "Pronto").build());
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "worker error", e);
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_LAST_ERROR, e.getClass().getSimpleName()).apply();
            return Result.retry();
        }
    }

    private static void parseChapterInto(List<VerseEntity> out, String bookKey, int chapter, String chunk) {
        chunk = sanitizeChunk(chunk);

        List<Mark> marks = new ArrayList<>();
        Matcher m = VERSE_MARK.matcher(chunk);
        while (m.find()) {
            int v = Integer.parseInt(m.group(2));
            marks.add(new Mark(v, m.start(2), m.end(2)));
        }
        if (marks.isEmpty()) return;

        int startIdx = 0;
        int prevVerse = -1;
        // Caso Genesi 2: "2  ..." (numero capitolo) e v1 implicito
        if (chapter != 1 && marks.size() >= 2 && marks.get(0).v == chapter && marks.get(1).v == 2) {
            String v1 = clean(chunk.substring(marks.get(0).afterNum, marks.get(1).numStart), chapter, 1);
            if (!v1.isEmpty()) out.add(row(bookKey, chapter, 1, v1));
            startIdx = 1;
        }

        for (int i = startIdx; i < marks.size(); i++) {
            int v = marks.get(i).v;

            // Se la numerazione riparte chiaramente (es. ...18,21 poi 1 ...),
            // siamo entrati nel capitolo/libro successivo nello stesso chunk.
            if (prevVerse > 0 && isLikelyRestart(prevVerse, v)) {
                break;
            }

            int cs = marks.get(i).afterNum;
            int ce = (i + 1 < marks.size()) ? marks.get(i + 1).numStart : chunk.length();
            String body = clean(chunk.substring(cs, ce), chapter, v);
            if (!body.isEmpty()) out.add(row(bookKey, chapter, v, body));
            prevVerse = v;
        }
    }

    private static boolean isLikelyRestart(int prevVerse, int currentVerse) {
        return prevVerse >= 6 && currentVerse <= 2 && (prevVerse - currentVerse) >= 4;
    }

    private static VerseEntity row(String bookKey, int chapter, int verse, String text) {
        VerseEntity e = new VerseEntity();
        e.bookKey = bookKey;
        e.chapter = chapter;
        e.verse = verse;
        e.text = text;
        return e;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        s = s.replace('\u00A0', ' ').replace('\u202F', ' ');
        s = s.replace("\r\n", "\n").replace("\r", "\n");
        return s;
    }

    private static String sanitizeChunk(String s) {
        s = normalize(s);
    
        // 1) fix “layout”: unisci sillabazioni prima di tutto
        s = s.replaceAll("(?m)(\\p{L})-\\s*\\n\\s*(\\p{L}+)", "$1$2");
    
        // 2) taglia subito roba “macro” che crea falsi versetti
        s = FRONT_MATTER_LINE.matcher(s).replaceAll("\n");
        s = SUMMARY_BLOCK.matcher(s).replaceAll("\n");          // blocchi SOMMARIO
        s = SUMMARY_ENTRY_BLOCK.matcher(s).replaceAll("\n");    // righe numerate del sommario (43,44,45...)
    
        // 3) rimuovi header pagina / righe ponte (molto presenti nei tuoi esempi)
        s = PAGE_HEADER_BOOK_TO_SUMMARY.matcher(s).replaceAll("\n");
        s = PAGE_HEADER_SUMMARY_TO.matcher(s).replaceAll("\n");
        s = PAGE_HEADER_LINE.matcher(s).replaceAll("\n");
        s = PAGE_HEADER_CROSS_BOOK.matcher(s).replaceAll("\n");
    
        // 4) note/pagina/marker vari
        s = PAGE_NOTE.matcher(s).replaceAll(" ");
        s = CAP_MARKER.matcher(s).replaceAll(" ");
        s = PAGE_NUMBER_LINE.matcher(s).replaceAll("\n");
        s = COLUMN_MARKER_LINE.matcher(s).replaceAll("\n");
        s = NOISE_SYMBOL_LINE.matcher(s).replaceAll("\n");
    
        // 5) colonne centrali / note: righe di soli riferimenti o dense
        s = BARE_CHAP_VER_LINE.matcher(s).replaceAll("\n");
        s = DENSE_REFERENCE_LINE.matcher(s).replaceAll("\n");
        s = SINGLE_REFERENCE_LINE.matcher(s).replaceAll("\n");
        s = FOOTNOTE_REFERENCE_LINE.matcher(s).replaceAll("\n");
        s = FOOTNOTE_CLUSTER_LINE.matcher(s).replaceAll("\n");
    
        // 6) indice/sommario “tipo TOC”
        s = TOC_ITEM_LINE.matcher(s).replaceAll("\n");
        s = NUMBERED_TOC_ITEM_LINE.matcher(s).replaceAll("\n");
    
        // 7) riferimenti laterali “inline”
        s = FOOTNOTE_GLOSS.matcher(s).replaceAll(" ");
        s = SIDE_REFERENCE.matcher(s).replaceAll(" ");
        s = INLINE_REFERENCE_TAIL.matcher(s).replaceAll(" ");
    
        // 8) pulizia finale
        s = s.replaceAll("(?m)^\\s*[\\|¦]\\s*$", "");
        s = s.replaceAll("\\n{3,}", "\n\n");
        return s;
    }

    private static String clean(String s, int chapter, int verse) {
        s = normalize(s);
        s = PAGE_NOTE.matcher(s).replaceAll(" ");
        s = SIDE_REFERENCE.matcher(s).replaceAll(" ");
        s = s.replaceAll("[\\*+]+", "");
        s = s.replaceAll("\\s+", " ").trim();

        // Caso reale segnalato: Genesi 12:1 -> testo sporco che parte con "12 ..."
        if (verse == 1) {
            s = s.replaceFirst("^" + chapter + "\\s+(?=\\p{L})", "");
        }

        s = s.replaceFirst("^(?i)sommario\\s+", "");
        s = s.replaceFirst("^(?i)(genesi|esodo|levitico|numeri|deuteronomio|rivelazione|apocalisse)\\s+", "");
        return s;
    }

    private static final class Hdr {
        final String bookKey;      // key del nome standard
        final String stdBookName;  // es. "1 Corinti"
        final String bookRaw;      // per log/debug
        final int chapter;
        final int contentStart;
        final int headerStart;
        int end;
    
        Hdr(String bookKey, String stdBookName, String bookRaw, int chapter, int contentStart, int headerStart) {
            this.bookKey = bookKey;
            this.stdBookName = stdBookName;
            this.bookRaw = bookRaw;
            this.chapter = chapter;
            this.contentStart = contentStart;
            this.headerStart = headerStart;
        }
    }

    private static final class Mark {
        final int v;
        final int numStart;
        final int afterNum;

        Mark(int v, int numStart, int afterNum) {
            this.v = v;
            this.numStart = numStart;
            this.afterNum = afterNum;
        }
    }

}

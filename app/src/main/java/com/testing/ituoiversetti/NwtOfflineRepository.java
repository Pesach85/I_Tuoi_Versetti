package com.testing.ituoiversetti;

import android.content.Context;

import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NwtOfflineRepository {

    private static final String TAG = "NwtOfflineRepo";

    // Header: 2 righe -> "Libro" + "cap:inizio-fine"
        private static final Pattern HEADER = Pattern.compile(
            "(?m)^\\s*(\\S.*\\S)\\s*(?:\\R|\\s+)\\s*(\\d+):(\\d+)[\\-–—](\\d+)\\s*$"
        );

    private static final String SOMMARIO_RX = "(?:SOMMARIO|S\\\\s*O\\\\s*M\\\\s*M\\\\s*A\\\\s*R\\\\s*I\\\\s*O)";

    // Marker versetto: SOLO se preceduto da inizio riga o punteggiatura forte (evita "10 figli")
    // Esempi validi: "\n4  Poi..."  oppure ".+ 4  Poi..."
    private static final Pattern VERSE_MARK = Pattern.compile(
            "(?m)(^|[\\n\\r\\.\\!\\?\\+;:”“\"»«])\\s*([1-9]\\d{0,2})\\s+(?![º°])"
    );

        // Rumore tipico PDF a 2 colonne: header pagina/capitolo e note pagina
        private static final Pattern PAGE_HEADER_LINE = Pattern.compile(
                "(?im)^\\s*(?:\\d{1,4}\\s+)?[A-ZÀ-Ü][A-ZÀ-Ü\\s'’]{2,}\\s+(?:" + SOMMARIO_RX + "\\s+)?\\d{1,3}:\\d{1,3}(?:\\s*[\\-–—]\\s*\\d{1,3}:\\d{1,3})?\\s*(?:\\d{1,4})?\\s*$"
        );
            private static final Pattern PAGE_HEADER_SUMMARY_TO = Pattern.compile(
                "(?im)^\\s*(?:\\d{1,4}\\s+)?(?:[1-3]\\s+)?[A-ZÀ-Ü][A-ZÀ-Ü\\s'’]{1,40}\\s+" + SOMMARIO_RX + "\\s*[\\-–—]\\s*\\d{1,3}:\\d{1,3}\\s*(?:\\d{1,4})?\\s*$"
            );
                private static final Pattern PAGE_HEADER_BOOK_TO_SUMMARY = Pattern.compile(
                "(?im)^\\s*(?:\\d{1,4}\\s+)?(?:[1-3]\\s+)?[A-ZÀ-Ü][A-ZÀ-Ü\\s'’]{1,40}\\s+\\d{1,3}\\s*[\\-–—]\\s*(?:[1-3]\\s+)?[A-ZÀ-Ü][A-ZÀ-Ü\\s'’]{1,40}\\s+" + SOMMARIO_RX + "\\s*(?:\\d{1,4})?\\s*$"
                );
            private static final Pattern PAGE_HEADER_CROSS_BOOK = Pattern.compile(
                    "(?im)^\\s*(?:\\d{1,4}\\s+)?(?:[1-3]\\s+)?[A-ZÀ-Ü][A-ZÀ-Ü\\s'’]{1,40}\\s+\\d{1,3}(?::\\d{1,3})?\\s*[\\-–—]\\s*(?:[1-3]\\s+)?[A-ZÀ-Ü][A-ZÀ-Ü\\s'’]{1,40}\\s+\\d{1,3}(?::\\d{1,3})?\\s*(?:\\d{1,4})?\\s*$"
            );
        private static final Pattern PAGE_NOTE = Pattern.compile("\\[\\s*Pagina\\s+\\d{1,4}\\s*]", Pattern.CASE_INSENSITIVE);
        private static final Pattern SIDE_REFERENCE = Pattern.compile(
            "\\b(?:[1-3]\\s*)?[A-ZÀ-Ü][a-zà-ù]{1,16}\\s+\\d{1,3}:\\d{1,3}(?:\\s*,\\s*\\d{1,3})?(?:\\s*[\\-–—]\\s*\\d{1,3})?\\b"
        );
            private static final Pattern SUMMARY_BLOCK = Pattern.compile(
                    "(?is)(?:\\bSOMMARIO\\b|\\bS\\s*O\\s*M\\s*M\\s*A\\s*R\\s*I\\s*O\\b).*?(?=(?:\\n\\s*[1-9]\\d{0,2}\\s+)|$)"
            );
            private static final Pattern FRONT_MATTER_LINE = Pattern.compile(
                "(?im)^\\s*(?:PRIMA|SECONDA|TERZA)\\s+LETTERA\\s+DI\\s*$|^\\s*[A-ZÀ-Ü]{3,}(?:\\s+[A-ZÀ-Ü]{2,}){0,5}\\s*$"
            );
            private static final Pattern CAP_MARKER = Pattern.compile("(?im)\\bCAP\\.?\\s*\\d{1,3}\\b");
            private static final Pattern PAGE_NUMBER_LINE = Pattern.compile("(?m)^\\s*\\d{3,4}\\s*$");
                private static final Pattern COLUMN_MARKER_LINE = Pattern.compile("(?im)^\\s*\\d+\\s*[°º]\\s*col\\.?\\s*$");
                private static final Pattern DENSE_REFERENCE_LINE = Pattern.compile(
                    "(?im)^\\s*(?:[a-z]\\s+)?(?:[1-3]?\\s*[A-ZÀ-Ü][a-zà-ù]{1,10}\\s+\\d{1,3}:\\d{1,3}(?:\\s*,\\s*\\d{1,3})?(?:\\s*[\\-–—]\\s*\\d{1,3})?\\s*){1,3}$"
                );
                    private static final Pattern SINGLE_REFERENCE_LINE = Pattern.compile(
                        "(?im)^\\s*(?:[a-z]\\s+)?(?:[1-3]\\s*)?[A-ZÀ-Ü][a-zà-ù]{1,16}\\s+\\d{1,3}:\\d{1,3}(?:\\s*,\\s*\\d{1,3})?(?:\\s*[\\-–—]\\s*\\d{1,3})?\\s*$"
                    );
                    private static final Pattern TOC_ITEM_LINE = Pattern.compile(
                        "(?im)^\\s*[A-ZÀ-Ü][^\\n]{0,120}\\(\\d{1,3}[a-z]?(?:[\\-–—]\\d{1,3}[a-z]?)?\\)\\s*$"
                    );
                        private static final Pattern NUMBERED_TOC_ITEM_LINE = Pattern.compile(
                            "(?im)^\\s*\\d{1,3}\\s+[A-ZÀ-Ü][^\\n]{0,120}\\(\\d{1,3}[a-z]?(?:[\\-–—]\\d{1,3}[a-z]?)?\\)\\s*$"
                        );
                private static final Pattern FOOTNOTE_GLOSS = Pattern.compile(
                    "(?is)\\b\\d{1,3}:\\d{1,3}\\s+[“\"']?o\\s+[^\\n]{1,120}(?:[.;]|$)"
                );
                    private static final Pattern FOOTNOTE_REFERENCE_LINE = Pattern.compile(
                        "(?im)^\\s*(?:[1-3]\\s*)?\\d{1,3}:\\d{1,3}(?:\\s*[\\-–—]\\s*\\d{1,3})?\\s+(?:Lett\\.|Cioè|O|Oppure|Vedi|In riferimento)\\b.*$"
                    );
                    private static final Pattern FOOTNOTE_CLUSTER_LINE = Pattern.compile(
                        "(?im)^\\s*(?:[1-3]\\s*)?\\d{1,3}:\\d{1,3}[^\\n]*\\d{1,3}:\\d{1,3}[^\\n]*$"
                    );
                        private static final Pattern NOISE_SYMBOL_LINE = Pattern.compile("(?m)^\\s*[`´^~_=*•·\\-]{2,}\\s*$");
                            private static final Pattern INLINE_REFERENCE_TAIL = Pattern.compile(
                                "\\s+[a-z]\\s+(?:[1-3]\\s*)?[A-ZÀ-Ü][a-zà-ù]{1,16}\\s+\\d{1,3}:\\d{1,3}.*$"
                            );

            // Header pagina con range (es. "RIVELAZIONE 7:15-9:5" oppure "1659 RIVELAZIONE 20:5-21:8")
        private static final Pattern PAGE_RANGE_HEADER = Pattern.compile(
                "(?m)^\\s*(?:\\d{1,4}\\s+)?(\\S.*?\\S)\\s+(\\d+):(\\d+)\\s*[\\-–—]\\s*(\\d+):(\\d+)\\s*(?:\\d{1,4})?\\s*$"
        );

    private static volatile String MEM_TEXT;                 // testo estratto normalizzato
    private static volatile List<Section> MEM_INDEX;         // indice capitoli
    private static volatile List<PageSection> MEM_PAGE_INDEX; // indice pagine con range capitoli

    private static final class Section {
        final String bookRaw;
        final String bookKey;
        final int chapter;
        final int start; // start contenuto (dopo header)
        final int end;   // end contenuto (prima del prossimo header)

        Section(String bookRaw, int chapter, int start, int end) {
            this.bookRaw = bookRaw;
            this.bookKey = key(bookRaw);
            this.chapter = chapter;
            this.start = start;
            this.end = end;
        }
    }

    private static final class PageSection {
        final String bookKey;
        final int startChapter;
        final int startVerse;
        final int endChapter;
        final int endVerse;
        final int start;
        final int end;

        PageSection(String bookRaw, int startChapter, int startVerse, int endChapter, int endVerse, int start, int end) {
            this.bookKey = key(bookRaw);
            this.startChapter = startChapter;
            this.startVerse = startVerse;
            this.endChapter = endChapter;
            this.endVerse = endVerse;
            this.start = start;
            this.end = end;
        }
    }

    /** API: cerca range di versetti */
    public static String findVerseRange(Context ctx, String userBook, int chapter, int vIn, int vFin) throws IOException {
        if (vFin < vIn) vFin = vIn;

        String text = getText(ctx);
        List<Section> index = getIndex(text);
        List<PageSection> pageIndex = getPageIndex(text);

        // prova match con più “candidati” del titolo (es. "1 Samuele" -> "Primo libro di Samuele")
        String[] candidates = bookCandidates(userBook);
        Section sec = null;
        for (String cand : candidates) {
            String k = key(cand);
            sec = findSection(index, k, chapter);
            if (sec != null) break;
        }
        if (sec == null) {
            DebugLog.d(ctx, TAG, "Header capitolo non trovato. Fallback page-range per: " + userBook + " " + chapter + ":" + vIn + "-" + vFin);
            // fallback: usa header pagina con range (es. RIVELAZIONE 7:15-9:5)
            for (String cand : candidates) {
                PageSection ps = findPageSection(pageIndex, key(cand), chapter);
                if (ps != null) {
                    DebugLog.d(ctx, TAG, "Page-range match: cand='" + cand + "' range=" + ps.startChapter + ":" + ps.startVerse + "-" + ps.endChapter + ":" + ps.endVerse);
                    String pageChunk = text.substring(ps.start, ps.end);
                    String fromPage = extractVersesFromPageChunk(pageChunk, chapter, vIn, vFin, ps.startChapter, ps.startVerse);
                    if (!fromPage.startsWith("Offline:")) {
                        DebugLog.d(ctx, TAG, "Fallback OK per " + userBook + " " + chapter + ":" + vIn + "-" + vFin);
                        return fromPage;
                    }
                    DebugLog.d(ctx, TAG, "Fallback page-range senza output utile: " + fromPage);
                }
            }
            DebugLog.w(ctx, TAG, "Nessun page-range utile trovato per " + userBook + " " + chapter);
            return "Offline: capitolo non trovato nel PDF (" + userBook + " " + chapter + ")";
        }

        String chunk = text.substring(sec.start, sec.end);
        return extractVersesFromChunk(chunk, chapter, vIn, vFin);
    }

    private static String extractVersesFromPageChunk(String chunk, int targetChapter, int vIn, int vFin, int startChapter, int startVerse) {
        chunk = sanitizeChunk(chunk);

        List<int[]> marks = new ArrayList<>(); // [verseNum, numStart, afterNum]
        Matcher m = VERSE_MARK.matcher(chunk);
        while (m.find()) {
            int num = Integer.parseInt(m.group(2));
            marks.add(new int[]{num, m.start(2), m.end(2)});
        }

        if (marks.isEmpty()) {
            return "Offline: marker versetti non trovati (capitolo " + targetChapter + ")";
        }

        StringBuilder out = new StringBuilder();

        // Se il range pagina attraversa capitoli (es. 20:5-21:8), prova un ancoraggio esplicito
        // sul numero capitolo in testa (schema: "21 ..." seguito da marker versetto 2).
        int anchorIndex = findChapterAnchorIndex(marks, targetChapter);
        if (anchorIndex >= 0) {
            for (int i = anchorIndex; i < marks.size(); i++) {
                int verse = marks.get(i)[0];
                int contentStart = marks.get(i)[2];
                int contentEnd = (i + 1 < marks.size()) ? marks.get(i + 1)[1] : chunk.length();

                if (verse > vFin) break;
                if (verse >= vIn && verse <= vFin) {
                    String body = cleanVerseText(chunk.substring(contentStart, contentEnd), targetChapter, verse);
                    out.append(verse).append(" ").append(body).append(" ");
                }
            }

            String anchored = out.toString().trim();
            if (!anchored.isEmpty()) return anchored;
            out.setLength(0);
        }

        int currentChapter = startChapter;
        int prevVerse = Math.max(0, startVerse - 1);

        for (int i = 0; i < marks.size(); i++) {
            int verse = marks.get(i)[0];
            int contentStart = marks.get(i)[2];
            int contentEnd = (i + 1 < marks.size()) ? marks.get(i + 1)[1] : chunk.length();

            // quando la numerazione riparte (anche con capitoli brevi), passa al capitolo successivo
            if (i > 0 && isLikelyRestart(prevVerse, verse)) {
                currentChapter++;
            }
            prevVerse = verse;

            if (currentChapter == targetChapter && verse >= vIn && verse <= vFin) {
                String body = cleanVerseText(chunk.substring(contentStart, contentEnd), targetChapter, verse);
                out.append(verse).append(" ").append(body).append(" ");
            }

            if (currentChapter > targetChapter) break;
        }

        String res = out.toString().trim();
        if (res.isEmpty()) {
            return "Offline: versetti non trovati (" + targetChapter + ":" + vIn + "-" + vFin + ")";
        }
        return res;
    }

    private static int findChapterAnchorIndex(List<int[]> marks, int targetChapter) {
        if (marks.size() < 2) return -1;
        for (int i = 0; i + 1 < marks.size(); i++) {
            int cur = marks.get(i)[0];
            int next = marks.get(i + 1)[0];
            if (cur == targetChapter && next == 2) {
                return i + 1; // parte dal marker del versetto 2 (v1 implicito tra cap e 2)
            }
        }
        return -1;
    }

    // -----------------------------
    // Core parsing
    // -----------------------------

    private static String extractVersesFromChunk(String chunk, int chapter, int vIn, int vFin) {
        chunk = sanitizeChunk(chunk);

        // raccogli marker (posizioni + numero)
        List<int[]> marks = new ArrayList<>(); // [verseNum, numStart, afterNum]
        Matcher m = VERSE_MARK.matcher(chunk);
        while (m.find()) {
            int num = Integer.parseInt(m.group(2));
            int numStart = m.start(2);
            int afterNum = m.end(2);
            marks.add(new int[]{num, numStart, afterNum});
        }

        if (marks.isEmpty()) {
            return "Offline: marker versetti non trovati (capitolo " + chapter + ")";
        }

        StringBuilder out = new StringBuilder();

        int i = 0;
        int prevVerse = -1;

        // Caso speciale: il primo numero è il NUMERO CAPITOLO (es. "2  Così...")
        // Se dopo arriva un "2 ..." (versetto 2), allora quello iniziale è capitolo -> significa versetto 1 “implicito”.
        if (marks.size() >= 2 && marks.get(0)[0] == chapter && marks.get(1)[0] == 2) {
            int v1Start = marks.get(0)[2];
            int v1End = marks.get(1)[1];
            String v1 = cleanVerseText(chunk.substring(v1Start, v1End), chapter, 1);
            if (1 >= vIn && 1 <= vFin) {
                out.append("1 ").append(v1).append(" ");
            }
            i = 1; // riparti dal marker del versetto 2
        }

        // parsing standard: ogni marker delimita il testo fino al prossimo marker
        for (; i < marks.size(); i++) {
            int verse = marks.get(i)[0];

            if (prevVerse > 0 && isLikelyRestart(prevVerse, verse)) {
                break;
            }

            int contentStart = marks.get(i)[2];
            int contentEnd = (i + 1 < marks.size()) ? marks.get(i + 1)[1] : chunk.length();

            // stop se abbiamo già finito
            if (verse > vFin) break;

            if (verse >= vIn && verse <= vFin) {
                String body = cleanVerseText(chunk.substring(contentStart, contentEnd), chapter, verse);
                out.append(verse).append(" ").append(body).append(" ");
            }
            prevVerse = verse;
        }

        String res = out.toString().trim();
        if (res.isEmpty()) {
            return "Offline: versetti non trovati (" + chapter + ":" + vIn + "-" + vFin + ")";
        }
        return res;
    }

    private static String cleanVerseText(String s, int chapter, int verse) {
        // toglie marcatori note tipo + e * e compatta spazi
        s = s.replace('\u00A0', ' ').replace('\u202F', ' ');
        s = PAGE_NOTE.matcher(s).replaceAll(" ");
        s = SIDE_REFERENCE.matcher(s).replaceAll(" ");
        s = s.replaceAll("[\\*+]+", "");
        s = s.replaceAll("\\s+", " ").trim();

        if (verse == 1) {
            s = s.replaceFirst("^" + chapter + "\\s+(?=\\p{L})", "");
        }

        s = s.replaceFirst("^(?i)sommario\\s+", "");
        s = s.replaceFirst("^(?i)(genesi|esodo|levitico|numeri|deuteronomio|rivelazione|apocalisse)\\s+", "");
        return s;
    }

    private static String sanitizeChunk(String s) {
        s = normalizeSpaces(s);
        s = FRONT_MATTER_LINE.matcher(s).replaceAll("\n");
        s = SUMMARY_BLOCK.matcher(s).replaceAll("\n");
        s = PAGE_HEADER_BOOK_TO_SUMMARY.matcher(s).replaceAll("\n");
        s = PAGE_HEADER_SUMMARY_TO.matcher(s).replaceAll("\n");
        s = PAGE_HEADER_LINE.matcher(s).replaceAll("\n");
        s = PAGE_HEADER_CROSS_BOOK.matcher(s).replaceAll("\n");
        s = PAGE_NOTE.matcher(s).replaceAll(" ");
        s = CAP_MARKER.matcher(s).replaceAll(" ");
        s = PAGE_NUMBER_LINE.matcher(s).replaceAll("\n");
        s = COLUMN_MARKER_LINE.matcher(s).replaceAll("\n");
        s = DENSE_REFERENCE_LINE.matcher(s).replaceAll("\n");
        s = SINGLE_REFERENCE_LINE.matcher(s).replaceAll("\n");
        s = TOC_ITEM_LINE.matcher(s).replaceAll("\n");
        s = NUMBERED_TOC_ITEM_LINE.matcher(s).replaceAll("\n");
        s = FOOTNOTE_GLOSS.matcher(s).replaceAll(" ");
        s = FOOTNOTE_REFERENCE_LINE.matcher(s).replaceAll("\n");
        s = FOOTNOTE_CLUSTER_LINE.matcher(s).replaceAll("\n");
        s = NOISE_SYMBOL_LINE.matcher(s).replaceAll("\n");
        s = SIDE_REFERENCE.matcher(s).replaceAll(" ");
        s = INLINE_REFERENCE_TAIL.matcher(s).replaceAll(" ");
        s = s.replaceAll("(?m)^\\s*[\\|¦]\\s*$", "");
        s = s.replaceAll("\\n{3,}", "\n\n");
        return s;
    }

    private static boolean isLikelyRestart(int prevVerse, int currentVerse) {
        return prevVerse >= 6 && currentVerse <= 2 && (prevVerse - currentVerse) >= 4;
    }

    private static String normalizeSpaces(String s) {
        s = s.replace('\u00A0', ' ').replace('\u202F', ' ');
        return s;
    }

    // -----------------------------
    // Index / text cache
    // -----------------------------

    private static String getText(Context ctx) throws IOException {
        if (MEM_TEXT != null) return MEM_TEXT;
        synchronized (NwtOfflineRepository.class) {
            if (MEM_TEXT == null) {
                // estrai dal PDF (consigliato: una volta e poi cache su file, ma qui resto semplice)
                MEM_TEXT = PdfParser.extractAllText(ctx);
            }
            return MEM_TEXT;
        }
    }

    private static List<Section> getIndex(String text) {
        if (MEM_INDEX != null) return MEM_INDEX;

        synchronized (NwtOfflineRepository.class) {
            if (MEM_INDEX != null) return MEM_INDEX;

            List<int[]> headers = new ArrayList<>(); // [bookStart, headerEnd, chapter]
            List<String> books = new ArrayList<>();

            Matcher hm = HEADER.matcher(text);
            while (hm.find()) {
                String book = hm.group(1).trim();
                int chap = Integer.parseInt(hm.group(2));
                int headerEnd = hm.end();    // contenuto subito dopo l’header
                int headerStart = hm.start();
                books.add(book);
                headers.add(new int[]{headerStart, headerEnd, chap});
            }

            List<Section> idx = new ArrayList<>();
            for (int i = 0; i < headers.size(); i++) {
                int[] cur = headers.get(i);
                int end = (i + 1 < headers.size()) ? headers.get(i + 1)[0] : text.length();
                idx.add(new Section(books.get(i), cur[2], cur[1], end));
            }

            MEM_INDEX = idx;
            return MEM_INDEX;
        }
    }

    private static List<PageSection> getPageIndex(String text) {
        if (MEM_PAGE_INDEX != null) return MEM_PAGE_INDEX;

        synchronized (NwtOfflineRepository.class) {
            if (MEM_PAGE_INDEX != null) return MEM_PAGE_INDEX;

            List<PageSection> idx = new ArrayList<>();
            Matcher pm = PAGE_RANGE_HEADER.matcher(text);

            List<int[]> ranges = new ArrayList<>(); // [headerStart, headerEnd, sc, sv, ec, ev]
            List<String> books = new ArrayList<>();

            while (pm.find()) {
                books.add(pm.group(1).trim());
                ranges.add(new int[]{
                        pm.start(),
                        pm.end(),
                        Integer.parseInt(pm.group(2)),
                        Integer.parseInt(pm.group(3)),
                        Integer.parseInt(pm.group(4)),
                        Integer.parseInt(pm.group(5))
                });
            }

            for (int i = 0; i < ranges.size(); i++) {
                int[] cur = ranges.get(i);
                int end = (i + 1 < ranges.size()) ? ranges.get(i + 1)[0] : text.length();
                idx.add(new PageSection(
                        books.get(i),
                        cur[2],
                        cur[3],
                        cur[4],
                        cur[5],
                        cur[1],
                        end
                ));
            }

            MEM_PAGE_INDEX = idx;
            return MEM_PAGE_INDEX;
        }
    }

    private static Section findSection(List<Section> index, String bookKey, int chapter) {
        for (Section s : index) {
            if (s.chapter == chapter && s.bookKey.equals(bookKey)) return s;
        }
        return null;
    }

    private static PageSection findPageSection(List<PageSection> index, String bookKey, int chapter) {
        for (PageSection s : index) {
            if (!s.bookKey.equals(bookKey)) continue;
            if (chapter < s.startChapter || chapter > s.endChapter) continue;
            return s;
        }
        return null;
    }

    // -----------------------------
    // Book title mapping / normalization
    // -----------------------------

    private static String key(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase(Locale.ITALIAN);

        // rimuovi diacritici (È -> E)
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");

        // tieni lettere/numeri/spazi
        s = s.replaceAll("[^a-z0-9 ]+", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private static String[] bookCandidates(String userBook) {
        if (userBook == null) return new String[]{""};
        String b = userBook.trim();

        List<String> out = new ArrayList<>();
        out.add(b);

        // prova mapping per "1 Samuele" / "I Samuele" ecc.
        int n = leadingNumber(b);
        String tail = stripLeadingNumber(b);

        if (n >= 1 && n <= 3) {
            String tailKey = key(tail);

            if (tailKey.equals("samuele")) {
                if (n == 1) out.add("Primo libro di Samuele");
                if (n == 2) out.add("Secondo libro di Samuele");
            }

            if (tailKey.equals("giovanni")) {
                if (n == 1) out.add("Prima lettera di Giovanni");
                if (n == 2) out.add("Seconda lettera di Giovanni");
                if (n == 3) out.add("Terza lettera di Giovanni");
            }
        }

        String k = key(b);
        if (k.equals("rivelazione")) out.add("Apocalisse");
        if (k.equals("apocalisse")) out.add("Rivelazione");

        // aggiungi anche una versione “solo lettere” (utile se l’utente scrive con apostrofi/accents)
        out.add(key(b));

        // rimuovi duplicati preservando ordine
        LinkedHashSet<String> uniq = new LinkedHashSet<>(out);
        return uniq.toArray(new String[0]);
    }

    private static int leadingNumber(String s) {
        s = s.trim();
        // digit
        if (s.matches("^[1-3].*")) return Character.getNumericValue(s.charAt(0));

        // roman numerals semplici
        String up = s.toUpperCase(Locale.ITALIAN);
        if (up.startsWith("I ")) return 1;
        if (up.startsWith("II ")) return 2;
        if (up.startsWith("III ")) return 3;

        return -1;
    }

    private static String stripLeadingNumber(String s) {
        s = s.trim();
        if (s.matches("^[1-3]\\s+.*")) return s.substring(1).trim();
        String up = s.toUpperCase(Locale.ITALIAN);
        if (up.startsWith("III ")) return s.substring(4).trim();
        if (up.startsWith("II ")) return s.substring(3).trim();
        if (up.startsWith("I ")) return s.substring(2).trim();
        return s;
    }

    /** Se aggiorni il PDF, chiama questo per ricostruire testo+indice */
    public static void invalidate() {
        synchronized (NwtOfflineRepository.class) {
            MEM_TEXT = null;
            MEM_INDEX = null;
            MEM_PAGE_INDEX = null;
        }
    }
}

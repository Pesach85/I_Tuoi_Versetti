package com.testing.ituoiversetti;

import android.content.Context;

import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NwtOfflineRepository {

    // Header: 2 righe -> "Libro" + "cap:inizio-fine"
    private static final Pattern HEADER = Pattern.compile(
            "(?m)^\\s*(\\S.*\\S)\\s*\\R\\s*(\\d+):(\\d+)-(\\d+)\\s*$"
    );

    // Marker versetto: SOLO se preceduto da inizio riga o punteggiatura forte (evita "10 figli")
    // Esempi validi: "\n4  Poi..."  oppure ".+ 4  Poi..."
    private static final Pattern VERSE_MARK = Pattern.compile(
            "(?m)(^|[\\n\\r\\.\\!\\?\\+;:”“\"»«])\\s*([1-9]\\d{0,2})\\s+(?![º°])"
    );

    private static volatile String MEM_TEXT;                 // testo estratto normalizzato
    private static volatile List<Section> MEM_INDEX;         // indice capitoli

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

    /** API: cerca range di versetti */
    public static String findVerseRange(Context ctx, String userBook, int chapter, int vIn, int vFin) throws IOException {
        if (vFin < vIn) vFin = vIn;

        String text = getText(ctx);
        List<Section> index = getIndex(text);

        // prova match con più “candidati” del titolo (es. "1 Samuele" -> "Primo libro di Samuele")
        String[] candidates = bookCandidates(userBook);
        Section sec = null;
        for (String cand : candidates) {
            String k = key(cand);
            sec = findSection(index, k, chapter);
            if (sec != null) break;
        }
        if (sec == null) {
            return "Offline: capitolo non trovato nel PDF (" + userBook + " " + chapter + ")";
        }

        String chunk = text.substring(sec.start, sec.end);
        return extractVersesFromChunk(chunk, chapter, vIn, vFin);
    }

    // -----------------------------
    // Core parsing
    // -----------------------------

    private static String extractVersesFromChunk(String chunk, int chapter, int vIn, int vFin) {
        chunk = normalizeSpaces(chunk);

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

        // Caso speciale: il primo numero è il NUMERO CAPITOLO (es. "2  Così...")
        // Se dopo arriva un "2 ..." (versetto 2), allora quello iniziale è capitolo -> significa versetto 1 “implicito”.
        if (marks.size() >= 2 && marks.get(0)[0] == chapter && marks.get(1)[0] == 2) {
            int v1Start = marks.get(0)[2];
            int v1End = marks.get(1)[1];
            String v1 = cleanVerseText(chunk.substring(v1Start, v1End));
            if (1 >= vIn && 1 <= vFin) {
                out.append("1 ").append(v1).append(" ");
            }
            i = 1; // riparti dal marker del versetto 2
        }

        // parsing standard: ogni marker delimita il testo fino al prossimo marker
        for (; i < marks.size(); i++) {
            int verse = marks.get(i)[0];
            int contentStart = marks.get(i)[2];
            int contentEnd = (i + 1 < marks.size()) ? marks.get(i + 1)[1] : chunk.length();

            // stop se abbiamo già finito
            if (verse > vFin) break;

            if (verse >= vIn && verse <= vFin) {
                String body = cleanVerseText(chunk.substring(contentStart, contentEnd));
                out.append(verse).append(" ").append(body).append(" ");
            }
        }

        String res = out.toString().trim();
        if (res.isEmpty()) {
            return "Offline: versetti non trovati (" + chapter + ":" + vIn + "-" + vFin + ")";
        }
        return res;
    }

    private static String cleanVerseText(String s) {
        // toglie marcatori note tipo + e * e compatta spazi
        s = s.replace('\u00A0', ' ').replace('\u202F', ' ');
        s = s.replaceAll("[\\*+]+", "");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
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

    private static Section findSection(List<Section> index, String bookKey, int chapter) {
        for (Section s : index) {
            if (s.chapter == chapter && s.bookKey.equals(bookKey)) return s;
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
        }
    }
}

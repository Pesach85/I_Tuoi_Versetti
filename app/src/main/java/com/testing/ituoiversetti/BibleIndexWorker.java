package com.testing.ituoiversetti;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BibleIndexWorker extends Worker {

    private static final String PREFS = "bible_index";
    private static final String KEY_FINGERPRINT = "pdf_fp";

    // Header: "Libro" + "cap:inizio-fine"
    private static final Pattern HEADER = Pattern.compile(
            "(?m)^\\s*(\\S.*\\S)\\s*\\R\\s*(\\d+):(\\d+)-(\\d+)\\s*$"
    );

    // Marker versetto: inizio riga o punteggiatura forte prima del numero (riduce falsi "10 figli")
    private static final Pattern VERSE_MARK = Pattern.compile(
            "(?m)(^|[\\n\\r\\.\\!\\?\\+;:”“\"»«])\\s*([1-9]\\d{0,2})\\s+(?![º°])"
    );

    public BibleIndexWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context ctx = getApplicationContext();

            File pdf = PdfParser.getReadablePdfFile(ctx);
            String fp = pdf.length() + ":" + pdf.lastModified();

            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String doneFp = sp.getString(KEY_FINGERPRINT, "");
            if (fp.equals(doneFp)) {
                return Result.success(); // già indicizzato per questo PDF
            }

            String text = normalize(PdfParser.extractAllText(ctx));

            // costruisci lista header
            List<Hdr> headers = new ArrayList<>();
            Matcher hm = HEADER.matcher(text);
            while (hm.find()) {
                String bookRaw = hm.group(1).trim();
                int chap = Integer.parseInt(hm.group(2));
                headers.add(new Hdr(bookRaw, chap, hm.end(), hm.start()));
            }
            if (headers.isEmpty()) return Result.retry();

            // end = start del prossimo header
            for (int i = 0; i < headers.size(); i++) {
                int end = (i + 1 < headers.size()) ? headers.get(i + 1).headerStart : text.length();
                headers.get(i).end = end;
            }

            BibleDb db = BibleDb.get(ctx);
            VerseDao dao = db.verseDao();

            // rebuild completo (semplice e sicuro)
            dao.clearAll();

            List<VerseEntity> batch = new ArrayList<>(500);

            for (Hdr h : headers) {
                String bookKey = BookNameUtil.key(h.bookRaw);
                String chunk = text.substring(h.contentStart, h.end);

                parseChapterInto(batch, bookKey, h.chapter, chunk);

                if (batch.size() >= 500) {
                    dao.upsertAll(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                dao.upsertAll(batch);
                batch.clear();
            }

            sp.edit().putString(KEY_FINGERPRINT, fp).apply();
            NwtOfflineRepository.invalidate(); // reset cache in RAM (se usi ancora quella)
            return Result.success();

        } catch (Exception e) {
            return Result.retry();
        }
    }

    private static void parseChapterInto(List<VerseEntity> out, String bookKey, int chapter, String chunk) {
        chunk = normalize(chunk);

        // raccogli marker
        List<Mark> marks = new ArrayList<>();
        Matcher m = VERSE_MARK.matcher(chunk);
        while (m.find()) {
            int v = Integer.parseInt(m.group(2));
            marks.add(new Mark(v, m.start(2), m.end(2)));
        }
        if (marks.isEmpty()) return;

        // Caso speciale: alcuni capitoli iniziano con "2  ..." (numero capitolo) e il versetto 1 è implicito.
        // Heuristica robusta: se chapter != 1 e first==chapter e second==2 => versetto 1 è tra first e second.
        int startIdx = 0;
        if (chapter != 1 && marks.size() >= 2 && marks.get(0).v == chapter && marks.get(1).v == 2) {
            String v1 = clean(chunk.substring(marks.get(0).afterNum, marks.get(1).numStart));
            if (!v1.isEmpty()) out.add(row(bookKey, chapter, 1, v1));
            startIdx = 1;
        }

        for (int i = startIdx; i < marks.size(); i++) {
            int v = marks.get(i).v;
            int cs = marks.get(i).afterNum;
            int ce = (i + 1 < marks.size()) ? marks.get(i + 1).numStart : chunk.length();
            String body = clean(chunk.substring(cs, ce));
            if (!body.isEmpty()) out.add(row(bookKey, chapter, v, body));
        }
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

    private static String clean(String s) {
        s = normalize(s);
        s = s.replaceAll("[\\*+]+", "");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private static final class Hdr {
        final String bookRaw;
        final int chapter;
        final int contentStart;
        final int headerStart;
        int end;

        Hdr(String bookRaw, int chapter, int contentStart, int headerStart) {
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


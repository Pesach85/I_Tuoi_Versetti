package com.testing.ituoiversetti;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BibleIndexWorker extends Worker {

    private static final String PREFS = "bible_index";
    private static final String KEY_FP = "pdf_fp";

    // Header: "Libro" + "cap:inizio-fine"
    private static final Pattern HEADER = Pattern.compile(
        "(?m)^\\s*(\\S.*\\S)\\s*(?:\\R|\\s+)\\s*(\\d+):(\\d+)[\\-–—](\\d+)\\s*$"
    );


    // Marker versetto: start riga o punteggiatura forte prima del numero
    private static final Pattern VERSE_MARK = Pattern.compile(
            "(?m)(^|[\\n\\r\\.\\!\\?\\+;:”“\"»«])\\s*([1-9]\\d{0,2})\\s+(?![º°])"
    );

    public BibleIndexWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        setProgressAsync(new Data.Builder().putInt("pct", 1).putString("stage","Apro PDF").build());
        try {
            Context ctx = getApplicationContext();

            // fingerprint per non reindicizzare inutilmente
            File pdf = PdfParser.getReadablePdfFile(ctx);
            String fp = pdf.length() + ":" + pdf.lastModified();

            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String done = sp.getString(KEY_FP, "");
            if (fp.equals(done)) {
                setProgressAsync(new Data.Builder().putInt("pct", 100).build());
                return Result.success();
            }

            // estrai testo (pesante ma una tantum)
            setProgressAsync(new Data.Builder().putInt("pct", 5).putString("stage","Estraggo testo PDF (può essere lento)").build());
            String text = PdfParser.extractAllText(ctx);
            text = normalize(text);
            setProgressAsync(new Data.Builder().putInt("pct", 15).putString("stage","Cerco intestazioni (Libro + cap:da-a)").build());

            // trova headers
            List<Hdr> headers = new ArrayList<>();
            Matcher hm = HEADER.matcher(text);
            while (hm.find()) {
                String bookRaw = hm.group(1).trim();
                int chap = Integer.parseInt(hm.group(2));
                headers.add(new Hdr(bookRaw, chap, hm.end(), hm.start()));
            }
            if (headers.isEmpty()) {
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

            BibleDb db = BibleDb.get(ctx);
            VerseDao dao = db.verseDao();

            // rebuild completo
            dao.clearAll();

            List<VerseEntity> batch = new ArrayList<>(600);

            for (int i = 0; i < headers.size(); i++) {
                Hdr h = headers.get(i);

                int pct = (int) (((i + 1) * 100L) / headers.size());
                setProgressAsync(new Data.Builder().putInt("pct", pct).build());

                String bookKey = BookNameUtil.key(h.bookRaw);
                String chunk = text.substring(h.contentStart, h.end);

                parseChapterInto(batch, bookKey, h.chapter, chunk);

                if (batch.size() >= 600) {
                    dao.upsertAll(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                dao.upsertAll(batch);
                batch.clear();
            }

            sp.edit().putString(KEY_FP, fp).apply();
            NwtOfflineRepository.invalidate(); // se la usi ancora
            setProgressAsync(new Data.Builder().putInt("pct", 100).build());
            return Result.success();

        } catch (Exception e) {
            return Result.retry();
        }
    }

    private static void parseChapterInto(List<VerseEntity> out, String bookKey, int chapter, String chunk) {
        chunk = normalize(chunk);

        List<Mark> marks = new ArrayList<>();
        Matcher m = VERSE_MARK.matcher(chunk);
        while (m.find()) {
            int v = Integer.parseInt(m.group(2));
            marks.add(new Mark(v, m.start(2), m.end(2)));
        }
        if (marks.isEmpty()) return;

        int startIdx = 0;

        // Caso Genesi 2: "2  ..." (numero capitolo) e v1 implicito
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

package com.testing.ituoiversetti;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public final class WolVerseFetcher {

    private WolVerseFetcher() {}

    private static final int MAX_PARTS = 7;

    public static List<VerseEntity> fetchRange(Context ctx,
                                               String bookTitle,
                                               int chapter,
                                               int fromV,
                                               int toV) throws Exception {

        if (toV < fromV) { int t = fromV; fromV = toV; toV = t; }

        // usa la tua mappatura (1..66)
        int bookNum = new Bibbia().convertLibro(bookTitle);

        String url = "https://wol.jw.org/it/wol/b/r6/lp-i/nwtsty"
                + "/" + bookNum + "/" + chapter
                + "#study=discover&v="
                + bookNum + ":" + chapter + ":" + fromV
                + "-" + bookNum + ":" + chapter + ":" + toV;

        String ua = System.getProperty("http.agent");
        if (ua == null || ua.trim().isEmpty()) ua = "ituoiversetti/1.0";

        Connection conn = Jsoup.connect(url)
                .userAgent(ua)
                .referrer("https://wol.jw.org/it/wol/binav/r6/lp-i/nwtsty")
                .timeout(20000)
                .maxBodySize(0)
                .followRedirects(true);

        Document doc = conn.get();

        String bookKey = BookNameUtil.key(bookTitle);
        List<VerseEntity> out = new ArrayList<>();

        for (int v = fromV; v <= toV; v++) {
            StringBuilder sb = new StringBuilder();

            for (int part = 1; part <= MAX_PARTS; part++) {
                String id = "v" + bookNum + "-" + chapter + "-" + v + "-" + part;
                Element el = doc.getElementById(id);
                if (el == null) break;
                sb.append(el.text()).append(' ');
            }

            String text = sb.toString()
                    .replaceAll("[\\*+]+", "")
                    .replaceAll("\\s+", " ")
                    .trim();
                text = stripLeadingVerseNumber(text, v);

            if (!text.isEmpty()) {
                VerseEntity e = new VerseEntity();
                e.bookKey = bookKey;
                e.chapter = chapter;
                e.verse = v;
                e.text = text;
                out.add(e);
            }
        }

        return out;
    }

    private static String stripLeadingVerseNumber(String text, int verse) {
        if (text == null) return "";

        int idx = 0;
        int n = text.length();
        while (idx < n && Character.isWhitespace(text.charAt(idx))) idx++;

        String verseStr = String.valueOf(verse);
        int end = idx + verseStr.length();
        if (end > n || !text.regionMatches(idx, verseStr, 0, verseStr.length())) {
            return text.trim();
        }

        if (end < n) {
            char c = text.charAt(end);
            if (!Character.isWhitespace(c)) return text.trim();
        }

        while (end < n && Character.isWhitespace(text.charAt(end))) end++;
        return text.substring(end).trim();
    }
}

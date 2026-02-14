package com.testing.ituoiversetti;

import java.text.Normalizer;
import java.util.*;

public final class BookNameUtil {

    private BookNameUtil() {}

    public static String key(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase(Locale.ITALIAN);
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.replaceAll("[^a-z0-9 ]+", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    public static List<String> candidateKeys(String userBook) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(key(userBook));

        int n = leadingNumber(userBook);
        String tail = stripLeadingNumber(userBook);

        String tailKey = key(tail);
        if (n >= 1 && n <= 3) {
            if (tailKey.equals("samuele")) {
                if (n == 1) out.add(key("Primo libro di Samuele"));
                if (n == 2) out.add(key("Secondo libro di Samuele"));
            }
            if (tailKey.equals("giovanni")) {
                if (n == 1) out.add(key("Prima lettera di Giovanni"));
                if (n == 2) out.add(key("Seconda lettera di Giovanni"));
                if (n == 3) out.add(key("Terza lettera di Giovanni"));
            }
        }

        // aggiungi anche l'originale “come titolo” (a volte InputCheck già lo converte)
        out.add(key(userBook));

        return new ArrayList<>(out);
    }

    private static int leadingNumber(String s) {
        if (s == null) return -1;
        s = s.trim();
        if (s.matches("^[1-3].*")) return Character.getNumericValue(s.charAt(0));
        String up = s.toUpperCase(Locale.ITALIAN);
        if (up.startsWith("III ")) return 3;
        if (up.startsWith("II ")) return 2;
        if (up.startsWith("I ")) return 1;
        return -1;
    }

    private static String stripLeadingNumber(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.matches("^[1-3]\\s+.*")) return s.substring(1).trim();
        String up = s.toUpperCase(Locale.ITALIAN);
        if (up.startsWith("III ")) return s.substring(4).trim();
        if (up.startsWith("II ")) return s.substring(3).trim();
        if (up.startsWith("I ")) return s.substring(2).trim();
        return s;
    }
}


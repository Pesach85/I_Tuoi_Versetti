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

    public static String toStandardBookName(String rawFound, Map<String, String> keyToStandard) {
        if (rawFound == null) return null;
        for (String k : candidateKeys(rawFound)) {
            String std = keyToStandard.get(k);
            if (std != null && !std.trim().isEmpty()) return std;
        }
        return null;
    }

    public static Map<String, String> buildKeyToStandardMap(Collection<String> standardBookNames) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (standardBookNames == null) return out;

        for (String std : standardBookNames) {
            if (std == null) continue;
            String stdTrim = std.trim();
            if (stdTrim.isEmpty()) continue;

            out.putIfAbsent(key(stdTrim), stdTrim);
            for (String k : candidateKeys(stdTrim)) out.putIfAbsent(k, stdTrim);
        }
        return out;
    }

    public static List<String> candidateKeys(String userBook) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (userBook == null) return new ArrayList<>(out);

        out.add(key(userBook));

        int n = leadingNumberOrOrdinal(userBook);
        String tail = stripLeadingNumberOrOrdinal(userBook);

        // normalizza il “tail” togliendo parole comuni nei titoli lunghi
        String tailCore = tail.replaceAll("(?i)\\b(lettera|libro|ai|a|di|dei|degli|delle|del|della|agli|alle|allo|alla|al|ad)\\b", " ");
        String tailKey = key(tailCore);

        if (n >= 1 && n <= 3) {

            if (tailKey.equals("samuele")) {
                if (n == 1) {
                    out.add(key("Primo libro di Samuele"));
                    out.add(key("1 Samuele"));
                }
                if (n == 2) {
                    out.add(key("Secondo libro di Samuele"));
                    out.add(key("2 Samuele"));
                }
            }

            if (tailKey.equals("re")) {
                if (n == 1) {
                    out.add(key("Primo libro dei Re"));
                    out.add(key("1 Re"));
                }
                if (n == 2) {
                    out.add(key("Secondo libro dei Re"));
                    out.add(key("2 Re"));
                }
            }

            if (tailKey.equals("cronache")) {
                if (n == 1) {
                    out.add(key("Primo libro delle Cronache"));
                    out.add(key("1 Cronache"));
                }
                if (n == 2) {
                    out.add(key("Secondo libro delle Cronache"));
                    out.add(key("2 Cronache"));
                }
            }

            if (tailKey.equals("giovanni")) {
                if (n == 1) {
                    out.add(key("Prima lettera di Giovanni"));
                    out.add(key("1 Giovanni"));
                }
                if (n == 2) {
                    out.add(key("Seconda lettera di Giovanni"));
                    out.add(key("2 Giovanni"));
                }
                if (n == 3) {
                    out.add(key("Terza lettera di Giovanni"));
                    out.add(key("3 Giovanni"));
                }
            }

            if (tailKey.equals("corinti") || tailKey.equals("corinzi")) {
                if (n == 1) {
                    out.add(key("Prima lettera ai Corinti"));
                    out.add(key("Prima lettera ai Corinzi"));
                    out.add(key("Prima Lettera ai Corinti"));
                    out.add(key("Prima Lettera ai Corinzi"));
                    out.add(key("1 Corinti"));
                }
                if (n == 2) {
                    out.add(key("Seconda lettera ai Corinti"));
                    out.add(key("Seconda lettera ai Corinzi"));
                    out.add(key("Seconda Lettera ai Corinti"));
                    out.add(key("Seconda Lettera ai Corinzi"));
                    out.add(key("2 Corinti"));
                }
            }

            if (tailKey.equals("pietro")) {
                if (n == 1) out.add(key("Prima lettera di Pietro"));
                if (n == 2) out.add(key("Seconda lettera di Pietro"));
            }

            if (tailKey.equals("tessalonicesi")) {
                if (n == 1) out.add(key("Prima lettera ai Tessalonicesi"));
                if (n == 2) out.add(key("Seconda lettera ai Tessalonicesi"));
            }

            if (tailKey.equals("timoteo")) {
                if (n == 1) out.add(key("Prima lettera a Timoteo"));
                if (n == 2) out.add(key("Seconda lettera a Timoteo"));
            }

            if (tailKey.equals("romani")) {
                out.add(key("Lettera ai Romani"));
                out.add(key("Romani"));
            }
        }

        return new ArrayList<>(out);
    }

    private static int leadingNumberOrOrdinal(String s) {
        if (s == null) return -1;
        s = s.trim();
        if (s.isEmpty()) return -1;

        if (s.matches("^[1-3].*")) return Character.getNumericValue(s.charAt(0));

        String up = s.toUpperCase(Locale.ITALIAN);
        if (up.startsWith("III")) return 3;
        if (up.startsWith("II")) return 2;
        if (up.startsWith("I")) return 1;

        // ordinale scritto
        if (s.matches("(?i)^(prima|primo)\\b.*")) return 1;
        if (s.matches("(?i)^(seconda|secondo)\\b.*")) return 2;
        if (s.matches("(?i)^(terza|terzo)\\b.*")) return 3;

        return -1;
    }

    private static String stripLeadingNumberOrOrdinal(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.isEmpty()) return "";

        if (s.matches("^[1-3].*")) return s.substring(1).trim();

        String up = s.toUpperCase(Locale.ITALIAN);
        if (up.startsWith("III")) return s.substring(3).trim();
        if (up.startsWith("II")) return s.substring(2).trim();
        if (up.startsWith("I")) return s.substring(1).trim();

        return s.replaceFirst("(?i)^(prima|primo|seconda|secondo|terza|terzo)\\b\\s*", "").trim();
    }
}
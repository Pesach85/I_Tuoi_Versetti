package com.testing.ituoiversetti;

public final class AllowedBooks {
    private static final java.util.Set<String> KEYS = build();

    private static java.util.Set<String> build() {
        java.util.HashSet<String> s = new java.util.HashSet<>();
        Bibbia b = new Bibbia();
        for (String t : b.composeBibbia()) s.add(BookNameUtil.key(t));
        // varianti “lunghe” usate nel PDF
        s.add(BookNameUtil.key("Primo libro di Samuele"));
        s.add(BookNameUtil.key("Secondo libro di Samuele"));
        s.add(BookNameUtil.key("Prima lettera di Giovanni"));
        s.add(BookNameUtil.key("Seconda lettera di Giovanni"));
        s.add(BookNameUtil.key("Terza lettera di Giovanni"));
        return s;
    }

    public static boolean contains(String key) { return KEYS.contains(key); }

    private AllowedBooks() {}
}
package com.testing.ituoiversetti;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

public class PdfParser {

    /**
     * Inizializza PDFBox per Android.
     * IMPORTANTE: Chiama questo metodo una volta all'avvio dell'app (es. in onCreate di Application o Activity)
     */
    public static void init(Context context) {
        PDFBoxResourceLoader.init(context);
    }

    /**
     * Estrae tutto il testo da un file PDF usando il percorso del file.
     *
     * @param filePath Il percorso del file PDF.
     * @return Il testo estratto come stringa.
     * @throws IOException se c'è un errore nella lettura del file.
     */
    public static String extractTextFromPdf(String filePath) throws IOException {
        PDDocument document = null;
        try {
            document = PDDocument.load(new File(filePath));
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

    /**
     * Estrae tutto il testo da un PDF usando un InputStream (utile per assets o URI).
     *
     * @param inputStream L'InputStream del file PDF.
     * @return Il testo estratto come stringa.
     * @throws IOException se c'è un errore nella lettura.
     */
    public static String extractTextFromPdf(InputStream inputStream) throws IOException {
        PDDocument document = null;
        try {
            document = PDDocument.load(inputStream);
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

    /**
     * Analizza il testo estratto per trovare frasi associate a capitoli e versetti.
     * Questo esempio assume un pattern come "Chapter 1, Verse 1: " o simile.
     *
     * @param text Il testo estratto dal PDF.
     * @return Una mappa dove la chiave è l'identificatore del versetto (es. "1:1") e il valore è la frase.
     */
    public static Map<String, String> parseText(String text) {
        Map<String, String> sentencesByVerse = new HashMap<>();
        // Questo pattern regex deve essere adattato al formato specifico del tuo PDF.
        // Cerca "Chapter X, Verse Y:" seguito dalla frase.
        Pattern pattern = Pattern.compile("Chapter (\\d+), Verse (\\d+):\\s*(.*?)(?=(Chapter \\d+, Verse \\d+)|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String chapter = matcher.group(1);
            String verse = matcher.group(2);
            String sentence = matcher.group(3).trim();
            sentencesByVerse.put(chapter + ":" + verse, sentence);
        }
        return sentencesByVerse;
    }
}
package com.testing.ituoiversetti;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

public class PdfParser {

    public static void main(String[] args) {
        String filePath = "path/to/your/pdf_file.pdf";
        try {
            String text = extractTextFromPdf(filePath);
            Map<String, String> sentencesByVerse = parseText(text);

            for (Map.Entry<String, String> entry : sentencesByVerse.entrySet()) {
                System.out.println("Verse " + entry.getKey() + ": " + entry.getValue());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extracts all text from a PDF file.
     *
     * @param filePath The path to the PDF file.
     * @return The extracted text as a single string.
     * @throws IOException if there is an error reading the file.
     */
    public static String extractTextFromPdf(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Parses the extracted text to find sentences associated with chapters and verses.
     * This example assumes a pattern like "Chapter 1, Verse 1: " or similar.
     *
     * @param text The text extracted from the PDF.
     * @return A map where the key is the verse identifier (e.g., "1:1") and the value is the sentence.
     */
    public static Map<String, String> parseText(String text) {
        Map<String, String> sentencesByVerse = new HashMap<>();
        // This regex pattern needs to be adapted to the specific format of your PDF.
        // It looks for "Chapter X, Verse Y:" followed by the sentence.
        // A more flexible pattern might be needed for different formats.
        Pattern pattern = Pattern.compile("Chapter (\\d+), Verse (\\d+):\\s*(.*?)(?=(Chapter \\d+, Verse \\d+)|$)");
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



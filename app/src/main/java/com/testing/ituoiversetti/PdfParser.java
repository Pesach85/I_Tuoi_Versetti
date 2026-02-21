package com.testing.ituoiversetti;

import android.content.Context;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;

import java.io.*;
import java.util.List;

public class PdfParser {

    private static final String PDF_NAME = "nwt_I.pdf";
    private static final int MIN_PDF_SIZE_BYTES = 1024;

    // --- 2 colonne: tuning (A5/A6-friendly) ---
    private static final boolean TWO_COLUMN_READING_ORDER = true;

    // gutter dinamico: percentuale della larghezza pagina + clamp
    private static final float CENTER_GUTTER_RATIO = 0.12f;  // 12% della larghezza
    private static final float CENTER_GUTTER_MIN_PT = 55f;   // minimo assoluto (pt PDF)
    private static final float CENTER_GUTTER_MAX_PT = 120f;  // massimo assoluto (pt PDF)

    private static final int MIN_RIGHT_COL_CHARS = 20; // su A5 la destra può avere meno testo

    // --- 2 colonne: tuning ---

    // “taglio” centrale per evitare colonna note (in punti PDF). 24-36 di solito ok.
    private static final float CENTER_GUTTER_PT = 70f;

    // se il testo della colonna destra è troppo corto, lo considero pagina mono-colonna

    public static void init(Context context) {
        PDFBoxResourceLoader.init(context.getApplicationContext());
    }

    /** Ritorna un File leggibile da PDFBox (filesDir aggiornabile -> altrimenti asset copiato in cache). */
    public static File getReadablePdfFile(Context ctx) throws IOException {
        File updated = new File(ctx.getFilesDir(), PDF_NAME);
        if (updated.exists() && updated.length() > 0) {
            if (isLikelyPdf(updated)) return updated;
            //noinspection ResultOfMethodCallIgnored
            updated.delete();
        }

        File cached = new File(ctx.getCacheDir(), PDF_NAME);
        if (!cached.exists() || cached.length() == 0 || !isLikelyPdf(cached)) {
            copyAssetTo(ctx, PDF_NAME, cached);
        }
        return cached;
    }

    /**
     * Estrae tutto il testo.
     * Se TWO_COLUMN_READING_ORDER=true: per pagina emette colonna sinistra poi destra (con taglio centrale).
     */
    public static String extractAllText(Context ctx) throws IOException {
        File pdfFile = getReadablePdfFile(ctx);
    
        try (PDDocument document = PDDocument.load(pdfFile, MemoryUsageSetting.setupTempFileOnly())) {
        
            if (!TWO_COLUMN_READING_ORDER) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                stripper.setLineSeparator("\n");
                return stripper.getText(document);
            }
        
            StringBuilder sb = new StringBuilder(4_000_000);
            int pageCount = document.getNumberOfPages();
        
            for (int i = 0; i < pageCount; i++) {
                PDPage page = document.getPage(i);
                PDRectangle box = page.getCropBox();
                if (box == null) box = page.getMediaBox();
            
                float width = (box != null) ? box.getWidth() : 600f;
                float mid = width / 2f;
            
                float gutter = width * CENTER_GUTTER_RATIO;
                if (gutter < CENTER_GUTTER_MIN_PT) gutter = CENTER_GUTTER_MIN_PT;
                if (gutter > CENTER_GUTTER_MAX_PT) gutter = CENTER_GUTTER_MAX_PT;
            
                float leftMinX = 0f;
                float leftMaxX = Math.max(0f, mid - gutter);
            
                float rightMinX = Math.min(width, mid + gutter);
                float rightMaxX = width;
            
                String left = extractPageXRange(document, i + 1, leftMinX, leftMaxX);
                String right = extractPageXRange(document, i + 1, rightMinX, rightMaxX);
            
                left = normalizeNewlines(left);
                right = normalizeNewlines(right);
            
                if (!left.isEmpty()) sb.append(left).append("\n");
                if (right.trim().length() >= MIN_RIGHT_COL_CHARS) sb.append(right).append("\n");
            
                sb.append("\n"); // separatore pagina
            }
        
            return sb.toString();
        }
    }

    /** Estrae una singola pagina filtrando per coordinate X (in punti PDF). */
    private static String extractPageXRange(PDDocument doc, int page1Based, float minX, float maxX) throws IOException {
        ColumnXStripper stripper = new ColumnXStripper(minX, maxX);
        stripper.setStartPage(page1Based);
        stripper.setEndPage(page1Based);
        stripper.setSortByPosition(true);
        stripper.setLineSeparator("\n");

        StringWriter sw = new StringWriter(64_000);
        stripper.writeText(doc, sw);
        return sw.toString();
    }

    /** Strip “solo caratteri” in una fascia X (così tagli la colonna centrale note). */
    private static final class ColumnXStripper extends PDFTextStripper {

        private final float minX;
        private final float maxX;

        ColumnXStripper(float minX, float maxX) throws IOException {
            super();
            this.minX = minX;
            this.maxX = maxX;
        }

        @Override
        protected void processTextPosition(TextPosition text) {
            if (text == null) return;

            // coordinate “aggiustate” considerando rotazioni/direzione
            float x = text.getXDirAdj();

            if (x >= minX && x <= maxX) {
                super.processTextPosition(text);
            }
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            // niente di speciale: lasciamo che PDFTextStripper gestisca spazi/linee
            super.writeString(text, textPositions);
        }
    }

    /** Salva/aggiorna il PDF in filesDir in modo "atomico" (sicuro contro file incompleti). */
    public static void replaceUpdatedPdf(Context ctx, InputStream newPdfStream) throws IOException {
        File target = new File(ctx.getFilesDir(), PDF_NAME);
        File tmp = new File(ctx.getCacheDir(), PDF_NAME + ".tmp");

        try (InputStream in = new BufferedInputStream(newPdfStream);
             OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }

        // sanity check minimale
        if (tmp.length() < MIN_PDF_SIZE_BYTES || !isLikelyPdf(tmp)) {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            throw new IOException("PDF non valido o corrotto");
        }

        // rimpiazzo best-effort
        if (target.exists()) {
            //noinspection ResultOfMethodCallIgnored
            target.delete();
        }
        if (!tmp.renameTo(target)) {
            // fallback copy se rename non funziona
            try (InputStream in = new FileInputStream(tmp);
                 OutputStream out = new FileOutputStream(target)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            }
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }

        NwtOfflineRepository.invalidate();
    }

    private static void copyAssetTo(Context ctx, String assetName, File dest) throws IOException {
        try (InputStream in = new BufferedInputStream(ctx.getAssets().open(assetName));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }

        if (!isLikelyPdf(dest)) {
            //noinspection ResultOfMethodCallIgnored
            dest.delete();
            throw new IOException("Asset PDF non valido: " + assetName);
        }
    }

    private static boolean isLikelyPdf(File file) {
        if (file == null || !file.exists() || file.length() < 5) return false;
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            byte[] header = new byte[5];
            int read = in.read(header);
            return read == 5
                    && header[0] == '%'
                    && header[1] == 'P'
                    && header[2] == 'D'
                    && header[3] == 'F'
                    && header[4] == '-';
        } catch (IOException e) {
            return false;
        }
    }

    private static String normalizeNewlines(String s) {
        if (s == null) return "";
        s = s.replace("\r\n", "\n").replace("\r", "\n");
        return s;
    }
}
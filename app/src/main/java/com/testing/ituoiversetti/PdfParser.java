package com.testing.ituoiversetti;

import android.content.Context;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.*;

public class PdfParser {

    private static final String PDF_NAME = "nwt_i.pdf";

    public static void init(Context context) {
        PDFBoxResourceLoader.init(context.getApplicationContext());
    }

    /** Ritorna un File leggibile da PDFBox (filesDir aggiornabile -> altrimenti asset copiato in cache). */
    public static File getReadablePdfFile(Context ctx) throws IOException {
        File updated = new File(ctx.getFilesDir(), PDF_NAME);
        if (updated.exists() && updated.length() > 0) return updated;

        File cached = new File(ctx.getCacheDir(), PDF_NAME);
        if (!cached.exists() || cached.length() == 0) {
            copyAssetTo(ctx, PDF_NAME, cached);
        }
        return cached;
    }

    public static String extractAllText(Context ctx) throws IOException {
        File pdfFile = getReadablePdfFile(ctx);
        try (PDDocument document = PDDocument.load(pdfFile, MemoryUsageSetting.setupTempFileOnly())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    /** Salva/aggiorna il PDF in filesDir in modo "atomico" (sicuro contro file incompleti). */
    public static void replaceUpdatedPdf(Context ctx, InputStream newPdfStream) throws IOException {
        File target = new File(ctx.getFilesDir(), PDF_NAME);
        File tmp = new File(ctx.getCacheDir(), PDF_NAME + ".tmp");

        try (OutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = newPdfStream.read(buf)) != -1) out.write(buf, 0, r);
        }

        // sanity check minimale
        if (tmp.length() < 1024) { // evita scritture vuote/corrotte
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            throw new IOException("PDF troppo piccolo o corrotto");
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

        // se hai cache testo/indice, invalidala qui:
        NwtOfflineRepository.invalidate(); // se usi il repo che ti ho passato
    }

    private static void copyAssetTo(Context ctx, String assetName, File dest) throws IOException {
        try (InputStream in = ctx.getAssets().open(assetName);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }
    }
}

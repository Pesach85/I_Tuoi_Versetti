package com.testing.ituoiversetti;

import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import android.content.Intent;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VerseImageActivity extends AppCompatActivity {

    // Registrati gratis su https://pixabay.com/api/docs/ per ottenere la key
    private static final String PIXABAY_API_KEY = "15015379-041f495b3c2fe11efa0fd21b9";
    private static final String PIXABAY_URL     = "https://pixabay.com/api/";

    private Bitmap verseBitmap;
    private ImageView imageView;
    private ProgressBar progressBar;
    private TextView tvLoading;
    private MaterialButton btnShareImage;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verse_image);

        String verseText = getIntent().getStringExtra("verse_text");
        if (verseText == null || verseText.isEmpty()) { finish(); return; }

        imageView     = findViewById(R.id.verseImageView);
        progressBar   = findViewById(R.id.progressBar);
        tvLoading     = findViewById(R.id.tvLoading);
        btnShareImage = findViewById(R.id.btnShareImage);
        btnShareImage.setEnabled(false);

        generateImage(verseText);
        btnShareImage.setOnClickListener(v -> shareImage());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    // -------------------------------------------------------------------------
    // Orchestrazione
    // -------------------------------------------------------------------------

    private void generateImage(String verseText) {
        progressBar.setVisibility(View.VISIBLE);
        tvLoading.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            Bitmap background = null;
            try {
                String keyword = extractBestKeyword(verseText);
                String imageUrl = fetchPixabayImageUrl(keyword);
                if (imageUrl != null) background = downloadBitmap(imageUrl);
            } catch (Exception ignored) {
                // fallback gradiente
            }

            final Bitmap bg = background;
            handler.post(() -> {
                verseBitmap = composeFinalBitmap(verseText, bg);
                imageView.setImageBitmap(verseBitmap);
                progressBar.setVisibility(View.GONE);
                tvLoading.setVisibility(View.GONE);
                btnShareImage.setEnabled(true);
            });
        });
    }

    // -------------------------------------------------------------------------
    // Pixabay: estrai keyword → cerca → scarica
    // -------------------------------------------------------------------------

    /**
     * Estrae la parola chiave più significativa dal versetto,
     * mappandola su temi visivi adatti a Pixabay.
     */
    private String extractBestKeyword(String verseText) {
        String lower = verseText.toLowerCase();

        // Mappa temi biblici → keyword visive efficaci su Pixabay
        if (containsAny(lower, "luce","illumina","splende","alba","sole","giorno"))
            return "divine light rays nature";
        if (containsAny(lower, "acqua","fiume","mare","sorgente","pioggia","torrente"))
            return "peaceful river water nature";
        if (containsAny(lower, "montagna","monte","altura","collina","roccia","pietra"))
            return "majestic mountain landscape";
        if (containsAny(lower, "cielo","nuvole","stelle","firmamento","angelo","angeli"))
            return "heavenly sky clouds golden";
        if (containsAny(lower, "pace","riposo","quiete","calma","sereno"))
            return "peaceful serene nature sunset";
        if (containsAny(lower, "amore","cuore","misericordia","grazia","tenerezza"))
            return "warm golden light heart nature";
        if (containsAny(lower, "forza","potenza","vittoria","guerra","battaglia","spada"))
            return "powerful storm lightning sky";
        if (containsAny(lower, "pane","cibo","vino","mensa","nutrimento"))
            return "wheat field golden harvest";
        if (containsAny(lower, "pastore","gregge","pecora","agnello"))
            return "shepherd sheep meadow pastoral";
        if (containsAny(lower, "verde","prato","erba","giardino","fiore"))
            return "green meadow flowers nature";
        if (containsAny(lower, "fuoco","fiamma","ardore","bruciare"))
            return "fire flame dramatic light";
        if (containsAny(lower, "albero","radice","frutto","vite","vigna"))
            return "ancient tree roots nature";
        if (containsAny(lower, "deserto","sabbia","sete","cammino","viaggio"))
            return "desert landscape sunrise";
        if (containsAny(lower, "notte","buio","tenebre","oscurità"))
            return "starry night sky milky way";
        if (containsAny(lower, "speranza","futuro","promessa","attesa"))
            return "sunrise horizon hopeful sky";
        if (containsAny(lower, "preghiera","pregare","ginocchio","supplica"))
            return "person praying silhouette sunset";

        // Default spirituale generico
        return "spiritual nature golden light landscape";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) if (text.contains(k)) return true;
        return false;
    }

    /**
     * Chiama l'API Pixabay e restituisce l'URL dell'immagine più rilevante.
     * Filtra per immagini orizzontali ad alta risoluzione, categoria nature/backgrounds.
     */
    private String fetchPixabayImageUrl(String keyword) throws Exception {
        String encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8");
        String urlStr = PIXABAY_URL
                + "?key=" + PIXABAY_API_KEY
                + "&q=" + encodedKeyword
                + "&image_type=photo"
                + "&orientation=vertical"
                + "&category=nature"
                + "&min_width=1080"
                + "&min_height=1080"
                + "&safesearch=true"
                + "&per_page=5"
                + "&order=popular";

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);

        int code = conn.getResponseCode();
        if (code != 200) throw new IOException("Pixabay HTTP " + code);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        JSONObject response = new JSONObject(sb.toString());
        JSONArray hits = response.getJSONArray("hits");
        if (hits.length() == 0) return null;

        // Prendi la prima immagine (più popolare)
        JSONObject first = hits.getJSONObject(0);

        // Usa largeImageURL per qualità massima
        return first.optString("largeImageURL", null);
    }

    /**
     * Scarica una bitmap da URL.
     */
    private Bitmap downloadBitmap(String imageUrl) throws Exception {
        URL url = new URL(imageUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        try (InputStream is = conn.getInputStream()) {
            return BitmapFactory.decodeStream(is);
        }
    }

    // -------------------------------------------------------------------------
    // Composita finale
    // -------------------------------------------------------------------------

    private Bitmap composeFinalBitmap(String verseText, Bitmap aiBackground) {
        int W = 1080, H = 1080;
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // 1. Sfondo Pixabay o gradiente fallback
        if (aiBackground != null) {
            // Crop centrato per riempire 1080x1080
            Bitmap scaled = cropAndScale(aiBackground, W, H);
            canvas.drawBitmap(scaled, 0, 0, null);
        } else {
            Paint bgPaint = new Paint();
            LinearGradient gradient = new LinearGradient(
                    0, 0, W, H,
                    new int[]{0xFF1a1a2e, 0xFF16213e, 0xFF0f3460},
                    null, Shader.TileMode.CLAMP);
            bgPaint.setShader(gradient);
            canvas.drawRect(0, 0, W, H, bgPaint);
        }

        // 2. Overlay scuro sfumato per leggibilità testo
        Paint overlayPaint = new Paint();
        LinearGradient overlayGrad = new LinearGradient(
                0, 0, 0, H,
                new int[]{0x44000000, 0xBB000000, 0xEE000000},
                new float[]{0f, 0.35f, 1f},
                Shader.TileMode.CLAMP);
        overlayPaint.setShader(overlayGrad);
        canvas.drawRect(0, 0, W, H, overlayPaint);

        // 3. Linee ornamentali dorate
        drawOrnamentalLines(canvas, W, H);

        // 4. Virgolette decorative
        Paint quotePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        quotePaint.setColor(0xFFc9a84c);
        quotePaint.setAlpha(70);
        quotePaint.setTextSize(260f);
        quotePaint.setTypeface(Typeface.create("serif", Typeface.BOLD));
        canvas.drawText("\u201C", 55, 300, quotePaint);

        // 5. Testo poetico
        List<String> poeticLines = toPoeticLines(verseText);
        drawPoeticText(canvas, poeticLines, W, H);

        // 6. Firma
        Paint signPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        signPaint.setColor(0xFFc9a84c);
        signPaint.setAlpha(200);
        signPaint.setTextSize(28f);
        signPaint.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        signPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("✦  I Tuoi Versetti  ✦", W / 2f, H - 50, signPaint);

        return bmp;
    }

    /**
     * Scala e taglia la bitmap sorgente per riempire esattamente WxH (center crop).
     */
    private Bitmap cropAndScale(Bitmap src, int W, int H) {
        float scaleW = (float) W / src.getWidth();
        float scaleH = (float) H / src.getHeight();
        float scale  = Math.max(scaleW, scaleH);

        int newW = Math.round(src.getWidth()  * scale);
        int newH = Math.round(src.getHeight() * scale);
        Bitmap scaled = Bitmap.createScaledBitmap(src, newW, newH, true);

        int offsetX = (newW - W) / 2;
        int offsetY = (newH - H) / 2;
        return Bitmap.createBitmap(scaled, offsetX, offsetY, W, H);
    }

    private void drawOrnamentalLines(Canvas canvas, int W, int H) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFc9a84c);
        p.setStrokeWidth(2f);
        p.setAlpha(160);
        canvas.drawLine(70, 108, W - 70, 108, p);
        canvas.drawLine(70, 116, W - 70, 116, p);
        canvas.drawLine(70, H - 108, W - 70, H - 108, p);
        canvas.drawLine(70, H - 116, W - 70, H - 116, p);

        // Piccoli ornamenti agli angoli
        p.setStrokeWidth(3f);
        canvas.drawLine(70, 108, 70, 130, p);
        canvas.drawLine(W - 70, 108, W - 70, 130, p);
        canvas.drawLine(70, H - 108, 70, H - 130, p);
        canvas.drawLine(W - 70, H - 108, W - 70, H - 130, p);
    }

    // -------------------------------------------------------------------------
    // Trasforma il testo in linee poetiche gestendo i riferimenti biblici
    // -------------------------------------------------------------------------
    private List<String> toPoeticLines(String text) {
        if (text == null) return new ArrayList<>();

        // 1. Incolla il numero ai due punti usando uno spazio non divisibile (\u00A0)
        // Esempio: "Giovanni 3: 16" -> "Giovanni 3:\u00A016"
        text = text.trim().replaceAll(":\\s*(\\d+)", ":\u00A0$1");

        // 2. Normalizza gli spazi generici
        text = text.replaceAll("\\s+", " ");

        // 3. Spezza su punteggiatura (ma i due punti non spezzeranno perché seguiti da \u00A0)
        String[] chunks = text.split("(?<=[.,;!?])\\s+");

        List<String> lines = new ArrayList<>();
        for (String chunk : chunks) {
            chunk = chunk.trim();
            if (chunk.isEmpty()) continue;

            // Capitalizza la prima lettera del chunk
            if (chunk.length() > 1) {
                chunk = Character.toUpperCase(chunk.charAt(0)) + chunk.substring(1);
            }

            // Se il chunk è ancora troppo lungo, lo spezza per parole
            if (chunk.length() > 38) {
                lines.addAll(splitLongChunk(chunk, 38));
            } else {
                lines.add(chunk);
            }
        }
        return lines;
    }

    private List<String> splitLongChunk(String chunk, int maxLen) {
        List<String> result = new ArrayList<>();
        // Lo split per spazio include anche lo spazio non divisibile (\u00A0) 
        // se l'engine regex lo supporta, altrimenti usiamo un matcher specifico
        String[] words = chunk.split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() > 0 && current.length() + 1 + word.length() > maxLen) {
                result.add(current.toString());
                current = new StringBuilder(word);
            } else {
                if (current.length() > 0) current.append(" ");
                current.append(word);
            }
        }
        if (current.length() > 0) result.add(current.toString());
        return result;
    }

    // -------------------------------------------------------------------------
    // Disegna il testo sul Canvas con stile alternato e performance ottimizzate
    // -------------------------------------------------------------------------
    private void drawPoeticText(Canvas canvas, List<String> lines, int W, int H) {
        int n = lines.size();
        if (n == 0) return;

        // Logica dinamica per dimensioni e interlinea
        float lineHeight = n <= 5 ? 72f : n <= 8 ? 62f : n <= 11 ? 52f : 44f;
        float textSize   = n <= 4 ? 54f : n <= 7 ? 46f : n <= 10 ? 38f : 32f;

        float totalH = n * lineHeight;
        float startY = (H - totalH) / 2f + 60f;

        // OTTIMIZZAZIONE: Paint creato una sola volta fuori dal ciclo
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(textSize);
        p.setShadowLayer(8f, 2f, 3f, 0xFF000000);

        for (int i = 0; i < n; i++) {
            boolean isLast = (i == n - 1);

            if (isLast) {
                // Ultima riga (Riferimento): Oro Bold Italic
                p.setColor(0xFFC9A84C);
                p.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));
            } else if (i % 2 == 0) {
                // Righe pari: Bianco caldo
                p.setColor(0xFFF5ECD7);
                p.setTypeface(Typeface.create("serif", Typeface.ITALIC));
            } else {
                // Righe dispari: Oro chiaro
                p.setColor(0xFFE8D5A3);
                p.setTypeface(Typeface.create("serif", Typeface.ITALIC));
            }

            canvas.drawText(lines.get(i), W / 2f, startY + i * lineHeight, p);
        }
    }

    // -------------------------------------------------------------------------
    // Condivisione
    // -------------------------------------------------------------------------

    private void shareImage() {
        if (verseBitmap == null) return;
        try {
            File imgFile = new File(getCacheDir(), "versetto.png");
            try (FileOutputStream fos = new FileOutputStream(imgFile)) {
                verseBitmap.compress(Bitmap.CompressFormat.PNG, 95, fos);
            }
            android.net.Uri uri = FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", imgFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Condividi immagine versetto"));
        } catch (Exception e) {
            Toast.makeText(this, "Errore condivisione", Toast.LENGTH_SHORT).show();
        }
    }
}
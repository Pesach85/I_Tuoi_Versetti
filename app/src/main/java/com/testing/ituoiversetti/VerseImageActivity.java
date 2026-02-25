package com.testing.ituoiversetti;

import android.graphics.*;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import android.content.Intent;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class VerseImageActivity extends AppCompatActivity {

    private Bitmap verseBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verse_image);

        String verseText = getIntent().getStringExtra("verse_text");
        if (verseText == null || verseText.isEmpty()) { finish(); return; }

        ImageView imageView = findViewById(R.id.verseImageView);
        MaterialButton btnShareImage = findViewById(R.id.btnShareImage);

        verseBitmap = generateVerseBitmap(verseText);
        imageView.setImageBitmap(verseBitmap);

        btnShareImage.setOnClickListener(v -> shareImage());
    }

    private Bitmap generateVerseBitmap(String text) {
        int W = 1080, H = 1080;
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Sfondo sfumato
        Paint bgPaint = new Paint();
        LinearGradient gradient = new LinearGradient(
                0, 0, W, H,
                new int[]{0xFF1a1a2e, 0xFF16213e, 0xFF0f3460},
                null, Shader.TileMode.CLAMP);
        bgPaint.setShader(gradient);
        canvas.drawRect(0, 0, W, H, bgPaint);

        // Cerchio decorativo sfumato in basso a destra
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RadialGradient radial = new RadialGradient(
                W * 0.85f, H * 0.85f, 420,
                0x22c9a84c, 0x00000000, Shader.TileMode.CLAMP);
        circlePaint.setShader(radial);
        canvas.drawCircle(W * 0.85f, H * 0.85f, 420, circlePaint);

        // Secondo cerchio in alto a sinistra
        RadialGradient radial2 = new RadialGradient(
                W * 0.15f, H * 0.1f, 300,
                0x22e8a0d0, 0x00000000, Shader.TileMode.CLAMP);
        circlePaint.setShader(radial2);
        canvas.drawCircle(W * 0.15f, H * 0.1f, 300, circlePaint);

        // Virgolette decorative
        Paint quotePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        quotePaint.setColor(0x33c9a84c);
        quotePaint.setTextSize(320f);
        Typeface serif = Typeface.create("serif", Typeface.BOLD);
        quotePaint.setTypeface(serif);
        canvas.drawText("\u201C", 30, 280, quotePaint);

        // Linea ornamentale in alto
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(0xFFc9a84c);
        linePaint.setStrokeWidth(2f);
        linePaint.setAlpha(120);
        canvas.drawLine(80, 120, W - 80, 120, linePaint);
        canvas.drawLine(80, 130, W - 80, 130, linePaint);

        // Testo versetto — wrapping manuale
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFF0EAD6);
        textPaint.setTextSize(52f);
        textPaint.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        textPaint.setTextAlign(Paint.Align.CENTER);

        float maxWidth = W - 160f;
        List<String> lines = wrapText(text, textPaint, maxWidth);
        float totalTextH = lines.size() * 68f;
        float startY = (H - totalTextH) / 2f + 20;

        for (String line : lines) {
            canvas.drawText(line, W / 2f, startY, textPaint);
            startY += 68f;
        }

        // Linea ornamentale in basso
        linePaint.setAlpha(120);
        canvas.drawLine(80, H - 120, W - 80, H - 120, linePaint);
        canvas.drawLine(80, H - 130, W - 80, H - 130, linePaint);

        // Firma app in basso
        Paint signPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        signPaint.setColor(0xAAc9a84c);
        signPaint.setTextSize(28f);
        signPaint.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        signPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("I Tuoi Versetti", W / 2f, H - 60, signPaint);

        return bmp;
    }

    private java.util.List<String> wrapText(String text, Paint paint, float maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.length() == 0 ? word : current + " " + word;
            if (paint.measureText(test) <= maxWidth) {
                current = new StringBuilder(test);
            } else {
                if (current.length() > 0) lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    private void shareImage() {
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
            startActivity(Intent.createChooser(intent, "Condividi immagine"));
        } catch (Exception e) {
            Toast.makeText(this, "Errore condivisione", Toast.LENGTH_SHORT).show();
        }
    }
}
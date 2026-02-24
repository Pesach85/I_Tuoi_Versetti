package com.testing.ituoiversetti;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class BatchVerseFetchActivity extends AppCompatActivity {
    private EditText libroView, capitoloView, versettoInView, versettoFinView, iterazioniView;
    private Button startBtn;
    private TextView outputView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_verse_fetch);

        libroView = findViewById(R.id.batchLibro);
        capitoloView = findViewById(R.id.batchCapitolo);
        versettoInView = findViewById(R.id.batchVersettoIn);
        versettoFinView = findViewById(R.id.batchVersettoFin);
        iterazioniView = findViewById(R.id.batchIterazioni);
        startBtn = findViewById(R.id.batchStartBtn);
        outputView = findViewById(R.id.batchOutput);

        startBtn.setOnClickListener(v -> {
            String libro = libroView.getText().toString().trim();
            int capitolo = parseInt(capitoloView);
            int vIn = parseInt(versettoInView);
            int vFin = parseInt(versettoFinView);
            int iter = parseInt(iterazioniView);
            if (libro.isEmpty() || capitolo <= 0 || vIn <= 0 || vFin < vIn || iter <= 0) {
                outputView.setText("Dati non validi");
                return;
            }
            startBtn.setEnabled(false);
            outputView.setText("Inizio batch...");
            batchFetch(libro, capitolo, vIn, vFin, iter);
        });
    }

    private void batchFetch(String libro, int capitolo, int vIn, int vFin, int iter) {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable[] task = new Runnable[1];
        task[0] = new Runnable() {
            int current = 0;
            int cap = capitolo;
            @Override
            public void run() {
                if (current >= iter) {
                    outputView.append("\nBatch completato.");
                    startBtn.setEnabled(true);
                    return;
                }
                outputView.append("\nRicerca: " + libro + " " + cap + ": " + vIn + "-" + vFin);
                new Thread(() -> {
                    try {
                        String result = MainActivity.searchDbThenWebAndCacheStatic(
                            getApplicationContext(), libro, cap, vIn, vFin
                        );
                        handler.post(() -> {
                            outputView.append("\nRisultato: " + result);
                            current++;
                            cap++;
                            handler.postDelayed(task[0], 5000);
                        });
                    } catch (Exception e) {
                        handler.post(() -> outputView.append("\nERRORE: " + e.getClass().getName() + " - " + e.getMessage()));
                        try {
                            java.io.FileWriter fw = new java.io.FileWriter(
                                getExternalFilesDir(null) + "/crash_log.txt", true
                            );
                            fw.write(e.toString() + "\n");
                            fw.close();
                        } catch (Exception ignored) {}
                        handler.post(() -> {
                            current++;
                            cap++;
                            handler.postDelayed(task[0], 5000);
                        });
                    }
                }).start();
            }
        };
        handler.post(task[0]);
    }

    private int parseInt(EditText v) {
        try { return Integer.parseInt(v.getText().toString().trim()); } catch (Exception e) { return -1; }
    }
}

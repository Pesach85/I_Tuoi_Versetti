package com.testing.ituoiversetti;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialSwitch swEnabled = findViewById(R.id.sw_debug);
        MaterialSwitch swVerbose = findViewById(R.id.sw_verbose);
        MaterialButton btnShow = findViewById(R.id.btn_show_log);
        MaterialButton btnClear = findViewById(R.id.btn_clear_log);
        TextView info = findViewById(R.id.txt_info);

        swEnabled.setChecked(DebugLog.isEnabled(this));
        swVerbose.setChecked(DebugLog.isVerbose(this));

        info.setText(
                "filesDir: " + getFilesDir().getAbsolutePath() + "\n" +
                "db: " + getDatabasePath("bible.db").getAbsolutePath()
        );

        swEnabled.setOnCheckedChangeListener((v, checked) -> {
            DebugLog.setEnabled(this, checked);
            Toast.makeText(this, checked ? "Logging ON" : "Logging OFF", Toast.LENGTH_SHORT).show();
        });

        swVerbose.setOnCheckedChangeListener((v, checked) -> {
            DebugLog.setVerbose(this, checked);
            Toast.makeText(this, checked ? "Verbose ON" : "Verbose OFF", Toast.LENGTH_SHORT).show();
        });

        btnShow.setOnClickListener(v -> {
            String t = DebugLog.tail(this, 64 * 1024); // ultimi 64KB
            TextView tv = new TextView(this);
            tv.setText(t);
            tv.setTextIsSelectable(true);
            tv.setPadding(24, 24, 24, 24);
            tv.setMovementMethod(new ScrollingMovementMethod());

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Debug log (tail)")
                    .setView(tv)
                    .setPositiveButton("OK", null)
                    .show();
        });

        btnClear.setOnClickListener(v -> {
            DebugLog.clear(this);
            Toast.makeText(this, "Log pulito", Toast.LENGTH_SHORT).show();
        });
    }
}

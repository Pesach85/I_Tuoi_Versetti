package com.testing.ituoiversetti;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Objects;

/**
 * @author Pasquale Edmondo Lombardi under Open Source license. plombardi85@gmail.com
 */
public class MainActivity extends AppCompatActivity {
    AutoCompleteTextView[] mEdit = new AutoCompleteTextView[5];
    ArrayAdapter<String> arrayAdapter;
    String libro = "";
    Integer capitolo = 0;
    Integer versetto_in = 0;
    Integer versetto_final = 0;
    Bibbia bibbia = new Bibbia();
    String testo = "";
    InputCheck ok = new InputCheck();

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) throws NullPointerException {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEdit[0] = new MultiAutoCompleteTextView(this);
        mEdit[1] = new MultiAutoCompleteTextView(this);

        // Create a new data adapter object.
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bibbia.composeBibbia());

        mEdit[0] = findViewById(R.id.autoCompleteTextView);
        mEdit[1] = findViewById(R.id.autoCompleteTextView2);
        mEdit[2] = findViewById(R.id.autoCompleteTextView3);
        mEdit[3] = findViewById(R.id.autoCompleteTextView4);

        mEdit[0].setAdapter(arrayAdapter);


        final Button button = findViewById(R.id.button);

        button.setOnClickListener(v -> {
            testo = null;
            libro = ""; capitolo = 0; versetto_in = 0; versetto_final = 0;
            libro = mEdit[0].getText().toString();
            libro = ok.setTitoloCorrected(libro);
            try {
                new NumCapitoli().selectCapN(libro);
            } catch (IOException e) {
                e.printStackTrace();
            }
            capitolo = Integer.parseInt(mEdit[1].getText().toString());
            try {
                capitolo = ok.setCapitoloCorrected(capitolo);
            } catch (IOException e) {
                e.printStackTrace();
            }
            versetto_in = Integer.parseInt(mEdit[2].getText().toString());
            if (mEdit[3].getText().toString().isEmpty()) {
                try {
                    while (testo == null) testo = temp_string(versetto_in, versetto_in);
                } catch (NullPointerException | IOException f) {
                    f.printStackTrace();
                }
            } else {
                versetto_final = Integer.parseInt(mEdit[3].getText().toString());
                try {
                    while (testo == null) testo = temp_string(versetto_in, versetto_final);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mEdit[4] = findViewById(R.id.multiAutoCompleteTextView);
            try {
                mEdit[4].setText(new StringBuilder().append(libro).append(" ")
                        .append(capitolo).append(": ").append(testo));
            } catch (NullPointerException ignored) {}
        });

        final ImageButton button1 = findViewById(R.id.imageButton);
        button1.setOnClickListener(v -> {
            // your handler code here
            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setType("text/plain");
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, mEdit[4].getText().toString());
            try {
                MainActivity.this.startActivity(whatsappIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                                 Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp")));
            }
        });
    }

    public String temp_string(Integer versetto_in, Integer versetto_final) throws IOException {
        String temp = null;
        long startTime = System.currentTimeMillis();

        boolean connected;
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        connected = cm.getNetworkCapabilities(cm.getActiveNetwork()) != null;


        if (connected) {
            Bibbia bibbia2 = new Bibbia();
            while ( bibbia2.src.equals("") && (System.currentTimeMillis() - startTime < 17000)) {
                try {
                    bibbia2.getWebContent(libro, capitolo, versetto_in, versetto_final);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                if ((System.currentTimeMillis() - startTime) == 16500) { temp = "Conn timeout"; break; }
                }
            if (bibbia2.search) temp = bibbia2.src;
            }
        else temp = " no connection";
        return temp;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}

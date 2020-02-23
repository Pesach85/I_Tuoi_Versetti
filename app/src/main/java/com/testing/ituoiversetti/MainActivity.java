package com.testing.ituoiversetti;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.util.Objects;

/**
 * @author Pasquale Edmondo Lombardi under Open Source license. plombardi85@gmail.com
 */
public class MainActivity extends AppCompatActivity {
    EditText[] mEdit = new EditText[5];
    String libro = "";
    Integer capitolo = 0;
    Integer versetto_in = 0;
    Integer versetto_final = 0;
    Bibbia bibbia = new Bibbia();
    String testo = "";
    InputCheck ok = new InputCheck();

    public MainActivity() throws IOException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) throws NullPointerException {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEdit = new EditText[5];
                testo = "";
                libro = ""; capitolo = 0; versetto_in = 0; versetto_final = 0;
                // your handler code here
                mEdit[0] = findViewById(R.id.autoCompleteTextView);
                mEdit[1] = findViewById(R.id.autoCompleteTextView2);
                mEdit[2] = findViewById(R.id.autoCompleteTextView3);
                mEdit[3] = findViewById(R.id.autoCompleteTextView4);
                libro = mEdit[0].getText().toString();
                libro = ok.setTitoloCorrected(libro);
                capitolo = Integer.parseInt(mEdit[1].getText().toString());
                try {
                    capitolo = ok.setCapitoloCorrected(capitolo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                versetto_in = Integer.parseInt(mEdit[2].getText().toString());
                if (mEdit[3].getText().toString().isEmpty()) {
                    try {
                        testo = bibbia.getWebContent(libro,capitolo,versetto_in);
                    } catch (NullPointerException | IOException f) {
                        f.printStackTrace();
                    }
                } else {
                    versetto_final = Integer.parseInt(mEdit[3].getText().toString());
                    try {
                        testo = bibbia.getWebContent(libro,capitolo,versetto_in,versetto_final);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                mEdit[4] = findViewById(R.id.multiAutoCompleteTextView);
                try {
                    mEdit[4].setText(new StringBuilder().append(libro).append(" ")
                            .append(capitolo).append(": ").append(testo));
                } catch (NullPointerException j) {System.out.println("Errore di input");}
            }
        });

        final ImageButton button1 = findViewById(R.id.imageButton);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // your handler code here
                Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
                whatsappIntent.setType("text/plain");
                whatsappIntent.setPackage("com.whatsapp");
                whatsappIntent.putExtra(Intent.EXTRA_TEXT, mEdit[4].getText().toString());
                try {
                    Objects.requireNonNull(MainActivity.this).startActivity(whatsappIntent);
                } catch (android.content.ActivityNotFoundException ex) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                                     Uri.parse("http://play.google.com/store/apps/details?id=com.whatsapp")));
                }
            }
        });
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

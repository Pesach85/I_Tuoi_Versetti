package com.testing.ituoiversetti;

import android.os.AsyncTask;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import java.io.IOException;



public class JsoupParser extends AsyncTask<Void,Void, Void> {

    public Connection conn;
    public Response res;
    boolean done;


    @Override
    protected Void doInBackground(Void... arg0) {
        res = null;

        String userAgent = System.getProperty("http.agent");

        try {
            conn = Jsoup.connect("https://wol.jw.org/it/wol/binav/r6/lp-i/nwtsty");


            res = conn
                    .userAgent(userAgent)
                    .referrer("https://wol.jw.org/it/wol/h/r6/lp-i")
                    .ignoreContentType(true)
                    .header(":authority", "wol.jw.org")
                    .header(":method", "GET")
                    .header(":path", "/wol/ls?locale=it&type=documentOptions&wtlocale=I")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("Accept-Encoding", "gzip, deflate, br, zstd")
                    .header("Accept-Language", "it,it-IT;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                    .timeout(12000)
                    .maxBodySize(0)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)//.get();
                    .execute();

        } catch (IOException e)
        {
            e.printStackTrace();
        }
        this.done = true;
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }

}


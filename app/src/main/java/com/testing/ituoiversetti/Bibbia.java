package com.testing.ituoiversetti;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

/**
 * @author Pasquale Edmondo Lombardi under Open Source license. plombardi85@gmail.com
 */
public class Bibbia {

    static int MAXLENGHTVERS = 7;

    private int num_libro = 0;

    JsoupParser js = new JsoupParser();

    Document doc = null;
    Connection.Response res = null;

    public boolean search = false;

    public String src = "";


    Bibbia() {
        composeBibbia();
    }


    public void getWebContent(String a, int b, int c, int d) throws IOException {
        int temp = 0;
        int temp2 = 0;

        String bibleTemp = "https://wol.jw.org/it/wol/b/r6/lp-i/nwtsty";
        String testo2 = "";
        String result = "";

        String userAgent = System.getProperty("http.agent");

        int aCon = 0;

        aCon = convertLibro(a);

        bibleTemp = bibleTemp.concat("/" + aCon + "/" + b + "#study=discover&v=" +
                aCon + ":" + b + ":" + c + "-" + aCon + ":" +
                b + ":" + d);


        js.execute();

        // Thread downloadThread = new Thread(new Runnable() {
        Connection conn = Jsoup.connect(bibleTemp);

        while (res == null) {
        //while (doc == null) {
            try
            {
            res = conn
                //doc = conn
                    .userAgent(userAgent)
                    .referrer("https://wol.jw.org/it/wol/binav/r6/lp-i/nwtsty")
                    .ignoreContentType(true)
                    .header(":authority", "wol.jw.org")
                    .header(":method", "GET")
                    .header(":path", "/wol/ls?locale=it&type=documentOptions&wtlocale=I")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("Accept-Encoding", "gzip, deflate, br, zstd")
                    .header("Accept-Language", "it,it-IT;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                    .header("Cookie", "cookieConsent-STRICTLY_NECESSARY=true; cookieConsent-FUNCTIONAL=true; cookieConsent-DIAGNOSTIC=true; cookieConsent-USAGE=true;")
                    .timeout(12000)
                    .maxBodySize(0)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)//.get();
                    .execute();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }

        while (doc == null) {
                try
                {
                doc = res.parse();
                }

                catch (IOException e)
                {
                    e.printStackTrace();
                }
        }

        //});


        ArrayList<String> ids = new ArrayList<>();

        for (int i = 0; i<=(d-c); i++) {

            for (int j = 1; j < MAXLENGHTVERS; j++) {
                String id = "v" + aCon + "-" + b + "-" +
                        (c + i) + "-" + j;
                try {
                    if (doc.getElementById(id).text() != null) {
                        ids.add(id);
                        temp++;
                    }
                    else {
                        break;
                    }
                } catch (NullPointerException ignored) {
                }
            }
            temp2++;
        }

        for(int i = 0; i<(temp+temp2); i++) {
            try {
                result = result.concat(doc.getElementById(ids.get(i)).text() + " ");
            } catch (IndexOutOfBoundsException ignored) {}
        }

        testo2 = result.replaceAll("[+*]", "");
        if ((c==1)&&(testo2!=null)) testo2 = testo2.substring(2);
        if (js.done)
        {
            search = true;
            src = testo2;
        }
    }

    protected ArrayList<String> composeBibbia() {
        List<String> titolo_libri = Arrays.asList("Genesi", "Esodo", "Levitico", "Numeri", "Deuteronomio", "Giosuè", "Giudici", "Rut",
                "1 Samuele", "2 Samuele", "1 Re", "2 Re", "1 Cronache", "2 Cronache", "Esdra", "Neemia",
                "Ester", "Giobbe", "Salmi", "Proverbi", "Ecclesiaste", "Cantico dei Cantici", "Isaia",
                "Geremia", "Lamentazioni", "Ezechiele", "Daniele", "Osea", "Gioele", "Amos", "Abdia",
                "Giona", "Michea", "Naum", "Abacuc", "Sofonia", "Aggeo", "Zaccaria", "Malachia",
                "Matteo", "Marco", "Luca", "Giovanni", "Atti", "Romani", "1 Corinti", "2 Corinti", "Galati",
                "Efesini", "Filippesi", "Colossesi", "1 Tessalonicesi", "2 Tessalonicesi", "1 Timoteo",
                "2 Timoteo", "Tito", "Filemone", "Ebrei", "Giacomo", "1 Pietro", "2 Pietro", "1 Giovanni", "2 Giovanni",
                "3 Giovanni", "Giuda", "Rivelazione");
        return new ArrayList<>(titolo_libri);
    }

    protected int convertLibro(String a){
        for (int i = 0; i<composeBibbia().size(); i++)
        {
            if (composeBibbia().get(i).contains(a)) {
                this.num_libro = i;
                break;
            }
        }
        return this.num_libro+1;
    }

}

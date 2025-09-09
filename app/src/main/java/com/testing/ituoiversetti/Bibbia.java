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

    private final String bible = "https://wol.jw.org/it/wol/b/r6/lp-i/nwtsty";
    private int num_libro;
    private String testo = "";
    private String bibleTemp = "";
    private int temp;
    private int temp2;


    Bibbia() {
        composeBibbia();
    }

    protected String getWebContent(String a, int b, int c) throws IOException {
        String result2 = "";
        bibleTemp = "";
        bibleTemp = bible;
        int chap = convertLibro(a);

        bibleTemp = bibleTemp.concat("/" + chap + "/" + b + "#study=discover&v=" + chap + ":" + b + ":" + c);
        JsoupParser js = new JsoupParser(bibleTemp);

        if (200 == js.res.statusCode()) {
            Document doc = js.res.parse();

            for (int i = 1; i < 6; i++) {
                String id2 = "v" + chap + "-" + b + "-" + c + "-" + i;
                try {
                    if (!Objects.requireNonNull(doc.getElementById(id2)).text().equals(""))
                        result2 += Objects.requireNonNull(doc.getElementById(id2)).text() + " ";
                    else break;
                } catch (NullPointerException ignored) {
                }

            }
            this.testo = result2.replaceAll("[+*]", "");
            if (c == 1) this.testo = testo.substring(2);
        }
        else {
            this.testo = " no connection";
        }
        return testo;
    }

    protected String getWebContent(String a, int b, int c, int d) throws IOException {
        bibleTemp = ""; testo="";
        StringBuilder result = new StringBuilder();
        int aCon;
        bibleTemp = bible;
        aCon = convertLibro(a);
        bibleTemp = bibleTemp.concat("/" + aCon + "/" + b + "#study=discover&v=" +
                aCon + ":" + b + ":" + c + "-" + aCon + ":" +
                b + ":" + d);

        Connection.Response response= Jsoup.connect(this.bibleTemp)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
                .header("Accept-Language", "it-IT")
                .referrer("http://www.google.com")
                .timeout(20000)
                .followRedirects(true)
                .execute();
        if (200 == response.statusCode()) {
            Document doc = response.parse();

            ArrayList<String> ids = new ArrayList<>();

            for (int i = 0; i <= (d - c); i++) {

                for (int j = 1; j < MAXLENGHTVERS; j++) {
                    String id = "v" + aCon + "-" + b + "-" +
                            (c + i) + "-" + j;
                    try {
                        if (!Objects.requireNonNull(doc.getElementById(id)).text().equals("")) {
                            ids.add(id);
                            temp++;
                        } else {
                            break;
                        }
                    } catch (NullPointerException ignored) {
                    }
                }
                temp2++;
            }

            for (int i = 0; i < (temp + temp2); i++) {
                try {
                    result.append(Objects.requireNonNull(doc.getElementById(ids.get(i))).text()).append(" ");
                } catch (IndexOutOfBoundsException ignored) {
                }
            }

            this.testo = result.toString().replaceAll("[+*]", "");
            if ((c == 1) && (this.testo != null)) this.testo = testo.substring(2);
        }
        else {
            this.testo = " no connection";
        }
        return this.testo;
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

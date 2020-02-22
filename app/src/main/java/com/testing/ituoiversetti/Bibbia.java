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

    private int aCon;
    private String result = "";
    private String result2 = "";
    private ArrayList<String> ids;
    private String id = "";
    private String bible = "https://wol.jw.org/it/wol/b/r6/lp-i/nwtsty";
    private int num_libro;
    private ArrayList<String> bibbia;
    private List<String> titolo_libri;
    private String testo = "";
    private String bibleTemp = "";
    private int temp;
    private int temp2;


    Bibbia() throws IOException {
        composeBibbia();
    }

    protected String getWebContent(String a, int b, int c) throws IOException {
        result2 = ""; bibleTemp = "";
        bibleTemp = bible;
        bibleTemp = bibleTemp.concat(new StringBuilder().append("/").append(convertLibro(a)).append("/").append(b).append("#study=discover&v=").append(convertLibro(a)).append(":").append(b).append(":").append(c).toString());
        Connection conn = Jsoup.connect(bibleTemp);
        Document doc = conn.get();
        for (int i = 1; i < 6; i++) {
            String id2 = new StringBuilder().append("v").append(convertLibro(a)).append("-").append(b).append("-").append(c).append("-").append(i).toString();
            try {
                if (doc.getElementById(id2).text() != null) result2 += doc.getElementById(id2).text()+" ";
                else break;
            } catch (NullPointerException h) {};
        }
        this.testo = result2.replaceAll("[+*]", "");
        if (c==1) this.testo = testo.substring(2);
        return testo;
    }

    protected String getWebContent(String a, int b, int c, int d) throws IOException {
        bibleTemp = ""; testo=""; result = ""; aCon = 0;
        bibleTemp = bible;
        aCon = convertLibro(a);
        bibleTemp = bibleTemp.concat(new StringBuilder().append("/").append(aCon).append("/").append(b).append("#study=discover&v=")
                                .append(aCon).append(":").append(b).append(":").append(c).append("-").append(aCon).append(":")
                                .append(b).append(":").append(d).toString());
        Connection conn = Jsoup.connect(this.bibleTemp);
        Document doc = conn.get();

        ids = new ArrayList<String>();

        for (int i = 0; i<=(d-c); i++) {

            for (int j = 1; j < MAXLENGHTVERS; j++) {
                this.id = new StringBuilder().append("v").append(aCon).append("-").append(b).append("-")
                                             .append(c + i).append("-").append(j).toString();
                try {
                    if (doc.getElementById(id).text() != null) {
                        this.ids.add(id);
                        temp++;
                    }
                    else {
                        break;
                    }
                } catch (NullPointerException h) {
                }
            }
            temp2++;
        }

        for(int i = 0; i<(temp+temp2); i++) {
           try {
               this.result += doc.getElementById(ids.get(i)).text()+" ";
           } catch (IndexOutOfBoundsException j) {}
        }

        this.testo = result.replaceAll("[+*]", "");
        if ((c==1)&&(this.testo!=null)) this.testo = testo.substring(2);
        return this.testo;
    }

    protected ArrayList<String> composeBibbia() {
        titolo_libri = Arrays.asList("Genesi","Esodo","Levitico", "Numeri", "Deuteronomio", "Giosuè", "Giudici", "Rut",
                "1 Samuele","2 Samuele", "1 Re", "2 Re", "1 Cronache", "2 Cronache", "Esdra", "Neemia",
                "Ester", "Giobbe", "Salmi", "Proverbi", "Ecclesiaste", "Cantico dei Cantici", "Isaia",
                "Geremia", "Lamentazioni", "Ezechiele", "Daniele", "Osea", "Gioele", "Amos", "Abdia",
                "Giona", "Michea", "Naum", "Abacuc", "Sofonia", "Aggeo", "Zaccaria", "Malachia",
                "Matteo","Marco","Luca","Giovanni","Atti","Romani","1 Corinti","2 Corinti", "Galati",
                "Efesini", "Filippesi", "Colossesi", "1 Tessalonicesi", "2 Tessalonicesi", "1 Timoteo",
                "2 Timoteo", "Tito", "Filemone", "Ebrei", "Giacomo", "1 Pietro", "2 Pietro", "1 Giovanni", "2 Giovanni",
                "3 Giovanni", "Giuda", "Rivelazione");
        this.bibbia = new ArrayList<String>();
        this.bibbia.addAll(titolo_libri);
        return this.bibbia;
    }

    protected int convertLibro(String a){
        for (int i = 0; i<composeBibbia().size(); i++)
        {
            if (composeBibbia().get(i).indexOf(a)!=-1) {
                this.num_libro = i;
                break;
            }
        }
        return this.num_libro+1;
    }

}

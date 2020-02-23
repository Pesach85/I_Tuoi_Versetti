package com.testing.ituoiversetti;

import java.io.IOException;

/**
 * @author Pasquale Edmondo Lombardi under Open Source license. plombardi85@gmail.com
 */
public class InputCheck {

    private static String corrLib;
    private int capLib;

    protected String setTitoloCorrected(String i) {
        corrLib = "";
        if (i.contains("aa")) i = i.replace("aa", "a");
        if (i.contains("ii")) i = i.replace("ii", "i");
        if (i.contains("oo")) i = i.replace("oo", "o");
        if (i.contains("uu")) i = i.replace("uu", "u");
        if (i.contains("ttt")) i = i.replace("ttt", "tt");
        if (i.contains("sss")) i = i.replace("sss", "ss");
        if (i.contains("ppp")) i = i.replace("ppp", "pp");
        if (i.contains("nnn")) i = i.replace("nnn", "nn");
        if (i.contains("11")) i = i.replace("11", "1");
        if (i.contains("22")) i = i.replace("22", "2");
        if (i.contains("33")) i = i.replace("33", "3");
        if (i.contains("111")) i = i.replace("111", "1");
        if (i.contains("222")) i = i.replace("222", "2");
        if (i.contains("333")) i = i.replace("333", "3");
        if (i.contains("1Samuele")) i = i.replace("1Samuele", "1 Samuele");
        if (i.contains("salmo")) i = i.replace("salmo", "Salmi");
        if (i.contains("Salmo")) i = i.replace("Salmo", "Salmi");
        if (i.contains("Cantico dei cantici")) i = i.replace("Cantico dei cantici", "Cantico dei Cantici");
        if (i.contains("CanticodeiCantici")) i = i.replace("CanticodeiCantici", "Cantico dei Cantici");
        if (i.contains("Canticodei Cantici")) i = i.replace("Canticodei Cantici", "Cantico dei Cantici");
        if (i.contains("Cantico deiCantici")) i = i.replace("Cantico deiCantici", "Cantico dei Cantici");
        if (i.contains("2Samuele")) i = i.replace("2Samuele", "2 Samuele");
        if (i.contains("2samuele")) i = i.replace("2samuele", "2 Samuele");
        if (i.contains("1Tessalonicesi")) i = i.replace("1Tessalonicesi", "1 Tessalonicesi");
        if (i.contains("2Tessalonicesi")) i = i.replace("2Tessalonicesi", "2 Tessalonicesi");
        if (i.contains("1Corinti")) i = i.replace("1Corinti", "1 Corinti");
        if (i.contains("1corinti")) i = i.replace("1corinti", "1 Corinti");
        if (i.contains("1 corinti")) i = i.replace("1 corinti", "1 Corinti");
        if (i.contains("2Corinti")) i = i.replace("2Corinti", "2 Corinti");
        if (i.contains("2 corinti")) i = i.replace("2 corinti", "2 Corinti");
        if (i.contains("2corinti")) i = i.replace("2corinti", "2 Corinti");
        if (i.contains("1Giovanni")) i = i.replace("1Giovanni", "1 Giovanni");
        if (i.contains("2Giovanni")) i = i.replace("2Giovanni", "2 Giovanni");
        if (i.contains("3Giovanni")) i = i.replace("3Giovanni", "3 Giovanni");
        if (i.contains("#")||i.contains(".")) i = i.replaceAll("[#.]", "");
        if (i.contains("  ")) i = i.replaceAll("  ", " ");
        if (i.startsWith(" ")) i = i.substring(1);
        if (i.endsWith(" ")) i = removeLastChar(i);
        if (Character.isLowerCase(i.charAt(0))&Character.isAlphabetic(i.charAt(0))) i = capitalize(i);
        if (Character.isLowerCase(i.charAt(2))&Character.isDigit(i.charAt(0))) i.toUpperCase().charAt(1);
        System.out.println("Richiesta accettata = " + i);
        corrLib=i;
        return corrLib;
    }

    private String capitalize(String str) {
        if(str == null || str.isEmpty()) {
            return str;
        }

        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }


    private String removeLastChar(String str) {
        return str.substring(0, str.length() - 1);
    }


    protected int setCapitoloCorrected(int num) throws IOException, IndexOutOfBoundsException {
        int max;
        capLib = 0;
        int val = new Bibbia().convertLibro(corrLib);

            max = new Capitolo().getCapitoli().get((val-1));
            if (num < 1) num = 1;
            else if (num > max) num = max;
            capLib = num;

        return capLib;
    }

}

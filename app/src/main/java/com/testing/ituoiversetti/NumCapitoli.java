package com.testing.ituoiversetti;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Pasquale
 */
public class NumCapitoli extends Capitolo {
    ArrayList<String> caps;
    Capitolo nuovo;

    public NumCapitoli() throws IOException {
        caps = new ArrayList<String>();
        nuovo = new Capitolo();
        nuovo.createNumCap();
    }

    protected void selectCapN(String s) throws IOException {
        switch (s) {
            case "Genesi":
                for (int i = 1; i<=nuovo.getCapitoli().get(0); i++) caps.add(String.valueOf(i));
                System.out.println(caps.toString());
                break;
            case "Esodo":
                for (int i = 1; i<=nuovo.getCapitoli().get(1); i++) caps.add(String.valueOf(i));
                break;
            case "Levitico":
                for (int i = 1; i<=nuovo.getCapitoli().get(2); i++) caps.add(String.valueOf(i));
                break;
            case "Numeri":
                for (int i = 1; i<=nuovo.getCapitoli().get(3); i++) caps.add(String.valueOf(i));
                break;
            case "Deuteronomio":
                for (int i = 1; i<=nuovo.getCapitoli().get(4); i++) caps.add(String.valueOf(i));
                break;
            case "Giosuè":
                for (int i = 1; i<=nuovo.getCapitoli().get(5); i++) caps.add(String.valueOf(i));
                break;
            case "Giudici":
                for (int i = 1; i<=nuovo.getCapitoli().get(6); i++) caps.add(String.valueOf(i));
                break;
            case "Rut":
                for (int i = 1; i<=nuovo.getCapitoli().get(7); i++) caps.add(String.valueOf(i));
                break;
            case "1 Samuele":
                for (int i = 1; i<=nuovo.getCapitoli().get(8); i++) caps.add(String.valueOf(i));
                break;
            case "2 Samuele":
                for (int i = 1; i<=nuovo.getCapitoli().get(9); i++) caps.add(String.valueOf(i));
                break;
            case "1 Re":
                for (int i = 1; i<=nuovo.getCapitoli().get(10); i++) caps.add(String.valueOf(i));
                break;
            case "2 Re":
                for (int i = 1; i<=nuovo.getCapitoli().get(11); i++) caps.add(String.valueOf(i));
                break;
            case "1 Cronache":
                for (int i = 1; i<=nuovo.getCapitoli().get(12); i++) caps.add(String.valueOf(i));
                break;
            case "2 Cronache":
                for (int i = 1; i<=nuovo.getCapitoli().get(13); i++) caps.add(String.valueOf(i));
                break;
            case "Esdra":
                for (int i = 1; i<=nuovo.getCapitoli().get(14); i++) caps.add(String.valueOf(i));
                break;
            case "Neemia":
                for (int i = 1; i<=nuovo.getCapitoli().get(15); i++) caps.add(String.valueOf(i));
                break;
            case "Ester":
                for (int i = 1; i<=nuovo.getCapitoli().get(16); i++) caps.add(String.valueOf(i));
                break;
            case "Giobbe":
                for (int i = 1; i<=nuovo.getCapitoli().get(17); i++) caps.add(String.valueOf(i));
                break;
            case "Salmi":
                for (int i = 1; i<=nuovo.getCapitoli().get(18); i++) caps.add(String.valueOf(i));
                break;
            case "Proverbi":
                for (int i = 1; i<=nuovo.getCapitoli().get(19); i++) caps.add(String.valueOf(i));
                break;
            case "Ecclesiaste":
                for (int i = 1; i<=nuovo.getCapitoli().get(20); i++) caps.add(String.valueOf(i));
                break;
            case "Cantico dei Cantici":
                for (int i = 1; i<=nuovo.getCapitoli().get(21); i++) caps.add(String.valueOf(i));
                break;
            case "Isaia":
                for (int i = 1; i<=nuovo.getCapitoli().get(22); i++) caps.add(String.valueOf(i));
                break;
            case "Geremia":
                for (int i = 1; i<=nuovo.getCapitoli().get(23); i++) caps.add(String.valueOf(i));
                break;
            case "Lamentazioni":
                for (int i = 1; i<=nuovo.getCapitoli().get(24); i++) caps.add(String.valueOf(i));
                break;
            case "Ezechiele":
                for (int i = 1; i<=nuovo.getCapitoli().get(25); i++) caps.add(String.valueOf(i));
                break;
            case "Daniele":
                for (int i = 1; i<=nuovo.getCapitoli().get(26); i++) caps.add(String.valueOf(i));
                break;
            case "Osea":
                for (int i = 1; i<=nuovo.getCapitoli().get(27); i++) caps.add(String.valueOf(i));
                break;
            case "Gioele":
                for (int i = 1; i<=nuovo.getCapitoli().get(28); i++) caps.add(String.valueOf(i));
                break;
            case "Amos":
                for (int i = 1; i<=nuovo.getCapitoli().get(29); i++) caps.add(String.valueOf(i));
                break;
            case "Abdia":
                for (int i = 1; i<=nuovo.getCapitoli().get(30); i++) caps.add(String.valueOf(i));
                break;
            case "Giona":
                for (int i = 1; i<=nuovo.getCapitoli().get(31); i++) caps.add(String.valueOf(i));
                break;
            case "Michea":
                for (int i = 1; i<=nuovo.getCapitoli().get(32); i++) caps.add(String.valueOf(i));
                break;
            case "Naum":
                for (int i = 1; i<=nuovo.getCapitoli().get(33); i++) caps.add(String.valueOf(i));
                break;
            case "Abacuc":
                for (int i = 1; i<=nuovo.getCapitoli().get(34); i++) caps.add(String.valueOf(i));
                break;
            case "Sofonia":
                for (int i = 1; i<=nuovo.getCapitoli().get(35); i++) caps.add(String.valueOf(i));
                break;
            case "Aggeo":
                for (int i = 1; i<=nuovo.getCapitoli().get(36); i++) caps.add(String.valueOf(i));
                break;
            case "Zaccaria":
                for (int i = 1; i<=nuovo.getCapitoli().get(37); i++) caps.add(String.valueOf(i));
                break;
            case "Malachia":
                for (int i = 1; i<=nuovo.getCapitoli().get(38); i++) caps.add(String.valueOf(i));
                break;
            case "Matteo":
                for (int i = 1; i<=nuovo.getCapitoli().get(39); i++) caps.add(String.valueOf(i));
                break;
            case "Marco":
                for (int i = 1; i<=nuovo.getCapitoli().get(40); i++) caps.add(String.valueOf(i));
                break;
            case "Luca":
                for (int i = 1; i<=nuovo.getCapitoli().get(41); i++) caps.add(String.valueOf(i));
                break;
            case "Giovanni":
                for (int i = 1; i<=nuovo.getCapitoli().get(42); i++) caps.add(String.valueOf(i));
                break;
            case "Atti":
                for (int i = 1; i<=nuovo.getCapitoli().get(43); i++) caps.add(String.valueOf(i));
                break;
            case "Romani":
                for (int i = 1; i<=nuovo.getCapitoli().get(44); i++) caps.add(String.valueOf(i));
                break;
            case "1 Corinti":
                for (int i = 1; i<=nuovo.getCapitoli().get(45); i++) caps.add(String.valueOf(i));
                break;
            case "2 Corinti":
                for (int i = 1; i<=nuovo.getCapitoli().get(46); i++) caps.add(String.valueOf(i));
                break;
            case "Galati":
                for (int i = 1; i<=nuovo.getCapitoli().get(47); i++) caps.add(String.valueOf(i));
                break;
            case "Efesini":
                for (int i = 1; i<=nuovo.getCapitoli().get(48); i++) caps.add(String.valueOf(i));
                break;
            case "Filippesi":
                for (int i = 1; i<=nuovo.getCapitoli().get(49); i++) caps.add(String.valueOf(i));
                break;
            case "Colossesi":
                for (int i = 1; i<=nuovo.getCapitoli().get(50); i++) caps.add(String.valueOf(i));
                break;
            case "1 Tessalonicesi":
                for (int i = 1; i<=nuovo.getCapitoli().get(51); i++) caps.add(String.valueOf(i));
                break;
            case "2 Tessalonicesi":
                for (int i = 1; i<=nuovo.getCapitoli().get(52); i++) caps.add(String.valueOf(i));
                break;
            case "1 Timoteo":
                for (int i = 1; i<=nuovo.getCapitoli().get(53); i++) caps.add(String.valueOf(i));
                break;
            case "2 Timoteo":
                for (int i = 1; i<=nuovo.getCapitoli().get(54); i++) caps.add(String.valueOf(i));
                break;
            case "Tito":
                for (int i = 1; i<=nuovo.getCapitoli().get(55); i++) caps.add(String.valueOf(i));
                break;
            case "Filemone":
                for (int i = 1; i<=nuovo.getCapitoli().get(56); i++) caps.add(String.valueOf(i));
                break;
            case "Ebrei":
                for (int i = 1; i<=nuovo.getCapitoli().get(57); i++) caps.add(String.valueOf(i));
                break;
            case "Giacomo":
                for (int i = 1; i<=nuovo.getCapitoli().get(58); i++) caps.add(String.valueOf(i));
                break;
            case "1 Pietro":
                for (int i = 1; i<=nuovo.getCapitoli().get(59); i++) caps.add(String.valueOf(i));
                break;
            case "2 Pietro":
                for (int i = 1; i<=nuovo.getCapitoli().get(60); i++) caps.add(String.valueOf(i));
                break;
            case "1 Giovanni":
                for (int i = 1; i<=nuovo.getCapitoli().get(61); i++) caps.add(String.valueOf(i));
                break;
            case "2 Giovanni":
                for (int i = 1; i<=nuovo.getCapitoli().get(62); i++) caps.add(String.valueOf(i));
                break;
            case "3 Giovanni":
                for (int i = 1; i<=nuovo.getCapitoli().get(63); i++) caps.add(String.valueOf(i));
                break;
            case "Giuda":
                for (int i = 1; i<=nuovo.getCapitoli().get(64); i++) caps.add(String.valueOf(i));
                break;
            case "Rivelazione":
                for (int i = 1; i<=nuovo.getCapitoli().get(65); i++) caps.add(String.valueOf(i));
                break;
            default:
                System.out.println("Sorry, you made an invalid choice.");
                break;
        }
    }
}

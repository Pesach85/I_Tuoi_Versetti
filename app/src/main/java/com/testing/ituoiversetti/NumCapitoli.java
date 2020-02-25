package com.testing.ituoiversetti;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Pasquale
 */
public class NumCapitoli extends Capitolo {
    ArrayList<Integer> caps = new ArrayList<Integer>();

    public NumCapitoli() throws IOException {
    }

    protected ArrayList<Integer> createCapN(String s) throws IOException {
        int val = new Bibbia().convertLibro(s);
        int max = new Capitolo().getCapitoli().get((val-1));
        for (int i = 0; i < max ; i++) {
            caps.add(i+1);
        }
      return caps;
    }
}

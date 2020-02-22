package com.testing.ituoiversetti;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Pasquale Edmondo Lombardi under Open Source license. plombardi85@gmail.com
 */
public class Capitolo extends Bibbia {

    protected ArrayList<Integer> getCapitoli() {
        return capitoli;
    }

    private ArrayList<Integer> capitoli = new ArrayList<Integer>();
    Collection<Integer> capitolo;

    Capitolo() throws IOException {
      createNumCap();
    }

    protected void createNumCap() {
     capitolo = Arrays.asList(50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150, 31, 12, 8, 66,
                              52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4, 28, 16, 24, 21, 28, 16, 16, 13, 6, 6,
                              4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22);
     capitoli.addAll(capitolo);
    }



}
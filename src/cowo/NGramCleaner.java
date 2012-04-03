/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import java.util.Iterator;

/**
 *
 * @author C. Levallois
 */
public class NGramCleaner {

    static void cleanIt() {

        Clock nGramHousekeeping = new Clock("cleaning");


        Iterator<String> ITsetNGrams = Main.setNGrams.elementSet().iterator();
        while (ITsetNGrams.hasNext()) {
            String currNGram = ITsetNGrams.next();

            if (currNGram.length() >= Main.minWordLength
                    & currNGram.length() < 50
                    & Main.setNGrams.count(currNGram) > Main.occurrenceThreshold
                    ) {
                {
                    Main.freqSet.add(currNGram, Main.setNGrams.count(currNGram));

                }
            }
        }



        nGramHousekeeping.closeAndPrintClock();

    }
}

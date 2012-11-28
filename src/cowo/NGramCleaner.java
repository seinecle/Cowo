/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.Iterator;

/**
 *
 * @author C. Levallois
 */
public class NGramCleaner {

    static HashMultiset<String> cleanIt(Multiset<String> multisetNGramsOriginal) {

        Clock nGramHousekeeping = new Clock("cleaning ngrams: deleting n-grams less frequent than " + Controller.occurrenceThreshold + " and shorter than " + Controller.minWordLength + " characters.");

        HashMultiset<String> multisetToReturn = HashMultiset.create();
        Iterator<String> ITsetNGrams = multisetNGramsOriginal.elementSet().iterator();
        String currNGram;
        while (ITsetNGrams.hasNext()) {
            currNGram = ITsetNGrams.next();
//            if (currNGram.equals("working memory")) {
//                System.out.println("working memory in the cleaning!");
//            }
            if (currNGram.length() >= Controller.minWordLength
                    & multisetNGramsOriginal.count(currNGram) >= Controller.occurrenceThreshold) {
                multisetToReturn.add(currNGram, multisetNGramsOriginal.count(currNGram));
//                System.out.println(currNGram + ", " + Main.multisetNGrams.count(currNGram));
            }
        }


        nGramHousekeeping.addText("number of words after cleaning: " + multisetToReturn.elementSet().size());
        nGramHousekeeping.closeAndPrintClock();
        return multisetToReturn;
    }
}

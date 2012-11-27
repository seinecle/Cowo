/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * @author C. Levallois
 */
public class NGramDuplicatesCleaner {

    static HashMultiset<String> removeDuplicates(HashMultiset<String> setNGrams) {

        Clock deletingDuplicatesTime = new Clock("Deleting n-grams when they are already included in longer n-grams");

        deletingDuplicatesTime.addText("Example: it will remove \"United States of \" because \"United States of America\" exists and is quite frequent too");
        deletingDuplicatesTime.printText();
        
        HashMultiset multisetWords = HashMultiset.create();


        Iterator<Multiset.Entry<String>> itFreqList = setNGrams.entrySet().iterator();
        HashSet<String> wordsToBeRemoved = new HashSet();


        while (itFreqList.hasNext()) {
            Multiset.Entry<String> entry = itFreqList.next();
            String currWord = entry.getElement();
            int currWordCount = entry.getCount();
//                if ("consumers".equals(currWord)) {
//                    System.out.println("consumers");
//                }
            if (currWord.contains(" ")) {

                Iterator<Multiset.Entry<String>> itFreqList2 = setNGrams.entrySet().iterator();
                while (itFreqList2.hasNext()) {
                    Multiset.Entry<String> entry2 = itFreqList2.next();
                    String currWord2 = entry2.getElement();
                    int currWordCount2 = entry2.getCount();

                    if (!currWord.equals(currWord2)
                            // there is a parameter here which determines how frequent should "United States of America" be for "United States of" to be removed        
                            & currWord.contains(currWord2) & currWordCount * 2 > currWordCount2) {

                        //System.out.println(currWord + ", " + currWord2);
                        wordsToBeRemoved.add(currWord2);
                        //                                                            if ("health care".equals(currWord2)){
                        //                System.out.println(currWord);
                        //                                }

                    }
                }
            }
        }

        itFreqList = setNGrams.entrySet().iterator();
        while (itFreqList.hasNext()) {
            boolean toRemain;
            Multiset.Entry<String> entry = itFreqList.next();
            String currWord3 = entry.getElement();
            toRemain = wordsToBeRemoved.add(currWord3);

            //This line includes the condition for an important word to remain in the list of words, even if listed with stopwords.
            if (((toRemain & !Controller.setStopWords.contains(currWord3)) | Controller.setKeepWords.contains(currWord3)) & currWord3.length() >= Controller.minWordLength) {
                multisetWords.add(entry.getElement(), entry.getCount());
                Controller.setFreqWords.add(entry.getElement());
//                if ("consumers".equals(entry.getElement())){
//                    System.out.println("added to freqListFiltered: "+entry.getElement());
//                }

            }


        }



        deletingDuplicatesTime.addText("Number of words after removing redundant n-grams: " + Controller.setFreqWords.elementSet().size());
        deletingDuplicatesTime.closeAndPrintClock();
        
        return multisetWords;

    }
}

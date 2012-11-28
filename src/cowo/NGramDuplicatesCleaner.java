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

    static HashMultiset<String> removeDuplicates(Multiset<String> setNGrams) {

        Clock deletingDuplicatesTime = new Clock("Deleting n-grams when they are already included in longer n-grams");

        deletingDuplicatesTime.addText("Example: it will remove \"United States of \" because \"United States of America\" exists and is quite frequent too");
        deletingDuplicatesTime.printText();

        HashMultiset multisetWords = HashMultiset.create();


        Iterator<Multiset.Entry<String>> itFreqList = setNGrams.entrySet().iterator();
        HashSet<String> wordsToBeRemoved = new HashSet();
        Multiset.Entry<String> entry;
        Multiset.Entry<String> entry2;
        Iterator<Multiset.Entry<String>> itFreqList2;
        String currWord;
        int currWordCount;
        String currWord2;
        int currWordCount2;
        Clock loopOne = new Clock("loop one");
        while (itFreqList.hasNext()) {
            entry = itFreqList.next();
            currWord = entry.getElement().trim();
            currWordCount = entry.getCount();
//                if ("consumers".equals(currWord)) {
//                    System.out.println("consumers");
//                }
            if (currWord.contains(" ")) {

                itFreqList2 = setNGrams.entrySet().iterator();
                while (itFreqList2.hasNext()) {
                    entry2 = itFreqList2.next();
                    currWord2 = entry2.getElement();
                    currWordCount2 = entry2.getCount();

                    if (currWord.contains(currWord2) && !currWord.equals(currWord2)
                            // there is a parameter here which determines how frequent should "United States of America" be for "United States of" to be removed        
                            && currWordCount * 2 > currWordCount2) {

                        //System.out.println(currWord + ", " + currWord2);
                        wordsToBeRemoved.add(currWord2);
                        //                                                            if ("health care".equals(currWord2)){
                        //                System.out.println(currWord);
                        //                                }

                    }
                }
            }
        }
        loopOne.closeAndPrintClock();

        Clock loop2 = new Clock("loop2");
        itFreqList = setNGrams.entrySet().iterator();
        while (itFreqList.hasNext()) {
            boolean toRemain;
            entry = itFreqList.next();
            currWord = entry.getElement();
            toRemain = wordsToBeRemoved.add(currWord);

            //This line includes the condition for an important word to remain in the list of words, even if listed with stopwords.
            if (((toRemain & !Controller.setStopWords.contains(currWord)) | Controller.setKeepWords.contains(currWord)) & currWord.length() >= Controller.minWordLength) {
                multisetWords.add(entry.getElement(), entry.getCount());
//                if ("consumers".equals(entry.getElement())){
//                    System.out.println("added to freqListFiltered: "+entry.getElement());
//                }

            }


        }
        loop2.closeAndPrintClock();



        deletingDuplicatesTime.addText("Number of words after removing redundant n-grams: " + multisetWords.elementSet().size());
        deletingDuplicatesTime.closeAndPrintClock();

        return multisetWords;

    }
}

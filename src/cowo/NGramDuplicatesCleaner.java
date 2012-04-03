/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import com.google.common.collect.Multiset;
import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * @author C. Levallois
 */
public class NGramDuplicatesCleaner {

    static void removeDuplicates() {

        Clock deletingDuplicatesTime = new Clock("Deleting n-grams when they are already included in longer n-grams");

        System.out.println(
                "Example: it will remove \"United States of \" because \"United States of America\" exists and is quite frequent too");


        Iterator<Multiset.Entry<String>> itFreqList = Main.freqList.iterator();
        HashSet<String> wordsToBeRemoved = new HashSet();


        while (itFreqList.hasNext()) {
            Multiset.Entry<String> entry = itFreqList.next();
            String currWord = entry.getElement();
            int currWordCount = entry.getCount();
//                if ("consumers".equals(currWord)) {
//                    System.out.println("consumers");
//                }
            if (currWord.contains(" ")) {

                Iterator<Multiset.Entry<String>> itFreqList2 = Main.freqList.iterator();
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
        Iterator<Multiset.Entry<String>> itFreqList3 = Main.freqList.iterator();


        while (itFreqList3.hasNext()) {
            boolean toRemain;
            Multiset.Entry<String> entry = itFreqList3.next();
            String currWord3 = entry.getElement();
            toRemain = wordsToBeRemoved.add(currWord3);

            //This line includes the condition for an important word to remain in the list of words, even if listed with stopwords.
            if ((toRemain & !Main.setStopWords.contains(currWord3)) | Main.setKeepWords.contains(currWord3)) {
                Main.freqListFiltered.add(entry);
                Main.setFreqWords.add(entry.getElement());
//                if ("consumers".equals(entry.getElement())){
//                    System.out.println("added to freqListFiltered: "+entry.getElement());
//                }

            }


        }



        deletingDuplicatesTime.closeAndPrintClock();

    }
}

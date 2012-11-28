/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author C. Levallois adapted from
 * http://stackoverflow.com/questions/3656762/n-gram-generation-from-a-sentence
 */
public class NGramFinder {

    private static HashMultiset<String> freqSetN = HashMultiset.create();
    private static String[] words;
//    public static HashMultiset<String> multisetNGrams = HashMultiset.create();
    private static HashSet<String> setUniqueNGramsPerLine;
    private static HashMultiset<String> setAllNGramsPerLine;
    private static HashMultiset<String> multisetToReturn;

    public static HashMultiset<String> runIt(Map<Integer, String> mapofLines) {
        Clock extractingNGramsPerLine = new Clock("extracting ngrams");
        multisetToReturn = HashMultiset.create();

        for (Integer lineNumber : mapofLines.keySet()) {

            setAllNGramsPerLine = HashMultiset.create();
            setAllNGramsPerLine.addAll(run(mapofLines.get(lineNumber), Controller.maxgram));
//            if (mapofLines.get(lineNumber).contains("working memory")) {
//                System.out.println("alert!");
//            }


            //takes care of the binary counting. For the Alchemy API case, this happens in the AlchemyAPI extractor class
            if (Controller.binary) {
                setUniqueNGramsPerLine = new HashSet();
                setUniqueNGramsPerLine.addAll(setAllNGramsPerLine);
                multisetToReturn.addAll(setUniqueNGramsPerLine);
            } else {
                multisetToReturn.addAll(setAllNGramsPerLine);
            }

        }
        extractingNGramsPerLine.addText("number of unique terms after nGram detection: " + multisetToReturn.elementSet().size());
        extractingNGramsPerLine.closeAndPrintClock();
        return multisetToReturn;

    }

    public static void ngrams(int n, String str) {

        words = str.split(Controller.wordSeparator);
//            System.out.println(str);
//            System.out.println(words[0]);



        for (int i = 0; i < words.length - n + 1; i++) {
            freqSetN.add(concat(words, i, i + n, n));
        }


    }

    public static String concat(String[] words, int start, int end, int ngram) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append(i > start ? " " : "").append(words[i]);
        }
        return sb.toString();
    }

    public static Multiset<String> run(String toBeParsed, int nGram) {
        freqSetN.clear();

        for (int n = 1; n <= nGram; n++) {


            ngrams(n, toBeParsed);
        }
        //System.out.println(freqList.get(i));
        return freqSetN;
    }
}

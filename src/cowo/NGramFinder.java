/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author C. Levallois
 * adapted from http://stackoverflow.com/questions/3656762/n-gram-generation-from-a-sentence
 */
public class NGramFinder {

    private static HashMultiset<String> freqSetN = HashMultiset.create();
    private static String[] words;
    
//    public static HashMultiset<String> setNGrams = HashMultiset.create();
    public static HashSet<String> setUniqueNGramsPerLine;
    public static HashMultiset<String> setAllNGramsPerLine;
    
    
    
    
    public static void runIt(HashMap<Integer,String> mapofLines){
    Clock extractingNGramsPerLine = new Clock("extracting ngrams");


//
            for (Integer lineNumber : mapofLines.keySet()) {



                setAllNGramsPerLine = HashMultiset.create();

                setAllNGramsPerLine.addAll(run(mapofLines.get(lineNumber), Main.maxgram));

                //takes care of the binary counting. For the Alchemy API case, this happens in the AlchemyAPI extractor class
                if (Main.binary) {
                    setUniqueNGramsPerLine = new HashSet();
                    setUniqueNGramsPerLine.addAll(setAllNGramsPerLine);
                    Main.setNGrams.addAll(setUniqueNGramsPerLine);
                } else {
                    Main.setNGrams.addAll(setAllNGramsPerLine);
                }

            }

    extractingNGramsPerLine.closeAndPrintClock();
    
}
    

    public static void ngrams(int n, String str) {

            words = str.split(Main.wordSeparator);
//            System.out.println(str);
//            System.out.println(words[0]);
       
            

            for (int i = 0; i < words.length - n + 1; i++) {
                    freqSetN.add(concat(words, i, i + n, n));
                }

        
    }

    public static String concat(String[] words, int start, int end, int ngram) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++)
        {
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

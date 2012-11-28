/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author C. Levallois
 */
public class CooccurrencesCounter {

    Map<Integer, String> mapofLines;
    Multiset<String> setFreqWords;
    Multiset<String> ngramsInLine = ConcurrentHashMultiset.create();
    Multiset<String> multisetOcc = ConcurrentHashMultiset.create();
    Multiset<String> setCombinations = ConcurrentHashMultiset.create();
    HashMultiset<String> countTermsInDocs = HashMultiset.create();

    public CooccurrencesCounter(Map<Integer, String> mapOfLines, Multiset<String> multisetNGrams) {
        this.mapofLines = mapOfLines;
        this.setFreqWords = multisetNGrams;
    }

    public Multiset<String> launch() throws InterruptedException, IOException {

        //COUNTING IN HOW MANY DOCS EACH FREQUENT TERM OCCURS (FOR THE TD IDF MEASURE)
        Iterator<String> itIDF = setFreqWords.elementSet().iterator();

        while (itIDF.hasNext()) {
            String freqWordTerm = itIDF.next();
            Iterator<String> itLines = mapofLines.values().iterator();
            while (itLines.hasNext()) {
                if (itLines.next().contains(freqWordTerm)) {
                    countTermsInDocs.add(freqWordTerm);
                }

            }

        }

        for (Integer lineNumber : mapofLines.keySet()) {

            HashMap<String, Float> tdIDFScores = new HashMap();
            String currWords = mapofLines.get(lineNumber);
//                System.out.println("in the loop: " + currWords);
            int countTermsInThisDoc;
            if (Controller.useAAPI_Entity) {
                countTermsInThisDoc = currWords.split("\\|").length;
            } else {
                countTermsInThisDoc = currWords.split(Controller.wordSeparator).length;
//                    System.out.println("nb terms in this doc: " + countTermsInThisDoc);
            }


            if (countTermsInThisDoc < 2) {
                if (countTermsInThisDoc > 0) {
//                        System.out.println("term 1 in the currLine: " + currWords.split(wordSeparator)[0]);
                }
                System.out.println("breaking because just one word");

                continue;
            }


            if (currWords == null) {
                System.out.println("breaking because of null string!");
                continue;
            }

//                System.out.println("this is the line \"" + currWords + "\"");
//                currWords = TextCleaner.doBasicCleaning(currWords);
//                System.out.println("currWords after basic cleaning: " + currWords);
//                System.out.println("currWords:");
            if (currWords.equals("")) {
                System.out.println("breaking because of empty string!");
                continue;
            }


            Iterator<String> it3 = setFreqWords.elementSet().iterator();

            while (it3.hasNext()) {
                String currFreqTerm = it3.next();

                if (currWords.contains(currFreqTerm)) {
                    ngramsInLine.add(currFreqTerm);
//                        System.out.println("currFreqTerm matched is:" + currFreqTerm);

                    //snippet to find the count of a word in the current line
                    int lastIndex = 0;
                    int countTermInThisDoc = 0;
                    while (lastIndex != -1) {
                        lastIndex = currWords.indexOf(currFreqTerm, lastIndex);

                        if (lastIndex != -1) {
                            countTermInThisDoc++;
                            lastIndex++;
                        } else {
                            break;
                        }
                    }
//                        System.out.println("countTermInThisDoc: " + countTermInThisDoc);
                    //end snippet
                    if (Controller.useTDIDF) {
//                        System.out.println("countTermsInThisDoc: " + countTermsInThisDoc);
                        int countDocsInCorpus = Controller.numberOfDocs;
//                        System.out.println("countDocsInCorpus: " + countDocsInCorpus);
                        int countDocsContainingThisTerm = countTermsInDocs.count(currFreqTerm);
//                        System.out.println("countDocsContainingThisTerm: " + countDocsContainingThisTerm);
                        float tdIDFscore = (float) (((float) countTermInThisDoc / (float) countTermsInThisDoc) * (float) Math.log((double) countDocsInCorpus / (double) countDocsContainingThisTerm));
//                        System.out.println("tdIDFscore: " + tdIDFscore);

                        tdIDFScores.put(currFreqTerm, tdIDFscore);

                    }
//                        System.out.println(currFreqTerm);

                }

            }

            String arrayWords[] = new String[ngramsInLine.size()];

//                for (String element : arrayWords) {
//                    System.out.print(element+" ");
//                }
//                System.out.println("");
            if (arrayWords.length >= 2) {
                HashSet<String> setOcc = new HashSet();
                setOcc.addAll(new PerformCombinations(ngramsInLine.toArray(arrayWords)).call());

                Iterator<String> itOcc = setOcc.iterator();
                while (itOcc.hasNext()) {

                    String pairOcc = itOcc.next();
//                        System.out.println("current pair is:"+ pairOcc);
                    String[] pair = pairOcc.split(",");


                    if (pair.length == 2
                            & !pair[0].trim().equals(pair[1].trim()) & !pair[0].contains(pair[1]) & !pair[1].contains(pair[0])) {

                        if (Controller.useTDIDF) {
                            int weightOfThisEdge = Math.round(10000 * (float) ((float) tdIDFScores.get(pair[0]) + (float) tdIDFScores.get(pair[1])));
//                            System.out.println(weightOfThisEdge);
                            multisetOcc.add(pairOcc, weightOfThisEdge);
                        } else {
                            multisetOcc.add(pairOcc, (ngramsInLine.count(pair[0]) + ngramsInLine.count(pair[1])));

                        }

                    }

                }
                setCombinations.addAll(multisetOcc);
            }
            ngramsInLine.clear();
            multisetOcc.clear();

        }
        return setCombinations;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import com.google.common.collect.HashMultiset;

/**
 *
 * @author C. Levallois
 */
public final class StopWordsRemover {

    //private final Entry<String> entry;
    private String entryWord;
    boolean multipleWord;
    //private SnowballStemmer stemmer;
    //private static String lang = "english";
    private final int entryCount;
    private HashMultiset<String> multisetToReturn;
    //static int numberOfThreads = Runtime.getRuntime().availableProcessors();
    //static int numberOfThreads = 7;
    //private static ExecutorService pool = Executors.newCachedThreadPool();

//    StopWordsRemoverWT(Entry<String> entry) {
//
//        this.entry = entry;
//        this.entryWord = entry.getElement().toLowerCase().trim();
//        this.entryCount = entry.getCount();
//        //run();
//    }
    StopWordsRemover(String element, int entryCount) {
        this.entryWord = element.replaceAll(" +", " ");
        this.entryCount = entryCount;
        this.multisetToReturn = HashMultiset.create();


    }

    //@Override
    public HashMultiset<String> call() {

        boolean write = true;


        if (Controller.useScientificStopWords) {
            multipleWord = entryWord.contains(" ");


            if (multipleWord) {
                String[] wordsNGrams = entryWord.split(" ");



                for (int n = 0; n < wordsNGrams.length; n++) {

                    if (wordsNGrams[n].length() < Controller.minWordLength) {
                        write = false;
                        break;
                    }

                }

                if (wordsNGrams.length == 2
                        && ((Controller.setStopWordsScientificOrShort.contains(wordsNGrams[0].toLowerCase().trim())
                        || Controller.setStopWordsScientificOrShort.contains(wordsNGrams[1].toLowerCase().trim())))) {
                    write = false;

                }

                if (wordsNGrams.length > 2) {
                    int scoreGarbage = 0;

                    for (int i = 0; i < wordsNGrams.length; i++) {

                        if ((i == 0 | i == (wordsNGrams.length - 1)) & Controller.setStopWordsScientificOrShort.contains(wordsNGrams[i].toLowerCase().trim())) {
                            scoreGarbage = Controller.maxAcceptedGarbage + 1;
                            continue;
                        }


                        if (Controller.setStopWordsShort.contains(wordsNGrams[i].toLowerCase().trim())) {
                            scoreGarbage = scoreGarbage + 3;
                            continue;
                        }

                        if (Controller.setStopWordsScientific.contains(wordsNGrams[i].toLowerCase().trim())) {
                            scoreGarbage = scoreGarbage + 2;
                            continue;
                        }

                    }

                    if (Controller.setStopWords.contains(entryWord)) {
                        scoreGarbage = Controller.maxAcceptedGarbage + 1;
                    }

                    //                    if (Main.setStopWordsShort.contains(wordsNGrams[0].toLowerCase().trim())
//                            || (Main.setStopWordsShort.contains(wordsNGrams[1].toLowerCase().trim()) & Main.setStopWordsShort.contains(wordsNGrams[2].toLowerCase().trim()))
//                            || (Main.setStopWords.contains(wordsNGrams[0].toLowerCase().trim())& Main.setStopWordsShort.contains(wordsNGrams[1].toLowerCase().trim()) && Main.setStopWords.contains(wordsNGrams[2].toLowerCase().trim()))
//                            || (Main.setStopWordsShort.contains(wordsNGrams[wordsNGrams.length - 1].toLowerCase().trim()))) {
                    if (scoreGarbage > Controller.maxAcceptedGarbage) {

                        write = false;
                    }
                }






            } else {
//                System.out.println("single word!");
//                    Future<String> result = pool.submit(new StanfordLemmatization(entryWord));
//                    entryWord = result.get();
                //entryWord = new StanfordLemmatization(entryWord).call();

                if (Controller.setStopWords.contains(entryWord) & !Controller.setKeepWords.contains(entryWord)) {

                    write = false;


                }
            }


            if (Controller.setKeepWords.contains(entryWord)) {
                write = true;
            }

        } else {
            String[] wordsNGrams = entryWord.split(" ");
            for (int i = 0; i < wordsNGrams.length; i++) {
                if (Controller.setStopWords.contains(wordsNGrams[i])) {
                    write = false;
                }
            }
        } //end of else block       

        if (write) {
            multisetToReturn.add(entryWord, entryCount);
//            System.out.println("term added!");
//                if ("risk".equals(entryWord)){
//                    System.out.println("risk added to filteredFreqSet "+entryCount);
//                }


//                return toReturn;
        }

        return multisetToReturn;
    }
}
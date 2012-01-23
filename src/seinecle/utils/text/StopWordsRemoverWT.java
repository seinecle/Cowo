/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package seinecle.utils.text;

import java.util.concurrent.ExecutionException;

/**
 *
 * @author C. Levallois
 */
public final class StopWordsRemoverWT {

    //private final Entry<String> entry;
    private String entryWord;
    boolean multipleWord;
    //private SnowballStemmer stemmer;
    //private static String lang = "english";
    private final int entryCount;
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
    StopWordsRemoverWT(String element, int entryCount) throws InterruptedException, ExecutionException {
        this.entryWord = element;
        this.entryCount = entryCount;


        call();

    }

    //@Override
    public void call() throws InterruptedException, ExecutionException {

        boolean write = true;

        //System.out.println(Main.counter);

        try {

            multipleWord = entryWord.contains(" ");



            if (multipleWord) {

                String[] wordsNGrams = entryWord.split(" ");



                for (int n = 0; n < wordsNGrams.length; n++) {
                    //System.out.println(wordsNGrams[n]);

                    if (wordsNGrams[n].length() < Main.minWordLength) {
                        write = false;
                        break;
                    }

                }

                if (wordsNGrams.length == 2
                        && (Main.setStopWordsShort.contains(wordsNGrams[0].toLowerCase().trim())
                        || Main.setStopWordsShort.contains(wordsNGrams[1].toLowerCase().trim()))) {
                    write = false;

                }

                if (wordsNGrams.length > 2) {


                    if (Main.setStopWordsShort.contains(wordsNGrams[0].toLowerCase().trim())
                            || (Main.setStopWordsShort.contains(wordsNGrams[1].toLowerCase().trim()) & Main.setStopWordsShort.contains(wordsNGrams[2].toLowerCase().trim()))
                            || (Main.setStopWords.contains(wordsNGrams[0].toLowerCase().trim())& Main.setStopWordsShort.contains(wordsNGrams[1].toLowerCase().trim()) && Main.setStopWords.contains(wordsNGrams[2].toLowerCase().trim()))
                            || (Main.setStopWordsShort.contains(wordsNGrams[wordsNGrams.length - 1].toLowerCase().trim()))) {
                        write = false;
                    }


                }






            } else {

//                    Future<String> result = pool.submit(new StanfordLemmatization(entryWord));
//                    entryWord = result.get();
                //entryWord = new StanfordLemmatization(entryWord).call();

                if (Main.setStopWords.contains(entryWord) & !Main.setKeepWords.contains(entryWord)) {

                    write = false;


                }
            }


        } catch (IndexOutOfBoundsException e) {
            System.out.println("problem: " + entryWord);
            write = false;

        }



        if (write) {
            synchronized (Main.filteredFreqSet) {
                Main.filteredFreqSet.add(entryWord, entryCount);

            }

//                return toReturn;
        }
    }
}
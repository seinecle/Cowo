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
public final class GUIStopWordsRemoverWT {

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
    GUIStopWordsRemoverWT(String element, int entryCount) throws InterruptedException, ExecutionException {
        this.entryWord = element;
        this.entryCount = entryCount;


        call();

    }

    //@Override
    public void call() throws InterruptedException, ExecutionException {

        boolean write = true;

        //System.out.println(GUIMain.counter);

        try {

            multipleWord = entryWord.contains(" ");



            if (multipleWord) {

                String[] wordsNGrams = entryWord.split(" ");



                for (int n = 0; n < wordsNGrams.length; n++) {
                    //System.out.println(wordsNGrams[n]);

                    if (wordsNGrams[n].length() < GUIMain.minWordLength) {
                        write = false;
                        break;
                    }

                }

                if (wordsNGrams.length == 2
                        && (
                        
                            (GUIMain.setStopWordsScientificOrShort.contains(wordsNGrams[0].toLowerCase().trim())
                                || GUIMain.setStopWordsScientificOrShort.contains(wordsNGrams[1].toLowerCase().trim()))
                            )
                   )
                    
                         {
                    write = false;

                }

                if (wordsNGrams.length > 2) {


                    if (GUIMain.setStopWordsShort.contains(wordsNGrams[0].toLowerCase().trim())
                            || (GUIMain.setStopWordsShort.contains(wordsNGrams[1].toLowerCase().trim()) & GUIMain.setStopWordsShort.contains(wordsNGrams[2].toLowerCase().trim()))
                            || (GUIMain.setStopWords.contains(wordsNGrams[0].toLowerCase().trim())& GUIMain.setStopWordsShort.contains(wordsNGrams[1].toLowerCase().trim()) && GUIMain.setStopWords.contains(wordsNGrams[2].toLowerCase().trim()))
                            || (GUIMain.setStopWordsShort.contains(wordsNGrams[wordsNGrams.length - 1].toLowerCase().trim()))) {
                        write = false;
                    }


                }






            } else {

//                    Future<String> result = pool.submit(new StanfordLemmatization(entryWord));
//                    entryWord = result.get();
                //entryWord = new StanfordLemmatization(entryWord).call();

                if (GUIMain.setStopWords.contains(entryWord) & !GUIMain.setKeepWords.contains(entryWord)) {

                    write = false;


                }
            }


        } catch (IndexOutOfBoundsException e) {
            System.out.println("problem: " + entryWord);
            write = false;

        }



        if (write) {
            synchronized (GUIMain.filteredFreqSet) {
                GUIMain.filteredFreqSet.add(entryWord, entryCount);

            }

//                return toReturn;
        }
    }
}
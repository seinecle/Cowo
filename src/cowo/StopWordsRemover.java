/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

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
    StopWordsRemover(String element, int entryCount)  {
        this.entryWord = element;
        this.entryCount = entryCount;


        call();

    }

    //@Override
    public void call()  {

        boolean write = true;

        //System.out.println(Main.counter);

        if (!Main.ownStopWords.equals("nothing")) {
            String[] wordsNGrams = entryWord.split(" ");
            for (int i = 0; i < wordsNGrams.length; i++) {
                if (Main.setStopWords.contains(wordsNGrams[i])) {
                    write = false;
                }
            }

        } else {
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
                            && ((Main.setStopWordsScientificOrShort.contains(wordsNGrams[0].toLowerCase().trim())
                            || Main.setStopWordsScientificOrShort.contains(wordsNGrams[1].toLowerCase().trim())))) {
                        write = false;

                    }

                    if (wordsNGrams.length > 2) {
                        int scoreGarbage = 0;

                        for (int i = 0; i < wordsNGrams.length; i++) {

                            if ((i == 0 | i == (wordsNGrams.length - 1)) & Main.setStopWordsScientificOrShort.contains(wordsNGrams[i].toLowerCase().trim())) {
                                scoreGarbage = Main.maxAcceptedGarbage + 1;
                                continue;
                            }


                            if (Main.setStopWordsShort.contains(wordsNGrams[i].toLowerCase().trim())) {
                                scoreGarbage = scoreGarbage + 3;
                                continue;
                            }

                            if (Main.setStopWordsScientific.contains(wordsNGrams[i].toLowerCase().trim())) {
                                scoreGarbage = scoreGarbage + 2;
                                continue;
                            }





                        }

                        if (Main.setStopWords.contains(entryWord)) {
                            scoreGarbage = Main.maxAcceptedGarbage + 1;
                        }

                        //                    if (Main.setStopWordsShort.contains(wordsNGrams[0].toLowerCase().trim())
//                            || (Main.setStopWordsShort.contains(wordsNGrams[1].toLowerCase().trim()) & Main.setStopWordsShort.contains(wordsNGrams[2].toLowerCase().trim()))
//                            || (Main.setStopWords.contains(wordsNGrams[0].toLowerCase().trim())& Main.setStopWordsShort.contains(wordsNGrams[1].toLowerCase().trim()) && Main.setStopWords.contains(wordsNGrams[2].toLowerCase().trim()))
//                            || (Main.setStopWordsShort.contains(wordsNGrams[wordsNGrams.length - 1].toLowerCase().trim()))) {
                        if (scoreGarbage > Main.maxAcceptedGarbage) {

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




            if (Main.setKeepWords.contains(entryWord)) {
                write = true;
            }

        } //end of else block       

        if (write) {
            synchronized (Main.filteredFreqSet) {
                Main.filteredFreqSet.add(entryWord, entryCount);
//                if ("risk".equals(entryWord)){
//                    System.out.println("risk added to filteredFreqSet "+entryCount);
//                }

            }

//                return toReturn;
        }
    }
}
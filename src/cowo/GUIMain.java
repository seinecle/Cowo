/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import GUI.Screen1;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.openide.util.Exceptions;

/**
 *
 * @author C. Levallois
 *
 * FUNCTION OF THIS PROGRAM: Take a text as input, returns semantic networks as
 * an output.
 *
 * DETAIL OF OPERATIONS: 1. loads several stopwords files in memory 2. reads the
 * text file and does some housekeeping on it 3. lemmatization of the text 4.
 * extracts n-grams 5. housekeeping on n-grams, removal of least frequent terms
 * 6. removal of stopwords, removal of least frequent terms 7. removal of
 * redudant n-grams (as in: removal of "united states of" if "united stated of
 * America" exists frequently enough 8. determines all word co-occurrences for
 * each line of the text 9. prints vosViewer output 10. prints GML file 11.
 * print a short report of all these operations
 */
public class GUIMain implements Runnable {

    public static String currLine = "";
    public static Multiset<String> freqSet = ConcurrentHashMultiset.create();
    //public static Multiset<Pair<String, String>> wordsPerLine = ConcurrentHashMultiset.create();
    public static LinkedHashMultimap<Integer, String> wordsPerLine = LinkedHashMultimap.create();
    public static LinkedHashMultimap<Integer, String> wordsPerLineFiltered = LinkedHashMultimap.create();
    public static HashSet<String> setOfWords = new HashSet();
    public static HashMultiset<String> setNGrams = HashMultiset.create();
    public static HashSet<String> setUniqueNGramsPerLine;
    public static HashMultiset<String> setAllNGramsPerLine;
    public static Multiset<String> multisetOfWords = ConcurrentHashMultiset.create();
    public static Multiset<String> multisetOcc = ConcurrentHashMultiset.create();
    public static Multiset<String> filteredFreqSet = ConcurrentHashMultiset.create();
    public static Multiset<String> ngramsInLine = ConcurrentHashMultiset.create();
    public static HashMap<String, Integer> ngramsCountinCorpus = new HashMap();
    public static Multiset<String> setCombinations = ConcurrentHashMultiset.create();
    public static Multiset<String> future = ConcurrentHashMultiset.create();
    public static List<Entry<String>> freqList;
    public static List<Entry<String>> freqListFiltered = new ArrayList();
    public static String[] stopwords;
    public static int occurrenceThreshold = 4;
    private static FileReader fr;
    private static int maxgram = 4;
    private final static int nbStopWords = 5000;
    private final static int nbStopWordsShort = 200;
    public final static int maxAcceptedGarbage = 3;
    //static int numberOfThreads = Runtime.getRuntime().availableProcessors();
    static int numberOfThreads = 7;
    private static Integer counterLines = 0;
    // logic of freqThreshold: the higher the number of stopwords filtered out, the lower the number of significant words which should be expected
    private static int freqThreshold = 400;
    private String wk;
    public static String wkOutput;
    private String textFileName;
    private String textFile;
    static String cleanWord;
    public static int counter = 0;
    private static int numberOfDocs;
    private static BufferedReader fileStopWords;
    private static BufferedReader fileKeepWords;
    private static BufferedReader fileStopWords2;
    private static String[] stopwordsSeinecle;
    private static BufferedReader fileStopWords4;
    public static String[] stopwordsShort;
    private static String[] stopwordsScientific;
    public static Set<String> setStopWords = new HashSet();
    public static Set<String> setStopWordsScientific = new HashSet();
    public static Set<String> setStopWordsScientificOrShort = new HashSet();
    public static Set<String> setNoLemma = new HashSet();
    public static int minWordLength = 3;
    private static HashMap<Integer, String> mapofLines = new HashMap();
    private static HashSet<String> setFreqWords = new HashSet();
    private static String fileMapName;
    private static BufferedWriter fileMapFile;
    private static String fileNetworkName;
    private static BufferedWriter fileNetworkFile;
    private static String fileParametersName;
    private static BufferedWriter fileParametersFile;
    public static Set<String> setStopWordsShort = new HashSet();
    public static Set<String> setStopWordsSeinecle = new HashSet();
    public static Set<String> setKeepWords = new HashSet();
    private static BufferedReader fileNoLemma;
    private static String[] noLemmaArray;
    public static String[] keepWordsArray;
    static InputStream in10000 = Main.class.getResourceAsStream("stopwords_10000_most_frequent_filtered.txt");
    static InputStream inscientific = Main.class.getResourceAsStream("scientificstopwords.txt");
    static InputStream inseinecle = Main.class.getResourceAsStream("stopwords_seinecle.txt");
    static InputStream inkeep = Main.class.getResourceAsStream("stopwords_tokeep.txt");
    static InputStream innolemma = Main.class.getResourceAsStream("nolemmatization.txt");
    public static String ownStopWords;
    public static String fileGMLName;
    private static BufferedWriter fileGMLFile;
    private static boolean binary = true;
    static private boolean filterDifficultChars = true;

    public GUIMain(String wkGUI, String textFileGUI, String textFileNameGUI, String binaryYes, String freqThreshold, String minWordLength,String maxgram, String occurrenceThreshold,String ownStopWords,String filterDifficultChars) {

        textFile = textFileGUI;
        wk = wkGUI;
        textFileName = textFileNameGUI;
        if (!"true".equals(binaryYes)){
            GUIMain.binary = !GUIMain.binary;}
        GUIMain.freqThreshold = Integer.valueOf(freqThreshold);
        GUIMain.minWordLength = Integer.valueOf(minWordLength);
        GUIMain.maxgram = Integer.valueOf(maxgram);
        GUIMain.occurrenceThreshold = Integer.valueOf(occurrenceThreshold);
        GUIMain.ownStopWords = ownStopWords;
        if (!"true".equals(filterDifficultChars)){
            GUIMain.filterDifficultChars = !GUIMain.filterDifficultChars;}

    }

    @Override
    public void run() {
        try {
            wkOutput = wk.concat("\\");


            System.out.println("---------------------------------");
            System.out.println();

            // #### 1. LOADING FILES CONTAINING STOPWORDS
            // Several sources of stopfiles are used.
            // Once transformed in array, they will be invoked by the StopWordsRemoverRT class

            Clock loadingStopWordsTime = new Clock("Loading the list of stopwords");
            fileStopWords = new BufferedReader(new InputStreamReader(in10000));
            // Once transformed in array, they will be invoked by the StopWordsRemoverRT class
            fileStopWords2 = new BufferedReader(new InputStreamReader(inseinecle));
            fileStopWords4 = new BufferedReader(new InputStreamReader(inscientific));
            fileKeepWords = new BufferedReader(new InputStreamReader(inkeep));
            fileNoLemma = new BufferedReader(new InputStreamReader(innolemma));
            stopwords = fileStopWords.readLine().split(",");
            noLemmaArray = fileNoLemma.readLine().split(",");
            keepWordsArray = fileKeepWords.readLine().split(",");
            stopwordsSeinecle = fileStopWords2.readLine().split(",");
            stopwordsScientific = fileStopWords4.readLine().split(",");
            stopwords = Arrays.copyOf(stopwords, nbStopWords);
            stopwordsShort = Arrays.copyOf(stopwords, nbStopWordsShort);
            stopwords = ArrayUtils.addAll(stopwords, stopwordsSeinecle);
            stopwords = ArrayUtils.addAll(stopwords, stopwordsScientific);

            setStopWords.addAll(Arrays.asList(stopwords));
            setStopWordsScientific.addAll(Arrays.asList(stopwordsScientific));
            setStopWordsSeinecle.addAll(Arrays.asList(stopwordsSeinecle));
            setNoLemma.addAll(Arrays.asList(noLemmaArray));
            setKeepWords.addAll(Arrays.asList(keepWordsArray));
            setStopWordsShort.addAll(Arrays.asList(stopwordsShort));
            setStopWordsScientificOrShort.addAll(setStopWordsScientific);
            setStopWordsScientificOrShort.addAll(setStopWordsShort);
            setStopWordsScientificOrShort.addAll(setStopWordsSeinecle);

            fileStopWords.close();
            fileStopWords2.close();
            fileStopWords4.close();
            fileNoLemma.close();
            fileKeepWords.close();
            
            if (!ownStopWords.equals("nothing")){
                
            fileStopWords = new BufferedReader(new FileReader(ownStopWords));
            stopwords = fileStopWords.readLine().split(",");
            setStopWords.addAll(Arrays.asList(stopwords));
                
            }

            loadingStopWordsTime.closeAndPrintClock();
            //-------------------------------------------------------------------------------------------------------------







            // ### 2. LOADING FILE IN MEMORY AND CLEANING  ...

            Clock loadingAndLemmatizingTime = new Clock("Loading text file: " + textFile + "\nCleaning a bit and lemmatizing");

            fr = new FileReader(textFile);
            BufferedReader br = new BufferedReader(fr);
            StringBuilder sb = new StringBuilder();
            while ((currLine = br.readLine()) != null) {
                currLine = currLine.toLowerCase().trim().replaceAll("http[^ ]* ", " ").trim();
                currLine = currLine.toLowerCase().trim().replaceAll("&amp;", "and").trim();

                currLine = currLine.replaceAll("\\(i\\)", " ");
                currLine = currLine.replaceAll("\\(ii\\)", " ");
                currLine = currLine.replaceAll("\\(iii\\)", " ");
                if(filterDifficultChars){
                currLine = currLine.toLowerCase().trim().replaceAll("\\p{C}", " ").trim();
                currLine = currLine.replaceAll("’", "'");
                currLine = currLine.replaceAll("[^A-Za-z']", " ").trim();}
                //currLine = currLine.replaceAll("[^A-Za-z'éèàç$êëï]", " ").trim();
                currLine = currLine.replaceAll(" +", " ");

                currLine = currLine.replaceAll("orbitofrontal cortex (ofc)", "orbitofrontal cortex");
                currLine = currLine.replaceAll("ofc", "orbitofrontal cortex");
                currLine = currLine.replaceAll("medial prefrontal cortex (mpfc)", "medial prefrontal cortex");
                currLine = currLine.replaceAll("medial prefrontal cortex (mpfc)", "medial prefrontal cortex");
                currLine = currLine.replaceAll("dorsolateral prefrontal cortex (dpfc)", "dorsolateral prefrontal cortex");
                currLine = currLine.replaceAll("dpfc", "dorsolateral prefrontal cortex");
                currLine = currLine.replaceAll("dorsolateral prefrontal cortex (dlpfc)", "dorsolateral prefrontal cortex");
                currLine = currLine.replaceAll("dlpfc", "dorsolateral prefrontal cortex");
                currLine = currLine.replaceAll(" anterior cingulate cortex (acc) ", " anterior cingulate cortex ");
                currLine = currLine.replaceAll(" acc ", " anterior cingulate cortex ");
                currLine = currLine.replaceAll(" behavioural ", "behavorial");
                currLine = currLine.replaceAll(" skin conductance response (scr) ", " skin conductance response ");
                currLine = currLine.replaceAll(" scr ", " skin conductance response ");
                currLine = currLine.replaceAll(" prefrontal cortex (pfc) ", " prefrontal cortex ");
                currLine = currLine.replaceAll(" pfc ", " prefrontal cortex ");
                currLine = currLine.replaceAll(" ventromedial prefrontal cortex (vmpfc) ", " ventromedial prefrontal cortex ");
                currLine = currLine.replaceAll(" vmpfc ", " ventromedial prefrontal cortex ");
                currLine = currLine.replaceAll(" behavioral ", " behavior ");
                currLine = currLine.replaceAll(" functional magnetic resonance imaging (fmri)", " fmri ");
                currLine = currLine.replaceAll(" functional magnetic resonance imaging ", " fmri ");
                currLine = currLine.replaceAll(" attention deficit hyperactivity disorder (adhd)", " adhd ");
                currLine = currLine.replaceAll(" attention deficit hyperactivity disorder ", " adhd ");
                currLine = currLine.replaceAll(" obsessive compulsive disorder (ocd)", " obsessive compulsice disorder ");
                currLine = currLine.replaceAll(" odc ", " obsessive compulsive disorder ");
                currLine = currLine.replaceAll(" risky ", " risk ");
                counterLines++;
                System.out.println(currLine);
                ArrayList<String> wordsOfLine = new ArrayList();
                wordsOfLine.addAll(Arrays.asList(currLine.split(" ")));



                //          ### 3. BASIC LEMMATIZATION: turns some common plural forms to their singular form)
                //                  -> Makes use of nolemmatization.txt file, which contains terms ending with an s which should not be trasnformed.



                Iterator<String> itwl = wordsOfLine.iterator();
                StringBuilder sbWords = new StringBuilder();


                while (itwl.hasNext()) {
                    String currEntry = itwl.next().trim().toLowerCase();

//                    if ("consumers".equals(currEntry))
//                        System.out.println("consumers");


                    if (currEntry.endsWith("ies")) {
                        if (!setNoLemma.contains(currEntry)) {
                            currEntry = currEntry.substring(0, currEntry.length() - 3) + "y";

                        }
                    } else if (currEntry.endsWith("'s")) {
                        currEntry = currEntry.substring(0, currEntry.length() - 2);


                    } else if ((currEntry.endsWith("s") | currEntry.endsWith("s'"))
                            && !currEntry.endsWith("us")
                            && !currEntry.endsWith("as")
                            && !currEntry.endsWith("ss")
                            && !setNoLemma.contains(currEntry)
                            && !currEntry.endsWith("is")) {
                        if (currEntry.endsWith("s")) {
                            currEntry = currEntry.substring(0, currEntry.length() - 1);
                        }
                        if (currEntry.endsWith("s'")) {
                            currEntry = currEntry.substring(0, currEntry.length() - 2);
                        }

                    } else if (currEntry.endsWith("'")) {
                        currEntry = currEntry.substring(0, currEntry.length() - 1);
                    }

                    sbWords.append(currEntry.trim()).append(" ");
//                    if ("consumers".equals(currEntry))
//                        System.out.println("consumers even after lemmatization!");

                } // end looping through all words of the line which is currently read
                mapofLines.put(counterLines, sbWords.toString().trim());
                sb.append(sbWords.toString().trim());
                sbWords = null;



            } // end looping through all lines of the original text file


            numberOfDocs = counterLines;

            fr.close();

            br.close();

            loadingAndLemmatizingTime.closeAndPrintClock();


            //-------------------------------------------------------------------------------------------------------------

            // ### 4. EXTRACTING N-GRAMS


//            Clock extractingNGrams = new Clock("Extracting n-grams");
//
//            HashMultiset<String> setNGrams = HashMultiset.create();
//
//            setNGrams.addAll(NGramFinder.run(sb.toString(), maxgram));
//
//            sb = null;
//
//            extractingNGrams.closeAndPrintClock();


            //-------------------------------------------------------------------------------------------------------------

            // ### 4 bis. EXTRACTING set of NGrams, optionally unique N-GRAMS per Line.


            Clock extractingNGramsPerLine = new Clock("Extracting n-grams");
            HashMap<Integer, HashSet<String>> mapOfLinesNGrammed = new HashMap();

            for (Integer lineNumber : mapofLines.keySet()) {


                setUniqueNGramsPerLine = new HashSet();
                setAllNGramsPerLine = HashMultiset.create();

                setAllNGramsPerLine.addAll(NGramFinder.run(mapofLines.get(lineNumber), maxgram));
                setUniqueNGramsPerLine.addAll(setAllNGramsPerLine);

                if (binary) {
                    setNGrams.addAll(setUniqueNGramsPerLine);
                } else {
                    setNGrams.addAll(setAllNGramsPerLine);
                }



            }
            extractingNGramsPerLine.closeAndPrintClock();
            //-------------------------------------------------------------------------------------------------------------
            // ### 5. DELETING SMALL WORDS, TOO LONG WORDS AND WORDS WITH TOO MANY SPACES IN THEM


            Clock nGramHousekeeping = new Clock("Housekeeping on n-grams and removal of least frequent terms");


            for (Entry<String> entry : setNGrams.entrySet()) {


                if (entry.getElement().length() >= minWordLength
                        & entry.getElement().length() < 50
                        & StringUtils.countMatches(entry.getElement(), " ") < maxgram) {
                    {
                        freqSet.add(entry.getElement(), entry.getCount());

                    }
                }
            }




            // #### FILTERS OUT LOW FREQUENCY TERMS IN freqSet
            Iterator<String> itFreqSet = freqSet.iterator();


            while (itFreqSet.hasNext()) {
                String curr = itFreqSet.next();
//                if ("risk".equals(curr)){
//                    
//                    System.out.println("risk");
//                    System.out.println(freqSet.count("risk"));
//                    
//                }

                if (freqSet.count(curr) < occurrenceThreshold) {
                    itFreqSet.remove();
                }
            }

            nGramHousekeeping.closeAndPrintClock();


            //-------------------------------------------------------------------------------------------------------------        

            // #### 6. REMOVING STOPWORDS
            // lines in comment can be uncommented for concurrency

            Clock stopwordsRemovalTime = new Clock("Removing stopwords");

            Iterator<Entry<String>> it = freqSet.entrySet().iterator();


            while (it.hasNext()) {
                counter++;
                Entry<String> entry = it.next();
//                if (entry.getElement().equals("consumers")) System.out.println("consumers");
                //Future<String> cleanWord = pool.submit(new StopWordsRemoverWT(entry.getElement()));
                //if (entry.getElement().equals("game")) System.out.println("game");

                new GUIStopWordsRemoverWT(entry.getElement().trim(), entry.getCount());
            }
            counter = 0;
            counterLines = 0;
            //pool.shutdown();
            //pool.awaitTermination(1, TimeUnit.SECONDS);







            // #### SORTS TERMS BY FREQUENCY, LEAVING OUT THE LESS FREQUENT ONE

            freqList = MultiSetSorter.sortMultisetPerEntryCount(filteredFreqSet);




            ListIterator<Entry<String>> li = freqList.listIterator(Math.min(freqThreshold, freqList.size()));


            while (li.hasNext()) {
                Entry<String> entry = li.next();
                li.remove();
            }


            stopwordsRemovalTime.closeAndPrintClock();

            //-------------------------------------------------------------------------------------------------------------   






            // #### 7. DELETES bi-grams trigrams and above, IF they are already contained in n+1 grams


            Clock deletingDuplicatesTime = new Clock("Deleting n-grams when they are already included in longer n-grams");

            System.out.println(
                    "Example: it will remove \"United States of \" because \"United States of America\" exists and is quite frequent too");


            Iterator<Entry<String>> itFreqList = freqList.iterator();
            HashSet<String> wordsToBeRemoved = new HashSet();


            while (itFreqList.hasNext()) {
                Entry<String> entry = itFreqList.next();
                String currWord = entry.getElement();
                int currWordCount = entry.getCount();
//                if ("consumers".equals(currWord)) {
//                    System.out.println("consumers");
//                }
                if (currWord.contains(" ")) {

                    Iterator<Entry<String>> itFreqList2 = freqList.iterator();
                    while (itFreqList2.hasNext()) {
                        Entry<String> entry2 = itFreqList2.next();
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
            Iterator<Entry<String>> itFreqList3 = freqList.iterator();


            while (itFreqList3.hasNext()) {
                boolean toRemain;
                Entry<String> entry = itFreqList3.next();
                String currWord3 = entry.getElement();
                toRemain = wordsToBeRemoved.add(currWord3);

                //This line includes the condition for an important word to remain in the list of words, even if listed with stopwords.
                if ((toRemain & !setStopWords.contains(currWord3)) | setKeepWords.contains(currWord3)) {
                    freqListFiltered.add(entry);
                    setFreqWords.add(entry.getElement());
//                if ("consumers".equals(entry.getElement())){
//                    System.out.println("added to freqListFiltered: "+entry.getElement());
//                }

                }


            }

            wordsToBeRemoved.clear();


            deletingDuplicatesTime.closeAndPrintClock();


            //-------------------------------------------------------------------------------------------------------------           

            // #### PRINTING MOST FREQUENT TERMS         
            StringBuilder mostFrequentTerms = new StringBuilder();
            String currFrequentTerm;
            for (int i = 0;
                    i < freqListFiltered.size()
                    && i < freqThreshold; i++) {
                currFrequentTerm = freqListFiltered.get(i).toString();
                System.out.println("most frequent words: " + currFrequentTerm);
                mostFrequentTerms.append("most frequent words: ").append(currFrequentTerm).append("\n");
                
            }
//    -------------------------------------------------------------------------------------------------------------  


            // #### 8. COUNTING CO-OCCURRENCES PER LINE


            Clock calculatingCooccurrencesTime = new Clock("Determining all word co-occurrences for each line of the text");


            for (Integer lineNumber : mapofLines.keySet()) {


                String currWords = mapofLines.get(lineNumber);
                Iterator<String> it3 = setFreqWords.iterator();



                while (it3.hasNext()) {
                    //int termCount = 0;
                    String currFreqTerm = it3.next();
                    //                if (currWords.contains("health care") & currFreqTerm.equals("health care")) {
                    //                    found = !found;
                    //                }
                    if (currWords.contains(currFreqTerm)) {

//                        if ("consumers".equals(currFreqTerm))
//                            System.out.println("consumers");
                        //                        if (currValue.equals(itFreqList4.next().getElement())) {
                        //System.out.println("currValue   " + currValue);
                        //docLength = currLine.split(" ").length;
                        //System.out.println("docLength  " + docLength);
                        //termCount = StringUtils.countMatches(currWords, currFreqTerm);
                        //System.out.println("FreqWord "+ currFreqTerm+ " found in current line. TermCount  " + termCount);
                        //termCountinCorpus = multisetOfWords.count(currValue);
                        //System.out.println("termCountinCorpus  " + termCountinCorpus);
                        //tdIDF = (termCount / docLength) * Math.log(numberOfDocs / termCountinCorpus);
                        //System.out.println("tdIDF  " + tdIDF);
                        //ngramsInLine.add(currFreqTerm.trim(), termCount);
                        ngramsInLine.add(currFreqTerm.trim());
                        //ngramsCountinCorpus.put(entry.getValue(), (int) termCountinCorpus);

                        //System.out.println("n-gram being added to the list of occurrences of line " + counterLines + ": " + entry.getValue());

                    }
                    //                    }

                }
                //                    }


                //System.out.println(ngramsInLine.size());
                //System.out.println(ngramsInLine.toString());

                String arrayWords[] = new String[ngramsInLine.size()];
                if (arrayWords.length > 2) {
                    HashSet<String> setOcc = new HashSet();
                    setOcc.addAll(new PerformCombinations(ngramsInLine.toArray(arrayWords)).call());

                    Iterator<String> itOcc = setOcc.iterator();
                    while (itOcc.hasNext()) {
                        boolean add = true;
                        String pairOcc = itOcc.next();

                        String[] pair = pairOcc.split(",");



                        if (!pair[0].trim().equals(pair[1].trim()) && !pair[0].contains(pair[1]) && !pair[1].contains(pair[0]) && add) //                                            System.out.println(pairOcc.toString());
                        //                                            System.out.println(pair[0]+" "+freqSet.count(pair[0]));
                        //                                            System.out.println(pair[1]+" "+freqSet.count(pair[1]));
                        //                                            System.out.println(pair[0]+" "+ngramsInLine.count(pair[0]));
                        //                                            System.out.println(pair[1]+" "+ngramsInLine.count(pair[1]));
                        //                                            System.out.println(pairOcc.toString()+" "+(ngramsInLine.count(pair[0]) + ngramsInLine.count(pair[1]))*10000000/(freqSet.count(pair[0])*freqSet.count(pair[1])));
                        {
                            multisetOcc.add(pairOcc, (ngramsInLine.count(pair[0]) + ngramsInLine.count(pair[1])));
                            // * 100*numberOfDocs/(freqSet.count(pair[0]) * freqSet.count(pair[1])));
                        }
                        //System.out.println(multisetOcc);

                    }
                    setCombinations.addAll(multisetOcc);
                }
                //System.out.println(setCombinations.toString());

                //System.out.println("Total number of co-occurring pairs: " + setCombinations.entrySet().size());
                ngramsInLine.clear();
                multisetOcc.clear();

            }
            calculatingCooccurrencesTime.closeAndPrintClock();

            //-------------------------------------------------------------------------------------------------------------                 
            Clock printingOutputTime = new Clock("Printing Vosviewer files, GML file, report file");
            //-------------------------------------------------------------------------------------------------------------          
            // #### 9. PRINTING VOS VIEWER OUTPUT        



            freqList.clear();
            freqList = MultiSetSorter.sortMultisetPerEntryCount(setCombinations);

            HashMap<String, Integer> id = new HashMap();
            HashSet<String> idSet = new HashSet();
            int counterIds = 0;
            fileMapName = StringUtils.substring(textFileName, 0, textFileName.length() - 4).concat("_VosViewer_map.txt");
            fileMapFile = new BufferedWriter(new FileWriter(wkOutput + fileMapName));
            StringBuilder mapSb = new StringBuilder();

            mapSb.append(
                    "label,id\n");

            // #### Creates the map of ids

            for (int i = 0;
                    i < freqList.size() //&& i < freqThreshold
                    ;
                    i++) {
                String[] edge = freqList.get(i).getElement().split(",");
                if (idSet.add(edge[0])) {
                    id.put(edge[0], counterIds++);
                    mapSb.append(edge[0]).append(", ").append(counterIds).append("\n");
                }
                if (idSet.add(edge[1])) {
                    id.put(edge[1], counterIds++);
                    mapSb.append(edge[1]).append(", ").append(counterIds).append("\n");
                }


            }

            fileMapFile.write(mapSb.toString());
            fileMapFile.flush();

            fileMapFile.close();
            mapSb = null;


            // #### Creates the Vosviewer network (edges) of ids

            fileNetworkName = StringUtils.substring(textFileName, 0, textFileName.length() - 4).concat("_VosViewer_network.txt");
            fileNetworkFile = new BufferedWriter(new FileWriter(wkOutput + fileNetworkName));
            System.out.println(wkOutput + fileNetworkName);
            StringBuilder networkSb = new StringBuilder();

            for (int i = 0;
                    i < freqList.size() //&& i < freqThreshold
                    ;
                    i++) {
                String[] edge = freqList.get(i).getElement().split(",");
                try {
                    networkSb.append(id.get(edge[0]) + 1).append(",").append(id.get(edge[1]) + 1).append(",").append(freqList.get(i).getCount()).append("\n");
                } catch (NullPointerException e) {
                }
            }

            fileNetworkFile.write(networkSb.toString());
            fileNetworkFile.flush();

            fileNetworkFile.close();
            networkSb = null;



            //-------------------------------------------------------------------------------------------------------------     
            // #### 10. PRINTING GML output        


            HashMap<String, Integer> idGML = new HashMap();
            HashSet<String> idSetGML = new HashSet();
            counterIds = 0;
            fileGMLName = StringUtils.substring(textFileName, 0, textFileName.length() - 4).concat(".gml");
            fileGMLFile = new BufferedWriter(new FileWriter(wkOutput + fileGMLName));
            StringBuilder GMLSb = new StringBuilder();

            GMLSb.append(
                    "graph [\n");

            // #### Creates the nodes

            for (int i = 0;
                    i < freqList.size() //&& i < freqThreshold
                    ;
                    i++) {
                String[] edge = freqList.get(i).getElement().split(",");
                if (idSetGML.add(edge[0])) {
                    idGML.put(edge[0], counterIds++);
                    GMLSb.append("node\n[\nid ").append(counterIds).append("\nlabel \"").append(edge[0]).append("\"\n]\n");
                }
                if (idSetGML.add(edge[1])) {
                    idGML.put(edge[1], counterIds++);
                    GMLSb.append("node\n[\nid ").append(counterIds).append("\nlabel \"").append(edge[1]).append("\"\n]\n");
                }


            }

            for (int i = 0;
                    i < freqList.size() //&& i < freqThreshold
                    ;
                    i++) {
                String[] edge = freqList.get(i).getElement().split(",");
                try {
                    GMLSb.append("edge\n[\nsource ").append(id.get(edge[0]) + 1).append("\ntarget ").append(id.get(edge[1]) + 1).append("\nvalue ").append(freqList.get(i).getCount()).append("\n]\n");
                } catch (NullPointerException e) {
                }
            }


            fileGMLFile.write(GMLSb.toString());
            fileGMLFile.flush();

            fileGMLFile.close();
            GMLSb = null;


            //-------------------------------------------------------------------------------------------------------------          
            // #### 11. PRINTING REPORT ON PARAMETERS EMPLOYED:

            fileParametersName = StringUtils.substring(textFileName, 0, textFileName.length() - 4).concat("_parameters.txt");
            fileParametersFile = new BufferedWriter(new FileWriter(wkOutput + fileParametersName));
            StringBuilder parametersSb = new StringBuilder();

            parametersSb.append(
                    "Report of the parameters used to extract co-occurrences in file \"").append(textFileName).append("\".\n");
            parametersSb.append(
                    "Number of documents in the corpus: ").append(numberOfDocs).append(".\n");
            parametersSb.append(
                    "Inclusion of n-grams up to (and including) ").append(maxgram).append("-grams.\n");
            parametersSb.append(
                    "Binary or full counting of co-occurrences per document? Binary = ").append(binary).append("\n");
            parametersSb.append(
                    "Size of the list of most frequent stopwords removed: ").append(nbStopWords).append(".\n");
            parametersSb.append(
                    "Only for bigrams and above: size of the list of most frequent stopwords used to filter out: ").append(setStopWordsScientificOrShort.size()).append(".\n");
            parametersSb.append(
                    "max number of words allowed: ").append(freqThreshold).append(".\n");
            parametersSb.append(
                    "min nb of occurrences in the entire corpus for a word to be processed: ").append(occurrenceThreshold).append(".\n");

            parametersSb.append(
                    "min nb of chaacters for a word to be processed: ").append(minWordLength).append(".\n");
            parametersSb.append(
                    "number of words found including n-grams: ").append(setFreqWords.size()).append(".\n");
            parametersSb.append(
                    "number of nodes: ").append(counterIds).append(".\n");
            parametersSb.append(
                    "number of edges: ").append(freqList.size()).append(".\n\n\n");
            parametersSb.append(
                    mostFrequentTerms.toString());

            fileParametersFile.write(parametersSb.toString());
            fileParametersFile.flush();

            fileParametersFile.close();


            //-------------------------------------------------------------------------------------------------------------     
            printingOutputTime.closeAndPrintClock();

            Screen1.logArea.setText(Screen1.logArea.getText().concat("ANALYSIS COMPLETED.\n"
                    + "diverse network files have been created in the same directory as your input text file\n"
                    ));
            Screen1.logArea.setCaretPosition(Screen1.logArea.getText().length());
            Screen1.youTube.setVisible(true);
            Screen1.exitButton.setVisible(true);
            
            //-------Printing GEPHI Output--------------------------------------------------------------------------------     

//            Clock GephiVizTime = new Clock("Creating pdf of Gephi viz");
//            Screen1.logArea.setText(Screen1.logArea.getText().concat("This step takes longer - count 15 seconds on a powerful desktop\n"));
//            Screen1.logArea.setCaretPosition(Screen1.logArea.getText().length());
//            GephiTooKit.main(wkOutput, fileGMLName);
//            GephiVizTime.closeAndPrintClock();
//            Screen1.logArea.setText(Screen1.logArea.getText().concat("The resulting network is a file called \"GEPHI_output.png\" located in the same directory as your text file.\n"
//                    + "Results are ugly but it's going to improve! Check again regularly.\n"
//                    + "The source for this program is freely available at:\n"
//                    + "https://github.com/seinecle/MapText/blob/master/src/seinecle/utils/text/GUIMain.java\n"
//                    + "Thank you!\n"
//                    + "www.clementlevallois.net // twitter: @seinecle"));
//            Screen1.logArea.setCaretPosition(Screen1.logArea.getText().length());


            //System.exit(0);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}

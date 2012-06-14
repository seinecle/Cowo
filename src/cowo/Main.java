/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import GUI.Screen1;
import com.google.common.collect.*;
import com.google.common.collect.Multiset.Entry;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

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
public class Main implements Runnable {

    public static String currLine = "";
    public static HashMultiset<String> freqSet = HashMultiset.create();
    //public static Multiset<Pair<String, String>> wordsPerLine = ConcurrentHashMultiset.create();
    public static LinkedHashMultimap<Integer, String> wordsPerLine = LinkedHashMultimap.create();
    public static LinkedHashMultimap<Integer, String> wordsPerLineFiltered = LinkedHashMultimap.create();
    public static HashSet<String> setOfWords = new HashSet();
    public static HashMultiset<String> setNGrams = HashMultiset.create();
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
    public static int maxgram = 4;
    private final static int nbStopWords = 5000;
    private final static int nbStopWordsShort = 200;
    public final static int maxAcceptedGarbage = 3;
    //static int numberOfThreads = Runtime.getRuntime().availableProcessors();
    static int numberOfThreads = 7;
    private static Integer counterLines = 0;
    // logic of freqThreshold: the higher the number of stopwords filtered out, the lower the number of significant words which should be expected
    private static int freqThreshold = 400;
    public static String wordSeparator;
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
    private static BufferedReader fileStopWordsFrench;
    private static String[] stopwordsOwn;
    private static BufferedReader fileStopWords4;
    public static String[] stopwordsShort;
    public static String[] stopwordsFrench;
    private static String[] stopwordsScientific;
    public static Set<String> setStopWords = new HashSet();
    public static Set<String> setStopWordsScientific = new HashSet();
    public static Set<String> setStopWordsScientificOrShort = new HashSet();
    public static Set<String> setNoLemma = new HashSet();
    public static int minWordLength = 3;
    public static HashMap<Integer, String> mapofLines = new HashMap();
    public static HashMultiset<String> setFreqWords = HashMultiset.create();
    private static String fileMapName;
    private static BufferedWriter fileMapFile;
    private static String fileNetworkName;
    private static BufferedWriter fileNetworkFile;
    private static String fileParametersName;
    private static BufferedWriter fileParametersFile;
    public static Set<String> setStopWordsShort = new HashSet();
    public static Set<String> setStopWordsOwn = new HashSet();
    public static Set<String> setStopWordsFrench = new HashSet();
    public static Set<String> setKeepWords = new HashSet();
    private static BufferedReader fileNoLemma;
    private static String[] noLemmaArray;
    public static String[] keepWordsArray;
    static InputStream in10000 = Main.class.getResourceAsStream("stopwords_10000_most_frequent_filtered.txt");
    static InputStream inFrench = Main.class.getResourceAsStream("stopwords_french.txt");
    static InputStream inscientific = Main.class.getResourceAsStream("scientificstopwords.txt");
    static InputStream inOwn;
    static InputStream inkeep = Main.class.getResourceAsStream("stopwords_tokeep.txt");
    static InputStream innolemma = Main.class.getResourceAsStream("nolemmatization.txt");
    public static String ownStopWords;
    public static String fileGMLName;
    private static BufferedWriter fileGMLFile;
    public static boolean binary = true;
    public static boolean filterDifficultChars = true;
    static private boolean useScientificStopWords = false;
    private static boolean useTDIDF = false;
    //
    //
    // Alchemy API variables
    //
    //
    public boolean useAAPI_Entity = false;
    public static String AlchemyAPIKey = "35876638f85ebcba7e31184b52fefe52e339e18e";
    public static HashMultimap<String, String> currMapTypeToText = HashMultimap.create();
    public static HashMultimap<String, String> overallMapTypeToText = HashMultimap.create();
    public static HashMap<String, String> overallMapTextToType = new HashMap();
    public static ExecutorService executor;
    public static HashMap<Integer, Future<String>> listFutures;
//    public static HashMultiset<String> multisetTypesOfEntities = HashMultiset.create();
    public static HashSet<String> setFilteredFields = new HashSet();
    public static HashMultiset<String> multisetFreqWordsViaAlchemy = HashMultiset.create();
    public static String msgAlchemy;
    StringBuilder AlchemyAPIfieldsAndNumbers;

    public Main(String wkGUI, String textFileGUI, String textFileNameGUI, String binaryYes, String freqThresholdGUI, String minWordLengthGUI, String maxgramGUI, String occurrenceThresholdGUI, String ownStopWordsGUI, String filterDifficultCharsGUI, String useScientificStopWordsGUI, String AlchemyAPIKey, String wordSeparatorGUI, String useTDIDFGUI) {

        textFile = textFileGUI;
        wk = wkGUI;
        textFileName = textFileNameGUI;
        if (!"true".equals(binaryYes)) {
            Main.binary = !Main.binary;
        }
        Main.freqThreshold = Integer.valueOf(freqThresholdGUI);
        Main.minWordLength = Integer.valueOf(minWordLengthGUI);
        Main.maxgram = Integer.valueOf(maxgramGUI);
        Main.occurrenceThreshold = Integer.valueOf(occurrenceThresholdGUI);
        Main.ownStopWords = ownStopWordsGUI;
        System.out.println("path of the personal stopword list: " + ownStopWordsGUI);
        if (!"true".equals(filterDifficultCharsGUI)) {
            Main.filterDifficultChars = !Main.filterDifficultChars;
        }
        if ("true".equals(useScientificStopWordsGUI)) {
            Main.useScientificStopWords = !Main.useScientificStopWords;
        }
        System.out.println("use scientific stopwords? " + Main.useScientificStopWords);
        Main.wordSeparator = wordSeparatorGUI;
        System.out.println("word separator is: \"" + wordSeparator + "\"");
        if ("true".equals(useTDIDFGUI)) {
            Main.useTDIDF = !Main.useTDIDF;
        }
        System.out.println("word separator is: \"" + wordSeparator + "\"");

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
            if (!ownStopWords.equals("nothing")) {
                inOwn = Main.class.getResourceAsStream(ownStopWords);
            } //this else condition should be deleted at release and the following lines included in the preceding if condition
            else {
                inOwn = Main.class.getResourceAsStream("stopwords_seinecle.txt");
            }
            fileStopWords2 = new BufferedReader(new InputStreamReader(inOwn));
            stopwordsOwn = fileStopWords2.readLine().split(",");
            setStopWordsOwn.addAll(Arrays.asList(stopwordsOwn));
            fileStopWords2.close();

            fileStopWordsFrench = new BufferedReader(new InputStreamReader(inFrench));
            stopwordsFrench = fileStopWordsFrench.readLine().split(",");
            setStopWordsFrench.addAll(Arrays.asList(stopwordsFrench));
            fileStopWordsFrench.close();

            if (Main.useScientificStopWords) {

                fileStopWords4 = new BufferedReader(new InputStreamReader(inscientific));
                stopwordsScientific = fileStopWords4.readLine().split(",");
                setStopWordsScientific.addAll(Arrays.asList(stopwordsScientific));
                fileStopWords4.close();
            }

            fileKeepWords = new BufferedReader(new InputStreamReader(inkeep));
            keepWordsArray = fileKeepWords.readLine().split(",");
            setKeepWords.addAll(Arrays.asList(keepWordsArray));
            //fileKeepWords.close();

            fileNoLemma = new BufferedReader(new InputStreamReader(innolemma));
            noLemmaArray = fileNoLemma.readLine().split(",");
            setNoLemma.addAll(Arrays.asList(noLemmaArray));
            fileNoLemma.close();

            fileStopWords = new BufferedReader(new InputStreamReader(in10000));
            stopwords = fileStopWords.readLine().split(",");
            stopwords = Arrays.copyOf(stopwords, nbStopWords);
            fileStopWords.close();

//            if (!ownStopWords.equals("nothing")) {
            stopwords = ArrayUtils.addAll(stopwords, stopwordsOwn);
            stopwords = ArrayUtils.addAll(stopwords, stopwordsFrench);
//            }

            if (Main.useScientificStopWords) {
                stopwords = ArrayUtils.addAll(stopwords, stopwordsScientific);
            }

            setStopWords.addAll(Arrays.asList(stopwords));


            stopwordsShort = Arrays.copyOf(stopwords, nbStopWordsShort);

            setStopWordsShort.addAll(Arrays.asList(stopwordsShort));
            if (Main.useScientificStopWords) {
                setStopWordsScientificOrShort.addAll(setStopWordsScientific);
            }

            setStopWordsScientificOrShort.addAll(setStopWordsShort);

            if (!ownStopWords.equals("nothing")) {
                setStopWordsScientificOrShort.addAll(setStopWordsOwn);
            }

            if (!ownStopWords.equals("nothing")) {

                fileStopWords = new BufferedReader(new FileReader(ownStopWords));
                stopwords = fileStopWords.readLine().split(",");
                setStopWords.addAll(Arrays.asList(stopwords));

            }

            loadingStopWordsTime.closeAndPrintClock();
            //-------------------------------------------------------------------------------------------------------------


            // ### 2. LOADING FILE IN MEMORY AND CLEANING  ...

            if (useAAPI_Entity) {
                msgAlchemy = " \nExtracting entities with Alchemy API in each line.";
            } else {
                msgAlchemy = "";

            }
            Clock loadingTime = new Clock("Loading text file: " + textFile + msgAlchemy);

            fr = new FileReader(textFile);
            BufferedReader br = new BufferedReader(fr);

            LineNumberReader lnr = new LineNumberReader(new FileReader(new File(textFile)));
            lnr.skip(Long.MAX_VALUE);
            int nbLines = lnr.getLineNumber();
            int countLinesInFile = 0;


            if (useAAPI_Entity) {//these 5 lines are specfic to the AlchemyAPI option
                executor = Executors.newFixedThreadPool(60);
                listFutures = new HashMap();
                // this line takes the fields selected by the user in the GUI and puts then in a set.
                setFilteredFields.addAll(Arrays.asList((String[])Screen1.screen3.listFields.getSelectedValues()));
                System.out.println("List of fields selected:\n" + setFilteredFields.toString());
            }

            while ((currLine = br.readLine()) != null) {
                countLinesInFile++;
                if (!currLine.matches(".*\\w.*")) {
                    continue;
                }

                counterLines++;


                if (useAAPI_Entity) {

                    AlchemyExtractor callable = new AlchemyExtractor(currLine);
                    Future<String> futureString = executor.submit(callable);
                    listFutures.put(counterLines, futureString);
                    //currLine = new AlchemyExtractor(currLine).call();


                } else {
                    currLine = TextCleaner.doBasicCleaning(currLine);
                    mapofLines.put(counterLines, currLine);
                    //System.out.println(currLine);


                } //end else condition




            } // end looping through all lines of the original text file
            counterLines = 0;
            fr.close();
            br.close();
            loadingTime.closeAndPrintClock();
            //### END of file reading---------------------------------------------------------




            if (useAAPI_Entity) {
                Iterator<Map.Entry<Integer, Future<String>>> itMap = listFutures.entrySet().iterator();
                while (itMap.hasNext()) {
                    try {
                        counterLines++;
                        Map.Entry<Integer, Future<String>> entry = itMap.next();
                        String currLine = entry.getValue().get();
                        mapofLines.put(counterLines, currLine);

                    } catch (IllegalArgumentException e) {
                        System.out.println("API call returned no text");
                        currLine = "";
                    } catch (ExecutionException ee) {
                        System.out.println("API call returned no text");
                        currLine = "";
                    }
                }


                // ### END of creation of a map (nbLine, Line)-------------------------------------------------------------   



                // ### these lines print a list of the categories selected by the user and show how many of them have been found in the text.
                Iterator<String> ITFields = overallMapTypeToText.keySet().iterator();
                AlchemyAPIfieldsAndNumbers = new StringBuilder();
                while (ITFields.hasNext()) {

                    String currField = ITFields.next();
                    AlchemyAPIfieldsAndNumbers.append("- ").append(currField).append(", ").append(overallMapTypeToText.get(currField).size()).append(".\n");
                    System.out.println("nb of " + currField + " = " + overallMapTypeToText.get(currField).size());

                }

                System.out.println("total of entities: " + overallMapTypeToText.size());


            }


            numberOfDocs = mapofLines.keySet().size();

            System.out.println("nb of docs treated: " + numberOfDocs);





            // ### EXTRACTING set of NGrams and LEMMATIZING
            if (!useAAPI_Entity) {


                NGramFinder.runIt(mapofLines);
                NGramCleaner.cleanIt();

                Clock LemmatizerClock = new Clock("Lemmatizing");

                freqSet = Lemmatizer.doLemmatizationReturnMultiSet(freqSet);

                LemmatizerClock.closeAndPrintClock();

                System.out.println(
                        "size of freqSet (unique Words):" + freqSet.elementSet().size());

            }




            //-------------------------------------------------------------------------------------------------------------        
            // #### 6. REMOVING STOPWORDS


            if (!useAAPI_Entity & (useScientificStopWords | !ownStopWords.equals("nothing"))) {
                Clock stopwordsRemovalTime = new Clock("Removing stopwords");
                Iterator<Entry<String>> it = freqSet.entrySet().iterator();


                while (it.hasNext()) {
                    counter++;
                    Entry<String> entry = it.next();


                    new StopWordsRemover(entry.getElement().trim(), entry.getCount());
                }
                counter = 0;
                counterLines = 0;
                stopwordsRemovalTime.closeAndPrintClock();

            } else {
                filteredFreqSet.addAll(freqSet);
            }


            // #### SORTS TERMS BY FREQUENCY, LEAVING OUT THE LESS FREQUENT ONE
            freqList = MultiSetSorter.sortMultisetPerEntryCount(filteredFreqSet);
            System.out.println("size of freqList: " + freqList.size());
            ListIterator<Entry<String>> li = freqList.listIterator(Math.min(freqThreshold, freqList.size()));

            while (li.hasNext()) {
                li.next();
                li.remove();
            }
            System.out.println("size of the set of words after terms less frequent than " + freqThreshold + " are removed: " + freqList.size());



            //-------------------------------------------------------------------------------------------------------------   
            // #### DELETES bi-grams trigrams and above, IF they are already contained in n+1 grams
            if (!useAAPI_Entity) {
                NGramDuplicatesCleaner.removeDuplicates();

            } //---------------------------------------------------------------------------------------------------------------
            //Deletes terms below the frequency threshold and in the case of a person, deletes it if there is no space in it.
            else {

                freqListFiltered.addAll(freqList);
                Iterator<Multiset.Entry<String>> itFreqList3 = Main.freqList.iterator();


                while (itFreqList3.hasNext()) {
                    Entry<String> currEntry = itFreqList3.next();
                    String currElement = currEntry.getElement();
                    int currElementCount = currEntry.getCount();
                    if (currElementCount >= occurrenceThreshold & (!"Person".equals(Main.overallMapTextToType.get(currElement))
                            | (Main.overallMapTextToType.get(currElement)).equals("Person") & currElement.trim().contains(" "))) {
                        setFreqWords.add(currElement, currElementCount);
                    }

                }
            }
            System.out.println("size of setFreqWords:" + setFreqWords.elementSet().size());


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


            //COUNTING IN HOW MANY DOCS EACH FREQUENT TERM OCCURS (FOR THE TD IDF MEASURE)
            Iterator<String> itIDF = setFreqWords.elementSet().iterator();
            HashMultiset countTermsInDocs = HashMultiset.create();

            while (itIDF.hasNext()) {
                String freqWordTerm = itIDF.next();
                Iterator<String> itLines = mapofLines.values().iterator();
                while (itLines.hasNext()) {
                    if (itLines.next().contains(freqWordTerm)) {
                        countTermsInDocs.add(freqWordTerm);
                    }

                }

            }




            //    -------------------------------------------------------------------------------------------------------------  
            // #### 8. COUNTING CO-OCCURRENCES PER LINE
            Clock calculatingCooccurrencesTime = new Clock("Determining all word co-occurrences for each line of the text");
            for (Integer lineNumber : mapofLines.keySet()) {

                HashMap<String, Float> tdIDFScores = new HashMap();
                String currWords = mapofLines.get(lineNumber);
                //System.out.println("in the loop: " + currWords);
                int countTermsInThisDoc;
                if (useAAPI_Entity) {
                    countTermsInThisDoc = currWords.split("\\|").length;
                } else {
                    countTermsInThisDoc = currWords.split(wordSeparator).length;
                }


                if (countTermsInThisDoc < 2) {
                    if (countTermsInThisDoc > 0) {
//                        System.out.println("term 1 in the currLine: " + currWords.split(wordSeparator)[0]);
                    }
//                    System.out.println("breaking because just one word");

                    continue;
                }


                if (currWords == null) {
//                    System.out.println("breaking because of null string!");
                    continue;
                }

//                System.out.println("this is the line \"" + currWords + "\"");
                currWords = TextCleaner.doBasicCleaning(currWords);
                if (currWords.equals("")) {
//                    System.out.println("breaking because of empty string!");
                    continue;
                }


                Iterator<String> it3 = setFreqWords.elementSet().iterator();



                while (it3.hasNext()) {
                    String currFreqTerm = it3.next();

                    if (currWords.contains(currFreqTerm)) {
                        ngramsInLine.add(currFreqTerm);
                        //System.out.println("currFreqTerm matched is:" + currFreqTerm);

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
                        if (useTDIDF) {
//                        System.out.println("countTermsInThisDoc: " + countTermsInThisDoc);
                            int countDocsInCorpus = numberOfDocs;
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

                            if (useTDIDF) {
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


//            Iterator<Multiset.Entry<String>> itSetCombinations = setCombinations.entrySet().iterator();
//            HashMap<String,Float> combiAndWeights = new HashMap();
//            while (itSetCombinations.hasNext()) {
//                Entry<String> currEntry = itSetCombinations.next();
//                combiAndWeights.put(currEntry.getElement(), (float)currEntry.getCount() / (float)10000);
//            }


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
//                System.out.println(freqList.get(i).getElement());
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

            System.out.println(wkOutput
                    + fileNetworkName);
            StringBuilder networkSb = new StringBuilder();
            for (int i = 0;
                    i < freqList.size() //&& i < freqThreshold
                    ;
                    i++) {
                String[] edge = freqList.get(i).getElement().split(",");
                try {
                    float edgeWeight;
                    if (useTDIDF) {
                        edgeWeight = (float) freqList.get(i).getCount() / (float) 1000;
                    } else {
                        edgeWeight = (float) freqList.get(i).getCount();
                    }
                    networkSb.append(id.get(edge[0]) + 1).append(",").append(id.get(edge[1]) + 1).append(",").append((Float.toString(edgeWeight))).append("\n");
                } catch (NullPointerException e) {
                }
            }

            fileNetworkFile.write(networkSb.toString());
            fileNetworkFile.flush();

            fileNetworkFile.close();
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
            //-------------------------------------------------------------------------------------------------------------          
            // #### 11. PRINTING REPORT ON PARAMETERS EMPLOYED:
            fileParametersName = StringUtils.substring(textFileName, 0, textFileName.length() - 4).concat("_parameters.txt");
            fileParametersFile = new BufferedWriter(new FileWriter(wkOutput + fileParametersName));
            StringBuilder parametersSb = new StringBuilder();

            parametersSb.append(
                    "Report of the parameters used to extract co-occurrences in file \"").append(textFileName).append("\".\n\n");
            if (useAAPI_Entity) {
                parametersSb.append(
                        "AlchemyAPI is used to detected entities in the corpus.\nType and numbers of entities extracted:\n").append(
                        AlchemyAPIfieldsAndNumbers.toString()).append("\n");
            }
            parametersSb.append(
                    "TD-IDF measure used to correct the frequency of terms per docs: ").append(useTDIDF).append(".\n");
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
                    "min nb of occurrences in the entire corpus for a word to be processed: ").append(Main.occurrenceThreshold).append(".\n");

            parametersSb.append(
                    "min nb of characters for a word to be processed: ").append(minWordLength).append(".\n");
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
                    + "diverse network files have been created in the same directory as your input text file\n"));
            Screen1.logArea.setCaretPosition(Screen1.logArea.getText().length());
            Screen1.youTube.setVisible(
                    true);
            Screen1.exitButton.setVisible(
                    true);

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
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

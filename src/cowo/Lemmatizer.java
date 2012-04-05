/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import com.google.common.collect.HashMultiset;
import java.util.Iterator;

/**
 *
 * @author C. Levallois
 */
public class Lemmatizer {

    Lemmatizer() {
    }

    public static String doLemmatizationReturnString(HashMultiset wordsOfLine) {
        
        
        Iterator<String> itwl = wordsOfLine.elementSet().iterator();
        StringBuilder sbWords = new StringBuilder();

        while (itwl.hasNext()) {
            String currEntryOriginal = itwl.next().trim().toLowerCase();
            String currEntry = currEntryOriginal;


//                    if ("consumers".equals(currEntry))
//                        System.out.println("consumers");


            if (currEntry.endsWith("ies")) {
                if (!Main.setNoLemma.contains(currEntry)) {
                    currEntry = currEntry.substring(0, currEntry.length() - 3) + "y";

                }
            } else if (currEntry.endsWith("'s")) {
                currEntry = currEntry.substring(0, currEntry.length() - 2);


            } else if ((currEntry.endsWith("s") | currEntry.endsWith("s'"))
                    && !currEntry.endsWith("us")
                    && !currEntry.endsWith("as")
                    && !currEntry.endsWith("ss")
                    && !Main.setNoLemma.contains(currEntry)
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

            for (int i = 0; i < wordsOfLine.count(currEntryOriginal); i++) {
                if (currEntry.trim().length()>=Main.minWordLength)
                    sbWords.append(currEntry.trim()).append(" ");
            }

        } // end looping through all words of the line which is currently read

        return sbWords.toString();

    }
    
    public static HashMultiset doLemmatizationReturnMultiSet(HashMultiset wordsOfLine) {
        
        HashMultiset result = HashMultiset.create();
        
        Iterator<String> itwl = wordsOfLine.elementSet().iterator();

        while (itwl.hasNext()) {
            String currEntryOriginal = itwl.next().trim().toLowerCase();
            String currEntry = currEntryOriginal;


//                    if ("consumers".equals(currEntry))
//                        System.out.println("consumers");


            if (currEntry.endsWith("ies")) {
                if (!Main.setNoLemma.contains(currEntry)) {
                    currEntry = currEntry.substring(0, currEntry.length() - 3) + "y";

                }
            } else if (currEntry.endsWith("'s")) {
                currEntry = currEntry.substring(0, currEntry.length() - 2);


            } else if ((currEntry.endsWith("s") | currEntry.endsWith("s'"))
                    && !currEntry.endsWith("us")
                    && !currEntry.endsWith("as")
                    && !currEntry.endsWith("ss")
                    && !Main.setNoLemma.contains(currEntry)
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

                if (currEntry.trim().length()>=Main.minWordLength)
                    result.add(currEntry.trim(),wordsOfLine.count(currEntryOriginal));
            
        } // end looping through all words of the line which is currently read

        return result;

    }
}

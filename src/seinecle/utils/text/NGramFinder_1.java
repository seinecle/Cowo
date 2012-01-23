/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package seinecle.utils.text;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author C. Levallois
 * adapted from http://stackoverflow.com/questions/3656762/n-gram-generation-from-a-sentence
 */
public class NGramFinder_1 {

    private static Multiset<String> freqSetN = HashMultiset.create();
    private static int minLength;
    //private static StringBuilder sb;

    public static Multiset<String> run(String toBeParsed, int nGram, int minWordLength) {

        freqSetN.clear();
        try {

            String[] words = toBeParsed.split(" ");
            List<String> wordsList = Arrays.asList(words);
                      

            for (int i = 0; i < wordsList.size(); i++) {

                for (int j = 1; j <= nGram && j+i<wordsList.size(); j++) {

                    freqSetN.add(wordsList.subList(i, i+j).toString());
                    
                    
                    //if (sb.toString().trim().length()>minLength) 

                    System.out.println(wordsList.subList(i, i+j).toString());
                }

                
                //System.out.println(freqSetN);

            }
        } catch (NullPointerException e) {
        }
        catch (IndexOutOfBoundsException f) {}
        return freqSetN;
    }
}

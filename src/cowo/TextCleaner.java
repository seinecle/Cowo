/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author C. Levallois
 */
public class TextCleaner {

    public static String doBasicCleaning(String currLine) {

//        try {

//        currLine = StringUtils.substring(currLine, StringUtils.ordinalIndexOf(currLine, ";", 3));
//        currLine = StringUtils.substring(currLine, 0, StringUtils.ordinalIndexOf(currLine, "NUFORC Note:", 1));

        if (Controller.filterDifficultChars) {
            //System.out.println("line inside the Text Cleaner: \""+currLine+"\"");
//            System.out.println("currLine after trimming: " + currLine);
//            currLine = currLine.replaceAll("\\p{C}", " ");
            currLine = currLine.replaceAll("’", "'");
            currLine = currLine.replaceAll("[^A-Za-z'\\|]", " ");
            currLine = currLine.toLowerCase();
//            currLine = currLine.replaceAll(" +", " ");
            //System.out.println("line inside the Text Cleaner, after the cleaning: \""+currLine+"\"");

        }

        //        } catch (NullPointerException e) {
//            currLine = "";
//            System.out.println("error in the Text cleaner!");
//            System.out.println(e);
//        }
        return currLine;
    }
}

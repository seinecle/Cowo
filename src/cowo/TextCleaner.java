/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

/**
 *
 * @author C. Levallois
 */
public class TextCleaner {


    public static String doBasicCleaning(String currLine) {

//        try {

            if (Main.filterDifficultChars) {
                //System.out.println("line inside the Text Cleaner: \""+currLine+"\"");
                currLine = currLine.replaceAll("\\p{C}", " ");
                currLine = currLine.replaceAll("’", "'");
                currLine = currLine.replaceAll("[^A-Za-z'\\|]", " ");
                currLine = currLine.toLowerCase();
                currLine = currLine.replaceAll(" +", " ");
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

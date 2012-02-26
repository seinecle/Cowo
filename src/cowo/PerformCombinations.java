/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

/**
 *
 * @author C. Levallois
 * code adapted from a sample found on Internet.
 */
import java.io.IOException;
import java.util.HashSet;

public class PerformCombinations {
    private final String[] table;

    PerformCombinations(String [] table) throws InterruptedException, IOException {
    this.table = table;        
    
    }

    public HashSet<String> call() throws InterruptedException, IOException {

        int i = 0;
        HashSet set = new HashSet();


        //finds all pairs (2) of the brands
        int[] indices;

        CombinationGenerator x = new CombinationGenerator(table.length, 2);

        StringBuffer combination;

        while (x.hasMore()) {
            combination = new StringBuffer();
            indices = x.getNext();
            for (int j = 0; j < indices.length; j++)
                combination.append(table[indices[j]]).append(",");
            
            // save all these pairs in a MultiSet which counts how many of each there are.
            set.add(combination.toString());

        }
        return set;
        
    }
}

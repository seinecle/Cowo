/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 *
 * @author C. Levallois
 */
public class MultiSetSorter {
    
    public static <T> List<Entry<T>> sortMultisetPerEntryCount(Multiset<T> multiset){
	
        Comparator<Multiset.Entry<T>> occurence_comparator = new Comparator<Multiset.Entry<T>>() {
            
            @Override
		public int compare(Multiset.Entry<T> e1, Multiset.Entry<T> e2) {
			return e2.getCount() - e1.getCount() ;
		}
	};
        
	List<Entry<T>> sortedByCount = new ArrayList<Entry<T>>(multiset.entrySet());
	Collections.sort(sortedByCount,occurence_comparator);
 
	return sortedByCount;
}
    
    
    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Utils;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * C. Levallois
 */
public class FindAllPairs<T extends Comparable<? super T>> {

    private T t;

    public Set<Pair<T, T>> getAllPairs(Set<T> setObjects) {
        Set<T> setObjectsProcessed = new TreeSet<T>();
        Set<Pair<T, T>> setPairs = new TreeSet<Pair<T, T>>();
        Iterator<T> setObjectsIteratorA = setObjects.iterator();
        Iterator<T> setObjectsIteratorB;
        T currTA;
        T currTB;
        while (setObjectsIteratorA.hasNext()) {
            currTA = setObjectsIteratorA.next();
            setObjectsIteratorB = setObjects.iterator();
            while (setObjectsIteratorB.hasNext()) {
                currTB = setObjectsIteratorB.next();
                if (!setObjectsProcessed.contains(currTB) && currTA != currTB) {
                    setPairs.add(new Pair(currTA, currTB));
                }
            }
            setObjectsProcessed.add(currTA);
        }
        return setPairs;

    }
}

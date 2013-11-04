/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author C. Levallois
 */
public class findAllPairsAuthorsInSet {

    static public TreeSet<Pair<Author, Author>> getAllPairs(Set<Author> setAuthorsInHere) {
        Set<Author> setAuthorsProcessed = new TreeSet<Author>();
        TreeSet<Pair<Author, Author>> setPairs = new TreeSet<Pair<Author, Author>>();
        Iterator<Author> setAuthorsIteratorA = setAuthorsInHere.iterator();
        Iterator<Author> setAuthorsIteratorB;
        Author currAuthorA;
        Author currAuthorB;
        while (setAuthorsIteratorA.hasNext()) {
            currAuthorA = setAuthorsIteratorA.next();
            setAuthorsIteratorB = setAuthorsInHere.iterator();
            while (setAuthorsIteratorB.hasNext()) {
                currAuthorB = setAuthorsIteratorB.next();
                if (!setAuthorsProcessed.contains(currAuthorB) && currAuthorA != currAuthorB) {
                    setPairs.add(new Pair(currAuthorA, currAuthorB));
                }
            }
            setAuthorsProcessed.add(currAuthorA);
        }
        return setPairs;

    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package seinecle.utils.text;

import com.google.common.collect.HashMultiset;

/**
 *
 * @author C. Levallois
 */
public class NGramFinder_3 {

    private final int n;
    private final String text;

    private final int[] indexes;
    private int index = -1;
    private int found = 0;

    public NGramFinder_3 (String text, int n) {
        this.text = text;
        this.n = n;
        indexes = new int[n];
    }

    private boolean seek() {
        if (index >= text.length()) {
            return false;
        }
        push();
        while(++index < text.length()) {
            if (text.charAt(index) == ' ') {
                found++;
                if (found<n) {
                    push();
                } else {
                    return true;
                }
            }
        }
        return true;
    }

    private void push() {
        for (int i = 0; i < n-1; i++) {
            indexes[i] = indexes[i+1];
        }
        indexes[n-1] = index+1;
    }

    public HashMultiset<String> list() {
        HashMultiset<String> ngrams = HashMultiset.create();
        while (seek()) {
            ngrams.add(get());
        }
        return ngrams;
    }

    private String get() {
        return text.substring(indexes[0], index);
    }
}

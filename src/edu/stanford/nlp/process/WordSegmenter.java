package edu.stanford.nlp.process;

import java.io.Serializable;
import java.util.List;
import java.util.Collection;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.trees.Tree;

/** An interface for segmenting strings into words
 *  (in unwordsegmented languages).
 *
 *  @author Galen Andrew
 */
public interface WordSegmenter extends Serializable {

  void train(Collection<Tree> trees);

  void loadSegmenter(String filename);

  List<HasWord> segment(String s);
}

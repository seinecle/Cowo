package edu.stanford.nlp.dcoref;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.dcoref.Dictionaries.MentionType;
import edu.stanford.nlp.util.Pair;

/**
 * Semantic knowledge based on WordNet
 */
public class WordNet implements Serializable {
  private static final long serialVersionUID = 7112442030504673315L;

  public WordNet () {
  }

  public boolean alias(Mention mention, Mention antecedent) {
    return false;
  }

  protected static class WNsynset implements Serializable {
    private static final long serialVersionUID = 1623663405576312167L;
  }

  public boolean checkSynonym(Mention m, Mention ant) {
    return false;
  }

  protected boolean checkSynonym(WNsynset mSynsets, WNsynset antSynsets) {
    return false;
  }


  public boolean checkHypernym(CorefCluster mentionCluster, CorefCluster potentialAntecedent, Mention m, Mention ant) {
    return false;
  }
  protected boolean checkHypernym(WNsynset mSynsets, WNsynset antSynsets) {
    return false;
  }
}

package edu.stanford.nlp.parser.lexparser;

import edu.stanford.nlp.util.Index;

/**
 * An unknown word model for German; relies on BaseUnknownWordModel plus number matching.
 * An assumption of this model is that numbers (arabic digit sequences)
 * are tagged CARD. This is correct for all of NEGRA/Tiger/TuebaDZ.
 *
 * @author Roger Levy
 * @author Greg Donaker (corrections and modeling improvements)
 * @author Christopher Manning (generalized and improved what Greg did)
 */
public class GermanUnknownWordModel extends BaseUnknownWordModel {

  private static final long serialVersionUID = 221L;

  private static final String numberMatch = "[0-9]+(?:\\.[0-9]*)";

  public GermanUnknownWordModel(Options op, Lexicon lex, Index<String> wordIndex, Index<String> tagIndex) {
    super(op, lex, wordIndex, tagIndex);
  }


  /** Calculate the log-prob score of a particular TaggedWord in the
   *  unknown word model.
   *
   *  @param itw the tag->word production in IntTaggedWord form
   *  @return The log-prob score of a particular TaggedWord.
   */
  @Override
  public float score(IntTaggedWord itw, String word) {
    String tag = itw.tagString(tagIndex);

    if (word.matches(numberMatch)) {
      //EncodingPrintWriter.out.println("Number match for " + word,encoding);
      if (tag.equals("CARD")) {
        return 0.0f;
      } else {
        //EncodingPrintWriter.out.println("Unknown word estimate for " + word + " as " + tag + ": " + logProb,encoding); //debugging
        return Float.NEGATIVE_INFINITY;
      }
    } else {
      return super.score(itw, word);
    }
  }

}


package edu.stanford.nlp.parser.lexparser;

import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.Index;
import edu.stanford.nlp.util.Pair;

import java.util.Collection;
import java.util.Collections;


/**
 * A lexicon class for Chinese.  Extends the (English) BaseLexicon class,
 * overriding its score and train methods to include a
 * ChineseUnknownWordModel.
 *
 * @author Roger Levy
 */
public class ChineseLexicon extends BaseLexicon {

  private static final long serialVersionUID = -7836464391021114960L;

  private static final boolean useRandomWalk = false;
  public final boolean useCharBasedUnknownWordModel;
  // public static final boolean useMaxentUnknownWordModel;
  public final boolean useGoodTuringUnknownWordModel;

  //private ChineseUnknownWordModel unknown;
  // private ChineseMaxentLexicon cml;
  private static final int STEPS = 1;
  private RandomWalk probRandomWalk;


  public ChineseLexicon(Options op, ChineseTreebankParserParams params, Index<String> wordIndex, Index<String> tagIndex) {
    super(op, wordIndex, tagIndex);
    useCharBasedUnknownWordModel = params.useCharBasedUnknownWordModel;
    useGoodTuringUnknownWordModel = params.useGoodTuringUnknownWordModel;
    // if (useMaxentUnknownWordModel) {
    //  cml = new ChineseMaxentLexicon();
    // } else {
    //unknown = new ChineseUnknownWordModel();
    //this.setUnknownWordModel(new ChineseUnknownWordModel(op));
    // this.getUnknownWordModel().setLexicon(this);
    // }
  }


  /** Trains a lexicon on a collection of trees. */
  @Override
  public void train(Collection<Tree> trees) {
    super.train(trees);
    if (useRandomWalk) {
      // This code was wrong, since RandomWalk took a collection of MutablePair not Tree!  We're not using it currently, so commented out...
      //   probRandomWalk = new RandomWalk(trees, STEPS);
      probRandomWalk = new RandomWalk(Collections.<Pair<?,?>>emptyList(), STEPS);
    }
    // if (useMaxentUnknownWordModel) {
    //  cml.trainUnknownWordModel(trees);
    // } else {
    //  getUnknownWordModel().train(trees);
    // }
  }


  @Override
  public float score(IntTaggedWord iTW, int loc, String word) {
    double c_W = seenCounter.getCount(iTW);
    boolean seen = (c_W > 0.0);

    if (seen) {
      if (useRandomWalk) {
        return (float) scoreRandomWalk(iTW);
      } else {
        return super.score(iTW, loc, word);
      }
    } else {
      float score;
      // if (useMaxentUnknownWordModel) {
      //  score = cml.score(iTW, 0);
      // } else {
      score = this.getUnknownWordModel().score(iTW, loc, 0.0, 0.0, 0.0, word); // ChineseUnknownWordModel doesn't use the final three params
      // }
      return score;
    }
  }


  private double scoreRandomWalk(IntTaggedWord itw) {
    TaggedWord tw = itw.toTaggedWord(wordIndex, tagIndex);
    String word = tw.value();
    String tag = tw.tag();
    return probRandomWalk.score(tag, word);
  }

}

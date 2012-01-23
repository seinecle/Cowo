package edu.stanford.nlp.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.process.WordToSentenceProcessor;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Timing;

/**
 * This class assumes that there is either a
 * <code>List&lt;? extends CoreLabel&gt;</code> or a
 * <code>List&lt;List&lt;? extends CoreLabel&gt;&gt;</code> under the
 * <code>Annotation.WORDS_KEY</code> field, and it takes
 * each <code>List&lt;? extends CoreLabel&gt;</code> and runs it
 * through {@link edu.stanford.nlp.process.WordToSentenceProcessor} and
 * put the new <code>List&lt;List&lt;? extends CoreLabel&gt;&gt;</code>
 * (it is now definitely a
 * <code>List&lt;List&lt;? extends CoreLabel&gt;&gt;</code>) back under
 * the Annotation.WORDS_KEY field.
 *
 * @author Jenny Finkel
 */
public class WordsToSentencesAnnotator implements Annotator{

  final private WordToSentenceProcessor<CoreLabel> wts;
  private Timing timer = new Timing();

  private boolean VERBOSE = true;

  public WordsToSentencesAnnotator() {
    this(true);
  }

  public WordsToSentencesAnnotator(boolean verbose) {
    VERBOSE = verbose;
    wts = new WordToSentenceProcessor<CoreLabel>();
  }

  private WordsToSentencesAnnotator(WordToSentenceProcessor<CoreLabel> wts,
                                    boolean verbose) {
    VERBOSE = verbose;
    this.wts = wts;
  }

  public static WordsToSentencesAnnotator newlineSplitter(boolean verbose) {
    WordToSentenceProcessor<CoreLabel> wts = 
      new WordToSentenceProcessor<CoreLabel>("", 
                                             Collections.<String>emptySet(),
                                             Collections.singleton("\n"));
    return new WordsToSentencesAnnotator(wts, verbose);
  }

  @SuppressWarnings("unused")
  private static long millisecondsAnnotating = 0;
  
  public void setSentenceBoundaryToDiscard(Set<String> boundaries) {
    wts.setSentenceBoundaryToDiscard(boundaries);
  }
  
  public void addHtmlSentenceBoundaryToDiscard(Set<String> boundaries) {
    wts.addHtmlSentenceBoundaryToDiscard(boundaries);
  }

  public void setOneSentence(boolean isOneSentence)
  {
    wts.setOneSentence(isOneSentence);
  }

  public void annotate(Annotation annotation) {
    if (VERBOSE) {    
      timer.start();
      System.err.print("PTB tokenizing...");
    }
    if (annotation.has(CoreAnnotations.TokensAnnotation.class)) {
      
      // get text and tokens from the document
      String text = annotation.get(CoreAnnotations.TextAnnotation.class);
      List<CoreLabel> tokens = annotation.get(CoreAnnotations.TokensAnnotation.class);
      
      // assemble the sentence annotations
      int tokenOffset = 0;
      List<CoreMap> sentences = new ArrayList<CoreMap>();
      for (List<CoreLabel> sentenceTokens: this.wts.process(tokens)) {
        if (sentenceTokens.size() == 0) {
          throw new RuntimeException("unexpected empty sentence: " + sentenceTokens);
        }

        // get the sentence text from the first and last character offsets
        int begin = sentenceTokens.get(0).get(CharacterOffsetBeginAnnotation.class);
        int last = sentenceTokens.size() - 1;
        int end = sentenceTokens.get(last).get(CharacterOffsetEndAnnotation.class);
        String sentenceText = text.substring(begin, end);

        // create a sentence annotation with text and token offsets
        Annotation sentence = new Annotation(sentenceText);
        sentence.set(CharacterOffsetBeginAnnotation.class, begin);
        sentence.set(CharacterOffsetEndAnnotation.class, end);
        sentence.set(CoreAnnotations.TokensAnnotation.class, sentenceTokens);
        sentence.set(CoreAnnotations.TokenBeginAnnotation.class, tokenOffset);
        tokenOffset += sentenceTokens.size();
        sentence.set(CoreAnnotations.TokenEndAnnotation.class, tokenOffset);

        // add the sentence to the list
        sentences.add(sentence);
      }
      // the condition below is possible if sentenceBoundaryToDiscard is initialized!
      /*
      if (tokenOffset != tokens.size()) {
        throw new RuntimeException(String.format(
            "expected %d tokens, found %d", tokens.size(), tokenOffset));
      }
      */
      
      // add the sentences annotations to the document
      annotation.set(CoreAnnotations.SentencesAnnotation.class, sentences);

    } else {
      throw new RuntimeException("unable to find words/tokens in: " + annotation);
    }
  }
}

package edu.stanford.nlp.parser.lexparser;

import java.io.Serializable;

import edu.stanford.nlp.util.Index;

/**
 * Stores the serialized material representing the grammar and lexicon of a
 * parser, and an Options that specifies things like how unknown words were
 * handled and how distances were binned that will also be needed to parse
 * with the grammar.
 *
 * @author Dan Klein
 * @author Christopher Manning
 */
public class ParserData implements Serializable {

  public Lexicon lex;
  public BinaryGrammar bg;
  public UnaryGrammar ug;
  public DependencyGrammar dg;
  public Index<String> stateIndex, wordIndex, tagIndex;
  public Options pt;

  public ParserData(Lexicon lex, BinaryGrammar bg, UnaryGrammar ug, DependencyGrammar dg, Index<String> stateIndex, Index<String> wordIndex, Index<String> tagIndex, Options pt) {
    this.lex = lex;
    this.bg = bg;
    this.ug = ug;
    this.dg = dg;
    this.stateIndex = stateIndex;
    this.wordIndex = wordIndex;
    this.tagIndex = tagIndex;
    this.pt = pt;
  }

  private static final long serialVersionUID = 1;

}

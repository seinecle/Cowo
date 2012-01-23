// Stanford Parser -- a probabilistic lexicalized NL CFG parser
// Copyright (c) 2002 - 2011 The Board of Trustees of
// The Leland Stanford Junior University. All Rights Reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// For more information, bug reports, fixes, contact:
//    Christopher Manning
//    Dept of Computer Science, Gates 1A
//    Stanford CA 94305-9010
//    USA
//    parser-support@lists.stanford.edu
//    http://nlp.stanford.edu/software/lex-parser.shtml

package edu.stanford.nlp.parser.lexparser;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.io.NumberRangeFileFilter;
import edu.stanford.nlp.io.NumberRangesFileFilter;
import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.util.HashIndex;
import edu.stanford.nlp.util.Index;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.international.arabic.ArabicTreebankLanguagePack;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Timing;
import edu.stanford.nlp.util.Triple;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// Miscellaneous documentation by Roger (please correct any errors in this documentation!)
//
// I believe that a lexicalized parser is always constructed by creating a ParserData object and then calling
// makeParsers().
//

/**
 * This class provides the top-level API and command-line interface to a set
 * of reasonably good treebank-trained parsers.  The name reflects the main
 * factored parsing model, which provides a lexicalized PCFG parser
 * implemented as a product
 * model of a plain PCFG parser and a lexicalized dependency parser.
 * But you can also run either component parser alone.  In particular, it
 * is often useful to do unlexicalized PCFG parsing by using just that
 * component parser.
 * <p>
 * See the package documentation for more details and examples of use.
 * See the main method documentation for details of invoking the parser.
 * <p>
 * Note that training on a 1 million word treebank requires a fair amount of
 * memory to run.  Try -mx1500m.
 *
 * @author Dan Klein (original version)
 * @author Christopher Manning (better features, ParserParams, serialization)
 * @author Roger Levy (internationalization)
 * @author Teg Grenager (grammar compaction, tokenization, etc.)
 * @author Galen Andrew (considerable refactoring)
 */
public class LexicalizedParser implements Function<Object,Tree> {

  private ParserData pd;
  ParserData getPD() { return pd; }

  private Options op;
  public Options getOp() { return op; }

  private static final String SERIALIZED_PARSER_PROPERTY = "edu.stanford.nlp.SerializedLexicalizedParser";
  public static final String DEFAULT_PARSER_LOC = ((System.getenv("NLP_PARSER") != null) ?
                                                   System.getenv("NLP_PARSER") :
                                                   "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");

  /**
   * Construct a new LexicalizedParser object from a previously
   * serialized grammar read from a property
   * <code>edu.stanford.nlp.SerializedLexicalizedParser</code>, or a
   * default file location.
   */
  public LexicalizedParser() {
    this(new Options());
  }

  public LexicalizedParser(String[] extraFlags) {
    this(new Options(), extraFlags);
  }

  /**
   * Construct a new LexicalizedParser object from a previously
   * serialized grammar read from a System property
   * <code>edu.stanford.nlp.SerializedLexicalizedParser</code>, or a
   * default file location
   * (<code>/u/nlp/data/lexparser/englishPCFG.ser.gz</code>).
   *
   * @param op Options to the parser.  These get overwritten by the
   *           Options read from the serialized parser; I think the only
   *           thing determined by them is the encoding of the grammar
   *           iff it is a text grammar
   */
  public LexicalizedParser(Options op) {
    this.op = op;
    String source = System.getProperty(SERIALIZED_PARSER_PROPERTY);
    if (source == null) {
      source = DEFAULT_PARSER_LOC;
    }
    pd = getParserDataFromFile(source, op);
    this.op = pd.pt;
    makeParsers();
  }

  public LexicalizedParser(Options op, String[] extraFlags) {
    this(op);
    if (extraFlags.length > 0) {
      setOptionFlags(extraFlags);
    }
  }

  public LexicalizedParser(String parserFileOrUrl,
                           String ... extraFlags) {
    this(parserFileOrUrl, new Options(), extraFlags);
  }

  /**
   * Construct a new LexicalizedParser.  This loads a grammar
   * that was previously assembled and stored as a serialized file.
   * @param parserFileOrUrl Filename/URL to load parser from
   * @param op Options for this parser. These will normally be overwritten
   *     by options stored in the file
   * @throws IllegalArgumentException If parser data cannot be loaded
   */
  public LexicalizedParser(String parserFileOrUrl, Options op,
                           String ... extraFlags) {
    this.op = op;
    //    System.err.print("Loading parser from file " + parserFileOrUrl);
    pd = getParserDataFromFile(parserFileOrUrl, op);
    this.op = pd.pt; // in case a serialized options was read in
    makeParsers();
    if (extraFlags.length > 0) {
      setOptionFlags(extraFlags);
    }
  }

  /**
   * Construct a new LexicalizedParser.  This loads a grammar that
   * was previously assembled and stored.
   *
   * @throws IllegalArgumentException If parser data cannot be loaded
   */
  public LexicalizedParser(String parserFileOrUrl,
                           boolean isTextGrammar, Options op) {
    this.op = op;
    if (isTextGrammar) {
      pd = getParserDataFromTextFile(parserFileOrUrl, op);
    } else {
      pd = getParserDataFromSerializedFile(parserFileOrUrl);
      this.op = pd.pt;
    }
    makeParsers();
  }

  /**
   * Construct a new LexicalizedParser object from a previously
   * assembled grammar.
   *
   * @param pd A <code>ParserData</code> object (not <code>null</code>)
   */
  public LexicalizedParser(ParserData pd) {
    this.pd = pd;
    makeParsers();
  }

  /**
   * Construct a new LexicalizedParser object from a previously
   * assembled grammar read from an InputStream.  One (ParserData)
   * object is read from the stream.  It isn't closed.
   *
   * @param in The ObjectInputStream
   */
  public LexicalizedParser(ObjectInputStream in) throws Exception {
    this((ParserData) in.readObject());
  }

  /**
   * Construct a new LexicalizedParser.
   *
   * @param trainTreebank a treebank to train from
   */
  public LexicalizedParser(Treebank trainTreebank,
                           GrammarCompactor compactor, Options op) {
    this(trainTreebank, compactor, op, null);
  }

  public LexicalizedParser(String treebankPath, FileFilter filt,
                           Options op) {
    this(makeTreebank(treebankPath, op, filt), op);
  }

  public LexicalizedParser(Treebank trainTreebank, Options op) {
    this(trainTreebank, null, op);
  }

  /**
   * Construct a new LexicalizedParser.
   *
   * @param trainTreebank a treebank to train from
   * @param compactor A class for compacting grammars. May be null.
   * @param op Options for how the grammar is built from the treebank
   * @param tuneTreebank  a treebank to tune free params on (may be null)
   */
  public LexicalizedParser(Treebank trainTreebank,
                           GrammarCompactor compactor, Options op,
                           Treebank tuneTreebank) {
    this.op = op;
    pd = getParserDataFromTreebank(trainTreebank, compactor, tuneTreebank);
    makeParsers();
  }

  /**
   * Construct a new LexicalizedParser.
   *
   * @param trainTreebank a treebank to train from
   * @param secondaryTrainTreebank another treebank to train from
   * @param weight a weight factor to give the secondary treebank. If the weight
   *     is 0.25, each example in the secondaryTrainTreebank will be treated as
   *     1/4 of an example sentence.
   * @param compactor A class for compacting grammars. May be null.
   * @param op Options for how the grammar is built from the treebank
   */
  public LexicalizedParser(Treebank trainTreebank, DiskTreebank secondaryTrainTreebank, double weight, GrammarCompactor compactor, Options op) {
    this.op = op;
    pd = getParserDataFromTreebank(trainTreebank, secondaryTrainTreebank, weight, compactor);
    makeParsers();
  }



  /**
   * Converts a Sentence/List/String into a Tree.  If it can't be parsed,
   * it is made into a trivial tree in which each word is attached to a
   * dummy tag ("X") and then to a start nonterminal (also "X").
   *
   * @param in The input Sentence/List/String
   * @return A Tree that is the parse tree for the sentence.  If the parser
   *         fails, a new Tree is synthesized which attaches all words to the
   *         root.
   * @throws IllegalArgumentException If argument isn't a List or String
   */
  public Tree apply(Object in) {
    List<? extends HasWord> lst;
    if (in instanceof String) {
      TokenizerFactory<? extends HasWord> tf = op.tlpParams.treebankLanguagePack().getTokenizerFactory();
      Tokenizer<? extends HasWord> tokenizer = tf.getTokenizer(new BufferedReader(new StringReader((String) in)));
      lst = tokenizer.tokenize();
    } else if (in instanceof List) {
      lst = (List<? extends HasWord>) in;
    } else {
      throw new IllegalArgumentException("Can only parse Sentence/List/String");
    }

    try {
      LexicalizedParserQuery pq = new LexicalizedParserQuery(this);
      if (pq.parse(lst)) {
        Tree bestparse = pq.getBestParse();
        // -10000 denotes unknown words
        bestparse.setScore(pq.getPCFGScore() % -10000.0);
        return bestparse;
      }
    } catch (Exception e) {
      System.err.println("Following exception caught during parsing:");
      e.printStackTrace();
      System.err.println("Recovering using fall through strategy: will construct an (X ...) tree.");
    }
    // if can't parse or exception, fall through
    // TODO: merge with ParserAnnotatorUtils
    TreeFactory lstf = new LabeledScoredTreeFactory();
    List<Tree> lst2 = new ArrayList<Tree>();
    for (HasWord obj : lst) {
      String s = obj.word();
      Tree t = lstf.newLeaf(s);
      Tree t2 = lstf.newTreeNode("X", Collections.singletonList(t));
      lst2.add(t2);
    }
    return lstf.newTreeNode("X", lst2);
  }

  /** Return a TreePrint for formatting parsed output trees.
   *  @return A TreePrint for formatting parsed output trees.
   */
  public TreePrint getTreePrint() {
    return op.testOptions.treePrint(op.tlpParams);
  }

  public Tree parseTree(List<? extends HasWord> sentence) {
    LexicalizedParserQuery pq = new LexicalizedParserQuery(this);
    if (pq.parse(sentence)) {
      return pq.getBestParse();
    } else {
      return null;
    }
  }

  public LexicalizedParserQuery parserQuery() {
    return new LexicalizedParserQuery(this);
  }

  public static ParserData getParserDataFromFile(String parserFileOrUrl, Options op) {
    ParserData pd = getParserDataFromSerializedFile(parserFileOrUrl);
    if (pd == null) {
      pd = getParserDataFromTextFile(parserFileOrUrl, op);
    }
    return pd;
  }

  private static Treebank makeTreebank(String treebankPath, Options op, FileFilter filt) {
    System.err.println("Training a parser from treebank dir: " + treebankPath);
    Treebank trainTreebank = op.tlpParams.diskTreebank();
    System.err.print("Reading trees...");
    if (filt == null) {
      trainTreebank.loadPath(treebankPath);
    } else {
      trainTreebank.loadPath(treebankPath, filt);
    }

    Timing.tick("done [read " + trainTreebank.size() + " trees].");
    return trainTreebank;
  }

  private static DiskTreebank makeSecondaryTreebank(String treebankPath, Options op, FileFilter filt) {
    System.err.println("Additionally training using secondary disk treebank: " + treebankPath + ' ' + filt);
    DiskTreebank trainTreebank = op.tlpParams.diskTreebank();
    System.err.print("Reading trees...");
    if (filt == null) {
      trainTreebank.loadPath(treebankPath);
    } else {
      trainTreebank.loadPath(treebankPath, filt);
    }
    Timing.tick("done [read " + trainTreebank.size() + " trees].");
    return trainTreebank;
  }

  public ParserData parserData() {
    return pd;
  }

  public Lexicon getLexicon() {
    return pd.lex;
  }

  /**
   * Saves the parser defined by pd to the given filename.
   * If there is an error, a RuntimeIOException is thrown.
   */
  static void saveParserDataToSerialized(ParserData pd, String filename) {
    try {
      System.err.print("Writing parser in serialized format to file " + filename + ' ');
      ObjectOutputStream out = IOUtils.writeStreamFromString(filename);
      out.writeObject(pd);
      out.close();
      System.err.println("done.");
    } catch (IOException ioe) {
      throw new RuntimeIOException(ioe);
    }
  }

  /**
   * Saves the parser defined by pd to the given filename.
   * If there is an error, a RuntimeIOException is thrown.
   */
  static void saveParserDataToTextFile(ParserData pd, String filename) {
    try {
      System.err.print("Writing parser in text grammar format to file " + filename);
      OutputStream os;
      if (filename.endsWith(".gz")) {
        // it's faster to do the buffering _outside_ the gzipping as here
        os = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(filename)));
      } else {
        os = new BufferedOutputStream(new FileOutputStream(filename));
      }
      PrintWriter out = new PrintWriter(os);
      String prefix = "BEGIN ";

      out.println(prefix + "OPTIONS");
      if (pd.pt != null) {
        pd.pt.writeData(out);
      }
      out.println();
      System.err.print(".");

      out.println(prefix + "STATE_INDEX");
      if (pd.stateIndex != null) {
        pd.stateIndex.saveToWriter(out);
      }
      out.println();
      System.err.print(".");

      out.println(prefix + "WORD_INDEX");
      if (pd.stateIndex != null) {
        pd.wordIndex.saveToWriter(out);
      }
      out.println();
      System.err.print(".");

      out.println(prefix + "TAG_INDEX");
      if (pd.stateIndex != null) {
        pd.tagIndex.saveToWriter(out);
      }
      out.println();
      System.err.print(".");

      out.println(prefix + "LEXICON");
      if (pd.lex != null) {
        pd.lex.writeData(out);
      }
      out.println();
      System.err.print(".");

      out.println(prefix + "UNARY_GRAMMAR");
      if (pd.ug != null) {
        pd.ug.writeData(out);
      }
      out.println();
      System.err.print(".");

      out.println(prefix + "BINARY_GRAMMAR");
      if (pd.bg != null) {
        pd.bg.writeData(out);
      }
      out.println();
      System.err.print(".");

      out.println(prefix + "DEPENDENCY_GRAMMAR");
      if (pd.dg != null) {
        pd.dg.writeData(out);
      }
      out.println();
      System.err.print(".");

      out.flush();
      out.close();
      System.err.println("done.");
    } catch (IOException e) {
      System.err.println("Trouble saving parser data to ASCII format.");
      throw new RuntimeIOException(e);
    }
  }

  private static void confirmBeginBlock(String file, String line) {
    if (line == null) {
      throw new RuntimeException(file + ": expecting BEGIN block; got end of file.");
    } else if (! line.startsWith("BEGIN")) {
      throw new RuntimeException(file + ": expecting BEGIN block; got " + line);
    }
  }

  protected static ParserData getParserDataFromTextFile(String textFileOrUrl, Options op) {
    try {
      Timing tim = new Timing();
      System.err.print("Loading parser from text file " + textFileOrUrl + ' ');
      BufferedReader in = IOUtils.readReaderFromString(textFileOrUrl);
      Timing.startTime();

      String line = in.readLine();
      confirmBeginBlock(textFileOrUrl, line);
      op.readData(in);
      System.err.print(".");

      line = in.readLine();
      confirmBeginBlock(textFileOrUrl, line);
      Index<String> stateIndex = HashIndex.loadFromReader(in);
      System.err.print(".");

      line = in.readLine();
      confirmBeginBlock(textFileOrUrl, line);
      Index<String> wordIndex = HashIndex.loadFromReader(in);
      System.err.print(".");

      line = in.readLine();
      confirmBeginBlock(textFileOrUrl, line);
      Index<String> tagIndex = HashIndex.loadFromReader(in);
      System.err.print(".");

      line = in.readLine();
      confirmBeginBlock(textFileOrUrl, line);
      Lexicon lex = op.tlpParams.lex(op, wordIndex, tagIndex);
      lex.readData(in);
      System.err.print(".");

      line = in.readLine();
      confirmBeginBlock(textFileOrUrl, line);
      UnaryGrammar ug = new UnaryGrammar(stateIndex);
      ug.readData(in);
      System.err.print(".");

      line = in.readLine();
      confirmBeginBlock(textFileOrUrl, line);
      BinaryGrammar bg = new BinaryGrammar(stateIndex);
      bg.readData(in);
      System.err.print(".");

      line = in.readLine();
      confirmBeginBlock(textFileOrUrl, line);
      DependencyGrammar dg = new MLEDependencyGrammar(op.tlpParams, op.directional, op.distance, op.coarseDistance, op.trainOptions.basicCategoryTagsInDependencyGrammar, op, wordIndex, tagIndex);
      dg.readData(in);
      System.err.print(".");

      in.close();
      System.err.println(" done [" + tim.toSecondsString() + " sec].");
      return new ParserData(lex, bg, ug, dg, stateIndex, wordIndex, tagIndex, op);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }


  public static ParserData getParserDataFromSerializedFile(String serializedFileOrUrl) {
    try {
      Timing tim = new Timing();
      System.err.print("Loading parser from serialized file " + serializedFileOrUrl + " ...");
      ObjectInputStream in = IOUtils.readStreamFromString(serializedFileOrUrl);
      ParserData pd = (ParserData) in.readObject();

      in.close();
      System.err.println(" done [" + tim.toSecondsString() + " sec].");
      return pd;
    } catch (InvalidClassException ice) {
      // For this, it's not a good idea to continue and try it as a text file!
      System.err.println();   // as in middle of line from above message
      throw new RuntimeException("Invalid class in file: " + serializedFileOrUrl, ice);
    } catch (FileNotFoundException fnfe) {
      // For this, it's not a good idea to continue and try it as a text file!
      System.err.println();   // as in middle of line from above message
      throw new RuntimeException("File not found: " + serializedFileOrUrl, fnfe);
    } catch (StreamCorruptedException sce) {
      // suppress error message, on the assumption that we've really got
      // a text grammar, and that'll be tried next
      System.err.println();
    } catch (Exception e) {
      System.err.println();   // as in middle of line from above message
      e.printStackTrace();
    }
    return null;
  }


  private static void printOptions(boolean train, Options op) {
    op.display();
    if (train) {
      op.trainOptions.display();
    } else {
      op.testOptions.display();
    }
    op.tlpParams.display();
  }


  /** @return a pair of binaryTrainTreebank,binaryTuneTreebank.
   */
  public static Pair<List<Tree>,List<Tree>> getAnnotatedBinaryTreebankFromTreebank(Treebank trainTreebank,
      Treebank tuneTreebank,
      Options op) {
    Timing.startTime();
    // setup tree transforms
    TreebankLangParserParams tlpParams = op.tlpParams;
    TreebankLanguagePack tlp = tlpParams.treebankLanguagePack();

    if (op.testOptions.verbose) {
      PrintWriter pwErr = tlpParams.pw(System.err);
      pwErr.print("Training ");
      pwErr.println(trainTreebank.textualSummary(tlp));
    }

    System.err.print("Binarizing trees...");
    TreeAnnotatorAndBinarizer binarizer;
    if (!op.trainOptions.leftToRight) {
      binarizer = new TreeAnnotatorAndBinarizer(tlpParams, op.forceCNF, !op.trainOptions.outsideFactor(), true, op);
    } else {
      binarizer = new TreeAnnotatorAndBinarizer(tlpParams.headFinder(), new LeftHeadFinder(), tlpParams, op.forceCNF, !op.trainOptions.outsideFactor(), true, op);
    }
    CollinsPuncTransformer collinsPuncTransformer = null;
    if (op.trainOptions.collinsPunc) {
      collinsPuncTransformer = new CollinsPuncTransformer(tlp);
    }
    List<Tree> binaryTrainTrees = new ArrayList<Tree>();
    List<Tree> binaryTuneTrees = new ArrayList<Tree>();

    if (op.trainOptions.selectiveSplit) {
      op.trainOptions.splitters = ParentAnnotationStats.getSplitCategories(trainTreebank, op.trainOptions.tagSelectiveSplit, 0, op.trainOptions.selectiveSplitCutOff, op.trainOptions.tagSelectiveSplitCutOff, tlp);
      removeDeleteSplittersFromSplitters(tlp, op);
      if (op.testOptions.verbose) {
        List<String> list = new ArrayList<String>(op.trainOptions.splitters);
        Collections.sort(list);
        System.err.println("Parent split categories: " + list);
      }
    }
    if (op.trainOptions.selectivePostSplit) {
      // Do all the transformations once just to learn selective splits on annotated categories
      TreeTransformer myTransformer = new TreeAnnotator(tlpParams.headFinder(), tlpParams, op);
      Treebank annotatedTB = trainTreebank.transform(myTransformer);
      op.trainOptions.postSplitters = ParentAnnotationStats.getSplitCategories(annotatedTB, true, 0, op.trainOptions.selectivePostSplitCutOff, op.trainOptions.tagSelectivePostSplitCutOff, tlp);
      if (op.testOptions.verbose) {
        System.err.println("Parent post annotation split categories: " + op.trainOptions.postSplitters);
      }
    }
    if (op.trainOptions.hSelSplit) {
      // We run through all the trees once just to gather counts for hSelSplit!
      int ptt = op.trainOptions.printTreeTransformations;
      op.trainOptions.printTreeTransformations = 0;
      binarizer.setDoSelectiveSplit(false);
      for (Tree tree : trainTreebank) {
        if (op.trainOptions.collinsPunc) {
          tree = collinsPuncTransformer.transformTree(tree);
        }
        binarizer.transformTree(tree);
      }
      binarizer.setDoSelectiveSplit(true);
      op.trainOptions.printTreeTransformations = ptt;
    }
    // we've done all the setup now. here's where the train treebank is transformed.
    for (Tree tree : trainTreebank) {
      if (op.trainOptions.collinsPunc) {
        tree = collinsPuncTransformer.transformTree(tree);
      }
      tree = binarizer.transformTree(tree);
      if (tree.yield().size() - 1 <= op.trainOptions.trainLengthLimit) {
        // TEG: have to subtract off the boundary symbol!
        binaryTrainTrees.add(tree);
      }
    }
    if (op.trainOptions.printAnnotatedStateCounts) {
      binarizer.printStateCounts();
    }
    if (op.trainOptions.printAnnotatedRuleCounts) {
      binarizer.printRuleCounts();
    }

    if (tuneTreebank != null) {
      for (Tree tree : tuneTreebank) {
        if (op.trainOptions.collinsPunc) {
          tree = collinsPuncTransformer.transformTree(tree);
        }
        tree = binarizer.transformTree(tree);
        if (tree.yield().size() - 1 <= op.trainOptions.trainLengthLimit) {
          binaryTuneTrees.add(tree);
        }
      }
    }

    Timing.tick("done.");
    if (op.testOptions.verbose) {
      binarizer.dumpStats();
    }

    return new Pair<List<Tree>,List<Tree>>(binaryTrainTrees, binaryTuneTrees);
  }

  private static void removeDeleteSplittersFromSplitters(TreebankLanguagePack tlp, Options op) {
    if (op.trainOptions.deleteSplitters != null) {
      List<String> deleted = new ArrayList<String>();
      for (String del : op.trainOptions.deleteSplitters) {
        String baseDel = tlp.basicCategory(del);
        boolean checkBasic = del.equals(baseDel);
        for (Iterator<String> it = op.trainOptions.splitters.iterator(); it.hasNext(); ) {
          String elem = it.next();
          String baseElem = tlp.basicCategory(elem);
          boolean delStr = checkBasic && baseElem.equals(baseDel) || elem.equals(del);
          if (delStr) {
            it.remove();
            deleted.add(elem);
          }
        }
      }
      if (op.testOptions.verbose) {
        System.err.println("Removed from vertical splitters: " + deleted);
      }
    }
  }


  public final ParserData getParserDataFromTreebank(Treebank trainTreebank,
      GrammarCompactor compactor,
      Treebank tuneTreebank) {
    System.err.println("Currently " + new Date());
    printOptions(true, op);
    Pair<List<Tree>,List<Tree>> pair = getAnnotatedBinaryTreebankFromTreebank(trainTreebank, tuneTreebank, op);
    List<Tree> binaryTrainTrees = pair.first();
    List<Tree> binaryTuneTrees = pair.second();

    Index<String> stateIndex = new HashIndex<String>();
    Index<String> wordIndex = new HashIndex<String>();
    Index<String> tagIndex = new HashIndex<String>();

    // extract grammars
    Extractor<Pair<UnaryGrammar,BinaryGrammar>> bgExtractor = new BinaryGrammarExtractor(op, stateIndex);
    // Extractor lexExtractor = new LexiconExtractor();
    //TreeExtractor uwmExtractor = new UnknownWordModelExtractor(binaryTrainTrees.size());
    System.err.print("Extracting PCFG...");
    Pair<UnaryGrammar,BinaryGrammar> bgug = bgExtractor.extract(binaryTrainTrees);
    Timing.tick("done.");

    //TODO: wsg2011 Not sure if this should come before or after grammar compaction
    // TODO: why is this done before the tagIndex is not filled out?
    // Why is this done here and not in the version with two
    // treebanks?
    if(op.trainOptions.ruleSmoothing) {
      System.err.print("Smoothing PCFG...");
      Function<Pair<UnaryGrammar,BinaryGrammar>,Pair<UnaryGrammar,BinaryGrammar>> smoother = new LinearGrammarSmoother(op.trainOptions, stateIndex, tagIndex);
      bgug = smoother.apply(bgug);
      Timing.tick("done.");
    }

    if (compactor != null) {
      System.err.print("Compacting grammar...");
      Triple<Index<String>, UnaryGrammar, BinaryGrammar> compacted = compactor.compactGrammar(bgug, stateIndex);
      stateIndex = compacted.first();
      bgug.setFirst(compacted.second());
      bgug.setSecond(compacted.third());
      Timing.tick("done.");
    }

    System.err.print("Compiling grammar...");
    BinaryGrammar bg = bgug.second;
    bg.splitRules();
    UnaryGrammar ug = bgug.first;
    // System.err.println("\nUnary grammar built by BinaryGrammarExtractor");
    // ug.writeAllData(new OutputStreamWriter(System.err));
    ug.purgeRules();
    // System.err.println("Unary grammar after purgeRules");
    // ug.writeAllData(new OutputStreamWriter(System.err));
    Timing.tick("done");

    System.err.print("Extracting Lexicon...");
    Lexicon lex = op.tlpParams.lex(op, wordIndex, tagIndex);
    lex.train(binaryTrainTrees);
    Timing.tick("done.");

    DependencyGrammar dg = null;
    if (op.doDep) {
      System.err.print("Extracting Dependencies...");
      Extractor<DependencyGrammar> dgExtractor = op.tlpParams.dependencyGrammarExtractor(op, wordIndex, tagIndex);
      dg = dgExtractor.extract(binaryTrainTrees);
      //      ((ChineseSimWordAvgDepGrammar)dg).setLex(lex);
      Timing.tick("done.");
      //System.out.println(dg);
      //System.err.print("Extracting Unknown Word Model...");
      //UnknownWordModel uwm = (UnknownWordModel)uwmExtractor.extract(binaryTrainTrees);
      //Timing.tick("done.");
      if (tuneTreebank != null) {
        System.err.print("Tuning Dependency Model...");
        dg.setLexicon(lex); // MG2008: needed if using PwGt model
        dg.tune(binaryTuneTrees);
        Timing.tick("done.");
      }
    }

    System.err.println("Done training parser.");
    if (op.trainOptions.trainTreeFile!=null) {
      try {
        System.err.print("Writing out binary trees to "+ op.trainOptions.trainTreeFile+"...");
        IOUtils.writeObjectToFile(binaryTrainTrees, op.trainOptions.trainTreeFile);
        Timing.tick("done.");
      } catch (Exception e) {
        System.err.println("Problem writing out binary trees.");
      }
    }
    return new ParserData(lex, bg, ug, dg, stateIndex, wordIndex, tagIndex, op);
  }

  // TODO: Unify the below method with the method above
  // TODO: Make below method work with arbitrarily large secondary treebank via iteration
  // TODO: Have weight implemented for training lexicon

  /**
   * A method for training from two different treebanks, the second of which is presumed
   * to be orders of magnitude larger.
   * <p/>
   * Trees are not read into memory but processed as they are read from disk.
   * <p/>
   * A weight (typically &lt;= 1) can be put on the second treebank.
   */
  protected final ParserData getParserDataFromTreebank(Treebank trainTreebank, DiskTreebank secondaryTrainTreebank, double weight, GrammarCompactor compactor) {
    System.err.println("Currently " + new Date());
    printOptions(true, op);
    Timing.startTime();

    // setup tree transforms
    TreebankLangParserParams tlpParams = op.tlpParams;
    TreebankLanguagePack tlp = tlpParams.treebankLanguagePack();

    if (op.testOptions.verbose) {
      PrintWriter pwErr = tlpParams.pw(System.err);
      pwErr.print("Training ");
      pwErr.println(trainTreebank.textualSummary(tlp));
      pwErr.print("Secondary training ");
      pwErr.println(secondaryTrainTreebank.textualSummary(tlp));
    }

    CompositeTreeTransformer trainTransformer = new CompositeTreeTransformer();

    if (op.trainOptions.collinsPunc) {
      CollinsPuncTransformer collinsPuncTransformer = new CollinsPuncTransformer(tlp);
      trainTransformer.addTransformer(collinsPuncTransformer);
    }

    TreeAnnotatorAndBinarizer binarizer;
    if (!op.trainOptions.leftToRight) {
      binarizer = new TreeAnnotatorAndBinarizer(tlpParams, op.forceCNF, !op.trainOptions.outsideFactor(), true, op);
    } else {
      binarizer = new TreeAnnotatorAndBinarizer(tlpParams.headFinder(), new LeftHeadFinder(), tlpParams, op.forceCNF, !op.trainOptions.outsideFactor(), true, op);
    }
    trainTransformer.addTransformer(binarizer);

    CompositeTreebank wholeTreebank = new CompositeTreebank(trainTreebank, secondaryTrainTreebank);
    if (op.trainOptions.selectiveSplit) {
      op.trainOptions.splitters = ParentAnnotationStats.getSplitCategories(wholeTreebank, op.trainOptions.tagSelectiveSplit, 0, op.trainOptions.selectiveSplitCutOff, op.trainOptions.tagSelectiveSplitCutOff, tlp);
      removeDeleteSplittersFromSplitters(tlp, op);
      if (op.testOptions.verbose) {
        List<String> list = new ArrayList<String>(op.trainOptions.splitters);
        Collections.sort(list);
        System.err.println("Parent split categories: " + list);
      }
    }

    Treebank transformedWholeTreebank = wholeTreebank;

    if (op.trainOptions.selectivePostSplit) {
      // Do all the transformations once just to learn selective splits on annotated categories
      TreeTransformer annotator = new TreeAnnotator(tlpParams.headFinder(), tlpParams, op);
      //wholeTreebank.transformOnRead(annotator);
      transformedWholeTreebank = transformedWholeTreebank.transform(annotator);
      op.trainOptions.postSplitters = ParentAnnotationStats.getSplitCategories(transformedWholeTreebank, true, 0, op.trainOptions.selectivePostSplitCutOff, op.trainOptions.tagSelectivePostSplitCutOff, tlp);
      if (op.testOptions.verbose) {
        System.err.println("Parent post annotation split categories: " + op.trainOptions.postSplitters);
      }
    }
    if (op.trainOptions.hSelSplit) {
      // We run through all the trees once just to gather counts for hSelSplit!
      int ptt = op.trainOptions.printTreeTransformations;
      op.trainOptions.printTreeTransformations = 0;
      binarizer.setDoSelectiveSplit(false);
      for (Tree tree : transformedWholeTreebank) {
        trainTransformer.transformTree(tree);
      }
      binarizer.setDoSelectiveSplit(true);
      op.trainOptions.printTreeTransformations = ptt;
    }

    trainTreebank = trainTreebank.transform(trainTransformer);
    //secondaryTrainTreebank.transformOnRead(trainTransformer);
    Treebank transformedSecondaryTrainTreebank = secondaryTrainTreebank.transform(trainTransformer);

    Index<String> stateIndex = new HashIndex<String>();

    // extract grammars
    BinaryGrammarExtractor bgExtractor = new BinaryGrammarExtractor(op, stateIndex);
    // Extractor lexExtractor = new LexiconExtractor();
    //TreeExtractor uwmExtractor = new UnknownWordModelExtractor(binaryTrainTrees.size());
    System.err.print("Extracting PCFG...");
    Pair<UnaryGrammar,BinaryGrammar> bgug = bgExtractor.extract(trainTreebank, 1.0, transformedSecondaryTrainTreebank, weight);
    Timing.tick("done.");
    if (compactor != null) {
      System.err.print("Compacting grammar...");
      Triple<Index<String>, UnaryGrammar, BinaryGrammar> compacted = compactor.compactGrammar(bgug, stateIndex);
      stateIndex = compacted.first();
      bgug.setFirst(compacted.second());
      bgug.setSecond(compacted.third());
      Timing.tick("done.");
    }
    System.err.print("Compiling grammar...");
    BinaryGrammar bg = bgug.second;
    bg.splitRules();
    UnaryGrammar ug = bgug.first;
    ug.purgeRules();
    Timing.tick("done");
    System.err.print("Extracting Lexicon...");
    Index<String> wordIndex = new HashIndex<String>();
    Index<String> tagIndex = new HashIndex<String>();
    Lexicon lex = op.tlpParams.lex(op, wordIndex, tagIndex);
    // lex.train(wholeTreebank);
    //    lex.train(trainTreebank);
    //    ((BaseLexicon)lex).train(secondaryTrainTreebank, weight);
    // todo. BUG: With this implementation the lexicon is always trained with the
    // secondaryTreebank having a weight of 1.0.  But at least the code now is basically
    // correct rather than totally broken.  CDM Dec 2006.
    CompositeTreebank wholeBinaryTreebank = new CompositeTreebank(trainTreebank, transformedSecondaryTrainTreebank);
    lex.train(wholeBinaryTreebank);
    Timing.tick("done.");

    DependencyGrammar dg = null;
    if (op.doDep) {
      System.err.print("Extracting Dependencies...");
      AbstractTreeExtractor<DependencyGrammar> dgExtractor = new MLEDependencyGrammarExtractor(op, wordIndex, tagIndex);
      dg = dgExtractor.extract(trainTreebank, 1.0, transformedSecondaryTrainTreebank, weight);
      Timing.tick("done.");
    }

    System.err.println("Done training parser.");
    return new ParserData(lex, bg, ug, dg, stateIndex, wordIndex, tagIndex, op);
  }


  private void makeParsers() {
    if (pd == null) {
      throw new IllegalArgumentException("Error loading parser data: pd null");
    }
    // This checks to see if commandline options for the arabic
    // tokenizer, in which case they override the serialized versions.
    // TODO: may not be needed now that options get reset after the
    // parser is initialized.
    if(pd.pt.tlpParams.treebankLanguagePack() instanceof ArabicTreebankLanguagePack){
      ArabicTreebankLanguagePack tlp = ((ArabicTreebankLanguagePack) op.tlpParams.treebankLanguagePack());
      if ( tlp.getTokenizerFactory() != null ){
        try {
          ((ArabicTreebankLanguagePack) pd.pt.tlpParams.treebankLanguagePack()).setTokenizerFactory(tlp.getTokenizerFactory());
        } catch( Exception e){
          System.err.println(" Attempt to apply command line options to serialized parser failed; " + e.toString() );
        }
      }
    }
    op = pd.pt;
  }


  // helper function
  private static int numSubArgs(String[] args, int index) {
    int i = index;
    while (i + 1 < args.length && args[i + 1].charAt(0) != '-') {
      i++;
    }
    return i - index;
  }


  /**
   * This will set options to the parser, in a way exactly equivalent to
   * passing in the same sequence of command-line arguments.  This is a useful
   * convenience method when building a parser programmatically. The options
   * passed in should
   * be specified like command-line arguments, including with an initial
   * minus sign.
   * <p/>
   * <i>Notes:</i> This can be used to set parsing-time flags for a
   * serialized parser.  You can also still change things serialized
   * in Options, but this will probably degrade parsing performance.
   * The vast majority of command line flags can be passed to this
   * method, but you cannot pass in options that specify the treebank
   * or grammar to be loaded, the grammar to be written, trees or
   * files to be parsed or details of their encoding, nor the
   * TreebankLangParserParams (<code>-tLPP</code>) to use. The
   * TreebankLangParserParams should be set up on construction of a
   * LexicalizedParser, by constructing an Options that uses
   * the required TreebankLangParserParams, and passing that to a
   * LexicalizedParser constructor.  Note that despite this
   * method being an instance method, many flags are actually set as
   * static class variables.
   *
   * @param flags Arguments to the parser, for example,
   *              {"-outputFormat", "typedDependencies", "-maxLength", "70"}
   * @throws IllegalArgumentException If an unknown flag is passed in
   */
  void setOptionFlags(String... flags) {
    op.setOptions(flags);
  }


  private static void printArgs(String[] args, PrintStream ps) {
    ps.print("LexicalizedParser invoked with arguments:");
    for (String arg : args) {
      ps.print(' ' + arg);
    }
    ps.println();
  }

  /**
   * A main program for using the parser with various options.
   * This program can be used for building and serializing
   * a parser from treebank data, for parsing sentences from a file
   * or URL using a serialized or text grammar parser,
   * and (mainly for parser quality testing)
   * for training and testing a parser on a treebank all in one go. <p>
   * Sample Usages: <br><code>
   * java -mx1500m
   * edu.stanford.nlp.parser.lexparser.LexicalizedParser [-v]
   * -train trainFilesPath fileRange -saveToSerializedFile
   * serializedGrammarFilename</code><p>
   * <code> java -mx1500m
   * edu.stanford.nlp.parser.lexparser.LexicalizedParser
   * [-v] -train trainFilesPath fileRange
   * -testTreebank testFilePath fileRange</code><p>
   * <code>java -mx512m
   * edu.stanford.nlp.parser.lexparser.LexicalizedParser
   * [-v] serializedGrammarPath filename+</code><p>
   * <code>java -mx512m
   * edu.stanford.nlp.parser.lexparser.LexicalizedParser
   * [-v] -loadFromSerializedFile serializedGrammarPath
   * -testTreebank testFilePath fileRange
   * </code><p>
   * If the <code>serializedGrammarPath</code> ends in <code>.gz</code>,
   * then the grammar is written and read as a compressed file (GZip).
   * If the <code>serializedGrammarPath</code> is a URL, starting with
   * <code>http://</code>, then the parser is read from the URL.
   * A fileRange specifies a numeric value that must be included within a
   * filename for it to be used in training or testing (this works well with
   * most current treebanks).  It can be specified like a range of pages to be
   * printed, for instance as <code>200-2199</code> or
   * <code>1-300,500-725,9000</code> or just as <code>1</code> (if all your
   * trees are in a single file, just give a dummy argument such as
   * <code>0</code> or <code>1</code>).
   * The parser can write a grammar as either a serialized Java object file
   * or in a text format (or as both), specified with the following options:
   * <p>
   * <code>java edu.stanford.nlp.parser.lexparser.LexicalizedParser
   * [-v] -train
   * trainFilesPath [fileRange] [-saveToSerializedFile grammarPath]
   * [-saveToTextFile grammarPath]</code><p>
   * If no files are supplied to parse, then a hardwired sentence
   * is parsed. <p>
   *
   * In the same position as the verbose flag (<code>-v</code>), many other
   * options can be specified.  The most useful to an end user are:
   * <UL>
   * <LI><code>-tLPP class</code> Specify a different
   * TreebankLangParserParams, for when using a different language or
   * treebank (the default is English Penn Treebank). <i>This option MUST occur
   * before any other language-specific options that are used (or else they
   * are ignored!).</i>
   * (It's usually a good idea to specify this option even when loading a
   * serialized grammar; it is necessary if the language pack specifies a
   * needed character encoding or you wish to specify language-specific
   * options on the command line.)</LI>
   * <LI><code>-encoding charset</code> Specify the character encoding of the
   * input and output files.  This will override the value in the
   * <code>TreebankLangParserParams</code>, provided this option appears
   * <i>after</i> any <code>-tLPP</code> option.</LI>
   * <LI><code>-tokenized</code> Says that the input is already separated
   * into whitespace-delimited tokens.  If this option is specified, any
   * tokenizer specified for the language is ignored, and a universal (Unicode)
   * tokenizer, which divides only on whitespace, is used.
   * Unless you also specify
   * <code>-escaper</code>, the tokens <i>must</i> all be correctly
   * tokenized tokens of the appropriate treebank for the parser to work
   * well (for instance, if using the Penn English Treebank, you must have
   * coded "(" as "-LRB-", "3/4" as "3\/4", etc.)</LI>
   * <li><code>-escaper class</code> Specify a class of type
   * {@link Function}&lt;List&lt;HasWord&gt;,List&lt;HasWord&gt;&gt; to do
   * customized escaping of tokenized text.  This class will be run over the
   * tokenized text and can fix the representation of tokens. For instance,
   * it could change "(" to "-LRB-" for the Penn English Treebank.  A
   * provided escaper that does such things for the Penn English Treebank is
   * <code>edu.stanford.nlp.process.PTBEscapingProcessor</code>
   * <li><code>-tokenizerFactory class</code> Specifies a
   * TokenizerFactory class to be used for tokenization</li>
   * <li><code>-tokenizerOptions options</code> Specifies options to a
   * TokenizerFactory class to be used for tokenization.   A comma-separated
   * list. For PTBTokenizer, options of interest include
   * <code>americanize=false</code> and <code>asciiQuotes</code> (for German).
   * Note that any choice of tokenizer options that conflicts with the
   * tokenization used in the parser training data will likely degrade parser
   * performance. </li>
   * <li><code>-sentences token </code> Specifies a token that marks sentence
   * boundaries.  A value of <code>newline</code> causes sentence breaking on
   * newlines.  A value of <code>onePerElement</code> causes each element
   * (using the XML <code>-parseInside</code> option) to be treated as a
   * sentence. All other tokens will be interpreted literally, and must be
   * exactly the same as tokens returned by the tokenizer.  For example,
   * you might specify "|||" and put that symbol sequence as a token between
   * sentences.
   * If no explicit sentence breaking option is chosen, sentence breaking
   * is done based on a set of language-particular sentence-ending patterns.
   * </li>
   * <LI><code>-parseInside element</code> Specifies that parsing should only
   * be done for tokens inside the indicated XML-style
   * elements (done as simple pattern matching, rather than XML parsing).
   * For example, if this is specified as <code>sentence</code>, then
   * the text inside the <code>sentence</code> element
   * would be parsed.
   * Using "-parseInside s" gives you support for the input format of
   * Charniak's parser. Sentences cannot span elements. Whether the
   * contents of the element are treated as one sentence or potentially
   * multiple sentences is controlled by the <code>-sentences</code> flag.
   * The default is potentially multiple sentences.
   * This option gives support for extracting and parsing
   * text from very simple SGML and XML documents, and is provided as a
   * user convenience for that purpose. If you want to really parse XML
   * documents before NLP parsing them, you should use an XML parser, and then
   * call to a LexicalizedParser on appropriate CDATA.
   * <LI><code>-tagSeparator char</code> Specifies to look for tags on words
   * following the word and separated from it by a special character
   * <code>char</code>.  For instance, many tagged corpora have the
   * representation "house/NN" and you would use <code>-tagSeparator /</code>.
   * Notes: This option requires that the input be pretokenized.
   * The separator has to be only a single character, and there is no
   * escaping mechanism. However, splitting is done on the <i>last</i>
   * instance of the character in the token, so that cases like
   * "3\/4/CD" are handled correctly.  The parser will in all normal
   * circumstances use the tag you provide, but will override it in the
   * case of very common words in cases where the tag that you provide
   * is not one that it regards as a possible tagging for the word.
   * The parser supports a format where only some of the words in a sentence
   * have a tag (if you are calling the parser programmatically, you indicate
   * them by having them implement the <code>HasTag</code> interface).
   * You can do this at the command-line by only having tags after some words,
   * but you are limited by the fact that there is no way to escape the
   * tagSeparator character.</LI>
   * <LI><code>-maxLength leng</code> Specify the longest sentence that
   * will be parsed (and hence indirectly the amount of memory
   * needed for the parser). If this is not specified, the parser will
   * try to dynamically grow its parse chart when long sentence are
   * encountered, but may run out of memory trying to do so.</LI>
   * <LI><code>-outputFormat styles</code> Choose the style(s) of output
   * sentences: <code>penn</code> for prettyprinting as in the Penn
   * treebank files, or <code>oneline</code> for printing sentences one
   * per line, <code>words</code>, <code>wordsAndTags</code>,
   * <code>dependencies</code>, <code>typedDependencies</code>,
   * or <code>typedDependenciesCollapsed</code>.
   * Multiple options may be specified as a comma-separated
   * list.  See TreePrint class for further documentation.</LI>
   * <LI><code>-outputFormatOptions</code> Provide options that control the
   * behavior of various <code>-outputFormat</code> choices, such as
   * <code>lexicalize</code>, <code>stem</code>, <code>markHeadNodes</code>,
   * or <code>xml</code>.
   * Options are specified as a comma-separated list.</LI>
   * <LI><code>-writeOutputFiles</code> Write output files corresponding
   * to the input files, with the same name but a <code>".stp"</code>
   * file extension.  The format of these files depends on the
   * <code>outputFormat</code> option.  (If not specified, output is sent
   * to stdout.)</LI>
   * <LI><code>-outputFilesExtension</code> The extension that is appended to
   * the filename that is being parsed to produce an output file name (with the
   * -writeOutputFiles option). The default is <code>stp</code>.  Don't
   * include the period.
   * <LI><code>-outputFilesDirectory</code> The directory in which output
   * files are written (when the -writeOutputFiles option is specified).
   * If not specified, output files are written in the same directory as the
   * input files.
   * </UL>
   * See also the package documentation for more details and examples of use.
   *
   * @param args Command line arguments, as above
   */
  public static void main(String[] args) {
    boolean train = false;
    boolean saveToSerializedFile = false;
    boolean saveToTextFile = false;
    String serializedInputFileOrUrl = null;
    String textInputFileOrUrl = null;
    String serializedOutputFileOrUrl = null;
    String textOutputFileOrUrl = null;
    String treebankPath = null;
    Treebank testTreebank = null;
    Treebank tuneTreebank = null;
    String testPath = null;
    FileFilter testFilter = null;
    String tunePath = null;
    FileFilter tuneFilter = null;
    FileFilter trainFilter = null;
    String secondaryTreebankPath = null;
    double secondaryTreebankWeight = 1.0;
    FileFilter secondaryTrainFilter = null;

    // variables needed to process the files to be parsed
    TokenizerFactory<? extends HasWord> tokenizerFactory = null;
    String tokenizerOptions = null;
    String tokenizerFactoryClass = null;
    boolean tokenized = false; // whether or not the input file has already been tokenized
    Function<List<HasWord>, List<HasWord>> escaper = null;
    String tagDelimiter = null;
    String sentenceDelimiter = null;
    String elementDelimiter = null;
    int argIndex = 0;
    if (args.length < 1) {
      System.err.println("Basic usage (see Javadoc for more): java edu.stanford.nlp.parser.lexparser.LexicalizedParser parserFileOrUrl filename*");
      return;
    }

    Options op = new Options();
    ArrayList<String> optionArgs = new ArrayList<String>();
    String encoding = null;
    // while loop through option arguments
    while (argIndex < args.length && args[argIndex].charAt(0) == '-') {
      if (args[argIndex].equalsIgnoreCase("-train") ||
          args[argIndex].equalsIgnoreCase("-trainTreebank")) {
        train = true;
        int numSubArgs = numSubArgs(args, argIndex);
        argIndex++;
        if (numSubArgs >= 1) {
          treebankPath = args[argIndex];
          argIndex++;
        } else {
          throw new RuntimeException("Error: -train option must have treebankPath as first argument.");
        }
        if (numSubArgs == 2) {
          trainFilter = new NumberRangesFileFilter(args[argIndex++], true);
        } else if (numSubArgs >= 3) {
          try {
            int low = Integer.parseInt(args[argIndex]);
            int high = Integer.parseInt(args[argIndex + 1]);
            trainFilter = new NumberRangeFileFilter(low, high, true);
            argIndex += 2;
          } catch (NumberFormatException e) {
            // maybe it's a ranges expression?
            trainFilter = new NumberRangesFileFilter(args[argIndex], true);
            argIndex++;
          }
        }
      } else if (args[argIndex].equalsIgnoreCase("-train2")) {
        // train = true;     // cdm july 2005: should require -train for this
        int numSubArgs = numSubArgs(args, argIndex);
        argIndex++;
        if (numSubArgs < 2) {
          throw new RuntimeException("Error: -train2 <treebankPath> [<ranges>] <weight>.");
        }
        secondaryTreebankPath = args[argIndex++];
        secondaryTrainFilter = (numSubArgs == 3) ? new NumberRangesFileFilter(args[argIndex++], true) : null;
        secondaryTreebankWeight = Double.parseDouble(args[argIndex++]);
      } else if (args[argIndex].equalsIgnoreCase("-tLPP") && (argIndex + 1 < args.length)) {
        try {
          op.tlpParams = (TreebankLangParserParams) Class.forName(args[argIndex + 1]).newInstance();
        } catch (ClassNotFoundException e) {
          System.err.println("Class not found: " + args[argIndex + 1]);
          throw new RuntimeException(e);
        } catch (InstantiationException e) {
          System.err.println("Couldn't instantiate: " + args[argIndex + 1] + ": " + e.toString());
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          System.err.println("Illegal access" + e);
          throw new RuntimeException(e);
        }
        argIndex += 2;
      } else if (args[argIndex].equalsIgnoreCase("-encoding")) {
        // sets encoding for TreebankLangParserParams
        // redone later to override any serialized parser one read in
        encoding = args[argIndex + 1];
        op.tlpParams.setInputEncoding(encoding);
        op.tlpParams.setOutputEncoding(encoding);
        argIndex += 2;
      } else if (args[argIndex].equalsIgnoreCase("-tokenized")) {
        tokenized = true;
        argIndex += 1;
      } else if (args[argIndex].equalsIgnoreCase("-escaper")) {
        try {
          escaper = (Function<List<HasWord>, List<HasWord>>) Class.forName(args[argIndex + 1]).newInstance();
        } catch (Exception e) {
          System.err.println("Couldn't instantiate escaper " + args[argIndex + 1] + ": " + e);
        }
        argIndex += 2;
      } else if (args[argIndex].equalsIgnoreCase("-tokenizerOptions")) {
        tokenizerOptions = args[argIndex + 1];
        argIndex += 2;
      } else if (args[argIndex].equalsIgnoreCase("-tokenizerFactory")) {
        tokenizerFactoryClass = args[argIndex + 1];
        argIndex += 2;
      } else if (args[argIndex].equalsIgnoreCase("-sentences")) {
        sentenceDelimiter = args[argIndex + 1];
        if (sentenceDelimiter.equalsIgnoreCase("newline")) {
          sentenceDelimiter = "\n";
        }
        argIndex += 2;
      } else if (args[argIndex].equalsIgnoreCase("-parseInside")) {
        elementDelimiter = args[argIndex + 1];
        argIndex += 2;
      } else if (args[argIndex].equalsIgnoreCase("-tagSeparator")) {
        tagDelimiter = args[argIndex + 1];
        argIndex += 2;
      } else if (args[argIndex].equalsIgnoreCase("-loadFromSerializedFile")) {
        // load the parser from a binary serialized file
        // the next argument must be the path to the parser file
        serializedInputFileOrUrl = args[argIndex + 1];
        argIndex += 2;
      } else if (args[argIndex].equalsIgnoreCase("-loadFromTextFile")) {
        // load the parser from declarative text file
        // the next argument must be the path to the parser file
        textInputFileOrUrl = args[argIndex + 1];
        argIndex += 2;
      } else if (args[argIndex].equalsIgnoreCase("-saveToSerializedFile")) {
        saveToSerializedFile = true;
        if (numSubArgs(args, argIndex) < 1) {
          System.err.println("Missing path: -saveToSerialized filename");
        } else {
          serializedOutputFileOrUrl = args[argIndex + 1];
        }
        argIndex += 2;
      } else if (args[argIndex].equalsIgnoreCase("-saveToTextFile")) {
        // save the parser to declarative text file
        saveToTextFile = true;
        textOutputFileOrUrl = args[argIndex + 1];
        argIndex += 2;
      } else if (args[argIndex].equalsIgnoreCase("-saveTrainTrees")) {
        // save the training trees to a binary file
        op.trainOptions.trainTreeFile = args[argIndex + 1];
        argIndex += 2;
      }
      else if (args[argIndex].equalsIgnoreCase("-treebank") ||
          args[argIndex].equalsIgnoreCase("-testTreebank") ||
          args[argIndex].equalsIgnoreCase("-test")) {
        // the next arguments are the treebank path and maybe the range for testing
        int numSubArgs = numSubArgs(args, argIndex);
        if (numSubArgs > 0 && numSubArgs < 3) {
          argIndex++;
          testPath = args[argIndex++];
          if (numSubArgs == 2) {
            testFilter = new NumberRangesFileFilter(args[argIndex++], true);
          } else if (numSubArgs == 3) {
            try {
              int low = Integer.parseInt(args[argIndex]);
              int high = Integer.parseInt(args[argIndex + 1]);
              testFilter = new NumberRangeFileFilter(low, high, true);
              argIndex += 2;
            } catch (NumberFormatException e) {
              // maybe it's a ranges expression?
              testFilter = new NumberRangesFileFilter(args[argIndex++], true);
            }
          }
        } else {
          throw new IllegalArgumentException("Bad arguments after -testTreebank");
        }
      } else if (args[argIndex].equalsIgnoreCase("-tune")) {
        // the next argument is the treebank path and range for tuning
        int numSubArgs = numSubArgs(args, argIndex);
        argIndex++;
        if (numSubArgs == 1) {
          tuneFilter = new NumberRangesFileFilter(args[argIndex++], true);
        } else if (numSubArgs > 1) {
          tunePath = args[argIndex++];
          if (numSubArgs == 2) {
            tuneFilter = new NumberRangesFileFilter(args[argIndex++], true);
          } else if (numSubArgs >= 3) {
            try {
              int low = Integer.parseInt(args[argIndex]);
              int high = Integer.parseInt(args[argIndex + 1]);
              tuneFilter = new NumberRangeFileFilter(low, high, true);
              argIndex += 2;
            } catch (NumberFormatException e) {
              // maybe it's a ranges expression?
              tuneFilter = new NumberRangesFileFilter(args[argIndex++], true);
            }
          }
        }
      } else {
        int oldIndex = argIndex;
        argIndex = op.setOptionOrWarn(args, argIndex);
        for (int i = oldIndex; i < argIndex; ++i) {
          optionArgs.add(args[i]);
        }
      }
    } // end while loop through arguments

    // set up tokenizerFactory with options if provided
    if (tokenizerFactoryClass != null || tokenizerOptions != null) {
      try {
        if (tokenizerFactoryClass != null) {
          Class<TokenizerFactory<? extends HasWord>> clazz = (Class<TokenizerFactory<? extends HasWord>>) Class.forName(tokenizerFactoryClass);
          Method factoryMethod;
          if (tokenizerOptions != null) {
            factoryMethod = clazz.getMethod("newWordTokenizerFactory", String.class);
            tokenizerFactory = (TokenizerFactory<? extends HasWord>) factoryMethod.invoke(null, tokenizerOptions);
          } else {
            factoryMethod = clazz.getMethod("newTokenizerFactory");
            tokenizerFactory = (TokenizerFactory<? extends HasWord>) factoryMethod.invoke(null);
          }
        } else {
          // have options but no tokenizer factory; default to PTB
          tokenizerFactory = PTBTokenizer.PTBTokenizerFactory.newWordTokenizerFactory(tokenizerOptions);
        }
      } catch (IllegalAccessException e) {
        System.err.println("Couldn't instantiate TokenizerFactory " + tokenizerFactoryClass + " with options " + tokenizerOptions);
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        System.err.println("Couldn't instantiate TokenizerFactory " + tokenizerFactoryClass + " with options " + tokenizerOptions);
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        System.err.println("Couldn't instantiate TokenizerFactory " + tokenizerFactoryClass + " with options " + tokenizerOptions);
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        System.err.println("Couldn't instantiate TokenizerFactory " + tokenizerFactoryClass + " with options " + tokenizerOptions);
        throw new RuntimeException(e);
      }
    }

    // all other arguments are order dependent and
    // are processed in order below

    if (tuneFilter != null) {
      if (tunePath == null) {
        if (treebankPath == null) {
          throw new RuntimeException("No tune treebank path specified...");
        } else {
          System.err.println("No tune treebank path specified.  Using train path: \"" + treebankPath + '\"');
          tunePath = treebankPath;
        }
      }
      tuneTreebank = op.tlpParams.testMemoryTreebank();
      tuneTreebank.loadPath(tunePath, tuneFilter);
    }

    if (!train && op.testOptions.verbose) {
      System.err.println("Currently " + new Date());
      printArgs(args, System.err);
    }
    LexicalizedParser lp = null;
    if (train) {
      printArgs(args, System.err);
      // so we train a parser using the treebank
      GrammarCompactor compactor = null;
      if (op.trainOptions.compactGrammar() == 3) {
        compactor = new ExactGrammarCompactor(op, false, false);
      }
      Treebank trainTreebank = makeTreebank(treebankPath, op, trainFilter);
      if (secondaryTreebankPath != null) {
        DiskTreebank secondaryTrainTreebank = makeSecondaryTreebank(secondaryTreebankPath, op, secondaryTrainFilter);
        lp = new LexicalizedParser(trainTreebank, secondaryTrainTreebank, secondaryTreebankWeight, compactor, op);
      } else {
        lp = new LexicalizedParser(trainTreebank, compactor, op, tuneTreebank);
      }
    } else if (textInputFileOrUrl != null) {
      // so we load the parser from a text grammar file
      lp = new LexicalizedParser(textInputFileOrUrl, true, op);
    } else {
      // so we load a serialized parser
      if (serializedInputFileOrUrl == null && argIndex < args.length) {
        // the next argument must be the path to the serialized parser
        serializedInputFileOrUrl = args[argIndex];
        argIndex++;
      }
      if (serializedInputFileOrUrl == null) {
        System.err.println("No grammar specified, exiting...");
        return;
      }
      String[] extraArgs = new String[optionArgs.size()];
      extraArgs = optionArgs.toArray(extraArgs);
      try {
        lp = new LexicalizedParser(serializedInputFileOrUrl, op, extraArgs);
        op = lp.op;
      } catch (IllegalArgumentException e) {
        System.err.println("Error loading parser, exiting...");
        throw e;
      }
    }

    // the following has to go after reading parser to make sure
    // op and tlpParams are the same for train and test
    // THIS IS BUTT UGLY BUT IT STOPS USER SPECIFIED ENCODING BEING
    // OVERWRITTEN BY ONE SPECIFIED IN SERIALIZED PARSER
    if (encoding != null) {
      op.tlpParams.setInputEncoding(encoding);
      op.tlpParams.setOutputEncoding(encoding);
    }

    if (testFilter != null || testPath != null) {
      if (testPath == null) {
        if (treebankPath == null) {
          throw new RuntimeException("No test treebank path specified...");
        } else {
          System.err.println("No test treebank path specified.  Using train path: \"" + treebankPath + '\"');
          testPath = treebankPath;
        }
      }
      testTreebank = op.tlpParams.testMemoryTreebank();
      testTreebank.loadPath(testPath, testFilter);
    }

    op.trainOptions.sisterSplitters = new HashSet<String>(Arrays.asList(op.tlpParams.sisterSplitters()));

    // at this point we should be sure that op.tlpParams is
    // set appropriately (from command line, or from grammar file),
    // and will never change again.  -- Roger

    // Now what do we do with the parser we've made
    if (saveToTextFile) {
      // save the parser to textGrammar format
      if (textOutputFileOrUrl != null) {
        saveParserDataToTextFile(lp.pd, textOutputFileOrUrl);
      } else {
        System.err.println("Usage: must specify a text grammar output path");
      }
    }
    if (saveToSerializedFile) {
      if (serializedOutputFileOrUrl != null) {
        saveParserDataToSerialized(lp.pd, serializedOutputFileOrUrl);
      } else if (textOutputFileOrUrl == null && testTreebank == null) {
        // no saving/parsing request has been specified
        System.err.println("usage: " + "java edu.stanford.nlp.parser.lexparser.LexicalizedParser " + "-train trainFilesPath [fileRange] -saveToSerializedFile serializedParserFilename");
      }
    }

    if (op.testOptions.verbose || train) {
      // Tell the user a little or a lot about what we have made
      // get lexicon size separately as it may have its own prints in it....
      // TODO: changed pparser to pd here.  Make sure that still works
      String lexNumRules = lp.pd.lex != null ? Integer.toString(lp.pd.lex.numRules()): "";
      System.err.println("Grammar\tStates\tTags\tWords\tUnaryR\tBinaryR\tTaggings");
      System.err.println("Grammar\t" +
          lp.pd.stateIndex.size() + '\t' +
          lp.pd.tagIndex.size() + '\t' +
          lp.pd.wordIndex.size() + '\t' +
          (lp.pd.ug != null ? lp.pd.ug.numRules(): "") + '\t' +
          (lp.pd.bg != null ? lp.pd.bg.numRules(): "") + '\t' +
          lexNumRules);
      System.err.println("ParserPack is " + op.tlpParams.getClass().getName());
      System.err.println("Lexicon is " + lp.pd.lex.getClass().getName());
      if (op.testOptions.verbose) {
        System.err.println("Tags are: " + lp.pd.tagIndex);
        // System.err.println("States are: " + lp.pd.stateIndex); // This is too verbose. It was already printed out by the below printOptions command if the flag -printStates is given!
      }
      printOptions(false, op);
    }

    if (testTreebank != null) {
      // test parser on treebank
      lp.parserQuery().testOnTreebank(testTreebank);
    } else if (argIndex >= args.length) {
      // no more arguments, so we just parse our own test sentence
      PrintWriter pwOut = op.tlpParams.pw();
      PrintWriter pwErr = op.tlpParams.pw(System.err);
      LexicalizedParserQuery pq = lp.parserQuery();
      if (pq.parse(op.tlpParams.defaultTestSentence())) {
        op.testOptions.treePrint(op.tlpParams).printTree(pq.getBestParse(), pwOut);
      } else {
        pwErr.println("Error. Can't parse test sentence: " +
                      op.tlpParams.defaultTestSentence());
      }
    } else {
      // We parse filenames given by the remaining arguments
      lp.parserQuery().parseFiles(args, argIndex, tokenized, tokenizerFactory, elementDelimiter, sentenceDelimiter, escaper, tagDelimiter);
    }
  } // end main

} // end class LexicalizedParser

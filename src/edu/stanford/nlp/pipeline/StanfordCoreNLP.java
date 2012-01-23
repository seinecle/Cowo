//
// StanfordCoreNLP -- a suite of NLP tools.
// Copyright (c) 2009-2011 The Board of Trustees of
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
//

package edu.stanford.nlp.pipeline;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.io.EncodingFileWriter;
import edu.stanford.nlp.io.FileSequentialCollection;
import edu.stanford.nlp.objectbank.ObjectBank;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ProcessingInstruction;
import nu.xom.Serializer;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.ie.NERClassifierCombiner;
import edu.stanford.nlp.ie.machinereading.structure.EntityMention;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;
import edu.stanford.nlp.ie.machinereading.structure.RelationMention;
import edu.stanford.nlp.ie.regexp.NumberSequenceClassifier;
import edu.stanford.nlp.ie.regexp.RegexNERSequenceClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.DocDateAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NormalizedNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.time.TimeAnnotations.TimexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TrueCaseAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TrueCaseTextAnnotation;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.time.Timex;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Factory;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.ReflectionLoading;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.util.Timing;

/**
 * This is a pipeline that takes in a string and returns various analyzed
 * linguistic forms.
 * The String is tokenized via a tokenizer (such as PTBTokenizerAnnotator), and
 * then other sequence model style annotation can be used to add things like
 * lemmas, POS tags, and named entities.  These are returned as a list of CoreLabels.
 * Other analysis components build and store parse trees, dependency graphs, etc.
 * <p>
 * This class is designed to apply multiple Annotators
 * to an Annotation.  The idea is that you first
 * build up the pipeline by adding Annotators, and then
 * you take the objects you wish to annotate and pass
 * them in and get in return a fully annotated object.
 * Please see the package level javadocs for sample usage
 * and a more complete description.
 * <p>
 * The main entry point for the API is StanfordCoreNLP.process()
 * <p>
 * <i>Implementation note:</i> There are other annotation pipelines, but they
 * don't extend this one. Look for classes that implement Annotator and which
 * have "Pipeline" in their name.
 *
 * @author Jenny Finkel
 * @author Anna Rafferty
 * @author Christopher Manning
 * @author Mihai Surdeanu
 * @author Steven Bethard
 */

public class StanfordCoreNLP extends AnnotationPipeline {

  /*
   * List of all known annotator property names
   * Add new annotators and/or annotators from other groups here!
   */
  public static final String STANFORD_TOKENIZE = "tokenize";
  public static final String STANFORD_CLEAN_XML = "cleanxml";
  public static final String STANFORD_SSPLIT = "ssplit";
  public static final String STANFORD_POS = "pos";
  public static final String STANFORD_LEMMA = "lemma";
  public static final String STANFORD_NER = "ner";
  public static final String STANFORD_REGEXNER = "regexner";
  public static final String STANFORD_GENDER = "gender";
  private static final String STANFORD_NFL_TOKENIZE = "nfltokenize"; // hidden annotator constructed automagically for the NFL domain
  public static final String STANFORD_NFL = "nfl";
  public static final String STANFORD_TRUECASE = "truecase";
  public static final String STANFORD_PARSE = "parse";
  public static final String STANFORD_DETERMINISTIC_COREF = "dcoref";

  public static final String POS_TAGGING = STANFORD_POS + " " + STANFORD_PARSE;

  // other constants
  public static final String CUSTOM_ANNOTATOR_PREFIX = "customAnnotatorClass.";
  private static final String PROPS_SUFFIX = ".properties";
  public static final String NEWLINE_SPLITTER_PROPERTY = "ssplit.eolonly";
  // the namespace is set in the XSLT file
  private static final String NAMESPACE_URI = null;
  private static final String STYLESHEET_NAME = "CoreNLP-to-HTML.xsl";

  /** Formats the constituent parse trees for display */
  private TreePrint constituentTreePrinter;
  /** Formats the dependency parse trees for human-readable display */
  private TreePrint dependencyTreePrinter;
  /** Converts the constituent tree to a set of dependencies (for display) */
  private GrammaticalStructureFactory gsf;

  /** Stores the overall number of words processed */
  private int numWords;

  /** Maintains the shared pool of annotators */
  private static AnnotatorPool pool = null;

  private Properties properties;

  /**
   * Constructs a pipeline using as properties the properties file found in the classpath
   */
  public StanfordCoreNLP() {
    this((Properties) null);
  }

  /**
   * Construct a basic pipeline. The Properties will be used to determine
   * which annotators to create, and a default AnnotatorPool will be used
   * to create the annotators.
   *
   */
  public StanfordCoreNLP(Properties props)  {
    this(props, true);
  }

  public StanfordCoreNLP(AnnotatorPool pool, Properties props) {
    this(pool, props, true);
  }

  public StanfordCoreNLP(Properties props, boolean enforceRequirements)  {
    this(null, props, enforceRequirements);
  }

  /**
   * Construct a basic pipeline. The Properties will be used to determine
   * which annotators to create, and the AnnotatorPool will be used to create
   * the specified annotators.
   *
   */
  public StanfordCoreNLP(AnnotatorPool pool, Properties props, boolean enforceRequirements) {
    construct(pool, props, enforceRequirements);
  }

  /**
   * Constructs a pipeline with the properties read from this file, which must be found in the classpath
   * @param propsFileNamePrefix
   */
  public StanfordCoreNLP(String propsFileNamePrefix) {
    this(propsFileNamePrefix, true);
  }

  public StanfordCoreNLP(String propsFileNamePrefix, boolean enforceRequirements) {
    Properties props = loadProperties(propsFileNamePrefix);
    if(props == null){
      throw new RuntimeException("ERROR: cannot find properties file \"" + propsFileNamePrefix + "\" in the classpath!");
    }
    construct(null, props, enforceRequirements);
  }

  //
  // property-specific methods
  //

  private static String getRequiredProperty(Properties props, String name) {
    String val = props.getProperty(name);
    if (val == null) {
      System.err.println("Missing property \"" + name + "\"!");
      printRequiredProperties(System.err);
      throw new RuntimeException("Missing property: \"" + name + '\"');
    }
    return val;
  }

  /**
   * Finds the properties file in the classpath and loads the properties from there
   */
  private static Properties loadPropertiesFromClasspath() {
    List<String> validNames = Arrays.asList("StanfordCoreNLP", "edu.stanford.nlp.pipeline.StanfordCoreNLP");
    for(String name: validNames){
      Properties props = loadProperties(name);
      if(props != null) return props;
    }
    throw new RuntimeException("ERROR: Could not find properties file in the classpath!");
  }

  private static Properties loadProperties(String name) {
    return loadProperties(name, Thread.currentThread().getContextClassLoader());
  }

  private static Properties loadProperties(String name, ClassLoader loader){
    if(name.endsWith (PROPS_SUFFIX)) name = name.substring(0, name.length () - PROPS_SUFFIX.length ());
    name = name.replace('.', '/');
    name += PROPS_SUFFIX;
    Properties result = null;

    // Returns null on lookup failures
    System.err.println("Searching for resource: " + name);
    InputStream in = loader.getResourceAsStream (name);
    try {
      if (in != null) {
        result = new Properties ();
        result.load(in); // Can throw IOException
      }
    } catch (IOException e) {
      result = null;
    } finally {
      if (in != null) try { in.close (); } catch (Throwable ignore) {}
    }

    return result;
  }

  /** Fetches the Properties object used to construct this Annotator */
  public Properties getProperties() { return properties; }

  //
  // AnnotatorPool construction support
  //

  private void construct(AnnotatorPool pool, Properties props, boolean enforceRequirements) {
    this.numWords = 0;
    this.constituentTreePrinter = new TreePrint("penn");
    this.dependencyTreePrinter = new TreePrint("typedDependenciesCollapsed");
    this.gsf = new PennTreebankLanguagePack().grammaticalStructureFactory();

    if(props == null){
      // if undefined, find the props file in the classpath
      props = loadPropertiesFromClasspath();
    } else if (props.getProperty("annotators") == null) {
      // this happens when some command line options are specified (e.g just "-filelist") but no properties file is.
      // we use the options that are given and let them override the default properties from the class path properties.
      Properties fromClassPath = loadPropertiesFromClasspath();
      fromClassPath.putAll(props);
      props = fromClassPath;
    }
    this.properties = props;
    if(pool == null) {
      // if undefined, use the default pool
      pool = getDefaultAnnotatorPool(props);
    }

    // define requirements
    Map<String, Requirement> requires = new HashMap<String, Requirement>();
    if (enforceRequirements){
      requires.put(STANFORD_TOKENIZE, new Requirement());
      requires.put(STANFORD_CLEAN_XML, new Requirement(STANFORD_TOKENIZE));
      requires.put(STANFORD_SSPLIT, new Requirement(STANFORD_TOKENIZE));
      requires.put(STANFORD_POS, new Requirement(STANFORD_TOKENIZE, STANFORD_SSPLIT));
      requires.put(STANFORD_LEMMA, new Requirement(STANFORD_TOKENIZE, STANFORD_SSPLIT, POS_TAGGING));
      requires.put(STANFORD_NER, new Requirement(STANFORD_TOKENIZE, STANFORD_SSPLIT, POS_TAGGING, STANFORD_LEMMA));
      requires.put(STANFORD_REGEXNER, new Requirement(STANFORD_TOKENIZE, STANFORD_SSPLIT));
      requires.put(STANFORD_GENDER, new Requirement(STANFORD_TOKENIZE, STANFORD_SSPLIT));
      requires.put(STANFORD_TRUECASE, new Requirement(STANFORD_TOKENIZE, STANFORD_SSPLIT, POS_TAGGING, STANFORD_LEMMA));
      requires.put(STANFORD_NFL, new Requirement(STANFORD_TOKENIZE, STANFORD_SSPLIT, POS_TAGGING, STANFORD_LEMMA, STANFORD_NER, STANFORD_PARSE));
      requires.put(STANFORD_PARSE, new Requirement(STANFORD_TOKENIZE, STANFORD_SSPLIT));
      requires.put(STANFORD_DETERMINISTIC_COREF, new Requirement(STANFORD_TOKENIZE, STANFORD_SSPLIT, POS_TAGGING, STANFORD_NER, STANFORD_PARSE));
    }

    // now construct the annotators from the given props in the given order
    List<String> annoNames = Arrays.asList(getRequiredProperty(props, "annotators").split("[, \t]+"));
    HashSet<String> alreadyAddedAnnoNames = new HashSet<String>();
    for (String name : annoNames) {
      //System.err.println("Adding annotator " + name);

      if (requires.containsKey(name)) {
        String missingRequirement =
          requires.get(name).getMissingRequirement(alreadyAddedAnnoNames);
        if (missingRequirement != null) {
          String fmt = "annotator \"%s\" requires annotator \"%s\"";
          throw new IllegalArgumentException(String.format(fmt, name, missingRequirement));
        }
      }

      Annotator an = pool.get(name);
      this.addAnnotator(an);

      // the NFL domain requires several post-processing rules after
      // tokenization.  add these transparently if the NFL annotator
      // is required
      if (name.equals(STANFORD_TOKENIZE) &&
          annoNames.contains(STANFORD_NFL) &&
          !annoNames.contains(STANFORD_NFL_TOKENIZE)) {
        Annotator pp = pool.get(STANFORD_NFL_TOKENIZE);
        this.addAnnotator(pp);
      }

      alreadyAddedAnnoNames.add(name);
    }
  }

  private static synchronized AnnotatorPool getDefaultAnnotatorPool(final Properties props) {
    // if the pool already exists reuse!
    if(pool != null) return pool;

    pool = new AnnotatorPool();

    //
    // tokenizer: breaks text into a sequence of tokens
    // this is required for all following annotators!
    //
    pool.register(STANFORD_TOKENIZE, new Factory<Annotator>() {
      private static final long serialVersionUID = 1L;
      public Annotator create() {
        if (Boolean.valueOf(props.getProperty("tokenize.whitespace",
                                              "false"))) {
          return new WhitespaceTokenizerAnnotator(props);
        } else {
          String options =
            props.getProperty("tokenize.options",
                              PTBTokenizerAnnotator.DEFAULT_OPTIONS);
          return new PTBTokenizerAnnotator(false, options);
        }
      }
    });

    pool.register(STANFORD_CLEAN_XML, new Factory<Annotator>() {
      private static final long serialVersionUID = 1L;
      public Annotator create() {
        String xmlTags =
          props.getProperty("clean.xmltags",
                            CleanXmlAnnotator.DEFAULT_XML_TAGS);
        String sentenceEndingTags =
          props.getProperty("clean.sentenceendingtags",
                            CleanXmlAnnotator.DEFAULT_SENTENCE_ENDERS);
        String allowFlawedString = props.getProperty("clean.allowflawedxml");
        boolean allowFlawed = CleanXmlAnnotator.DEFAULT_ALLOW_FLAWS;
        if (allowFlawedString != null)
          allowFlawed = Boolean.valueOf(allowFlawedString);
        String dateTags =
          props.getProperty("clean.datetags",
                            CleanXmlAnnotator.DEFAULT_DATE_TAGS);
        return new CleanXmlAnnotator(xmlTags,
            sentenceEndingTags,
            dateTags,
            allowFlawed);
      }
    });

    //
    // sentence splitter: splits the above sequence of tokens into
    // sentences.  This is required when processing entire documents or
    // text consisting of multiple sentences
    //
    pool.register(STANFORD_SSPLIT, new Factory<Annotator>() {
      private static final long serialVersionUID = 1L;
      public Annotator create() {
        if (Boolean.valueOf(props.getProperty(NEWLINE_SPLITTER_PROPERTY,
                                              "false"))) {
          WordsToSentencesAnnotator wts =
            WordsToSentencesAnnotator.newlineSplitter(false);
          return wts;
        } else {
          WordsToSentencesAnnotator wts =
            new WordsToSentencesAnnotator(false);

          // regular boundaries
          String bounds = props.getProperty("ssplit.boundariesToDiscard");
          if (bounds != null){
            String [] toks = bounds.split(",");
            // for(int i = 0; i < toks.length; i ++)
            //   System.err.println("BOUNDARY: " + toks[i]);
            wts.setSentenceBoundaryToDiscard(new HashSet<String>
                                             (Arrays.asList(toks)));
          }

          // HTML boundaries
          bounds = props.getProperty("ssplit.htmlBoundariesToDiscard");
          if (bounds != null){
            String [] toks = bounds.split(",");
            wts.addHtmlSentenceBoundaryToDiscard(new HashSet<String>
                                                 (Arrays.asList(toks)));
          }

          // Treat as one sentence
          String isOneSentence = props.getProperty("ssplit.isOneSentence");
          if (isOneSentence != null){
            wts.setOneSentence(Boolean.parseBoolean(isOneSentence));
          }

          return wts;
        }
      }
    });

    //
    // POS tagger
    //
    pool.register(STANFORD_POS, new Factory<Annotator>() {
      private static final long serialVersionUID = 1L;
      public Annotator create() {
        try {
          String maxLenStr = props.getProperty("pos.maxlen");
          int maxLen = Integer.MAX_VALUE;
          if(maxLenStr != null) maxLen = Integer.parseInt(maxLenStr);
          return new POSTaggerAnnotator(props.getProperty("pos.model", DefaultPaths.DEFAULT_POS_MODEL), true, maxLen);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });

    //
    // Lemmatizer
    //
    pool.register(STANFORD_LEMMA, new Factory<Annotator>() {
      private static final long serialVersionUID = 1L;
      public Annotator create() {
        return new MorphaAnnotator(false);
      }
    });

    //
    // NER
    //
    pool.register(STANFORD_NER, new Factory<Annotator>() {
      private static final long serialVersionUID = 1L;
      public Annotator create() {
        List<String> models = new ArrayList<String>();
        List<Pair<String, String>> modelNames = new ArrayList<Pair<String,String>>();
        modelNames.add(new Pair<String, String>("ner.model", null));
        modelNames.add(new Pair<String, String>("ner.model.3class", DefaultPaths.DEFAULT_NER_THREECLASS_MODEL));
        modelNames.add(new Pair<String, String>("ner.model.7class", DefaultPaths.DEFAULT_NER_MUC_MODEL));
        modelNames.add(new Pair<String, String>("ner.model.MISCclass", DefaultPaths.DEFAULT_NER_CONLL_MODEL));

        for (Pair<String, String> name : modelNames) {
          String model = props.getProperty(name.first, name.second);
          if (model != null && model.length() > 0) {
            models.addAll(Arrays.asList(model.split(",")));
          }
        }
        if (models.isEmpty()) {
          throw new RuntimeException("no NER models specified");
        }
        NERClassifierCombiner nerCombiner;
        try {
          boolean applyNumericClassifiers =
            PropertiesUtils.getBool(props,
                NERClassifierCombiner.APPLY_NUMERIC_CLASSIFIERS_PROPERTY,
                NERClassifierCombiner.APPLY_NUMERIC_CLASSIFIERS_DEFAULT);
          boolean useSUTime =
            PropertiesUtils.getBool(props,
                NumberSequenceClassifier.USE_SUTIME_PROPERTY,
                NumberSequenceClassifier.USE_SUTIME_DEFAULT);
          nerCombiner = new NERClassifierCombiner(applyNumericClassifiers,
                useSUTime, props,
                models.toArray(new String[models.size()]));
        } catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        }
        // ms 2009, no longer needed: the functionality of all these annotators is now included in NERClassifierCombiner
        /*
        AnnotationPipeline pipeline = new AnnotationPipeline();
        pipeline.addAnnotator(new NERCombinerAnnotator(nerCombiner, false));
        pipeline.addAnnotator(new NumberAnnotator(false));
        pipeline.addAnnotator(new TimeWordAnnotator(false));
        pipeline.addAnnotator(new QuantifiableEntityNormalizingAnnotator(false, false));
        return pipeline;
        */
        return new NERCombinerAnnotator(nerCombiner, false);
      }
    });

    //
    // Regex NER
    //
    pool.register(STANFORD_REGEXNER, new Factory<Annotator>() {
      private static final long serialVersionUID = 1L;
      public Annotator create() {
        String mapping = props.getProperty("regexner.mapping", DefaultPaths.DEFAULT_REGEXNER_RULES);
        String ignoreCase = props.getProperty("regexner.ignorecase", "false");
        String validPosPattern = props.getProperty("regexner.validpospattern", RegexNERSequenceClassifier.DEFAULT_VALID_POS);
        return new RegexNERAnnotator(mapping, Boolean.valueOf(ignoreCase), validPosPattern);
      }
    });

    //
    // Gender Annotator
    //
    pool.register(STANFORD_GENDER, new Factory<Annotator>() {
      private static final long serialVersionUID = 1L;
      public Annotator create() {
        return new GenderAnnotator(false, props.getProperty("gender.firstnames", DefaultPaths.DEFAULT_GENDER_FIRST_NAMES));
      }
    });


    //
    // True caser
    //
    pool.register(STANFORD_TRUECASE, new Factory<Annotator>() {
      private static final long serialVersionUID = 1L;
      public Annotator create() {
        String model = props.getProperty("truecase.model", DefaultPaths.DEFAULT_TRUECASE_MODEL);
        String bias = props.getProperty("truecase.bias", TrueCaseAnnotator.DEFAULT_MODEL_BIAS);
        String mixed = props.getProperty("truecase.mixedcasefile", DefaultPaths.DEFAULT_TRUECASE_DISAMBIGUATION_LIST);
        return new TrueCaseAnnotator(model, bias, mixed, false);
      }
    });

    //
    // Post-processing tokenization rules for the NFL domain
    //
    pool.register(STANFORD_NFL_TOKENIZE, new Factory<Annotator>() {
      private static final long serialVersionUID = 1L;
      public Annotator create() {
        final String className =
          "edu.stanford.nlp.pipeline.NFLTokenizerAnnotator";
        return ReflectionLoading.loadByReflection(className);
      }
    });

    //
    // Entity and relation extraction for the NFL domain
    //
    pool.register(STANFORD_NFL, new Factory<Annotator>() {
      private static final long serialVersionUID = 1L;
      public Annotator create() {
        // these paths now extracted inside c'tor
        // String gazetteer = props.getProperty("nfl.gazetteer", DefaultPaths.DEFAULT_NFL_GAZETTEER);
        // String entityModel = props.getProperty("nfl.entity.model", DefaultPaths.DEFAULT_NFL_ENTITY_MODEL);
        // String relationModel = props.getProperty("nfl.relation.model", DefaultPaths.DEFAULT_NFL_RELATION_MODEL);
        final String className = "edu.stanford.nlp.pipeline.NFLAnnotator";
        return ReflectionLoading.loadByReflection(className, props);
      }
    });

    //
    // Parser
    //
    pool.register(STANFORD_PARSE, new Factory<Annotator>() {
      private static final long serialVersionUID = 1L;
      public Annotator create() {
        String parserType = props.getProperty("parser.type", "stanford");
        String maxLenStr = props.getProperty("parser.maxlen");

        if (parserType.equalsIgnoreCase("stanford")) {
          int maxLen = -1;
          if (maxLenStr != null) {
            maxLen = Integer.parseInt(maxLenStr);
          }
          String parserPath = props.getProperty("parser.model",
                                    DefaultPaths.DEFAULT_PARSER_MODEL);
          boolean parserDebug =
            PropertiesUtils.hasProperty(props, "parser.debug");
          String parserFlags = props.getProperty("parser.flags");
          String[] parserFlagList;
          if (parserFlags == null) {
            parserFlagList = ParserAnnotator.DEFAULT_FLAGS;
          } else if (parserFlags.trim().equals("")) {
            parserFlagList = StringUtils.EMPTY_STRING_ARRAY;
          } else {
            parserFlagList = parserFlags.trim().split("\\s+");
          }
          ParserAnnotator anno = new ParserAnnotator(parserPath, parserDebug,
                                                     maxLen, parserFlagList);
          return anno;
        } else if (parserType.equalsIgnoreCase("charniak")) {
          String model = props.getProperty("parser.model");
          String parserExecutable = props.getProperty("parser.executable");
          if (model == null || parserExecutable == null) {
            throw new RuntimeException("Both parser.model and parser.executable properties must be specified if parser.type=charniak");
          }
          int maxLen = 399;
          if (maxLenStr != null) {
            maxLen = Integer.parseInt(maxLenStr);
          }

          CharniakParserAnnotator anno = new CharniakParserAnnotator(model, parserExecutable, false, maxLen);

          return anno;
        } else {
          throw new RuntimeException("Unknown parser type: " + parserType + " (currently supported: stanford and charniak)");
        }
      }
    });

    //
    // Coreference resolution
    //
    pool.register(STANFORD_DETERMINISTIC_COREF, new Factory<Annotator>() {
      private static final long serialVersionUID = 1L;
      public Annotator create() {
        return new DeterministicCorefAnnotator(props);
      }
    });

    // add annotators loaded via reflection from classnames specified
    // in the properties
    for (Object propertyKey : props.keySet()) {
      if (!(propertyKey instanceof String))
        continue; // should this be an Exception?
      String property = (String) propertyKey;
      if (property.startsWith(CUSTOM_ANNOTATOR_PREFIX)) {
        final String customName =
          property.substring(CUSTOM_ANNOTATOR_PREFIX.length());
        final String customClassName = props.getProperty(property);
        System.err.println("Registering annotator " + customName +
                           " with class " + customClassName);
        pool.register(customName, new Factory<Annotator>() {
          private static final long serialVersionUID = 1L;
          private final String name = customName;
          private final String className = customClassName;
          private final Properties properties = props;
          public Annotator create() {
            return ReflectionLoading.loadByReflection(className, name,
                                                      properties);
          }
        });
      }
    }


    //
    // add more annotators here!
    //
    return pool;
  }

  public static synchronized Annotator getExistingAnnotator(String name) {
    if(pool == null){
      System.err.println("ERROR: attempted to fetch annotator \"" + name + "\" before the annotator pool was created!");
      return null;
    }
    try {
      Annotator a =  pool.get(name);
      return a;
    } catch(IllegalArgumentException e) {
      System.err.println("ERROR: attempted to fetch annotator \"" + name + "\" but the annotator pool does not store any such type!");
      return null;
    }
  }

  @Override
  public void annotate(Annotation annotation) {
    super.annotate(annotation);
    List<CoreLabel> words = annotation.get(CoreAnnotations.TokensAnnotation.class);
    if (words != null) {
      numWords += words.size();
    }
  }

  /**
   * Runs the entire pipeline on the content of the given text passed in.
   * @param text The text to process
   * @return An Annotation object containing the output of all annotators
   */
  public Annotation process(String text) {
    Annotation annotation = new Annotation(text);
    annotate(annotation);
    return annotation;
  }

  //
  // output and formatting methods (including XML-specific methods)
  //

  /**
   * Displays the output of all annotators in a format easily readable by people.
   * @param annotation Contains the output of all annotators
   * @param os The output stream
   */
  public void prettyPrint(Annotation annotation, PrintWriter os) {
    String beamAsString = properties.getProperty("printable.relation.beam");
    double beam = 0.0;
    if (beamAsString != null) {
      beam = Double.parseDouble(beamAsString);
    }

    List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

    // Display docid if available
    String docId =  annotation.get(CoreAnnotations.DocIDAnnotation.class);
    if (docId != null) {
      List<CoreLabel> tokens = annotation.get(CoreAnnotations.TokensAnnotation.class);
      int nSentences = (sentences != null)? sentences.size():0;
      int nTokens = (tokens != null)? tokens.size():0;
      os.printf("Document: ID=%s (%d sentences, %d tokens)\n", docId, nSentences, nTokens);
    }

    // display each sentence in this annotation
    if (sentences != null) {
      for(int i = 0, sz = sentences.size(); i < sz; i ++) {
        CoreMap sentence = sentences.get(i);
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        os.printf("Sentence #%d (%d tokens):\n", (i + 1), tokens.size());

        String text = sentence.get(CoreAnnotations.TextAnnotation.class);
        os.println(text);

        // display the token-level annotations
        String[] tokenAnnotations = {
                "Text", "PartOfSpeech", "Lemma", "Answer", "NamedEntityTag", "CharacterOffsetBegin", "CharacterOffsetEnd", "NormalizedNamedEntityTag", "Timex", "TrueCase", "TrueCaseText" };
        for (CoreLabel token: tokens) {
          os.print(token.toShorterString(tokenAnnotations));
          os.print(' ');
        }
        os.println();

        // display the parse tree for this sentence
        Tree tree = sentence.get(TreeAnnotation.class);
        if (tree != null){
          constituentTreePrinter.printTree(tree, os);
          dependencyTreePrinter.printTree(tree, os);
        }

        // display MachineReading entities and relations
        List<EntityMention> entities = sentence.get(MachineReadingAnnotations.EntityMentionsAnnotation.class);
        if (entities != null) {
          System.err.println("Extracted the following MachineReading entity mentions:");
          for (EntityMention e : entities) {
            System.err.println("\t" + e);
          }
        }
        List<RelationMention> relations = sentence.get(MachineReadingAnnotations.RelationMentionsAnnotation.class);
        if(relations != null){
          System.err.println("Extracted the following MachineReading relation mentions:");
          for(RelationMention r: relations){
            if(r.printableObject(beam)){
              System.err.println(r);
            }
          }
        }
      }
    }

    // display the old-style doc-level coref annotations
    // this is not supported anymore!
    //String corefAnno = annotation.get(CorefPLAnnotation.class);
    //if(corefAnno != null) os.println(corefAnno);

    // display the new-style coreference graph
    Map<Integer, CorefChain> corefChains =
      annotation.get(CorefCoreAnnotations.CorefChainAnnotation.class);
    if (corefChains != null && sentences != null) {
      List<List<CoreLabel>> sents = new ArrayList<List<CoreLabel>>();
      for (CoreMap sentence : sentences) {
        List<CoreLabel> tokens =
          sentence.get(CoreAnnotations.TokensAnnotation.class);
        sents.add(tokens);
      }

      for (CorefChain chain : corefChains.values()) {
        CorefChain.CorefMention representative =
          chain.getRepresentativeMention();
        boolean outputHeading = false;
        for (CorefChain.CorefMention mention : chain.getCorefMentions()) {
          if (mention == representative)
            continue;
          if (!outputHeading) {
            outputHeading = true;
            os.println("Coreference set:");
          }
          // all offsets start at 1!
          os.println("\t(" + mention.sentNum + "," +
              mention.headIndex + ",[" +
              mention.startIndex + "," +
              mention.endIndex + ")) -> (" +
              representative.sentNum + "," +
              representative.headIndex + ",[" +
              representative.startIndex + "," +
              representative.endIndex + ")), that is: \"" +
              mention.mentionSpan + "\" -> \"" +
              representative.mentionSpan + "\"");
        }
      }
    }

    os.flush();
  }

  /**
   * Wrapper around xmlPrint(Annotation, OutputStream).
   * Added for backward compatibility.
   * @param annotation
   * @param w The Writer to send the output to
   * @throws IOException
   */
  public void xmlPrint(Annotation annotation, Writer w) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    xmlPrint(annotation, os); // this builds it as UTF-8, always
    w.write(new String(os.toByteArray(), "UTF-8")); // This will convert it to something else
    w.flush();
  }

  /**
   * Displays the output of all annotators in XML format.
   * @param annotation Contains the output of all annotators
   * @param os The output stream
   * @throws IOException
   */
  public void xmlPrint(Annotation annotation, OutputStream os) throws IOException {
    Document xmlDoc = annotationToDoc(annotation);
    Serializer ser = new Serializer(os, "UTF-8");
    ser.setIndent(2);
    ser.setMaxLength(0);
    ser.write(xmlDoc);
    ser.flush();
  }

  /**
   * Converts the given annotation to an XML document
   */
  public Document annotationToDoc(Annotation annotation) {
    //
    // create the XML document with the root node pointing to the namespace URL
    //
    Element root = new Element("root", NAMESPACE_URI);
    Document xmlDoc = new Document(root);
    ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet",
          "href=\"" + STYLESHEET_NAME + "\" type=\"text/xsl\"");
    xmlDoc.insertChild(pi, 0);
    Element docElem = new Element("document", NAMESPACE_URI);
    root.appendChild(docElem);

    String docId =  annotation.get(CoreAnnotations.DocIDAnnotation.class);
    if (docId != null) {
      setSingleElement(docElem, "docId", NAMESPACE_URI, docId);
    }

    String docDate = annotation.get(DocDateAnnotation.class);
    if(docDate != null){
      setSingleElement(docElem, "docDate", NAMESPACE_URI, docDate);
    }

    Element sentencesElem = new Element("sentences", NAMESPACE_URI);
    docElem.appendChild(sentencesElem);

    //
    // save the info for each sentence in this doc
    //
    if(annotation.get(CoreAnnotations.SentencesAnnotation.class) != null){
      int sentCount = 1;
      for (CoreMap sentence: annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
        Element sentElem = new Element("sentence", NAMESPACE_URI);
        sentElem.addAttribute(new Attribute("id", Integer.toString(sentCount)));
        sentCount ++;

        // add the word table with all token-level annotations
        Element wordTable = new Element("tokens", NAMESPACE_URI);
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        for(int j = 0; j < tokens.size(); j ++){
          Element wordInfo = new Element("token", NAMESPACE_URI);
          addWordInfo(wordInfo, tokens.get(j), j + 1, NAMESPACE_URI);
          wordTable.appendChild(wordInfo);
        }
        sentElem.appendChild(wordTable);

        // add tree info
        Tree tree = sentence.get(TreeAnnotation.class);

        if(tree != null){
          // add the constituent tree for this sentence
          Element parseInfo = new Element("parse", NAMESPACE_URI);
          addConstituentTreeInfo(parseInfo, tree);
          sentElem.appendChild(parseInfo);

          // add the dependencies for this sentence
          Element depInfo = new Element("basic-dependencies", NAMESPACE_URI);
          addDependencyTreeInfo(depInfo, sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class), tokens, NAMESPACE_URI);
          sentElem.appendChild(depInfo);

          depInfo = new Element("collapsed-dependencies", NAMESPACE_URI);
          addDependencyTreeInfo(depInfo, sentence.get(SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation.class), tokens, NAMESPACE_URI);
          sentElem.appendChild(depInfo);

          depInfo = new Element("collapsed-ccprocessed-dependencies", NAMESPACE_URI);
          addDependencyTreeInfo(depInfo, sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class), tokens, NAMESPACE_URI);
          sentElem.appendChild(depInfo);
        }

        // add the MR entities and relations
        List<EntityMention> entities = sentence.get(MachineReadingAnnotations.EntityMentionsAnnotation.class);
        List<RelationMention> relations = sentence.get(MachineReadingAnnotations.RelationMentionsAnnotation.class);
        if (entities != null && entities.size() > 0){
          Element mrElem = new Element("MachineReading", NAMESPACE_URI);
          Element entElem = new Element("entities", NAMESPACE_URI);
          addEntities(entities, entElem, NAMESPACE_URI);
          mrElem.appendChild(entElem);

          if(relations != null){
            Element relElem = new Element("relations", NAMESPACE_URI);
            addRelations(relations, relElem, NAMESPACE_URI, properties.getProperty("printable.relation.beam"));
            mrElem.appendChild(relElem);
          }

          sentElem.appendChild(mrElem);
        }

        // add the sentence to the root
        sentencesElem.appendChild(sentElem);
      }
    }

    //
    // add the coref graph
    //
    Map<Integer, CorefChain> corefChains =
      annotation.get(CorefCoreAnnotations.CorefChainAnnotation.class);
    if (corefChains != null) {
      Element corefInfo = new Element("coreference", NAMESPACE_URI);
      if (addCorefGraphInfo(corefInfo, corefChains, NAMESPACE_URI))
        docElem.appendChild(corefInfo);
    }

    //
    // save any document-level annotations here
    //

    return xmlDoc;
  }

  /**
   * Generates the XML content for a constituent tree
   */
  private void addConstituentTreeInfo(Element treeInfo, Tree tree) {
    StringWriter treeStrWriter = new StringWriter();
    constituentTreePrinter.printTree(tree, new PrintWriter(treeStrWriter, true));
    String temp = treeStrWriter.toString();
    //System.err.println(temp);
    treeInfo.appendChild(temp);
  }

  private static void addDependencyTreeInfo(Element depInfo, SemanticGraph graph, List<CoreLabel> tokens, String curNS) {
    if(graph != null) {
      for (SemanticGraphEdge edge : graph.edgeListSorted()) {
        String rel = edge.getRelation().toString();
        rel = rel.replaceAll("\\s+", "");
        int source = edge.getSource().index();
        int target = edge.getTarget().index();

        Element depElem = new Element("dep", curNS);
        depElem.addAttribute(new Attribute("type", rel));

        Element govElem = new Element("governor", curNS);
        govElem.addAttribute(new Attribute("idx", Integer.toString(source)));
        govElem.appendChild(tokens.get(source - 1).word());
        depElem.appendChild(govElem);

        Element dependElem = new Element("dependent", curNS);
        dependElem.addAttribute(new Attribute("idx", Integer.toString(target)));
        dependElem.appendChild(tokens.get(target -1).word());
        depElem.appendChild(dependElem);

        depInfo.appendChild(depElem);
      }
    }
  }

  /**
   * Generates the XML content for a dependency tree
   */
  @SuppressWarnings("unused")
  private void addDependencyTreeInfo(Element depInfo, Tree tree, String curNS) {
    if (tree != null){
      GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
      Collection<TypedDependency> deps = gs.typedDependencies();
      for(TypedDependency dep: deps){
        Element depElem = new Element("dep", curNS);
        depElem.addAttribute(new Attribute("type", dep.reln().getShortName()));

        Element govElem = new Element("governor", curNS);
        govElem.addAttribute(new Attribute("idx", Integer.toString(dep.gov().index())));
        govElem.appendChild(dep.gov().value());
        depElem.appendChild(govElem);

        Element dependElem = new Element("dependent", curNS);
        dependElem.addAttribute(new Attribute("idx", Integer.toString(dep.dep().index())));
        dependElem.appendChild(dep.dep().value());
        depElem.appendChild(dependElem);

        depInfo.appendChild(depElem);
      }
    }
  }

  /**
   * Generates the XML content for MachineReading entities
   */
  private static void addEntities(List<EntityMention> entities, Element top, String curNS) {
    for(EntityMention e: entities){
      Element ee = e.toXML(curNS);
      top.appendChild(ee);
    }
  }

  /**
   * Generates the XML content for MachineReading relations
   */
  private static void addRelations(List<RelationMention> relations, Element top, String curNS, String beamAsString){
    double beam = 0;
    if(beamAsString != null) beam = Double.parseDouble(beamAsString);
    for(RelationMention r: relations){
      if(r.printableObject(beam)) {
        Element re = r.toXML(curNS);
        top.appendChild(re);
      }
    }
  }

  /**
   * Generates the XML content for the coreference chain object
   */
  private static boolean addCorefGraphInfo
    (Element corefInfo, Map<Integer, CorefChain> corefChains, String curNS)
  {
    boolean foundCoref = false;
    for (CorefChain chain : corefChains.values()) {
      if (chain.getCorefMentions().size() <= 1)
        continue;
      foundCoref = true;
      Element chainElem = new Element("coreference", curNS);
      CorefChain.CorefMention source = chain.getRepresentativeMention();
      addCorefMention(chainElem, curNS, source, true);
      for (CorefChain.CorefMention mention : chain.getCorefMentions()) {
        if (mention == source)
          continue;
        addCorefMention(chainElem, curNS, mention, false);
      }
      corefInfo.appendChild(chainElem);
    }
    return foundCoref;
  }

  private static void addCorefMention(Element chainElem, String curNS,
                                      CorefChain.CorefMention mention,
                                      boolean representative) {
    Element mentionElem = new Element("mention", curNS);
    if (representative) {
      mentionElem.addAttribute(new Attribute("representative", "true"));
    }

    setSingleElement(mentionElem, "sentence", curNS,
                     Integer.toString(mention.sentNum));
    setSingleElement(mentionElem, "start", curNS,
                     Integer.toString(mention.startIndex));
    setSingleElement(mentionElem, "end", curNS,
                     Integer.toString(mention.endIndex));
    setSingleElement(mentionElem, "head", curNS,
                     Integer.toString(mention.headIndex));

    chainElem.appendChild(mentionElem);
  }

  private static void addWordInfo(Element wordInfo, CoreMap token, int id, String curNS) {
    // store the position of this word in the sentence
    wordInfo.addAttribute(new Attribute("id", Integer.toString(id)));

    setSingleElement(wordInfo, "word", curNS, token.get(TextAnnotation.class));
    setSingleElement(wordInfo, "lemma", curNS, token.get(LemmaAnnotation.class));

    if (token.containsKey(CharacterOffsetBeginAnnotation.class) && token.containsKey(CharacterOffsetEndAnnotation.class)) {
      setSingleElement(wordInfo, "CharacterOffsetBegin", curNS, Integer.toString(token.get(CharacterOffsetBeginAnnotation.class)));
      setSingleElement(wordInfo, "CharacterOffsetEnd", curNS, Integer.toString(token.get(CharacterOffsetEndAnnotation.class)));
    }

    if (token.containsKey(PartOfSpeechAnnotation.class)) {
      setSingleElement(wordInfo, "POS", curNS, token.get(PartOfSpeechAnnotation.class));
    }

    if (token.containsKey(NamedEntityTagAnnotation.class)) {
      setSingleElement(wordInfo, "NER", curNS, token.get(NamedEntityTagAnnotation.class));
    }

    if (token.containsKey(NormalizedNamedEntityTagAnnotation.class)) {
      setSingleElement(wordInfo, "NormalizedNER", curNS, token.get(NormalizedNamedEntityTagAnnotation.class));
    }

    if (token.containsKey(TimexAnnotation.class)) {
      Timex timex = token.get(TimexAnnotation.class);
      Element timexElem = new Element("Timex", curNS);
      timexElem.addAttribute(new Attribute("tid", timex.tid()));
      timexElem.addAttribute(new Attribute("type", timex.timexType()));
      timexElem.appendChild(timex.value());
      wordInfo.appendChild(timexElem);
    }

    if (token.containsKey(CoreAnnotations.TrueCaseAnnotation.class)) {
      Element cur = new Element("TrueCase", curNS);
      cur.appendChild(token.get(TrueCaseAnnotation.class));
      wordInfo.appendChild(cur);
    }
    if (token.containsKey(CoreAnnotations.TrueCaseTextAnnotation.class)) {
      Element cur = new Element("TrueCaseText", curNS);
      cur.appendChild(token.get(TrueCaseTextAnnotation.class));
      wordInfo.appendChild(cur);
    }

//    IntTuple corefDest;
//    if((corefDest = label.get(CorefDestAnnotation.class)) != null){
//      Element cur = new Element("coref", curNS);
//      String value = Integer.toString(corefDest.get(0)) + "." + Integer.toString(corefDest.get(1));
//      cur.setText(value);
//      wordInfo.addContent(cur);
//    }
  }

  /**
   * Helper method for addWordInfo().  If the value is not null,
   * creates an element of the given name and namespace and adds it to the
   * tokenElement.
   *
   * @param tokenElement This is the element to which the newly created element will be added
   * @param elemName This is the name for the new XML element
   * @param curNS    The current namespace
   * @param value    This is its value
   */
  private static void setSingleElement(Element tokenElement, String elemName, String curNS, String value) {
    Element cur = new Element(elemName, curNS);
    if (value != null) {
      cur.appendChild(value);
      tokenElement.appendChild(cur);
    }
  }

  //
  // runtime, shell-specific, and help menu methods
  //

  /**
   * Prints the list of properties required to run the pipeline
   * @param os PrintStream to print usage to
   * @param helpTopic a topic to print help about (or null for general options)
   */
  private static void printHelp(PrintStream os, String helpTopic) {
    if (helpTopic.toLowerCase().startsWith("pars")) {
      os.println("StanfordCoreNLP currently supports the following parsers:");
      os.println("\tstanford - Stanford lexicalized parser (default)");
      os.println("\tcharniak - Charniak and Johnson reranking parser (sold separately)");
      os.println();
      os.println("General options: (all parsers)");
      os.println("\tparser.type - selects the parser to use");
      os.println("\tparser.model - path to model file for parser");
      os.println("\tparser.maxlen - maximum sentence length");
      os.println();
      os.println("Stanford Parser-specific options:");
      os.println("(In general, you shouldn't need to set this flags)");
      os.println("\tparser.flags - extra flags to the parser (default: -retainTmpSubcategories)");
      os.println("\tparser.debug - set to true to make the parser slightly more verbose");
      os.println();
      os.println("Charniak and Johnson parser-specific options:");
      os.println("\tparser.executable - path to the parseIt binary or parse.sh script");
    } else {
      // argsToProperties will set the value of a -h or -help to "true" if no arguments are given
      if ( ! helpTopic.equalsIgnoreCase("true")) {
        os.println("Unknown help topic: " + helpTopic);
        os.println("See -help for a list of all help topics.");
      } else {
        printRequiredProperties(os);
      }
    }
  }

  /**
   * Prints the list of properties required to run the pipeline
   * @param os PrintStream to print usage to
   */
  private static void printRequiredProperties(PrintStream os) {
    // TODO some annotators (ssplit, regexner, gender, some parser
    // options, dcoref?) are not documented
    os.println("The following properties can be defined:");
    os.println("(if -props or -annotators is not passed in, default properties will be loaded via the classpath)");
    os.println("\t\"props\" - path to file with configuration properties");
    os.println("\t\"annotators\" - comma separated list of annotators");
    os.println("\tThe following annotators are supported: cleanxml, tokenize, ssplit, pos, lemma, ner, truecase, parse, coref, dcoref, nfl");

    os.println("\n\tIf annotator \"tokenize\" is defined:");
    os.println("\t\"tokenize.options\" - PTBTokenizer options (see edu.stanford.nlp.process.PTBTokenizer for details)");
    os.println("\t\"tokenize.whitespace\" - If true, just use whitespace tokenization");

    os.println("\n\tIf annotator \"cleanxml\" is defined:");
    os.println("\t\"clean.xmltags\" - regex of tags to extract text from");
    os.println("\t\"clean.sentenceendingtags\" - regex of tags which mark sentence endings");
    os.println("\t\"clean.allowflawedxml\" - if set to false, don't complain about XML errors");

    os.println("\n\tIf annotator \"pos\" is defined:");
    os.println("\t\"pos.model\" - path towards the POS tagger model");

    os.println("\n\tIf annotator \"ner\" is defined:");
    os.println("\t\"ner.model.3class\" - path towards the three-class NER model");
    os.println("\t\"ner.model.7class\" - path towards the seven-class NER model");
    os.println("\t\"ner.model.MISCclass\" - path towards the NER model with a MISC class");

    os.println("\n\tIf annotator \"truecase\" is defined:");
    os.println("\t\"truecase.model\" - path towards the true-casing model; default: " + DefaultPaths.DEFAULT_TRUECASE_MODEL);
    os.println("\t\"truecase.bias\" - class bias of the true case model; default: " + TrueCaseAnnotator.DEFAULT_MODEL_BIAS);
    os.println("\t\"truecase.mixedcasefile\" - path towards the mixed case file; default: " + DefaultPaths.DEFAULT_TRUECASE_DISAMBIGUATION_LIST);

    os.println("\n\tIf annotator \"nfl\" is defined:");
    os.println("\t\"nfl.gazetteer\" - path towards the gazetteer for the NFL domain");
    os.println("\t\"nfl.relation.model\" - path towards the NFL relation extraction model");

    os.println("\n\tIf annotator \"parse\" is defined:");
    os.println("\t\"parser.model\" - path towards the PCFG parser model");

    /* XXX: unstable, do not use for now
    os.println("\n\tIf annotator \"srl\" is defined:");
    os.println("\t\"srl.verb.args\" - path to the file listing verbs and their core arguments (\"verbs.core_args\")");
    os.println("\t\"srl.model.id\" - path prefix for the role identification model (adds \".model.gz\" and \".fe\" to this prefix)");
    os.println("\t\"srl.model.cls\" - path prefix for the role classification model (adds \".model.gz\" and \".fe\" to this prefix)");
    os.println("\t\"srl.model.jic\" - path to the directory containing the joint model's \"model.gz\", \"fe\" and \"je\" files");
    os.println("\t                  (if not specified, the joint model will not be used)");
    */

    os.println("\nCommand line properties:");
    os.println("\t\"file\" - run the pipeline on the content of this file, or on the content of the files in this directory");
    os.println("\t         XML output is generated for every input file \"file\" as file.xml");
    os.println("\t\"extension\" - if -file used with a directory, process only the files with this extension");
    os.println("\t\"filelist\" - run the pipeline on the list of files given in this file");
    os.println("\t             output is generated for every input file as file.outputExtension");
    os.println("\t\"outputDirectory\" - where to put output (defaults to the current directory)");
    os.println("\t\"outputExtension\" - extension to use for the output file (defaults to \".xml\" for XML, \".ser.gz\" for serialized).  Don't forget the dot!");
    os.println("\t\"outputFormat\" - \"xml\" to output XML (default), \"serialized\" to output serialized Java objects");
    os.println("\t\"replaceExtension\" - flag to chop off the last extension before adding outputExtension to file");
    os.println("\t\"noClobber\" - don't automatically override (clobber) output files that already exist");
    os.println("\nIf none of the above are present, run the pipeline in an interactive shell (default properties will be loaded from the classpath).");
    os.println("The shell accepts input from stdin and displays the output at stdout.");

    os.println("\nRun with -help [topic] for more help on a specific topic.");
    os.println("Current topics include: parser");

    os.println();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String timingInformation() {
    StringBuilder sb = new StringBuilder(super.timingInformation());
    if (TIME && numWords >= 0) {
      long total = this.getTotalTime();
      sb.append(" for ").append(this.numWords).append(" tokens at ");
      sb.append(String.format("%.1f", numWords / (((double) total)/1000)));
      sb.append( " tokens/sec.");
    }
    return sb.toString();
  }

  /**
   * Runs an interactive shell where input text is processed with the given pipeline
   * @param pipeline The pipeline to be used
   * @throws IOException If IO problem with stdin
   */
  private static void shell(StanfordCoreNLP pipeline) throws IOException {
    BufferedReader is = new BufferedReader(new InputStreamReader(System.in));
    PrintWriter os = new PrintWriter(System.out);
    System.out.println("Entering interactive shell. Type q to quit.");
    while(true){
      System.out.print("NLP> ");
      String line = is.readLine();
      if(line != null && line.length() > 0){
        if(line.equalsIgnoreCase("q")) break;
        Annotation anno = pipeline.process(line);
        pipeline.prettyPrint(anno, os);
      }
    }
  }

  private static Collection<File> readFileList(String fileName) throws IOException {
    return ObjectBank.getLineIterator(fileName, new ObjectBank.PathToFileFunction());
  }

  void processFiles(Collection<File> files, Properties props)
    throws IOException
  {
    for (File file : files) {
      String outputFilename = new File(props.getProperty("outputDirectory", "."), file.getName()).getPath();
      if (props.getProperty("replaceExtension") != null) {
        int lastDot = outputFilename.lastIndexOf('.');
        // for paths like "./zzz", lastDot will be 0
        if (lastDot > 0) {
          outputFilename = outputFilename.substring(0, lastDot);
        }
      }
      String outputFormat = props.getProperty("outputFormat", "xml");
      String defaultExtension = outputFormat.equalsIgnoreCase("xml") ? ".xml" :  outputFormat.equalsIgnoreCase("text") ? ".out" : ".ser.gz";
      String extension = props.getProperty("outputExtension", defaultExtension);
      // ensure we don't make filenames with doubled extensions like .xml.xml
      if (!outputFilename.endsWith(extension)) {
        outputFilename += extension;
      }
      // normalize filename for the upcoming comparison
      outputFilename = new File(outputFilename).getCanonicalPath();

      // TODO this could fail if there are softlinks, etc. -- need some sort of sameFile tester
      if (outputFilename.equals(file.getCanonicalPath())) {
        System.out.println("Skipping " + file.getName() + ": output file " + outputFilename + " has the same filename as the input file -- assuming you don't actually want to do this.");
        continue;
      }
      if (props.getProperty("noClobber") != null && new File(outputFilename).exists()) {
        System.out.println("Skipping " + file.getName() + ": output file " + outputFilename + " as it already exists.  Don't use the noClobber option to override this.");
        continue;
      }

      String encoding = props.getProperty("encoding");
      System.err.println("Processing file " + file.getAbsolutePath() + " ... (writing to " + outputFilename + ")");

      String text = IOUtils.slurpFile(file, encoding);

      Annotation annotation = process(text);

      if (outputFormat.equalsIgnoreCase("xml")) {
        Writer writer = EncodingFileWriter.getWriter(outputFilename, encoding);
        xmlPrint(annotation, writer);
        writer.close();
      } else if (outputFormat.equalsIgnoreCase("text")) {
        PrintWriter pw = new PrintWriter(EncodingFileWriter.getWriter(outputFilename, encoding));
        prettyPrint(annotation, pw);
        pw.close();
      } else {
        IOUtils.writeObjectToFile(annotation, outputFilename);
      }
    }
  }

  /**
   * This can be used just for testing or for command-line text processing.
   * This runs the pipeline you specify on the
   * text in the file that you specify and sends some results to stdout.
   * The current code in this main method assumes that each line of the file
   * is to be processed separately as a single sentence.
   * <p>
   * Example usage:<br>
   * java -mx6g edu.stanford.nlp.pipeline.StanfordCoreNLP properties
   *
   * @param args List of required properties
   * @throws java.io.IOException If IO problem
   * @throws ClassNotFoundException If class loading problem
   */
  public static void main(String[] args) throws IOException, ClassNotFoundException {
    Timing tim = new Timing();

    //
    // process the arguments
    //
    // extract all the properties from the command line
    // if cmd line is empty, set the props to null. The processor will search for the properties file in the classpath
    Properties props = null;
    if(args.length > 0){
      props = StringUtils.argsToProperties(args);
      boolean hasH = props.containsKey("h");
      boolean hasHelp = props.containsKey("help");
      if (hasH || hasHelp) {
        String helpValue = hasH ? props.getProperty("h") : props.getProperty("help");
        printHelp(System.err, helpValue);
        return;
      }
    }

    //
    // construct the pipeline
    //
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    props = pipeline.getProperties();
    long setupTime = tim.report();

    // blank line after all the loading statements to make output more readable
    System.err.println();

    //
    // Process one file or a directory of files
    //
    if(props.containsKey("file")){
      String fileName = props.getProperty("file");
      Collection<File> files = new FileSequentialCollection(new File(fileName), props.getProperty("extension"), true);
      pipeline.processFiles(files, props);
    }

    //
    // Process a list of files
    //
    else if(props.containsKey("filelist")){
      String fileName = props.getProperty("filelist");
      Collection<File> files = readFileList(fileName);
      pipeline.processFiles(files, props);
    }

    //
    // Run the interactive shell
    //
    else {
      shell(pipeline);
    }

    if (TIME) {
      System.err.println();
      System.err.println(pipeline.timingInformation());
      System.err.println("Pipeline setup: " +
          Timing.toSecondsString(setupTime) + " sec.");
      System.err.println("Total time for StanfordCoreNLP pipeline: " +
          tim.toSecondsString() + " sec.");
    }
  }
}

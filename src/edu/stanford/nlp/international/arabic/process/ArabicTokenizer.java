package edu.stanford.nlp.international.arabic.process;

import java.io.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.process.AbstractTokenizer;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.util.StringUtils;

/**
 * Tokenizer for UTF-8 Arabic. Buckwalter encoding is *not* supported.
 * <p>
 * TODO(spenceg): Merge in rules from ibm tokenizer (v5).
 * TODO(spenceg): Add XML escaping
 * TODO(spenceg): When running from the command line, the tokenizer does not
 *   produce the correct number of newline-delimited lines for the ATB data
 *   sets.
 * 
 * @author Spence Green
 */
public class ArabicTokenizer<T extends HasWord> extends AbstractTokenizer<T> {

  // The underlying JFlex lexer
  private final ArabicLexer lexer;

  // Produces the normalization for parsing used in Green and Manning (2010)
  private static final String atbOptions = "normArDigits=true,normArPunc=true,normAlif=true,removeDiacritics=true,removeTatweel=true,removeQuranChars=true";

  public static ArabicTokenizer<CoreLabel> newArabicTokenizer(Reader r, Properties lexerProperties) {
    return new ArabicTokenizer<CoreLabel>(r, new CoreLabelTokenFactory(), lexerProperties);
  }

  public ArabicTokenizer(Reader r, LexedTokenFactory<T> tf, Properties lexerProperties) {
    lexer = new ArabicLexer(r, tf, lexerProperties);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected T getNext() {
    try {
      T nextToken = null;
      // Depending on the orthographic normalization options,
      // some tokens can be obliterated. In this case, keep iterating
      // until we see a non-zero length token.
      do {
        nextToken = (T) lexer.next();
      } while (nextToken != null && nextToken.word().length() == 0);

      return nextToken;

    } catch (IOException e) {
      throw new RuntimeIOException(e);
    }
  }

  public static class ArabicTokenizerFactory<T extends HasWord> implements TokenizerFactory<T>  {

    protected final LexedTokenFactory<T> factory;
    protected Properties lexerProperties = null;

    /**
     * Constructs a new TokenizerFactory that returns HasWord objects and
     * treats carriage returns as normal whitespace.
     * THIS METHOD IS INVOKED BY REFLECTION BY SOME OF THE JAVANLP
     * CODE TO LOAD A TOKENIZER FACTORY.  IT SHOULD BE PRESENT IN A
     * TokenizerFactory.
     *
     * @return A TokenizerFactory that returns HasWord objects
     */
    public static TokenizerFactory<CoreLabel> newTokenizerFactory() {
      return new ArabicTokenizerFactory<CoreLabel>(new CoreLabelTokenFactory());
    }

    private ArabicTokenizerFactory(LexedTokenFactory<T> factory) {
      this.factory = factory;
    }

    public Iterator<T> getIterator(Reader r) {
      return getTokenizer(r);
    }

    public Tokenizer<T> getTokenizer(Reader r) {
      return new ArabicTokenizer<T>(r, factory, lexerProperties);
    }

    @SuppressWarnings("unchecked")
    public void setOptions(String options) {
      if (lexerProperties == null) {
        lexerProperties = StringUtils.stringToProperties(options);
      } else {
        Properties newProps = StringUtils.stringToProperties(options);
        for (Enumeration<String> e = (Enumeration<String>) newProps.propertyNames();
        e.hasMoreElements(); ) {
          String key = e.nextElement();
          String value = newProps.getProperty(key);
          lexerProperties.put(key, value);
        }
      }
    }

    public Tokenizer<T> getTokenizer(Reader r, String extraOptions) {
      setOptions(extraOptions);
      return getTokenizer(r);
    }
  }

  public static TokenizerFactory<CoreLabel> factory() {
    return ArabicTokenizerFactory.newTokenizerFactory();
  }

  public static TokenizerFactory<CoreLabel> atbFactory() {
    TokenizerFactory<CoreLabel> tf = ArabicTokenizerFactory.newTokenizerFactory();
    tf.setOptions(atbOptions);
    return tf;
  }

  /**
   * 
   * @param args
   */
  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.printf("Usage: java %s [-atb|tokenizer_opts] < lines%n", ArabicTokenizer.class.getName());
      System.exit(-1);
    }
    String encoding = "UTF-8";
    String tokenizerOptions = args[0];
    try {
      TokenizerFactory<CoreLabel> tf;
      if (args[0].equals("-atb")) {
        tf = ArabicTokenizer.atbFactory();
      } else {
        tf = ArabicTokenizer.factory();
        tf.setOptions(tokenizerOptions);        
      }
      
      String nl = System.getProperty("line.separator");
      Tokenizer<CoreLabel> tokenizer = tf.getTokenizer(new InputStreamReader(System.in, encoding));
      while (tokenizer.hasNext()) {
        String word = tokenizer.next().word();
        System.out.print(word);
        if ( ! word.equals(nl)) {
          System.out.print(" ");
        }
      }

    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }
}

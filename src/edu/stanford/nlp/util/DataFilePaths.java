package edu.stanford.nlp.util;

/**
 * Simple utility class: reads the environment variable in
 * ENV_VARIABLE and provides a method that converts strings which
 * start with that environment variable to file paths.  For example,
 * you can send it
 * "$NLP_DATA_HOME/data/pos-tagger/wsj3t0-18-left3words"
 * and it will convert that to
 * "/u/nlp/data/pos-tagger/wsj3t0-18-left3words"
 * unless you have set $NLP_DATA_HOME to something else.
 * <br>
 * The only environment variable expanded is that defined by
 * ENV_VARIABLE, and the only place in the string it is expanded is at
 * the start of the string.
 *
 * @author John Bauer
 */

public class DataFilePaths {
  private DataFilePaths() {}

  static final String ENV_VARIABLE = "NLP_DATA_HOME";
  static final String ENV_VARIABLE_PREFIX = "$" + ENV_VARIABLE;

  static final String NLP_DATA_HOME = 
    ((System.getenv(ENV_VARIABLE) != null) ?
     System.getenv(ENV_VARIABLE) : "/u/nlp");

  static public String convert(String path) {
    if (path.startsWith(ENV_VARIABLE_PREFIX))
      return NLP_DATA_HOME + path.substring(ENV_VARIABLE_PREFIX.length());
    return path;
  }
}
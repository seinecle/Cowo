package edu.stanford.nlp.time;

import java.util.Properties;
import edu.stanford.nlp.util.PropertiesUtils;

/**
 * Various options for using time expression extractor
 *
 * @author Angel Chang
 */
public class Options {
  public enum RelativeHeuristicLevel { NONE, BASIC, MORE };

  // Whether to mark time ranges like from 1991 to 1992 as one timex
  // or leave it separate
  boolean markTimeRanges = false;
  // Heuristics for determining relative time
  // level 1 = no heuristics (default)
  // level 2 = basic heuristics taking into past tense
  // level 3 = more heuristics with since/until
  RelativeHeuristicLevel teRelHeurLevel = RelativeHeuristicLevel.NONE;
  // Include nested time expressions
  boolean includeNested = false;
  // Convert times to ranges
  boolean includeRange = false;
  // TODO: Add default country for holidays and default time format
  // would want a per document default as well


  boolean verbose = false;

  public Options()
  {
  }

  public Options(String name, Properties props)
  {
    includeRange = PropertiesUtils.getBool(props, name + ".includeRange",
                                           includeRange);
    markTimeRanges = PropertiesUtils.getBool(props, name + ".markTimeRanges",
                                             markTimeRanges);
    includeNested = PropertiesUtils.getBool(props, name + ".includeNested",
                                            includeNested);
    teRelHeurLevel = RelativeHeuristicLevel.valueOf(
                       props.getProperty(name + ".teRelHeurLevel",
                                         teRelHeurLevel.toString()));
    verbose = PropertiesUtils.getBool(props, name + ".verbose", verbose);
  }
}

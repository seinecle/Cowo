package edu.stanford.nlp.time;

import edu.stanford.nlp.ie.NumberNormalizer;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.tokensregex.*;
import edu.stanford.nlp.pipeline.ChunkAnnotationUtils;
import edu.stanford.nlp.time.TimeAnnotations.TimexAnnotation;
import edu.stanford.nlp.util.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts time expressions
 *
 * @author Angel Chang
 */
@SuppressWarnings("unchecked")
public class TimeExpressionExtractor {
  private static final Logger logger = Logger.getLogger(TimeExpressionExtractor.class.getName());

  // Patterns for extracting time expressions
  TimeExpressionPatterns timexPatterns;

  // Rule to extract time expressions directly from tokens
  ExtractRule<CoreMap, TimeExpression> extractRule;
  // Rule to extract composite time expressions (use when some tokens have already been grouped into time expressions)
  ExtractRule<List<? extends CoreMap>, TimeExpression> compositeExtractRule;

  // Index of temporal object to ids
  //SUTime.TimeIndex timeIndex = new SUTime.TimeIndex();

  Options options;

  // Options

  public TimeExpressionExtractor()
  {
    init(new Options());
  }

  public TimeExpressionExtractor(String name, Properties props)
  {
    init(name, props);
  }

  public void init(String name, Properties props)
  {
    init(new Options(name, props));
  }

  public void init(Options options)
  {
    this.options = options;
    timexPatterns = new TimeExpressionPatterns(options);
    extractRule = timexPatterns.getTimeExtractionRule();
    compositeExtractRule = timexPatterns.getCompositeTimeExtractionRule();
    // TODO: does not allow for multiple loggers
    if (options.verbose) {
      logger.setLevel(Level.FINE);
    } else {
      logger.setLevel(Level.SEVERE);
    } 
    NumberNormalizer.setVerbose(options.verbose);
  }

  public List<CoreMap> extractTimeExpressionCoreMaps(CoreMap annotation, String docDate, SUTime.TimeIndex timeIndex) 
  {
    List<TimeExpression> timeExpressions = extractTimeExpressions(annotation, docDate);
    return toCoreMaps(annotation, timeExpressions, timeIndex);
  }

  private List<CoreMap> toCoreMaps(CoreMap annotation, List<TimeExpression> timeExpressions, SUTime.TimeIndex timeIndex)
  {
    if (timeExpressions == null) return null;
    List<CoreMap> coreMaps = new ArrayList<CoreMap>(timeExpressions.size());
    for (TimeExpression te:timeExpressions) {
      CoreMap cm = te.getAnnotation();
      SUTime.Temporal temporal = te.getTemporal();
      if (temporal != null) {
        String origText = annotation.get(CoreAnnotations.TextAnnotation.class);
        String text = cm.get(CoreAnnotations.TextAnnotation.class);
        if (origText != null) {
          // Make sure the text is from original (and not from concatenated tokens)
          ChunkAnnotationUtils.annotateChunkText(cm, annotation);
          text = cm.get(CoreAnnotations.TextAnnotation.class);
      /*    Integer offset = annotation.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
          if (offset == null) { offset = 0; }
          text = origText.substring(cm.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class) - offset,
                  cm.get(CoreAnnotations.CharacterOffsetEndAnnotation.class) - offset);
          cm.set(CoreAnnotations.TextAnnotation.class, text);       */
        }
        Map<String,String> timexAttributes = temporal.getTimexAttributes(timeIndex);
        if (options.includeRange) {
          SUTime.Temporal rangeTemporal = temporal.getRange();
          if (rangeTemporal != null) {
            timexAttributes.put("range", rangeTemporal.toString());
          }
        }
        Timex timex;
        try {
          timex = Timex.fromMap(text, timexAttributes);
        } catch (Exception e) {
          logger.log(Level.WARNING, "Failed to process " + text + " with attributes " + timexAttributes, e);
          continue;
        }
        cm.set(TimexAnnotation.class, timex);
        if (timex != null) {
          coreMaps.add(cm);
        } else {
          logger.warning("No timex expression for: " + text);
        }
      }
    }
    return coreMaps;
  }

  public List<TimeExpression> extractTimeExpressions(CoreMap annotation, String docDateStr)
  {
    List<CoreMap> mergedNumbers = NumberNormalizer.findAndMergeNumbers(annotation);
    annotation.set(CoreAnnotations.NumerizedTokensAnnotation.class, mergedNumbers);

    // TODO: docDate may not have century....
    SUTime.Time docDate = DateTimeUtils.parseDateTime(docDateStr);
    // Extract potential time expressions
    List<TimeExpression> timeExpressions = new ArrayList<TimeExpression>();
    extractRule.extract(annotation, timeExpressions);

    annotateTimeExpressions(annotation, timeExpressions);
    timeExpressions = removeNested(timeExpressions);

    // TODO: Look around to see if found time expressions need to be fixed
    // TODO: Look at surrounding words to see if time expressions need to be expanded
    if (compositeExtractRule != null) {
      List<? extends CoreMap> merged = replaceMerged(annotation.get(CoreAnnotations.NumerizedTokensAnnotation.class), timeExpressions);
//      List<? extends CoreMap> merged = replaceMerged(annotation.get(CoreAnnotations.TokensAnnotation.class), timeExpressions);
      // Apply higher order rules
      boolean done = false;
      while (!done) {
        List<TimeExpression> newExprs = new ArrayList<TimeExpression>();
        boolean extracted = compositeExtractRule.extract(merged, newExprs);
        if (extracted) {
          annotateTimeExpressions(merged, newExprs);
          newExprs = removeNested(newExprs);
          merged = replaceMerged(merged, newExprs);
          timeExpressions.addAll(newExprs);
          timeExpressions = removeNested(timeExpressions);
        }
        done = !extracted;
      }
    }
    // Add back nested time expressions for ranges....
    // For now only one level of nesting...
    if (options.includeNested) {
      List<TimeExpression> nestedTimeExpressions = new ArrayList<TimeExpression>();
      for (TimeExpression te:timeExpressions) {
        if (te.includeNested)  {
          List<? extends CoreMap> children = te.annotation.get(TimeExpression.ChildrenAnnotation.class);
          if (children != null) {
            for (CoreMap child:children) {
              TimeExpression childTe = child.get(TimeExpression.Annotation.class);
              if (childTe != null) {
                nestedTimeExpressions.add(childTe);
              }
            }
          }
        }
      }
      timeExpressions.addAll(nestedTimeExpressions);
    }

//    Collections.sort(timeExpressions, TIME_EXPR_TOKEN_OFFSET_COMPARATOR);
    Collections.sort(timeExpressions, TIME_EXPR_TOKEN_OFFSETS_NESTED_FIRST_COMPARATOR);
    timeExpressions = filterInvalidTimeExpressions(timeExpressions);

    // Some resolving is done even if docDate null...
    if ( /*docDate != null && */ timeExpressions != null) {
      resolveTimeExpressions(annotation, timeExpressions, docDate);
    }
    // Annotate timex
    return timeExpressions;
  }

  private List<TimeExpression> filterInvalidTimeExpressions(List<TimeExpression> timeExprs)
  {
    int nfiltered = 0;
    List<TimeExpression> filtered = new ArrayList<TimeExpression>(timeExprs.size());   // Approximate size
    for (TimeExpression timeExpr:timeExprs) {
      if (timexPatterns.checkTimeExpression(timeExpr)) {
        filtered.add(timeExpr);
      } else {
        nfiltered++;
      }
    }
    if (nfiltered > 0) {
      logger.finest("Filtered " + nfiltered);
    }
    return filtered;
  }

  private List<? extends CoreMap> replaceMerged(List<? extends CoreMap> list, List<TimeExpression> timeExprs)
  {
    Collections.sort(timeExprs, TIME_EXPR_TOKEN_OFFSET_COMPARATOR);
    List<CoreMap> merged = new ArrayList<CoreMap>(list.size());   // Approximate size
    int last = 0;
    for (TimeExpression timeExpr:timeExprs) {
      int start = timeExpr.chunkOffsets.first();
      int end = timeExpr.chunkOffsets.second();
      if (start >= last) {
        merged.addAll(list.subList(last,start));
        CoreMap m = timeExpr.getAnnotation();
        merged.add(m);
        last = end;
      }
    }
    // Add rest of elements
    if (last < list.size()) {
      merged.addAll(list.subList(last, list.size()));
    }
    return merged;
  }

  private void resolveTimeExpressions(CoreMap annotation, List<TimeExpression> timeExpressions, SUTime.Time docDate)
  {
    for (TimeExpression te:timeExpressions) {
      SUTime.Temporal temporal = te.getTemporal();
      if (temporal != null) {
        // TODO: use correct time for anchor
        try {
          int flags = timexPatterns.determineRelFlags(annotation, te);
          //int flags = 0;
          SUTime.Temporal grounded = temporal.resolve(docDate, flags);
          if (grounded == null) {
            logger.warning("Error resolving " + temporal + ", using docDate=" + docDate);
          }
          if (grounded != temporal) {
            te.origTemporal = temporal;
            te.temporal = grounded;
          }
        } catch (Exception ex) {
          logger.log(Level.WARNING, "Error resolving " + temporal, ex);
        }
      }
    }
  }

  private static List<TimeExpression> removeNested(List<TimeExpression> chunks)
  {
    if (chunks.size() > 1) {
      // TODO: presort chunks by priority, length, given order
      for (int i = 0; i < chunks.size(); i++) {
        chunks.get(i).order = i;
      }
      return IntervalTree.getNonOverlapping(chunks, TIME_EXPR_TO_TOKEN_OFFSETS_INTERVAL_FUNC, TIME_EXPR_PRIORITY_LENGTH_COMPARATOR /* TIME_EXPR_LENGTH_COMPARATOR  */);
    } else {
      return chunks;
    }
  }

  @SuppressWarnings("unused")
  private static Function<CoreMap, Interval<Integer>> COREMAP_TO_TOKEN_OFFSETS_INTERVAL_FUNC =
    new Function<CoreMap, Interval<Integer>>() {
      public Interval<Integer> apply(CoreMap in) {
        return Interval.toInterval(
              in.get(CoreAnnotations.TokenBeginAnnotation.class),
              in.get(CoreAnnotations.TokenEndAnnotation.class));
      }
    };

  private static Function<TimeExpression, Interval<Integer>> TIME_EXPR_TO_TOKEN_OFFSETS_INTERVAL_FUNC =
    new Function<TimeExpression, Interval<Integer>>() {
      public Interval<Integer> apply(TimeExpression in) {
        return in.tokenOffsets;
      }
    };

  private static Comparator<TimeExpression> TIME_EXPR_PRIORITY_COMPARATOR =
    new Comparator<TimeExpression>() {
    public int compare(TimeExpression e1, TimeExpression e2) {
      double s1 = e1.score;
      double s2 = e2.score;
      if (s1 == s2) {
        return 0;
      } else {
        return (s1 > s2)? -1:1;
      }
    }
  };

  private static Comparator<TimeExpression> TIME_EXPR_ORDER_COMPARATOR =
    new Comparator<TimeExpression>() {
    public int compare(TimeExpression e1, TimeExpression e2) {
      int s1 = e1.order;
      int s2 = e2.order;
      if (s1 == s2) {
        return 0;
      } else {
        return (s1 < s2)? -1:1;
      }
    }
  };

  // Compares two time expressions.
  // Use to order time expressions by:
  //    length (longest first), then whether it has temporal or not (has temporal first),
  // Returns -1 if e1 is longer than e2, 1 if e2 is longer
  // If e1 and e2 are the same length:
  //    Returns -1 if e1 has temporal, but e2 doesn't (1 if e2 has temporal, but e1 doesn't)
  //    Otherwise, both e1 and e2 has temporal or no temporal
  private static Comparator<TimeExpression> TIME_EXPR_LENGTH_COMPARATOR =
    new Comparator<TimeExpression>() {
      public int compare(TimeExpression e1, TimeExpression e2) {
        if (e1.getTemporal() == null && e2.getTemporal() != null) {
          return 1;
        }
        if (e1.getTemporal() != null && e2.getTemporal() == null) {
          return -1;
        }
        int len1 = e1.tokenOffsets.getEnd() - e1.tokenOffsets.getBegin();
        int len2 = e2.tokenOffsets.getEnd() - e2.tokenOffsets.getBegin();
        if (len1 == len2) {
          return 0;
        } else {
          return (len1 > len2)? -1:1;
        }
      }
    };

  private static Comparator<TimeExpression> TIME_EXPR_TOKEN_OFFSET_COMPARATOR =
    new Comparator<TimeExpression>() {
      public int compare(TimeExpression e1, TimeExpression e2) {
        return (e1.tokenOffsets.compareTo(e2.tokenOffsets));
      }
    };

  private static Comparator<TimeExpression> TIME_EXPR_TOKEN_OFFSETS_NESTED_FIRST_COMPARATOR =
    new Comparator<TimeExpression>() {
      public int compare(TimeExpression e1, TimeExpression e2) {
        Interval.RelType rel = e1.tokenOffsets.getRelation(e2.tokenOffsets);
        if (rel.equals(Interval.RelType.CONTAIN)) {
          return 1;
        } else if (rel.equals(Interval.RelType.INSIDE)) {
          return -1;
        } else {
          return (e1.tokenOffsets.compareTo(e2.tokenOffsets));
        }
      }
    };

  // Compares two time expressions.
  // Use to order time expressions by:
   //   score
  //    length (longest first), then whether it has temporal or not (has temporal first),
  //    original order
  //    and then begining token offset (smaller offset first)
  private static Comparator<TimeExpression> TIME_EXPR_PRIORITY_LENGTH_COMPARATOR =
          Comparators.chain(TIME_EXPR_PRIORITY_COMPARATOR, TIME_EXPR_LENGTH_COMPARATOR, TIME_EXPR_ORDER_COMPARATOR, TIME_EXPR_TOKEN_OFFSET_COMPARATOR);


  @SuppressWarnings("unused")
  private static Function<IntPair, Interval<Integer>> INTPAIR_TO_INTERVAL_FUNC =
    new Function<IntPair, Interval<Integer>>() {
      public Interval<Integer> apply(IntPair in) {
        return Interval.toInterval(in.getSource(), in.getTarget());
      }
    };

  private void annotateTimeExpressions(CoreMap annotation, List<TimeExpression> timeExpressions)
  {
    List<TimeExpression> toDiscard = new ArrayList<TimeExpression>();
    for (TimeExpression te:timeExpressions) {
      // Add attributes and tids
      try {
        @SuppressWarnings("unused")
        CoreMap chunk = te.extractAnnotation(annotation);
        SUTime.Temporal temporal = te.getTemporal();
        if (temporal != null)  {
          if (temporal != SUTime.TIME_NONE_OK) {
            te.temporal = timexPatterns.addMod(te.text, temporal);
    //      Timex timex = Timex.fromMap(chunk.get(CoreAnnotations.TextAnnotation.class), temporal.getTimexAttributes(timeIndex));
    //      chunk.set(Timex.Annotation.class, timex);
          } else {
            toDiscard.add(te);
          }
        } else {
          logger.log(Level.WARNING, "Unable to get temporal for time expression " + te);
          logger.log(Level.WARNING, "Temporal func: " + te.temporalFunc);
        }
      } catch (Exception ex) {
        logger.log(Level.WARNING, "Error extracting annotation with temporal from time expression: " + te, ex);
      }
    }
    timeExpressions.removeAll(toDiscard);
  }

  private void annotateTimeExpressions(List<? extends CoreMap> chunks, List<TimeExpression> timeExpressions)
  {
    List<TimeExpression> toDiscard = new ArrayList<TimeExpression>();
    for (TimeExpression te:timeExpressions) {
      // Add attributes and tids
      try {
        @SuppressWarnings("unused")
        CoreMap chunk = te.extractAnnotation(chunks);
        SUTime.Temporal temporal = te.getTemporal();
        if (temporal != null)  {
          if (temporal != SUTime.TIME_NONE_OK) {
           te.temporal = timexPatterns.addMod(te.text, temporal);
     //      Timex timex = Timex.fromMap(chunk.get(CoreAnnotations.TextAnnotation.class), temporal.getTimexAttributes(timeIndex));
    //      chunk.set(Timex.Annotation.class, timex);
          } else {
            toDiscard.add(te);
          }
        } else {
          logger.log(Level.WARNING, "Unable to get temporal for time expression " + te);
        }
      } catch (Exception ex) {
        logger.log(Level.WARNING, "Error extracting annotation with temporal from time expression: " + te, ex);
      }
    }
    timeExpressions.removeAll(toDiscard);
  }

  static interface TemporalExtractor extends Function<CoreMap, SUTime.Temporal>
  {
    public SUTime.Temporal apply(CoreMap in);
  }

  static interface ExtractRule<I,O> {
    public boolean extract(I in, List<O> out);
  };

  static class FilterRule<I,O> implements ExtractRule<I,O>
  {
    Filter<I> filter;
    ExtractRule<I,O> rule;
    public FilterRule(Filter<I> filter, ExtractRule<I,O> rule) {
      this.filter = filter;
      this.rule = rule;
    }

    public FilterRule(Filter<I> filter, ExtractRule<I,O>... rules) {
      this.filter = filter;
      this.rule = new ListRule<I,O>(rules);
    }

    public boolean extract(I in, List<O> out) {
      if (filter.accept(in)) {
        return rule.extract(in,out);
      } else {
        return false;
      }
    }
  }

  static class ListRule<I,O> implements ExtractRule<I,O>
  {
    List<ExtractRule<I,O>> rules;

    public ListRule(Collection<ExtractRule<I,O>> rules)
    {
      this.rules = new ArrayList<ExtractRule<I,O>>(rules);
    }

    public ListRule(ExtractRule<I,O>... rules)
    {
      this.rules = new ArrayList<ExtractRule<I,O>>(rules.length);
      for (ExtractRule<I,O> rule:rules) {
        this.rules.add(rule);
      }
    }

    public boolean extract(I in, List<O> out) {
      boolean extracted = false;
      for (ExtractRule<I,O> rule:rules) {
        if (rule.extract(in,out)) {
          extracted = true;
        }
      }
      return extracted;
    }

    public void addRules(ExtractRule<I,O>... rules)
    {
      for (ExtractRule<I,O> rule:rules) {
        this.rules.add(rule);
      }
    }

    public void addRules(Collection<ExtractRule<I,O>> rules)
    {
      this.rules.addAll(rules);
    }

  }

  static class TokenSeqPatternRule implements ExtractRule< List<? extends CoreMap>, TimeExpression>
  {
    SequencePattern<CoreMap> pattern;
    TemporalExtractor extractor;
    boolean includeNested;
    int group = 0;

    public TokenSeqPatternRule(TokenSequencePattern.Env env, String regex, TemporalExtractor extractor) {
      this.extractor = extractor;
      this.pattern = TokenSequencePattern.compile(env, regex);
    }

    public TokenSeqPatternRule(TokenSequencePattern p, TemporalExtractor extractor) {
      this.extractor = extractor;
      this.pattern = p;
    }

    public boolean extract(List<? extends CoreMap> tokens, List<TimeExpression> out) {
      boolean extracted = false;
      SequenceMatcher<CoreMap> m = pattern.getMatcher(tokens);
      while (m.find()) {
        TimeExpression te = new TimeExpression(null, Interval.toInterval(m.start(group), m.end(group), Interval.INTERVAL_OPEN_END), extractor, m.score());
        te.includeNested = includeNested;
        out.add(te);
        extracted = true;
      }
      return extracted;
    }

  }

  static class StringPatternRule implements ExtractRule<String, TimeExpression>
  {
    Pattern pattern;
    TemporalExtractor extractor;
    boolean includeNested;
    int group = 0;

    public StringPatternRule(Pattern pattern, TemporalExtractor extractor) {
      this.pattern = pattern;
      this.extractor = extractor;
    }

    public StringPatternRule(String regex, TemporalExtractor extractor) {
      this(regex, extractor, false);
    }

    public StringPatternRule(String regex, TemporalExtractor extractor, boolean addWordBoundaries) {
      this.extractor = extractor;
      if (addWordBoundaries) { regex = "\\b" + regex + "\\b"; }
      pattern = TimeExpressionPatterns.getPattern(regex);
    }

    public boolean extract(String str, List<TimeExpression> out) {
      //List<CoreLabel> str = annotation.get(CoreAnnotations.TextAnnotation.class);
      boolean extracted = false;
      Matcher m = pattern.matcher(str);
      while (m.find()) {
        TimeExpression te = new TimeExpression(Interval.toInterval(m.start(group), m.end(group), Interval.INTERVAL_OPEN_END), null, extractor, 0);
        te.includeNested = includeNested;
        out.add(te);
        extracted = true;
      }
      return extracted;
    }

  }


/**
 *  Takes in an expression assumed to be a duration expression and returns a
 *   duration value, such as P3D for "3 days"
 *  Duration Format: PnYnMnDTnHnMnS
 */

  static  class DurationRule implements TemporalExtractor  {
    TimeExpressionPatterns patterns;
    Pattern stringPattern;
    TokenSequencePattern tokenPattern;
    int exprGroup = 0;
    int valMatchGroup = -1;
    int valMatchGroup2 = -1;
    int unitMatchGroup = -1;
    int underspecifiedValMatchGroup = -1;
    String defaultUnderspecifiedValue;
    SUTime.Time beginTime;
    SUTime.Time endTime;

    private DurationRule(TimeExpressionPatterns patterns, int valMatchGroup, int unitMatchGroup, SUTime.Time beginTime, SUTime.Time endTime) {
      this.patterns = patterns;
      this.valMatchGroup = valMatchGroup;
      this.unitMatchGroup = unitMatchGroup;
      this.beginTime = beginTime;
      this.endTime = endTime;
    }

    public DurationRule(TimeExpressionPatterns patterns, Pattern p, int valMatchGroup, int unitMatchGroup) {
      this(patterns, p, valMatchGroup, unitMatchGroup, SUTime.TIME_NONE, SUTime.TIME_NONE);
    }

    public DurationRule(TimeExpressionPatterns patterns, Pattern p, int valMatchGroup, int unitMatchGroup, SUTime.Time beginTime, SUTime.Time endTime) {
      this(patterns, valMatchGroup, unitMatchGroup, beginTime, endTime);
      this.stringPattern = p;
    }

  public DurationRule(TimeExpressionPatterns patterns, Pattern p, int valMatchGroup, int valMatchGroup2, int unitMatchGroup, SUTime.Time beginTime, SUTime.Time endTime) {
    this(patterns, valMatchGroup, unitMatchGroup, beginTime, endTime);
    this.valMatchGroup2 = valMatchGroup2;
    this.stringPattern = p;
  }

    public DurationRule(TimeExpressionPatterns patterns, TokenSequencePattern p, int valMatchGroup, int unitMatchGroup) {
      this(patterns, p, valMatchGroup, unitMatchGroup, SUTime.TIME_NONE, SUTime.TIME_NONE);
    }

    public DurationRule(TimeExpressionPatterns patterns, TokenSequencePattern p, int valMatchGroup, int unitMatchGroup, SUTime.Time beginTime, SUTime.Time endTime) {
      this(patterns, valMatchGroup, unitMatchGroup, beginTime, endTime);
      this.tokenPattern = p;
    }

  public DurationRule(TimeExpressionPatterns patterns, TokenSequencePattern p, int valMatchGroup, int valMatchGroup2, int unitMatchGroup, SUTime.Time beginTime, SUTime.Time endTime) {
    this(patterns, valMatchGroup, unitMatchGroup, beginTime, endTime);
    this.valMatchGroup2 = valMatchGroup2;
    this.tokenPattern = p;
  }

    public boolean useTokens()
    {
      return (tokenPattern != null);
    }

    public void setUnderspecifiedValueMatchGroup(int matchGroup, String defaultValue) {
      underspecifiedValMatchGroup = matchGroup;
      defaultUnderspecifiedValue = defaultValue;
    }

    public SUTime.Temporal apply(CoreMap chunk) {
      if (tokenPattern != null) {
        return apply(chunk.get(CoreAnnotations.NumerizedTokensAnnotation.class));
//          return apply(chunk.get(CoreAnnotations.TokensAnnotation.class));
      } else {
        return apply(chunk.get(CoreAnnotations.TextAnnotation.class));
      }
    }

    public SUTime.Temporal apply(String expression) {
      Matcher matcher = stringPattern.matcher(expression);
      if (matcher.find()) {
        return extract(matcher);
      }
      return null;
    }

    public SUTime.Temporal apply(List<? extends CoreMap> tokens) {
      TokenSequenceMatcher matcher = tokenPattern.getMatcher(tokens);
      if (matcher.find()) {
        return extract(matcher);
      }
      return null;
    }

    private SUTime.Temporal extract(MatchResult m)
    {
      String val = null;
      if (valMatchGroup >= 0) {
        val = m.group(valMatchGroup);
      }
      SUTime.Duration d = extractDuration(m, val);
      if (valMatchGroup2 >= 0) {
        String val2 = m.group(valMatchGroup2);
        if (val2 != null) {
          SUTime.Duration d2 = extractDuration(m, val2);
          if (val != null && d != null) {
            d = new SUTime.DurationRange(d, d2);
          } else {
            d = d2;
          }
        }
      }       

      return addEndPoints(d);
    }

    private SUTime.Temporal addEndPoints(SUTime.Duration d)
    {
      SUTime.Temporal t = d;
      if (d != null && (beginTime != null || endTime != null)) {
        SUTime.Time b = beginTime;
        SUTime.Time e = endTime;
        // New so we get different time ids
        if (b == SUTime.TIME_REF_UNKNOWN) {
          b = new SUTime.RefTime("UNKNOWN");
        } else if (b == SUTime.TIME_UNKNOWN) {
          b = new SUTime.SimpleTime("UNKNOWN");
        }
        if (e == SUTime.TIME_REF_UNKNOWN) {
          e = new SUTime.RefTime("UNKNOWN");
        } else if (e == SUTime.TIME_UNKNOWN) {
          e = new SUTime.SimpleTime("UNKNOWN");
        }
        t = new SUTime.Range(b,e,d);
      }
      return t;
    }

    private SUTime.Duration extractDuration(MatchResult results, String val) {
      String unit = null;
      if (unitMatchGroup >= 0) unit = results.group(unitMatchGroup);
      if (val == null) {
        val = (unit.endsWith("s"))? "X":"1";
//        val = "1";
      }
      if (underspecifiedValMatchGroup >= 0) {
        val = defaultUnderspecifiedValue;
        if (results.groupCount() >= underspecifiedValMatchGroup) {
          if (results.group(underspecifiedValMatchGroup) != null) {
            val = "X";
          }
        }
      }

      SUTime.Duration d = patterns.getDuration(val, unit);
      if (d == null) {
        logger.warning("Unable to get duration with: val=" + val + ", unit=" + unit + ", matched=" + results.group());
      }
      return d;
    }

  }

  static abstract class TimePatternExtractor implements TemporalExtractor  {
    Pattern stringPattern;
    TokenSequencePattern tokenPattern;

    public SUTime.Temporal apply(CoreMap chunk) {
      if (tokenPattern != null) {
        if (chunk.containsKey(TimeExpression.ChildrenAnnotation.class)) {
          return apply(chunk.get(TimeExpression.ChildrenAnnotation.class));          
        } else {
          return apply(chunk.get(CoreAnnotations.NumerizedTokensAnnotation.class));
//            return apply(chunk.get(CoreAnnotations.TokensAnnotation.class));
        }
      } else if (stringPattern != null) {
        return apply(chunk.get(CoreAnnotations.TextAnnotation.class));
      } else {
        return extract(null);
      }
    }

    public SUTime.Temporal apply(String expression) {
      Matcher matcher = stringPattern.matcher(expression);
      if (matcher.find()) {
        return extract(matcher);
      } else {
        return null;
      }
    }

    public SUTime.Temporal apply(List<? extends CoreMap> tokens) {
      TokenSequenceMatcher matcher = tokenPattern.getMatcher(tokens);
      if (matcher.find()) {
        return extract(matcher);
      } else {
        return null;
      }
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder(getClass().getName());
      if (stringPattern != null) {
        sb.append(" with string pattern=" + stringPattern.pattern());
      } else {
        sb.append(" with token pattern=" + tokenPattern.pattern());
      }
      return sb.toString();
    }

    protected abstract SUTime.Temporal extract(MatchResult results);
  }

  static class GenericTimePatternExtractor extends TimePatternExtractor  {
    Function<MatchResult,SUTime.Temporal> tempFunc;

    protected GenericTimePatternExtractor(
            Function<MatchResult, SUTime.Temporal> tempFunc)
    {
      this.tempFunc = tempFunc;
    }

    protected GenericTimePatternExtractor(
            TokenSequencePattern tokenPattern,
            Function<MatchResult, SUTime.Temporal> tempFunc)
    {
      this.tokenPattern = tokenPattern;
      this.tempFunc = tempFunc;
    }

    protected GenericTimePatternExtractor(
            Pattern stringPattern,
            Function<MatchResult, SUTime.Temporal> tempFunc)
    {
      this.stringPattern = stringPattern;
      this.tempFunc = tempFunc;
    }
    

    protected SUTime.Temporal extract(MatchResult results) {
      try {
        return tempFunc.apply(results);
      } catch(org.joda.time.IllegalFieldValueException e) {
        logger.warning("WARNING: found invalid temporal expression: \"" + e.getMessage() +"\". Will discard it...");
        return null;
      }
    }
  }

  private static TimeExpression getTimeExpression(List<? extends CoreMap> list, int index)
  {
    return list.get(index).get(TimeExpression.Annotation.class);
  }

  @SuppressWarnings("unused")
  private static String getText(List<? extends CoreMap> list, int index)
  {
    return list.get(index).get(CoreAnnotations.TextAnnotation.class);
  }

  // FUNCTIONS for getting temporals
  static class TemporalConstFunc implements Function<MatchResult,SUTime.Temporal>
  {
    SUTime.Temporal temporal;

    TemporalConstFunc(SUTime.Temporal temporal) {
      this.temporal = temporal;
    }

    public SUTime.Temporal apply(MatchResult in) {
      return temporal;
    }
  }

  static class TemporalLookupFunc implements Function<MatchResult,SUTime.Temporal>
  {
    TimeExpressionPatterns patterns;
    int group;

    TemporalLookupFunc(TimeExpressionPatterns patterns, int group) {
      this.patterns = patterns;
      this.group = group;
    }

    public SUTime.Temporal apply(MatchResult in) {
      if (group >= 0) {
        String expr = in.group(group);
        if (expr != null) {
          return patterns.lookupTemporal(expr);
        }
      }
      return null;
    }
  }

  static class TemporalGetTEFunc implements Function<MatchResult,SUTime.Temporal>
  {
    int group = 0;
    int nodeIndex = 0;

    TemporalGetTEFunc(int group, int nodeIndex) {
      this.group = group;
      this.nodeIndex = nodeIndex;
    }

    public SUTime.Temporal apply(MatchResult in) {
      if (in instanceof SequenceMatchResult) {
        SequenceMatchResult<CoreMap> mr = (SequenceMatchResult<CoreMap>) (in);
        if (group >= 0) {
          List<? extends CoreMap> matched = mr.groupNodes(group);
          if (matched != null) {
            int i = (nodeIndex >= 0)? 0: (matched.size() + nodeIndex);
            TimeExpression te = getTimeExpression(matched, i);
            if (te != null) { return te.getTemporal(); }
          }
        }
      }
      return null;
    }
  }

  static class TemporalOpConstFunc implements Function<MatchResult,SUTime.TemporalOp>
  {
    SUTime.TemporalOp temporalOp;

    TemporalOpConstFunc(SUTime.TemporalOp temporalOp) {
      this.temporalOp = temporalOp;
    }

    public SUTime.TemporalOp apply(MatchResult in) {
      return temporalOp;
    }
  }

  static class TemporalOpLookupFunc implements Function<MatchResult,SUTime.TemporalOp>
  {
    TimeExpressionPatterns patterns;
    int group;

    TemporalOpLookupFunc(TimeExpressionPatterns patterns, int group) {
      this.patterns = patterns;
      this.group = group;
    }

    public SUTime.TemporalOp apply(MatchResult in) {
      if (group >= 0) {
        String expr = in.group(group);
        if (expr != null) {
          return patterns.lookupTemporalOp(expr);
        }
      }
      return null;
    }
  }

  static class TemporalComposeFunc implements Function<MatchResult,SUTime.Temporal>
  {
    Function<MatchResult,SUTime.TemporalOp> opFunc;
    Function<MatchResult,? extends SUTime.Temporal>[] argFuncs;

    TemporalComposeFunc(Function<MatchResult, SUTime.TemporalOp> opFunc,
                      Function<MatchResult, ? extends SUTime.Temporal>... argFuncs) {
      this.opFunc = opFunc;
      this.argFuncs = argFuncs;
    }

    public SUTime.Temporal apply(MatchResult in) {
      SUTime.TemporalOp relOp = (opFunc != null)? opFunc.apply(in):null;
      SUTime.Temporal[] args = new SUTime.Temporal[argFuncs.length];
      for (int i = 0; i < argFuncs.length; i++) {
        args[i] = (argFuncs[i] != null)? argFuncs[i].apply(in):null;
      }
      return relOp.apply(args);
      //return new SUTime.RelativeTime((SUTime.Time) ref, relOp, relArg);
    }
  }

  static class TemporalComposeObjFunc implements Function<MatchResult,SUTime.Temporal>
  {
    Function<MatchResult,SUTime.TemporalOp> opFunc;
    Function<MatchResult,? extends Object>[] argFuncs;

    TemporalComposeObjFunc(Function<MatchResult, SUTime.TemporalOp> opFunc,
                           Function<MatchResult, ? extends Object>... argFuncs) {
      this.opFunc = opFunc;
      this.argFuncs = argFuncs;
    }

    public SUTime.Temporal apply(MatchResult in) {
      SUTime.TemporalOp relOp = (opFunc != null)? opFunc.apply(in):null;
      Object[] args = new Object[argFuncs.length];
      for (int i = 0; i < argFuncs.length; i++) {
        args[i] = (argFuncs[i] != null)? argFuncs[i].apply(in):null;
      }
      return relOp.apply(args);
      //return new SUTime.RelativeTime((SUTime.Time) ref, relOp, relArg);
    }
  }

  static TimePatternExtractor getTimeExtractor(SUTime.Temporal t)
  {
    return new GenericTimePatternExtractor(new TemporalConstFunc(t));
  }

  static TimePatternExtractor getTimeLookupExtractor(TimeExpressionPatterns patterns, Pattern pattern, int group)
  {
    return new GenericTimePatternExtractor(new TemporalLookupFunc(patterns, group));
  }

  static TimePatternExtractor getTimeLookupExtractor(TimeExpressionPatterns patterns, TokenSequencePattern pattern, int group)
  {
    return new GenericTimePatternExtractor(pattern, new TemporalLookupFunc(patterns, group));
  }

  static TimePatternExtractor getRelativeTimeExtractor(
                     Pattern pattern,
                     Function<MatchResult, SUTime.Temporal> refFunc,
                     Function<MatchResult, SUTime.TemporalOp> relOpFunc,
                     Function<MatchResult, SUTime.Temporal> relArgFunc)
  {
    return new GenericTimePatternExtractor(pattern, new TemporalComposeFunc(relOpFunc, refFunc, relArgFunc));
  }

  static TimePatternExtractor getRelativeTimeExtractor(
                     TokenSequencePattern pattern, 
                     Function<MatchResult, SUTime.Temporal> refFunc,
                     Function<MatchResult, SUTime.TemporalOp> relOpFunc,
                     Function<MatchResult, SUTime.Temporal> relArgFunc)
  {
    return new GenericTimePatternExtractor(pattern, new TemporalComposeFunc(relOpFunc, refFunc, relArgFunc));
  }

  static TimePatternExtractor getRelativeTimeLookupExtractor(
                     TimeExpressionPatterns patterns,
                     Pattern pattern,
                     SUTime.Temporal ref,
                     SUTime.TemporalOp relOp,
                     int relArgGroup)
  {
    return new GenericTimePatternExtractor(pattern, new TemporalComposeFunc(
            new TemporalOpConstFunc(relOp), new TemporalConstFunc(ref), new TemporalLookupFunc(patterns, relArgGroup)));
  }

  static TimePatternExtractor getRelativeTimeLookupExtractor(
                     TimeExpressionPatterns patterns,
                     TokenSequencePattern pattern,
                     SUTime.Temporal ref,
                     SUTime.TemporalOp relOp,
                     int relArgGroup)
  {
    return new GenericTimePatternExtractor(pattern, new TemporalComposeFunc(
            new TemporalOpConstFunc(relOp), new TemporalConstFunc(ref), new TemporalLookupFunc(patterns, relArgGroup)));
  }

  public static TimePatternExtractor getIsoDateExtractor(TokenSequencePattern p, int yearGroup, int monthGroup, int dayGroup, boolean yearPartial)
  {
    return new GenericTimePatternExtractor(p,
      new IsoDateTimePatternFunc(yearGroup, monthGroup, dayGroup, -1, -1, -1, yearPartial));
  }

  public static TimePatternExtractor getIsoDateExtractor(Pattern p, int yearGroup, int monthGroup, int dayGroup, boolean yearPartial)
  {
    return new GenericTimePatternExtractor(p,
      new IsoDateTimePatternFunc( yearGroup, monthGroup, dayGroup, -1, -1, -1, yearPartial));
  }

  public static TimePatternExtractor getIsoTimeExtractor(TokenSequencePattern p, int hourGroup, int minuteGroup, int secGroup)
  {
    return new GenericTimePatternExtractor(p,
      new IsoDateTimePatternFunc( -1, -1, -1, hourGroup, minuteGroup, secGroup, false));
  }

  public static TimePatternExtractor getIsoTimeExtractor(Pattern p, int hourGroup, int minuteGroup, int secGroup)
  {
    return new GenericTimePatternExtractor(p,
      new IsoDateTimePatternFunc( -1, -1, -1, hourGroup, minuteGroup, secGroup, false));
  }

  public static TimePatternExtractor getIsoDateTimeExtractor(Pattern p, int yearGroup, int monthGroup, int dayGroup,
                                                             int hourGroup, int minuteGroup, int secGroup, boolean yearPartial)
  {
    return new GenericTimePatternExtractor(p,
      new IsoDateTimePatternFunc( yearGroup, monthGroup, dayGroup, hourGroup, minuteGroup, secGroup, yearPartial));
  }

  public static TimePatternExtractor getIsoDateTimeExtractor(TokenSequencePattern p, int yearGroup, int monthGroup, int dayGroup,
                                                             int hourGroup, int minuteGroup, int secGroup, boolean yearPartial)
  {
    return new GenericTimePatternExtractor(p,
      new IsoDateTimePatternFunc( yearGroup, monthGroup, dayGroup, hourGroup, minuteGroup, secGroup, yearPartial));
  }

  static class IsoDateTimePatternFunc implements Function<MatchResult, SUTime.Temporal>  {

    boolean partialYear = false;
    int yearGroup = -1;
    int monthGroup = -1;
    int dayGroup = -1;
    int hourGroup = -1;
    int minuteGroup = -1;
    int secGroup = -1;

    public IsoDateTimePatternFunc(int yearGroup, int monthGroup, int dayGroup, int hourGroup, int minuteGroup, int secGroup, boolean partialYear) {
      this.yearGroup = yearGroup;
      this.monthGroup = monthGroup;
      this.dayGroup = dayGroup;
      this.hourGroup = hourGroup;
      this.minuteGroup = minuteGroup;
      this.secGroup = secGroup;
      this.partialYear = partialYear;
    }

    public void setPartialYear(boolean partialYear) {
      this.partialYear = partialYear;
    }

    public SUTime.Temporal apply(MatchResult results) {
      SUTime.IsoTime isoTime = null;
      SUTime.IsoDate isoDate = null;
      boolean hasDate = (yearGroup >= 0 || monthGroup >= 0 || dayGroup >= 0);
      boolean hasTime = (hourGroup >= 0 || minuteGroup >= 0 || secGroup >= 0);
      if (hasTime) {
        String h = (hourGroup >= 0)? results.group(hourGroup):null;
        String m = (minuteGroup >= 0)? results.group(minuteGroup):null;
        String s = (secGroup >= 0)? results.group(secGroup):null;
        if (h != null || m != null || s != null) {
          isoTime = new SUTime.IsoTime(h,m,s);
        }
      }
      if (hasDate) {
        String yearStr = (yearGroup >= 0)? results.group(yearGroup):null;
        if (yearStr != null && yearStr.length() == 2 && partialYear) {
          yearStr = SUTime.PAD_FIELD_UNKNOWN2 + yearStr;
        }
        String m = (monthGroup >= 0)? results.group(monthGroup):null;
        String d = (dayGroup >= 0)? results.group(dayGroup):null;
        if (yearStr != null || m != null || d != null) {
          isoDate = new SUTime.IsoDate(yearStr, m, d);
        }
      }
      if (isoTime != null && isoDate != null) {
        return new SUTime.IsoDateTime(isoDate, isoTime);
      } else if (isoTime != null) {
        return isoTime;
      } else if (isoDate != null) {
        return isoDate;
      } else {
        return null;
      }
    }
  }

  static class IsoDateTimeExtractor implements TemporalExtractor  {
    DateTimeFormatter formatter;
    boolean hasDate;
    boolean hasTime;

    public IsoDateTimeExtractor(DateTimeFormatter formatter, boolean hasDate, boolean hasTime)
    {
      this.formatter = formatter;
      this.hasDate = hasDate;
      this.hasTime = hasTime;
    }

    public SUTime.Temporal apply(CoreMap chunk) {
      return apply(chunk.get(CoreAnnotations.TextAnnotation.class));
    }

    public SUTime.Temporal apply(String text) {
      // TODO: TIMEZONE?
      DateTime dateTime = null; 
      try {
        dateTime = formatter.parseDateTime(text);
      } catch(org.joda.time.IllegalFieldValueException e) {
        logger.warning("WARNING: Invalid temporal \"" + text + "\" (" + e.getMessage() + "). Skipping and continuing...");
        return null;
      }
      assert(dateTime != null);
      if (hasDate && hasTime) {
        return new SUTime.GroundedTime(dateTime);
//        return new SUTime.IsoDateTime( new SUTime.IsoTime(dateTime.getHourOfDay(), dateTime.getMinuteOfHour(), dateTime.getSecondOfMinute());
//        Date d = new SUTime.IsoDate(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth()) );
      } else if (hasTime) {
        // TODO: Millisecs?
        return new SUTime.IsoTime(dateTime.getHourOfDay(), dateTime.getMinuteOfHour(), dateTime.getSecondOfMinute());
      } else if (hasDate) {
        return new SUTime.IsoDate(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
      } else {
        return null;
      }      
    }

  }
}

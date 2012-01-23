package edu.stanford.nlp.ling.tokensregex;

import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Token Sequence Pattern for regular expressions for sequences over tokens (as the more general <code>CoreMap</code>)
 *
 * <p>
 * To use
 * <pre><code>
 *   TokenSequencePattern p = TokenSequencePattern.compile("....");
 *   TokenSequenceMatcher m = p.getMatcher(tokens);
 *   while (m.find()) ....
 * </code></pre>
 * </p>
 *
 * <p>
 * Supports the following:
 * <ul>
 *  <li>Concatenation: <code>X Y</code></li>
 *  <li>Or: <code>X | Y</code></li>
 *  <li>Groups:</li>
 *     <ul><li>capturing: <code>(X)</code></li>
 *     <li>noncapturing: <code>(?:X)</code></li>
 *     </ul>
 * <li>
 *  Capturing groups can be retrieved with group id:
 *  <br>
 *     To retrieve group: <code>SequenceMatchResult.match(id);</code>
 *     <br>   group 0 is matched string
 *  <br>
 *     bind variable name to group: <code>(?$var X)</code>
 *     <br>  can be retrieved using <code>SequenceMatchResult.match("$var");</code>
 *         only available in the match result
 * </li>
 * <li>Greedy Quantifiers:  <code>X+, X?, X*, X{n,m}, X{n}, X{n,}</code></li>
 * <li>Reluctant Quantifiers: <code>X+?, X??, X*?, X{n,m}?, X{n}?, X{n,}?</code></li>
 * <li>Back references: <code>\captureid</code> </li>
 * </ul>
 *
 * <p>
 * Individual tokens are marked by <code>"[" TOKEN_EXPR "]" </code>
 * <br>Possible <code>TOKEN_EXPR</code>:
 * <ul>
 * <li> All specified token attributes match:
 *     For Strings:
 *     <code> { lemma:/.../; tag:"NNP" } </code> = attributes that need to all match
 *      NOTE: <code>//</code> used for regular expressions,
 *            <code>""</code> for exact string matches
 *     For Numbers:
 *      <code>{ word>=2 }</code>
 *      NOTE: Relation can be <code>">=", "<=", ">", "<",</code> or <code>"=="</code>
 *     Others:
 *      <code>{ word#IS_NUM } , { word#IS_NIL } </code> or
 *      <code>{ word#NOT_EXISTS }, { word#NOT_NIL } </code> or <code> { word#EXISTS } </code>
 * </li>
 * <li>  Short hand for just word/text match:
 *     <code> /.../ </code>  or  <code>"..." </code>
 * </li>
 * <li>
 *  Negation:
 *     <code> !{} </code>
 * </li>
 * <li>
 *  Conjuction or Disjuction:
 *     <code> {} & {} </code>   or  <code> {} | {} </code>
 * </li>
 * </ui>
 * </p>
 *
 * <p>
 * Special tokens:
 *   Any token: <code>[]</code>
 * </p>
 *
 * <p>
 * String pattern match across multiple tokens:
 *   <code>(?m){min,max} /pattern/</code>
 * </p>
 *
 * <p>
 * Binding of variables for use in compiling patterns:
 * <ol>
 * <li> Use  Env env = TokenSequencePattern.getNewEnv() to create new environment for binding </li>
 * <li> Bind string to attribute key (Class) lookup
 *    env.bind("numtype", CoreAnnotations.NumericTypeAnnotation.class);
 * </li>
 * <li> Bind patterns / strings for compiling patterns
 *    <pre><code>
 *    // Bind string for later compilation using: compile("/it/ /was/ $RELDAY");
 *    env.bind("$RELDAY", "/today|yesterday|tomorrow|tonight|tonite/");
 *    // Bind pre-compiled patter for later compilation using: compile("/it/ /was/ $RELDAY");
 *    env.bind("$RELDAY", TokenSequencePattern.compile(env, "/today|yesterday|tomorrow|tonight|tonite/"));
 *    </code></pre>
 * </li>
 * <li> Bind custom node pattern functions (currently no arguments are supported)
 *    <pre><code>
 *    // Bind node pattern so we can do patterns like: compile("... temporal#IS_TIMEX_DATE ...");
 *    //   (TimexTypeMatchNodePattern is a NodePattern that implements some custom logic)
 *    env.bind("#IS_TIMEX_DATE", new TimexTypeMatchNodePattern(SUTime.TimexType.DATE));
 *   </code></pre>
 * </li>
 * </ol>
 * </p>
 *
 * <p>
 * Actions (partially implemented)
 * <ul>
 * <li> <code>pattern => action</code> </li>
 * <li> Supported action:
 *    <code>&annotate( { ner="DATE" } )</code> </li>
 * <li> Not applied automatically, associated with a pattern.</li>
 * <li> To apply, call <code>pattern.getAction().apply(match, groupid)</code></li>
 * </ul>
 * </p>
 *
 * @author Angel Chang
 */
public class TokenSequencePattern extends SequencePattern<CoreMap> {

  private static Env DEFAULT_ENV = getNewEnv();

  protected TokenSequencePattern(String patternStr, SequencePattern.PatternExpr nodeSequencePattern) {
    super(patternStr, nodeSequencePattern);
  }

  protected TokenSequencePattern(String patternStr, SequencePattern.PatternExpr nodeSequencePattern,
                                 SequenceMatchAction<CoreMap> action) {
    super(patternStr, nodeSequencePattern, action);
  }

  public static Env getNewEnv() {
    return new Env(new TokenSequenceParser());
  }

  public static TokenSequencePattern compile(String string)
  {
    return compile(DEFAULT_ENV, string);
  }

  public static TokenSequencePattern compile(Env env, String string)
  {
    try {
//      SequencePattern.PatternExpr nodeSequencePattern = TokenSequenceParser.parseSequence(env, string);
//      return new TokenSequencePattern(string, nodeSequencePattern);
      // TODO: Check token sequence parser?
      Pair<PatternExpr, SequenceMatchAction<CoreMap>> p = env.parser.parseSequenceWithAction(env, string);
      return new TokenSequencePattern(string, p.first(), p.second());

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static TokenSequencePattern compile(String... strings)
  {
    return compile(DEFAULT_ENV, strings);
  }
  
  public static TokenSequencePattern compile(Env env, String... strings)
  {
    try {
      List<SequencePattern.PatternExpr> patterns = new ArrayList<SequencePattern.PatternExpr>();
      for (String string:strings) {
        // TODO: Check token sequence parser?
        SequencePattern.PatternExpr pattern = env.parser.parseSequence(env, string);
        patterns.add(pattern);
      }
      SequencePattern.PatternExpr nodeSequencePattern = new SequencePattern.SequencePatternExpr(patterns);
      return new TokenSequencePattern(StringUtils.join(strings), nodeSequencePattern);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  protected static TokenSequencePattern compile(SequencePattern.PatternExpr nodeSequencePattern)
  {
    return new TokenSequencePattern(null, nodeSequencePattern);
  }

  public TokenSequenceMatcher getMatcher(List<? extends CoreMap> tokens) {
    return new TokenSequenceMatcher(this, tokens);
  }


}

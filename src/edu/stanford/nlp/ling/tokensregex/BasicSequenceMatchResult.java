package edu.stanford.nlp.ling.tokensregex;

import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.util.Interval;
import edu.stanford.nlp.util.StringUtils;

import java.util.List;

/**
* Basic results for a Sequence Match
*
* @author Angel Chang
*/
public class BasicSequenceMatchResult<T> implements SequenceMatchResult<T>
{
  List<? extends T> elements;      // Original sequence
  MatchedGroup[] matchedGroups;    // Groups that we matched
  Function<List<? extends T>, String> nodesToStringConverter;
  SequencePattern.VarGroupBindings varGroupBindings;
  double score = 0.0;
  int order;

  public List<? extends T> elements() { return elements; }

  public static <T> BasicSequenceMatchResult<T> toBasicSequenceMatchResult(List<? extends T> elements) {
    BasicSequenceMatchResult<T> matchResult = new BasicSequenceMatchResult<T>();
    matchResult.elements = elements;
    matchResult.matchedGroups = new MatchedGroup[0];
    return matchResult;
  }

  public BasicSequenceMatchResult<T> toBasicSequenceMatchResult() {
    return copy();
  }

  public BasicSequenceMatchResult<T> copy() {
    BasicSequenceMatchResult res = new BasicSequenceMatchResult<T>();
    res.elements = elements;
    res.matchedGroups = new MatchedGroup[matchedGroups.length];
    res.nodesToStringConverter = nodesToStringConverter;
    res.score = score;
    res.order = order;
    res.varGroupBindings = varGroupBindings;
    for (int i = 0; i < matchedGroups.length; i++ ) {
      if (matchedGroups[i] != null) {
        res.matchedGroups[i] = new MatchedGroup(matchedGroups[i]);
      }
    }
    return res;
  }

  public Interval<Integer> getInterval() {
    return TO_INTERVAL.apply(this);
  }

  public int getOrder() {
    return order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  public double score() {
    return score;
  }

  public int start() {
    return start(0);
  }

  public int start(int group) {
    if (group == GROUP_BEFORE_MATCH) {
      return 0;
    } else if (group == GROUP_AFTER_MATCH) {
      return matchedGroups[0].matchEnd;
    }
    if (matchedGroups[group] != null) {
      return matchedGroups[group].matchBegin;
    } else {
      return -1;
    }
  }

  public int start(String var) {
    int g = getFirstVarGroup(var);
    if (g >= 0) {
      return start(g);
    } else {
      return -1;
    }
  }

  public int end() {
    return end(0);
  }

  public int end(int group) {
    if (group == GROUP_BEFORE_MATCH) {
      return matchedGroups[0].matchBegin;
    } else if (group == GROUP_AFTER_MATCH) {
      return elements.size();
    }
    if (matchedGroups[group] != null) {
      return matchedGroups[0].matchEnd;
    } else {
      return -1;
    }
  }

  public int end(String var) {
    int g = getFirstVarGroup(var);
    if (g >= 0) {
      return end(g);
    } else {
      return -1;
    }
  }

  public String group() {
    return group(0);
  }

  public String group(int group) {
    List<? extends T> groupTokens = groupNodes(group);
    if (nodesToStringConverter == null) {
      return (groupTokens != null)? StringUtils.join(groupTokens, " "): null;
    } else {
      return nodesToStringConverter.apply(groupTokens);
    }
  }

  public String group(String var) {
    int g = getFirstVarGroup(var);
    if (g >= 0) {
      return group(g);
    } else {
      return null;
    }
  }

  public List<? extends T> groupNodes() {
    return groupNodes(0);
  }

  public List<? extends T> groupNodes(int group) {
    if (group == GROUP_BEFORE_MATCH || group == GROUP_AFTER_MATCH) {
      return elements.subList(start(group), end(group));
    }
    if (matchedGroups[group] != null) {
      return elements.subList(matchedGroups[group].matchBegin, matchedGroups[group].matchEnd);
    } else {
      return null;
    }
  }

  public List<? extends T> groupNodes(String var) {
    int g = getFirstVarGroup(var);
    if (g >= 0) {
      return groupNodes(g);
    } else {
      return null;
    }
  }

  public int groupCount() {
    return matchedGroups.length-1;
  }

  private int getFirstVarGroup(String v)
  {
    for (int i = 0; i < varGroupBindings.varnames.length; i++) {
      String s = varGroupBindings.varnames[i];
      if (v.equals(s)) {
        if (matchedGroups[i] != null) {
          return i;
        }
      }
    }
    return -1;
  }

  protected static class MatchedGroup
  {
    int matchBegin = -1;
    int matchEnd = -1;

    protected MatchedGroup(MatchedGroup mg) {
      this.matchBegin = mg.matchBegin;
      this.matchEnd = mg.matchEnd;
    }

    protected MatchedGroup(int matchBegin, int matchEnd) {
      this.matchBegin = matchBegin;
      this.matchEnd = matchEnd;
    }

    public String toString()
    {
      return "(" + matchBegin + "," + matchEnd + ")";
    }
  }

}

package edu.stanford.nlp.ling.tokensregex;

import edu.stanford.nlp.util.Comparators;
import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.util.HasInterval;
import edu.stanford.nlp.util.Interval;

import java.util.Comparator;
import java.util.List;
import java.util.regex.MatchResult;

/**
 * Sequence Match Result
 *
 * @author Angel Chang
 */
public interface SequenceMatchResult<T> extends MatchResult, HasInterval<Integer> {
  // TODO: Need to be careful with GROUP_BEFORE_MATCH/GROUP_AFTER_MATCH
  public static int GROUP_BEFORE_MATCH = Integer.MIN_VALUE;  // Special match groups (before match)
  public static int GROUP_AFTER_MATCH = Integer.MIN_VALUE+1;   // Special match groups (after match)

  public double score();

  public List<? extends T> elements();
  
  public List<? extends T> groupNodes();

  public List<? extends T> groupNodes(int group);

  public BasicSequenceMatchResult<T> toBasicSequenceMatchResult();

  // String lookup versions using variables

  public List<? extends T> groupNodes(String groupVar);
  public String group(String groupVar);
  public int start(String groupVar);
  public int end(String groupVar);
  public int getOrder();

  public static final GroupToIntervalFunc TO_INTERVAL = new GroupToIntervalFunc(0);
  public static class GroupToIntervalFunc<MR extends MatchResult> implements Function<MR, Interval<Integer>> {
    int group;
    public GroupToIntervalFunc(int group) { this.group = group; }
    public Interval<Integer> apply(MR in) {
      return Interval.toInterval(in.start(group), in.end(group), Interval.INTERVAL_OPEN_END);
    }
  }

  public final static Comparator<MatchResult> SCORE_COMPARATOR = new Comparator<MatchResult>() {
    public int compare(MatchResult e1, MatchResult e2) {
      double s1 = 0;
      if (e1 instanceof SequenceMatchResult) { s1 =  ((SequenceMatchResult) e1).score(); };
      double s2 = 0;
      if (e2 instanceof SequenceMatchResult) { s2 =  ((SequenceMatchResult) e2).score(); };
      if (s1 == s2) {
        return 0;
      } else {
        return (s1 > s2)? -1:1;
      }
    }
  };

  public final static Comparator<MatchResult> ORDER_COMPARATOR =
    new Comparator<MatchResult>() {
    public int compare(MatchResult e1, MatchResult e2) {
      int o1 = 0;
      if (e1 instanceof SequenceMatchResult) {o1 =  ((SequenceMatchResult) e1).getOrder(); };
      int o2 = 0;
      if (e2 instanceof SequenceMatchResult) {o2 =  ((SequenceMatchResult) e2).getOrder(); };
      if (o1 == o2) {
        return 0;
      } else {
        return (o1 < o2)? -1:1;
      }
    }
  };

  // Compares two match results.
  // Use to order match results by:
  //    length (longest first),
  public final static Comparator<MatchResult> LENGTH_COMPARATOR =
    new Comparator<MatchResult>() {
      public int compare(MatchResult e1, MatchResult e2) {
        int len1 = e1.end() - e1.start();
        int len2 = e2.end() - e2.start();
        if (len1 == len2) {
          return 0;
        } else {
          return (len1 > len2)? -1:1;
        }
      }
    };

  public final static Comparator<MatchResult> OFFSET_COMPARATOR =
    new Comparator<MatchResult>() {
      public int compare(MatchResult e1, MatchResult e2) {
        if (e1.start() == e2.start()) {
          if (e1.end() == e2.end()) {
            return 0;
          } else {
            return (e1.end() < e2.end())? -1:1;
          }
        } else {
          return (e1.start() < e2.start())? -1:1;
        }
      }
    };

  // Compares two match results.
  // Use to order match results by:
   //   score
  //    length (longest first),
  //       and then begining token offset (smaller offset first)
  //    original order (smaller first)
  public final static Comparator<MatchResult> SCORE_LENGTH_ORDER_OFFSET_COMPARATOR =
          Comparators.chain(SCORE_COMPARATOR, LENGTH_COMPARATOR, ORDER_COMPARATOR, OFFSET_COMPARATOR);
  public final static Comparator<? super MatchResult> DEFAULT_COMPARATOR = SCORE_LENGTH_ORDER_OFFSET_COMPARATOR;


}

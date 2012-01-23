package edu.stanford.nlp.ling.tokensregex;

import java.util.List;

/**
 * Matches a Node (i.e a Token)
 *
 * @author Angel Chang
 */
public abstract class NodePattern<T> {
  public final static NodePattern ANY_NODE = new AnyNodePattern();

  protected abstract boolean match(T node);
  protected Object matchWithResult(T node) {
    if (match(node)) return Boolean.TRUE;
    else return null;
  }

  protected static class AnyNodePattern<T> extends NodePattern<T> {
    protected AnyNodePattern() {
    }

    protected boolean match(T node) {
      return true;
    }
  }

  protected static class NegateNodePattern<T> extends NodePattern<T> {
    NodePattern<T> p;

    protected NegateNodePattern(NodePattern<T> p) {
      this.p = p;
    }

    protected boolean match(T node)
    {
      return !p.match(node);
    }
  }

  protected static class ConjNodePattern<T> extends NodePattern<T> {
    List<NodePattern<T>> nodePatterns;

    protected ConjNodePattern(List<NodePattern<T>> nodePatterns) {
      this.nodePatterns = nodePatterns;
    }

    protected boolean match(T node)
    {
      boolean matched = true;
      for (NodePattern<T> p:nodePatterns) {
        if (!p.match(node)) {
          matched = false;
          break;
        }
      }
      return matched;
    }
  }

  protected static class DisjNodePattern<T> extends NodePattern<T> {
    List<NodePattern<T>> nodePatterns;

    protected DisjNodePattern(List<NodePattern<T>> nodePatterns) {
      this.nodePatterns = nodePatterns;
    }

    protected boolean match(T node)
    {
      boolean matched = false;
      for (NodePattern<T> p:nodePatterns) {
        if (p.match(node)) {
          matched = true;
          break;
        }
      }
      return matched;
    }
  }
}

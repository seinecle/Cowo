package edu.stanford.nlp.pipeline;

import java.util.HashMap;
import java.util.Map;

import edu.stanford.nlp.util.Factory;

/**
 * An object for keeping track of Annotators. Typical use is to allow multiple
 * pipelines to share any Annotators in common.
 * 
 * For example, if multiple pipelines exist, and they both need a
 * ParserAnnotator, it would be bad to load two such Annotators into memory.
 * Instead, an AnnotatorPool will only create one Annotator and allow both
 * pipelines to share it.  
 * 
 * @author bethard
 */
public class AnnotatorPool {
  private Map<String, Annotator> annotators;
  private Map<String, Factory<Annotator>> factories;
  
  /**
   * Create an empty AnnotatorPool. 
   */
  public AnnotatorPool() {
    this.annotators = new HashMap<String, Annotator>();
    this.factories = new HashMap<String, Factory<Annotator>>();
  }
  
  /**
   * Register an Annotator that can be created by the pool.
   * 
   * Note that factories are used here so that many possible annotators can
   * be defined within the AnnotatorPool, but an Annotator is only created
   * when one is actually needed.
   * 
   * @param name    The name to be associated with the Annotator.
   * @param factory A factory that creates an instance of the desired Annotator.
   */
  public void register(String name, Factory<Annotator> factory) {
    if (this.factories.containsKey(name)) {
      String fmt = "name %s already associated with factory %s";
      String msg = String.format(fmt, name, this.factories.get(name));
      throw new IllegalArgumentException(msg);
    }
    this.factories.put(name, factory);
  }
  
  /**
   * Retrieve an Annotator from the pool. If the named Annotator has not yet
   * been requested, it will be created. Otherwise, the existing instance of
   * the Annotator will be returned.   
   * 
   */
  public synchronized Annotator get(String name) {
    if (!this.annotators.containsKey(name)) {
      Factory<Annotator> factory = this.factories.get(name);
      if (factory == null) {
        throw new IllegalArgumentException("no annotator named " + name);
      }
      this.annotators.put(name, factory.create());
    }
    return this.annotators.get(name);
  }
}

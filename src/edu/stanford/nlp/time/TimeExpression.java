package edu.stanford.nlp.time;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.ChunkAnnotationUtils;
import edu.stanford.nlp.pipeline.CoreMapAttributeAggregator;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.ErasureUtils;
import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.util.Interval;

import java.util.List;
import java.util.Map;

/**
 * Time Expression
 *
 *
 * @author Angel Chang
 */
public class TimeExpression {
  /**
   * The CoreMap key for storing a TimeExpression annotation
   */
  public static class Annotation implements CoreAnnotation<TimeExpression> {
    public Class<TimeExpression> getType() {
      return TimeExpression.class;
    }
  }

  /**
   * The CoreMap key for storing a nested annotations
   */
  public static class ChildrenAnnotation implements CoreAnnotation<List<? extends CoreMap>> {
    public Class<List<? extends CoreMap>> getType() {
      return ErasureUtils.<Class<List<? extends CoreMap>>> uncheckedCast(List.class);
    }
  }
  String text; // Text representing the time
  int tid;     // Time ID // TODO: Populate

  private Interval<Integer> charOffsets;
  Interval<Integer> tokenOffsets;
  Interval<Integer> chunkOffsets;
  Map<String,String> attributes;
  SUTime.Temporal temporal;
  SUTime.Temporal origTemporal;
  CoreMap annotation;
  Function<CoreMap, SUTime.Temporal> temporalFunc;
  int anchorTimeId = -1;
  boolean includeNested = false;

  // Used to disambiguate time expressions
  double score;
  int order;

  public TimeExpression(CoreMap annotation, Function<CoreMap, SUTime.Temporal> temporalFunc, double score)
  {
    this.annotation = annotation;
    this.temporalFunc = temporalFunc;
    text = annotation.get(CoreAnnotations.TextAnnotation.class);
    temporal = temporalFunc.apply(annotation);
    this.score = score;
  }

  public TimeExpression(Interval<Integer> charOffsets, Interval<Integer> tokenOffsets, Function<CoreMap, SUTime.Temporal> temporalFunc, double score)
  {
    this.charOffsets = charOffsets;
    this.tokenOffsets = tokenOffsets;
    this.chunkOffsets = tokenOffsets;
    this.temporalFunc = temporalFunc;
    this.score = score;
  }

  public CoreMap extractAnnotation(CoreMap sourceAnnotation)
  {
    if (chunkOffsets != null) {
      annotation = ChunkAnnotationUtils.getMergedChunk(sourceAnnotation.get(CoreAnnotations.NumerizedTokensAnnotation.class),
              chunkOffsets.getBegin(), chunkOffsets.getEnd(), CoreMapAttributeAggregator.DEFAULT_NUMERIC_TOKENS_AGGREGATORS );
      if (sourceAnnotation.containsKey(CoreAnnotations.TextAnnotation.class)) {
        ChunkAnnotationUtils.annotateChunkText(annotation, sourceAnnotation);
      }

      charOffsets = Interval.toInterval(annotation.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class), annotation.get(CoreAnnotations.CharacterOffsetEndAnnotation.class));
      tokenOffsets = Interval.toInterval(annotation.get(CoreAnnotations.TokenBeginAnnotation.class),
              annotation.get(CoreAnnotations.TokenEndAnnotation.class), Interval.INTERVAL_OPEN_END);
    } else {
      Integer baseCharOffset = sourceAnnotation.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
      if (baseCharOffset == null) {
        baseCharOffset = 0;
      }

      chunkOffsets = ChunkAnnotationUtils.getChunkOffsetsUsingCharOffsets(sourceAnnotation.get(CoreAnnotations.NumerizedTokensAnnotation.class),
              charOffsets.getBegin() + baseCharOffset, charOffsets.getEnd()  + baseCharOffset);
      CoreMap annotation2 = ChunkAnnotationUtils.getMergedChunk(sourceAnnotation.get(CoreAnnotations.NumerizedTokensAnnotation.class),
              chunkOffsets.getBegin(), chunkOffsets.getEnd(), CoreMapAttributeAggregator.DEFAULT_NUMERIC_TOKENS_AGGREGATORS );

      annotation = ChunkAnnotationUtils.getAnnotatedChunkUsingCharOffsets(sourceAnnotation, charOffsets.getBegin(), charOffsets.getEnd());
      tokenOffsets = Interval.toInterval(annotation.get(CoreAnnotations.TokenBeginAnnotation.class),
              annotation.get(CoreAnnotations.TokenEndAnnotation.class), Interval.INTERVAL_OPEN_END);
      annotation.set(CoreAnnotations.NumerizedTokensAnnotation.class, annotation2.get(CoreAnnotations.NumerizedTokensAnnotation.class));
    }
    annotation.set(TimeExpression.ChildrenAnnotation.class, annotation.get(CoreAnnotations.NumerizedTokensAnnotation.class));
    annotation.set(Annotation.class, this);
    text = annotation.get(CoreAnnotations.TextAnnotation.class);
    temporal = temporalFunc.apply(annotation);
    return annotation;
  }

/*  public CoreMap extractAnnotation(CoreMap sourceAnnotation)
  {
    if (tokenOffsets != null) {
      annotation = ChunkAnnotationUtils.getAnnotatedChunk(sourceAnnotation, tokenOffsets.getBegin(), tokenOffsets.getEnd());
      charOffsets = Interval.toInterval(annotation.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class), annotation.get(CoreAnnotations.CharacterOffsetEndAnnotation.class));
      tokenOffsets = Interval.toInterval(annotation.get(CoreAnnotations.TokenBeginAnnotation.class),
              annotation.get(CoreAnnotations.TokenEndAnnotation.class), Interval.INTERVAL_OPEN_END);
    } else {
      annotation = ChunkAnnotationUtils.getAnnotatedChunkUsingCharOffsets(sourceAnnotation, charOffsets.getBegin(), charOffsets.getEnd());
      tokenOffsets = Interval.toInterval(annotation.get(CoreAnnotations.TokenBeginAnnotation.class),
              annotation.get(CoreAnnotations.TokenEndAnnotation.class), Interval.INTERVAL_OPEN_END);
      Integer baseTokenOffset = annotation.get(CoreAnnotations.TokenBeginAnnotation.class);
      if (baseTokenOffset == null) { baseTokenOffset = 0; };
      chunkOffsets = Interval.toInterval(annotation.get(CoreAnnotations.TokenBeginAnnotation.class) - baseTokenOffset,
              annotation.get(CoreAnnotations.TokenEndAnnotation.class) - baseTokenOffset, Interval.INTERVAL_OPEN_END);
    }
    annotation.set(TimeExpression.ChildrenAnnotation.class, annotation.get(CoreAnnotations.TokensAnnotation.class));
    annotation.set(Annotation.class, this);
    text = annotation.get(CoreAnnotations.TextAnnotation.class);
    temporal = temporalFunc.apply(annotation);
    return annotation;
  } */

  public CoreMap extractAnnotation(List<? extends CoreMap> source)
  {
    annotation = ChunkAnnotationUtils.getMergedChunk(source,  chunkOffsets.getBegin(), chunkOffsets.getEnd(), CoreMapAttributeAggregator.getDefaultAggregators());
    charOffsets = Interval.toInterval(annotation.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class),
            annotation.get(CoreAnnotations.CharacterOffsetEndAnnotation.class), Interval.INTERVAL_OPEN_END);
    tokenOffsets = Interval.toInterval(annotation.get(CoreAnnotations.TokenBeginAnnotation.class),
              annotation.get(CoreAnnotations.TokenEndAnnotation.class), Interval.INTERVAL_OPEN_END);
    annotation.set(Annotation.class, this);
    annotation.set(TimeExpression.ChildrenAnnotation.class, source.subList(chunkOffsets.getBegin(), chunkOffsets.getEnd()));
    text = annotation.get(CoreAnnotations.TextAnnotation.class);
    temporal = temporalFunc.apply(annotation);
    return annotation;
  }

  public int getTid() {
    return tid;
  }

  public String getText() {
    return text;
  }

  public CoreMap getAnnotation() {
    return annotation;
  }

  public SUTime.Temporal getTemporal() { return temporal; }

  public String toString()
  {
    return text;
  }

/*  public Timex getTimex(SUTime.TimeIndex timeIndex) {
    Timex timex = temporal.getTimex(timeIndex);
    timex.text = text;
    timex.xml = timex
    assert(timex.tid == tid);
  } */


}

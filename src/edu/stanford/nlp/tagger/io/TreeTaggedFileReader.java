package edu.stanford.nlp.tagger.io;

import java.util.Iterator;
import java.util.List;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.trees.DiskTreebank;
import edu.stanford.nlp.trees.LabeledScoredTreeReaderFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.Treebank;
import edu.stanford.nlp.trees.TreeNormalizer;
import edu.stanford.nlp.trees.TreeReaderFactory;
import edu.stanford.nlp.trees.TreeTransformer;

public class TreeTaggedFileReader implements TaggedFileReader {
  final Treebank treebank;
  final String filename;
  final TreeReaderFactory trf;
  final TreeTransformer transformer;
  final TreeNormalizer normalizer;

  final Iterator<Tree> treeIterator;
  
  int numSentences = 0;

  public TreeTaggedFileReader(TaggedFileRecord record) {
    filename = record.file;
    trf = new LabeledScoredTreeReaderFactory();
    transformer = record.treeTransformer;
    normalizer = record.treeNormalizer;

    treebank = new DiskTreebank(trf, record.encoding);
    if (record.treeRange != null) {
      treebank.loadPath(filename, record.treeRange);
    } else {
      treebank.loadPath(filename);
    }

    treeIterator = treebank.iterator();
  }

  public Iterator<List<TaggedWord>> iterator() { return this; }

  public String filename() { return filename; }

  public boolean hasNext() { return treeIterator.hasNext(); }

  public List<TaggedWord> next() {
    Tree t = treeIterator.next();
    if (normalizer != null) {
      t = normalizer.normalizeWholeTree(t, t.treeFactory());
    }
    if (transformer != null) {
      t = t.transform(transformer);
    }
    return t.taggedYield();
  }

  public void remove() { throw new UnsupportedOperationException(); }
}

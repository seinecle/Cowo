/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package seinecle.utils.text;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 *
 * @author C. Levallois
 */
public class StanfordLemmatization  {
    private final String entryWord;
    
    StanfordLemmatization(String input){
        
        this.entryWord = input;
        call();
    }
    

    //@Override
   public String call(){
    
        String exitWord = "";        
        
        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
                Properties props = new Properties();
                props.put("annotators", "tokenize, ssplit, pos, parse, lemma");
                StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

                // create an empty Annotation just with the given text
                Annotation document = new Annotation(entryWord);

                // run all Annotators on this text
                pipeline.annotate(document);
                // these are all the sentences in this document
                // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
                List<CoreMap> sentences = document.get(SentencesAnnotation.class);

                for (CoreMap sentence : sentences) {
                    // traversing the words in the current sentence
                    // a CoreLabel is a CoreMap with additional token-specific methods
                    for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                        // this is the text of the token
                        exitWord = token.get(LemmaAnnotation.class);



                    }

                }
            
    return exitWord;
    }
    
}

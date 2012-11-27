/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import com.alchemyapi.api.AlchemyAPI;
import com.google.common.collect.HashMultimap;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author C. Levallois
 */
public class AlchemyExtractor implements Callable {

    String currLine;

    AlchemyExtractor(String currLine) {

        this.currLine = currLine;
    }

    @Override
    public String call() throws IOException {
        try {
            StringBuilder currAlchemyText = new StringBuilder();
            HashMultimap<String, String> currMapTypeToText = HashMultimap.create();
            HashMap<String, String> currMapTextToType = new HashMap();

            AlchemyAPI alchemyObj = AlchemyAPI.GetInstanceFromString(Controller.AlchemyAPIKey);

            Document doc = alchemyObj.TextGetRankedNamedEntities(currLine);


            //System.out.println(getStringFromDocument(doc));
            String[] xmlLines = getStringFromDocument(doc).split("\n");
            int countPair = 0;
            String type = "";
            String text = "";
            for (int i = 0; i < xmlLines.length; i++) {

                if (xmlLines[i].contains("<type>")) {
                    type = xmlLines[i].replaceAll("(.*<type>)(.*)(</type>.*)", "$2").trim();
                    //System.out.println("type is: \"" + type+"\"");
                    countPair = 1;
                }
                if (xmlLines[i].contains("<text>")) {
                    text = xmlLines[i].replaceAll("(.*<text>)(.*)(</text>.*)", "$2").trim();
                    //System.out.println("text is: \"" + text+"\"");
                    countPair = 2;
                }
                if (countPair == 2) {
                    if (Controller.setFilteredFields.contains(type) & text.length() >= Controller.minWordLength) {
                        //System.out.println("detected type " + type + ": " + text);
                        if (type.equals("Person")) {
                            if (text.contains(" ")) {
                                currMapTypeToText.put(type, text.toLowerCase());
                                currMapTextToType.put(text.toLowerCase(), type);
                            }
                        } else {
                            currMapTypeToText.put(type, text.toLowerCase());
                            currMapTextToType.put(text.toLowerCase(), type);
                        }


                    }

                }


            }


            Controller.overallMapTypeToText.putAll(currMapTypeToText);
            Controller.overallMapTextToType.putAll(currMapTextToType);
            Iterator<String> ITCurrMap = currMapTypeToText.values().iterator();
            while (ITCurrMap.hasNext()) {
                currAlchemyText.append(ITCurrMap.next()).append("|");

            }


            //these if and else conditions take care of the binary counting option.
            //when relying on n-grams, the binary counting case is dealt in the n-gram finder class
            if (Controller.binary) {
                HashSet setUniqueValues = new HashSet();
                setUniqueValues.addAll(currMapTypeToText.values());
                Controller.freqSet.addAll(setUniqueValues);

            } else {
                Controller.freqSet.addAll(currMapTypeToText.values());
            }

            currLine = currAlchemyText.toString();
            //System.out.println("currLine via Alchemy: " + currLine);

        } catch (SAXException ex) {
            Logger.getLogger(AlchemyExtractor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(AlchemyExtractor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(AlchemyExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }

        return currLine;



    }

    private static String getStringFromDocument(Document doc) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);

            return writer.toString();
        } catch (TransformerException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}

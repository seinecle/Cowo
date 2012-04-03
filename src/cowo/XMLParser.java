/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

/**
 *
 * @author C. Levallois
 */
import com.google.common.collect.HashMultimap;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class XMLParser extends DefaultHandler {

    static SAXParser sp;
    static InputSource toBeParsed;
    static SAXParserFactory spf;
    
    public String tempVal;
    int numberOfThreads = Runtime.getRuntime().availableProcessors();
    ExecutorService pool = Executors.newFixedThreadPool(numberOfThreads);
    public String itemType;
    public static int counter;
    public String extractedText;

    
    StringBuilder test;
    String currItemType;
    String currYearHere;
    String currMonthHere;
    
    private boolean newEntity;
    private String type;
    private String text;
    private boolean newType;
    private boolean newText;
    
    private HashMultimap<String,String> mapTypeToText = HashMultimap.create();

    public XMLParser(InputSource is) throws IOException {

            toBeParsed = is;
            System.out.println("we are in the XML parser constructor");

    }


    public void parseDocument() throws IOException {

        System.out.println("we are in the parse document method 1");
        
        //get a factory
        spf = SAXParserFactory.newInstance();
        try {
            System.out.println("we are in the parse document method 2");

            //get a new instance of parser
            sp = spf.newSAXParser();
                    System.out.println("we are in the parse document method 3");

            //parse the file and also register this class for call backs
            sp.parse(toBeParsed, this);



        } catch (SAXException se) {
        } catch (ParserConfigurationException pce) {
        } catch (IOException ie) {
        }
    
        
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {


        test = new StringBuilder();



        if (qName.equals("entity")) {
            newEntity = true;
        }


        if (qName.equals("type")) {
            newType = true;
        }

        if (qName.equalsIgnoreCase("text")) {
            newText = true;
            System.out.println("in a text: ");
            Main.currMapTypeToText.put(type, text);
            
        }


    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {

        if (newText) {
            test.append(ch, start, length);
            System.out.println("text: "+test.toString());
        }

    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {



        if (qName.matches("entity")) {
            System.out.println("out entity");
            newEntity = false;
        }

        if (qName.matches("type")) {
            newType = false;
        }

        if (qName.matches("type")) {
            newText = false;
        }



    }



}

package org.goods.living.nhif.uhc.data.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;

/**
 * @author ernestmurimi
 */
public final class Utilities {

    private final Logging logger;

    public Utilities(Logging logger) {
        this.logger = logger;
    }

    /**
     * This function gets a mapString and removes the curly brackets then
     * converts it into a String array by splitting it between the = then creates
     * a map in a foreach loop
     * @param mapString - The map string to convert into HashMap
     * @return -  a result map from the string
     * @throws Exception thrown when any error occurs, recoverable but function
     * cannot proceed. The result is a null map
     */
    public static Map<String,String> convertStringToMap(String mapString) throws Exception {
        mapString = mapString.substring(1, mapString.length()-1);           //remove curly brackets
        String[] keyValuePairs = mapString.split(",");              //split the string to creat key-value pairs
        HashMap<String,String> map = new HashMap<String,String>();               

        for(String pair : keyValuePairs)                        //iterate over the pairs
        {
            String[] entry = pair.split("=");                   //split the pairs to get key and value 
            map.put(entry[0].trim(), entry[1].trim());          //add them to the hashmap and trim whitespaces
        }
        return map;
    }
    
    /**
     * Generate Random UUID using Java UUID Utility
     *
     * @return - UUID Generated
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Utility methods to handle date
     */
    public static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("YY-MM-dd");
        }
    };

    public static final ThreadLocal<SimpleDateFormat> monthDayFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("MMDD");
        }
    };

    public static final ThreadLocal<SimpleDateFormat> timeFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("HHmmss");
        }
    };
    public static final ThreadLocal<SimpleDateFormat> dateTimeFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    /**
     * This function gets a SOAP message and converts it into a String, this
     * enables easy logging of the message and usage in other dependant
     * functions.
     *
     * @param soapMessage - The soap message prepared by the daemon or received
     * by daemon from API
     * @return - A string instance of the SOAP payload which maybe logged or
     * transformed into a DOM Document to get node elements from it
     * @throws Exception - Exception is thrown incase the function is unable to
     * Stringify the SOAP message
     */
    public static String toStringSOAPMessage(SOAPMessage soapMessage) throws Exception {
        Source source = soapMessage.getSOAPPart().getContent();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        StringWriter stringWriter = new StringWriter();
        StreamResult streamResult = new StreamResult(stringWriter);
        transformer.transform(source, streamResult);

        return stringWriter.toString();
    }

    /**
     * This function gets a String SOAP message and converts it back into a SOAP
     * message
     *
     * @param xml - the String XML payload
     * @return - the processed SOAP message
     * @throws SOAPException - SOAP exception is thrown if it fails to create
     * the SOAP message
     * @throws IOException - IO Exception thrown if it cannot get the XML string
     * to process
     */
    public static SOAPMessage getSoapMessageFromString(String xml) throws SOAPException, IOException {
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage message = factory.createMessage(new MimeHeaders(), new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-8"))));
        return message;
    }

    /**
     * Convert nodes from xml.
     *
     * This will receive the xml as a String and convert it to a bytStream and
     * then into an XML file. Then it passes it to the createMap function that
     * will iterate and get the nodes and their values
     *
     * @param xml the xml from the server
     * @return the object - a Map of node and value
     * @throws Exception the exception
     */
    public static Map convertNodesFromXml(String xml) throws Exception {

        InputStream is = new ByteArrayInputStream(xml.getBytes());
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(is);
        return (Map) createMap(document.getDocumentElement());
    }

    /**
     * Create map.
     *
     * This will create a Map Object from the XML Doc that's been created and
     * Unmarshall it into a Map. This is done by reading the XML node elements
     * and their values to make a Map of Key(node) and value(element).
     *
     * @param node the node <node>nodeData</node>
     * @return the object
     */
    public static Object createMap(Node node) {
        Map<String, Object> map = new HashMap<String, Object>();
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            String name = currentNode.getNodeName();
            Object value = null;
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                value = createMap(currentNode);
            } else if (currentNode.getNodeType() == Node.TEXT_NODE) {
                return currentNode.getTextContent();
            }
            if (map.containsKey(name)) {
                Object object = map.get(name);
                if (object instanceof List) {
                    ((List<Object>) object).add(value);
                } else {
                    List<Object> objectList = new LinkedList<Object>();
                    objectList.add(object);
                    objectList.add(value);
                    map.put(name, objectList);
                }
            } else {
                map.put(name, value);
            }
        }
        return map;
    }

    /**
     * This sends an echo test to the Server to ensure that we have a connection
     * before we can make a service request on the server.
     *
     * @return 0 for failure and 1 for success
     */
    public static int pingAPIURL(String url, int timeout) {
        // Otherwise an exception may be thrown on invalid SSL certificates:
        url = url.replaceFirst("^https", "http");

        int responseCode = 0;

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("HEAD");
            responseCode = connection.getResponseCode();
            return responseCode;
        } catch (IOException exception) {
            return responseCode;
        }
    }

    /**
     * this function receives a string URL query and converts it into a hashmap
     *
     * @param query - The URL query string
     * @return - HashMap of key and object
     */
    public static Map<String, String> convertStringQueryToMap(String query) throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            } else {
                result.put(pair[0], "");
            }
        }
        return result;
    }

    public static Document createDOMDocument(String xml) throws Exception {
        InputStream is = new ByteArrayInputStream(xml.getBytes());
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(is);

        return document;
    }

    /**
     * To string xml message string.
     *
     * This gets an XML DOM Document and converts it into a String variable
     *
     * @param xmlPayload the xml payload
     * @return the string
     * @throws Exception the exception
     */
    public static String toStringXMLMessage(Document xmlPayload) throws Exception {
        DOMSource source = new DOMSource(xmlPayload);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter stringWriter = new StringWriter();
        StreamResult streamResult = new StreamResult(stringWriter);
        transformer.transform(source, streamResult);

        return stringWriter.toString().replace("\n", "");
    }
    
    public static HashMap parseMap(Map<String, String> responseMap) {
        HashMap dataMap = new HashMap();
        for (String key : responseMap.keySet()) {
            dataMap.put(key, responseMap.get(key));
        }

        return dataMap;
    }

    /**
     * Log pre string.
     *
     * @return the string
     */
    public static String logPreString() {
        return "NHIF Registration Service | " + Thread.currentThread().getStackTrace()[2].getClassName() + " | "
                + Thread.currentThread().getStackTrace()[2].getLineNumber() + " | "
                + Thread.currentThread().getStackTrace()[2].getMethodName() + "() | ";
    }
}

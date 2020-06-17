package com.example.scripts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Helper class to convert Xliff files to localized string resources.
 */
public class XliffToXml {

    public static final String ATTR_NAME = "name";
    public static final String ATTR_STRING = "string";
    public static final String ATTR_TRANS_UNIT = "trans-unit";
    public static final String ATTR_ID = "id";
    public static final String ATTR_TARGET = "target";
    public static final String ATTR_FILE = "file";
    public static final String ATTR_TARGET_LANGUAGE = "target-language";
    public static final String ATTR_RESOURCES = "resources";

    public static final String PATH_DEFAULT_STRINGS = "connect-button/src/main/res/values/strings.xml";
    public static final String PATH_XLIFF_PACKAGE = "scripts/src/IFTTTXliffProject";
    public static final String PATH_VALUES_LOCALE = "connect-button/src/main/res/values-";
    public static final String PATH_STRINGS_ABSOLUTE = "/strings.xml";

    public static void main(String[] args)
            throws ParserConfigurationException, SAXException, IOException {
        Set<String> stringIds = getIdList();

        List<String> files = getXLiffFileList();
        for (String fileName: files) {
            convertXliffToXml(fileName, stringIds);
        }
    }

    /**
     * @returns a set of string IDs contained in the default strings.xml file within connect-button module
     */
    private static Set<String> getIdList() {
        try {
            File inputFile = new File(PATH_DEFAULT_STRINGS);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName(ATTR_STRING);
            Set<String> stringIds = new HashSet<>();
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    stringIds.add(eElement.getAttribute(ATTR_NAME));
                }
            }
            return stringIds;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @returns a list of xliff files contained within the xliff package
     */
    private static List<String> getXLiffFileList() {
        List<String> xliffFiles = new ArrayList<>();
        File folder = new File(PATH_XLIFF_PACKAGE);
        File[] listOfFiles = folder.listFiles();
        assert listOfFiles != null;
        for (File file: listOfFiles) {
            if (file.isFile() && file.getName().endsWith(".xliff")) {
                xliffFiles.add(PATH_XLIFF_PACKAGE + "/" +file.getName());
            }
        }

        return xliffFiles;
    }

    /**
     *
     * @param file to be parsed
     * @param stringIds IDs for which the translation units must be parsed
     * This method parses the file, and gets the locale and translation units to be added to the xml
     */
    private static void convertXliffToXml(String file, Set<String> stringIds)
            throws ParserConfigurationException, IOException, SAXException {
        Map<String, String> translationUnitsMap = new HashMap<>();
        File inputFile = new File(file);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();

        NodeList fileElements = doc.getElementsByTagName(ATTR_FILE);
        assert fileElements.getLength() == 1;
        String targetLanguage = ((Element) fileElements.item(0)).getAttribute(ATTR_TARGET_LANGUAGE);
        String locale = targetLanguage;
        if (targetLanguage.contains("-")) {
            locale = targetLanguage.replace("-", "-r");
        }

        NodeList nList = doc.getElementsByTagName(ATTR_TRANS_UNIT);

        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);

            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element transUnitElement = (Element) nNode;
                String id = transUnitElement.getAttribute(ATTR_ID);
                if (stringIds.contains(id)) {
                    String translatedValue = transUnitElement.getElementsByTagName(ATTR_TARGET)
                            .item(0)
                            .getTextContent();
                    translationUnitsMap.put(id, translatedValue);
                }
            }
        }

        createLocaleFile(locale, translationUnitsMap);
    }

    /**
     *
     * @param locale to create the respective strings resource file
     * @param translationUnitsMap map of String ID to value to be added to the strings resource
     */
    private static void createLocaleFile(String locale, Map<String, String> translationUnitsMap) {
        String targetDirName = PATH_VALUES_LOCALE+locale;
        new File(targetDirName).mkdirs();

        String targetFilePath = targetDirName + PATH_STRINGS_ABSOLUTE;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            //Add elements to Document
            Element rootElement = doc.createElement(ATTR_RESOURCES);
            //Append root element to document
            doc.appendChild(rootElement);

            for (String key: translationUnitsMap.keySet()) {
                rootElement.appendChild(createStringElement(doc, key, translationUnitsMap.get(key)));
            }

            //For output to file, console
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            //For pretty print
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            //Write data
            transformer.transform(source, new StreamResult(new File(targetFilePath)));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param id to be used to create the string name attribute
     * @param value to be used to add a value for the string name
     * @return Node in the format <string name="name">value</string>
     */
    private static Node createStringElement(Document doc, String id, String value) {
        Element element = doc.createElement(ATTR_STRING);
        element.setAttribute(ATTR_NAME, id);
        element.setTextContent(value);
        return element;
    }
}

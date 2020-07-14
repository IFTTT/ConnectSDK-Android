package com.script.xliffscript;

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
 * In the main function, replace the Default strings path and Target directory path to:
 * PATH_SDK_DEFAULT_VALUES and PATH_SDK_VALUES_LOCALE for SDK.
 * PATH_IFTTT_APP_DEFAULT_STRINGS and PATH_IFTTT_APP_VALUES_LOCALE for the IFTTT Handoff flow.
 * Before running the script, replace %1$s by %2$s where necessary
 */
public class XliffScript {

    private static final String ATTR_NAME = "name";
    private static final String ATTR_STRING = "string";
    private static final String ATTR_TRANS_UNIT = "trans-unit";
    private static final String ATTR_ID = "id";
    private static final String ATTR_TARGET = "target";
    private static final String ATTR_FILE = "file";
    private static final String ATTR_TARGET_LANGUAGE = "target-language";
    private static final String ATTR_RESOURCES = "resources";
    private static final String ATTR_TRANSLATABLE = "translatable";

    private static final String PATH_SDK_DEFAULT_STRINGS = "connect-button/src/main/res/values/strings.xml";
    private static final String PATH_SDK_VALUES_LOCALE = "connect-button/src/main/res/values-";
    private static final String PATH_IFTTT_APP_DEFAULT_STRINGS = "Access/src/main/res/values/strings.xml";
    private static final String PATH_IFTTT_APP_VALUES_LOCALE = "Access/src/main/res/values-";

    private static final String PATH_XLIFF_PACKAGE = "xliffscript/src/main/java/com/script/xliffscript/IFTTTXliffFiles";
    private static final String PATH_STRINGS_ABSOLUTE = "/strings.xml";

    public static void main(String[] args)
            throws ParserConfigurationException, SAXException, IOException {
        Set<String> stringIds = getIdList(PATH_SDK_DEFAULT_STRINGS);

        List<String> files = getXLiffFileList();
        for (String fileName: files) {
            convertXliffToXml(fileName, stringIds, PATH_SDK_VALUES_LOCALE);
        }
    }

    /**
     * @returns a set of string IDs contained in the default strings.xml file within connect-button module
     */
    private static Set<String> getIdList(String filePath) {
        try {
            File inputFile = new File(filePath);
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
                    // Only translate string if translatable attribute is not set or is true
                    if ((!eElement.hasAttribute(ATTR_TRANSLATABLE) || eElement.getAttribute(ATTR_TRANSLATABLE).equals("true"))) {
                        stringIds.add(eElement.getAttribute(ATTR_NAME));
                    }
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
    private static void convertXliffToXml(String file, Set<String> stringIds, String targetDirPath)
            throws ParserConfigurationException, IOException, SAXException {
        Map<String, String> translationUnitsMap = new HashMap<>();
        File inputFile = new File(file);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();

        NodeList fileElements = doc.getElementsByTagName(ATTR_FILE);
        assert fileElements.getLength() == 1;
        String languageTag = ((Element) fileElements.item(0)).getAttribute(ATTR_TARGET_LANGUAGE);
        // Add language tag only for the SDK
        if (targetDirPath.equals(PATH_SDK_VALUES_LOCALE)) {
            translationUnitsMap.put("language_tag", languageTag);
        }

        String locale = ((Element) fileElements.item(0)).getAttribute(ATTR_TARGET_LANGUAGE)
                .replace("en-GB", "en-rGB")
                .replace("fr-CA", "fr-rCA")
                .replace("es-419", "es-rUS")
                .replace("pt-BR", "pt-rBR")
                .replace("pt-PT", "pt-rPT")
                .replace("zh-Hans", "zh-rCN")
                .replace("zh-Hant", "zh-rTW");


        NodeList nList = doc.getElementsByTagName(ATTR_TRANS_UNIT);

        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);

            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element transUnitElement = (Element) nNode;
                String id = transUnitElement.getAttribute(ATTR_ID);
                // "continue" is a reserved java keyword, but the xliff files have "continue" as the id.
                if (stringIds.contains(id) || (id.equals("continue") && stringIds.contains("term_continue"))) {
                    String translatedValue = transUnitElement.getElementsByTagName(ATTR_TARGET)
                            .item(0)
                            .getTextContent();
                    // Replace ' with \'
                    // Replace %@ with %1$s
                    String formattedString = translatedValue
                            .replace("'", "\\'")
                            .replace("%@", "%1$s");

                    translationUnitsMap.put(id.replace("continue", "term_continue"), formattedString);
                }
            }
        }

        createLocaleFile(locale, translationUnitsMap, targetDirPath);
    }

    /**
     *
     * @param locale to create the respective strings resource file
     * @param translationUnitsMap map of String ID to value to be added to the strings resource
     */
    private static void createLocaleFile(String locale, Map<String, String> translationUnitsMap, String targetDirPath) {
        String targetDirName = targetDirPath+locale;
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
     * @returns Node in the format <string name="name">value</string>
     */
    private static Node createStringElement(Document doc, String id, String value) {
        Element element = doc.createElement(ATTR_STRING);
        element.setAttribute(ATTR_NAME, id);
        element.setTextContent(value);
        return element;
    }
}

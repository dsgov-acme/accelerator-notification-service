package io.nuvalence.platform.notification.service.utils;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.XMLConstants;

/**
 * Utility class for XML operations.
 */
public class XmlUtils {

    private XmlUtils() {}

    /**
     * <p>
     * Verifies the string represents a valid XML file.
     * And returns it minified for transfer convenience.
     * </p>
     *
     * @param xmlString the XML string to be parsed
     * @return valid XML string in pretty format
     * @throws JDOMException when errors occur in parsing
     * @throws IOException   when an I/O error prevents a document from being fully
     *                       parsed
     */
    public static String xmlValidateAndMinify(String xmlString) throws JDOMException, IOException {

        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        try (var reader = new StringReader(xmlString)) {
            Document xmlDoc = saxBuilder.build(reader);
            return new XMLOutputter(Format.getCompactFormat()).outputString(xmlDoc);
        }
    }

    /**
     * <p>
     * Verifies the string represents a valid XML file.
     * And returns its representation as a Jdom2 document.
     * </p>
     * @param xmlString the XML string to be parsed
     * @return valid XML Jdom2 Document
     * @throws JDOMException when errors occur in parsing
     * @throws IOException   when an I/O error prevents a document from being fully
     *                       parsed
     */
    public static Document getXmlDocument(String xmlString) throws JDOMException, IOException {

        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        try (var reader = new StringReader(xmlString)) {
            return saxBuilder.build(reader);
        }
    }
}

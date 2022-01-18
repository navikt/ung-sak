package no.nav.k9.sak.mottak.inntektsmelding.xml;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

public final class JaxbHelper {
    private static final Map<Class<?>, JAXBContext> CONTEXTS = new ConcurrentHashMap<>(); // NOSONAR
    private static final Map<String, Schema> SCHEMAS = new ConcurrentHashMap<>(); // NOSONAR


    private JaxbHelper() {
    }

    public static <T> T unmarshalAndValidateXMLWithStAX(Class<T> clazz, String xml, String xsdLocation) throws JAXBException, XMLStreamException, SAXException {
        if (!CONTEXTS.containsKey(clazz)) {
            CONTEXTS.put(clazz, JAXBContext.newInstance(clazz));
        }

        Schema schema = null;
        if (xsdLocation != null) {
            schema = getSchema(xsdLocation);
        }

        return unmarshalAndValidateXMLWithStAXProvidingSchema(clazz, new StreamSource(new StringReader(xml)), schema);
    }

    public static <T> T unmarshalAndValidateXMLWithStAXProvidingSchema(Class<T> clazz, Source source, Schema schema)
            throws JAXBException, XMLStreamException {
        if (!CONTEXTS.containsKey(clazz)) {
            CONTEXTS.put(clazz, JAXBContext.newInstance(clazz));
        }
        Unmarshaller unmarshaller = CONTEXTS.get(clazz).createUnmarshaller();

        if (schema != null) {
            unmarshaller.setSchema(schema);
        }

        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(source);

        JAXBElement<T> root = unmarshaller.unmarshal(xmlStreamReader, clazz);

        return root.getValue();
    }

    private static Schema getSchema(String xsdLocation) throws SAXException {
        if (!SCHEMAS.containsKey(xsdLocation)) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final String systemId = JaxbHelper.class.getClassLoader().getResource(xsdLocation).toExternalForm();
            final StreamSource source = new StreamSource(systemId);
            SCHEMAS.putIfAbsent(xsdLocation, schemaFactory.newSchema(source));
        }
        return SCHEMAS.get(xsdLocation);
    }

    public static void clear() {
        CONTEXTS.clear();
        SCHEMAS.clear();
    }

    public static String retrieveNameSpaceOfXML(Source xmlSource) throws XMLStreamException {
        XMLInputFactory xmlif = XMLInputFactory.newInstance();
        XMLStreamReader xmlStreamReader = xmlif.createXMLStreamReader(xmlSource);
        while (!xmlStreamReader.isStartElement()) {
            xmlStreamReader.next();
        }
        return xmlStreamReader.getNamespaceURI();
    }

    public static String retrieveNameSpaceOfXML(String xml) throws XMLStreamException {
        try (final StringReader reader = new StringReader(xml)) {
            return retrieveNameSpaceOfXML(new StreamSource(reader));
        }
    }
}

/*
 * MetaObjectSAPIR.java
 *
 * Created on 2. duben 2007, 10:34
 *
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinaryOutputStream;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author xbatko
 */
public class MetaObjectSAPIR extends MetaObject implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;
        
    /****************** Constructors ******************/

    /** Creates a new instance of MetaObjectSAPIR */
    public MetaObjectSAPIR(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        super(locatorURI, objects, cloneObjects);
    }

    /** Creates a new instance of MetaObjectSAPIR */
    public MetaObjectSAPIR(String locatorURI, Map<String, LocalAbstractObject> objects) {
        super(locatorURI, objects);
    }

    /** Creates a new instance of MetaObjectSAPIR */
    public MetaObjectSAPIR(BufferedReader stream) throws IOException {
        super(stream);
    }

    /** Creates a new instance of MetaObjectSAPIR */
    public MetaObjectSAPIR(BufferedReader stream, Set<String> restrictedNames) throws IOException {
        super(stream, restrictedNames);
    }

    /**
     * Returns list of supported visual descriptor types that this object recognizes in XML.
     * @return list of supported visual descriptor types
     */
    public static List<String> getSupportedVisualDescriptorTypes() {
        List<String> rtv = new ArrayList<String>();
        for (Method method : XMLHandlerSAPIR.class.getMethods()) {
            Class[] methodPrototype = method.getParameterTypes();
            if (method.getName().startsWith("new") && methodPrototype.length == 1 && Map.class.equals(methodPrototype[0]))
                rtv.add(method.getName().substring(3));
        }
        return rtv;
    }

    /****************** XML parsing ******************/

    /** Factory method that creates MetaObjects from SAPIR XML files */
    public static MetaObjectSAPIR create(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(xmlFile, xmlHandler);
        return new MetaObjectSAPIR(xmlHandler.getLocatorURI(), xmlHandler.getObjects());
    }

    /** Factory method that creates MetaObjects from SAPIR XML files retrieved from the passed URI */
    public static MetaObjectSAPIRWeightedDist create(String uri) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(uri, xmlHandler);
        return new MetaObjectSAPIRWeightedDist(xmlHandler.getLocatorURI(), xmlHandler.getObjects());
    }

    /** Factory method that creates MetaObjects from SAPIR XML files retrieved from the passed InputStream */
    public static MetaObjectSAPIRWeightedDist create(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(is, xmlHandler);
        return new MetaObjectSAPIRWeightedDist(xmlHandler.getLocatorURI(), xmlHandler.getObjects());
    }

    public String getObjectsXML() {
        StringBuffer rtv = new StringBuffer();
        for (String name : objects.keySet())
            XMLHandlerSAPIR.appendObjectXML(rtv, name, objects);
        return rtv.toString();
    }

    public String getObjectXML(String name) throws NoSuchElementException {
        StringBuffer rtv = new StringBuffer();
        XMLHandlerSAPIR.appendObjectXML(rtv, name, objects);
        return rtv.toString();
    }

    /** Internal class that parses SAPIR XML */
    public static class XMLHandlerSAPIR extends DefaultHandler {

        /****************** Constants ******************/

        protected static final String descriptorTagName = "VisualDescriptor";

        protected static final String descriptorTypeAttributeName = "type";
        
        /****************** Attributes ******************/

        /** List of parsed local abstract objects */
        protected final Map<String, LocalAbstractObject> objects = new HashMap<String, LocalAbstractObject>();
        /** Parsed locator URI */
        protected String locatorURI = null;

        public Map<String, LocalAbstractObject> getObjects() {
            return objects;
        }

        public int getObjectCount() {
            return objects.size();
        }

        public String getLocatorURI() {
            return locatorURI;
        }

        public void resetObjects() {
            objects.clear();
            locatorURI = null;
        }

        /****************** Constructors ******************/

        /** Creates new instance of XMLHandlerSAPIR */
        public XMLHandlerSAPIR() {
        }

        /****************** Parsing methods ******************/

        protected Stack<String> elementNamesStack = new Stack<String>();
        protected Map<String, String> descriptorData = new HashMap<String, String>();
        protected String descriptorName = null;

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            String name = (localName.length() == 0)?qName:localName;
            elementNamesStack.push(name);

            // If this is a descriptor node and we are not inside one already
            if (descriptorName == null && name.equals(descriptorTagName)) // Ignore additional stack checks: && elementNamesStack.size() >= 2 && elementNamesStack.get(elementNamesStack.size() - 2).equals("Image"))
                descriptorName = attributes.getValue(descriptorTypeAttributeName);
            else if (name.equals("location")) { // HACK! for location
                try {
                    float latitude = Float.parseFloat(attributes.getValue("latitude"));
                    float longitude = Float.parseFloat(attributes.getValue("longitude"));
                    objects.put("Location", new ObjectGPSCoordinate(latitude, longitude));
                } catch (Exception e) {
                    throw new SAXException("Error parsing descriptor 'Location' at " + elementNamesStack.toString(), e);
                }
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            String name = elementNamesStack.pop();
            if (localName.length() == 0) {
                if (!qName.equals(name))
                    throw new SAXException("Closing element '" + qName + "', but should close '" + name + "'");
            } else {
                if (!localName.equals(name))
                    throw new SAXException("Closing element '" + localName + "', but should close '" + name + "'");
            }

            // If this is an end of descriptor node
            if (descriptorName != null && name.equals(descriptorTagName)) { // Ignore additional stack checks: && elementNamesStack.size() >= 1 && elementNamesStack.peek().equals("Image")) {
                try {
                    objects.put(descriptorName, (LocalAbstractObject)XMLHandlerSAPIR.class.getMethod("new" + descriptorName, Map.class).invoke(this, descriptorData));
                } catch (InvocationTargetException e) {
                    throw new SAXException("Error parsing descriptor '" + descriptorName + "' at " + elementNamesStack.toString(), e);
                } catch (IllegalArgumentException thisShouldNeverHappen) {
                    thisShouldNeverHappen.printStackTrace();
                } catch (IllegalAccessException thisShouldNeverHappen) {
                    thisShouldNeverHappen.printStackTrace();
                } catch (NoSuchMethodException ignore) {
                }
                
                descriptorName = null;
                descriptorData.clear();
            }
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            // If inside descriptor
            if (descriptorName != null) {
                String tagName = elementNamesStack.peek();
                // Get previous value of this tag
                String value = descriptorData.get(tagName);
                // Prepare new value and put it under the name of the current element (append to the previous value)
                if (value == null)
                    value = new String(ch, start, length);
                else value += new String(ch, start, length);
                descriptorData.put(tagName, value);
            
            // If this is a locator node
            } else if (elementNamesStack.size() >= 3 && elementNamesStack.peek().equals("MediaUri")) {
                locatorURI = new String(ch, start, length);
            }
        }


        /****************** XML builder methods ******************/

        public String getObjectsXML() {
            StringBuffer rtv = new StringBuffer();
            for (String name : objects.keySet())
                appendObjectXML(rtv, name, objects);
            return rtv.toString();
        }

        public String getObjectXML(String name) throws NoSuchElementException {
            StringBuffer rtv = new StringBuffer();
            appendObjectXML(rtv, name, objects);
            return rtv.toString();
        }

        protected static StringBuffer appendObjectXML(StringBuffer xmlString, String name, Map<String, LocalAbstractObject> objects) throws NoSuchElementException {
            // Get object for the specified name
            LocalAbstractObject object = objects.get(name);
            if (object == null)
                throw new NoSuchElementException("There is no object for descriptor '" + name + "'");

            // Append opening descriptor tag
            xmlString.append('<');
            xmlString.append(descriptorTagName);
            xmlString.append(' ').append(descriptorTypeAttributeName).append("=\"").append(name).append('"');  // Descriptor type attribute
            xmlString.append('>');
            try {
                //objects.put(descriptorName, (LocalAbstractObject)getClass().getMethod("new" + descriptorName, Map.class).invoke(this, descriptorData));
                XMLHandlerSAPIR.class.getMethod("xmlFrom" + name, StringBuffer.class, LocalAbstractObject.class).invoke(null, xmlString, object);
            } catch (IllegalArgumentException thisShouldNeverHappen) {
                thisShouldNeverHappen.printStackTrace();
                throw new NoSuchElementException(thisShouldNeverHappen.toString());
            } catch (IllegalAccessException thisShouldNeverHappen) {
                thisShouldNeverHappen.printStackTrace();
                throw new NoSuchElementException(thisShouldNeverHappen.toString());
            } catch (InvocationTargetException e) {
                throw new NoSuchElementException(e.getCause().toString());
            } catch (NoSuchMethodException ex) {
                throw new NoSuchElementException("There is no XML writer method for descriptor '" + name + "'");
            }

            // Append closing descriptor tag
            xmlString.append("</").append(descriptorTagName).append('>');

            // Return xml string to allow chaining
            return xmlString;
        }

        protected static StringBuffer appendArrayXML(StringBuffer xmlString, String tagName, Object array, int fromIndex, int toIndex, String separator) {
            // Ignore null arrays
            if (array == null)
                return xmlString;

            // Check size constraints to avoid IndexOutOfBounds exceptions
            if (fromIndex <= 0)
                fromIndex = 0;
            int lastIndex = Array.getLength(array) - 1;
            if (toIndex < 0 || toIndex > lastIndex)
                toIndex = lastIndex;

            // Ignore empty arrays
            if (toIndex < 0)
                return xmlString;

            xmlString.append('<').append(tagName).append('>');
            
            while (fromIndex <= toIndex) {
                xmlString.append(Array.get(array, fromIndex));
                if (fromIndex != toIndex)
                    xmlString.append(separator);
                fromIndex++;
            }

            xmlString.append("</").append(tagName).append('>');
            
            // Return xml string to allow chaining
            return xmlString;
        }

        /****************** Specific object descriptor creators ******************/

        ///////////////// Scalable Color Type /////////////////

        public LocalAbstractObject newScalableColorType(Map<String, String> data) {
            String[] numbers = data.get("Coeff").split("\\p{Space}+");
            int[] parsedData = new int[numbers.length];
            for (int i = 0; i < numbers.length; i++)
                parsedData[i] = Integer.parseInt(numbers[i]);
            return new ObjectIntVectorL1(parsedData);
        }

        public static StringBuffer xmlFromScalableColorType(StringBuffer xmlString, LocalAbstractObject object) {
            return appendArrayXML(xmlString, "Coeff", ((ObjectIntVectorL1)object).data, 0, -1, " ");
        }

        ///////////////// Color Structure Type /////////////////

        public LocalAbstractObject newColorStructureType(Map<String, String> data) {
            String[] numbers = data.get("Values").split("\\p{Space}+");
            short[] parsedData = new short[numbers.length];
            for (int i = 0; i < numbers.length; i++)
                parsedData[i] = Short.parseShort(numbers[i]);
            return new ObjectShortVectorL1(parsedData);
        }

        public static StringBuffer xmlFromColorStructureType(StringBuffer xmlString, LocalAbstractObject object) {
            return appendArrayXML(xmlString, "Values", ((ObjectShortVectorL1)object).data, 0, -1, " ");
        }

        ///////////////// Edge Histogram Type /////////////////

        public LocalAbstractObject newEdgeHistogramType(Map<String, String> data) {
            String[] numbers = data.get("BinCounts").split("\\p{Space}+");
            byte[] parsedData = new byte[numbers.length];
            for (int i = 0; i < numbers.length; i++)
                parsedData[i] = Byte.parseByte(numbers[i]);
            return new ObjectVectorEdgecomp(parsedData);
        }

        public static StringBuffer xmlFromEdgeHistogramType(StringBuffer xmlString, LocalAbstractObject object) {
            return appendArrayXML(xmlString, "BinCounts", ((ObjectVectorEdgecomp)object).data, 0, -1, " ");
        }

        ///////////////// Homogeneous Texture Type /////////////////

        public LocalAbstractObject newHomogeneousTextureType(Map<String, String> data) {
            // Parse energy values
            String[] numbers = data.get("Energy").split("\\p{Space}+");
            short[] energy = new short[numbers.length];
            for (int i = 0; i < numbers.length; i++)
                energy[i] = Short.parseShort(numbers[i]);
            short[] energyDeviation;
            // Parse energy deviation values
            if (data.containsKey("EnergyDeviation")) {
                numbers = data.get("EnergyDeviation").split("\\p{Space}+");
                energyDeviation = new short[numbers.length];
                for (int i = 0; i < numbers.length; i++)
                    energyDeviation[i] = Short.parseShort(numbers[i]);
            } else energyDeviation = null;

            return new ObjectHomogeneousTexture(Short.parseShort(data.get("Average")), Short.parseShort(data.get("StandardDeviation")), energy, energyDeviation);
        }

        public static StringBuffer xmlFromHomogeneousTextureType(StringBuffer xmlString, LocalAbstractObject object) {
            xmlString.append("<Average>").append(((ObjectHomogeneousTexture)object).average).append("</Average>");
            xmlString.append("<StandardDeviation>").append(((ObjectHomogeneousTexture)object).standardDeviation).append("</StandardDeviation>");
            appendArrayXML(xmlString, "Energy", ((ObjectHomogeneousTexture)object).energy, 0, -1, " ");
            appendArrayXML(xmlString, "EnergyDeviation", ((ObjectHomogeneousTexture)object).energyDeviation, 0, -1, " ");

            // Return xml string to allow chaining
            return xmlString;
        }

        ///////////////// Color Layout Type /////////////////

        public LocalAbstractObject newColorLayoutType(Map<String, String> data) {
            // Parse Y coeffs
            String[] numbers = data.get("YACCoeff5").split("\\p{Space}+");
            byte[] YCoeff = new byte[numbers.length + 1];
            YCoeff[0] = Byte.parseByte(data.get("YDCCoeff"));
            for (int i = 1; i <= numbers.length; i++)
                YCoeff[i] = Byte.parseByte(numbers[i - 1]);
            // Parse Cb coeffs
            numbers = data.get("CbACCoeff2").split("\\p{Space}+");
            byte[] CbCoeff = new byte[numbers.length + 1];
            CbCoeff[0] = Byte.parseByte(data.get("CbDCCoeff"));
            for (int i = 1; i <= numbers.length; i++)
                CbCoeff[i] = Byte.parseByte(numbers[i - 1]);
            // Parse Cr coeffs
            numbers = data.get("CrACCoeff2").split("\\p{Space}+");
            byte[] CrCoeff = new byte[numbers.length + 1];
            CrCoeff[0] = Byte.parseByte(data.get("CrDCCoeff"));
            for (int i = 1; i <= numbers.length; i++)
                CrCoeff[i] = Byte.parseByte(numbers[i - 1]);
            return new ObjectColorLayout(YCoeff, CbCoeff, CrCoeff);
        }

        public static StringBuffer xmlFromColorLayoutType(StringBuffer xmlString, LocalAbstractObject object) {
            // Get coefficients data from scalable color object
            byte[] YCoeff = ((ObjectColorLayout)object).YCoeff;
            byte[] CbCoeff = ((ObjectColorLayout)object).CbCoeff;
            byte[] CrCoeff = ((ObjectColorLayout)object).CrCoeff;

            xmlString.append("<YDCCoeff>").append(YCoeff[0]).append("</YDCCoeff>");
            xmlString.append("<CbDCCoeff>").append(CbCoeff[0]).append("</CbDCCoeff>");
            xmlString.append("<CrDCCoeff>").append(CrCoeff[0]).append("</CrDCCoeff>");
            appendArrayXML(xmlString, "YACCoeff5", YCoeff, 1, -1, " ");
            appendArrayXML(xmlString, "CbACCoeff2", CbCoeff, 1, -1, " ");
            appendArrayXML(xmlString, "CrACCoeff2", CrCoeff, 1, -1, " ");

            // Return xml string to allow chaining
            return xmlString;
        }

    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObjectSAPIR loaded from binary input stream.
     * 
     * @param input the stream to read the MetaObjectSAPIR from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the stream
     */
    protected MetaObjectSAPIR(BinaryInputStream input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the output stream this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    @Override
    public int binarySerialize(BinaryOutputStream output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator);
    }

}

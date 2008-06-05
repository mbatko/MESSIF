/*
 * MetaObjectSAPIR.java
 *
 * Created on 2. duben 2007, 10:34
 *
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Stack;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import messif.objects.AbstractObjectKey;
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

    //****************** The list of supported names ******************//

    /** The list of the names for the encapsulated objects */
    protected static final String[] descriptorNames = {"ColorLayoutType","ColorStructureType","EdgeHistogramType","HomogeneousTextureType","ScalableColorType","Location"};

    //****************** Attributes ******************//

    /** Object for the ColorLayoutType */
    protected ObjectColorLayout colorLayout;
    /** Object for the ColorStructureType */
    protected ObjectShortVectorL1 colorStructure;
    /** Object for the EdgeHistogramType */
    protected ObjectVectorEdgecomp edgeHistogram;
    /** Object for the HomogeneousTextureType */
    protected ObjectHomogeneousTexture homogeneousTexture;
    /** Object for the ScalableColorType */
    protected ObjectIntVectorL1 scalableColor;
    /** Object for the Location */
    protected ObjectGPSCoordinate location;


    /****************** Constructors ******************/

    /** Creates a new instance of MetaObjectSAPIR */
    public MetaObjectSAPIR(String locatorURI, ObjectColorLayout colorLayout, ObjectShortVectorL1 colorStructure, ObjectVectorEdgecomp edgeHistogram, ObjectHomogeneousTexture homogeneousTexture, ObjectIntVectorL1 scalableColor, ObjectGPSCoordinate location) {
        super(locatorURI);
        this.colorLayout = colorLayout;
        this.colorStructure = colorStructure;
        this.edgeHistogram = edgeHistogram;
        this.homogeneousTexture = homogeneousTexture;
        this.scalableColor = scalableColor;
        this.location = location;
    }

    /** Creates a new instance of MetaObjectSAPIR */
    public MetaObjectSAPIR(BufferedReader stream) throws IOException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line;
        do {
            line = stream.readLine();
            if (line == null)
                throw new EOFException("EoF reached while initializing MetaObject.");
        } while (processObjectComment(line));

        // The line should have format "URI;name1;class1;name2;class2;..." and URI can be skipped (including the semicolon)
        String[] uriNamesClasses = line.split(";");

        // Skip the first name if the number of elements is odd
        int i = uriNamesClasses.length % 2;

        // If the URI locator is used (and it is not set from the previous - this is the old format)
        if (i == 1) {
            if ((this.objectKey == null) && (uriNamesClasses[0].length() > 0))
                    this.objectKey = new AbstractObjectKey(uriNamesClasses[0]);
        }

        for (; i < uriNamesClasses.length; i += 2) {
            if ("ColorLayoutType".equals(uriNamesClasses[i]))
                colorLayout = readObject(stream, ObjectColorLayout.class);
            else if ("ColorStructureType".equals(uriNamesClasses[i]))
                colorStructure = readObject(stream, ObjectShortVectorL1.class);
            else if ("EdgeHistogramType".equals(uriNamesClasses[i]))
                edgeHistogram = readObject(stream, ObjectVectorEdgecomp.class);
            else if ("HomogeneousTextureType".equals(uriNamesClasses[i]))
                homogeneousTexture = readObject(stream, ObjectHomogeneousTexture.class);
            else if ("ScalableColorType".equals(uriNamesClasses[i]))
                scalableColor = readObject(stream, ObjectIntVectorL1.class);
            else if ("Location".equals(uriNamesClasses[i]))
                location = readObject(stream, ObjectGPSCoordinate.class);
        }
    }

    /**
     * Returns list of supported visual descriptor types that this object recognizes in XML.
     * @return list of supported visual descriptor types
     */
    public static String[] getSupportedVisualDescriptorTypes() {
        return descriptorNames;
    }


    /****************** MetaObject overrides ******************/

    /**
     * Returns the number of encapsulated objects.
     * @return the number of encapsulated objects
     */
    @Override
    public int getObjectCount() {
        return 6;
    }

    /**
     * Returns the encapsulated object for given symbolic name.
     *
     * @param name the symbolic name of the object to return
     * @return encapsulated object for given name or <tt>null</tt> if the key is unknown
     */
    @Override
    public LocalAbstractObject getObject(String name) {
        if ("ColorLayoutType".equals(name))
            return colorLayout;
        else if ("ColorStructureType".equals(name))
            return colorStructure;
        else if ("EdgeHistogramType".equals(name))
            return edgeHistogram;
        else if ("HomogeneousTextureType".equals(name))
            return homogeneousTexture;
        else if ("ScalableColorType".equals(name))
            return scalableColor;
        else if ("Location".equals(name))
            return location;
        else
            return null;
    }

    /**
     * Returns the set of symbolic names of the encapsulated objects.
     * @return the set of symbolic names of the encapsulated objects
     */
    @Override
    public Collection<String> getObjectNames() {
        return Arrays.asList(descriptorNames);
    }

    /**
     * Returns a collection of all the encapsulated objects associated with their symbolic names.
     * Note that the collection can contain <tt>null</tt> values.
     * @return a map with symbolic names as keyas and the respective encapsulated objects as values
     */
    public Map<String, LocalAbstractObject> getObjectMap() {
        Map<String, LocalAbstractObject> map = new HashMap<String, LocalAbstractObject>(6);
        if (colorLayout != null)
            map.put("ColorLayoutType", colorLayout);
        if (colorStructure != null)
            map.put("ColorStructureType", colorStructure);
        if (edgeHistogram != null)
            map.put("EdgeHistogramType", edgeHistogram);
        if (homogeneousTexture != null)
            map.put("HomogeneousTextureType", homogeneousTexture);
        if (scalableColor != null)
            map.put("ScalableColorType", scalableColor);
        if (location != null)
            map.put("Location", location);
        return map;
    }


    /****************** Clonning ******************/

    /**
     * Creates and returns a copy of this object. The precise meaning 
     * of "copy" may depend on the class of the object.
     * @param cloneFilterChain  the flag wheter the filter chain must be cloned as well.
     * @return a clone of this instance.
     * @throws CloneNotSupportedException if the object's class does not support clonning or there was an error
     */
    @Override
    public LocalAbstractObject clone(boolean cloneFilterChain) throws CloneNotSupportedException {
        MetaObjectSAPIR rtv = (MetaObjectSAPIR)super.clone(cloneFilterChain);
        if (colorLayout != null)
            rtv.colorLayout = (ObjectColorLayout)colorLayout.clone(cloneFilterChain);
        if (colorStructure != null)
            rtv.colorStructure = (ObjectShortVectorL1)colorStructure.clone(cloneFilterChain);
        if (edgeHistogram != null)
            rtv.edgeHistogram = (ObjectVectorEdgecomp)edgeHistogram.clone(cloneFilterChain);
        if (homogeneousTexture != null)
            rtv.homogeneousTexture = (ObjectHomogeneousTexture)homogeneousTexture.clone(cloneFilterChain);
        if (scalableColor != null)
            rtv.scalableColor = (ObjectIntVectorL1)scalableColor.clone(cloneFilterChain);
        if (location != null)
            rtv.location = (ObjectGPSCoordinate)location.clone(cloneFilterChain);

        return rtv;
    }

    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        MetaObjectSAPIR rtv = (MetaObjectSAPIR)super.clone(true);
        if (colorLayout != null)
            rtv.colorLayout = (ObjectColorLayout)colorLayout.cloneRandomlyModify(args);
        if (colorStructure != null)
            rtv.colorStructure = (ObjectShortVectorL1)colorStructure.cloneRandomlyModify(args);
        if (edgeHistogram != null)
            rtv.edgeHistogram = (ObjectVectorEdgecomp)edgeHistogram.cloneRandomlyModify(args);
        if (homogeneousTexture != null)
            rtv.homogeneousTexture = (ObjectHomogeneousTexture)homogeneousTexture.cloneRandomlyModify(args);
        if (scalableColor != null)
            rtv.scalableColor = (ObjectIntVectorL1)scalableColor.cloneRandomlyModify(args);
        if (location != null)
            rtv.location = (ObjectGPSCoordinate)location.cloneRandomlyModify(args);
        return rtv;
    }

    /**
     * Store this object to a text stream.
     * This method should have the opposite deserialization in constructor of a given object class.
     *
     * @param stream the stream to store this object to
     * @throws IOException if there was an error while writing to stream
     */
    protected void writeData(OutputStream stream) throws IOException {
        if (colorLayout != null)
            stream.write("ColorLayoutType;messif.objects.impl.ObjectColorLayout;".getBytes());
        if (colorStructure != null)
            stream.write("ColorStructureType;messif.objects.impl.ObjectShortVectorL1;".getBytes());
        if (edgeHistogram != null)
            stream.write("EdgeHistogramType;messif.objects.impl.ObjectVectorEdgecomp;".getBytes());
        if (homogeneousTexture != null)
            stream.write("HomogeneousTextureType;messif.objects.impl.ObjectHomogeneousTexture;".getBytes());
        if (scalableColor != null)
            stream.write("ScalableColorType;messif.objects.impl.ObjectIntVectorL1;".getBytes());
        if (location != null)
            stream.write("Location;messif.objects.impl.ObjectGPSCoordinate;".getBytes());

        // Write a line for every object from the list (skip the comments)
        if (colorLayout != null)
            colorLayout.writeData(stream);
        if (colorStructure != null)
            colorStructure.writeData(stream);
        if (edgeHistogram != null)
            edgeHistogram.writeData(stream);
        if (homogeneousTexture != null)
            homogeneousTexture.writeData(stream);
        if (scalableColor != null)
            scalableColor.writeData(stream);
        if (location != null)
            location.writeData(stream);
    }

    /****************** XML parsing ******************/

    /** Factory method that creates MetaObjects from SAPIR XML files */
    public static MetaObjectSAPIR create(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(xmlFile, xmlHandler);
        Map<String, LocalAbstractObject> objects = xmlHandler.getObjects();
        return new MetaObjectSAPIR(xmlHandler.getLocatorURI(), 
                (ObjectColorLayout)objects.get("ColorLayoutType"),
                (ObjectShortVectorL1)objects.get("ColorStructureType"),
                (ObjectVectorEdgecomp)objects.get("EdgeHistogramType"),
                (ObjectHomogeneousTexture)objects.get("HomogeneousTextureType"),
                (ObjectIntVectorL1)objects.get("ScalableColorType"),
                (ObjectGPSCoordinate)objects.get("Location")
        );
    }

    /** Factory method that creates MetaObjects from SAPIR XML files retrieved from the passed URI */
    public static MetaObjectSAPIR create(String uri) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(uri, xmlHandler);
        Map<String, LocalAbstractObject> objects = xmlHandler.getObjects();
        return new MetaObjectSAPIR(xmlHandler.getLocatorURI(),
                (ObjectColorLayout)objects.get("ColorLayoutType"),
                (ObjectShortVectorL1)objects.get("ColorStructureType"),
                (ObjectVectorEdgecomp)objects.get("EdgeHistogramType"),
                (ObjectHomogeneousTexture)objects.get("HomogeneousTextureType"),
                (ObjectIntVectorL1)objects.get("ScalableColorType"),
                (ObjectGPSCoordinate)objects.get("Location")
        );
    }

    /** Factory method that creates MetaObjects from SAPIR XML files retrieved from the passed InputStream */
    public static MetaObjectSAPIR create(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        XMLHandlerSAPIR xmlHandler = new XMLHandlerSAPIR();
        SAXParserFactory.newInstance().newSAXParser().parse(is, xmlHandler);
        Map<String, LocalAbstractObject> objects = xmlHandler.getObjects();
        return new MetaObjectSAPIR(xmlHandler.getLocatorURI(),
                (ObjectColorLayout)objects.get("ColorLayoutType"),
                (ObjectShortVectorL1)objects.get("ColorStructureType"),
                (ObjectVectorEdgecomp)objects.get("EdgeHistogramType"),
                (ObjectHomogeneousTexture)objects.get("HomogeneousTextureType"),
                (ObjectIntVectorL1)objects.get("ScalableColorType"),
                (ObjectGPSCoordinate)objects.get("Location")
        );
    }

    public String getObjectsXML() {
        StringBuffer rtv = new StringBuffer();
        if (colorLayout != null)
            XMLHandlerSAPIR.appendObjectXML(rtv, "ColorLayoutType", colorLayout);
        if (colorStructure != null)
            XMLHandlerSAPIR.appendObjectXML(rtv, "ColorStructureType", colorStructure);
        if (edgeHistogram != null)
            XMLHandlerSAPIR.appendObjectXML(rtv, "EdgeHistogramType", edgeHistogram);
        if (homogeneousTexture != null)
            XMLHandlerSAPIR.appendObjectXML(rtv, "HomogeneousTextureType", homogeneousTexture);
        if (scalableColor != null)
            XMLHandlerSAPIR.appendObjectXML(rtv, "ScalableColorType", scalableColor);
        if (location != null)
            XMLHandlerSAPIR.appendObjectXML(rtv, "Location", location);
        return rtv.toString();
    }

    public String getObjectXML(String name) throws NoSuchElementException {
        StringBuffer rtv = new StringBuffer();
        XMLHandlerSAPIR.appendObjectXML(rtv, name, getObject(name));
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
            for (Entry<String, LocalAbstractObject> entry : objects.entrySet())
                appendObjectXML(rtv, entry.getKey(), entry.getValue());
            return rtv.toString();
        }

        public String getObjectXML(String name) throws NoSuchElementException {
            StringBuffer rtv = new StringBuffer();
            appendObjectXML(rtv, name, objects.get(name));
            return rtv.toString();
        }

        protected static StringBuffer appendObjectXML(StringBuffer xmlString, String name, LocalAbstractObject object) throws NoSuchElementException {
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
        colorLayout = serializator.readObject(input, ObjectColorLayout.class);
        colorStructure = serializator.readObject(input, ObjectShortVectorL1.class);
        edgeHistogram = serializator.readObject(input, ObjectVectorEdgecomp.class);
        homogeneousTexture = serializator.readObject(input, ObjectHomogeneousTexture.class);
        scalableColor = serializator.readObject(input, ObjectIntVectorL1.class);
        location = serializator.readObject(input, ObjectGPSCoordinate.class);
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
        int size = super.binarySerialize(output, serializator);
        size += serializator.write(output, colorLayout);
        size += serializator.write(output, colorStructure);
        size += serializator.write(output, edgeHistogram);
        size += serializator.write(output, homogeneousTexture);
        size += serializator.write(output, scalableColor);
        size += serializator.write(output, location);
        return size;
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator);
        size += serializator.getBinarySize(colorLayout);
        size += serializator.getBinarySize(colorStructure);
        size += serializator.getBinarySize(edgeHistogram);
        size += serializator.getBinarySize(homogeneousTexture);
        size += serializator.getBinarySize(scalableColor);
        size += serializator.getBinarySize(location);
        return size;
    }

}

/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class MetaObjectSAPIR extends MetaObject implements BinarySerializable {

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

    public MetaObjectSAPIR(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        this(locatorURI, objects);
        if (cloneObjects) {
            this.colorLayout = (ObjectColorLayout)this.colorLayout.clone(getObjectKey());
            this.colorStructure = (ObjectShortVectorL1)this.colorStructure.clone(getObjectKey());
            this.edgeHistogram = (ObjectVectorEdgecomp)this.edgeHistogram.clone(getObjectKey());
            this.homogeneousTexture = (ObjectHomogeneousTexture)this.homogeneousTexture.clone(getObjectKey());
            this.scalableColor = (ObjectIntVectorL1)this.scalableColor.clone(getObjectKey());
            this.location = (ObjectGPSCoordinate)this.location.clone(getObjectKey());
        }
    }

    public MetaObjectSAPIR(String locatorURI, Map<String, LocalAbstractObject> objects) {
        super(locatorURI);
        this.colorLayout = (ObjectColorLayout)objects.get("ColorLayoutType");
        this.colorStructure = (ObjectShortVectorL1)objects.get("ColorStructureType");
        this.edgeHistogram = (ObjectVectorEdgecomp)objects.get("EdgeHistogramType");
        this.homogeneousTexture = (ObjectHomogeneousTexture)objects.get("HomogeneousTextureType");
        this.scalableColor = (ObjectIntVectorL1)objects.get("ScalableColorType");
        this.location = (ObjectGPSCoordinate)objects.get("Location");
    }

    /** Creates a new instance of MetaObjectSAPIR */
    public MetaObjectSAPIR(BufferedReader stream, Set<String> restrictNames) throws IOException {
        Map<String, LocalAbstractObject> objects = readObjects(stream, restrictNames, readObjectsHeader(stream), new HashMap<String, LocalAbstractObject>());
        this.colorLayout = (ObjectColorLayout)objects.get("ColorLayoutType");
        this.colorStructure = (ObjectShortVectorL1)objects.get("ColorStructureType");
        this.edgeHistogram = (ObjectVectorEdgecomp)objects.get("EdgeHistogramType");
        this.homogeneousTexture = (ObjectHomogeneousTexture)objects.get("HomogeneousTextureType");
        this.scalableColor = (ObjectIntVectorL1)objects.get("ScalableColorType");
        this.location = (ObjectGPSCoordinate)objects.get("Location");
    }

    /** Creates a new instance of MetaObjectSAPIR */
    public MetaObjectSAPIR(BufferedReader stream, String[] restrictNames) throws IOException {
        this(stream, new HashSet<String>(Arrays.asList(restrictNames)));
    }

    /** Creates a new instance of MetaObjectSAPIR */
    public MetaObjectSAPIR(BufferedReader stream) throws IOException {
        this(stream, (Set<String>)null);
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
        int count = 0;
        if (colorLayout != null)
            count++;
        if (colorStructure != null)
            count++;
        if (edgeHistogram != null)
            count++;
        if (homogeneousTexture != null)
            count++;
        if (scalableColor != null)
            count++;
        if (location != null)
            count++;
        return count;
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

    @Override
    public Collection<LocalAbstractObject> getObjects() {
        Collection<LocalAbstractObject> objects = new ArrayList<LocalAbstractObject>(6);
        if (colorLayout != null)
            objects.add(colorLayout);
        if (colorStructure != null)
            objects.add(colorStructure);
        if (edgeHistogram != null)
            objects.add(edgeHistogram);
        if (homogeneousTexture != null)
            objects.add(homogeneousTexture);
        if (scalableColor != null)
            objects.add(scalableColor);
        if (location != null)
            objects.add(location);
        return objects;
    }

    @Override
    public Collection<String> getObjectNames() {
        Collection<String> names = new ArrayList<String>(6);
        if (colorLayout != null)
            names.add(descriptorNames[0]);
        if (colorStructure != null)
            names.add(descriptorNames[1]);
        if (edgeHistogram != null)
            names.add(descriptorNames[2]);
        if (homogeneousTexture != null)
            names.add(descriptorNames[3]);
        if (scalableColor != null)
            names.add(descriptorNames[4]);
        if (location != null)
            names.add(descriptorNames[5]);
        return names;
    }

    /**
     * Returns a collection of all the encapsulated objects associated with their symbolic names.
     * Note that the collection can contain <tt>null</tt> values.
     * @return a map with symbolic names as keys and the respective encapsulated objects as values
     */
    @Override
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


    //****************** Cloning ******************//

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
    @Override
    protected void writeData(OutputStream stream) throws IOException {
        boolean written = false;
        if (colorLayout != null) {
            stream.write("ColorLayoutType;messif.objects.impl.ObjectColorLayout".getBytes());
            written = true;
        }
        if (colorStructure != null) {
            if (written)
                stream.write(';');
            stream.write("ColorStructureType;messif.objects.impl.ObjectShortVectorL1".getBytes());
            written = true;
        }
        if (edgeHistogram != null) {
            if (written)
                stream.write(';');
            stream.write("EdgeHistogramType;messif.objects.impl.ObjectVectorEdgecomp".getBytes());
            written = true;
        }
        if (homogeneousTexture != null) {
            if (written)
                stream.write(';');
            stream.write("HomogeneousTextureType;messif.objects.impl.ObjectHomogeneousTexture".getBytes());
            written = true;
        }
        if (scalableColor != null) {
            if (written)
                stream.write(';');
            stream.write("ScalableColorType;messif.objects.impl.ObjectIntVectorL1".getBytes());
            written = true;
        }
        if (location != null) {
            if (written)
                stream.write(';');
            stream.write("Location;messif.objects.impl.ObjectGPSCoordinate".getBytes());
            written = true;
        }
        if (written) {
            stream.write('\n');
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
    }

    //****************** XML parsing ******************//

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

        @Override
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

        @Override
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
                    throw new SAXException("Error parsing descriptor '" + descriptorName + "' at " + elementNamesStack.toString(), (Exception)e.getCause());
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

        @Override
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

        public static StringBuffer appendObjectXML(StringBuffer xmlString, String name, LocalAbstractObject object) throws NoSuchElementException {
            // Hack for location
            if (name.equals("Location")) {
                ObjectGPSCoordinate location = (ObjectGPSCoordinate)object;
                xmlString.append("<location latitude=\"").append(location.getLatitude());
                xmlString.append("\" longitude=\"").append(location.getLongitude());
                xmlString.append("\"/>");
                return xmlString;
            }

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
     * Creates a new instance of MetaObjectSAPIR loaded from binary input buffer.
     * 
     * @param input the buffer to read the MetaObjectSAPIR from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectSAPIR(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        colorLayout = serializator.readObject(input, ObjectColorLayout.class);
        colorStructure = serializator.readObject(input, ObjectShortVectorL1.class);
        edgeHistogram = serializator.readObject(input, ObjectVectorEdgecomp.class);
        homogeneousTexture = serializator.readObject(input, ObjectHomogeneousTexture.class);
        scalableColor = serializator.readObject(input, ObjectIntVectorL1.class);
        location = serializator.readObject(input, ObjectGPSCoordinate.class);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        size += serializator.write(output, colorLayout);
        size += serializator.write(output, colorStructure);
        size += serializator.write(output, edgeHistogram);
        size += serializator.write(output, homogeneousTexture);
        size += serializator.write(output, scalableColor);
        size += serializator.write(output, location);
        return size;
    }

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

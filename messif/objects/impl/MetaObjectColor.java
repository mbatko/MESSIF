
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author xbatko
 */
public class MetaObjectColor extends MetaObject implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** The list of supported names ******************//

    /** The list of the names for the encapsulated objects */
    protected static final String[] descriptorNames = {"ColorLayoutType","ColorStructureType","ScalableColorType"};

    /** The list of the names for the encapsulated objects - in the form of a set */
    protected static final Set<String> descriptorNameSet = new HashSet<String>(Arrays.asList("ColorLayoutType","ColorStructureType","ScalableColorType"));

    //****************** Attributes ******************//

    /** Object for the ColorLayoutType */
    protected ObjectColorLayout colorLayout;
    /** Object for the ColorStructureType */
    protected ObjectShortVectorL1 colorStructure;
    /** Object for the ScalableColorType */
    protected ObjectIntVectorL1 scalableColor;


    /****************** Constructors ******************/

    /**
     * Creates a new instance of MetaObjectColor.
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param colorLayout color layout descriptor
     * @param colorStructure  color structure descriptor
     * @param scalableColor scalable color descriptor
     */
    public MetaObjectColor(String locatorURI, ObjectColorLayout colorLayout, ObjectShortVectorL1 colorStructure, ObjectIntVectorL1 scalableColor) {
        super(locatorURI);
        this.colorLayout = colorLayout;
        this.colorStructure = colorStructure;
        this.scalableColor = scalableColor;
    }

    public MetaObjectColor(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        this(locatorURI, objects);
        if (cloneObjects) {
            this.colorLayout = (ObjectColorLayout)this.colorLayout.clone(getObjectKey());
            this.colorStructure = (ObjectShortVectorL1)this.colorStructure.clone(getObjectKey());
            this.scalableColor = (ObjectIntVectorL1)this.scalableColor.clone(getObjectKey());
        }
    }

    public MetaObjectColor(String locatorURI, Map<String, LocalAbstractObject> objects) {
        super(locatorURI);
        this.colorLayout = (ObjectColorLayout)objects.get("ColorLayoutType");
        this.colorStructure = (ObjectShortVectorL1)objects.get("ColorStructureType");
        this.scalableColor = (ObjectIntVectorL1)objects.get("ScalableColorType");
    }

    public MetaObjectColor(MetaObject object) {
        this(object.getLocatorURI(), object.getObjectMap());
    }

    /**
     * Creates a new instance of MetaObjectColor.
     * 
     * @param stream stream to read the data from
     * @param restrictNames the sub-distances may be restricted by passing list of sub-dist names
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectColor(BufferedReader stream, Set<String> restrictNames) throws IOException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);

        // The line should have format "URI;name1;class1;name2;class2;..." and URI can be skipped (including the semicolon)
        String[] uriNamesClasses = line.split(";");

        // Skip the first name if the number of elements is odd
        int i = uriNamesClasses.length % 2;

        // If the URI locator is used (and it is not set from the previous - this is the old format)
        if (i == 1) {
            if ((getObjectKey() == null) && (uriNamesClasses[0].length() > 0)) {
                setObjectKey(new AbstractObjectKey(uriNamesClasses[0]));
            }
        }

        for (; i < uriNamesClasses.length; i += 2) {
            // Check restricted names
            if (restrictNames != null && !restrictNames.contains(uriNamesClasses[i])) {
                try {
                    readObject(stream, uriNamesClasses[i + 1]); // Read the object, but skip it
                } catch (IOException e) { // Ignore the error on skipped objects
                }
            } else if ("ColorLayoutType".equals(uriNamesClasses[i])) {
                colorLayout = readObject(stream, ObjectColorLayout.class);
            } else if ("ColorStructureType".equals(uriNamesClasses[i])) {
                colorStructure = readObject(stream, ObjectShortVectorL1.class);
            } else if ("ScalableColorType".equals(uriNamesClasses[i])) {
                scalableColor = readObject(stream, ObjectIntVectorL1.class);
            }
        }
    }

    /**
     * Creates a new instance of MetaObjectColor.
     *
     * @param stream stream to read the data from
     * @param restrictNames the sub-distances may be restricted by passing list of sub-dist names
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectColor(BufferedReader stream, String[] restrictNames) throws IOException {
        this(stream, new HashSet<String>(Arrays.asList(restrictNames)));
    }

    /**
     * Creates a new instance of MetaObjectColor.
     *
     * @param stream stream to read the data from
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectColor(BufferedReader stream) throws IOException {
        this(stream, descriptorNameSet);
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
        return 3;
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
        else if ("ScalableColorType".equals(name))
            return scalableColor;
        else
            return null;
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
        if (scalableColor != null)
            map.put("ScalableColorType", scalableColor);
        return map;
    }


    // ***************************  Distance computation  ******************************* //

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        MetaObjectColor castObj = (MetaObjectColor)obj;

        float rtv = 0;

        if (colorLayout != null && castObj.colorLayout != null) {
            if (metaDistances != null) {
                metaDistances[0] = colorLayout.getDistanceImpl(castObj.colorLayout, distThreshold)/300.0f;
                rtv += metaDistances[0]*2.0f;
            } else {
                rtv += colorLayout.getDistanceImpl(castObj.colorLayout, distThreshold)*2.0f/300.0f;
            }
        }

        if (colorStructure != null && castObj.colorStructure != null) {
            if (metaDistances != null) {
                metaDistances[1] = colorStructure.getDistanceImpl(castObj.colorStructure, distThreshold)/40.0f/255.0f;
                rtv += metaDistances[1]*2.0f;
            } else {
                rtv += colorStructure.getDistanceImpl(castObj.colorStructure, distThreshold)*2.0f/40.0f/255.0f;
            }
        }

        if (scalableColor != null && castObj.scalableColor != null) {
            if (metaDistances != null) {
                metaDistances[2] = scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold)/3000.0f;
                rtv += metaDistances[2]*2.0f;
            } else {
                rtv += scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold)*2.0f/3000.0f;
            }
        }

        return rtv;
    }

    public static float[] getWeights() {
        return new float[] { 2.0f, 2.0f, 2.0f};
    }

    @Override
    public float getMaxDistance() {
        return 6f;
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
        MetaObjectColor rtv = (MetaObjectColor)super.clone(cloneFilterChain);
        if (colorLayout != null)
            rtv.colorLayout = (ObjectColorLayout)colorLayout.clone(cloneFilterChain);
        if (colorStructure != null)
            rtv.colorStructure = (ObjectShortVectorL1)colorStructure.clone(cloneFilterChain);
        if (scalableColor != null)
            rtv.scalableColor = (ObjectIntVectorL1)scalableColor.clone(cloneFilterChain);

        return rtv;
    }

    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        MetaObjectColor rtv = (MetaObjectColor)super.clone(true);
        if (colorLayout != null)
            rtv.colorLayout = (ObjectColorLayout)colorLayout.cloneRandomlyModify(args);
        if (colorStructure != null)
            rtv.colorStructure = (ObjectShortVectorL1)colorStructure.cloneRandomlyModify(args);
        if (scalableColor != null)
            rtv.scalableColor = (ObjectIntVectorL1)scalableColor.cloneRandomlyModify(args);
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
        if (scalableColor != null) {
            if (written)
                stream.write(';');
            stream.write("ScalableColorType;messif.objects.impl.ObjectIntVectorL1".getBytes());
            written = true;
        }
        if (written) {
            stream.write('\n');
            // Write a line for every object from the list (skip the comments)
            if (colorLayout != null)
                colorLayout.writeData(stream);
            if (colorStructure != null)
                colorStructure.writeData(stream);
            if (scalableColor != null)
                scalableColor.writeData(stream);
        }
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObjectColor loaded from binary input buffer.
     * 
     * @param input the buffer to read the MetaObjectColor from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectColor(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        colorLayout = serializator.readObject(input, ObjectColorLayout.class);
        colorStructure = serializator.readObject(input, ObjectShortVectorL1.class);
        scalableColor = serializator.readObject(input, ObjectIntVectorL1.class);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        size += serializator.write(output, colorLayout);
        size += serializator.write(output, colorStructure);
        size += serializator.write(output, scalableColor);
        return size;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator);
        size += serializator.getBinarySize(colorLayout);
        size += serializator.getBinarySize(colorStructure);
        size += serializator.getBinarySize(scalableColor);
        return size;
    }

}


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
public class MetaObjectShape extends MetaObject implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** The list of supported names ******************//

    /** The list of the names for the encapsulated objects */
    protected static final String[] descriptorNames = {"EdgeHistogramType","RegionShapeType"};

    /** The list of the names for the encapsulated objects - in the form of a set */
    protected static final Set<String> descriptorNameSet = new HashSet<String>(Arrays.asList("EdgeHistogramType","RegionShapeType"));

    //****************** Attributes ******************//

    /** Object for the EdgeHistogramType */
    protected ObjectVectorEdgecomp edgeHistogram;
    /** Object for the RegionShapeType */
    protected ObjectRegionShape regionShape;


    /****************** Constructors ******************/

    /**
     * Creates a new instance of MetaObjectShape
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param edgeHistogram edge histogram descriptor
     * @param regionShape region shape descriptor
     */
    public MetaObjectShape(String locatorURI, ObjectVectorEdgecomp edgeHistogram, ObjectRegionShape regionShape) {
        super(locatorURI);
        this.edgeHistogram = edgeHistogram;
        this.regionShape = regionShape;
    }

    public MetaObjectShape(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        this(locatorURI, objects);
        if (cloneObjects) {
            this.edgeHistogram = (ObjectVectorEdgecomp)this.edgeHistogram.clone(getObjectKey());
            this.regionShape = (ObjectRegionShape)this.regionShape.clone(getObjectKey());
        }
    }

    public MetaObjectShape(String locatorURI, Map<String, LocalAbstractObject> objects) {
        super(locatorURI);
        this.edgeHistogram = (ObjectVectorEdgecomp)objects.get("EdgeHistogramType");
        this.regionShape = (ObjectRegionShape)objects.get("RegionShapeType");
    }

    public MetaObjectShape(MetaObject object) {
        this(object.getLocatorURI(), object.getObjectMap());
    }

    /** 
     * Creates a new instance of MetaObjectShape.
     *
     * @param stream stream to read the data from
     * @param restrictNames the sub-distances may be restricted by passing list of sub-dist names
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectShape(BufferedReader stream, Set<String> restrictNames) throws IOException {
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
            } else if ("EdgeHistogramType".equals(uriNamesClasses[i])) {
                edgeHistogram = readObject(stream, ObjectVectorEdgecomp.class);
            } else if ("RegionShapeType".equals(uriNamesClasses[i])) {
                regionShape = readObject(stream, ObjectRegionShape.class);
            }
        }
    }

    /**
     * Creates a new instance of MetaObjectShape.
     *
     * @param stream stream to read the data from
     * @param restrictNames the sub-distances may be restricted by passing list of sub-dist names
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectShape(BufferedReader stream, String[] restrictNames) throws IOException {
        this(stream, new HashSet<String>(Arrays.asList(restrictNames)));
    }

    /**
     * Creates a new instance of MetaObjectShape.
     *
     * @param stream stream to read the data from
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectShape(BufferedReader stream) throws IOException {
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
        return 2;
    }

    /**
     * Returns the encapsulated object for given symbolic name.
     *
     * @param name the symbolic name of the object to return
     * @return encapsulated object for given name or <tt>null</tt> if the key is unknown
     */
    @Override
    public LocalAbstractObject getObject(String name) {
        if ("EdgeHistogramType".equals(name))
            return edgeHistogram;
        else if ("RegionShapeType".equals(name))
            return regionShape;
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
        if (edgeHistogram != null)
            map.put("EdgeHistogramType", edgeHistogram);
        if (regionShape != null)
            map.put("RegionShapeType", regionShape);
        return map;
    }


    // ***************************  Distance computation  ******************************* //

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        MetaObjectShape castObj = (MetaObjectShape)obj;

        float rtv = 0;

        if (edgeHistogram != null && castObj.edgeHistogram != null) {
            if (metaDistances != null) {
                metaDistances[3] = edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold)/68.0f;
                rtv += metaDistances[3]*5.0f;
            } else {
                rtv += edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold)*5.0f/68.0f;
            }
        }

        if (regionShape != null && castObj.regionShape != null) {
            if (metaDistances != null) {
                metaDistances[4] = regionShape.getDistanceImpl(castObj.regionShape, distThreshold)/8.0f;
                rtv += metaDistances[4]*4.0f;
            } else {
                rtv += regionShape.getDistanceImpl(castObj.regionShape, distThreshold)*4.0f/8.0f;
            }
        }

        return rtv;
    }

    public static float[] getWeights() {
        return new float[] {5.0f, 4.0f};
    }

    @Override
    public float getMaxDistance() {
        return 10f;
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
        MetaObjectShape rtv = (MetaObjectShape)super.clone(cloneFilterChain);
        if (edgeHistogram != null)
            rtv.edgeHistogram = (ObjectVectorEdgecomp)edgeHistogram.clone(cloneFilterChain);
        if (regionShape != null)
            rtv.regionShape = (ObjectRegionShape)regionShape.clone(cloneFilterChain);

        return rtv;
    }

    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        MetaObjectShape rtv = (MetaObjectShape)super.clone(true);
        if (edgeHistogram != null)
            rtv.edgeHistogram = (ObjectVectorEdgecomp)edgeHistogram.cloneRandomlyModify(args);
        if (regionShape != null)
            rtv.regionShape = (ObjectRegionShape)regionShape.cloneRandomlyModify(args);
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
        if (edgeHistogram != null) {
            if (written)
                stream.write(';');
            stream.write("EdgeHistogramType;messif.objects.impl.ObjectVectorEdgecomp".getBytes());
            written = true;
        }
        if (regionShape != null) {
            if (written)
                stream.write(';');
            stream.write("RegionShapeType;messif.objects.impl.ObjectRegionShape".getBytes());
            written = true;
        }
        if (written) {
            stream.write('\n');
            // Write a line for every object from the list (skip the comments)
            if (edgeHistogram != null)
                edgeHistogram.writeData(stream);
            if (regionShape != null)
                regionShape.writeData(stream);
        }
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObjectShapeAndColor loaded from binary input buffer.
     * 
     * @param input the buffer to read the MetaObjectShapeAndColor from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectShape(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        edgeHistogram = serializator.readObject(input, ObjectVectorEdgecomp.class);
        regionShape = serializator.readObject(input, ObjectRegionShape.class);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        size += serializator.write(output, edgeHistogram);
        size += serializator.write(output, regionShape);
        return size;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator);
        size += serializator.getBinarySize(edgeHistogram);
        size += serializator.getBinarySize(regionShape);
        return size;
    }

}

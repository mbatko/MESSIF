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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 * This class represents a meta object that encapsulates MPEG7 descriptors for shape.
 * The descriptors are EdgeHistogram and RegionShape.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MetaObjectShape extends MetaObject implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 2L;

    //****************** Constants ******************//

    /** The list of the names for the encapsulated objects */
    protected static final String[] descriptorNames = {"EdgeHistogramType","RegionShapeType"};
    /** Descriptor weights used to compute the overall distance */
    protected static final float[] descriptorWeights = { 5.0f, 4.0f };


    //****************** Attributes ******************//

    /** Object for the EdgeHistogramType */
    protected ObjectVectorEdgecomp edgeHistogram;
    /** Object for the RegionShapeType */
    protected ObjectXMRegionShape regionShape;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of MetaObjectShape with the given locator and encapsulated objects.
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param edgeHistogram edge histogram descriptor
     * @param regionShape region shape descriptor
     */
    public MetaObjectShape(String locatorURI, ObjectVectorEdgecomp edgeHistogram, ObjectXMRegionShape regionShape) {
        super(locatorURI);
        this.edgeHistogram = edgeHistogram;
        this.regionShape = regionShape;
    }

    /**
     * Creates a new instance of MetaObjectShape with the given locator and encapsulated objects.
     * Objects with the given descriptor names are taken from the map.
     * Note that the encapsulated objects will have the correct key only if the
     * {@code cloneObjects} is requested.
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param objects the map of encapsulated objects
     * @param cloneObjects flag whether to clone (<tt>true</tt>) the encapsulated objects or
     *      use the instances from the objects map directly (<tt>false</tt>)
     * @throws CloneNotSupportedException if there was a problem clonning the objects from the map
     */
    public MetaObjectShape(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        super(locatorURI);
        this.edgeHistogram = getObjectFromMap(objects, descriptorNames[0], ObjectVectorEdgecomp.class, cloneObjects, getObjectKey());
        this.regionShape = getObjectFromMap(objects, descriptorNames[1], ObjectXMRegionShape.class, cloneObjects, getObjectKey());
    }

    /**
     * Creates a new instance of MetaObjectShape from the given map of objects.
     * Note that the encapsulated object are not clonned and will retain their keys.
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param objects a map of named objects from which to get the internal objects of the MetaObjectShapeAndColor
     */
    public MetaObjectShape(String locatorURI, Map<String, LocalAbstractObject> objects) {
        super(locatorURI);
        this.edgeHistogram = (ObjectVectorEdgecomp)objects.get(descriptorNames[0]);
        this.regionShape = (ObjectXMRegionShape)objects.get(descriptorNames[1]);
    }

    /**
     * Creates a new instance of MetaObjectShape by taking objects from another {@link MetaObject}.
     * Note that the objects are not clonned.
     * @param object the meta object from which this one is created
     */
    public MetaObjectShape(MetaObject object) {
        this(object.getLocatorURI(), object.getObjectMap());
    }

    /**
     * Creates a new instance of MetaObjectShape from a text stream.
     *
     * @param stream the text stream to read the data from
     * @param restrictNames the sub-distances may be restricted by passing list of sub-dist names
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectShape(BufferedReader stream, Set<String> restrictNames) throws IOException {
        Map<String, LocalAbstractObject> objects = readObjects(stream, restrictNames, readObjectsHeader(stream), new HashMap<String, LocalAbstractObject>(descriptorNames.length));
        this.edgeHistogram = (ObjectVectorEdgecomp)objects.get(descriptorNames[0]);
        this.regionShape = (ObjectXMRegionShape)objects.get(descriptorNames[1]);
    }

    /**
     * Creates a new instance of MetaObjectShape from a text stream.
     *
     * @param stream the text stream to read the data from
     * @param restrictNames the sub-distances may be restricted by passing list of sub-dist names
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectShape(BufferedReader stream, String[] restrictNames) throws IOException {
        this(stream, new HashSet<String>(Arrays.asList(restrictNames)));
    }

    /**
     * Creates a new instance of MetaObjectShape from a text stream.
     *
     * @param stream the text stream to read the data from
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectShape(BufferedReader stream) throws IOException {
        this(stream, descriptorNames);
    }


    //****************** Access to object names and weights by static methods ******************//

    /**
     * Returns list of supported visual descriptor types that this object recognizes in XML.
     * @return list of supported visual descriptor types
     */
    public static String[] getSupportedVisualDescriptorTypes() {
        return descriptorNames.clone();
    }

    /**
     * Returns the weights used to compute the overall distance.
     * @return the weights used to compute the overall distance
     */
    public static float[] getWeights() {
        return descriptorWeights.clone();
    }


    //****************** MetaObject overrides ******************//

    /**
     * Returns the number of encapsulated objects.
     * @return the number of encapsulated objects
     */
    @Override
    public int getObjectCount() {
        return descriptorNames.length;
    }

    /**
     * Returns the encapsulated object for given symbolic name.
     *
     * @param name the symbolic name of the object to return
     * @return encapsulated object for given name or <tt>null</tt> if the key is unknown
     */
    @Override
    public LocalAbstractObject getObject(String name) {
        if (descriptorNames[0].equals(name))
            return edgeHistogram;
        else if (descriptorNames[1].equals(name))
            return regionShape;
        else
            return null;
    }

    @Override
    public Collection<LocalAbstractObject> getObjects() {
        return Arrays.asList((LocalAbstractObject)edgeHistogram, regionShape);
    }

    @Override
    public Collection<String> getObjectNames() {
        return Arrays.asList(descriptorNames);
    }

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        writeObjects(stream, writeObjectsHeader(stream, getObjectMap()));
    }


    //***************************  Distance computation  *******************************//

    @Override
    protected float getDistanceImpl(MetaObject obj, float[] metaDistances, float distThreshold) {
        MetaObjectShape castObj = (MetaObjectShape)obj;

        float rtv = 0;

        if (edgeHistogram != null && castObj.edgeHistogram != null) {
            if (metaDistances != null) {
                metaDistances[0] = edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold) / 68.0f;
                rtv += metaDistances[0] * descriptorWeights[0];
            } else {
                rtv += edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold) * descriptorWeights[0] / 68.0f;
            }
        }

        if (regionShape != null && castObj.regionShape != null) {
            if (metaDistances != null) {
                metaDistances[1] = regionShape.getDistanceImpl(castObj.regionShape, distThreshold) / 8.0f;
                rtv += metaDistances[1] * descriptorWeights[1];
            } else {
                rtv += regionShape.getDistanceImpl(castObj.regionShape, distThreshold) * descriptorWeights[1] / 8.0f;
            }
        }

        return rtv;
    }

    @Override
    public float getMaxDistance() {
        float sum = 1; // Adjustment to overcome rounding problems
        for (int i = 0; i < descriptorWeights.length; i++)
            sum += descriptorWeights[i];
        return sum;
    }


    //****************** Clonning ******************//

    @Override
    public LocalAbstractObject clone(boolean cloneFilterChain) throws CloneNotSupportedException {
        MetaObjectShape rtv = (MetaObjectShape)super.clone(cloneFilterChain);
        if (edgeHistogram != null)
            rtv.edgeHistogram = (ObjectVectorEdgecomp)edgeHistogram.clone(cloneFilterChain);
        if (regionShape != null)
            rtv.regionShape = (ObjectXMRegionShape)regionShape.clone(cloneFilterChain);

        return rtv;
    }

    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        MetaObjectShape rtv = (MetaObjectShape)super.clone(true);
        if (edgeHistogram != null)
            rtv.edgeHistogram = (ObjectVectorEdgecomp)edgeHistogram.cloneRandomlyModify(args);
        if (regionShape != null)
            rtv.regionShape = (ObjectXMRegionShape)regionShape.cloneRandomlyModify(args);
        return rtv;
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObjectShape loaded from binary input buffer.
     * 
     * @param input the buffer to read the MetaObjectShapeAndColor from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectShape(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        edgeHistogram = serializator.readObject(input, ObjectVectorEdgecomp.class);
        regionShape = serializator.readObject(input, ObjectXMRegionShape.class);
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

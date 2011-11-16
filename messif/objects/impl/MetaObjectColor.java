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
 * This class represents a meta object that encapsulates MPEG7 descriptors for colors.
 * The descriptors are ColorLayout, ColorStructure, and ScalableColor.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MetaObjectColor extends MetaObject implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    /** The list of the names for the encapsulated objects */
    protected static final String[] descriptorNames = {"ColorLayoutType","ColorStructureType","ScalableColorType"};
    /** Descriptor weights used to compute the overall distance */
    protected static final float[] descriptorWeights = { 2.0f, 2.0f, 2.0f };


    //****************** Attributes ******************//

    /** Object for the ColorLayoutType */
    protected ObjectColorLayout colorLayout;
    /** Object for the ColorStructureType */
    protected ObjectShortVectorL1 colorStructure;
    /** Object for the ScalableColorType */
    protected ObjectIntVectorL1 scalableColor;


    //****************** Constructors ******************//

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

    /**
     * Creates a new instance of MetaObjectColor from the given map of objects.
     * Note that the encapsulated objects will have the correct key only if the
     * {@code cloneObjects} is requested.
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param objects a map of named objects from which to get the internal objects of the MetaObjectShapeAndColor
     * @param cloneObjects  flag whether to clone the objects from the map (<tt>true</tt>) or not (<tt>false</tt>)
     * @throws CloneNotSupportedException if the cloning was not supported by any of the cloned objects
     */
    public MetaObjectColor(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        super(locatorURI);
        this.colorLayout = getObjectFromMap(objects, descriptorNames[0], ObjectColorLayout.class, cloneObjects, getObjectKey());
        this.colorStructure = getObjectFromMap(objects, descriptorNames[1], ObjectShortVectorL1.class, cloneObjects, getObjectKey());
        this.scalableColor = getObjectFromMap(objects, descriptorNames[2], ObjectIntVectorL1.class, cloneObjects, getObjectKey());
    }

    /**
     * Creates a new instance of MetaObjectColor from the given map of objects.
     * Note that the encapsulated object are not cloned and will retain their keys.
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param objects a map of named objects from which to get the internal objects of the MetaObjectShapeAndColor
     */
    public MetaObjectColor(String locatorURI, Map<String, LocalAbstractObject> objects) {
        super(locatorURI);
        this.colorLayout = (ObjectColorLayout)objects.get(descriptorNames[0]);
        this.colorStructure = (ObjectShortVectorL1)objects.get(descriptorNames[1]);
        this.scalableColor = (ObjectIntVectorL1)objects.get(descriptorNames[2]);
    }

    /**
     * Creates a new instance of MetaObjectColor by taking objects from another {@link MetaObject}.
     * Note that the objects are not cloned.
     * @param object the meta object from which this one is created
     */
    public MetaObjectColor(MetaObject object) {
        this(object.getLocatorURI(), object.getObjectMap());
    }

    /**
     * Creates a new instance of MetaObjectColor from a text stream.
     * Only the descriptors specified in restrict names are loaded.
     *
     * @param stream the text stream to read the data from
     * @param restrictNames the sub-distances may be restricted by passing list of sub-dist names
     * @throws IOException if there was a problem reading from the stream
     */
    public MetaObjectColor(BufferedReader stream, Set<String> restrictNames) throws IOException {
        Map<String, LocalAbstractObject> objects = readObjects(stream, restrictNames, readObjectsHeader(stream), new HashMap<String, LocalAbstractObject>(descriptorNames.length));
        this.colorLayout = (ObjectColorLayout)objects.get(descriptorNames[0]);
        this.colorStructure = (ObjectShortVectorL1)objects.get(descriptorNames[1]);
        this.scalableColor = (ObjectIntVectorL1)objects.get(descriptorNames[2]);
    }

    /**
     * Creates a new instance of MetaObjectColor from a text stream.
     * Only the descriptors specified in restrict names are loaded.
     *
     * @param stream the text stream to read the data from
     * @param restrictNames the sub-distances may be restricted by passing list of sub-dist names
     * @throws IOException if there was a problem reading from the stream
     */
    public MetaObjectColor(BufferedReader stream, String[] restrictNames) throws IOException {
        this(stream, new HashSet<String>(Arrays.asList(restrictNames)));
    }

    /**
     * Creates a new instance of MetaObjectColor from a text stream.
     *
     * @param stream the text stream to read the data from
     * @throws IOException if there was a problem reading from the stream
     */
    public MetaObjectColor(BufferedReader stream) throws IOException {
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
            return colorLayout;
        else if (descriptorNames[1].equals(name))
            return colorStructure;
        else if (descriptorNames[2].equals(name))
            return scalableColor;
        else
            return null;
    }

    @Override
    public Collection<LocalAbstractObject> getObjects() {
        return Arrays.asList((LocalAbstractObject)colorLayout, colorStructure, scalableColor);
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
    protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
        MetaObjectColor castObj = (MetaObjectColor)obj;

        float rtv = 0;

        if (colorLayout != null && castObj.colorLayout != null) {
            if (metaDistances != null) {
                metaDistances[0] = colorLayout.getDistanceImpl(castObj.colorLayout, distThreshold) / 300.0f;
                rtv += metaDistances[0] * descriptorWeights[0];
            } else {
                rtv += colorLayout.getDistanceImpl(castObj.colorLayout, distThreshold) * descriptorWeights[0] / 300.0f;
            }
        }

        if (colorStructure != null && castObj.colorStructure != null) {
            if (metaDistances != null) {
                metaDistances[1] = colorStructure.getDistanceImpl(castObj.colorStructure, distThreshold) / 40.0f / 255.0f;
                rtv += metaDistances[1] * descriptorWeights[1];
            } else {
                rtv += colorStructure.getDistanceImpl(castObj.colorStructure, distThreshold) * descriptorWeights[1] / 40.0f / 255.0f;
            }
        }

        if (scalableColor != null && castObj.scalableColor != null) {
            if (metaDistances != null) {
                metaDistances[2] = scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold) / 3000.0f;
                rtv += metaDistances[2] * descriptorWeights[2];
            } else {
                rtv += scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold) * descriptorWeights[2] / 3000.0f;
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


    //****************** Cloning ******************//

    /**
     * Creates and returns a copy of this object. The precise meaning 
     * of "copy" may depend on the class of the object.
     * @param cloneFilterChain  the flag whether the filter chain must be cloned as well.
     * @return a clone of this instance.
     * @throws CloneNotSupportedException if the object's class does not support cloning or there was an error
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

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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MetaObjectShapeAndColor extends MetaObject implements BinarySerializable {

    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** The list of supported names ******************//

    /** The list of the names for the encapsulated objects */
    protected static final String[] descriptorNames = {"ColorLayoutType","ColorStructureType","ScalableColorType","EdgeHistogramType","RegionShapeType"};

    /** The list of the names for the encapsulated objects - in the form of a set */
    protected static final Set<String> descriptorNameSet = new HashSet<String>(Arrays.asList("ColorLayoutType","ColorStructureType","ScalableColorType","EdgeHistogramType","RegionShapeType"));

    //****************** Attributes ******************//

    /** Object for the ColorLayoutType */
    protected ObjectColorLayout colorLayout;
    /** Object for the ColorStructureType */
    protected ObjectShortVectorL1 colorStructure;
    /** Object for the ScalableColorType */
    protected ObjectIntVectorL1 scalableColor;
    /** Object for the EdgeHistogramType */
    protected ObjectVectorEdgecomp edgeHistogram;
    /** Object for the RegionShapeType */
    protected ObjectRegionShape regionShape;


    /****************** Constructors ******************/

    /** 
     * Creates a new instance of MetaObjectShapeAndColor
     *
     * @param locatorURI locator of the metaobject (and typically all of the passed objects)
     * @param colorLayout color layout descriptor
     * @param colorStructure  color structure descriptor
     * @param scalableColor scalable color descriptor
     * @param edgeHistogram edge histogram descriptor
     * @param regionShape region shape descriptor
     */
    public MetaObjectShapeAndColor(String locatorURI, ObjectColorLayout colorLayout, ObjectShortVectorL1 colorStructure, ObjectIntVectorL1 scalableColor, ObjectVectorEdgecomp edgeHistogram, ObjectRegionShape regionShape) {
        super(locatorURI);
        this.colorLayout = colorLayout;
        this.colorStructure = colorStructure;
        this.scalableColor = scalableColor;
        this.edgeHistogram = edgeHistogram;
        this.regionShape = regionShape;
    }

    public MetaObjectShapeAndColor(String locatorURI, Map<String, LocalAbstractObject> objects, boolean cloneObjects) throws CloneNotSupportedException {
        this(locatorURI, objects);
        if (cloneObjects) {
            this.colorLayout = (ObjectColorLayout)this.colorLayout.clone(getObjectKey());
            this.colorStructure = (ObjectShortVectorL1)this.colorStructure.clone(getObjectKey());
            this.scalableColor = (ObjectIntVectorL1)this.scalableColor.clone(getObjectKey());
            this.edgeHistogram = (ObjectVectorEdgecomp)this.edgeHistogram.clone(getObjectKey());
            this.regionShape = (ObjectRegionShape)this.regionShape.clone(getObjectKey());
        }
    }

    public MetaObjectShapeAndColor(String locatorURI, Map<String, LocalAbstractObject> objects) {
        super(locatorURI);
        this.colorLayout = (ObjectColorLayout)objects.get("ColorLayoutType");
        this.colorStructure = (ObjectShortVectorL1)objects.get("ColorStructureType");
        this.scalableColor = (ObjectIntVectorL1)objects.get("ScalableColorType");
        this.edgeHistogram = (ObjectVectorEdgecomp)objects.get("EdgeHistogramType");
        this.regionShape = (ObjectRegionShape)objects.get("RegionShapeType");
    }

    public MetaObjectShapeAndColor(MetaObject object) {
        this(object.getLocatorURI(), object.getObjectMap());
    }

    /**
     * Creates a new instance of MetaObjectShapeAndColor.
     * 
     * @param stream stream to read the data from
     * @param restrictNames the sub-distances may be restricted by passing list of sub-dist names
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectShapeAndColor(BufferedReader stream, Set<String> restrictNames) throws IOException {
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
            } else if ("EdgeHistogramType".equals(uriNamesClasses[i])) {
                edgeHistogram = readObject(stream, ObjectVectorEdgecomp.class);
            } else if ("RegionShapeType".equals(uriNamesClasses[i])) {
                regionShape = readObject(stream, ObjectRegionShape.class);
            }
        }
    }

    /**
     * Creates a new instance of MetaObjectShapeAndColor.
     *
     * @param stream stream to read the data from
     * @param restrictNames the sub-distances may be restricted by passing list of sub-dist names
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectShapeAndColor(BufferedReader stream, String[] restrictNames) throws IOException {
        this(stream, new HashSet<String>(Arrays.asList(restrictNames)));
    }

    /**
     * Creates a new instance of MetaObjectShapeAndColor.
     *
     * @param stream stream to read the data from
     * @throws IOException if reading from the stream fails
     */
    public MetaObjectShapeAndColor(BufferedReader stream) throws IOException {
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
        return 5;
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
        else if ("ScalableColorType".equals(name))
            return scalableColor;
        else if ("RegionShapeType".equals(name))
            return regionShape;
        else
            return null;
    }

    @Override
    public Collection<LocalAbstractObject> getObjects() {
        return Arrays.asList((LocalAbstractObject)colorLayout, colorStructure, edgeHistogram, scalableColor, regionShape);
    }

    @Override
    public Collection<String> getObjectNames() {
        return Arrays.asList(descriptorNames);
    }


    // ***************************  Distance computation  ******************************* //

    @Override
    protected float getDistanceImpl(MetaObject obj, float[] metaDistances, float distThreshold) {
        MetaObjectShapeAndColor castObj = (MetaObjectShapeAndColor)obj;

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
        return new float[] { 2.0f, 2.0f, 2.0f, 5.0f, 4.0f};
    }

    @Override
    public float getMaxDistance() {
        return 16f;
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
        MetaObjectShapeAndColor rtv = (MetaObjectShapeAndColor)super.clone(cloneFilterChain);
        if (colorLayout != null)
            rtv.colorLayout = (ObjectColorLayout)colorLayout.clone(cloneFilterChain);
        if (colorStructure != null)
            rtv.colorStructure = (ObjectShortVectorL1)colorStructure.clone(cloneFilterChain);
        if (edgeHistogram != null)
            rtv.edgeHistogram = (ObjectVectorEdgecomp)edgeHistogram.clone(cloneFilterChain);
        if (scalableColor != null)
            rtv.scalableColor = (ObjectIntVectorL1)scalableColor.clone(cloneFilterChain);
        if (regionShape != null)
            rtv.regionShape = (ObjectRegionShape)regionShape.clone(cloneFilterChain);

        return rtv;
    }

    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        MetaObjectShapeAndColor rtv = (MetaObjectShapeAndColor)super.clone(true);
        if (colorLayout != null)
            rtv.colorLayout = (ObjectColorLayout)colorLayout.cloneRandomlyModify(args);
        if (colorStructure != null)
            rtv.colorStructure = (ObjectShortVectorL1)colorStructure.cloneRandomlyModify(args);
        if (edgeHistogram != null)
            rtv.edgeHistogram = (ObjectVectorEdgecomp)edgeHistogram.cloneRandomlyModify(args);
        if (scalableColor != null)
            rtv.scalableColor = (ObjectIntVectorL1)scalableColor.cloneRandomlyModify(args);
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
        if (scalableColor != null) {
            if (written)
                stream.write(';');
            stream.write("ScalableColorType;messif.objects.impl.ObjectIntVectorL1".getBytes());
            written = true;
        }
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
            if (colorLayout != null)
                colorLayout.writeData(stream);
            if (colorStructure != null)
                colorStructure.writeData(stream);
            if (scalableColor != null)
                scalableColor.writeData(stream);
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
    protected MetaObjectShapeAndColor(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        colorLayout = serializator.readObject(input, ObjectColorLayout.class);
        colorStructure = serializator.readObject(input, ObjectShortVectorL1.class);
        scalableColor = serializator.readObject(input, ObjectIntVectorL1.class);
        edgeHistogram = serializator.readObject(input, ObjectVectorEdgecomp.class);
        regionShape = serializator.readObject(input, ObjectRegionShape.class);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        size += serializator.write(output, colorLayout);
        size += serializator.write(output, colorStructure);
        size += serializator.write(output, scalableColor);
        size += serializator.write(output, edgeHistogram);
        size += serializator.write(output, regionShape);
        return size;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator);
        size += serializator.getBinarySize(colorLayout);
        size += serializator.getBinarySize(colorStructure);
        size += serializator.getBinarySize(scalableColor);
        size += serializator.getBinarySize(edgeHistogram);
        size += serializator.getBinarySize(regionShape);
        return size;
    }

}

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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObjectParametric;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;

/**
 * This class represents a meta object that encapsulates MPEG7 descriptors for shape and color
 * with optional data stored via the {@link messif.utility.Parametric} interface.
 * The descriptors are ColorLayout, ColorStructure, ScalableColor, EdgeHistogram, and RegionShape.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MetaObjectParametricShapeAndColor extends MetaObjectParametricArrayWeightedSum implements BinarySerializable {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Constants ******************//

    /** The list of the names for the encapsulated objects */
    private static final String[] descriptorNames = {
        "ColorLayoutType", "ColorStructureType", "ScalableColorType", "EdgeHistogramType", "RegionShapeType"
    };

    /** Descriptor weights used to compute the overall distance */
    private static final float[] weights = { 2.0f / 300.0f, 2.0f / 40.0f / 255.0f, 2.0f / 3000.0f, 5.0f / 68.0f, 4.0f / 8.0f };

    /** Maximal distance */
    private static final float maxDistance = 16;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of MetaObjectParametricShapeAndColor with the given encapsulated objects.
     * 
     * @param locatorURI the locator URI for the new object
     * @param additionalParameters additional parameters for this meta object
     * @param colorLayout the color layout MPEG7 visual descriptor
     * @param colorStructure the color structure MPEG7 visual descriptor
     * @param scalableColor the scalable color MPEG7 visual descriptor
     * @param edgeHistogram the edge histogram MPEG7 visual descriptor
     * @param regionShape the region shape MPEG7 visual descriptor
     */
    public MetaObjectParametricShapeAndColor(String locatorURI, Map<String, ? extends Serializable> additionalParameters, ObjectColorLayout colorLayout, ObjectShortVectorL1 colorStructure, ObjectIntVectorL1 scalableColor, ObjectVectorEdgecomp edgeHistogram, ObjectXMRegionShape regionShape) {
        super(locatorURI, additionalParameters, colorLayout, colorStructure, scalableColor, edgeHistogram, regionShape);
    }

    /**
     * Creates a new instance of MetaObjectParametricShapeAndColor with the given encapsulated objects.
     * 
     * @param locatorURI the locator URI for the new object
     * @param additionalParameters additional parameters for this meta object
     * @param objects the encapsulated objects to add (keys should match the {@link #descriptorNames})
     */
    public MetaObjectParametricShapeAndColor(String locatorURI, Map<String, ? extends Serializable> additionalParameters, Map<String, ? extends LocalAbstractObject> objects) {
        super(locatorURI, additionalParameters, objects, descriptorNames);
    }

    /**
     * Creates a new instance of MetaObjectParametricShapeAndColor from another {@link MetaObjectParametric}.
     * @param metaObject the meta object the encapsulated objects of which to add (keys should match the {@link #descriptorNames})
     */
    public MetaObjectParametricShapeAndColor(MetaObjectParametric metaObject) {
        this(metaObject.getLocatorURI(), metaObject.getParameterMap(), metaObject.getObjectMap());
    }

    /**
     * Creates a new instance of MetaObjectParametricShapeAndColor from a stream.
     * 
     * @param stream text stream to read the data from
     * @throws IOException when an error appears during reading from given stream;
     *         {@link java.io.EOFException} is thrown when end-of-file of the given stream is reached
     * @throws NumberFormatException when the line with the descriptor is not valid
     */
    public MetaObjectParametricShapeAndColor(BufferedReader stream) throws IOException, NumberFormatException {
        this(stream, new HashMap<String, Serializable>());
    }

    /**
     * Creates a new instance of MetaObjectParametricShapeAndColor from a stream.
     * 
     * @param stream text stream to read the data from
     * @param additionalParameters additional parameters for this meta object
     * @throws IOException when an error appears during reading from given stream;
     *         {@link java.io.EOFException} is thrown when end-of-file of the given stream is reached
     * @throws NumberFormatException when the line with the descriptor is not valid
     */
    @SuppressWarnings("unchecked")
    public MetaObjectParametricShapeAndColor(BufferedReader stream, Map<String, ? extends Serializable> additionalParameters) throws IOException, NumberFormatException {
        super(stream, additionalParameters, ObjectColorLayout.class, ObjectShortVectorL1.class, ObjectIntVectorL1.class, ObjectVectorEdgecomp.class, ObjectXMRegionShape.class);
    }

    /**
     * Creates a new instance of MetaObjectParametricShapeAndColor loaded from binary input.
     * 
     * @param input the input to read the MetaObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectParametricShapeAndColor(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


    //************ Overrides ************//

    /**
     * Returns the list of the names of the possible encapsulated objects.
     * @return the list of the names of the possible encapsulated objects
     */
    public static String[] getDescriptorNames() {
        return descriptorNames.clone();
    }

    @Override
    protected String getObjectName(int index) {
        return descriptorNames[index];
    }

    /**
     * Returns the weights used for the respective encapsulated objects to compute overall distance.
     * @return the weights used in overall distance function
     */
    public static float[] getWeights() {
        return weights.clone();
    }

    @Override
    protected float getWeight(int index) {
        return weights[index];
    }

    @Override
    public float getMaxDistance() {
        return maxDistance;
    }

}

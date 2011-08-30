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
import java.util.Map;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 * Implementation of the object that encapsulates CoPhIR data including keywords.
 * The five MPEG-7 descriptors, GPS coordinates, and keyword identifiers
 * (from title, description, and tags) are stored in the object.
 * The distance function uses a {@link #getWeights() weighted} sum of the distances
 * of the respective visual descriptors.
 * 
 * @author Ondrej Nevelik, nevelik@gmail.com
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MetaObjectCophirKeywords extends MetaObjectArrayWeightedSum {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** The list of supported names ******************//

    /** The list of the names for the encapsulated objects */
    private static final String[] descriptorNames = {
        "Location", "ColorLayoutType", "ColorStructureType", "EdgeHistogramType", "HomogeneousTextureType",
        "ScalableColorType", "KeyWordsType"
    };

    /** Weights for the default distance function (location and keywords are not used) */
    private static final float[] weights = { 0.0f / 20000f, 1.5f / 300f, 2.5f / 40f / 255f, 4.5f / 68f, 0.5f / 25f, 2.5f / 3000f, 0.0f };

    /** Maximal distance */
    private static final float maxDistance = 16;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of MetaObjectCophirKeywords with the given encapsulated objects.
     * @param locatorURI the locator URI for the new object
     * @param location the GPS location object
     * @param colorLayout the color layout MPEG7 visual descriptor
     * @param colorStructure the color structure MPEG7 visual descriptor
     * @param edgeHistogram the edge histogram MPEG7 visual descriptor
     * @param homogeneousTexture the homogeneous texture MPEG7 visual descriptor
     * @param scalableColor the scalable color MPEG7 visual descriptor
     * @param keywords the keyword identifiers in multi-vector (representing title, description, and tags)
     */
    public MetaObjectCophirKeywords(String locatorURI, ObjectGPSCoordinate location, ObjectColorLayout colorLayout, ObjectShortVectorL1 colorStructure, ObjectVectorEdgecomp edgeHistogram, ObjectHomogeneousTexture homogeneousTexture, ObjectIntVectorL1 scalableColor, ObjectIntMultiVectorJaccard keywords) {
        super(locatorURI, location, colorLayout, colorStructure, edgeHistogram, homogeneousTexture, scalableColor, keywords);
    }

    /**
     * Creates a new instance of MetaObjectCophirKeywords with the given encapsulated objects.
     * @param locatorURI the locator URI for the new object
     * @param objects the encapsulated objects to add (keys should match the {@link #descriptorNames})
     */
    public MetaObjectCophirKeywords(String locatorURI, Map<String, ? extends LocalAbstractObject> objects) {
        super(locatorURI, objects, descriptorNames);
    }

    /**
     * Creates a new instance of MetaObjectCophirKeywords from another {@link MetaObject}.
     * @param metaObject the meta object the encapsulated objects of which to add (keys should match the {@link #descriptorNames})
     */
    public MetaObjectCophirKeywords(MetaObject metaObject) {
        this(metaObject.getLocatorURI(), metaObject.getObjectMap());
    }

    /**
     * Creates a new instance of MetaObjectCophirKeywords from a stream.
     * 
     * @param stream text stream to read the data from
     * @param wordLines number of lines the keyword identifiers are stored on (zero, one, or multiple)
     * @throws IOException when an error appears during reading from given stream;
     *         {@link java.io.EOFException} is thrown when end-of-file of the given stream is reached
     * @throws NumberFormatException when the line with the descriptor is not valid
     */
    @SuppressWarnings("unchecked")
    public MetaObjectCophirKeywords(BufferedReader stream, int wordLines) throws IOException, NumberFormatException {
        super(stream, ObjectGPSCoordinate.class, ObjectColorLayout.class, ObjectShortVectorL1.class,
                ObjectVectorEdgecomp.class, ObjectHomogeneousTexture.class, ObjectIntVectorL1.class, null);
        // Load keywords (special, not loaded automatically by the super constructor)
        switch (wordLines) {
            case 0:
                objects[6] = null;
                break;
            case 1:
                objects[6] = new ObjectIntMultiVectorJaccard(stream);
                break;
            default:
                objects[6] = new ObjectIntMultiVectorJaccard(stream, wordLines);
        }
    }

    /**
     * Creates a new instance of MetaObjectCophirKeywords from a stream.
     * The keyword identifiers are read from a single line.
     * @param stream text stream to read the data from
     * @throws IOException when an error appears during reading from given stream;
     *         {@link java.io.EOFException} is thrown when end-of-file of the given stream is reached
     * @throws NumberFormatException when the line with the descriptor is not valid
     */
    public MetaObjectCophirKeywords(BufferedReader stream) throws IOException {
        this(stream, 1);
    }

    /**
     * Creates a new instance of MetaObjectCophirKeywords loaded from binary input.
     * 
     * @param input the input to read the MetaObject from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectCophirKeywords(BinaryInput input, BinarySerializator serializator) throws IOException {
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

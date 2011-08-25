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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
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
public class MetaObjectCophirKeywords extends MetaObject {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** The list of supported names ******************//

    /** The list of the names for the encapsulated objects */
    private static final String[] descriptorNames = {
        "Location", "ColorLayoutType", "ColorStructureType", "EdgeHistogramType", "HomogeneousTextureType",
        "ScalableColorType", "KeyWordsType"
    };

    /** Weights for the default distance function (location and keywords are not used) */
    private static final float[] weights = { 0.0f, 1.5f, 2.5f, 4.5f, 0.5f, 2.5f, 0.0f };

    /** Maximal distance (computed as the sum of weights) */
    private static final float maxDistance;
    static {
        float weightSum = 1; // This is a safeguard to overcome possible normalization excesses
        for (int i = 0; i < weights.length; i++)
            weightSum += weights[i];
        maxDistance = weightSum;
    }


    //****************** Attributes ******************//

    /** Object for the Location */
    private final ObjectGPSCoordinate location;
    /** Object for the ColorLayoutType */
    private final ObjectColorLayout colorLayout;
    /** Object for the ColorStructureType */
    private final ObjectShortVectorL1 colorStructure;
    /** Object for the EdgeHistogramType */
    private final ObjectVectorEdgecomp edgeHistogram;
    /** Object for the HomogeneousTextureType */
    private final ObjectHomogeneousTexture homogeneousTexture;
    /** Object for the ScalableColorType */
    private final ObjectIntVectorL1 scalableColor;
    /** Keywords of this object */
    private final ObjectIntMultiVectorJaccard keywords;


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
        super(locatorURI);
        this.location = location;
        this.colorLayout = colorLayout;
        this.colorStructure = colorStructure;
        this.edgeHistogram = edgeHistogram;
        this.homogeneousTexture = homogeneousTexture;
        this.scalableColor = scalableColor;
        this.keywords = keywords;
    }

    /**
     * Creates a new instance of MetaObjectCophirKeywords with the given encapsulated objects.
     * @param locatorURI the locator URI for the new object
     * @param objects the encapsulated objects to add (keys should match the {@link #descriptorNames})
     */
    public MetaObjectCophirKeywords(String locatorURI, Map<String, ? extends LocalAbstractObject> objects) {
        super(locatorURI);
        this.location = (ObjectGPSCoordinate)objects.get(descriptorNames[0]);
        this.colorLayout = (ObjectColorLayout)objects.get(descriptorNames[1]);
        this.colorStructure = (ObjectShortVectorL1)objects.get(descriptorNames[2]);
        this.edgeHistogram = (ObjectVectorEdgecomp)objects.get(descriptorNames[3]);
        this.homogeneousTexture = (ObjectHomogeneousTexture)objects.get(descriptorNames[4]);
        this.scalableColor = (ObjectIntVectorL1)objects.get(descriptorNames[5]);
        this.keywords = (ObjectIntMultiVectorJaccard)objects.get(descriptorNames[6]);
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
    public MetaObjectCophirKeywords(BufferedReader stream, int wordLines) throws IOException, NumberFormatException {
        // Keep reading the lines while they are comments, then read the first line of the object
        // readObjectComments method returns first line that is not a comment
        String line = readObjectComments(stream);

        // Location is the first line, but can be empty
        if (line.isEmpty()) {
            location = null;
        } else {
            location = new ObjectGPSCoordinate(new BufferedReader(new StringReader(line)));
        }

        // Load visual descriptors
        colorLayout = new ObjectColorLayout(stream);
        colorStructure = new ObjectShortVectorL1(stream);
        edgeHistogram = new ObjectVectorEdgecomp(stream);
        homogeneousTexture = new ObjectHomogeneousTexture(stream);
        scalableColor = new ObjectIntVectorL1(stream);

        // Load keywords
        switch (wordLines) {
            case 0:
                keywords = null;
                break;
            case 1:
                keywords = new ObjectIntMultiVectorJaccard(stream);
                break;
            default:
                keywords = new ObjectIntMultiVectorJaccard(stream, wordLines);
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


    //****************** Data access methods ******************//

    /**
     * Returns the number of encapsulated objects.
     * @return the number of encapsulated objects
     */
    @Override
    public int getObjectCount() {
        int count = 0;
        if (location != null) {
            count++;
        }
        if (colorLayout != null) {
            count++;
        }
        if (colorStructure != null) {
            count++;
        }
        if (edgeHistogram != null) {
            count++;
        }
        if (homogeneousTexture != null) {
            count++;
        }
        if (scalableColor != null) {
            count++;
        }
        if (keywords != null) {
            count++;
        }
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
        if (descriptorNames[0].equals(name)) {
            return location;
        } else if (descriptorNames[1].equals(name)) {
            return colorLayout;
        } else if (descriptorNames[2].equals(name)) {
            return colorStructure;
        } else if (descriptorNames[3].equals(name)) {
            return edgeHistogram;
        } else if (descriptorNames[4].equals(name)) {
            return homogeneousTexture;
        } else if (descriptorNames[5].equals(name)) {
            return scalableColor;
        } else if (descriptorNames[6].equals(name)) {
            return keywords;
        } else {
            return null;
        }
    }

    /**
     * Returns the object that encapsulates the keywords for this metaobject.
     * @return the object that encapsulates the keywords
     */
    public ObjectIntMultiVectorJaccard getKeyWords() {
        return keywords;
    }


    /**
     * Store this object to a text stream. Print an empty line for encapsulated objects that are not present (are null).
     * This method should have the opposite deserialization in constructor of a given object class.
     *
     * @param stream the stream to store this object to
     * @throws IOException if there was an error while writing to stream
     */
    @Override
    protected void writeData(OutputStream stream) throws IOException {
        if (location != null) {
            location.writeData(stream);
        } else {
            stream.write('\n');
        }
        if (colorLayout != null) {
            colorLayout.writeData(stream);
        } else {
            stream.write('\n');
        }
        if (colorStructure != null) {
            colorStructure.writeData(stream);
        } else {
            stream.write('\n');
        }
        if (edgeHistogram != null) {
            edgeHistogram.writeData(stream);
        } else {
            stream.write('\n');
        }
        if (homogeneousTexture != null) {
            homogeneousTexture.writeData(stream);
        } else {
            stream.write('\n');
        }
        if (scalableColor != null) {
            scalableColor.writeData(stream);
        } else {
            stream.write('\n');
        }
        if (keywords != null) {
            keywords.writeData(stream);
        } else {
            stream.write('\n');
        }
    }

    @Override
    public Collection<String> getObjectNames() {
        Collection<String> names = new ArrayList<String>(6);
        if (location != null) {
            names.add(descriptorNames[0]);
        }
        if (colorLayout != null) {
            names.add(descriptorNames[1]);
        }
        if (colorStructure != null) {
            names.add(descriptorNames[2]);
        }
        if (edgeHistogram != null) {
            names.add(descriptorNames[3]);
        }
        if (homogeneousTexture != null) {
            names.add(descriptorNames[4]);
        }
        if (scalableColor != null) {
            names.add(descriptorNames[5]);
        }
        if (keywords != null) {
            names.add(descriptorNames[6]);
        }
        return names;
    }


    //************ Distance function implementation ************//

    @Override
    public float getMaxDistance() {
        return maxDistance;
    }

    /**
     * Returns the weights used for the respective encapsulated objects to compute overall distance.
     * @return the weights used in overall distance function
     */
    public static float[] getWeights() {
        return weights.clone();
    }

    @Override
    protected float getDistanceImpl(MetaObject obj, float[] metaDistances, float distThreshold) {
        MetaObjectCophirKeywords castObj = (MetaObjectCophirKeywords) obj;

        float rtv = 0;

        if (colorLayout != null && castObj.colorLayout != null) {
            if (metaDistances != null) {
                metaDistances[0] = colorLayout.getDistanceImpl(castObj.colorLayout, distThreshold) / 300.0f;
                rtv += metaDistances[0] * weights[1];
            } else {
                rtv += colorLayout.getDistanceImpl(castObj.colorLayout, distThreshold) * weights[1] / 300.0;
            }
        }

        if (colorStructure != null && castObj.colorStructure != null) {
            if (metaDistances != null) {
                metaDistances[1] = colorStructure.getDistanceImpl(castObj.colorStructure, distThreshold) / 40.0f / 255.0f;
                rtv += metaDistances[1] * weights[2];
            } else {
                rtv += colorStructure.getDistanceImpl(castObj.colorStructure, distThreshold) * weights[2] / 40.0 / 255.0;
            }
        }

        if (edgeHistogram != null && castObj.edgeHistogram != null) {
            if (metaDistances != null) {
                metaDistances[2] = edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold) / 68.0f;
                rtv += metaDistances[2] * weights[3];
            } else {
                rtv += edgeHistogram.getDistanceImpl(castObj.edgeHistogram, distThreshold) * weights[3] / 68.0;
            }
        }

        if (homogeneousTexture != null && castObj.homogeneousTexture != null) {
            if (metaDistances != null) {
                metaDistances[3] = homogeneousTexture.getDistanceImpl(castObj.homogeneousTexture, distThreshold) / 25.0f;
                rtv += metaDistances[3] * weights[4];
            } else {
                rtv += homogeneousTexture.getDistanceImpl(castObj.homogeneousTexture, distThreshold) * weights[4] / 25.0;
            }
        }

        if (scalableColor != null && castObj.scalableColor != null) {
            if (metaDistances != null) {
                metaDistances[4] = scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold) / 3000.0f;
                rtv += metaDistances[4] * weights[5];
            } else {
                rtv += scalableColor.getDistanceImpl(castObj.scalableColor, distThreshold) * weights[5] / 3000.0;
            }
        }

        return rtv;
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of MetaObjectCophirKeywords loaded from binary input buffer.
     *
     * @param input the buffer to read the MetaObjectCophirKeywords from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected MetaObjectCophirKeywords(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        location = serializator.readObject(input, ObjectGPSCoordinate.class);
        colorLayout = serializator.readObject(input, ObjectColorLayout.class);
        colorStructure = serializator.readObject(input, ObjectShortVectorL1.class);
        edgeHistogram = serializator.readObject(input, ObjectVectorEdgecomp.class);
        homogeneousTexture = serializator.readObject(input, ObjectHomogeneousTexture.class);
        scalableColor = serializator.readObject(input, ObjectIntVectorL1.class);
        keywords = serializator.readObject(input, ObjectIntMultiVectorJaccard.class);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator);
        size += serializator.write(output, location);
        size += serializator.write(output, colorLayout);
        size += serializator.write(output, colorStructure);
        size += serializator.write(output, edgeHistogram);
        size += serializator.write(output, homogeneousTexture);
        size += serializator.write(output, scalableColor);
        size += serializator.write(output, keywords);
        return size;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator);
        size += serializator.getBinarySize(location);
        size += serializator.getBinarySize(colorLayout);
        size += serializator.getBinarySize(colorStructure);
        size += serializator.getBinarySize(edgeHistogram);
        size += serializator.getBinarySize(homogeneousTexture);
        size += serializator.getBinarySize(scalableColor);
        size += serializator.getBinarySize(keywords);
        return size;
    }
}

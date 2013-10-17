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
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import messif.objects.DistanceFunction;
import messif.objects.LocalAbstractObject;
import messif.objects.MetaObject;
import messif.objects.impl.ObjectIntMultiVector.WeightProvider;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;
import messif.objects.text.StringFieldDataProvider;

/**
 * Implementation of the object that encapsulates CoPhIR data including keywords.
 * The five MPEG-7 descriptors, GPS coordinates, and keyword identifiers
 * (from title, description, and tags) are stored in the object.
 * The distance function uses a {@link #getWeights() weighted} sum of the distances
 * of the respective visual descriptors.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MetaObjectCophirKeywords extends MetaObjectArrayWeightedSum implements StringFieldDataProvider {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** The list of supported names ******************//

    /** The list of the names for the encapsulated objects */
    private static final String[] descriptorNames = {
        "Location", "ColorLayoutType", "ColorStructureType", "EdgeHistogramType", "HomogeneousTextureType",
        "ScalableColorType", "KeyWordsType"
    };

    /** The list of keyword field names, i.e. the fields in KeyWordsType descriptor and {@link #keywordStrings} attribute */
    private static final List<String> keywordFields = Arrays.asList("title", "tag", "description");

    /** Weights for the default distance function (location and keywords are not used) */
    private static final float[] weights = { 0.0f / 20000f, 1.5f / 300f, 2.5f / 40f / 255f, 4.5f / 68f, 0.5f / 25f, 2.5f / 3000f, 0.0f };

    /** Maximal distance */
    private static final float maxDistance = 16;

    /** Array of keyword strings that are represented by the KeyWordsType identifiers */
    protected transient String[] keywordStrings;


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
     * @param keywordStrings the keyword strings that are represented by the keyword identifiers  multi-vector
     */
    public MetaObjectCophirKeywords(String locatorURI, ObjectGPSCoordinate location, ObjectColorLayout colorLayout, ObjectShortVectorL1 colorStructure, ObjectVectorEdgecomp edgeHistogram, ObjectHomogeneousTexture homogeneousTexture, ObjectIntVectorL1 scalableColor, ObjectIntMultiVectorCosine keywords, String[] keywordStrings) {
        super(locatorURI, location, colorLayout, colorStructure, edgeHistogram, homogeneousTexture, scalableColor, keywords);
        this.keywordStrings = keywordStrings;
    }

    /**
     * Creates a new instance of MetaObjectCophirKeywords with the given encapsulated objects.
     * @param locatorURI the locator URI for the new object
     * @param objects the encapsulated objects to add (keys should match the {@link #descriptorNames})
     * @param keywordStrings the keyword strings that are represented by the keyword identifiers  multi-vector
     */
    public MetaObjectCophirKeywords(String locatorURI, Map<String, ? extends LocalAbstractObject> objects, String[] keywordStrings) {
        super(locatorURI, objects, descriptorNames);
        if (keywordStrings != null) {
            this.keywordStrings = new String[keywordStrings.length];
            // Strip invalid characters so that serialization is not garbled
            for (int i = 0; i < this.keywordStrings.length; i++) {
                if (keywordStrings[i] != null)
                    this.keywordStrings[i] = keywordStrings[i].replaceAll("[\\r\\n;]", " ");
            }
        } else {
            this.keywordStrings = null;
        }
    }

    /**
     * Creates a new instance of MetaObjectCophirKeywords with the given encapsulated objects.
     * @param locatorURI the locator URI for the new object
     * @param objects the encapsulated objects to add (keys should match the {@link #descriptorNames})
     */
    public MetaObjectCophirKeywords(String locatorURI, Map<String, ? extends LocalAbstractObject> objects) {
        this(locatorURI, objects, null);
    }

    /**
     * Creates a new instance of MetaObjectCophirKeywords from another {@link MetaObject}.
     * @param metaObject the meta object the encapsulated objects of which to add (keys should match the {@link #descriptorNames})
     */
    public MetaObjectCophirKeywords(MetaObject metaObject) {
        this(metaObject.getLocatorURI(), metaObject.getObjectMap(), metaObject instanceof MetaObjectCophirKeywords ? ((MetaObjectCophirKeywords)metaObject).keywordStrings : null);
    }

    /**
     * Creates a new instance of MetaObjectCophirKeywords from a stream.
     * 
     * @param stream text stream to read the data from
     * @param wordLines number of lines the keyword identifiers are stored on (zero, one, or multiple)
     * @param keywordStringLines number of lines the keyword strings are stored on (zero, one, or multiple)
     * @throws IOException when an error appears during reading from given stream;
     *         {@link java.io.EOFException} is thrown when end-of-file of the given stream is reached
     * @throws NumberFormatException when the line with the descriptor is not valid
     */
    @SuppressWarnings("unchecked")
    public MetaObjectCophirKeywords(BufferedReader stream, int wordLines, int keywordStringLines) throws IOException, NumberFormatException {
        super(stream, ObjectGPSCoordinate.class, ObjectColorLayout.class, ObjectShortVectorL1.class,
                ObjectVectorEdgecomp.class, ObjectHomogeneousTexture.class, ObjectIntVectorL1.class, null);
        // Load keywords (special, not loaded automatically by the super constructor)
        switch (wordLines) {
            case 0:
                objects[6] = null;
                break;
            case 1:
                objects[6] = new ObjectIntMultiVectorCosine(stream);
                break;
            default:
                objects[6] = new ObjectIntMultiVectorCosine(stream, wordLines);
        }
        // Load keyword strings (special, not loaded automatically by the super constructor)
        switch (keywordStringLines) {
            case 0:
                keywordStrings = null;
                break;
            case 1:
                keywordStrings = stream.readLine().split(";", -1);
                break;
            default:
                keywordStrings = new String[keywordStringLines];
                for (int i = 0; i < keywordStringLines; i++)
                    keywordStrings[i] = stream.readLine();
        }
        if (keywordStrings != null && keywordStrings.length != keywordFields.size())
            throw new IOException("Keyword strings load failed, expected " + keywordFields.size() + " keyword fields but got " + keywordStrings.length);
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
        this(stream, 1, 0);
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
     * Returns the encapsulated "KeyWordsType" object.
     * @return the encapsulated "KeyWordsType" object
     */
    public ObjectIntMultiVectorCosine getKeywordsObject() {
        return (ObjectIntMultiVectorCosine)getObject(descriptorNames[6]);
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

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        super.writeData(stream);
        if (keywordStrings != null) {
            for (int i = 0; i < keywordStrings.length; i++) {
                if (keywordStrings[i] != null)
                    stream.write(keywordStrings[i].getBytes());
                stream.write(i == keywordStrings.length - 1 ? '\n' : ';');
            }
        }
    }

    @Override
    public String getStringData() {
        if (keywordStrings == null)
            return null;
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < keywordStrings.length; i++) {
            str.append(keywordStrings[i]);
            str.append('\n');
        }
        return str.toString();
    }

    @Override
    public Collection<String> getStringDataFields() {
        return keywordFields;
    }

    @Override
    public String getStringData(String fieldName) throws IllegalArgumentException {
        if (keywordStrings == null)
            return null;
        int pos = keywordFields.indexOf(fieldName);
        if (pos < 0 || pos >= keywordStrings.length)
            throw new IllegalArgumentException("Unknown field '" + fieldName + "'");
        return keywordStrings[pos];
    }

    /**
     * Object that holds only keywords and measures the distance as the
     * weighted Cosine distance with weights based on tf-idf algorithm. Note
     * that the other object for the distance must be {@link MetaObjectCophirKeywords}.
     */
    public static class MetaObjectCophirKeywordsDistCosine extends MetaObjectCophirKeywords {
        /** Class id for serialization. */
        private static final long serialVersionUID = 2L;

        //****************** Attributes ******************//

        /** Weight for combining the keywords distance with the visual descriptors distance */
        private final Float keywordsWeight;
        /** Internal keyword weight provider based on tf-idf */
        private final WeightProvider kwWeightProvider;


        //****************** Constructor ******************//

        /**
         * Creates a new instance of MetaObjectCophirKeywordsDistCosine from the given {@link MetaObjectCophirKeywords}.
         * The locator and the encapsulated objects from the source {@code object} are
         * taken.
         *
         * @param object the source metaobject from which to get the data
         * @param keywordsWeight the weight for combining the keywords distance with the visual descriptors distance,
         *          if <tt>null</tt>, only the text distance is used
         * @param kwWeightProvider the weight provider for different layers of keywords (title, etc.)
         */
        public MetaObjectCophirKeywordsDistCosine(MetaObjectCophirKeywords object, Float keywordsWeight, WeightProvider kwWeightProvider) {
            super(object);
            this.keywordsWeight = keywordsWeight;
            this.kwWeightProvider = kwWeightProvider;
        }


        //****************** Distance function ******************//

        @Override
        protected float getDistanceImpl(LocalAbstractObject obj, float[] metaDistances, float distThreshold) {
            try {
                float distance = ObjectIntMultiVectorCosine.getWeightedCosineDistance(getKeywordsObject(), kwWeightProvider, ((MetaObjectCophirKeywords)obj).getKeywordsObject(), kwWeightProvider);
                if (keywordsWeight != null)
                    distance = super.getDistanceImpl(obj, metaDistances, distThreshold) + keywordsWeight * distance;
                return distance;
            } catch (RuntimeException e) {
                throw new IllegalStateException("Error computing distance between " + this + " and " + obj + ": " + e, e);
            }
        }
    }

    /**
     * Returns this object encapsulated in {@link MetaObjectCophirKeywordsWithTKStrings}.
     * @return this object encapsulated in {@link MetaObjectCophirKeywordsWithTKStrings}
     */
    public MetaObjectCophirKeywordsWithTKStrings wrapWithKwStringSerialization() {
        return new MetaObjectCophirKeywordsWithTKStrings(this);
    }

    /**
     * Returns this object only with title encapsulated in {@link MetaObjectCophirKeywordsWithTKStrings}.
     * @return this object only with title encapsulated in {@link MetaObjectCophirKeywordsWithTKStrings}
     */
    public MetaObjectCophirKeywordsWithTKStrings wrapWithTitleOnlySerialization() {
        Map<String, LocalAbstractObject> objectMap = new HashMap<String, LocalAbstractObject>(getObjectMap());
        objectMap.put(descriptorNames[6], new ObjectIntMultiVectorCosine(getKeywordsObject().getVectorData(0), new int[0], new int[0]));
        return new MetaObjectCophirKeywordsWithTKStrings(getLocatorURI(), objectMap, new String[] { keywordStrings[0], "", "" });
    }

    /**
     * Extension of the MetaObjectCophirKeywords that preserves also the keyword
     * strings in both binary and Java serialization.
     */
    public static class MetaObjectCophirKeywordsWithTKStrings extends MetaObjectCophirKeywords {
        /** Class id for serialization. */
        private static final long serialVersionUID = 1L;

        /**
         * Creates a new instance of MetaObjectCophirKeywordsWithTKStrings from the given {@link MetaObjectCophirKeywords}.
         * The locator, the attributes and the encapsulated objects from the source {@code object} are
         * taken.
         *
         * @param object the source metaobject from which to get the data
         */
        public MetaObjectCophirKeywordsWithTKStrings(MetaObjectCophirKeywords object) {
            super(object);
        }

        /**
         * Creates a new instance of MetaObjectCophirKeywordsWithTKStrings with the given encapsulated objects.
         *
         * @param locatorURI the locator URI for the new object
         * @param objects the encapsulated objects to add (keys should match the {@link #descriptorNames})
         * @param keywordStrings the keyword strings that are represented by the keyword identifiers multi-vector
         */
        public MetaObjectCophirKeywordsWithTKStrings(String locatorURI, Map<String, ? extends LocalAbstractObject> objects, String[] keywordStrings) {
            super(locatorURI, objects, keywordStrings);
        }

        /**
         * Creates a new instance of MetaObjectCophirKeywordsWithTKStrings from a stream.
         * The keyword identifiers are read from a single line.
         * @param stream text stream to read the data from
         * @throws IOException when an error appears during reading from given stream;
         *         {@link java.io.EOFException} is thrown when end-of-file of the given stream is reached
         * @throws NumberFormatException when the line with the descriptor is not valid
         */
        public MetaObjectCophirKeywordsWithTKStrings(BufferedReader stream) throws IOException {
            super(stream);
        }

        /**
         * Creates a new instance of MetaObjectCophirKeywordsWithTKStrings from a stream.
         * 
         * @param stream text stream to read the data from
         * @param wordLines number of lines the keyword identifiers are stored on (zero, one, or multiple)
         * @param keywordStringLines number of lines the keyword strings are stored on (zero, one, or multiple)
         * @throws IOException when an error appears during reading from given stream;
         *         {@link java.io.EOFException} is thrown when end-of-file of the given stream is reached
         * @throws NumberFormatException when the line with the descriptor is not valid
         */
        public MetaObjectCophirKeywordsWithTKStrings(BufferedReader stream, int wordLines, int keywordStringLines) throws IOException, NumberFormatException {
            super(stream, wordLines, keywordStringLines);
        }

        /**
         * Creates a new instance of MetaObjectCophirKeywordsWithTKStrings loaded from binary input buffer.
         *
         * @param input the buffer to read the MetaObjectProfiSCTWithTKStrings from
         * @param serializator the serializator used to write objects
         * @throws IOException if there was an I/O error reading from the buffer
         */
        protected MetaObjectCophirKeywordsWithTKStrings(BinaryInput input, BinarySerializator serializator) throws IOException {
            super(input, serializator);
            keywordStrings = serializator.readStringArray(input);
        }

        @Override
        public int getBinarySize(BinarySerializator serializator) {
            int size = super.getBinarySize(serializator);
            size += serializator.getBinarySize(keywordStrings);
            return size;
        }

        @Override
        public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
            int size = super.binarySerialize(output, serializator);
            size += serializator.write(output, keywordStrings);
            return size;
        }

        /**
         * Java native serialization method.
         * @param out the stream to serialize this object to
         * @throws IOException if there was an error writing to the stream {@code out}
         */
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeObject(keywordStrings);
        }

        /**
         * Java native serialization method.
         * @param in the stream to deserialize this object from
         * @throws IOException if there was an error reading from the stream {@code in}
         * @throws ClassNotFoundException if an unknown class was encountered in the stream 
         */
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            keywordStrings = (String[])in.readObject();
        }
    }

    /**
     * Class for distance functions that compute distances on keyword vectors
     * of two {@link MetaObjectCophirKeywords}}s using weighted Cosine distance.
     */
    public static class CophirWeightedCosineDistanceFunction implements DistanceFunction<MetaObjectCophirKeywords>, Serializable {
        /** Class id for serialization. */
        private static final long serialVersionUID = 1L;

        /** Weight provider for the first object */
        private final WeightProvider weightProviderO1;
        /** Weight provider for the second object */
        private final WeightProvider weightProviderO2;

        /**
         * Creates a new instance of weighted Cosine distance function.
         * @param weightProviderO1 the weight provider for the first object
         * @param weightProviderO2 the weight provider for the second object
         * @throws NullPointerException if either {@code weightProviderO1} or {@code weightProviderO2} is <tt>null</tt>
         */
        public CophirWeightedCosineDistanceFunction(WeightProvider weightProviderO1, WeightProvider weightProviderO2) throws NullPointerException {
            if (weightProviderO1 == null || weightProviderO2 == null)
                throw new NullPointerException();
            this.weightProviderO1 = weightProviderO1;
            this.weightProviderO2 = weightProviderO2;
        }

        /**
         * Returns the encapsulated weight provider for the first object.
         * @return the encapsulated weight provider for the first object
         */
        public WeightProvider getWeightProviderO1() {
            return weightProviderO1;
        }

        /**
         * Returns the encapsulated weight provider for the second object.
         * @return the encapsulated weight provider for the second object
         */
        public WeightProvider getWeightProviderO2() {
            return weightProviderO2;
        }

        @Override
        public float getDistance(MetaObjectCophirKeywords o1, MetaObjectCophirKeywords o2) {
            return ObjectIntMultiVectorCosine.getWeightedCosineDistance(o1.getKeywordsObject(), weightProviderO1, o2.getKeywordsObject(), weightProviderO2);
        }

        @Override
        public Class<? extends MetaObjectCophirKeywords> getDistanceObjectClass() {
            return MetaObjectCophirKeywords.class;
        }
    }
}

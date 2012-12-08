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
import messif.objects.DistanceFunction;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 * Implements the Jaccard coefficient distance function.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectIntMultiVectorJaccard extends ObjectIntMultiVector implements Serializable {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectIntMultiVectorJaccard.
     * If {@code forceSort} is <tt>false</tt>, the provided data are expected to be sorted!
     *
     * @param data the data content of the new object
     * @param forceSort if <tt>false</tt>, the data is expected to be sorted
     */
    public ObjectIntMultiVectorJaccard(int[][] data, boolean forceSort) {
        super(data);
        if (forceSort)
            sortData();
    }

    /**
     * Creates a new instance of ObjectIntMultiVectorJaccard.
     * @param data the data content of the new object
     */
    public ObjectIntMultiVectorJaccard(int[]... data) {
        this(data, true);
    }

    /**
     * Creates a new instance of randomly generated ObjectIntMultiVectorJaccard.
     * Content will be generated using normal distribution of random numbers from interval
     * [0;1).
     *
     * @param arrays the number of vector data arrays to create
     * @param dimension number of dimensions to generate
     */
    public ObjectIntMultiVectorJaccard(int arrays, int dimension) {
        super(arrays, dimension);
    }


    /**
     * Creates a new instance of ObjectIntMultiVectorJaccard from stream - it expects that the data is already sorted!
     * The data are stored as several lines of comma-separated integers.
     * @param stream text stream to read the data from
     * @param arrays number of arrays to read from the stream
     * @throws IOException when an error appears during reading from given stream.
     *  or  EOFException when end-of-file of the given stream is reached.
     * @throws NumberFormatException when the line read from given stream does
     * not consist of comma-separated or space-separated numbers.
     */
    public ObjectIntMultiVectorJaccard(BufferedReader stream, int arrays) throws IOException, NumberFormatException {
        super(stream, arrays);
    }

    /**
     * Creates a new instance of ObjectIntMultiVectorJaccard from stream - it expects that the data is already sorted!
     * The data are stored as a single line with semicolon separated lists of comma-separated integers.
     * @param stream text stream to read the data from
     * @throws IOException when an error appears during reading from given stream.
     *  or  EOFException when end-of-file of the given stream is reached.
     * @throws NumberFormatException when the line read from given stream does
     * not consist of comma-separated or space-separated numbers.
     */
    public ObjectIntMultiVectorJaccard(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }

    /**
     * Creates a new instance of ObjectIntMultiVectorJaccard loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectIntVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    public ObjectIntMultiVectorJaccard(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


    //****************** Distance function ******************//

    /**
     * Implements the Jaccard coefficient distance function.
     * @return 0, if both sets are empty; 1, if only one set is empty; Jaccard distance otherwise
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        SortedDataIterator iterator = getSortedIterator();
        SortedDataIterator objIterator = ((ObjectIntMultiVectorJaccard)obj).getSortedIterator();

        float intersectCount = 0;
        for (SDIteratorIntersectionResult intersect = iterator.intersect(objIterator); intersect.isIntersecting(); intersect = iterator.intersect(objIterator)) {
            if (intersect.isThisIntersecting())
                intersectCount++;
        }

        return 1f - intersectCount / (getDimensionality() + ((ObjectIntMultiVectorJaccard)obj).getDimensionality() - intersectCount);
    }


    //****************** Weighted Jaccard distance function ******************//

    /**
     * Computes a distance between two {@link ObjectIntMultiVector}s using
     * a non-metric weighted Jaccard coefficient.
     * @param o1 the object to compute distance from
     * @param weightProviderO1 the weight provider for object {@code o1}
     * @param o2 the object to compute distance to
     * @param weightProviderO2 the weight provider for object {@code o2}
     * @return the non-metric weighted Jaccard distance between object {@code o1} and object {@code o2}
     * @throws NullPointerException if either {@code weightProviderO1} or {@code weightProviderO2} is <tt>null</tt>
     */
    public static float getWeightedJaccardDistance(ObjectIntMultiVector o1, WeightProvider weightProviderO1, ObjectIntMultiVector o2, WeightProvider weightProviderO2) throws NullPointerException {
        SortedDataIterator o1Iterator = o1.getSortedIterator();
        SortedDataIterator o2Iterator = o2.getSortedIterator();

        float intersectWeight = 0;
        while (true) {
            SDIteratorIntersectionResult intersect = o1Iterator.intersect(o2Iterator);
            if (!intersect.isIntersecting())
                break;
            if (intersect.isThisIntersecting())
                intersectWeight += weightProviderO1.getWeight(o1Iterator);
            if (intersect.isArgumentIntersecting())
                intersectWeight += weightProviderO2.getWeight(o2Iterator);
        }

        float sumWeight = weightProviderO1.getWeightSum(o1) + weightProviderO2.getWeightSum(o2);
        if (Math.abs(intersectWeight - sumWeight) < 0.0001) // This is needed since the sum can be different when different number order is used
            return 0;
        return (intersectWeight == 0 && sumWeight == 0) ? 0 : (1f - intersectWeight / sumWeight);
    }

    /**
     * Implements a non-metric weighted Jaccard coefficient distance function.
     * If either {@code weightProviderThis} or {@code weightProviderObj} is <tt>null</tt>,
     * the normal Jaccard {@link #getDistanceImpl(messif.objects.LocalAbstractObject, float) distance} is returned.
     * @param obj the object to compute distance to
     * @param weightProviderThis the weight provider for this object
     * @param weightProviderObj the weight provider for {@code obj}
     * @return the non-metric weighted Jaccard distance between this object and the given {@code obj}
     */
    public float getWeightedJaccardDistance(ObjectIntMultiVector obj, WeightProvider weightProviderThis, WeightProvider weightProviderObj) {
        // If weights are not provided, fall back to non-weighted distance
        if (weightProviderThis == null || weightProviderObj == null)
            return getDistanceImpl(obj, MAX_DISTANCE);
        return getWeightedJaccardDistance(this, weightProviderThis, obj, weightProviderObj);
    }

    /**
     * Class for distance functions that compute distances between two
     * {@link ObjectIntMultiVector}s using a non-metric weighted Jaccard coefficient.
     */
    public static class WeightedJaccardDistanceFunction implements DistanceFunction<ObjectIntMultiVector>, Serializable {
        /** Class id for serialization. */
        private static final long serialVersionUID = 1L;

        /** Weight provider for the first object */
        private final WeightProvider weightProviderO1;
        /** Weight provider for the second object */
        private final WeightProvider weightProviderO2;

        /**
         * Creates a new instance of weighted Jaccard distance function.
         * @param weightProviderO1 the weight provider for the first object
         * @param weightProviderO2 the weight provider for the second object
         * @throws NullPointerException if either {@code weightProviderO1} or {@code weightProviderO2} is <tt>null</tt>
         */
        public WeightedJaccardDistanceFunction(WeightProvider weightProviderO1, WeightProvider weightProviderO2) throws NullPointerException {
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
        public float getDistance(ObjectIntMultiVector o1, ObjectIntMultiVector o2) {
            return getWeightedJaccardDistance(o1, weightProviderO1, o2, weightProviderO2);
        }

        @Override
        public Class<? extends ObjectIntMultiVector> getDistanceObjectClass() {
            return ObjectIntMultiVector.class;
        }
    }

}

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
 * Implements the Cosine distance function.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectIntMultiVectorCosine extends ObjectIntMultiVector implements Serializable {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectIntMultiVectorCosine.
     * If {@code forceSort} is <tt>false</tt>, the provided data are expected to be sorted!
     *
     * @param data the data content of the new object
     * @param forceSort if <tt>false</tt>, the data is expected to be sorted
     */
    public ObjectIntMultiVectorCosine(int[][] data, boolean forceSort) {
        super(data);
        if (forceSort)
            sortData();
    }

    /**
     * Creates a new instance of ObjectIntMultiVectorCosine.
     * @param data the data content of the new object
     */
    public ObjectIntMultiVectorCosine(int[]... data) {
        this(data, true);
    }

    /**
     * Creates a new instance of randomly generated ObjectIntMultiVectorCosine.
     * Content will be generated using normal distribution of random numbers from interval
     * [0;1).
     *
     * @param arrays the number of vector data arrays to create
     * @param dimension number of dimensions to generate
     */
    public ObjectIntMultiVectorCosine(int arrays, int dimension) {
        super(arrays, dimension);
    }


    /**
     * Creates a new instance of ObjectIntMultiVectorCosine from stream - it expects that the data is already sorted!
     * The data are stored as several lines of comma-separated integers.
     * @param stream text stream to read the data from
     * @param arrays number of arrays to read from the stream
     * @throws IOException when an error appears during reading from given stream.
     *  or  EOFException when end-of-file of the given stream is reached.
     * @throws NumberFormatException when the line read from given stream does
     * not consist of comma-separated or space-separated numbers.
     */
    public ObjectIntMultiVectorCosine(BufferedReader stream, int arrays) throws IOException, NumberFormatException {
        super(stream, arrays);
    }

    /**
     * Creates a new instance of ObjectIntMultiVectorCosine from stream - it expects that the data is already sorted!
     * The data are stored as a single line with semicolon separated lists of comma-separated integers.
     * @param stream text stream to read the data from
     * @throws IOException when an error appears during reading from given stream.
     *  or  EOFException when end-of-file of the given stream is reached.
     * @throws NumberFormatException when the line read from given stream does
     * not consist of comma-separated or space-separated numbers.
     */
    public ObjectIntMultiVectorCosine(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }

    /**
     * Creates a new instance of ObjectIntMultiVectorCosine loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectIntVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    public ObjectIntMultiVectorCosine(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


    //****************** Distance function ******************//

    /**
     * Implements the Cosine distance function.
     * @return 0, if both sets are empty; 1, if only one set is empty; Cosine distance otherwise
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        return getWeightedCosineDistance(this, null, (ObjectIntMultiVectorCosine)obj, null);
    }


    //****************** Weighted Cosine distance function ******************//

    /**
     * Returns the weight of the current sorted iterator value multiplied by the square-root of the value frequency.
     * @param iterator the iterator with the value
     * @param weightProvider the provider of the weights; if <tt>null</tt>, the weight equals the frequency
     * @return the frequency multiplied weight
     */
    private static double getWeightWithFrequency(SortedDataIterator iterator, WeightProvider weightProvider) {
        return (weightProvider == null ? 1 : weightProvider.getWeight(iterator)) * (Math.sqrt(1 + iterator.skipDuplicates()));
    }

    /**
     * Computes a distance between two {@link ObjectIntMultiVector}s using
     * a metric weighted cosine distance. Specifically, the dot product of
     * the intersecting weights divided by the multiplication of their norms
     * is returned.
     * @param o1 the object to compute distance from
     * @param weightProviderO1 the weight provider for object {@code o1}
     * @param o2 the object to compute distance to
     * @param weightProviderO2 the weight provider for object {@code o2}
     * @return the non-metric weighted Jaccard distance between object {@code o1} and object {@code o2}
     * @throws NullPointerException if either {@code weightProviderO1} or {@code weightProviderO2} is <tt>null</tt>
     */
    public static float getWeightedCosineDistance(ObjectIntMultiVector o1, WeightProvider weightProviderO1, ObjectIntMultiVector o2, WeightProvider weightProviderO2) throws NullPointerException {
        SortedDataIterator o1Iterator = o1.getSortedIterator();
        SortedDataIterator o2Iterator = o2.getSortedIterator();

        // If no data in either iterator, return max distance
        if (!o1Iterator.hasNext() || !o2Iterator.hasNext())
            return 1;

        // Get first values
        int o1Value = o1Iterator.nextInt();
        int o2Value = o2Iterator.nextInt();
        double weight1 = getWeightWithFrequency(o1Iterator, weightProviderO1);
        double weight2 = getWeightWithFrequency(o2Iterator, weightProviderO2);

        // Initialize computing variables
        double dotProduct = 0;
        double sumSqrWeight1 = weight1 * weight1;
        double sumSqrWeight2 = weight2 * weight2;

        // Iterate to find all intersections (for dot product)
        for (;;) {
            if (o1Value == o2Value)
                dotProduct += weight1 * weight2;
            if (o1Value <= o2Value) {
                if (!o1Iterator.hasNext()) { // Compute the other sumSqWeight
                    while (o2Iterator.hasNext()) {
                        o2Iterator.nextInt();
                        weight2 = getWeightWithFrequency(o2Iterator, weightProviderO2);
                        sumSqrWeight2 += weight2 * weight2;
                    }
                    break;
                }
                o1Value = o1Iterator.nextInt();
                weight1 = getWeightWithFrequency(o1Iterator, weightProviderO1);
                sumSqrWeight1 += weight1 * weight1;
            } else {
                if (!o2Iterator.hasNext()){ // Compute the other sumSqWeight
                    while (o1Iterator.hasNext()) {
                        o1Iterator.nextInt();
                        weight1 = getWeightWithFrequency(o1Iterator, weightProviderO1);
                        sumSqrWeight1 += weight1 * weight1;
                    }
                    break;
                }
                o2Value = o2Iterator.nextInt();
                weight2 = getWeightWithFrequency(o2Iterator, weightProviderO2);
                sumSqrWeight2 += weight2 * weight2;
            }
        }

        double distance = 1.0 - dotProduct / (Math.sqrt(sumSqrWeight1) * Math.sqrt(sumSqrWeight2));
        // Compensate for float sizing error
        if (distance < 0.0000001)
            return 0;
        return (float)distance;
    }

    /**
     * Class for distance functions that compute distances between two
     * {@link ObjectIntMultiVector}s using weighted Cosine distance.
     */
    public static class WeightedCosineDistanceFunction implements DistanceFunction<ObjectIntMultiVector>, Serializable {
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
        public WeightedCosineDistanceFunction(WeightProvider weightProviderO1, WeightProvider weightProviderO2) throws NullPointerException {
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
            return getWeightedCosineDistance(o1, weightProviderO1, o2, weightProviderO2);
        }

        @Override
        public Class<? extends ObjectIntMultiVector> getDistanceObjectClass() {
            return ObjectIntMultiVector.class;
        }
    }
}

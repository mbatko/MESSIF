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
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 * Implements the Jaccard coeficient distance function. The data is
 * expected to be sorted and without duplicities!
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectIntDualVectorJaccard extends ObjectIntDualVector implements Serializable {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectIntDualVectorJaccard.
     * @param data integer vector of the primary data
     * @param data2 integer vector of the secondary data 
     * @param forceSort if <tt>false</tt>, the data is expected to be sorted
     */
    public ObjectIntDualVectorJaccard(int[] data, int[] data2, boolean forceSort) {
        super(data, data2, forceSort);
    }

    /**
     * Creates a new instance of ObjectIntDualVectorJaccard.
     * @param data integer vector of the primary data
     * @param data2 integer vector of the secondary data
     */
    public ObjectIntDualVectorJaccard(int[] data, int[] data2) {
        super(data, data2);
    }

    /**
     * Creates a new instance of randomly generated ObjectIntDualVectorJaccard.
     * @param dimension the dimensionality of the primary data
     * @param dimension2 the dimensionality of the secondary data
     */
    public ObjectIntDualVectorJaccard(int dimension, int dimension2) {
        super(dimension, dimension2);
    }


    /**
     * Creates a new instance of ObjectIntDualVectorJaccard from stream - it expects that the data is already sorted!
     * @param stream text stream to read the data from
     * @throws IOException when an error appears during reading from given stream.
     *  or  EOFException when eof of the given stream is reached.
     * @throws NumberFormatException when the line read from given stream does
     * not consist of comma-separated or space-separated numbers.
     */
    public ObjectIntDualVectorJaccard(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }

    /**
     * Creates a new instance of ObjectIntDualVectorJaccard loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectIntVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    public ObjectIntDualVectorJaccard(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


    //****************** Distance function ******************//

    /**
     * Implements the Jaccard coeficient distance function. The data is
     *  expected to be sorted and without duplicites!
     * @return 0, if both sets are emtpy; 1, if only one set is empty; Jaccard distance otherwise
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        SortedDataIterator iterator = getSortedIterator();
        SortedDataIterator objIterator = ((ObjectIntDualVectorJaccard)obj).getSortedIterator();

        float intersectCount = 0;
        while (iterator.intersect(objIterator))
            intersectCount++;

        return 1f - intersectCount / (getDimensionality() + ((ObjectIntDualVectorJaccard)obj).getDimensionality() - intersectCount);
    }


    //****************** Weighted Jaccard distance function ******************//

    /**
     * Interface for providing the weights for the Jaccard distance function.
     */
    public static interface WeightProvider {
        /**
         * Returns the weight for the item that the {@code iterator} points to.
         * @param iterator this iterator's current object weight is to be retrieved
         * @return the weight for the current item of the iterator
         */
        public abstract float getWeight(SortedDataIterator iterator);
        /**
         * Returns the sum of all weights for the given object.
         * Note that this value must be consistent with {@link #getWeight(messif.objects.impl.ObjectIntDualVector.SortedDataIterator)},
         * i.e. the returned sum is the sum of the weight retrieved by iterating over
         * all items from the {@code obj.getSortedIterator()}.
         * @param obj the object for which the weights are given
         * @return the total sum of all weights
         */
        public abstract float getWeightSum(ObjectIntDualVector obj);
    }

    /**
     * Implements a non-metric weighted Jaccard coeficient distance function.
     * @param obj the object to compute distance to
     * @param weightProviderThis the weight provider for this object
     * @param weightProviderObj the weight provider for {@code obj}
     * @return the non-metric weighted Jaccard distance between this object and the given {@code obj}
     */
    public float getWeightedDistance(ObjectIntDualVector obj, WeightProvider weightProviderThis, WeightProvider weightProviderObj) {
        SortedDataIterator iterator = getSortedIterator();
        SortedDataIterator objIterator = obj.getSortedIterator();

        float intersectWeight = 0;
        while (iterator.intersect(objIterator)) {
            intersectWeight += weightProviderThis.getWeight(iterator);
            intersectWeight += weightProviderObj.getWeight(objIterator);
        }

        return 1f - intersectWeight / (weightProviderThis.getWeightSum(this) + weightProviderObj.getWeightSum(obj) - intersectWeight);
    }

    /**
     * Implementation of {@link WeightProvider} that has two weights, one for the
     * primary and one for the secondary data array of the {@link ObjectIntDualVector}.
     */
    public static class DualWeightProvider implements WeightProvider {
        /** Weight for the items in the primary data array */
        private final float primaryWeight;
        /** Weight for the items in the secondary data array */
        private final float secondaryWeight;

        /**
         * Creates a new instance of DualWeightProvider with the two given weights.
         * @param primaryWeight the weight for the items in the primary data array
         * @param secondaryWeight the weight for the items in the secondary data array
         */
        public DualWeightProvider(float primaryWeight, float secondaryWeight) {
            this.primaryWeight = primaryWeight;
            this.secondaryWeight = secondaryWeight;
        }

        public float getWeight(SortedDataIterator iterator) {
            return iterator.isCurrentPrimary() ? primaryWeight : secondaryWeight;
        }

        public float getWeightSum(ObjectIntDualVector obj) {
            return obj.data.length * primaryWeight + obj.data2.length * secondaryWeight;
        }
    }

    /**
     * Implementation of {@link WeightProvider} that has given weights for the
     * primary and one for the secondary data of the {@link ObjectIntDualVector}.
     * Note that the number of weights <em>must</em> be equal to the
     * number of data items in the primary or the secondary data of the
     * {@link ObjectIntDualVector} respectively.
     */
    public static class ArrayWeightProvider implements WeightProvider {
        /** Weights for the items in the primary data array */
        private final float[] primaryWeight;
        /** Weights for the items in the secondary data array */
        private final float[] secondaryWeight;
        /** Sum of the weights in primaryWeight and secondaryWeight */
        private final float weightSum;

        /**
         * Creates a new instance of ArrayWeightProvider with the two given weights.
         * @param primaryWeight the weights for the items in the primary data array
         * @param secondaryWeight the weights for the items in the secondary data array
         */
        public ArrayWeightProvider(float[] primaryWeight, float[] secondaryWeight) {
            this.primaryWeight = primaryWeight;
            this.secondaryWeight = secondaryWeight;
            this.weightSum = sum(primaryWeight) + sum(secondaryWeight);
        }

        /**
         * Computes a sum of items in the given array.
         * @param array the array for which to compute the sum
         * @return the sum of items in {@code array}
         */
        private static float sum(float[] array) {
            float sum = 0;
            for (int i = 0; i < array.length; i++)
                sum += array[i];
            return sum;
        }

        public float getWeight(SortedDataIterator iterator) {
            return (iterator.isCurrentPrimary() ? primaryWeight : secondaryWeight)[iterator.currentIndex()];
        }

        public float getWeightSum(ObjectIntDualVector obj) {
            return weightSum;
        }
    }
}

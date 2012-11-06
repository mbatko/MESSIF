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
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;


/**
 * This object uses multiple static array of integers as its data content.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class ObjectIntMultiVector extends LocalAbstractObject implements BinarySerializable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Data ******************//

    /** Data array */
    protected int[][] data;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectIntMultiVector.
     * @param data the data content of the new object
     */
    public ObjectIntMultiVector(int[][] data) {
        this.data = data;
    }

    /**
     * Creates a new instance of ObjectIntMultiVector with randomly generated content data.
     * Content will be generated using normal distribution of random numbers from interval
     * [0;1).
     *
     * @param arrays the number of vector data arrays to create
     * @param dimension number of dimensions to generate
     */
    public ObjectIntMultiVector(int arrays, int dimension) {
        this(arrays, dimension, 0, 1);
    }

    /**
     * Creates a new instance of ObjectIntMultiVector with randomly generated content data.
     * Content will be generated using normal distribution of random numbers from interval
     * [min;max).
     *
     * @param arrays the number of vector data arrays to create
     * @param dimension number of dimensions to generate
     * @param min lower bound of the random generated values (inclusive)
     * @param max upper bound of the random generated values (exclusive)
     */
    public ObjectIntMultiVector(int arrays, int dimension, int min, int max) {
        this.data = new int[arrays][];
        for (int i = 0; i < data.length; i++)
            this.data[i] = ObjectIntVector.randomData(dimension, min, max);
    }

    /**
     * Creates a new instance of ObjectIntMultiVector from text stream.
     * The data are stored as several lines of comma-separated integers.
     * @param stream the stream from which to read lines of text
     * @param arrays number of arrays to read from the stream
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException if there was an I/O error during reading from the stream
     * @throws NumberFormatException if a line read from the stream does not consist of comma-separated or space-separated numbers
     */
    public ObjectIntMultiVector(BufferedReader stream, int arrays) throws EOFException, IOException, NumberFormatException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);
        this.data = new int[arrays][];
        this.data[0] = ObjectIntVector.parseIntVector(line);
        for (int i = 1; i < data.length; i++)
            this.data[i] = ObjectIntVector.parseIntVector(stream.readLine());
    }

    /**
     * Creates a new instance of ObjectIntMultiVector from text stream.
     * The data are stored as a single line with semicolon separated lists of comma-separated integers.
     * @param stream the stream from which to read lines of text
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException if there was an I/O error during reading from the stream
     * @throws NumberFormatException if a line read from the stream does not consist of comma-separated or space-separated numbers
     */
    public ObjectIntMultiVector(BufferedReader stream) throws EOFException, IOException, NumberFormatException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);
        if (line.isEmpty()) {
            this.data = new int[0][];
        } else {
            String[] arrays = line.split("\\s*;\\s*");
            this.data = new int[arrays.length][];
            for (int i = 0; i < data.length; i++)
                this.data[i] = ObjectIntVector.parseIntVector(arrays[i]);
        }
    }


    //****************** Text file store/retrieve methods ******************//

    @Override
    protected void writeData(OutputStream stream) throws IOException {
        if (data == null || data.length == 0) {
            stream.write('\n');
        } else {
            for (int i = 0; i < data.length; i++)
                ObjectIntVector.writeIntVector(data[i], stream, ',', i == data.length - 1 ? '\n' : ';');
        }
    }


    //****************** Equality comparing function ******************

    @Override
    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectIntMultiVector))
            return false;

        return Arrays.deepEquals(((ObjectIntMultiVector)obj).data, data);
    }

    @Override
    public int dataHashCode() {
        return Arrays.deepHashCode(data);
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns the number of vector data arrays.
     * @return the number of vector data arrays
     */
    public int getVectorDataCount() {
        return data.length;
    }

    /**
     * Returns the vector of integer values, which represents the contents of the
     * respective data array of this object.
     * A copy is returned, so any modifications to the returned array do not affect the original object.
     * @param array the index of the array to return
     * @return the data contents of this object
     * @throws IndexOutOfBoundsException if the given {@code array} index is negative or greater or equal to {@link #getVectorDataCount()}
     */
    public int[] getVectorData(int array) throws IndexOutOfBoundsException {
        return data[array].clone();
    }

    /**
     * Returns the number of items in the respective data array of this object.
     * @param array the index of the array the length of which is returned
     * @return the number of items in the respective data array
     * @throws IndexOutOfBoundsException if the given {@code array} index is negative or greater or equal to {@link #getVectorDataCount()}
     */
    public int getVectorDataItemCount(int array) throws IndexOutOfBoundsException {
        return data[array].length;
    }

    /**
     * Returns the value at the given {@code index} of the respective data array of this object.
     * @param array the index of the array the item of which is returned
     * @param index the index of the item in the respective data array to retrieve
     * @return a single value of the respective data array
     * @throws IndexOutOfBoundsException if the given {@code array} or {@code index} is negative or
     *      greater or equal to {@link #getVectorDataCount()} or {@link #getVectorDataItemCount(int)} respectively
     */
    public int getVectorDataItem(int array, int index) throws IndexOutOfBoundsException {
        return data[array][index];
    }

    /**
     * Returns the vector of all integer values, which represents the contents of all the
     * respective data array of this object. The respective arrays are concatenated
     * according to their position.
     * A copy is returned, so any modifications to the returned array do not affect the original object.
     * @return the data contents of this object
     */
    public int[] getVectorData() {
        int[] ret = new int[getDimensionality()];
        int lastPos = 0;
        for (int i = 0; i < data.length; i++) {
            System.arraycopy(data[i], 0, ret, lastPos, data[i].length);
            lastPos += data[i].length;
        }
        return ret;
    }

    /**
     * Returns the number of dimensions of this vector.
     * Note that dimensions in all arrays are summed together.
     * @return the number of dimensions of this vector
     */
    public int getDimensionality() {
        int dim = 0;
        for (int i = 0; i < data.length; i++)
            dim += data[i].length;
        return dim;
    }

    @Override
    public int getSize() {
        return getDimensionality() * Integer.SIZE / 8;
    }


    //****************** Sorting support methods ******************//

    /**
     * Sort the internal arrays with data.
     */
    protected void sortData() {
        for (int i = 0; i < data.length; i++)
            Arrays.sort(data[i]);
    }

    /**
     * Returns an iterator over the integers from all vector data arrays.
     * The data are provided in sorted order, assuming that the respective data
     * arrays were previously sorted using {@link #sortData()}.
     * @return an iterator over the integers from all vector data arrays
     */
    public SortedDataIterator getSortedIterator() {
        return new SortedDataIterator();
    }

    /**
     * Represents resulting values that can be returned by
     * {@link SortedDataIterator#intersect(messif.objects.impl.ObjectIntMultiVector.SortedDataIterator) intersect}
     * method of the {@link SortedDataIterator}.
     */
    public static enum SDIteratorIntersectionResult {
        /**
         * There were no intersecting objects found. At least one iterator is completely read
         * (i.e. has no next object).
         */
        NONE,
        /** A new intersecting object was found in both iterators */
        BOTH,
        /**
         * A new intersecting object was found in the iterator, on which the intersect
         * method was called. The iterator passed as the argument did not advance.
         */
        THIS_ONLY,
        /**
         * A new intersecting object was found in the iterator, which was passed
         * as the argument of the intersect method. The iterator on which the intersect
         * was called did not advance.
         */
        ARGUMENT_ONLY;

        /**
         * Returns <tt>true</tt> if the result indicates that there was an intersecting object found.
         * Otherwise, <tt>false</tt> is returned, i.e. the results is {@link #NONE}.
         * @return <tt>true</tt> if the result indicates that there was an intersecting object found
         */
        public boolean isIntersecting() {
            return this != NONE;
        }

        /**
         * Returns <tt>true</tt> if the result indicates that there was an
         * intersecting object found in the iterator on which the intersect method was called.
         * @return <tt>true</tt> if the result indicates that there was an
         *      intersecting object found in the iterator on which the intersect method was called
         */
        public boolean isThisIntersecting() {
            return this == BOTH || this == THIS_ONLY;
        }

        /**
         * Returns <tt>true</tt> if the result indicates that there was an
         * intersecting object found in the iterator passed as argument.
         * @return <tt>true</tt> if the result indicates that there was an
         *      intersecting object found in the iterator passed as argument
         */
        public boolean isArgumentIntersecting() {
            return this == BOTH || this == ARGUMENT_ONLY;
        }

        /**
         * Returns <tt>true</tt> if the result indicates that there was an intersecting
         * object found both this iterator and the iterator passed as argument.
         * @return <tt>true</tt> if the result indicates that there was an intersecting
         *      object found both this iterator and the iterator passed as argument
         */
        public boolean isBothIntersecting() {
            return this == BOTH;
        }
    };

    /**
     * Internal iterator that provides sorted access to the vector data arrays of integers.
     */
    public final class SortedDataIterator implements Iterator<Integer> {
        /** Current index in the respective data array (all are set to zero from the start) */
        private int[] dataIndex = new int[data.length];
        /** Index of the data array from which the last value was returned */
        private int which = -1;
        /** Index of the data array from which the next value will be returned */
        private int nextWhich;

        /**
         * Creates a new instance of the sorted iterator.
         * The iterator finds the next data array to return the item from.
         */
        protected SortedDataIterator() {
            nextWhich = getNextDataArrayIndex();
        }

        /**
         * Returns the index of the data array that has the smallest next value.
         * @return the index of the data array or -1 if all the data array indexes are exhausted
         */
        private int getNextDataArrayIndex() {
            int nextInt = Integer.MAX_VALUE;
            int ret = -1;
            for (int i = 0; i < dataIndex.length; i++) {
                if (dataIndex[i] < data[i].length && data[i][dataIndex[i]] < nextInt) {
                    ret = i;
                    nextInt = data[i][dataIndex[i]];
                }
            }
            return ret;
        }

        @Override
        public boolean hasNext() {
            return nextWhich != -1;
        }

        /**
         * Returns the object this iterator operates on.
         * @return the object this iterator operates on
         */
        public ObjectIntMultiVector getIteratedObject() {
            return ObjectIntMultiVector.this;
        }

        /**
         * Returns the index of the vector data array, from which the last call to {@link #nextInt()} or {@link #next()}
         * returned the value. If the {@link #nextInt()} or {@link #next()} was not called yet,
         * {@link IllegalStateException} is thrown.
         * @return the index of the vector data array
         * @throws IllegalStateException if {@link #nextInt()} or {@link #next()} was not called yet
         */
        public int getCurrentVectorDataIndex() throws IllegalStateException {
            if (which == -1)
                throw new IllegalStateException("Next was not called yet");
            return which;
        }

        /**
         * Returns the index of respective data array (see {@link #getCurrentVectorDataIndex()})
         * of the data returned from the last call to {@link #nextInt()} or {@link #next()}.
         * @return the index of the primary or secondary data array
         * @throws IllegalStateException if {@link #nextInt()} or {@link #next()} was not called yet
         */
        public int currentIndex() {
            return dataIndex[getCurrentVectorDataIndex()];
        }

        /**
         * Returns the integer returned from the last call to {@link #nextInt()} or {@link #next()}.
         * @return the integer returned from the last call to {@link #nextInt()} or {@link #next()}
         * @throws IllegalStateException if {@link #nextInt()} or {@link #next()} was not called yet
         */
        public int currentInt() throws IllegalStateException {
            if (which == -1)
                throw new IllegalStateException("Next was not called yet");
            return data[which][dataIndex[which] - 1];
        }

        /**
         * Returns the next value as integer.
         * @return the next value as integer
         * @throws NoSuchElementException if there are no more items in this iterator
         */
        public int nextInt() throws NoSuchElementException {
            if (nextWhich == -1)
                throw new NoSuchElementException();
            which = nextWhich;
            dataIndex[nextWhich]++;
            nextWhich = getNextDataArrayIndex();
            return data[which][dataIndex[which] - 1];
        }

        /**
         * Returns the next value as integer, but does not advance the iterator.
         * @return the next value as integer
         * @throws NoSuchElementException if there are no more items in this iterator
         */
        public int peekNextInt() throws NoSuchElementException {
            if (nextWhich == -1)
                throw new NoSuchElementException();
            return data[nextWhich][dataIndex[nextWhich]];
        }

        /**
         * Returns <tt>true</tt> (and advances this iterator) if the next value in this iterator
         * is equal to the given {@code value}.
         * @param value the value to check
         * @return <tt>true</tt> if this iterator was advanced to the next value or <tt>false</tt> otherwise.
         */
        public boolean nextIfEqual(int value) {
            if (which == -1 || nextWhich == -1)
                return false;
            if (peekNextInt() != value)
                return false;
            nextInt();
            return true;
        }

        /**
         * Returns <tt>true</tt> (and advances this iterator) if the next value in this iterator
         * is equal to the current value of the given iterator.
         * This method is used in intersection to correctly handle duplicate values.
         * @param iterator the iterator the current value of which to check
         * @return <tt>true</tt> if this iterator was advanced to the next value or <tt>false</tt> otherwise.
         */
        private boolean nextIfEqual(SortedDataIterator iterator) {
            if (iterator.which == -1) // To avoid illegal state in the iterator if no next was called yet
                return false;
            return nextIfEqual(iterator.currentInt());
        }

        @Override
        public Integer next() {
            return nextInt();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("SortedDataIterator does not support removal");
        }

        /**
         * Read all items (by calling {@link #next()}) that have the same value 
         * as the current item.
         * @return the number of duplicates skipped
         * @throws IllegalStateException if the iterator has no current value
         */
        public int skipDuplicates() throws IllegalStateException {
            int count = 0;
            int curval = currentInt();
            while (nextIfEqual(curval))
                count++;
            return count;
        }

        /**
         * Find the next value that is present in this iterator and the given {@code iterator}.
         * The return value indicates whether {@link SDIteratorIntersectionResult#NONE no intersecting}
         * items were found (and either this or the given {@code iterator} has no more items),
         * or an intersecting item was found. In the later case the current item of
         * both iterators is the same. Note that there can be duplicate items in which
         * case the next call to this method will return either {@link SDIteratorIntersectionResult#THIS_ONLY}
         * or {@link SDIteratorIntersectionResult#ARGUMENT_ONLY} value to indicate
         * that a duplicate in the respective iterator was found.
         *
         * @param iterator the iterator to intersect with
         * @return {@link SDIteratorIntersectionResult#NONE} if one of the iterators reached last item without a matching value,
         *         or one of the other three {@link SDIteratorIntersectionResult} values if a value in both the iterators was found
         *         (their current value will be the same)
         */
        public SDIteratorIntersectionResult intersect(SortedDataIterator iterator) {
            // If this iterator has the same next value as the other iterator's current value (duplicate objects)
            if (nextIfEqual(iterator))
                return SDIteratorIntersectionResult.THIS_ONLY;

            // If the other iterator has the same next value as this iterator's current value (duplicate objects)
            if (iterator.nextIfEqual(this))
                return SDIteratorIntersectionResult.ARGUMENT_ONLY;

            // If there are no items in either iterator, exit
            if (!hasNext() || !iterator.hasNext())
                return SDIteratorIntersectionResult.NONE;

            // Read next item from both iterators - it is either at the beggining or after an intersection is found
            int thisInt = nextInt();
            int itInt = iterator.nextInt();

            // Repeat until an intersection is found or one of the iterators runs out of items
            for (;;) {
                if (thisInt < itInt) { // This iterator's value is smaller, advance this iterator or exit
                    if (!hasNext())
                        return SDIteratorIntersectionResult.NONE;
                    thisInt = nextInt();
                } else if (thisInt > itInt) { // Other iterator's value is smaller, advance the other iterator or exit
                    if (!iterator.hasNext())
                        return SDIteratorIntersectionResult.NONE;
                    itInt = iterator.nextInt();
                } else {
                    return SDIteratorIntersectionResult.BOTH;
                }
            }
        }

        /**
         * Find the next value that is present in this iterator and the given {@code iterator}.
         * The return value indicates whether {@link SDIteratorIntersectionResult#NONE no intersecting}
         * items were found (and either this or the given {@code iterator} has no more items),
         * or an intersecting item was found. In the later case the current item of
         * both iterators is the same. Note that there can be duplicate items in which
         * case the next call to this method will return either {@link SDIteratorIntersectionResult#THIS_ONLY}
         * or {@link SDIteratorIntersectionResult#ARGUMENT_ONLY} value to indicate
         * that a duplicate in the respective iterator was found. This can be avoided
         * if {@code duplicates} array is passed in which case the array is filled with
         * the number of duplicate items in this and the given {@code iterator}.
         *
         * @param iterator the iterator to intersect with
         * @param frequency if an array instance is provided, the first item will be set
         *          to the number of duplicate items encountered (skipped) in this iterator
         *          when an intersection is found and the second array item is set to the
         *          number of duplicates in the {@code iterator};
         *          this is ignored if <tt>null</tt> is passed
         * @return <tt>true</tt> if a value in both the iterators was found (their current value will be the same)
         *      or <tt>false</tt> if one of the iterators reached last item without a matching value
         */
        public SDIteratorIntersectionResult intersectSkipDuplicates(SortedDataIterator iterator, int[] frequency) {
            SDIteratorIntersectionResult result = intersect(iterator);
            // Skip all duplicates and set the frequencies into the array
            if (result != SDIteratorIntersectionResult.NONE && frequency != null && frequency.length == 2) {
                frequency[0] = 1 + skipDuplicates();
                frequency[1] = 1 + iterator.skipDuplicates();
            }
            return result;
        }
    }


    //****************** Weight provider interface ******************//

    /**
     * Interface for providing the weights for the Jaccard distance function.
     */
    public static interface WeightProvider {
        /**
         * Returns the weight for the item that the {@code iterator} points to.
         * @param iterator this iterator's current object weight is to be retrieved
         * @return the weight for the current item of the iterator
         */
        public float getWeight(SortedDataIterator iterator);
        /**
         * Returns the sum of all weights for the given object.
         * Note that this value must be consistent with {@link #getWeight(messif.objects.impl.ObjectIntMultiVector.SortedDataIterator)},
         * i.e. the returned sum is the sum of the weight retrieved by iterating over
         * all items from the {@code obj.getSortedIterator()}.
         * @param obj the object for which the weights are given
         * @return the total sum of all weights
         */
        public float getWeightSum(ObjectIntMultiVector obj);
        /**
         * Returns the square root of the sum of all weight squares for the given object.
         * Note that this value must be consistent with {@link #getWeight(messif.objects.impl.ObjectIntMultiVector.SortedDataIterator)},
         * i.e. the returned number is the square root of the sum of all weight squares retrieved by iterating over
         * all items from the {@code obj.getSortedIterator()}.
         * @param obj the object for which the weights are given
         * @return the total norm of all weights
         */
        public double getWeightNorm(ObjectIntMultiVector obj);
    }


    /**
     * Implementation of {@link WeightProvider} that has a single weight for every data array
     * of the {@link ObjectIntMultiVector}.
     */
    public static class MultiWeightProvider implements WeightProvider, Serializable {
        /** Class id for serialization. */
        private static final long serialVersionUID = 1L;

        /** Weights for data arrays - all the items in the respective data array has a single weight */
        protected final float[] weights;

        /**
         * Creates a new instance of MultiWeightProvider with the the given array of weights.
         * @param weights the weights for the data arrays
         */
        public MultiWeightProvider(float[] weights) {
            if (weights == null)
                throw new NullPointerException();
            this.weights = weights;
        }

        @Override
        public float getWeight(SortedDataIterator iterator) {
            return weights[iterator.getCurrentVectorDataIndex()];
        }

        /**
         * Returns the weights for data arrays encapsulated in this {@link WeightProvider}.
         * Note that a copy is returned, so any modifications in the returned array are
         * not reflected by the provider.
         * @return the weights for data arrays
         */
        public float[] getWeights() {
            return weights.clone();
        }

        /**
         * Returns the weight for the given data array encapsulated in this {@link WeightProvider}.
         * @param array the index of the data array for which to get the weight
         * @return the weight for the given data array
         */
        public float getWeight(int array) {
            return weights[array];
        }

        @Override
        public float getWeightSum(ObjectIntMultiVector obj) {
            float sum = 0;
            for (int i = 0; i < obj.data.length; i++)
                sum += obj.data[i].length * weights[i];
            return sum;
        }

        @Override
        public double getWeightNorm(ObjectIntMultiVector obj) {
            double sum = 0;
            for (int i = 0; i < obj.data.length; i++)
                sum += obj.data[i].length * weights[i] * weights[i];
            return Math.sqrt(sum);
        }
    }

    /**
     * Implementation of {@link WeightProvider} that has a given weight for every
     * item of every data array of {@link ObjectIntMultiVector}.
     * Note that the number of weights <em>must</em> be equal to the
     * number of data items in the respective data array of the
     * {@link ObjectIntMultiVector}.
     */
    public static class ArrayMultiWeightProvider implements WeightProvider, Serializable {
        /** Class id for serialization. */
        private static final long serialVersionUID = 1L;

        /** Weights for the items in respective data array */
        private final float[][] weights;
        /** Sum of the weights in all data arrays */
        private final float weightSum;
        /** Norm of the weights in all data arrays */
        private final double weightNorm;

        /**
         * Creates a new instance of ArrayWeightProvider with the two given weights.
         * @param weights the weights for the items in the data arrays
         */
        public ArrayMultiWeightProvider(float[][] weights) {
            this.weights = weights;
            float weightSumTemp = 0;
            double weightSqSum = 0;
            for (int i = 0; i < weights.length; i++)
                for (int j = 0; j < weights[i].length; j++) {
                    weightSumTemp += weights[i][j];
                    weightSqSum += weights[i][j] * weights[i][j];
                }
            
            this.weightSum = weightSumTemp;
            this.weightNorm = Math.sqrt(weightSqSum);
        }

        @Override
        public float getWeight(SortedDataIterator iterator) {
            return weights[iterator.getCurrentVectorDataIndex()][iterator.currentIndex()];
        }

        @Override
        public float getWeightSum(ObjectIntMultiVector obj) {
            return weightSum;
        }

        @Override
        public double getWeightNorm(ObjectIntMultiVector obj) {
            return weightNorm;
        }
    }

    /**
     * Implementation of {@link WeightProvider} that has a map of weights for
     * based on items of {@link ObjectIntMultiVector}.
     * Note that the map <em>must</em> contain a weight for every value in the
     * respective {@link ObjectIntMultiVector}.
     */
    public static class MapMultiWeightProvider implements WeightProvider, Serializable {
        /** Class id for serialization. */
        private static final long serialVersionUID = 1L;

        /** Map with weights for the values */
        private final Map<Integer, Float> dataToWeightMap;

        /**
         * Creates a new weight provider with a map of weights.
         * The key for the map is a value from the {@link ObjectIntMultiVector}.
         * @param dataToWeightMap the map of weights
         */
        public MapMultiWeightProvider(Map<Integer, Float> dataToWeightMap) {
            this.dataToWeightMap = dataToWeightMap;
        }

        /**
         * Returns the weight from the data weight map.
         * @param value the value for which to get the weight
         * @return the weight for the given value
         * @throws IllegalArgumentException if the map does not contain a weight for the given value
         */
        protected float getWeight(Integer value) throws IllegalArgumentException {
            Float weight = dataToWeightMap.get(value);
            if (weight == null)
                throw new IllegalArgumentException("Weight map has no value for value " + value);
            return weight.floatValue();
        }

        @Override
        public float getWeight(SortedDataIterator iterator) {
            return getWeight(iterator.currentInt());
        }

        @Override
        public float getWeightSum(ObjectIntMultiVector obj) {
            float sum = 0;
            for (int i = 0; i < obj.data.length; i++) {
                int[] data = obj.data[i];
                for (int j = 0; j < data.length; j++) {
                    sum += getWeight(data[j]);
                }
            }
            return sum;
        }

        @Override
        public double getWeightNorm(ObjectIntMultiVector obj) {
            double sum = 0;
            for (int i = 0; i < obj.data.length; i++) {
                int[] data = obj.data[i];
                for (int j = 0; j < data.length; j++) {
                    float weight = getWeight(data[j]);
                    sum += weight * weight;
                }
            }
            return Math.sqrt(sum);
        }
    }

    /**
     * Implementation of {@link WeightProvider} that has a single weight for every data array of the {@link ObjectIntMultiVector}
     *  and it ignores a specified list of integers (created from a given list of keywords) - the ignore weight is specified in the
     *  last weight in the weight array.
     */
    public static class MultiWeightIgnoreProvider extends MultiWeightProvider {
        /** Class id for serialization. */
        private static final long serialVersionUID = 51101L;

        /** Weight used for the specified list of ignored keywords. */
        private final float ignoreWeight;

        /** Set of integer IDs to be ignored */
        private final Set<Integer> ignoredItems;

        /**
         * Creates a new instance of MultiWeightProvider with the the given array of weights.
         * @param weights the weights for the data arrays
         * @param ignoreWeight weight used for the {@code ignoredKeywords}
         * @param ignoredItems  set of IDs to be ignored 
         */
        public MultiWeightIgnoreProvider(float[] weights, float ignoreWeight, Set<Integer> ignoredItems) {
            super(weights);
            this.ignoreWeight = ignoreWeight;
            if (ignoredItems == null)
                throw new NullPointerException();
            this.ignoredItems = ignoredItems;
        }

        @Override
        public float getWeight(SortedDataIterator iterator) {
            if (ignoredItems.contains(iterator.currentInt())) {
                return ignoreWeight;
            }
            return super.getWeight(iterator);
        }

        @Override
        public float getWeightSum(ObjectIntMultiVector obj) {
            float sum = 0;
            for (int i = 0; i < obj.data.length; i++) {
                sum += obj.data[i].length * weights[i];
                // substract the IDs to be ignored
                for (int j= 0; j < obj.data[i].length; j++) {
                    if (ignoredItems.contains(obj.data[i][j])) {
                        sum += (ignoreWeight - weights[i]);
                    }
                }
            }
            return sum;
        }

        @Override
        public double getWeightNorm(ObjectIntMultiVector obj) {
            double sum = 0;
            for (int i = 0; i < obj.data.length; i++) {
                sum += obj.data[i].length * weights[i] * weights[i];
                // substract the IDs to be ignored
                for (int j= 0; j < obj.data[i].length; j++) {
                    if (ignoredItems.contains(obj.data[i][j])) {
                        sum += (ignoreWeight * ignoreWeight - weights[i] * weights[i]);
                    }
                }
            }
            return Math.sqrt(sum);
        }
    }


    //****************************** Cloning *****************************//

    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        throw new UnsupportedOperationException("IntMultiVector does not support random modification");
    }


    //************ String representation ************//

    /**
     * Converts this object to a string representation.
     * The format is the comma-separated list of coordinates enclosed in square brackets
     * and the result of <code>super.toString()</code> is appended.
     */
    @Override
    public String toString() {
        StringBuffer rtv = new StringBuffer(super.toString()).append(" [");

        for (int i = 0; i < data.length; i++) {
            rtv.append(i > 0 ? ", [" : "[");
            for (int j = 0; j < data[i].length; j++) {
                if (j > 0)
                    rtv.append(", ");
                rtv.append(data[i][j]);
            }
            rtv.append(']');
        }
        rtv.append(']');

        return rtv.toString();
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectIntVector loaded from binary input buffer.
     * 
     * @param input the buffer to read the ObjectIntVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectIntMultiVector(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        this.data = new int[serializator.readInt(input)][];
        for (int i = 0; i < this.data.length; i++)
            this.data[i] = serializator.readIntArray(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        int size = super.binarySerialize(output, serializator) + serializator.write(output, data.length);
        for (int i = 0; i < this.data.length; i++)
            size += serializator.write(output, data[i]);
        return size;
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        int size = super.getBinarySize(serializator) + 4;
        for (int i = 0; i < this.data.length; i++)
            size += serializator.getBinarySize(data[i]);
        return size;
    }

}

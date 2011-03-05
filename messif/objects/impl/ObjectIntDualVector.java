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
import java.util.Arrays;
import java.util.Iterator;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializator;

/**
 * This object uses two static arrays of integers as its data content.
 * No implementation of distance function is provided - see {@link ObjectIntDualVectorJaccard}.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class ObjectIntDualVector extends ObjectIntVector implements Serializable {
    /** Class id for serialization. */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Secondary array of data */
    protected final int[] data2;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectIntDualVector.
     * @param data integer vector of the primary data
     * @param data2 integer vector of the secondary data 
     * @param forceSort if <tt>false</tt>, the data is expected to be sorted
     */
    public ObjectIntDualVector(int[] data, int[] data2, boolean forceSort) {
        super(data);
        this.data2 = data2;
        if (forceSort)
            sortData();
    }

    /**
     * Creates a new instance of ObjectIntDualVector.
     * @param data integer vector of the primary data
     * @param data2 integer vector of the secondary data
     */
    public ObjectIntDualVector(int[] data, int[] data2) {
        this(data, data2, true);
    }

    /**
     * Creates a new instance of randomly generated ObjectIntDualVector.
     * @param dimension the dimensionality of the primary data
     * @param dimension2 the dimensionality of the secondary data
     */
    public ObjectIntDualVector(int dimension, int dimension2) {
        super(dimension);
        this.data2 = randomData(dimension2, 0, 1);
        sortData();
    }


    /**
     * Creates a new instance of ObjectIntDualVector from stream - it expects that the data is already sorted!
     * @param stream text stream to read the data from
     * @throws IOException when an error appears during reading from given stream.
     *  or  EOFException when eof of the given stream is reached.
     * @throws NumberFormatException when the line read from given stream does
     * not consist of comma-separated or space-separated numbers.
     */
    public ObjectIntDualVector(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
        this.data2 = parseIntVector(stream.readLine());
    }


    //****************** Data access methods ******************//

    @Override
    public int getSize() {
        return super.getSize() + data2.length * Integer.SIZE / 8;
    }

    /**
     * Returns the secondary vector of integer values, which represents the contents of this object.
     * A copy is returned, so any modifications to the returned array do not affect the original object.
     * @return the secondary data contents of this object
     */
    public int[] getSecondaryVectorData() {
        return this.data2.clone();
    }

    /**
     * Returns the number of dimensions of this vector.
     * Note that the dimensions are counted as the sum of the primary and the secondary data.
     * @return the number of dimensions of this vector
     */
    @Override
    public int getDimensionality() {
        return data.length + data2.length;
    }

    @Override
    protected int[] getMinMaxOverCoords(int[] currRange) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        throw new UnsupportedOperationException();
    }


    //****************** Sorting support methods ******************//

    /**
     * Sort the internal array with data.
     */
    protected void sortData() {
        Arrays.sort(data);
        Arrays.sort(data2);
    }

    /**
     * Returns an iterator over both the primary and secondary data.
     * The data are provided in sorted order (mixing the data from the two arrays).
     * @return an iterator over the sorted primary and secondary data
     */
    protected SortedDataIterator getSortedIterator() {
        return new SortedDataIterator();
    }

    /**
     * Internal iterator that provides sorted access to the primary and secondary array of integers.
     */
    protected class SortedDataIterator implements Iterator<Integer> {
        /** Current index in the primary data array */
        private int dataIndex = 0;
        /** Current index in the secondary data array */
        private int data2Index = 0;
        /** Index of the data array from which the last value was returned */
        private int which = -1;

        @Override
        public boolean hasNext() {
            return dataIndex < data.length || data2Index < data2.length;
        }

        /**
         * Returns the object this iterator operates on.
         * @return the object this iterator operates on
         */
        public ObjectIntDualVector getIteratedObject() {
            return ObjectIntDualVector.this;
        }

        /**
         * Returns <tt>true</tt> if the integer returned from last call to {@link #nextInt()} or {@link #next()}
         * was from the primary data. Otherwise, <tt>false</tt> is returned if it was from the
         * secondary data. If the {@link #nextInt()} or {@link #next()} was not called yet,
         * {@link IllegalStateException} is thrown.
         * @return <tt>true</tt> if the current integer was from the primary data or <tt>false</tt> if
         *      it was from the secondary data
         * @throws IllegalStateException if {@link #nextInt()} or {@link #next()} was not called yet
         */
        public boolean isCurrentPrimary() throws IllegalStateException {
            if (which == -1)
                throw new IllegalStateException("Next was not called yet");
            return which == 0;
        }

        /**
         * Returns the index of the primary or secondary data (see {@link #isCurrentPrimary()} for disabiguation)
         * of the data returned from the last call to {@link #nextInt()} or {@link #next()}.
         * @return the index of the primary or secondary data array
         */
        public int currentIndex() {
            return isCurrentPrimary() ? dataIndex : data2Index;
        }

        /**
         * Returns the integer returned from last call to {@link #nextInt()} or {@link #next()}.
         * @return the integer returned from last call to {@link #nextInt()} or {@link #next()}
         * @throws IllegalStateException if {@link #nextInt()} or {@link #next()} was not called yet
         */
        public int currentInt() throws IllegalStateException {
            return isCurrentPrimary() ? data[dataIndex] : data2[data2Index];
        }

        /**
         * Returns the next value as integer.
         * @return the next value as integer
         * @throws IndexOutOfBoundsException if there are no more items in this iterator
         */
        public int nextInt() throws IndexOutOfBoundsException {
            if (dataIndex < data.length && (data2Index >= data2.length || data[dataIndex] < data2[data2Index])) {
                which = 0;
                return data[dataIndex++];
            } else {
                which = 1;
                return data2[data2Index++];
            }
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
         * Find the next value that is present in this iterator and the given {@code iterator}.
         * If no such value can be found, <tt>false</tt> is returned - in that case, at least
         * one of the iterators has run out of values.
         *
         * @param iterator the iterator to intersect with
         * @return <tt>true</tt> if a value in both the iterators was found (their current value will be the same)
         *      or <tt>false</tt> if one of the iterators reached last item without a matching value
         */
        public boolean intersect(SortedDataIterator iterator) {
            // If there are no items in either iterator, exit
            if (!hasNext() || !iterator.hasNext())
                return false;

            // Read next item from both iterators - it is either at the beggining or after an intersection is found
            int thisInt = nextInt();
            int itInt = nextInt();

            // Repeat until an intersection is found or one of the iterators runs out of items
            for (;;) {
                if (thisInt < itInt) { // This iterator's value is smaller, advance this iterator or exit
                    if (!hasNext())
                        return false;
                    thisInt = nextInt();
                } else if (thisInt > itInt) { // Other iterator's value is smaller, advance the other iterator or exit
                    if (!iterator.hasNext())
                        return false;
                    itInt = iterator.nextInt();
                } else {
                    return true;
                }
            }
        }
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectIntVector loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectIntVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectIntDualVector(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        data2 = serializator.readIntArray(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, data2);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + serializator.getBinarySize(data2);
    }

}

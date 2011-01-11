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
import java.util.Arrays;
import java.util.Random;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;
import messif.objects.util.AbstractObjectIterator;


/**
 * This object uses static array of integers as its data content.
 * No implementation of distance function is provided - see {@link ObjectIntVectorL1}
 * or {@link ObjectIntVectorL2}.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class ObjectIntVector extends LocalAbstractObject implements BinarySerializable {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Data ******************//

    /** Data array */
    protected int[] data;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectIntVector.
     * @param data the data content of the new object
     */
    public ObjectIntVector(int[] data) {
        this.data = data.clone();
    }

    /**
     * Creates a new instance of ObjectIntVector with randomly generated content data.
     * Content will be generated using normal distribution of random integer numbers
     * from interval [0;maxint).
     *
     * @param dimension number of dimensions to generate
     */
    public ObjectIntVector(int dimension) {
        this(dimension, 0, Integer.MAX_VALUE - 1);
    }

    /**
     * Creates a new instance of ObjectIntVector with randomly generated content data.
     * Content will be generated using normal distribution of random numbers from interval
     * [min;max).
     *
     * @param dimension number of dimensions to generate
     * @param min lower bound of the random generated values (inclusive)
     * @param max upper bound of the random generated values (exclusive)
     */
    public ObjectIntVector(int dimension, int min, int max) {
        this.data = randomData(dimension, min, max);
    }

    /**
     * Creates a new instance of ObjectIntVector from text stream.
     * @param stream the stream from which to read lines of text
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException if there was an I/O error during reading from the stream
     * @throws NumberFormatException if a line read from the stream does not consist of comma-separated or space-separated numbers
     */
    public ObjectIntVector(BufferedReader stream) throws EOFException, IOException, NumberFormatException {
        // Keep reading the lines while they are comments, then read the first line of the object
        String line = readObjectComments(stream);
        this.data = parseIntVector(line);
    }


    //****************** Text file store/retrieve methods ******************//

    /**
     * Parses a vector of integers from the given line of text.
     *
     * @param line the text from which to parse vector
     * @return the parsed vector of integers
     * @throws NumberFormatException if the given {@code line} does not have comma-separated or space-separated integers
     * @throws EOFException if a <tt>null</tt> {@code line} is given
     */
    public static int[] parseIntVector(String line) throws NumberFormatException, EOFException {
        if (line == null)
            throw new EOFException();
        line = line.trim();
        if (line.length() == 0)
            return new int[0];
        String[] numbers = line.split(line.indexOf(',') != -1 ? "\\s*,\\s*" : "\\s+");

        int[] data = new int[numbers.length];
        for (int i = 0; i < data.length; i++)
            data[i] = Integer.parseInt(numbers[i]);

        return data;
    }

    /**
     * Writes the given vector of integers to the given output stream as text.
     * 
     * @param data the vector of integers to output
     * @param stream the output stream to write the text to
     * @throws IOException if there was an I/O error while writing to the stream
     */
    public static void writeIntVector(int[] data, OutputStream stream) throws IOException {
        for (int i = 0; i < data.length; i++) {
            if (i > 0)
                stream.write(", ".getBytes());
            stream.write(String.valueOf(data[i]).getBytes());
        }
        stream.write('\n');
    }

    protected void writeData(OutputStream stream) throws IOException {
        writeIntVector(data, stream);
    }


    //****************** Random array generator ******************//

    /**
     * Generate an array of random integers using normal distribution of numbers
     * from interval [min;max).
     *
     * @param dimension number of dimensions to generate
     * @param min lower bound of the random generated values (inclusive)
     * @param max upper bound of the random generated values (exclusive)
     * @return a new array filled with random integers
     */
    public static int[] randomData(int dimension, int min, int max) {
        int[] data = new int[dimension];
        for (; dimension > 0; dimension--)
            data[dimension - 1] = (int)(min + getRandomNormal()*(max - min));
        return data;
    }


    //****************** Equality comparing function ******************

    public boolean dataEquals(Object obj) {
        if (!(obj instanceof ObjectIntVector))
            return false;

        return Arrays.equals(((ObjectIntVector)obj).data, data);
    }

    public int dataHashCode() {
        return Arrays.hashCode(data);
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns the vector of integer values, which represents the contents of this object.
     * A copy is returned, so any modifications to the returned array do not affect the original object.
     * @return the data contents of this object
     */
    public int[] getVectorData() {
        return this.data.clone();
    }

    public int getSize() {
        return this.data.length * Integer.SIZE / 8;
    }

    /**
     * Returns the number of dimensions of this vector.
     * @return the number of dimensions of this vector
     */
    public int getDimensionality() {
        return this.data.length;
    }

    /**
     * Computes minimum and maximum values over all coordinates of the current vector.
     *
     * @param currRange An optional parameter containing current minimum and maximum values. If null is passed
     *                  a new range with minimum and maximum is created, otherwise the passed array is updated.
     *
     * @return Returns an array of two integer values for the minimum and the maximum, respectively.
     */
    protected int[] getMinMaxOverCoords(int[] currRange) {
        int[] range;

        if (currRange != null)
            range = currRange;
        else
            range = new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE};

        for (int val : data) {
            if (val < range[0])         // Test the minimum
                range[0] = val;
            if (val > range[1])         // Test the maximum
                range[1] = val;
        }
        return range;
    }

    /**
     * Computes minimum and maximum values over all coordinates of vectors in the collection's
     * iterator.
     * @param iterator Iterator of a collection containing vectors to process.
     * @return Returns an array of two integer values for the minimum and the maximum per all
     *         coordinates, respectively.
     */
    public static int[] getMinMaxOverCoords(AbstractObjectIterator<? extends ObjectIntVector> iterator) {
        int[] range = {Integer.MAX_VALUE, Integer.MIN_VALUE};
        while (iterator.hasNext()) {
            ObjectIntVector obj = iterator.next();
            range = obj.getMinMaxOverCoords(range);
        }
        return range;
    }

    /**
     * Computes minimum and maximum values over every coordinate of vectors in the collection's
     * iterator.
     *
     * @param iterator Iterator of a collection containing vectors to process.
     *
     * @return Returns a 2-dimensional array of integer values. The first dimension distiguishes between
     *         minimum and maximum values. Index [0] contains an array of doubles which represent minimum
     *         values of individual coordinates over all vectors. Index [0] contains a respective array
     *         of maximum values. This return value can be directly passed to translateToUnitCube() method.
     */
    public static int[][] getMinMaxForEveryCoord(AbstractObjectIterator<? extends ObjectIntVector> iterator) {
        int[][] range = null;
        int dims = -1;

        while (iterator.hasNext()) {
            ObjectIntVector obj = iterator.next();

            if (range == null) {
                // Allocate result array
                dims = obj.getDimensionality();
                range = new int[2][dims];
                // Initialize it
                for (int i = 0; i < dims; i++) {
                    range[0][i] = Integer.MAX_VALUE;
                    range[1][i] = Integer.MIN_VALUE;
                }
            }

            int[] vec = obj.data;
            for (int i = 0; i < dims; i++) {
                if (vec[i] < range[0][i])         // Test the minimum
                    range[0][i] = vec[i];
                if (vec[i] > range[1][i])         // Test the maximum
                    range[1][i] = vec[i];
            }
        }
        return range;
    }


    //****************************** Cloning *****************************//

    /**
     * Creates and returns a randomly modified copy of this vector.
     * Selects a vector position in random and changes it - the final value stays in the given range.
     * The modification is small - only by (max-min)/1000
     *
     * @param  args  expected size of the array is 2: <b>minVector</b> vector with minimal values in all positions
     *         <b>maxVector</b> vector with maximal values in all positions
     * @return a randomly modified clone of this instance.
     */
    @Override
    public LocalAbstractObject cloneRandomlyModify(Object... args) throws CloneNotSupportedException {
        ObjectIntVector rtv = (ObjectIntVector) this.clone();
        rtv.data = this.data.clone();

        try {
            ObjectIntVector minVector = (ObjectIntVector) args[0];
            ObjectIntVector maxVector = (ObjectIntVector) args[1];
            Random random = new Random(System.currentTimeMillis());

            // pick a vector position in random
            int position = random.nextInt(Math.min(rtv.data.length, Math.min(minVector.data.length, maxVector.data.length)));

            // calculate 1/1000 of the possible range of this value and either add or substract it from the origival value
            float smallStep = (maxVector.data[position] - minVector.data[position]) / 1000;
            if (rtv.data[position] + smallStep <= maxVector.data[position])
                rtv.data[position] += smallStep;
            else rtv.data[position] -= smallStep;
        } catch (ArrayIndexOutOfBoundsException ignore) {
        } catch (ClassCastException ignore) { }

        return rtv;
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

        for (int i = 0; i < this.data.length; i++) {
            if (i > 0) rtv.append(", ");
            rtv.append(data[i]);
        }
        rtv.append("]");

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
    protected ObjectIntVector(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
        data = serializator.readIntArray(input);
    }

    @Override
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return super.binarySerialize(output, serializator) +
               serializator.write(output, data);
    }

    @Override
    public int getBinarySize(BinarySerializator serializator) {
        return super.getBinarySize(serializator) + serializator.getBinarySize(data);
    }

}

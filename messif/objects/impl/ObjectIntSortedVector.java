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
import java.util.Arrays;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;


/**
 * This class represents an integer vector sorted non-decreasingly - the data
 *  is sorted in the constructor, if not explicitely said that it's already sorted.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class ObjectIntSortedVector extends ObjectIntVector {

    /** Class id for serialization. */
    private static final long serialVersionUID = 20401L;


    //****************** Constructors ******************//

    /** 
     * Creates a new instance of object - make sure the data is sorted
     * @param data int vector data
     * @param forceSort if false, the data is expected to be sorted
     */
    public ObjectIntSortedVector(int[] data, boolean forceSort) {
        super(data);
        if (forceSort) {
            sortData();
        }
    }

    /**
     * Creates a new instance of object - make sure the data is sorted
     * @param data int vector data
     */
    public ObjectIntSortedVector(int[] data) {
        this(data, true);
    }

    /** 
     * Creates a new instance of randomly generated object
     * @param dimension vector dimensionality
     */
    public ObjectIntSortedVector(int dimension) {
        super(dimension);
        sortData();
    }

    //****************** Sorting procedure ******************//

    /**
     * Sort the internal array with data.
     */
    protected void sortData() {
        Arrays.sort(data);
    }


    //****************** Text file store/retrieve methods ******************//

    /** 
     * Creates a new instance of Object from text stream - it expects that the data is already sorted!
     * @param stream text stream to read the data from
     * @throws IOException when an error appears during reading from given stream.
     *  or  EOFException when eof of the given stream is reached.
     * @throws NumberFormatException when the line read from given stream does
     * not consist of comma-separated or space-separated numbers.
     */
    public ObjectIntSortedVector(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }

    /**
     * Computes minimum and maximum values over all coordinates of the current vector.
     *
     * @param currRange An optional parameter containing current minimum and maximum values. If null is passed
     *                  a new range with minimum and maximum is created, otherwise the passed array is updated.
     * @return Returns an array of two integer values for the minimum and the maximum, respectively.
     */
    @Override
    protected int[] getMinMaxOverCoords(int[] currRange) {
        int[] range;

        if (currRange != null)
            range = currRange;
        else
            range = new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE};
        if (data.length != 0) {
            range[0] = data[0];
            range[1] = data[data.length - 1];
        }
        return range;
    }


    //****************** Cloning ******************//

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
        throw new CloneNotSupportedException("Clonning with random modification is not supported for " + ObjectIntSortedVector.class);
    }


    //****************** BinarySerializable interface ******************//

    /**
     * Creates a new instance of ObjectIntSortedVector loaded from binary input buffer - it gotta be sorted already.
     *
     * @param input the buffer to read the ObjectIntVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectIntSortedVector(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

}

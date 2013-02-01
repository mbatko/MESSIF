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
import java.util.Arrays;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 * This object uses static array of floats as its data content.
 * The floats are checked to belong to a [0,1] interval.
 * No implementation of distance function is provided - see {@link ObjectFloatUnitVectorL2}
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class ObjectFloatUnitVector extends ObjectFloatVector {
    /** Class serial version ID for serialization. */
    private static final long serialVersionUID = 23701L;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectFloatUnitVector.
     * @param data the data content of the new object
     * @throws IllegalArgumentException if the given array contains float that are not within [0,1] interval 
     */
    public ObjectFloatUnitVector(float[] data) throws IllegalArgumentException {
        super(data);
        if (! checkUnitVector(data))
            throw new IllegalArgumentException("Error creating [0,1]^n float vector from " + Arrays.toString(data));
    }

    /**
     * Creates a new instance of ObjectFloatUnitVector with randomly generated content data.
     * Content will be generated using normal distribution of random numbers from interval
     * [0;1).
     *
     * @param dimension number of dimensions to generate
     */
    public ObjectFloatUnitVector(int dimension) {
        super(dimension, 0, 1);
    }

    /**
     * Creates a new instance of ObjectFloatUnitVector from text stream.
     * @param stream the stream from which to read lines of text
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException if there was an I/O error during reading from the stream
     * @throws NumberFormatException if a line read from the stream does not consist of comma-separated or space-separated numbers
     */
    public ObjectFloatUnitVector(BufferedReader stream) throws EOFException, IOException, NumberFormatException {
        super(stream);
        if (! checkUnitVector(data))
            throw new IllegalArgumentException("Error creating [0,1]^n float vector from " + Arrays.toString(data));
    }


    //****************** Utility method for constructor ******************//

    /**
     * This method checks that the float vector components are within interval [0,1].
     * @param data the float vector to check
     * @return <tt>true</tt> if the floats are all correct,
     *         <tt>false</tt> if at least one is not within the interval
     */
    private static boolean checkUnitVector(float [] data) {
        for (float f : data)
            if ((0f > f) || (f > 1f))
                return false;
        return true;
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectFloatUnitVector loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectFloatVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFloatUnitVector(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

}

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
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;


/**
 * Implementation of the {@link ObjectFloatUnitVector} with an L2 (Euclidean) metric distance.
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectFloatUnitVectorL2 extends ObjectFloatUnitVector {
    /** Class serial version ID for serialization. */
    private static final long serialVersionUID = 23601L;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectFloatUnitVectorL2.
     * @param data the data content of the new object
     * @throws IllegalArgumentException if the given array contains float that are not within [0,1] interval 
     */
    public ObjectFloatUnitVectorL2(float[] data) throws IllegalArgumentException {
        super(data);
    }

    /**
     * Creates a new instance of ObjectFloatUnitVectorL2 with randomly generated content data.
     * Content will be generated using normal distribution of random numbers from interval
     * [0;1).
     *
     * @param dimension number of dimensions to generate
     */
    public ObjectFloatUnitVectorL2(int dimension) {
        super(dimension);
    }

    /**
     * Creates a new instance of ObjectFloatUnitVectorL2 from text stream.
     * @param stream the stream from which to read lines of text
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException if there was an I/O error during reading from the stream
     * @throws NumberFormatException if a line read from the stream does not consist of comma-separated or space-separated numbers
     */
    public ObjectFloatUnitVectorL2(BufferedReader stream) throws EOFException, IOException, NumberFormatException {
        super(stream);
    }


    //****************** Distance function ******************//

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        float[] objdata = ((ObjectFloatUnitVector)obj).data;
        if (objdata.length != data.length)
            throw new IllegalArgumentException("Cannot compute distance on different vector dimensions (" + data.length + ", " + objdata.length + ")");

        float powSum = 0;
        for (int i = 0; i < data.length; i++) {
            float dif = (data[i] - objdata[i]);
            powSum += dif * dif;
        }
        return (float)Math.sqrt(powSum);
    }

    @Override
    public float getMaxDistance() {
        return (float) Math.sqrt(data.length);
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectFloatUnitVector loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectFloatVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFloatUnitVectorL2(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

}

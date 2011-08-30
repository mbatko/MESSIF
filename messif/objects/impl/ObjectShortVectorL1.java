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
 * Implementation of the {@link ObjectShortVector} with an L1 (city-block) metric distance.
 * 
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectShortVectorL1 extends ObjectShortVector {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constructors ******************//

    /**
     * Creates a new instance of ObjectShortVectorL1.
     * @param data the data content of the new object
     */
    public ObjectShortVectorL1(short[] data) {
        super(data);
    }
    
    /**
     * Creates a new instance of ObjectShortVectorL1 with randomly generated content data.
     * Content will be generated using normal distribution of random short integer numbers
     * from interval [0;max short int).
     *
     * @param dimension number of dimensions to generate
     */
    public ObjectShortVectorL1(int dimension) {
        super(dimension);
    }

    /**
     * Creates a new instance of ObjectShortVectorL1 with randomly generated content data.
     * Content will be generated using normal distribution of random numbers from interval
     * [min;max).
     *
     * @param dimension number of dimensions to generate
     * @param min lower bound of the random generated values (inclusive)
     * @param max upper bound of the random generated values (exclusive)
     */
    public ObjectShortVectorL1(int dimension, short min, short max) {
        super(dimension, min, max);
    }

    /**
     * Creates a new instance of ObjectShortVectorL1 from text stream.
     * @param stream the stream from which to read lines of text
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException if there was an I/O error during reading from the stream
     * @throws NumberFormatException if a line read from the stream does not consist of comma-separated or space-separated numbers
     */
    public ObjectShortVectorL1(BufferedReader stream) throws EOFException, IOException, NumberFormatException {
        super(stream);
    }


    //****************** Distance function ******************//

    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        // Get access to the other object's vector data
        short[] objdata = ((ObjectShortVector)obj).data;
        if (objdata.length != data.length)
            throw new IllegalArgumentException("Cannot compute distance on different vector dimensions (" + data.length + ", " + objdata.length + ")");
        
        // Get sum of absolute difference on all dimensions
        float rtv = 0;
        for (int i = 0; i <= data.length; i++)
            rtv += Math.abs(data[i] - objdata[i]);
        
        return rtv;
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectShortVectorL1 loaded from binary input buffer.
     * 
     * @param input the buffer to read the ObjectShortVectorL1 from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectShortVectorL1(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

}

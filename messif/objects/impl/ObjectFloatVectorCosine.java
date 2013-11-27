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
 * Implementation of the {@link ObjectFloatVector} with a Cosine metric
 * distance.
 *
 * @author Jan Sedmidubsky, xsedmid@fi.muni.cz, FI MU Brno, Czech Republic
 */
public class ObjectFloatVectorCosine extends ObjectFloatVector {

    // class id for serialization
    private static final long serialVersionUID = 1L;

    //****************** Constructors ******************//
    /**
     * Creates a new instance of {@link ObjectFloatVectorCosine}.
     *
     * @param data the data content of the new object
     */
    public ObjectFloatVectorCosine(float[] data) {
        super(data);
    }

    /**
     * Creates a new instance of {@link ObjectFloatVectorCosine} with randomly
     * generated content data. Content will be generated using normal
     * distribution of random numbers from interval [min;max).
     *
     * @param dimension number of dimensions to generate
     * @param min lower bound of the random generated values (inclusive)
     * @param max upper bound of the random generated values (exclusive)
     */
    public ObjectFloatVectorCosine(int dimension, float min, float max) {
        super(dimension, min, max);
    }

    /**
     * Creates a new instance of {@link ObjectFloatVectorCosine} from text
     * stream.
     *
     * @param stream the stream from which to read lines of text
     * @throws EOFException if the end-of-file of the given stream is reached
     * @throws IOException if there was an I/O error during reading from the
     * stream
     * @throws NumberFormatException if a line read from the stream does not
     * consist of comma-separated or space-separated numbers
     */
    public ObjectFloatVectorCosine(BufferedReader stream) throws EOFException, IOException, NumberFormatException {
        super(stream);
    }

    //****************** Distance function ******************//
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        float[] objData = ((ObjectFloatVector) obj).data;
        if (objData.length != data.length) {
            throw new IllegalArgumentException("Cannot compute distance on different vector dimensions (" + data.length + ", " + objData.length + ")");
        }

        double productSum = 0f;
        double powASum = 0f;
        double powBSum = 0f;
        for (int i = 0; i < data.length; i++) {
            productSum += data[i] * objData[i];
            powASum += data[i] * data[i];
            powBSum += objData[i] * objData[i];
        }
        return 1f - (float) (Math.abs(productSum) / Math.sqrt(powASum * powBSum));
    }

    //************ BinarySerializable interface ************//
    /**
     * Creates a new instance of {@link ObjectFloatVectorCosine} loaded from
     * binary input buffer.
     *
     * @param input the buffer to read the ObjectFloatVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFloatVectorCosine(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }
}

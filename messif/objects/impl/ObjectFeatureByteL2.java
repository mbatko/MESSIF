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
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

public class ObjectFeatureByteL2 extends ObjectFeatureByte {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /****************** Constructors ******************/
    
    /**
     * Creates a new instance of object
     * @param data
     */
    public ObjectFeatureByteL2(short[] data) {
        super(data);
    }
    /**
     * Creates a new instance of object
     * @param data
     */
    public ObjectFeatureByteL2(float x,  float y,  float ori,  float scl, short[] data) {
        super(x, y, ori, scl, data);
    }

    /** Creates a new instance of object from stream */
    public ObjectFeatureByteL2(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }


    /** Metric function
     *      Implements Euclidean distance (L2 metric).
     * @return if the vectors are of different length, {@link LocalAbstractObject#MAX_DISTANCE} is returned.
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        // Get access to the other object's vector data
        short [] objdata = ((ObjectFeatureByte)obj).data;

        // We must have the same number of dimensions
        if (objdata.length != data.length)
            return MAX_DISTANCE;
        double distTreshSq = distThreshold * distThreshold;

        // Get sum of absolute difference on all dimensions
        double rtv = 0;
        for (int i = data.length - 1; i >= 0; i--) {
            rtv += (data[i] - objdata[i])*(data[i] - objdata[i]);
            if (rtv > distTreshSq)
                return Float.MAX_VALUE;
        }
        return ((float)Math.sqrt(rtv));
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectIntVector loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectIntVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFeatureByteL2(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

}

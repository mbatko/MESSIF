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

/**
 * 
 * @author xdohnal
 * @deprecated I do not know the purpose of this class, so I marked it as deprecated.
 */
@Deprecated
public class ObjectFeatureFloatWClustersL2 extends ObjectFeatureFloatWClusters {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /****************** Constructors ******************/

    /** Creates a new instance of object */
    public ObjectFeatureFloatWClustersL2(float x,  float y,  float ori,  float scl, float[] data, double clusterid) {
        super(x, y, ori, scl, data, clusterid);
    }

    /** Creates a new instance of object from stream */
    public ObjectFeatureFloatWClustersL2(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }


    /** Metric function
     *      Implements city-block distance measure (so-called L1 metric)
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        // Get access to the other object's vector data
        float [] objdata = ((ObjectFeatureFloatWClustersL2)obj).data;

        // We must have the same number of dimensions
        if (objdata.length != data.length)
            return MAX_DISTANCE;

        // Get sum of absolute difference on all dimensions
        float rtv = 0;
        for (int i = data.length - 1; i >= 0; i--)
            rtv += (data[i] - objdata[i])*(data[i] - objdata[i]);

        return ((float)Math.sqrt(rtv));
    }

    public float getClustersDistance (LocalAbstractObject obj) {
        double objclusterid = ((ObjectFeatureFloatWClusters)obj).clusterid;
        if (objclusterid == clusterid)
            return 0.0f;
        else
            return Float.MAX_VALUE;
    }
    
    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectIntVector loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectIntVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFeatureFloatWClustersL2(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }
}

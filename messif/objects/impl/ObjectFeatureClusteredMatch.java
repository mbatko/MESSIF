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

public class ObjectFeatureClusteredMatch extends ObjectFeatureClustered {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /****************** Constructors ******************/
    
    /**
     * Creates a new instance of object
     * @param data
     */
    public ObjectFeatureClusteredMatch(double clusterid) {
        super(clusterid);
    }
    /**
     * Creates a new instance of object
     * @param data
     */
    public ObjectFeatureClusteredMatch(float x,  float y,  float ori,  float scl, double clusterid) {
        super(x, y, ori, scl, clusterid);
    }

    /** Creates a new instance of object from stream */
    public ObjectFeatureClusteredMatch(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }
    
    /** Metric function
     *      Implements city-block distance measure (so-called L1 metric)
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        // Get access to the other object's vector data
        double objdata = ((ObjectFeatureClustered)obj).clusterid;

        return (objdata != clusterid) ? Float.MAX_VALUE : 0.0f;
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectIntVector loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectIntVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFeatureClusteredMatch(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }
}

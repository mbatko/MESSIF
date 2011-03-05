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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ObjectShortVectorL2 extends ObjectShortVector {
    
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of object */
    public ObjectShortVectorL2(short[] data) {
        super(data);
    }
    
    /** Creates a new instance of randomly generated object */
    public ObjectShortVectorL2(int dimension) {
        super(dimension);
    }

    /** Creates a new instance of object from stream */
    public ObjectShortVectorL2(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }
    
    
    /** 
     * Metric function implements Euclidean (L_2) metric
     */
    @Override
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        // Get access to the other object's vector data
        short[] objdata = ((ObjectShortVector)obj).data;

        // We must have the same number of dimensions
        if (objdata.length != data.length)
            return MAX_DISTANCE;
        
        // Get sum of absolute difference on all dimensions
        float rtv = 0;
        for (int i = data.length - 1; i >= 0; i--) {
            float dif = (data[i] - objdata[i]);
            rtv += dif * dif;
        }
        
        return ((float)Math.sqrt(rtv));
    }

    
    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectShortVectorL1 loaded from binary input buffer.
     * 
     * @param input the buffer to read the ObjectShortVectorL1 from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectShortVectorL2(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }
}

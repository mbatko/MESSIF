/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInputStream;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author xbarton
 */
public class ObjectIntVectorL2 extends ObjectIntVector {
    
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of object */
    public ObjectIntVectorL2(int[] data) {
        super(data);
    }
    
    /** Creates a new instance of randomly generated object */
    public ObjectIntVectorL2(int dimension) {
        super(dimension);
    }

    /** Creates a new instance of object from stream */
    public ObjectIntVectorL2(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }
    
    
    /** Metric function
     *      Implements city-block distance measure (so-called L1 metric)
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        // Get access to the other object's vector data
        int[] objdata = ((ObjectIntVector)obj).data;

        // We must have the same number of dimensions
        if (objdata.length != data.length)
            return MAX_DISTANCE;
        
        // Get sum of absolute difference on all dimensions
        float rtv = 0;
        for (int i = data.length - 1; i >= 0; i--)
            rtv += (data[i] - objdata[i])*(data[i] - objdata[i]);
        
        return ((float)Math.sqrt(rtv));
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectIntVector loaded from binary input stream.
     * 
     * @param input the stream to read the ObjectIntVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the stream
     */
    protected ObjectIntVectorL2(BinaryInputStream input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }


}

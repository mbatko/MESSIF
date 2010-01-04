/*
 * ObjectVectorL1.java
 *
 * Created on 3. kveten 2003, 20:09
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.LocalAbstractObject;


/**
 *
 * @author  xbatko
 */
public class ObjectFloatVectorL1 extends ObjectFloatVector {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of object */
    public ObjectFloatVectorL1(float[] data) {
        super(data);
    }
    
    /** Creates a new instance of randomly generated object */
    public ObjectFloatVectorL1(int dimension) {
        super(dimension);
    }

    /** Creates a new instance of object from stream */
    public ObjectFloatVectorL1(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }
    
    
    /** Metric function
     *      Implements city-block distance measure (so-called L1 metric)
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        // Get access to the other object's vector data
        float[] objdata = ((ObjectFloatVector)obj).data;
        // We must have the same number of dimensions
        if (objdata.length != data.length)
            return MAX_DISTANCE;
        
        // Get sum of absolute difference on all dimensions
        float rtv = 0;
        for (int i = data.length - 1; i >= 0; i--)
            rtv += Math.abs(data[i] - objdata[i]);
        
        return rtv;
    }

}

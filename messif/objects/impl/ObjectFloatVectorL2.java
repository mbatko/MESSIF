/*
 * Object.java
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
public class ObjectFloatVectorL2 extends ObjectFloatVector {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of object */
    public ObjectFloatVectorL2(float[] data) {
        super(data);
    }
    
    /** Creates a new instance of randomly generated object */
    public ObjectFloatVectorL2(int dimension) {
        super(dimension);
    }

    /** Creates a new instance of object from stream */
    public ObjectFloatVectorL2(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }

    /** Metric function
     *      Implements euclidean distance measure (so-called L2 metric)
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        float powSum = 0;
        
        for (int i = Math.min(this.data.length, ((ObjectFloatVector)obj).data.length) - 1; i >= 0; i--)
            powSum += Math.pow(this.data[i] - ((ObjectFloatVector)obj).data[i], 2);
        
        return (float)Math.sqrt(powSum);
    }
}

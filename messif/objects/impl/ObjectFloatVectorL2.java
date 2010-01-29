/*
 * Object.java
 *
 * Created on 3. kveten 2003, 20:09
 */

package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;


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
    public ObjectFloatVectorL2(int dimension, float min, float max) {
        super(dimension, min, max);
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

        float [] objData = ((ObjectFloatVector)obj).data;

        for (int i = Math.min(this.data.length, objData.length) - 1; i >= 0; i--) {
            float dif = (data[i] - objData[i]);
            powSum += dif * dif;
        }

        return (float)Math.sqrt(powSum);
    }

    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectFloatVectorL2 loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectFloatVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFloatVectorL2(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }
}

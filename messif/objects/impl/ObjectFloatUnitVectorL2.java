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
public class ObjectFloatUnitVectorL2 extends ObjectFloatUnitVector {

    /** Class serial version ID for serialization. */
    private static final long serialVersionUID = 23601L;
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of object */
    public ObjectFloatUnitVectorL2(float[] data) {
        super(data);
    }
    
    /** Creates a new instance of randomly generated object */
    public ObjectFloatUnitVectorL2(int dimension) {
        super(dimension);
    }

    /** Creates a new instance of object from stream */
    public ObjectFloatUnitVectorL2(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }

    /** Metric function
     *      Implements euclidean distance measure (so-called L2 metric)
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        float powSum = 0;

        float [] objData = ((ObjectFloatVector)obj).data;

        for (int i = data.length - 1; i >= 0; i--) {
            float dif = (data[i] - objData[i]);
            powSum += dif * dif;
        }
        return (float)Math.sqrt(powSum);
    }

    @Override
    public float getMaxDistance() {
        return (float) Math.sqrt(data.length);
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectFloatUnitVector loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectFloatVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFloatUnitVectorL2(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

}

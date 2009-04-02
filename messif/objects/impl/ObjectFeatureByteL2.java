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

    /** Creates a new instance of object */
    public ObjectFeatureByteL2(short[] data) {
        super(data);
    }

    /** Creates a new instance of object from stream */
    public ObjectFeatureByteL2(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
    }


    /** Metric function
     *      Implements city-block distance measure (so-called L1 metric)
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        // Get access to the other object's vector data
        short [] objdata = ((ObjectFeatureByte)obj).data;

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

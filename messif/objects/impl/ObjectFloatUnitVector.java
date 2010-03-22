package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

/**
 *
 * @author xnovak8
 */
public abstract class ObjectFloatUnitVector extends ObjectFloatVector {
    /** Class serial version ID for serialization. */
    private static final long serialVersionUID = 23701L;

    public ObjectFloatUnitVector(BufferedReader stream) throws IOException, NumberFormatException {
        super(stream);
        if (! checkUnitVector(data)) {
            throw new IllegalArgumentException("Error creating [0,1]^n float vector from " + Arrays.toString(data));
        }
    }

    public ObjectFloatUnitVector(int dimension) {
        super(dimension, 0, 1);
    }

    public ObjectFloatUnitVector(float[] data) throws IllegalArgumentException {
        super(data);
        if (! checkUnitVector(data)) {
            throw new IllegalArgumentException("Error creating [0,1]^n float vector from " + Arrays.toString(data));
        }
    }

    /**
     * This method checks that the float vector components are within interval [0,1].
     * @param data float vector to check
     * @return true, if the data is ok
     */
    private static boolean checkUnitVector(float [] data) {
        for (float f : data) {
            if ((0f > f) || (f > 1f)) {
                return false;
            }
        }
        return true;
    }


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of ObjectFloatUnitVector loaded from binary input buffer.
     *
     * @param input the buffer to read the ObjectFloatVector from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the buffer
     */
    protected ObjectFloatUnitVector(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

}

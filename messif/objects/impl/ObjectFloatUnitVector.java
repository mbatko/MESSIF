
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
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
        this.data = new float[dimension];
        for (; dimension > 0; dimension--)
            this.data[dimension - 1] = (float)(getRandomNormal());
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

}

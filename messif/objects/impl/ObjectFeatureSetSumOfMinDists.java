package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

public class ObjectFeatureSetSumOfMinDists extends ObjectFeatureSet {

    /** Class id for serialization. */
    private static final long serialVersionUID = 668L;

    /**
     * Creates a new instance of ObjectFeatureSetSumOfMinDists from a text stream.
     * @param stream the text stream to read an object from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
     public ObjectFeatureSetSumOfMinDists(BufferedReader stream) throws IOException {
         super(stream);
     }

     public ObjectFeatureSetSumOfMinDists(BinaryInput input, BinarySerializator serializator) throws IOException {
         super (input, serializator);
     }
    //****************** Distance function ******************//

    /**
     * The actual implementation of the non-metric function.
     * The distance is computed as the difference of this and <code>obj</code>'s locator hash-codes.
     * The array <code>metaDistances</code> is ignored.
     *
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold
     * @see LocalAbstractObject#getDistance
     */

    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        double setdistance = 0.0;
        double setdistancerev = 0.0;
        float retval = 0;
        float distances[][] = new float[((ObjectFeatureSet)obj).getObjectCount()][getObjectCount()];
        for (int i = 0; i < this.getObjectCount(); i++) {
            float shortest = Float.MAX_VALUE;
            for (int j = 0; j < ((ObjectFeatureSet)obj).getObjectCount(); j++) {
                distances[j][i] = this.getObject(i).getDistance(((ObjectFeatureSet)obj).getObject(j), distThreshold);
                if (distances[j][i] < shortest) {
                    shortest = distances[j][i];
                }
            }
            setdistance += shortest;
            if (setdistance > 2 * distThreshold) {
                return (float) Math.min (setdistance, Float.MAX_VALUE);
            }
            // we can stop now if (setdistance > 2 * distTreshold) and return distTreshold
        }
        for (int j = 0; j < ((ObjectFeatureSet)obj).getObjectCount(); j++) {
            float shortest = Float.MAX_VALUE;
            for (int i = 0; i < getObjectCount(); i++) {
                if (distances[j][i] < shortest) {
                    shortest = distances[j][i];
                }
            }
            setdistancerev += shortest;
            if (setdistancerev + setdistance > 2 * distThreshold) {
                return (float) (Math.min (setdistancerev + setdistance, Float.MAX_VALUE));
            }
            // we can stop now if (setdistancerev > 2 * distTreshold) or if ((setdistancerev + setdistance) > 2 * distTreshold) and return distTreshold
        }
        if ((setdistance + setdistancerev) / 2 > Float.MAX_VALUE) {
            retval = Float.MAX_VALUE;
        } else {
            retval = (float) ((setdistance + setdistancerev) / 2.0);
        }
        return retval;
    }
}

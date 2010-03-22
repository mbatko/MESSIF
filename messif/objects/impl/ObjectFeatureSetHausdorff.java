package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

public class ObjectFeatureSetHausdorff  extends ObjectFeatureSet {

    /** class id for serialization */
    private static final long serialVersionUID = 660L;

    /**
     * Creates a new instance of ObjectFeatureSetHausdorff from a text stream.
     * @param stream the text stream to read an object from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
     public ObjectFeatureSetHausdorff(BufferedReader stream) throws IOException {
         super(stream);
     }

     public ObjectFeatureSetHausdorff(BinaryInput input, BinarySerializator serializator) throws IOException {
         super (input, serializator);
     }

    //****************** Distance function ******************//

    /**
     * The actual implementation of the metric Hausdorff function.
     * Method {@link #getDistance(messif.objects.LocalAbstractObject, float[], float)}
     * is called with <tt>null</tt> meta distances array in order to compute the
     * actual distance.
     *
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold
     * @see LocalAbstractObject#getDistance
     */
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        float setdistance = 0;
        float setdistancerev = 0;
        float distances[][] = new float[((ObjectFeatureSet)obj).getObjectCount()][getObjectCount()];
        for (int i = 0; i < this.getObjectCount(); i++) {
            float shortest = Float.MAX_VALUE;
            for (int j = 0; j < ((ObjectFeatureSet)obj).getObjectCount(); j++) {
                distances[j][i] = this.getObject(i).getDistance(((ObjectFeatureSet)obj).getObject(j), distThreshold);
                if (distances[j][i] < shortest) {
                    shortest = distances[j][i];
                }
            }
            if (shortest > setdistance) {
                setdistance = shortest; 
            }
            if (setdistance > distThreshold) {
                return setdistance;
            }
            //if (setdistance == Float.MAX_VALUE) {
            //    break;
            //}
        }
        for (int j = 0; j < ((ObjectFeatureSet)obj).getObjectCount(); j++) {
            float shortest = Float.MAX_VALUE;
            for (int i = 0; i < getObjectCount(); i++) {
                if (distances[j][i] < shortest) {
                    shortest = distances[j][i];
                }
            }
            if (shortest > setdistancerev) {
                setdistancerev = shortest;
            }
            if (setdistance + setdistancerev > distThreshold) {
                return setdistance + setdistancerev;
            }
            if (setdistancerev == Float.MAX_VALUE) {
                break;
            }
        }
        return Math.max(setdistance, setdistancerev);
    }
}

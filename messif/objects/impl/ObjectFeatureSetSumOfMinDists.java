/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
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

        ObjectFeatureSet objSet = (ObjectFeatureSet) obj;

        float distances[][] = new float[objSet.getObjectCount()][getObjectCount()];
        for (int i = 0; i < this.getObjectCount(); i++) {
            float shortest = Float.MAX_VALUE;
            for (int j = 0; j < objSet.getObjectCount(); j++) {
                distances[j][i] = this.getObject(i).getDistance(objSet.getObject(j), distThreshold);
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
        for (int j = 0; j < objSet.getObjectCount(); j++) {
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
            retval = (float) ((setdistance + setdistancerev) / 2.0) / (this.getObjectCount() + objSet.getObjectCount());
        }
        return retval;
    }
}

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
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import messif.objects.util.SequenceMatchingCost;

/**
 * Compute the number of similar features between this and other {@link ObjectFeatureSet}. The result is inversed so it
 * can be used as a distance within [0,1].
 * 
 * In particular, it chooses the smaller (SM) of the feature sets (this and the other object) and then it
 * finds for each feature in SM the most similar feature to it from the other set. Based on these features distance, 
 * 1 (very similar) or 0.5 (similar) is added to a total sum. Finally, the total sum is normalized by dividing by size of SM.
 * The resulting distance is 1f - the normalized sum.
 * 
 * CAVEAT: This distance may not be a metric!!! It may break triangle inequality. Please check it and change this warning appropriately.
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class ObjectFeatureSetMinNumOfSimilar extends ObjectFeatureSet {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    public ObjectFeatureSetMinNumOfSimilar() {
    }

    public ObjectFeatureSetMinNumOfSimilar(String locatorURI, Collection<? extends ObjectFeature> objects) {
        super(locatorURI, objects);
    }

    public ObjectFeatureSetMinNumOfSimilar(ObjectFeatureSet supSet, float minX, float maxX, float minY, float maxY) {
        super(supSet, minX, maxX, minY, maxY);
    }

    public ObjectFeatureSetMinNumOfSimilar(ObjectFeatureSet superSet) {
        super(superSet);
    }

    public ObjectFeatureSetMinNumOfSimilar (BufferedReader stream) throws IOException {
        super(stream);
    }

    public ObjectFeatureSetMinNumOfSimilar(BufferedReader stream, Map<String, ? extends Serializable> additionalParameters) throws IOException {
        super(stream, additionalParameters);
    }

    public ObjectFeatureSetMinNumOfSimilar (BinaryInput input, BinarySerializator serializator) throws IOException {
        super (input, serializator);
    }

    /** Default defaultCost */
    public static SequenceMatchingCost defaultCost = SequenceMatchingCost.SIFT_DEFAULT;
    
    @Override
    protected float getDistanceImpl(LocalAbstractObject o, float distThreshold) {
        boolean queryIsSmall = true;
        ObjectFeatureSet objS = this;
        ObjectFeatureSet objB = (ObjectFeatureSet)o;
        
        // Find the smaller set
        if (objS.getObjectCount() > objB.getObjectCount()) {
            ObjectFeatureSet x = objS;
            objS = objB;
            objB = x;
            queryIsSmall = false;
        }
        
        int maxSim = objS.getObjectCount();
        int numExact = 0;
        int numApprox = 0;
        
        for (ObjectFeature fS : objS.objects) {
            float minC = Float.NEGATIVE_INFINITY;
            for (ObjectFeature fB : objB.objects) {
                // Find the most similar feature in objB
                float c = (queryIsSmall) ? defaultCost.getCost(fS, fB) : defaultCost.getCost(fB, fS);     // Correct distance may be defined on query object only...
                if (c > minC)
                    minC = c;
            }
            if (minC >= defaultCost.getMatchExact())
                numExact++;
            else if (minC < defaultCost.getMatchExact() && minC >= defaultCost.getMatchApprox())
                numApprox++;
        }
        
        if (maxSim == 0)
            return getMaxDistance();
        else
            return getMaxDistance() - ((float)numExact + 0.5f * (float)numApprox) / (float)maxSim;
    }

    @Override
    public float getMaxDistance() {
        return 1f;
    }

}

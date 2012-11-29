package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import messif.objects.*;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import messif.objects.util.SequenceMatchingCost;
import messif.objects.util.SortDimension;
import messif.utility.ArrayResetableIterator;
import messif.utility.ResetableIterator;

/**
 * Needleman-Wunsch global sequence alignment algorithm.
 * 
 * Default scoring is set to {@link SequenceMatchingCost#SIFT_DEFAULT}.
 * A user-specific scoring can be set throgh static member {@link #defaultCost}.
 * 
 * Distance is computed based on the similarity evaluted after projecting the feature set to X axis and to Y axis.
 * In particular, distance is 1 - (sim_X + simY) / 2*max_sim, where 
 * max_sim = max(featureCount1, featureCount2) * max_cost.
 * 
 * If the feature sets are ordered in advance, the distance is returned only by this ordering 
 * (so no reorderings by X and Y axes are done)!!!
 * 
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class ObjectFeatureSetNeedlemanWunsch extends ObjectFeatureOrderedSet {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;

    public static SequenceMatchingCost defaultCost = SequenceMatchingCost.SIFT_DEFAULT;

    public ObjectFeatureSetNeedlemanWunsch(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

    public ObjectFeatureSetNeedlemanWunsch(BufferedReader stream) throws IOException {
        super(stream);
    }

    public ObjectFeatureSetNeedlemanWunsch(String locatorURI, int width, int height, 
                                         Collection<? extends ObjectFeature> objects) {
        super(locatorURI, width, height, objects);
    }
    
    public static float getMaximumSimilarity(SequenceMatchingCost cost, int featureCount1, int featureCount2) {
        if (featureCount1 == 0 && featureCount2 == 0)
            return cost.getMaxCost();       // This is just a protection against devision by zero exception.
        else
            return Math.max(featureCount1, featureCount2) * cost.getMaxCost();
    }
    
    @Override
    public float getDistanceImpl(LocalAbstractObject o, float distTreshold) {
        ObjectFeatureOrderedSet obj = (ObjectFeatureOrderedSet)o;
        
        if (this.isFeaturesOrdered() && this.getOrderOfFeatures() == obj.getOrderOfFeatures()) {
            return getDistance(defaultCost, new ArrayResetableIterator<ObjectFeature>(objects), new ArrayResetableIterator<ObjectFeature>(obj.objects));
        } else {
            orderFeatures(SortDimension.sortDimensionX);
            obj.orderFeatures(SortDimension.sortDimensionX);
            float simX = getSimilarity(defaultCost, new ArrayResetableIterator<ObjectFeature>(objects), new ArrayResetableIterator<ObjectFeature>(obj.objects));

            orderFeatures(SortDimension.sortDimensionY);
            obj.orderFeatures(SortDimension.sortDimensionY);
            float simY = getSimilarity(defaultCost, new ArrayResetableIterator<ObjectFeature>(objects), new ArrayResetableIterator<ObjectFeature>(obj.objects));

            return 1f - (simX + simY) / (2 * getMaximumSimilarity(defaultCost, this.getObjectCount(), obj.getObjectCount()));
        }
    }

    public static void setDefaultCost(SequenceMatchingCost cost) {
        defaultCost = cost;
    }

    //****************** Implementation ******************//

    /** 
     * CAVEAT: This implementation is different from the implementation in {@link #getDistanceImpl(messif.objects.LocalAbstractObject, float) getDistanceImpl}
     */
    protected static float getDistance(SequenceMatchingCost cost, ResetableIterator<ObjectFeature> it1, ResetableIterator<ObjectFeature> it2) {
        return 1f - getSimilarity(cost, it1, it2) / getMaximumSimilarity(cost, it1.size(), it2.size());
    }    

    public static float getSimilarity(SequenceMatchingCost cost, ResetableIterator<ObjectFeature> it1, ResetableIterator<ObjectFeature> it2) {
        int n = it1.size();       // length of this "string"
        int m = it2.size();       // length of o."string"

        if (m == 0 || n == 0)
            return 0f;

        //create matrix (n+1)x(m+1)
        final float[][] d = new float[n + 1][m + 1];

        //put row and column numbers in place
        for (int i = 0; i <= n; i++) {
            d[i][0] = 0;
        }
        for (int j = 0; j <= m; j++) {
            d[0][j] = 0;
        }

        // cycle through rest of table filling values from the lowest cost value of the three part cost function
        for (int i = 1; i <= n; i++) {
            ObjectFeature o1 = it1.next();

            for (int j = 1; j <= m; j++) {
                ObjectFeature o2 = it2.next();
                // get the substution cost
                float c = Math.max(0, cost.getCost(o1, o2));

                // find lowest cost at point from three possible
                d[i][j] = max3(d[i - 1][j] - cost.getGapOpening(), d[i][j - 1] - cost.getGapOpening(), d[i - 1][j - 1] + c);
            }
            it2.reset();
        }
        return Math.max(d[n][m], 0);
    }
    
    public static float getSimilarity(SequenceMatchingCost cost, float[][] distances) {
        int n = distances.length;       // length of this "string"
        int m = distances[0].length;    // length of o."string"

        if (m == 0 || n == 0)
            return 0f;
        
        //create matrix (n+1)x(m+1)
        final float[][] d = new float[n + 1][m + 1];

        //put row and column numbers in place
        for (int i = 0; i <= n; i++) {
            d[i][0] = 0;
        }
        for (int j = 0; j <= m; j++) {
            d[0][j] = 0;
        }

        // cycle through rest of table filling values from the lowest cost value of the three part cost function
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                // get the substution cost
                float c = Math.max(0, cost.getCost(distances[i-1][j-1]));

                // find lowest cost at point from three possible
                d[i][j] = max3(d[i - 1][j] - cost.getGapOpening(), d[i][j - 1] - cost.getGapOpening(), d[i - 1][j - 1] + c);
            }
        }
        return Math.max(d[n][m], 0);
    }
    
    public static float max3(final float f1, final float f2, final float f3) {
        return Math.max(Math.max(f1, f2), f3);
    }
        
    public static float getDistanceByWindowing(SequenceMatchingCost cost, SlidingWindow wnd,
                                               ObjectFeatureOrderedSet fs1, ObjectFeatureOrderedSet fs2) {
        float dist = LocalAbstractObject.MAX_DISTANCE;
        Iterator<SortDimension.Window> it1 = fs1.windowIterator(wnd);
        while (it1.hasNext()) {
            SortDimension.Window w1 = it1.next();
            Iterator<SortDimension.Window> it2 = fs2.windowIterator(wnd);
            while (it2.hasNext()) {
                SortDimension.Window w2 = it2.next();
                float wndDist = getDistance(cost, fs1.iterator(w1), fs2.iterator(w2));
                if (wndDist < dist)
                    dist = wndDist;
            }
        }
        return dist;
    }
}

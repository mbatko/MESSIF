/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import messif.objects.util.SequenceMatchingCost;
import messif.utility.ArrayResetableIterator;
import messif.utility.ResetableIterator;

/**
 *
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 */
public class ObjectFeatureSetSmithWaterman extends ObjectFeatureOrderedSet {
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 1L;
    
    protected SequenceMatchingCost cost;

    public ObjectFeatureSetSmithWaterman(BinaryInput input, BinarySerializator serializator) throws IOException {
        super(input, serializator);
    }

    public ObjectFeatureSetSmithWaterman(BufferedReader stream) throws IOException {
        super(stream);
    }

    public ObjectFeatureSetSmithWaterman(String locatorURI, int width, int height, 
                                         Collection<? extends ObjectFeature> objects,
                                         SequenceMatchingCost cost) {
        super(locatorURI, width, height, objects);
        this.cost = cost;
    }
    
    public static float getMaximumSimilarity(SequenceMatchingCost cost, int featureCount1, int featureCount2) {
//        return (featureCount1 + featureCount2) * cost.getMaxCost();
        if (featureCount1 == 0 || featureCount2 == 0)
            return cost.getMaxCost();       // This is just a protection against devision by zero exception.
        else
            return Math.min(featureCount1, featureCount2) * cost.getMaxCost();
    }
    
    @Override
    public float getDistanceImpl(LocalAbstractObject o, float distTreshold) {
        ObjectFeatureOrderedSet obj = (ObjectFeatureOrderedSet)o;
        
        if (this.isFeaturesOrdered() && this.getOrderOfFeatures() == obj.getOrderOfFeatures()) {
            return getDistance(this.cost, new ArrayResetableIterator<ObjectFeature>(objects), new ArrayResetableIterator<ObjectFeature>(obj.objects));
        } else {
            orderFeatures(ObjectFeatureOrderedSet.sortDimensionX);
            obj.orderFeatures(ObjectFeatureOrderedSet.sortDimensionX);
            float simX = getSimilarity(cost, new ArrayResetableIterator<ObjectFeature>(objects), new ArrayResetableIterator<ObjectFeature>(obj.objects));

            orderFeatures(ObjectFeatureOrderedSet.sortDimensionY);
            obj.orderFeatures(ObjectFeatureOrderedSet.sortDimensionY);
            float simY = getSimilarity(cost, new ArrayResetableIterator<ObjectFeature>(objects), new ArrayResetableIterator<ObjectFeature>(obj.objects));

            return 1f - (simX + simY) / (2 * getMaximumSimilarity(cost, this.getObjectCount(), obj.getObjectCount()));
        }
    }

    public static float getDistance(SequenceMatchingCost cost, ResetableIterator<ObjectFeature> it1, ResetableIterator<ObjectFeature> it2) {
        return 1f - getSimilarity(cost, it1, it2) / getMaximumSimilarity(cost, it1.size(), it2.size());
    }
    
    public static float getSimilarity(SequenceMatchingCost cost, ResetableIterator<ObjectFeature> it1, ResetableIterator<ObjectFeature> it2) {
        int n = it1.size();       // length of this "string"
        int m = it2.size();       // length of o."string"

        if (m == 0 || n == 0)
            return 0f;
        float max = 0f;

        float H[][] = new float[m+1][n+1];
        float E[][] = new float[m+1][n+1];
        float F[][] = new float[m+1][n+1];

        // zero the first line and first column
        for (int i = 0; i <= m; i++) {
            H[i][0] = 0.0f;
            E[i][0] = Float.NEGATIVE_INFINITY;//0.0f;
        }
        for (int j = 0; j <= n; j++) {
            H[0][j] = 0.0f;
            F[0][j] = Float.NEGATIVE_INFINITY;//0.0f;
        }

        for (int i = 1; i <= m; i++) {
            ObjectFeature o2 = it2.next();
            for (int j = 1; j <= n; j++) {
                ObjectFeature o1 = it1.next();
                E[i][j] = Math.max(E[i][j-1] - cost.getGapContinue(), H[i][j-1] - cost.getGapOpening());
                F[i][j] = Math.max(F[i-1][j] - cost.getGapContinue(), H[i-1][j] - cost.getGapOpening());
                
                H[i][j] = max4(
                        0,
                        E[i][j],
                        F[i][j],
                        H[i-1][j-1] + cost.getCost(o2, o1)
                        );

                if (H[i][j] > max) {
                    // i and j holds now the end of the sequence
                    max = H[i][j];
                }
            }
            it1.reset();
        }
        return max;
    }
    
    public static float getSimilarityN(SequenceMatchingCost cost, ResetableIterator<ObjectFeature> it1, ResetableIterator<ObjectFeature> it2) {
        int m = it1.size();       // length of this "string"
        int n = it2.size();       // length of o."string"

        if (m == 0 || n == 0)
            return 0f;
        float max = 0f;

        m++;
        n++;
        
        float f; // score of alignment x1...xi to y1...yi if xi aligns to yi
        float[] g = new float[n]; // score if xi aligns to a gap after yi
        float h; // score if yi aligns to a gap after xi
        float[] v = new float[n]; // best score of alignment x1...xi to
        // y1...yi
        float vDiagonal;
        
        g[0] = Float.NEGATIVE_INFINITY;
        h = Float.NEGATIVE_INFINITY;
        v[0] = 0;
        
        for (int j = 1; j < n; j++) {
            g[j] = Float.NEGATIVE_INFINITY;
            v[j] = 0;
        }
        
        float similarityScore, g1, g2, h1, h2;
        
        for (int i = 1, k = n; i < m; i++, k += n) {
            it2.reset();
            ObjectFeature obj1 = it1.next();
            
            h = Float.NEGATIVE_INFINITY;
            vDiagonal = v[0];
            for (int j = 1, l = k + 1; j < n; j++, l++) {
                ObjectFeature obj2 = it2.next();
                similarityScore = cost.getCost(obj1, obj2); // matrix[a1[i - 1]][a2[j - 1]];

                // Fill the matrices
                f = vDiagonal + similarityScore;
                
                g1 = g[j] - cost.getGapContinue();
                g2 = v[j] - cost.getGapOpening();
                if (g1 > g2) {
                    g[j] = g1;
                } else {
                    g[j] = g2;
                }
                
                h1 = h - cost.getGapContinue();
                h2 = v[j - 1] - cost.getGapOpening();
                if (h1 > h2) {
                    h = h1;
                } else {
                    h = h2;
                }
                
                vDiagonal = v[j];
                v[j] = max4(f, g[j], h, 0f);

                if (v[j] > max) {
                    max = v[j];
                }
            }
        }
        return max;        
    }
    
    
    public static float getSimilarity(SequenceMatchingCost cost, float[][] distances) {
        int n = distances.length;       // length of this "string"
        int m = distances[0].length;    // length of o."string"

        if (m == 0 || n == 0)
            return 0f;
        float max = 0.0f;

        float H[][] = new float[m+1][n+1];
        float E[][] = new float[m+1][n+1];
        float F[][] = new float[m+1][n+1];

        // zero the first line and first column
        for (int i = 0; i <= m; i++) {
            H[i][0] = 0.0f;
            E[i][0] = 0.0f;
        }
        for (int j = 0; j <= n; j++) {
            H[0][j] = 0.0f;
            F[0][j] = 0.0f;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                E[i][j] = Math.max(E[i][j-1] - cost.getGapContinue(), H[i][j-1] - cost.getGapOpening());
                F[i][j] = Math.max(F[i-1][j] - cost.getGapContinue(), H[i-1][j] - cost.getGapOpening());

                H[i][j] = max4(
                        0,
                        E[i][j],
                        F[i][j],
                        H[i-1][j-1] + cost.getCost(distances[j][i])
                        );

                if (H[i][j] > max) {
                    // i and j holds now the end of the sequence
                    max = H[i][j];
                }
            }
        }
        return max;
    }

    public static float max4 (final float f1, final float f2, final float f3, final float f4) {
        return Math.max(Math.max(f1, f2), Math.max(f3, f4));
    }
    
    public static float getDistanceByWindowing(SequenceMatchingCost cost, 
                                                 int wndWidth, int wndHeight, int shiftX, int shiftY,
                                                 ObjectFeatureOrderedSet fs1, ObjectFeatureOrderedSet fs2) {
        float dist = LocalAbstractObject.MAX_DISTANCE;
        Iterator<Window> it1 = fs1.windowIterator(wndWidth, wndHeight, shiftX, shiftY);
        while (it1.hasNext()) {
            Window w1 = it1.next();
            Iterator<Window> it2 = fs2.windowIterator(wndWidth, wndHeight, shiftX, shiftY);
            while (it2.hasNext()) {
                Window w2 = it2.next();
                float wndDist = getDistance(cost, fs1.iterator(w1), fs2.iterator(w2));
                if (wndDist < dist)
                    dist = wndDist;
            }
        }
        return dist;
    }
}


package messif.objects.impl;

import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

public class ObjectFeatureSetNumOfSimilar extends ObjectFeatureSetStringWindow {
    private static final long serialVersionUID = 1L;

    public ObjectFeatureSetNumOfSimilar () {
        super ();
    }

    public ObjectFeatureSetNumOfSimilar (BufferedReader stream) throws IOException {
        super(stream);
    }

    public ObjectFeatureSetNumOfSimilar (BinaryInput input, BinarySerializator serializator) throws IOException {
        super (input, serializator);
    }

    public ObjectFeatureSetNumOfSimilar (ObjectFeatureSetStringWindow superSet, float minX, float maxX, float minY, float maxY) {
        super (superSet, minX, maxX, minY, maxY);
    }

    public ObjectFeatureSetNumOfSimilar (ObjectFeatureSetStringWindow superSet) {
        super (superSet);
    }

    public float[] getDistanceInOneAxis (ObjectFeatureSet set2, Coordinate coord) {
        return getDistanceInOneAxis(set2, coord, 0, Float.MAX_VALUE, 0, Float.MAX_VALUE);
    }

    public float[] getDistanceInOneAxis (float[][] distances) {
        int n = distances.length;
        float retvalue[] = new float[2];
        if (n == 0)
        {
            retvalue[0] = 0;
            retvalue[1] = 0;
            return retvalue;
        }

        int m = distances[0].length;
        float soucetexact = 0;
        float soucetapprox = 0;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                float c = getCost(distances[j][i]);
                if (c == matchScoreExact) {
                    soucetexact++;
                    soucetapprox++;
                } else if (c >= matchScoreApprox) {
                    soucetapprox++;
                }
            }
        }
        retvalue[0] = soucetexact;
        retvalue[1] = soucetapprox;
        return retvalue;
    }
    
    // assuming that both set1 and set2 are sorted with respect to the "coordinate"
    public float[] getDistanceInOneAxis (ObjectFeatureSet set2,
            Coordinate coord,
            float MinCoordSet1,
            float MaxCoordSet1,
            float MinCoordSet2,
            float MaxCoordSet2) {
        
        int n = getObjectCount();
        int m = set2.getObjectCount();
        float [] emptyarr = new float[] { 0.0f, 0.0f };
        if (n == 0)
            return emptyarr;
        if (m == 0)
            return emptyarr;

        int startCoordSet1 = 0; int stopCoordSet1 = 0;
        int startCoordSet2 = 0; int stopCoordSet2 = 0;

        // count the number of features within the allowed range
        while ( ((coord == Coordinate.CoordX) ?
                ((ObjectFeature) getObject(startCoordSet1)).getX() :
                ((ObjectFeature) getObject(startCoordSet1)).getY()) <= MinCoordSet1 && startCoordSet1 < n) {
            startCoordSet1++;
        }
        stopCoordSet1 = startCoordSet1;
        while ( ((coord == Coordinate.CoordX) ?
                ((ObjectFeature) getObject(startCoordSet1)).getX() :
                ((ObjectFeature) getObject(startCoordSet1)).getY()) <= MaxCoordSet1 && stopCoordSet1 < n) {
            stopCoordSet1++;
        }
        n = stopCoordSet1 - startCoordSet1;
        if (n == 0)
            return emptyarr;

        while ( ((coord == Coordinate.CoordX) ?
                ((ObjectFeature) set2.getObject(startCoordSet1)).getX() :
                ((ObjectFeature) set2.getObject(startCoordSet1)).getY()) <= MinCoordSet2 && startCoordSet2 < m) {
            startCoordSet2++;
        }
        stopCoordSet1 = startCoordSet1;
        while ( ((coord == Coordinate.CoordX) ?
                ((ObjectFeature) set2.getObject(startCoordSet1)).getX() :
                ((ObjectFeature) set2.getObject(startCoordSet1)).getY()) <= MaxCoordSet2 && stopCoordSet2 < m) {
            stopCoordSet2++;
        }
        m = stopCoordSet2 - startCoordSet2;
        if (m == 0)
            return emptyarr;

        float distances[][] = new float[n][m];
        for (int i1 = n - 1; i1 >= 0; i1--) {
            for (int i2 = m - 1; i2 >= 0; i2--) {
                distances[i1][i2] = getObject(i1).getDistance(set2.getObject(i2));
            }
        }

        return getDistanceInOneAxis(distances);
    }

    @Override
    protected float getDistanceImpl(LocalAbstractObject o, float distThreshold) {
        ObjectFeatureSet obj = (ObjectFeatureSet)o;
        int numExact = 0;
        int numApprox = 0;
        int maxSim = obj.objects.size() * this.objects.size();
        
        for (ObjectFeature f2 : obj.objects) {
            for (ObjectFeature f1 : this.objects) {
                float c = getCost(f1, f2);
                if (c == matchScoreExact)
                    numExact++;
                else if (c >= matchScoreApprox)
                    numApprox++;
            }
        }
        
        return 1f - ((float)numExact + 0.5f * (float)numApprox) / (float)maxSim;
    }
}

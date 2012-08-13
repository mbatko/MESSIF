package messif.objects.impl;

import messif.objects.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;
import messif.utility.SortedCollection;

public class ObjectFeatureSetNeedlemanWunsch extends ObjectFeatureSetStringWindow {
    private static final long serialVersionUID = 1L;
    
    public ObjectFeatureSetNeedlemanWunsch () {
        super ();
    }

    public ObjectFeatureSetNeedlemanWunsch (BufferedReader stream) throws IOException {
        super(stream);
    }

    public ObjectFeatureSetNeedlemanWunsch (BinaryInput input, BinarySerializator serializator) throws IOException {
        super (input, serializator);
    }

    public ObjectFeatureSetNeedlemanWunsch (ObjectFeatureSetStringWindow superSet, float minX, float maxX, float minY, float maxY) {
        super (superSet, minX, maxX, minY, maxY);
    }

    public ObjectFeatureSetNeedlemanWunsch (ObjectFeatureSetStringWindow superSet) {
        super (superSet);
    }

    public float getDistanceInOneAxis (ObjectFeatureSet set2,
            Coordinate coord) {
        return getDistanceInOneAxis(set2, coord, 0.0f, Float.MAX_VALUE, 0.0f, Float.MAX_VALUE);
    }

    public float getDistanceInOneAxis (float [][] distances) {
        final float[][] d; // matrix


        int n = distances.length;
        if (n == 0)
            return 0;
        int m = distances[0].length;
        int i, j;
        float cost;
        
        //create matrix (n+1)x(m+1)
        d = new float[n + 1][m + 1];

        //put row and column numbers in place
        for (i = 0; i <= n; i++) {
            d[i][0] = 0;
        }
        for (j = 0; j <= m; j++) {
            d[0][j] = 0;
        }

        // cycle through rest of table filling values from the lowest cost value of the three part cost function
        for (i = 1; i <= n; i++) {
            for (j = 1; j <= m; j++) {
                // get the substution cost
                // cost = getCost(getObject(i - 1), obj.getObject(j - 1));
                cost = Math.max(0, getCost(distances[i-1][j-1]));

                // find lowest cost at point from three possible
                d[i][j] = max3(d[i - 1][j] - gapCostOpening, d[i][j - 1] - gapCostOpening, d[i - 1][j - 1] + cost);
            }
        }
        return Math.max(d[i-1][j-1], 0);

    }
    
    public float getDistanceInOneAxis (ObjectFeatureSet set2,
            Coordinate coord,
            float MinCoordSet1,
            float MaxCoordSet1,
            float MinCoordSet2,
            float MaxCoordSet2) {
            
        int n; // length of s
        int m; // length of t

        // check for zero length input
        n = getObjectCount();
        m = set2.getObjectCount();
        if (n == 0) {
            return 0;
        }
        if (m == 0) {
            return 0;
        }
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
            return m;

        while ( ((coord == Coordinate.CoordX) ?
                ((ObjectFeature) set2.getObject(startCoordSet1)).getX() :
                ((ObjectFeature) set2.getObject(startCoordSet1)).getY()) <= MinCoordSet2 && startCoordSet2 < m) {
            startCoordSet2++;
        }
        stopCoordSet2 = startCoordSet2;
        while ( ((coord == Coordinate.CoordX) ?
                ((ObjectFeature) set2.getObject(startCoordSet1)).getX() :
                ((ObjectFeature) set2.getObject(startCoordSet1)).getY()) <= MaxCoordSet2 && stopCoordSet2 < m) {
            stopCoordSet2++;
        }
        m = stopCoordSet2 - startCoordSet2;
        if (m == 0)
            return n;
        float distances[][] = new float[n][m];
        for (int i1 = n - 1; i1 >= 0; i1--) {
            for (int i2 = m - 1; i2 >= 0; i2--) {
                distances[i1][i2] = getObject(i1).getDistance(set2.getObject(i2));
            }
        }

        return getDistanceInOneAxis(distances);

    }
    
    @Override
    public float getDistanceImpl (LocalAbstractObject o, float distTreshold) {
        ObjectFeatureSet obj = (ObjectFeatureSet) o;

        Collections.sort(objects, new ObjectLocalFeatureComparatorX());
        Collections.sort(obj.objects, new ObjectLocalFeatureComparatorX());
        // scxthis.addAll(this. objects);
        float rx = getDistanceInOneAxis (obj, Coordinate.CoordX);

        Collections.sort(objects, new ObjectLocalFeatureComparatorY());
        Collections.sort(obj.objects, new ObjectLocalFeatureComparatorY());
        float ry = getDistanceInOneAxis (obj, Coordinate.CoordY);

        float res = rx + ry; // (rx + ry) / 2;

        //normalise into zero to one region from min max possible
        /*
        float maxValue = Math.max(getObjectCount(), obj.getObjectCount());
        float minValue = maxValue;
        if (getMaxCost() > gapCost) {
            maxValue *= equalityTreshold;
        } else {
            maxValue *= gapCost;
        }
        if (getMinCost() < gapCost) {
            minValue *= getMinCost();
        } else {
            minValue *= gapCost;
        }
        if (minValue < 0.0f) {
            maxValue -= minValue;
            res -= minValue;
        }

        //check for 0 maxLen
        if (maxValue == 0) {
            return 1.0f; //as both strings identically zero length
        } else {
            //return actual / possible NeedlemanWunch distance to get 0-1 range
            return 1.0f - (res / maxValue);
        }*/
        return res;
    }
}

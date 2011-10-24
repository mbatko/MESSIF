
package messif.objects.impl;

import messif.objects.*;
import java.io.*;
import java.util.Collections;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

public class ObjectFeatureSetSmithWatermanObsolete extends ObjectFeatureSetStringWindow {

    private static final long serialVersionUID = 1L;
    
    public ObjectFeatureSetSmithWatermanObsolete () {
        super();
    }

    public ObjectFeatureSetSmithWatermanObsolete(BufferedReader stream) throws IOException {
        super(stream);
    }

    public ObjectFeatureSetSmithWatermanObsolete(BinaryInput input, BinarySerializator serializator) throws IOException {
        super (input, serializator);
    }

    public ObjectFeatureSetSmithWatermanObsolete (ObjectFeatureSetStringWindow superSet, float minX, float maxX, float minY, float maxY) {
        super (superSet, minX, maxX, minY, maxY);
    }

    public ObjectFeatureSetSmithWatermanObsolete (ObjectFeatureSetStringWindow superSet) {
        super (superSet);
    }
    
    public float getDistanceInOneAxis (ObjectFeatureSet set2, Coordinate coord) {
        return getDistanceInOneAxis(set2, coord, 0, Float.MAX_VALUE, 0, Float.MAX_VALUE);
    }

    public float getDistanceInOneAxis (float[][] distances) {
        int n; // length of this "string"
        int m; // length of o."string"
        int i; // iterates through this "string"
        int j; // iterates through o."string"

        if (distances.length == 0)
            return 0;

        n = distances.length;
        m = distances[0].length;

        /*if (n == 0)
            return 0;*/
        if (m == 0)
            return 0;
        float max = 0.0f;
        int maxi = 0, maxj = 0;

        float H[][] = new float[m][n];
        float E[][] = new float[m][n];
        float F[][] = new float[m][n];

        // zero the first line and first column
        for (i = 0; i < m; i++) {
            H[i][0] = 0.0f;
            E[i][0] = 0.0f;
        }

        for (j = 0; j < n; j++) {
            H[0][j] = 0.0f;
            F[0][j] = 0.0f;
        }

        for (i = 1; i < m; i++) {
            for (j = 1; j < n; j++) {
                E[i][j] = Math.max (E[i][j-1] - gapCostCont, H[i][j-1] - gapCostOpening);
                F[i][j] = Math.max (F[i-1][j] - gapCostCont, H[i-1][j] - gapCostOpening);
                float c = getCost(distances[j][i]);
                H[i][j] = max4 (
                        0,
                        E[i][j],
                        F[i][j],
                        H[i-1][j-1] + c
                        );

                if (H[i][j] > max) {
                    // i and j holds now the end of the sequence
                    maxi = i;
                    maxj = j;
                    max = H[i][j];
                }
            }
        }
        return max;
    }
    
    // assuming that both set1 and set2 are sorted with respect to the "coordinate"
    public float getDistanceInOneAxis (
            ObjectFeatureSet set2,
            Coordinate coord, 
            float MinCoordSet1,
            float MaxCoordSet1,
            float MinCoordSet2,
            float MaxCoordSet2) {
        int n; // length of this "string"
        int m; // length of o."string"
        int i; // iterates through this "string"
        int j; // iterates through o."string"

        n = getObjectCount();
        m = set2.getObjectCount();

        if (n == 0)
            return 0;
        if (m == 0)
            return 0;

        int startCoordSet1 = 0; int stopCoordSet1 = 0;
        int startCoordSet2 = 0; int stopCoordSet2 = 0;

        // count the number of features within the allowed range
        while ( ((coord == Coordinate.CoordX) ?
                ((ObjectFeature) getObject(startCoordSet1)).getX() :
                ((ObjectFeature) getObject(startCoordSet1)).getY()) <= MinCoordSet1 && startCoordSet1 < n) {
            startCoordSet1++;
        };
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
        };
        stopCoordSet1 = startCoordSet1;
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
                distances[i1][i2] = getObject(i1 + startCoordSet1).getDistance(set2.getObject(i2 + startCoordSet2));
            }
        }

        return getDistanceInOneAxis(distances);
    }

    public String DumpMatrixes (String osa, int m, int n, float [][]H, float[][] E, float [][]F, char[][] Smery, ObjectFeatureSet set1, ObjectFeatureSet set2) {
        try {
            java.util.Locale loc = new java.util.Locale("cs");
            FileWriter wr = new FileWriter ("MaticeH_" + osa + ".txt");
            for (int hj = 0; hj < n; hj++) {
                wr.write (String.format("\t%d", hj));
            }
            wr.write ("\n");
            for (int hi = 0; hi < m; hi++) {
                wr.write (String.format("%d", hi));
                for (int hj = 0; hj < n; hj++) {
                    wr.write(String.format(loc, "\t%f", H[hi][hj]));
                }
                wr.write ("\n");
            }
            wr.close();

            wr = new FileWriter ("MaticeE_" + osa + ".txt");
            for (int hj = 0; hj < n; hj++) {
                wr.write (String.format("\t%d", hj));
            }
            wr.write ("\n");
            for (int hi = 0; hi < m; hi++) {
                wr.write (String.format("%d", hi));
                for (int hj = 0; hj < n; hj++) {
                    wr.write(String.format(loc, "\t%03f", E[hi][hj]));
                }
                wr.write ("\n");
            }
            wr.close();

            wr = new FileWriter ("MaticeF_" + osa + ".txt");
            for (int hj = 0; hj < n; hj++) {
                wr.write (String.format("\t%d", hj));
            }
            wr.write ("\n");
            for (int hi = 0; hi < m; hi++) {
                wr.write (String.format("%d", hi));
                for (int hj = 0; hj < n; hj++) {
                    wr.write(String.format(loc, "\t%f", F[hi][hj]));
                }
                wr.write ("\n");
            }
            wr.close();

            wr = new FileWriter ("MaticeDist_" + osa + ".txt");
            for (int hj = 0; hj < n; hj++) {
                wr.write (String.format("\t%d", hj));
            }
            wr.write ("\n");

            for (int hi = 0; hi < m; hi++) {
                wr.write (String.format("%d", hi));
                for (int hj = 0; hj < n; hj++) {
                    float dist = set1.getObject(hi).getDistance(set2.getObject(hj));
                    if (dist <= equalityUpperTreshold)
                        wr.write(String.format(loc, "\t%f", dist));
                    else
                        wr.write("\t");
                }
                wr.write ("\n");
            }
            wr.close();

            wr = new FileWriter ("MaticeSmery_" + osa + ".txt");
            for (int hj = 0; hj < n; hj++) {
                wr.write (String.format("\t%d", hj));
            }
            wr.write ("\n");
            for (int hi = 0; hi < m; hi++) {
                wr.write (String.format("%d", hi));
                for (int hj = 0; hj < n; hj++) {
                    wr.write("\t" + Smery[hi][hj]);
                }
                wr.write ("\n");
            }
            wr.close();
        } catch (Exception ex) {}
        return "";
    }

    @Override
    public float getDistanceImpl (LocalAbstractObject o, float distTreshold) {
        ObjectFeatureSet obj = (ObjectFeatureSet) o;
        
        Collections.sort(objects, new ObjectLocalFeatureComparatorX());
        Collections.sort(obj.objects, new ObjectLocalFeatureComparatorX());
        float rx = getDistanceInOneAxis (obj, Coordinate.CoordX);

        Collections.sort(objects, new ObjectLocalFeatureComparatorY());
        Collections.sort(obj.objects, new ObjectLocalFeatureComparatorY());
        float ry = getDistanceInOneAxis (obj, Coordinate.CoordY);

        return rx + ry; // (rx + ry) / 2.0f;
    }
/*
    public float getDistanceWindowOneAxis (LocalAbstractObject o, float distThreshold,
            boolean winsizeisinpixels,
            boolean floatingwindow,
            int windowsize,
            int windowshift,
            int minnumoffeaturesinsegment,
            boolean XAxe,
            int imgsize1,
            int imgsize2)
    {
        float retval = 0.0f;
        ObjectFeatureSet obj = (ObjectFeatureSet) o;
        if (XAxe)
        {
            Collections.sort(objects, new ObjectLocalFeatureComparatorX());
            Collections.sort(obj.objects, new ObjectLocalFeatureComparatorX());
        }
        else
        {
            Collections.sort(objects, new ObjectLocalFeatureComparatorY());
            Collections.sort(obj.objects, new ObjectLocalFeatureComparatorY());
        }

        if (winsizeisinpixels)
        {
            boolean isnextquerywindow = (objects.size() == 0) ? false : true;
            //float actwindowsize = windowsize;
            float indexfromquery = 0.0f;

            
            while (isnextquerywindow) {
                 float indexfromdb = 0.0f;
                 boolean isnextdbwindow = (obj.objects.size() == 0) ? false : true;

                 // v Q objektu je mozne zpracovat cele okno?
                 if (indexfromquery + (windowsize - windowshift) + windowsize <= imgsize1) {

                     while (isnextdbwindow) {
                         if (indexfromdb + (windowsize - windowshift) + windowsize <= imgsize2) {
                             float r = getDistanceInOneAxis(this, obj,
                                            (XAxe) ? Coordinate.CoordX : Coordinate.CoordY ,
                                            indexfromquery, indexfromquery + windowsize,
                                            indexfromdb, indexfromdb + windowsize,
                                            true);
                             retval = Math.max(r, retval);
                             indexfromdb += windowshift;
                         } else { // jsme na konci db objektu
                             indexfromdb = Math.max(0, imgsize2 - windowsize);
                             float r = getDistanceInOneAxis(this, obj,
                                            (XAxe) ? Coordinate.CoordX : Coordinate.CoordY,
                                            indexfromquery, indexfromquery + windowsize,
                                            indexfromdb, indexfromdb + windowsize,
                                            true);
                             retval = Math.max(r, retval);
                             isnextdbwindow = false;
                         }
                     }
                     indexfromquery += windowshift;

                 } else { // jsme na konci query objektu, posledni cele okno

                     indexfromquery = Math.max(0, imgsize1 - windowsize);

                     while (isnextdbwindow) {
                         if (indexfromdb + (windowsize - windowshift) + windowsize <= imgsize2) {
                             float r = getDistanceInOneAxis(this, obj,
                                                (XAxe) ? Coordinate.CoordX : Coordinate.CoordY ,
                                                indexfromquery, indexfromquery + windowsize,
                                                indexfromdb, indexfromdb + windowsize,
                                                true);
                             retval = Math.max(r, retval);
                             indexfromdb += windowshift;
                         } else { // jsme na konci db objektu
                             indexfromdb = Math.max(0, imgsize2 - windowsize);
                             float r = getDistanceInOneAxis(this, obj,
                                            (XAxe) ? Coordinate.CoordX : Coordinate.CoordY,
                                            indexfromquery, indexfromquery + windowsize,
                                            indexfromdb, indexfromdb + windowsize,
                                            true);
                             retval = Math.max(r, retval);
                             isnextdbwindow = false;
                         }
                     }
                     isnextquerywindow = false;
                 }
            }
            return retval;
        }
        return retval;
    }
*/
}

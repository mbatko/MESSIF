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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import messif.objects.LocalAbstractObject;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinarySerializator;

public class ObjectFeatureSetOrdpres extends ObjectFeatureSet {

    /** Class id for serialization. */
    private static final long serialVersionUID = 669L;

    /**
     * Minimal number of anchors per one object
     */
    private int limit1 = 3;
    /**
     * Minimal number of "well-ordered" objects on the output
     */
    private int limit2 = 3;
    /**
     * Epsilon paramater (the maximal distance of features which can be marked as equal)
     */
    private float queryFeatureSearchRadius = 150;
    /**
     * Number of query features requested from an m-tree
     */
    private int queryFeaturesCount = 18;
    
    /**
     * Creates a new instance of ObjectFeatureSetOrdpres with empty list of objects.
     */
    public ObjectFeatureSetOrdpres () {
        
    }
    
    /**
     * Creates a new instance of ObjectFeatureSetOrdpres from a text stream.
     * @param stream the text stream to read an object from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
     public ObjectFeatureSetOrdpres(BufferedReader stream) throws IOException {
         super(stream);
     }
    
    /**
     * Creates a new instance of ObjectFeatureSetOrdpress from a text stream
     * overriding default parameters
     * @param stream the text stream to read an object from
     * @param epsilon  queryFeatureSearchRadius parameter
     * @param limit1   limit1 parameter
     * @param limit2   limit2 parameters
     * @param nof  queryFeaturesCount parameter
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public ObjectFeatureSetOrdpres(BufferedReader stream, float epsilon, int limit1, int limit2, int nof) throws IOException {
		super(stream);
        this.queryFeatureSearchRadius = epsilon;
        this.limit1= limit1;
        this.limit2 = limit2;
        this.queryFeaturesCount = nof;
    }

     /**
      * Creates a new instance of ObjectFeatureSetOrdpress from a file
      * @param file text file to read an object from
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
      */
    public ObjectFeatureSetOrdpres(File file) throws IOException {
        this(new BufferedReader(new InputStreamReader(new FileInputStream(file))));
    }

    /**
      * Creates a new instance of ObjectFeatureSetOrdpress from a file with
     * overriding default parameters
      * @param file text file to read an object from
     * @param epsilon  queryFeatureSearchRadius parameter
     * @param limit1   limit1 parameter
     * @param limit2   limit2 parameters
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public ObjectFeatureSetOrdpres(File file, float epsilon, int limit1, int limit2) throws IOException {
        this (file);
        this.queryFeatureSearchRadius = epsilon;
        this.limit1= limit1;
        this.limit2 = limit2;
    }

    /**
      * Creates a new instance of ObjectFeatureSetOrdpress from a file with
     * overriding default parameters
      * @param file text file to read an object from
     * @param epsilon  queryFeatureSearchRadius parameter
     * @param limit1   limit1 parameter
     * @param limit2   limit2 parameters
     * @param nof  queryFeaturesCount parameter
     * @throws IOException when an error appears during reading from given stream,
     *         EOFException is returned if end of the given stream is reached.
     */
    public ObjectFeatureSetOrdpres(File file, float epsilon, int limit1, int limit2, int nof) throws IOException {
        this (file);
        this.queryFeatureSearchRadius = epsilon;
        this.limit1= limit1;
        this.limit2 = limit2;
        this.queryFeaturesCount = nof;
    }


     public ObjectFeatureSetOrdpres(BinaryInput input, BinarySerializator serializator) throws IOException {
         super (input, serializator);
     }

     /********************  getters *********************/
     /**
      * Returns limit1 paramter
      * @return limit1 paramter
      */
     public int getLimit1() {
        return limit1;
    }

     /**
      * Returns limit2 paramter
      * @return limit2 paramter
      */
    public int getLimit2() {
        return limit2;
    }

     /**
      * Returns limit2 queryFeatureSearchRadius
      * @return limit2 queryFeatureSearchRadius
      */
    public float getQueryFeatureSearchRadius() {
        return queryFeatureSearchRadius;
    }


     /**
      * Returns limit2 getQueryFeaturesCount
      * @return limit2 getQueryFeaturesCount
      */
    public int getQueryFeaturesCount() {
        return queryFeaturesCount;
    }
    //****************** Distance function ******************//

     /**
      * Returns bounding rect of spatial feature information
      * @return Array of floats, array index 0 = minimal X, 1 = maximal X, 2 = minimal Y, 3 = maximal Y
      */
     public ObjectRectangle2D getMaxMinCoords () {
         float minx = Float.MAX_VALUE, miny = Float.MAX_VALUE, maxx = Float.MIN_VALUE, maxy = Float.MIN_VALUE;
         for (LocalAbstractObject f : objects) {
             ObjectFeature of = (ObjectFeature) f;
             float x = of.getX();
             if (x > maxx) maxx = x;
             if (x < minx) minx = x;
             float y = of.getY();
             if (y > maxy) maxy = y;
             if (y < miny) miny = y;
         }
         return new ObjectRectangle2D(minx, miny, maxx, maxy);
     }

    /**
     * The actual implementation of the distance function.
     *
     * @param obj the object to compute distance to
     * @param distThreshold the threshold value on the distance
     * @return the actual distance between obj and this if the distance is lower than distThreshold
     * @see LocalAbstractObject#getDistance
     */
    @Override
    @SuppressWarnings("unchecked")
    protected float getDistanceImpl(LocalAbstractObject obj, float distThreshold) {
        Logger log = null;

        // try {
        // FileWriter fstream = new FileWriter("mereni.txt",true);
        // BufferedWriter merenicasu = new BufferedWriter(fstream);
        float retval = 0;
        boolean Loguj = false;
        String timeLogString = "";

        if (Loguj) {
            log = Logger.getLogger("subimage-algorithm");
            timeLogString = "start:\t" + System.nanoTime();
        }

        ObjectRectangle2D rect = null;
        HashMap<String,Object> params = null;
        if (suppData != null) {
            params = (HashMap<String, Object>)suppData;
            if (params.containsKey("epsilon"))
                queryFeatureSearchRadius = (Float) params.get("epsilon");
            if (params.containsKey("limit1"))
                limit1 = (Integer) params.get ("limit1");
            if (params.containsKey("limit1"))
                limit2 = (Integer) params.get("limit2");
            if (params.containsKey("rect")) {
                rect = (ObjectRectangle2D) params.get("rect");
            }
        }
        ObjectFeatureSet objSet = (ObjectFeatureSet) obj;
        int objectCount = getObjectCount();
        int objSetObjectCount = objSet.getObjectCount();
        /* mapi = mapping of the closest pairs of features */
        int [] mapi = new int [objectCount];
        /* remembered evaluated distances of closest features */
        float [] dists = new float [objectCount];
        if (Loguj)
            timeLogString += "\tparams done:\t" + System.nanoTime();

        for (int i = 0; i < mapi.length; i++) {
            mapi[i] = -1;
            dists[i] = Float.MAX_VALUE;
        }

        float maxdist, scalemine, scaletheir, theirminx, theirmaxx;
        float theirminy, theirmaxy;
        float theirminxnew, theirmaxxnew, theirminynew, theirmaxynew, theirx, theiry;
        int countinrect = 0;
        
        // For each "my" feature, find the closest one in "their" set
        for (int i = objectCount - 1; i >= 0; i--) {
            maxdist = queryFeatureSearchRadius;
            ObjectFeature objmine = (ObjectFeature) getObject(i);
            scalemine = objmine.getScale();
            
            for (int j = objSetObjectCount - 1; j >= 0; j--) {
                ObjectFeature o = (ObjectFeature) objSet.getObject(j);
                // "their" object have to be in bounding rect
                scaletheir = o.getScale();
                if (scalemine < scaletheir - scaletheir / 10 || scalemine > scaletheir + scaletheir / 10)
                    continue;
                theirminxnew = 0;
                theirmaxxnew = 1;
                theirminynew = 0;
                theirmaxynew = 1;
                /*theirminx = rect.getMinX(); theirmaxx = rect.getMaxX();
                theirminy = rect.getMinY(); theirmaxy = rect.getMaxY();
                // rozsirime vyhledavani o 10%
                theirminxnew = Math.max (0, theirminx - (theirmaxx - theirminx) / 10);
                theirmaxxnew = Math.min (1, theirmaxx + (theirmaxx - theirminx) / 10);
                theirminynew = Math.max (0, theirminy - (theirmaxy - theirminy) / 10);
                theirmaxynew = Math.min (1, theirmaxy + (theirmaxy - theirminy) / 10);*/
                theirx = o.getX();
                theiry = o.getY();

                if (theirx >= theirminxnew && theirx <= theirmaxxnew && theiry  >= theirminynew && theiry <= theirmaxynew) {
                    float d = objmine.getDistance(o, maxdist);
                    if (d < maxdist) {
                        maxdist = d;
                        mapi[i] = j;
                        dists[i] = d;
                    }
                    countinrect++;
                }
            }
            // we have found any distance <= epsilon, is it really the closest
            // to i^th "my" element?
            if (mapi[i] != -1) {
                for (int k = 0; k < mapi.length; k++) {
                    if (k != i && mapi[k] == mapi[i]) {
                        // recent distance is smaller or equal
                        if (dists[i] <= dists[k]) {
                            mapi[k] = -1;
                            dists[k] = Float.MAX_VALUE;
                        } else {
                            // previous distance is bigger
                            mapi[i] = -1;
                            dists[k] = Float.MAX_VALUE;
                        }                        
                        break;
                    }
                }
            }
        }

        int countok = 0;
        for (int i = 0; i < mapi.length; i++) {
            if (mapi[i] != -1) countok++;
        }

        if (Loguj)
            timeLogString += "\tmapi done\t" + System.nanoTime();

        if (countok <= limit2) {
            suppData = null;
            if (Loguj)
                log.log(Level.INFO, timeLogString + "\tcountok < limit2");
            // merenicasu.write(timeLogString + "\tcountinrect = " + countinrect + ", countok = " + countok + ", limit2 = " + limit2 + "\n");
            // merenicasu.close();
            return Float.MAX_VALUE;
        }
        // min X, max X, min Y, max Y of "their" object

        float minx = Float.MAX_VALUE;
        float miny = Float.MAX_VALUE;
        float maxx = Float.MIN_VALUE;
        float maxy = Float.MIN_VALUE;

        // Sort the found features
        LinkedHashMap<Integer, Float> myindexypodlex = new LinkedHashMap<Integer, Float>();
        for (int i = objectCount-1; i >= 0; i--) {
            if (mapi[i] != -1)
               myindexypodlex.put(i, ((ObjectFeatureByteL2) this.getObject(i)).getX());
        }
        myindexypodlex = sortHashMapByValues (myindexypodlex, true);

        if (Loguj)
            timeLogString += "\tmyindexypodlexTime:\t" + System.nanoTime();

        java.util.LinkedHashMap<Integer, Float> theirindexypodlex = new java.util.LinkedHashMap<Integer, Float> ();
        for (int i = objSetObjectCount-1; i >= 0; i--) {
            for (int j = 0; j < mapi.length; j++)
                if (mapi[j] == i) {
                    float x = ((ObjectFeature) objSet.getObject(i)).getX();
                    theirindexypodlex.put(i, x);
                    // it's faster to do it here instead of retype the hash to the array below
                    if (x < minx) minx = x;
                    if (x > maxx) maxx = x;
                }
        }
        theirindexypodlex = sortHashMapByValues (theirindexypodlex, true);

        if (Loguj)
            timeLogString += "\ttheirindexypodlexTime:\t" + System.nanoTime();

        Integer [] theirpole = new Integer[theirindexypodlex.keySet().size()];
        theirpole = theirindexypodlex.keySet().toArray(theirpole);
        Integer [] mypole = new Integer[myindexypodlex.keySet().size()];
        mypole = myindexypodlex.keySet().toArray(mypole);

        if (Loguj)
            timeLogString += "\tbeforeSpearmanxTime:\t" + System.nanoTime();

        retval = ordSpearman(mypole, theirpole, mapi);

        if (Loguj)
            timeLogString += "\tafterSpearmanxTime:\t" + System.nanoTime();

        java.util.LinkedHashMap<Integer, Float> myindexypodley = new java.util.LinkedHashMap<Integer, Float>();
        for (int i = objectCount-1; i >= 0; i--) {
            if (mapi[i] != -1)
               myindexypodley.put(i, ((ObjectFeatureByteL2) this.getObject(i)).getY());
        }
        myindexypodley = sortHashMapByValues (myindexypodley, true);

        if (Loguj)
            timeLogString += "\tmyindexypodleyTime:\t" + System.nanoTime();

        java.util.LinkedHashMap<Integer, Float> theirindexypodley = new java.util.LinkedHashMap<Integer, Float> ();
        for (int i = objSetObjectCount-1; i >= 0; i--) {
            for (int j = 0; j < mapi.length; j++)
                if (mapi[j] == i) {
                    float y = ((ObjectFeature) objSet.getObject(i)).getY();
                    theirindexypodley.put(i, y);
                    if (y < miny) miny = y;
                    if (y > maxy) maxy = y;
                }
        }
        theirindexypodley = sortHashMapByValues (theirindexypodley, true);

        if (Loguj)
            timeLogString += "\ttheirindexypodleyTime:\t" + System.nanoTime();

        theirpole = new Integer[theirindexypodley.keySet().size()];
        theirpole = theirindexypodley.keySet().toArray(theirpole);
        mypole = new Integer[myindexypodley.keySet().size()];
        mypole = myindexypodley.keySet().toArray(mypole);

        if (Loguj)
            timeLogString += "\tbeforeSpearmanYTime:\t" + System.nanoTime();

        retval += ordSpearman(mypole, theirpole, mapi);

        if (Loguj) {
            timeLogString += "\tafterSpearmanYTime\t" + System.nanoTime();

            timeLogString += "\tObjCountMine:\t" + objectCount + "\tCountInRect:\t" + countinrect + "\tObjCountTheir:\t"
                + objSetObjectCount + "\tMapiLen:\t" + mapi.length + "\tmypolelenx:\t"
                + myindexypodlex.size() + "\tmypoleleny:\t" + myindexypodley.size ();
        }
        retval /= mypole.length;
        obj.suppData = new ObjectRectangle2D(minx, miny, maxx, maxy);
        if (Loguj)
            log.log(Level.INFO, timeLogString);

        // merenicasu.write(timeLogString + "\n");
        // merenicasu.close();

        return retval;
        //}
        // catch (IOException e) {
        //    return Float.MAX_VALUE;
    }

    public int ordSpearman (Integer []minearray, Integer [] theirarray, int [] mapping) {
        if (minearray.length != theirarray.length)
            return Integer.MAX_VALUE;
        int sum = 0;
        for (int i = 0; i < minearray.length; i++) {
            // i - pozice v prvni permutaci
            // musime najit odpovidajici pozici ve druhe permutaci
            int j = 0;
            while (j < theirarray.length && theirarray[j] != mapping[minearray[i]]) j++;
            sum += Math.abs(i - j);
        }
        return sum;
    }

    @SuppressWarnings("unchecked")
    public LinkedHashMap sortHashMapByValues(LinkedHashMap<Integer, Float> passedMap, boolean ascending) {
        List<Integer> mapKeys = new ArrayList<Integer>(passedMap.keySet());
        List<Float> mapValues = new ArrayList<Float>(passedMap.values());
        Collections.sort(mapValues);
        Collections.sort(mapKeys);

        if (!ascending)
        Collections.reverse(mapValues);

        LinkedHashMap<Integer, Float> someMap = new LinkedHashMap<Integer, Float>();
        Iterator<Float> valueIt = mapValues.iterator();
        while (valueIt.hasNext()) {
            Float val = valueIt.next();
            Iterator<Integer> keyIt = mapKeys.iterator();
            while (keyIt.hasNext()) {
                Integer key = keyIt.next();
                if (passedMap.get(key).toString().equals(val.toString())) {
                    passedMap.remove(key);
                    mapKeys.remove(key);
                    someMap.put(key, val);
                    break;
                }
            }
        }
        return someMap;
    }

}

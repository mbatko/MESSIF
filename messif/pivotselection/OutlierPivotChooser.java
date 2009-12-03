/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package messif.pivotselection;

import java.io.Serializable;
import java.util.Arrays;
import messif.buckets.BucketFilterAfterAdd;
import messif.buckets.BucketFilterAfterRemove;
import messif.buckets.LocalBucket;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.AbstractObjectList;

/**
 * Selects pivots as outliers from the sample set.
 *
 * The procedure is as follows:
 * - the first pivot is the object farthest from the other objects.
 * - the second pivots is the farthest object from the first pivot.
 * - the third pivots is the object farthest from the previous two pivots (having sum of distances to previous pivots maximal).
 * - etc...
 *
 * Based on:
 * L. Mico, J. Oncina, and E. Vidal.
 *     A new version of the nearest-neighbor approximating and eliminating search (AESA)
 *     with linear preprocessing-time and memory requirements.
 *     Pattern Recognition Letters, 15:9{17, 1994.
 *
 * @author Vlastislav Dohnal (xdohnal), dohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class OutlierPivotChooser extends AbstractPivotChooser implements Serializable, BucketFilterAfterAdd, BucketFilterAfterRemove {
    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;

//    /** Size of the candidate set of pivots from which the best pivot is picked. */
//    public static int SAMPLE_PIVOT_SIZE = 100;

//    /** List of initial pivots */
//    protected AbstractObjectList<LocalAbstractObject> initialPivots = null;


    // *************** CONSTRUCTORS **********************

    /**
     * Creates a new instance of OutlierPivotChooser.
     */
    public OutlierPivotChooser() {
    }

//    /**
//     * Creates a new instance of OutlierPivotChooser.
//     * @param initialPivots the list of initial (already selected) pivots
//     */
//    public OutlierPivotChooser(AbstractObjectList<LocalAbstractObject> initialPivots) {
//        this.initialPivots = initialPivots;
//    }

    // *************** PIVOT SELECTION IMPLEMENTATION **********************

    @Override
    protected void selectPivot(int count, AbstractObjectIterator<? extends LocalAbstractObject> sampleSetIterator) {
        // Store all passed objects temporarily
        AbstractObjectList<LocalAbstractObject> objectList = new AbstractObjectList<LocalAbstractObject>(sampleSetIterator);

//        List<LocalAbstractObject> pivots = new ArrayList<LocalAbstractObject>(count);
        // Sum of distances to previously selected pivots
        float pivotDists[] = new float[objectList.size()-1];
        Arrays.fill(pivotDists, 0f);         // Set accumulated distances to zero

//        // initially select "count" pivots at random - or use (partly) preselected pivots
//        if (initialPivots != null) {
//            // Initialize the accumulator of distances
//            for (LocalAbstractObject p : initialPivots) {
//
//                if (count > pivots.size()) {
//                    pivots.add(preselPivot);
//                    System.out.println("adding preselected pivot: "+preselPivot.getLocatorURI());
//                }
//            }
//        }

        selectFirstPivot(objectList);
        for (int p = 1; p < count; p++) {
            // Selects a new pivot and updates the sums of distances to previous pivots
            selectNextPivot(objectList, pivotDists);
        }
    }

    private void selectFirstPivot(AbstractObjectList<LocalAbstractObject> objectList) {
        // First pivot
        LocalAbstractObject pivot = null;
        float pivotDist = -1f;
        int pivotIndex = -1;

        // Take one object at random
        LocalAbstractObject rand = objectList.randomObject();

        // The first pivot is the farthest object from rand
        int i = 0;
        for (LocalAbstractObject obj : objectList) {
            float d = rand.getDistance(obj);
            if (d > pivotDist) {
                pivot = obj;
                pivotDist = d;
                pivotIndex = i;
            }
            i++;
        }
        preselectedPivots.add(pivot);
        // Remove the pivot from the list of objects
        objectList.remove(pivotIndex);
    }


    private void selectNextPivot(AbstractObjectList<LocalAbstractObject> objectList, float[] pivotDists) {
        // Get the last pivot
        LocalAbstractObject lastPivot = preselectedPivots.get(preselectedPivots.size() - 1);

        // New pivot
        LocalAbstractObject pivot = null;
        float dist = -1f;
        int pivotIndex = -1;

        // Compute distances to all objects and accumulate them in the array of distances
        int i = 0;
        for (LocalAbstractObject obj : objectList) {
            pivotDists[i] += lastPivot.getDistance(obj);
            if (pivotDists[i] > dist) {
                pivot = obj;
                dist = pivotDists[i];
                pivotIndex = i;
            }
            i++;
        }

        preselectedPivots.add(pivot);
        // Remove the pivot from the list of objects
        objectList.remove(pivotIndex);
        System.arraycopy(pivotDists, pivotIndex+1, pivotDists, pivotIndex, pivotDists.length-pivotIndex-1);
    }

    // *************** SUPPORT FOR ON-FLY PIVOT SELECTION **********************

    @Override
    public void filterAfterAdd(LocalAbstractObject object, LocalBucket bucket) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void filterAfterRemove(LocalAbstractObject object, LocalBucket bucket) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}

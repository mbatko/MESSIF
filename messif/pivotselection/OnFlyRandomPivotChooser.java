/*
 * OnFlyRandomPivotChooser.java
 *
 * Created on 26. kveten 2005, 16:23
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package messif.pivotselection;

import java.io.Serializable;
import messif.buckets.BucketFilterInterface;
import messif.buckets.BucketFilterInterface.FilterSituations;
import messif.buckets.FilterRejectException;
import messif.buckets.LocalFilteredBucket;
import messif.objects.LocalAbstractObject;

/**
 * OnFlyRandomPivotChooser provides the capability of selecting a random object from the whole bucket.
 * It selects the first pivot on the fly from the objects being inserted into the bucket.
 * Additional pivots are pick among all objects stored in the bucket.
 *
 * Important notice: a new-coming object causes all selected pivots to be deleted! That implies, only one object is selected by this chooser.
 *
 * @author Vlastislav Dohnal, xdohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class OnFlyRandomPivotChooser extends RandomPivotChooser implements Serializable, BucketFilterInterface {
    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;

    /** Creates a new instance of OnFlyRandomPivotChooser */
    public OnFlyRandomPivotChooser() {
    }
    
    /** filterObject()
     * Filter method used to pick one pivot at random.
     *
     * This method selects one pivot at random using an incremental schema.
     * Whenever a new object arrives we compute a random number within the interval <0,N+1)
     * (where N is the number of object stored in this bucket). If the random number is 
     * equal to N we choose the new object as a pivot.
     * These technique is correct and the probability of selecting each object of the bucket
     * is the same and equal to 1/N. (This can be proved using mathematical induction applied to N).
     *
     * Notice that this filter erases all previously selested pivots (e.g. by getPivot(int)).
     */
    public void filterObject(LocalAbstractObject object, FilterSituations situation, LocalFilteredBucket inBucket) throws FilterRejectException {
        // Ignore other situations than after add
        if (situation != FilterSituations.AFTER_ADD)
            return;

        int idx = (int)(Math.random() * (float)(inBucket.getObjectCount() + 1));
        
        if (idx == inBucket.getObjectCount()) {
            // the new incoming object is selected as pivot
            preselectedPivots.clear();
            preselectedPivots.add(object);
        }
        // otherwise, leave the previous pivot unchanged
    }

}

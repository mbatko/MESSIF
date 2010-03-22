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
package messif.pivotselection;

import java.io.Serializable;
import messif.buckets.BucketFilterAfterAdd;
import messif.buckets.LocalBucket;
import messif.objects.LocalAbstractObject;

/**
 * OnFlyRandomPivotChooser provides the capability of selecting a random object from the whole bucket.
 * It selects the first pivot on the fly from the objects being inserted into the bucket.
 * Additional pivots are pick among all objects stored in the bucket.
 *
 * Important notice: a new-coming object causes all selected pivots to be deleted! That implies, only one object is selected by this chooser.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class OnFlyRandomPivotChooser extends RandomPivotChooser implements Serializable, BucketFilterAfterAdd {
    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;

    /** Creates a new instance of OnFlyRandomPivotChooser */
    public OnFlyRandomPivotChooser() {
    }
    
    /** filterObject()
     * Filter method used to pick one pivot at random.
     *
     * This method selects one pivot at random using an incremental schema.
     * Whenever a new object arrives we compute a random number within the interval &lt;0,N+1)
     * (where N is the number of object stored in this bucket). If the random number is 
     * equal to N we choose the new object as a pivot.
     * These technique is correct and the probability of selecting each object of the bucket
     * is the same and equal to 1/N. (This can be proved using mathematical induction applied to N).
     *
     * Notice that this filter erases all previously selested pivots (e.g. by getPivot(int)).
     */
    public void filterAfterAdd(LocalAbstractObject object, LocalBucket bucket) {
        int idx = (int)(Math.random() * (float)(bucket.getObjectCount() + 1));
        
        if (idx == bucket.getObjectCount()) {
            // the new incoming object is selected as pivot
            preselectedPivots.clear();
            preselectedPivots.add(object);
        }
        // otherwise, leave the previous pivot unchanged
    }

}

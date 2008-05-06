/*
 * TwoDistantIncrementalPivotChooser.java
 *
 * Created on 19. unor 2004, 1:44
 */

package messif.pivotselection;

import java.io.Serializable;
import messif.buckets.BucketFilterInterface;
import messif.buckets.BucketFilterInterface.FilterSituations;
import messif.buckets.FilterRejectException;
import messif.buckets.LocalFilteredBucket;
import messif.objects.AbstractObject;
import messif.objects.GenericAbstractObjectIterator;
import messif.objects.GenericObjectIterator;
import messif.objects.LocalAbstractObject;


/**
 *
 * @author  xbatko
 */
public class TwoDistantIncrementalPivotChooser extends AbstractPivotChooser implements Serializable, BucketFilterInterface {
    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;


    /****************** Selected pivots ******************/
    
    public LocalAbstractObject getLeftPivot() { return getPivot(0); }
    public LocalAbstractObject getRightPivot() { return getPivot(1); }

    private float pivotsDistance = 0;
    public float getPivotsDistance() { return pivotsDistance; }
    

    /****************** Construcotrs ******************/

    /** Creates a new instance of GHTPivotChooser */
    public TwoDistantIncrementalPivotChooser() {
    }
    
    
    /****************** Pivot choosing ******************/

    /** Preselect pivots */
    public void filterObject(LocalAbstractObject object, FilterSituations situation, LocalFilteredBucket inBucket) throws FilterRejectException {
        // Ignore other situations than after insert...
        if (situation == FilterSituations.AFTER_ADD)
            try {
                counterPivotDistComp.bindTo(counterObjectDistComp);
                selectPivot(1, new GenericAbstractObjectIterator<LocalAbstractObject>(object));
            } finally {
                counterPivotDistComp.unbind();
            }
    }
    
    
    /****************** Overrides ******************/
    
    protected void selectPivot(int count, GenericObjectIterator<? extends LocalAbstractObject> samplesList) {
        while (samplesList.hasNext() && count-- > 0) {
            LocalAbstractObject object = samplesList.next();
        
            synchronized (preselectedPivots) {
                switch (preselectedPivots.size()) {
                    // Get first two objects as pivots
                    case 1:
                        pivotsDistance = object.getDistance(getPivot(0));
                        preselectedPivots.add(object);
                        break;
                    case 0:
                        preselectedPivots.add(object);
                        break;
                    // Measure the next ones
                    default:
                        // Compute distance to the left and right pivots
                        float leftDistance = object.getDistance(getPivot(0));
                        float rightDistance = object.getDistance(getPivot(1));

                        if (leftDistance > rightDistance) {
                            if (leftDistance > pivotsDistance) {
                                preselectedPivots.set(1, object);
                                pivotsDistance = leftDistance;
                            }
                        } else {
                            if (rightDistance > pivotsDistance) {
                                preselectedPivots.set(0, object);
                                pivotsDistance = rightDistance;
                            }
                        }
                }
            }
        }
    }

    /** Clears the list of preselected pivots and reset the distance between them.
     */
    public void clear() {
        synchronized (preselectedPivots) {
            super.clear();
            pivotsDistance = 0;
        }
    }
    
}

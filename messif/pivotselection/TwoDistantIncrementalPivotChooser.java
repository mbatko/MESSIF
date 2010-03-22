/*
 * TwoDistantIncrementalPivotChooser.java
 *
 * Created on 19. unor 2004, 1:44
 */

package messif.pivotselection;

import java.io.Serializable;
import messif.buckets.BucketFilterAfterAdd;
import messif.buckets.FilterRejectException;
import messif.buckets.LocalBucket;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;


/**
 * This class provides a privot chooser that selects maximally two pivots.
 * The chooser is incrementally maintaining two objects that have a maximal distance.
 * 
 * @author  xbatko
 */
public class TwoDistantIncrementalPivotChooser extends AbstractPivotChooser implements Serializable, BucketFilterAfterAdd {
    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;

    /** Actual selected pivots' distance */
    private float pivotsDistance = 0;


    //****************** Selected pivots ******************//

    /**
     * Returns the left (first) pivot.
     * @return the left pivot
     */
    public LocalAbstractObject getLeftPivot() {
        return getPivot(0);
    }

    /**
     * Returns the right (second) pivot.
     * @return the right pivot
     */
    public LocalAbstractObject getRightPivot() {
        return getPivot(1);
    }

    /**
     * Returns the distance of the actually selected pivots or zero if there is not enough objects seen yet.
     * @return the distance of the actually selected pivots
     */
    public float getPivotsDistance() {
        return pivotsDistance;
    }


    //****************** Construcotrs ******************//

    /**
     * Creates a new instance of TwoDistantIncrementalPivotChooser.
     */
    public TwoDistantIncrementalPivotChooser() {
    }


    //****************** Pivot choosing ******************//

    /**
     * Method for preselecting pivots as they are added to a bucket.
     *
     * @param object the inserted object
     * @param bucket the bucket where the object was stored
     */
    public void filterAfterAdd(LocalAbstractObject object, LocalBucket bucket) {
        try {
            counterPivotDistComp.bindTo(counterObjectDistComp);
            updateSelectedPivots(object);
        } finally {
            counterPivotDistComp.unbind();
        }
    }


    //****************** Overrides ******************//
    
    /**
     * Select at least <i>count</i> pitvots and
     * add them by <code>addPivot</code> method.
     * @param count Number of pivots to generate
     * @param sampleSetIterator Iterator over the sample set of objects to choose new pivots from
     * @throws IllegalArgumentException if more than two pivots are requested
     */
    protected void selectPivot(int count, AbstractObjectIterator<? extends LocalAbstractObject> sampleSetIterator) throws IllegalArgumentException {
        if (count > 2)
            throw new IllegalArgumentException("Pivot chooser only supports two pivots");
        while (sampleSetIterator.hasNext() && count-- > 0)
            updateSelectedPivots(sampleSetIterator.next());
    }

    /**
     * Updates the selected pivots.
     * If the distance between the left or the right pivot is bigger than current pivots'
     * distance, the object replaces the other pivot.
     * @param object the object to check
     */
    protected void updateSelectedPivots(LocalAbstractObject object) {
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

    /**
     * Clears the list of preselected pivots and reset the distance between them.
     */
    @Override
    public void clear() {
        synchronized (preselectedPivots) {
            super.clear();
            pivotsDistance = 0;
        }
    }

}

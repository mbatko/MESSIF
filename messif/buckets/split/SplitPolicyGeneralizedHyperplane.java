/*
 * SplitPolicyGeneralizedHyperplane.java
 *
 * Created on 6. listopad 2007, 11:09
 *
 */

package messif.buckets.split;

import java.util.NoSuchElementException;
import messif.objects.BallRegion;
import messif.objects.LocalAbstractObject;

/**
 * This class defines a policy for bucket splitting based on the generalized hyperplane partitioning.
 * The objects are split according to two pivots - object closer to the left pivot stays while the one
 * closer to the right pivot moves.
 *
 * @author xbatko
 */
public class SplitPolicyGeneralizedHyperplane extends SplitPolicy {
    
    /****************** Attributes ******************/

    /** Policy parameter <i>left pivot</i> */
    @SplitPolicy.ParameterField("left pivot")
    protected LocalAbstractObject leftPivot = null;

    /** Policy parameter <i>right pivot</i> */
    @SplitPolicy.ParameterField("right pivot")
    protected LocalAbstractObject rightPivot = null;

    /** The distance between the pivots divided by two. If both the pivots are not set yet, {@link LocalAbstractObject#UNKNOWN_DISTANCE} is held. */
    protected float halfPivotDistance = LocalAbstractObject.UNKNOWN_DISTANCE;

    
    /** The distance to the left pivot computed by the last call to match(Region). */
    protected float leftDist = LocalAbstractObject.UNKNOWN_DISTANCE;

    /** The distance to the right pivot computed by the last call to match(Region). */
    protected float rightDist = LocalAbstractObject.UNKNOWN_DISTANCE;


    /****************** Constructor ******************/

    /** Creates a new instance of SplitPolicyBallPartitioning */
    public SplitPolicyGeneralizedHyperplane() {
    }


    /****************** Parameter quick setter/getters ******************/

    /**
     * Sets the first pivot for generalized hyperplane partitioning.
     * @param leftPivot the pivot
     */
    public void setLeftPivot(LocalAbstractObject leftPivot) {
        setParameter("left pivot", leftPivot);
    }

    /**
     * Returns the first pivot for generalized hyperplane partitioning.
     * @return the first pivot for generalized hyperplane partitioning
     */
    public LocalAbstractObject getLeftPivot() {
        return leftPivot;
    }

    /**
     * Sets the second pivot for generalized hyperplane partitioning.
     * @param rightPivot the pivot
     */
    public void setRightPivot(LocalAbstractObject rightPivot) {
        setParameter("right pivot", rightPivot);
    }

    /**
     * Returns the second pivot for generalized hyperplane partitioning.
     * @return the second pivot for generalized hyperplane partitioning
     */
    public LocalAbstractObject getRightPivot() {
        return rightPivot;
    }

    /**
     * Use this method to set the policy parameter.
     * @param parameter the name of the policy parameter
     * @param value new value for the parameter
     * @throws IllegalStateException if the specified parameter is locked
     * @throws NoSuchElementException if there is no parameter for the specified name
     * @throws NullPointerException if the specified value is <tt>null</tt>
     */
    @Override
    public void setParameter(String parameter, Object value) throws IllegalStateException, NoSuchElementException, NullPointerException {
        super.setParameter(parameter, value);
        if (leftPivot != null && rightPivot != null)
            halfPivotDistance = leftPivot.getDistance(rightPivot)/2.0f;
    }


    /**
     * Returns the distance to the left pivot which might have been computed
     * by the last call to match(Region). If it is equal to 
     * <code>LocalAbstractObject.UNKNOWN_DISTANCE</code>, the distance
     * was not evaluated.
     * @return the distance to the left pivot
     */
    public float getDistanceToLeftPivot() {
        return leftDist;
    }

    /**
     * Returns the distance to the right pivot which might have been computed
     * by the last call to match(Region). If it is equal to 
     * <code>LocalAbstractObject.UNKNOWN_DISTANCE</code>, the distance
     * was not evaluated.
     * @return the distance to the right pivot
     */
    public float getDistanceToRightPivot() {
        return rightDist;
    }

    
    /****************** Matching ******************/
    
    /**
     * Returns 0 for objects near the left pivot defined by this policy (or exactly in the middle) and 1 for objects near the right pivot.
     * @param object an object that is tested for partition
     *
     * @return 0 for objects near the left pivot defined by this policy (or exactly in the middle) and 1 for objects near the right pivot
     */
    public int match(LocalAbstractObject object) {
        // The GH-partitioning is defined that object (<=) fall in 0 partition and the others in 1 partition.
        // includeUsingPrecompDist used <= as well, so calling it against leftPivot and then against rightPivot is correct.
        if (object.includeUsingPrecompDist(leftPivot, halfPivotDistance))
            return 0;
        if (object.includeUsingPrecompDist(rightPivot, halfPivotDistance))
            return 1;

        // Definition of GH partitioning.
        if (leftPivot.getDistance(object) <= rightPivot.getDistance(object))
            return 0;
        else 
            return 1;
    }

    /**
     * Returns the number of partitions of this policy.
     * @return the number of partitions of this policy
     */
    public int getPartitionsCount() {
        return 2;
    }

    /**
     * Returns the group (partition) to which the whole ball region belongs.
     * Returns -1 if not all objects from the specified ball region fall into just one partition
     * or if this policy cannot decide. In that case, the ball region must be searched one object by one
     * using the {@link #match} method.
     *
     * @param region a ball region that is tested for the matching condition
     * @return the group (partition) to which the whole ball region belongs or -1 if it is uncertain
     */
    public int match(BallRegion region) {
        // Reset the distance to the left & right pivot
        leftDist = LocalAbstractObject.UNKNOWN_DISTANCE;
        rightDist = LocalAbstractObject.UNKNOWN_DISTANCE;
        
        // Try to make use of precomputed distances
        
        // The GH-partitioning is defined that object (<=) fall in 0 partition and the others in 1 partition.
        // includeUsingPrecompDist used <= as well, so calling it against leftPivot and then against rightPivot is correct.
        if (leftPivot.includeUsingPrecompDist(region.getPivot(), halfPivotDistance - region.getRadius()))
            // <=
            return 0;
        if (rightPivot.includeUsingPrecompDist(region.getPivot(), halfPivotDistance - region.getRadius()))
            // >
            return 1;
        
        
        // Compute the distance to the left pivot and decide on its basis
        leftDist = leftPivot.getDistance(region.getPivot());
        if (leftDist + region.getRadius() <= halfPivotDistance)        // The region 'region' is completely on the left
            // <=
            return 0;

        // Compute the distance to the right pivot and decide on its basis
        rightDist = rightPivot.getDistance(region.getPivot());
        if (rightDist + region.getRadius() < halfPivotDistance)        // The region 'region' is completely on the right
            // >
            return 1;
        
        // The region intersects both the partitions
        return -1;

        // Original implementation!            
//        float overlap = region.getOverlapWith(leftPivot, halfPivotDistance);
//        if (2.0F*region.getRadius() <= overlap)
//            return 0;
//        overlap = region.getOverlapWith(rightPivot, halfPivotDistance);
//        if (2.0F*region.getRadius() <= overlap)
//            return 1;
//        return -1;
    }
}

/*
 * SplitPolicyBallPartitioning.java
 *
 * Created on 6. listopad 2007, 11:09
 *
 */

package messif.buckets.split;

import messif.objects.BallRegion;
import messif.objects.LocalAbstractObject;
import messif.objects.util.ObjectMatcher;

/**
 * This class a ball-partitioning policy for bucket splitting.
 *
 * <p>
 * The policy is fully defined by:<br/>
 * {@link messif.objects.LocalAbstractObject LocalAbstractObject} <i>pivot</i><br/>
 * <code>float</code> <i>radius</i>
 * </p>
 *
 * <p>
 * All objects that have distances to the pivot smaller or equal to the radius form one partition,
 * the rest falls to the other one. The {@link messif.objects.util.ObjectMatcher matcher}
 * returns 1 for objects outside the ball and 0 for objects inside.
 * </p>
 *
 * @author xbatko
 */
public class SplitPolicyBallPartitioning extends SplitPolicy implements ObjectMatcher {

    //****************** Attributes ******************

    /** Policy parameter <i>pivot</i> */
    @SplitPolicy.ParameterField("pivot")
    protected LocalAbstractObject pivot = null;

    /** Policy parameter <i>radius</i> */
    @SplitPolicy.ParameterField("radius")
    protected float radius = LocalAbstractObject.UNKNOWN_DISTANCE;


    /** The distance to the pivot computed by the last call to match(Region). */
    protected float dist = LocalAbstractObject.UNKNOWN_DISTANCE;
    
    //****************** Constants ******************

    /**
     * Identification of the inner partition.
     * Equal to <code>0</code>
     */
    public final static int PART_ID_INNER = 0;

    /**
     * Identification of the outer partition.
     * Equal to <code>1</code>
     */
    public final static int PART_ID_OUTER = 1;

    //****************** Constructor ******************

    /** Creates a new instance of SplitPolicyBallPartitioning */
    public SplitPolicyBallPartitioning() {
    }


    //****************** Parameter quick setter/getters ******************

    /**
     * Sets the pivot for ball partitioning.
     * @param pivot the pivot
     */
    public void setPivot(LocalAbstractObject pivot) {
        setParameter("pivot", pivot);
    }

    /**
     * Returns the pivot for ball partitioning.
     * @return the pivot for ball partitioning
     */
    public LocalAbstractObject getPivot() {
        return pivot;
    }

    /**
     * Sets the radius for ball partitioning.
     * @param radius the radius
     */
    public void setRadius(float radius) {
        setParameter("radius", radius);
    }

    /**
     * Returns the radius for ball partitioning.
     * @return the radius for ball partitioning
     */
    public float getRadius() {
        return radius;
    }
    
    /**
     * Returns the distance to the pivot which might have been computed
     * by the last call to match(Region). If it is equal to 
     * <code>LocalAbstractObject.UNKNOWN_DISTANCE</code>, the distance
     * was not evaluated.
     * @return the distance to the pivot
     */
    public float getDistanceToPivot() {
        return dist;
    }

    //****************** Matching ******************
    
    /**
     * Returns 1 for objects outside the ball partition defined by this policy and 0 for objects belonging to the partition.
     * @param object an object that is tested for partition
     *
     * @return 1 for objects outside the ball partition defined by this policy and 0 for objects belonging to the partition
     */
    public int match(LocalAbstractObject object) {
        if (object.includeUsingPrecompDist(pivot, radius))
            return PART_ID_INNER;
        
        // Precomputed distances didn't help, so compute the exact distance
        if (object.getDistance(pivot) <= radius)
            return PART_ID_INNER;
        else 
            return PART_ID_OUTER;
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
        // Reset the distance to the pivot
        dist = LocalAbstractObject.UNKNOWN_DISTANCE;
        
        if (pivot.includeUsingPrecompDist(region.getPivot(), radius - region.getRadius()))
            // <=
            return PART_ID_INNER;
        if (pivot.excludeUsingPrecompDist(region.getPivot(), radius + region.getRadius()))
            // >
            return PART_ID_OUTER;
        
        // Compute the distance and decide on its basis
        dist = pivot.getDistance(region.getPivot());

        if (dist + region.getRadius() <= radius)        // The region 'region' is completely included
            // <=
            return PART_ID_INNER;
        else if (dist - region.getRadius() > radius)    // Regions do not intersect nor touch
            // >
            return PART_ID_OUTER;
        else
            // The region intersects both the partitions
            return PART_ID_ANY;

       // Original implementation!
//            float overlap = region.getOverlapWith(pivot, radius);
//            if (overlap < 0)           // Regions do not intersect nor touch
//                // >
//                return 1;
//            else if (2*region.getRadius() <= overlap)       // The region 'region' is completely included
//                // <=
//                return 0;
//            else
//                // The region intersects both the partitions
//                return -1;
    }

}

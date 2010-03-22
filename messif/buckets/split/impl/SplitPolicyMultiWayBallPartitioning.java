package messif.buckets.split.impl;

import messif.buckets.split.SplitPolicy;
import messif.objects.BallRegion;
import messif.objects.LocalAbstractObject;
import messif.objects.util.ObjectMatcher;

/**
 * This class is a multi-way ball-partitioning policy for bucket splitting.
 *
 * <p>
 * The policy is fully defined by:<br/>
 * {@link messif.objects.LocalAbstractObject LocalAbstractObject} <i>pivot</i><br/>
 * <code>float[]</code> <i>list of radii</i>
 * </p>
 *
 * The number of partitions produced by this policy is defined by the length of <code>radii</code> array incremented by one.
 *
 * <p>
 * The values of radii define the boundaries between two partitiongs. So, the first partition will
 * contain objects within this range of distances from pivot: [0,radii[0]]. The second partition will
 * contain objects within this range of distances from pivot: (radii[0],radii[1]]. Finally, the last
 * partition will contain objects within this range of distances from pivot: (radii[last],infinity).
 * The {@link messif.objects.util.ObjectMatcher matcher}
 * returns the index of the corresponding partition starting from zero.
 * </p>
 *
 * @author Vlastislav Dohnal, dohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class SplitPolicyMultiWayBallPartitioning extends SplitPolicy implements ObjectMatcher {

    //****************** Attributes ******************

    /** Policy parameter <i>pivot</i> */
    @SplitPolicy.ParameterField("pivot")
    protected LocalAbstractObject pivot = null;

    /** Policy parameter <i>radius</i> */
    @SplitPolicy.ParameterField("radii")
    protected float[] radii = null;


    /** The distance to the pivot computed by the last call to match(Region). */
    protected float dist = LocalAbstractObject.UNKNOWN_DISTANCE;

    //****************** Constructor ******************

    /** Creates a new instance of SplitPolicyBallPartitioning */
    public SplitPolicyMultiWayBallPartitioning() {
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
     * Sets the radii for multi-way ball partitioning.
     * @param radii the array of radii
     */
    public void setRadii(float[] radii) {
        setParameter("radii", radii);
    }

    /**
     * Returns the array of radii used for the multi-way ball partitioning.
     * @return the array of radii
     */
    public float[] getRadii() {
        return radii;
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
     * Returns the index of partition to which the <code>object</code> belongs.
     * @param object an object that is tested for partition
     *
     * @return partition identification (index)
     */
    @Override
    public int match(LocalAbstractObject object) {
        // Use precomputed distances
        for (int i = 0; i < radii.length; i++) {
            if (object.includeUsingPrecompDist(pivot, radii[i]))
                return i;
        }
        if (object.includeUsingPrecompDist(pivot, Float.MAX_VALUE))
            return radii.length;
        // Precomputed distances didn't help, so compute the exact distance
        float d = object.getDistance(pivot);
        for (int i = 0; i < radii.length; i++) {
            if (d <= radii[i])
                return i;
        }
        return radii.length;
    }

    /**
     * Returns the number of partitions of this policy.
     * @return the number of partitions of this policy
     */
    @Override
    public int getPartitionsCount() {
        return radii.length + 1;
    }


    /**
     * Returns the index of partition to which the whole ball region belongs.
     * Returns -1 if not all objects from the specified ball region fall into just one partition
     * or if this policy cannot decide. In that case, the ball region must be searched one object by one
     * using the {@link #match(messif.objects.LocalAbstractObject) match(LocalAbstractObject)} method.
     *
     * @param region a ball region that is tested for the matching condition
     * @return the index of partition in which the ball region is contained completely or -1 if it is uncertain
     */
    @Override
    public int match(BallRegion region) {
        // Reset the distance to the pivot
        dist = LocalAbstractObject.UNKNOWN_DISTANCE;

        // Use precomputed distances
        float min = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < radii.length; i++) {
            if (pivot.includeUsingPrecompDist(region.getPivot(), radii[i] - region.getRadius()) &&      // <=
                pivot.excludeUsingPrecompDist(region.getPivot(), min + region.getRadius()))             // >
                return i;
            if (pivot.includeUsingPrecompDist(region.getPivot(), radii[i] + region.getRadius()))
                return -1;      // Stop condition (when we moved behind the region).
            min = radii[i];
        }
        if (pivot.excludeUsingPrecompDist(region.getPivot(), radii[radii.length-1] + region.getRadius()))
            return radii.length;
        
        // Compute the distance and decide on its basis
        dist = pivot.getDistance(region.getPivot());
        min = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < radii.length; i++) {
            if (dist - region.getRadius() <= radii[i] &&      // <=
                dist + region.getRadius() > min)              // >
                return i;
            if (dist + region.getRadius() <= radii[i])
                return -1;      // Stop condition (when we moved behind the region).
            min = radii[i];
        }
        if (dist - region.getRadius() > radii[radii.length-1])
            return radii.length;
        else
            return PART_ID_ANY;  // The region intersects more partitions
    }

}

package messif.buckets.split.impl;

import messif.buckets.split.SplitPolicy;
import messif.objects.BallRegion;
import messif.objects.LocalAbstractObject;
import messif.objects.util.ObjectMatcher;

/**
 * This class implements Voronoi-like partitioning policy.
 *
 * @author Vlastislav Dohnal (xdohnal), dohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class SplitPolicyVoronoiPartitioning extends SplitPolicy implements ObjectMatcher {

    //****************** Attributes ******************

    /** Policy parameter <i>pivot</i> */
    @SplitPolicy.ParameterField("pivots")
    protected LocalAbstractObject[] pivots = null;

    //****************** Constructor ******************

    /** Creates a new instance of SplitPolicyBallPartitioning */
    public SplitPolicyVoronoiPartitioning() {
    }

    //****************** Parameter quick setter/getters ******************

    /**
     * Returns the array of pivots used for the Voronoi-like partitioning.
     * @return the array of pivots
     */
    public LocalAbstractObject[] getPivots() {
        return pivots;
    }

    /**
     * Sets the pivots for Voronoi-like partitioning.
     * @param pivots the array of pivots
     */
    public void setPivots(LocalAbstractObject[] pivots) {
        setParameter("pivots", pivots);
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
        int closestPivot = 0;
        float dist = LocalAbstractObject.MAX_DISTANCE;

        for (int p = 0; p < pivots.length; p++) {
            float d = pivots[p].getDistance(object);
            if (d <= dist) {
                closestPivot = p;
                dist = d;
            }
        }
        return closestPivot;
    }

    /**
     * Returns the number of partitions of this policy.
     * @return the number of partitions of this policy
     */
    @Override
    public int getPartitionsCount() {
        return pivots.length;
    }

    /**
     * NOT IMPLEMENTED YET!!!!
     *
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
        throw new UnsupportedOperationException("Not supported yet.");
    }


}

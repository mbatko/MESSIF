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
package messif.buckets.split.impl;

import messif.buckets.split.SplitPolicy;
import messif.objects.BallRegion;
import messif.objects.LocalAbstractObject;

/**
 * This class an excluded-middle ball-partitioning policy for bucket splitting.
 *
 * <p>
 * The policy is fully defined by:<br/>
 * {@link messif.objects.LocalAbstractObject LocalAbstractObject} <i>pivot</i><br/>
 * <code>float</code> <i>radius</i> and <br/>
 * <code>float</code> <i>rho</i>
 * </p>
 *
 * <p>
 * The parameter <code>rho</code> defines a ring of the 2rho width centered at the distance <code>radius</code> from the pivot.
 * All objects that have distances to the pivot smaller or equal to the radius minus rho form one partition,
 * object having the distances greater than the radius plus rho form the second partitiong. The remaining objects falls to
 * the excluded one (third partition). The {@link messif.objects.util.ObjectMatcher matcher}
 * returns 1 for objects outside farer than the ring, 0 for objects closer than the ring, and 2 for objects within the ring.
 * </p>
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class SplitPolicyExcludedMiddlePartitioning extends SplitPolicy {

    //****************** Attributes ******************

    /** Policy parameter <i>pivot</i> */
    @SplitPolicy.ParameterField("pivot")
    protected LocalAbstractObject pivot = null;

    /** Policy parameter <i>radius</i> */
    @SplitPolicy.ParameterField("radius")
    protected float radius = LocalAbstractObject.UNKNOWN_DISTANCE;

    /** Policy parameter <i>rho</i> */
    @SplitPolicy.ParameterField("rho")
    protected float rho = LocalAbstractObject.UNKNOWN_DISTANCE;


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

    /**
     * Identification of the outer partition.
     * Equal to <code>2</code>
     */
    public final static int PART_ID_EXCLUDED = 2;

    //****************** Constructor ******************

    /** Creates a new instance of SplitPolicyExcludedMiddlePartitioning */
    public SplitPolicyExcludedMiddlePartitioning() {
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
     * Sets the rho for excluded-middle partitioning.
     * @param rho the rho value
     */
    public void setRho(float rho) {
        this.rho = rho;
    }

    /**
     * Returns the rho for excluded-middle partitioning.
     * @return the rho for excluded-middle partitioning
     */
    public float getRho() {
        return rho;
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
    @Override
    public int match(LocalAbstractObject object) {
        if (object.includeUsingPrecompDist(pivot, radius-rho))
            return PART_ID_INNER;
        else if (object.excludeUsingPrecompDist(pivot, radius+rho))
            return PART_ID_OUTER;

        // Precomputed distances didn't help, so compute the exact distance
        float d = object.getDistance(pivot);
        if (d <= radius-rho)
            return PART_ID_INNER;
        else if (d > radius+rho)
            return PART_ID_OUTER;
        else
            return PART_ID_EXCLUDED;
    }

    /**
     * Returns the number of partitions of this policy.
     * @return the number of partitions of this policy
     */
    @Override
    public int getPartitionsCount() {
        return 3;
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
    @Override
    public int match(BallRegion region) {
        // Reset the distance to the pivot
        dist = LocalAbstractObject.UNKNOWN_DISTANCE;

        if (pivot.includeUsingPrecompDist(region.getPivot(), radius - rho - region.getRadius()))
            // <=
            return PART_ID_INNER;
        if (pivot.excludeUsingPrecompDist(region.getPivot(), radius + rho + region.getRadius()))
            // >
            return PART_ID_OUTER;

        // Compute the distance and decide on its basis
        dist = pivot.getDistance(region.getPivot());

        if (dist + region.getRadius() <= radius-rho)        // The region 'region' is completely included
            // <=
            return PART_ID_INNER;
        else if (dist - region.getRadius() > radius+rho)    // Regions do not intersect nor touch
            // >
            return PART_ID_OUTER;
        else if (dist - region.getRadius() > radius-rho && dist + region.getRadius() <= radius+rho)              // Region is within the excluded ring
            return PART_ID_EXCLUDED;
        else
            // The region intersects both the partitions
            return PART_ID_ANY;
    }

}

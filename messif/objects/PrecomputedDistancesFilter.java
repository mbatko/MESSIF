/*
 * PrecomputedDistancesFilter.java
 *
 */

package messif.objects;

import java.io.Serializable;
import messif.statistics.StatisticCounter;

/**
 * 
 *
 * @author xbatko
 */
public abstract class PrecomputedDistancesFilter implements Cloneable, Serializable {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /****************** Statistics ******************/

    /** Global counter for saving distance computations by using precomputed */
    protected static StatisticCounter counterPrecomputedDistanceSavings = StatisticCounter.getStatistics("DistanceComputations.Savings");


    /****************** Filter chaining ******************/

    /** The next filter in this chain */
    PrecomputedDistancesFilter nextFilter = null;

    /**
     * Returns the next filter in this filter's chain.
     * @return the next filter in this filter's chain
     */
    public PrecomputedDistancesFilter getNextFilter() {
        return nextFilter;
    }

    /**
     * Adds the specified filter to the end of this filter's chain.
     *
     * @param filter the filter to add to this filter's chain
     * @param replaceIfExists if <tt>true</tt> and there is another filter with the same class as the inserted filter, it is replaced
     * @return either the replaced or the existing filter that has the same class as the newly inserted one; <tt>null</tt> is
     *         returned if the filter was appended to the end of the chain
     */
    final PrecomputedDistancesFilter chainFilter(PrecomputedDistancesFilter filter, boolean replaceIfExists) {
        if (nextFilter == null) {
            // We are at the end of the chain
            nextFilter = filter;
            return null;
        } else if (nextFilter.getClass().equals(filter.getClass())) {
            if (!replaceIfExists)
                return nextFilter;
            // Preserve the chain link
            filter.nextFilter = nextFilter.nextFilter;
            // Replace filter
            PrecomputedDistancesFilter storedFilter = nextFilter;
            nextFilter = filter;
            return storedFilter;
        } else
            // Recurse into subfilters
            return nextFilter.chainFilter(filter, replaceIfExists);
    }


    /****************** Filtering methods ******************/

    /**
     * Returns the precomputed distance to an object.
     * If there is no distance associated with the object <tt>obj</tt>
     * the function returns {@link LocalAbstractObject#UNKNOWN_DISTANCE UNKNOWN_DISTANCE}.
     * 
     * 
     * @param obj the object for which the precomputed distance is returned
     * @return the precomputed distance to an object
     */
    public final float getPrecomputedDistance(LocalAbstractObject obj) {
        float distance = getPrecomputedDistanceImpl(obj);
        if (distance != LocalAbstractObject.UNKNOWN_DISTANCE) {
            counterPrecomputedDistanceSavings.add();
            return distance;
        } else if (nextFilter != null) {
            return nextFilter.getPrecomputedDistance(obj);
        } else return LocalAbstractObject.UNKNOWN_DISTANCE;
    }

    /**
     * Returns <tt>true</tt> if this object supports {@link #getPrecomputedDistance} method.
     * @return <tt>true</tt> if this object supports {@link #getPrecomputedDistance} method
     */
    public abstract boolean isGetterSupported();

    /**
     * Implement this method to return the precomputed distance to an object.
     * If there is no distance associated with the object <tt>obj</tt>
     * the function should return {@link LocalAbstractObject#UNKNOWN_DISTANCE UNKNOWN_DISTANCE}.
     * 
     * 
     * @param obj the object for which the precomputed distance is returned
     * @return the precomputed distance to an object
     */
    protected abstract float getPrecomputedDistanceImpl(LocalAbstractObject obj);

    /**
     * Returns <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be excluded (filtered out) using this precomputed distances.
     * See {@link messif.objects.LocalAbstractObject#excludeUsingPrecompDist} for full explanation.
     *
     * @param targetFilter the target precomputed distances
     * @param radius the radius to check the precomputed distances for
     * @return <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be excluded (filtered out) using this precomputed distances
     */
    public final boolean excludeUsingPrecompDist(PrecomputedDistancesFilter targetFilter, float radius) {
        if (excludeUsingPrecompDistImpl(targetFilter, radius)) {
            counterPrecomputedDistanceSavings.add();
            return true;
        } else if (nextFilter != null && targetFilter.nextFilter != null) {
            return nextFilter.excludeUsingPrecompDist(targetFilter.nextFilter, radius);
        } else return false;
    }

    /**
     * Returns <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be included using this precomputed distances.
     * See {@link messif.objects.LocalAbstractObject#includeUsingPrecompDist} for full explanation.
     *
     * @param targetFilter the target precomputed distances
     * @param radius the radius to check the precomputed distances for
     * @return <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be included using this precomputed distances
     */
    public final boolean includeUsingPrecompDist(PrecomputedDistancesFilter targetFilter, float radius) {
        if (includeUsingPrecompDistImpl(targetFilter, radius)) {
            counterPrecomputedDistanceSavings.add();
            return true;
        } else if (nextFilter != null && targetFilter.nextFilter != null) {
            return nextFilter.includeUsingPrecompDist(targetFilter.nextFilter, radius);
        } else return false;
    }

    /**
     * Returns <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be excluded (filtered out) using this precomputed distances.
     * See {@link messif.objects.LocalAbstractObject#excludeUsingPrecompDist} for full explanation.
     *
     * @param targetFilter the target precomputed distances
     * @param radius the radius to check the precomputed distances for
     * @return <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be excluded (filtered out) using this precomputed distances
     */
    protected abstract boolean excludeUsingPrecompDistImpl(PrecomputedDistancesFilter targetFilter, float radius);

    /**
     * Returns <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be included using this precomputed distances.
     * See {@link messif.objects.LocalAbstractObject#includeUsingPrecompDist} for full explanation.
     *
     * @param targetFilter the target precomputed distances
     * @param radius the radius to check the precomputed distances for
     * @return <tt>true</tt> if object associated with <tt>targetFilter</tt> filter can be included using this precomputed distances
     */
    protected abstract boolean includeUsingPrecompDistImpl(PrecomputedDistancesFilter targetFilter, float radius);


    /****************** Write *********************/

    /**
     * Return the string value of this filter.
     * @return the string value of this filter
     */
    public abstract String getText();


    /****************** Clonning ******************/

    /**
     * Creates and returns a copy of this object.
     * @return a copy of this object
     * @throws CloneNotSupportedException if this object cannot be cloned.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        PrecomputedDistancesFilter rtv = (PrecomputedDistancesFilter)super.clone();
        if (rtv.nextFilter != null)
            rtv.nextFilter = (PrecomputedDistancesFilter)rtv.nextFilter.clone();
        return rtv;
    }

}

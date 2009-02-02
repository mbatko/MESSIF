/*
 * PrecomputedDistancesFilter.java
 *
 */

package messif.objects;

import java.io.IOException;
import java.io.Serializable;
import messif.objects.nio.BinaryInput;
import messif.objects.nio.BinaryOutput;
import messif.objects.nio.BinarySerializable;
import messif.objects.nio.BinarySerializator;
import messif.statistics.StatisticCounter;

/**
 * This class provides a framework for metric-distance filtering techniques.
 * Using a triangle inequality prperty of the metric space, some distance calculations
 * can be avoided provided there is some additional information - the precomputed
 * distances.
 * 
 * <p>
 * A filter is added to a {@link LocalAbstractObject} via its
 * {@link LocalAbstractObject#chainFilter chainFilter} method. Objects can have
 * several filters chained - if the first filter fails to avoid the computation,
 * the next is used and so on. The filters are then used automatically whenever a
 * {@link LocalAbstractObject#getDistance(LocalAbstractObject) distance computation}
 * is evaluated.
 * </p>
 *
 * @author xbatko
 */
public abstract class PrecomputedDistancesFilter implements Cloneable, Serializable, BinarySerializable {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /****************** Statistics ******************/

    /** Global counter for saving distance computations by using precomputed */
    protected static StatisticCounter counterPrecomputedDistanceSavings = StatisticCounter.getStatistics("DistanceComputations.Savings");


    /****************** Constructor ******************/

    /**
     * Creates a new instance of PrecomputedDistancesFilter.
     */
    protected PrecomputedDistancesFilter() {
    }


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


    //************ BinarySerializable interface ************//

    /**
     * Creates a new instance of PrecomputedDistancesFilter loaded from binary input.
     * 
     * @param input the input to read the PrecomputedDistancesFilter from
     * @param serializator the serializator used to write objects
     * @throws IOException if there was an I/O error reading from the input
     */
    protected PrecomputedDistancesFilter(BinaryInput input, BinarySerializator serializator) throws IOException {
        nextFilter = serializator.readObject(input, PrecomputedDistancesFilter.class);
    }

    /**
     * Binary-serialize this object into the <code>output</code>.
     * @param output the output that this object is binary-serialized into
     * @param serializator the serializator used to write objects
     * @return the number of bytes actually written
     * @throws IOException if there was an I/O error during serialization
     */
    public int binarySerialize(BinaryOutput output, BinarySerializator serializator) throws IOException {
        return serializator.write(output, nextFilter);
    }

    /**
     * Returns the exact size of the binary-serialized version of this object in bytes.
     * @param serializator the serializator used to write objects
     * @return size of the binary-serialized version of this object
     */
    public int getBinarySize(BinarySerializator serializator) {
        return serializator.getBinarySize(nextFilter);
    }

}

/*
 * RankedSortedCollection
 *
 */

package messif.objects.util;

import java.util.Comparator;
import java.util.NoSuchElementException;
import messif.objects.LocalAbstractObject;
import messif.utility.SortedCollection;

/**
 * Specialization of {@link SortedCollection} that is specific for distance-ranked objects.
 * @author xbatko
 */
public class RankedSortedCollection extends SortedCollection<RankedAbstractObject> {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constructor ******************//

    /**
     * Constructs an empty collection with the specified initial and maximal capacity.
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capatity of the collection
     * @param comparator the comparator that defines ordering
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public RankedSortedCollection(int initialCapacity, int maximalCapacity, Comparator<? super RankedAbstractObject> comparator) throws IllegalArgumentException {
        super(initialCapacity, maximalCapacity, comparator);
    }

    /**
     * Constructs an empty collection with the specified initial and maximal capacity.
     * The order is defined using the natural order of items.
     * @param initialCapacity the initial capacity of the collection
     * @param maximalCapacity the maximal capatity of the collection
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public RankedSortedCollection(int initialCapacity, int maximalCapacity) throws IllegalArgumentException {
        super(initialCapacity, maximalCapacity, null);
    }

    /**
     * Constructs an empty collection.
     * The order is defined using the natural order of items.
     * The initial capacity of the collection is set to 16 and maximal capacity
     * is not limited.
     * @throws IllegalArgumentException if the specified initial or maximal capacity is invalid
     */
    public RankedSortedCollection() throws IllegalArgumentException {
        super();
    }


    //****************** Distance operations ******************//

    /**
     * Returns the distance of the last object in this collection.
     * @return the distance of the last object in this collection
     * @throws NoSuchElementException if this collection is empty
     */
    public float getLastDistance() throws NoSuchElementException {
        return last().getDistance();
    }

    /**
     * Returns the threshold distance for this collection.
     * If this collection has not reached the maximal size (specified in constructor) yet,
     * {@link LocalAbstractObject#MAX_DISTANCE} is returned.
     * Otherwise, the distance of the last object of this collection is returned.
     * @return the distance to the last object in this collection or
     *         {@link LocalAbstractObject#MAX_DISTANCE} if there are not enough objects.
     */
    public float getThresholdDistance() {
        if (isFull())
            return getLastDistance();
        else
            return LocalAbstractObject.MAX_DISTANCE;
    }

}

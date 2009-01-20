/*
 *  OrderedLocalBucket
 * 
 */

package messif.buckets;

import messif.buckets.index.ModifiableOrderedIndex;
import messif.buckets.index.OrderedIndex;
import messif.objects.LocalAbstractObject;

/**
 * An extension of {@link LocalBucket} that maintains the stored objects in
 * a certain order.
 * 
 * @param <C> type of the keys that this bucket's objects are ordered by
 *
 * @author  xbatko
 */
public abstract class OrderedLocalBucket<C> extends LocalBucket {
    /** class serial id for serialization */
    private static final long serialVersionUID = 934001L;

    /**
     * Constructs a new LocalBucket instance and setups all bucket limits
     *
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    protected OrderedLocalBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
    }

    @Override
    public OrderedIndex<C, LocalAbstractObject> getIndex() {
        // Update statistics
        counterBucketRead.add(this);
        
        return getModifiableIndex();
    }

    protected abstract ModifiableOrderedIndex<C, LocalAbstractObject> getModifiableIndex();

}

/*
 * MemoryStorageKeyBucket
 *
 */

package messif.buckets.impl;

import messif.objects.LocalAbstractObject;
import java.io.Serializable;
import messif.buckets.BucketDispatcher;
import messif.buckets.LocalBucket;
import messif.buckets.OrderedLocalBucket;
import messif.buckets.index.LocalAbstractObjectOrder;
import messif.buckets.index.ModifiableOrderedIndex;
import messif.buckets.index.impl.IntStorageIndex;
import messif.buckets.storage.impl.MemoryStorage;
import messif.objects.keys.AbstractObjectKey;


/**
 * A volatile implementation of {@link LocalBucket}.
 * It stores all objects in a {@link messif.buckets.storage.impl.MemoryStorage memory storage}.
 * Objects are indexed by their {@link LocalAbstractObject#getObjectKey() object keys} and
 * iterator will return the objects ordered.
 * 
 * <p>
 * This bucket has an efficient {@link LocalBucket#getObject(messif.objects.keys.AbstractObjectKey)} implementation
 * at the cost of additional memory overhead for maintaining the index.
 * If fast {@code getObject} implementation is not required and
 * the iteration over all objects is used frequently, consider using
 * {@link MemoryStorageBucket}.
 * </p>
 *
 * @author  xbatko
 * @see BucketDispatcher
 * @see LocalBucket
 */
public class MemoryStorageKeyBucket extends OrderedLocalBucket<AbstractObjectKey> implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Data storage ******************//

    /** Object storage with object-key index */
    protected ModifiableOrderedIndex<AbstractObjectKey, LocalAbstractObject> objects =
            new IntStorageIndex<AbstractObjectKey, LocalAbstractObject>(
                    new MemoryStorage<LocalAbstractObject>(),
                    LocalAbstractObjectOrder.keyToLocalObjectComparator
            );


    /****************** Constructors ******************/

    /**
     * Constructs a new instance of MemoryStorageKeyBucket.
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    public MemoryStorageKeyBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
    }


    //****************** Overrides ******************//

    @Override
    protected ModifiableOrderedIndex<AbstractObjectKey, LocalAbstractObject> getModifiableIndex() {
        return objects;
    }

}

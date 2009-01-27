/*
 * MemoryStorageBucket.java
 *
 * Created on 16. kveten 2008, 10:55
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
 * It stores all objects in main memory as a linked list.
 *
 * The bucket should be created by {@link BucketDispatcher}.
 *
 * @author  xbatko
 * @see BucketDispatcher
 * @see LocalBucket
 */
public class ObjectKeyMemoryStorageBucket extends OrderedLocalBucket<AbstractObjectKey> implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Data storage ******************//

    /** Object storage with ID index */
    protected ModifiableOrderedIndex<AbstractObjectKey, LocalAbstractObject> objects =
            new IntStorageIndex<AbstractObjectKey, LocalAbstractObject>(
                    new MemoryStorage<LocalAbstractObject>(),
                    LocalAbstractObjectOrder.keyToLocalObjectComparator
            );


    /****************** Constructors ******************/

    /**
     * Constructs a new MemoryStorageBucket instance
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    public ObjectKeyMemoryStorageBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
    }


    //****************** Overrides ******************//

    @Override
    protected ModifiableOrderedIndex<AbstractObjectKey, LocalAbstractObject> getModifiableIndex() {
        return objects;
    }

}

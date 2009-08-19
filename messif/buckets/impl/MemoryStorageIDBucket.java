/*
 * MemoryStorageIDBucket
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
import messif.objects.UniqueID;


/**
 * A volatile implementation of {@link LocalBucket}.
 * It stores all objects in a {@link messif.buckets.storage.impl.MemoryStorage memory storage}.
 * Objects are indexed by their {@link LocalAbstractObject#getObjectID object IDs} and
 * iterator will return the objects ordered.
 * 
 * <p>
 * This bucket has an efficient {@link LocalBucket#getObject(messif.objects.UniqueID)} implementation
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
public class MemoryStorageIDBucket extends OrderedLocalBucket<UniqueID> implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Data storage ******************//

    /** Object storage with object-id index */
    protected ModifiableOrderedIndex<UniqueID, LocalAbstractObject> objects =
            new IntStorageIndex<UniqueID, LocalAbstractObject>(
                    new MemoryStorage<LocalAbstractObject>(LocalAbstractObject.class),
                    LocalAbstractObjectOrder.uniqueIDComparator
            );


    /****************** Constructors ******************/

    /**
     * Constructs a new instance of MemoryStorageIDBucket.
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    public MemoryStorageIDBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
    }


    //****************** Overrides ******************//

    @Override
    protected ModifiableOrderedIndex<UniqueID, LocalAbstractObject> getModifiableIndex() {
        return objects;
    }

    @Override
    public void finalize() throws Throwable {
        objects.finalize();
        super.finalize();
    }

    @Override
    public void destroy() throws Throwable {
        objects.destroy();
        super.destroy();
    }

}

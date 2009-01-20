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
import messif.buckets.index.ModifiableIndex;
import messif.buckets.storage.impl.IndexedMemoryStorage;


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
public class MemoryStorageBucket extends LocalBucket implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 4L;

    //****************** Data storage ******************//

    /** Object storage with ID index */
    protected ModifiableIndex<LocalAbstractObject> objects = new IndexedMemoryStorage<LocalAbstractObject>();


    /****************** Constructors ******************/

    /**
     * Constructs a new MemoryStorageBucket instance
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    public MemoryStorageBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
    }


    //****************** Overrides ******************//

    @Override
    protected ModifiableIndex<LocalAbstractObject> getModifiableIndex() {
        return objects;
    }

}

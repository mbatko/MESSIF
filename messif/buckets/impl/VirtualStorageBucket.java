/*
 *  VirtualStorageBucket
 * 
 */

package messif.buckets.impl;

import messif.buckets.LocalBucket;
import messif.buckets.index.ModifiableIndex;
import messif.objects.LocalAbstractObject;

/**
 *
 * @author xbatko
 */
public class VirtualStorageBucket extends LocalBucket {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    
    /**
     * Constructs a new MemoryStorageBucket instance
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    public VirtualStorageBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
    }

    
    @Override
    protected ModifiableIndex<LocalAbstractObject> getModifiableIndex() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}

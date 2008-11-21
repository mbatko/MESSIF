/*
 * MemoryStorageIDBucket.java
 *
 * Created on 24. duben 2004, 12:17
 */

package messif.buckets.impl;

import messif.objects.LocalAbstractObject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import messif.buckets.Bucket;
import messif.buckets.BucketDispatcher;
import messif.buckets.BucketErrorCode;
import messif.buckets.LocalBucket;
import messif.buckets.LocalFilteredBucket;
import messif.buckets.OccupationLowException;
import messif.objects.UniqueID;


/**
 * A volatile implementation of {@link LocalBucket}.
 * It stores all objects in main memory as a hash table with object IDs as keys.
 * Thus it has an efficient {@link Bucket#getObject getObject} implementation
 * at the cost of additional memory overhead for maintaining the hash table.
 * If fast {@link Bucket#getObject getObject} implementation is not required and
 * the iteration over all objects is used frequently, consider using
 * {@link MemoryStorageBucket}.
 *
 * The bucket should be created by {@link BucketDispatcher}.
 *
 * @author  xbatko
 * @see BucketDispatcher
 * @see LocalBucket
 * @see MemoryStorageBucket
 */
public class MemoryStorageIDBucket extends LocalFilteredBucket implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 2L;

    /****************** Data storage ******************/

    /** Bucket object holder, stores <ID,object> pairs - good for fast access to objects by their ID. */
    protected Map<UniqueID, LocalAbstractObject> objects = new HashMap<UniqueID, LocalAbstractObject>();


    /****************** Constructors ******************/

    /**
     * Constructs a new MemoryStorageIDBucket instance
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    public MemoryStorageIDBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
    }


    /****************** Overrides ******************/

    /**
     * Returns current number of objects stored in bucket.
     * @return current number of objects stored in bucket
     */
    public int getObjectCount() {
        return objects.size();
    }

    /**
     * Stores the specified object in a the hash table.
     *
     * @param object the new object to be inserted
     * @return OBJECT_INSERTED if the object was successfuly inserted,
     *         otherwise an exception is thrown (usually OutOfMemoryError)
     */
    protected BucketErrorCode storeObject(LocalAbstractObject object) {
        objects.put(object, object);
        return BucketErrorCode.OBJECT_INSERTED;
    }

    /**
     * Returns iterator through all the objects in this bucket.
     * @return iterator through all the objects in this bucket
     */
    protected LocalBucketIterator<? extends MemoryStorageIDBucket> iterator() {
        // No specialize iterator is needed for this class.
        return new MemoryStorageBucketIterator<MemoryStorageIDBucket>(this);
    }


    /**
     * Delete all objects from this bucket.
     * @return the number of deleted objects
     * @throws OccupationLowException if the low occupation limit is reached when deleting objects
     */
    @Override
    public synchronized int deleteAllObjects() throws OccupationLowException {
        // If the bucket has some required lowest occupation, this method cannot be used
        if (lowOccupation > 0)
            throw new OccupationLowException();

        int deleted = objects.size();
        objects.clear();
        // Update statistics
        occupation = 0;
        counterBucketDelObject.add(this, deleted);
        return deleted;
    }


    /****************** Iterator object ******************/

    /**
     * Internal class for iterator implementation.
     * @param <T> the bucket class on which this iterator is implemented
     */
    protected static class MemoryStorageBucketIterator<T extends MemoryStorageIDBucket> extends LocalBucket.LocalBucketIterator<T> {
        /** Currently executed iterator */
        protected Iterator<LocalAbstractObject> iterator;
        /** Last returned object */
        protected LocalAbstractObject currentObject = null;

        /**
         * Creates a new instance of MemoryStorageBucketIterator with the MemoryStorageIDBucket.
         * This constructor is intended to be called only from MemoryStorageIDBucket class.
         * The method also initialize the iterator from the hash table objects.
         *
         * @param bucket actual instance of AlgorithmStorageBucket on which this iterator should work
         */
        protected MemoryStorageBucketIterator(T bucket) {
           super(bucket);
           this.iterator = bucket.objects.values().iterator();
        }

        /**
         * Physically remove the current object.
         */
        protected void removeInternal() {
            if (iterator == null)
                bucket.objects.remove(currentObject);
            else iterator.remove();
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration.
         * @throws NoSuchElementException iteration has no more elements.
         */
        public LocalAbstractObject next() throws NoSuchElementException {
            if (iterator == null)
                throw new NoSuchElementException("Can't call next after the last object has been retrieved");
            return currentObject = iterator.next();
        }

        /**
         * Returns <tt>true</tt> if the iteration has more elements. (In other
         * words, returns <tt>true</tt> if <tt>next</tt> would return an element
         * rather than throwing an exception.)
         *
         * @return <tt>true</tt> if the iterator has more elements.
         */
        public boolean hasNext() {
            return (iterator == null)?false:iterator.hasNext();
        }

        /**
         * Returns the object returned by the last call to next().
         * @return the object returned by the last call to next()
         * @throws NoSuchElementException if next() has not been called yet
         */
        public LocalAbstractObject getCurrentObject() throws NoSuchElementException {
            if (currentObject == null)
                throw new NoSuchElementException("Can't call getCurrentObject before next was called");
            
            return currentObject;
        }

        /**
         * Returns the first instance of object, that has the specified ID.
         *
         * @param objectID ID of the object that we are searching for
         * @return the first instance of object, that has the specified objectID
         * @throws NoSuchElementException if such an object cannot be found.
         */
        @Override
        public LocalAbstractObject getObjectByID(UniqueID objectID) throws NoSuchElementException {
            currentObject = bucket.objects.get(objectID);
            iterator = null;
            if (currentObject == null)
                throw new NoSuchElementException("There is no object with the specified ID");
            
            return currentObject;
        }

    }

}

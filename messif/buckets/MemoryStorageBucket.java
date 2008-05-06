/*
 * MemoryStorageBucket.java
 *
 * Created on 24. duben 2004, 12:17
 */

package messif.buckets;

import messif.objects.LocalAbstractObject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import messif.objects.UniqueID;


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
public class MemoryStorageBucket extends LocalFilteredBucket implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 2L;
    
    /****************** Data storage ******************/
    
    /** Bucket object holder, stores <ID,object> pairs - good for fast access to objects by their ID. */
    protected Map<UniqueID, LocalAbstractObject> objects = new HashMap<UniqueID, LocalAbstractObject>();
    

    /****************** Constructors ******************/

    /**
     * Constructs a new MemoryStorageBucket instance
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    protected MemoryStorageBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes) {
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
    protected LocalBucketIterator<? extends MemoryStorageBucket> iterator() {
        // No specialize iterator is needed for this class.
        return new MemoryStorageBucketIterator<MemoryStorageBucket>(this);
    }

    /****************** Iterator object ******************/
    
    /** Internal class for iterator implementation */
    protected static class MemoryStorageBucketIterator<T extends MemoryStorageBucket> extends LocalBucket.LocalBucketIterator<T> {
        /** Currently executed iterator */
        protected Iterator<LocalAbstractObject> iterator;
        /** Last returned object */
        protected LocalAbstractObject currentObject = null;

        /**
         * Creates a new instance of MemoryStorageBucketIterator with the MemoryStorageBucket.
         * This constructor is intended to be called only from MemoryStorageBucket class.
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

        public LocalAbstractObject getObjectByID(UniqueID objectID) throws NoSuchElementException {
            currentObject = bucket.objects.get(objectID);
            iterator = null;
            if (currentObject == null)
                throw new NoSuchElementException("There is no object with the specified ID");
            
            return currentObject;
        }

    }    
    

}

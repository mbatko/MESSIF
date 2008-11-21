/*
 * MemoryStorageLocatorBucket.java
 *
 */

package messif.buckets.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import messif.buckets.BucketErrorCode;
import messif.buckets.OccupationLowException;
import messif.objects.LocalAbstractObject;
import messif.objects.UniqueID;

/**
 * Extension of {@link MemoryStorageBucket} that supports fast object retrieval by locators.
 *
 * @author  xbatko
 * @see MemoryStorageBucket
 */
public class MemoryStorageLocatorBucket extends MemoryStorageBucket {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;

    /** Lookup table for object locators */
    protected transient Map<String, LocalAbstractObject> locatorMap = new HashMap<String, LocalAbstractObject>();


    /****************** Constructors ******************/

    /**
     * Constructs a new MemoryStorageLocatorBucket instance
     *
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    public MemoryStorageLocatorBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
    }


    /******************     Serialization     **********************/

    /** Deserialization -- restore the set using the objects already read by the ancestor */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        locatorMap = new HashMap<String, LocalAbstractObject>(objects.size());
        for (LocalAbstractObject object : objects)
            locatorMap.put(object.getLocatorURI(), object);
    }


    /****************** Storing duplicates override ******************/

    /**
     * Stores an object in a physical storage.
     * It should return OBJECT_INSERTED value if the object was successfuly inserted.
     *
     * @param object The new object to be inserted
     * @return error code - for details, see documentation of {@link BucketErrorCode}
     */
    @Override
    protected BucketErrorCode storeObject(LocalAbstractObject object) {
        BucketErrorCode errCode = super.storeObject(object);
        if (errCode.equals(BucketErrorCode.OBJECT_INSERTED) && object.getLocatorURI() != null)
            locatorMap.put(object.getLocatorURI(), object);
        return errCode;
    }


    /**
     * Delete object with specified ID from this bucket.
     * @param objectID ID of the object to delete
     * @throws NoSuchElementException This exception is thrown if there is no object with the specified ID in this bucket
     * @throws OccupationLowException This exception is throws if the low occupation limit is reached when deleting object
     * @return The object deleted from this bucket
     */
    @Override
    public LocalAbstractObject deleteObject(UniqueID objectID) throws NoSuchElementException, OccupationLowException {
        LocalAbstractObject rtv = super.deleteObject(objectID);
        locatorMap.remove(rtv.getLocatorURI());
        return rtv;
    }

    /**
     * Delete all objects from this bucket.
     * @return the number of deleted objects
     * @throws OccupationLowException if the low occupation limit is reached when deleting objects
     */
    @Override
    public synchronized int deleteAllObjects() throws OccupationLowException {
        int deleted = super.deleteAllObjects();
        locatorMap.clear();
        return deleted;
    }


    /****************** Iterator object ******************/

    /**
     * Internal class for iterator implementation
     * @param <T> the bucket class on which this iterator is implemented
     */
    protected static class MemoryStorageLocatorBucketIterator<T extends MemoryStorageLocatorBucket> extends MemoryStorageBucket.MemoryStorageBucketIterator<T> {
        /**
         * Creates a new instance of MemoryStorageLocatorBucketIterator with the MemoryStorageLocatorBucket.
         * This constructor is intended to be called only from MemoryStorageLocatorBucket class.
         * @param bucket actual instance of MemoryStorageLocatorBucket on which this iterator should work
         */
        protected MemoryStorageLocatorBucketIterator(T bucket) {
           super(bucket);
        }

        /**
         * Physically remove the object this iterator points at.
         * Simply calls the hashtable iterator remove method.
         */
        @Override
        protected void removeInternal() {
            super.removeInternal();
            if (currentObject.getLocatorURI() != null)
                bucket.locatorMap.remove(currentObject.getLocatorURI());
        }

        /**
         * Returns the first instance of object, that has one of the specified locators.
         * The locators are checked one by one using hash table. The first locator that
         * has an object associated is returned. The locators without an object associated
         * are also removed from the set if <code>removeFound</code> is <tt>true</tt>.
         *
         * @param locatorURIs the set of locators that we are searching for
         * @param removeFound if <tt>true</tt> the locators which were found are removed from the <tt>locatorURIs</tt> set, otherwise, <tt>locatorURIs</tt> is not touched
         * @return the first instance of object, that has one of the specified locators
         * @throws NoSuchElementException if there is no object with any of the specified locators
         */
        @Override
        public LocalAbstractObject getObjectByAnyLocator(Set<String> locatorURIs, boolean removeFound) throws NoSuchElementException {
            Iterator<String> urisIterator = locatorURIs.iterator();
            while (urisIterator.hasNext()) {
                currentObject = bucket.locatorMap.get(urisIterator.next());
                if (removeFound)
                    urisIterator.remove();
                if (currentObject != null)
                    return currentObject;
            }

            throw new NoSuchElementException("There is no object with the specified locator");
        }

    }

}

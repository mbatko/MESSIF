/*
 * BucketDescriptor.java
 *
 * Created on 4. kveten 2003, 22:02
 */

package messif.buckets;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import messif.buckets.split.SplitPolicy;
import messif.objects.util.AbstractObjectList;
import messif.objects.LocalAbstractObject;
import messif.objects.ObjectProvider;
import messif.objects.UniqueID;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.util.AbstractObjectIterator;
import messif.operations.QueryOperation;


/**
 * A data area that hold a collection of <tt>AbstractObjects</tt>.
 * A bucket can represent a metric space partition or it is used just as a generic object storage.
 *
 * The bucket provides methods for inserting one or more objects, deleting them, retrieving all
 * objects or just a particular one (providing its ID). It also has a method for evaluating
 * queries, which pushes all objects from the bucket to the sequential scan implementation of the
 * respective query (if not overriden).
 * 
 * Every bucket is also automatically assigned a unique ID used for addressing the bucket.
 *
 * @author  xbatko
 * @see LocalBucket
 * @see messif.netbucket.RemoteBucket
 */
public abstract class Bucket implements ObjectProvider<LocalAbstractObject> {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;


    //****************** Bucket attributes access ******************//

    /**
     * Returns the unique ID of this bucket.
     * @return this bucket ID
     */
    public abstract int getBucketID();


    //****************** Insertion of objects ******************//

    /**
     * Insert a new object into this bucket.
     * 
     * @param object a new object to be inserted
     * @throws BucketStorageException if the object cannot be inserted into the bucket
     */
    public abstract void addObject(LocalAbstractObject object) throws BucketStorageException;

    /**
     * Insert a new object into the bucket.
     * The object is passed through the {@link #storeObject} method to the lower layer
     *
     * @param object the new object to be inserted
     * @return the success/failure error code of the object insertion
     */
    public final BucketErrorCode addObjectErrCode(LocalAbstractObject object) {
        try {
            addObject(object);
            return BucketErrorCode.OBJECT_INSERTED;
        } catch (BucketStorageException e) {
            return e.getErrorCode();
        }
    }

    /**
     * Insert several new objects into this bucket.
     * 
     * This method can be overriden if there is more efficient implementation
     * available at the storage level.
     * 
     * @param objects the collection of new objects
     * @return number of objects actually added to bucket
     * @throws BucketStorageException if there was an object that cannot be inserted into the bucket
     */
    public int addObjects(Collection<? extends LocalAbstractObject> objects) throws BucketStorageException {
        return addObjects(objects.iterator());
    }

    /**
     * Insert several new objects to this bucket.
     * 
     * This method can be overriden if there is more efficient implementation
     * available at the storage level.
     * 
     * @param objects iterator that provides the new objects to insert
     * @return number of objects actually added to bucket
     * @throws BucketStorageException if there was an object that cannot be inserted into the bucket
     */
    public int addObjects(Iterator<? extends LocalAbstractObject> objects) throws BucketStorageException {
        if (objects == null)
            return 0;
        
        // Iterate through all objects and add one by one
        int ret = 0;
        while (objects.hasNext()) {
            addObject(objects.next().getLocalAbstractObject());
            ret++;
        }
        return ret;
    }


    //****************** Deletion of objects ******************//

    /**
     * Delete object with the specified ID from this bucket.
     * 
     * @param objectID the ID of the object to delete
     * @return the object deleted from this bucket
     * @throws NoSuchElementException if there is no object with the specified ID in this bucket
     * @throws BucketStorageException if the object cannot be deleted from the bucket
     */
    public abstract LocalAbstractObject deleteObject(UniqueID objectID) throws NoSuchElementException, BucketStorageException;

    /**
     * Delete all objects from this bucket that are {@link messif.objects.LocalAbstractObject#dataEquals data-equals} to
     * the specified object. If <code>deleteLimit</code> is greater than zero, only the first <code>deleteLimit</code> 
     * data-equal objects found are deleted.
     * 
     * @param object the object to match against
     * @param deleteLimit the maximal number of deleted objects (zero means unlimited)
     * @return the number of deleted objects
     * @throws BucketStorageException if there was an object that cannot be deleted from the bucket
     */
    public abstract int deleteObject(LocalAbstractObject object, int deleteLimit) throws BucketStorageException;

    /**
     * Delete all objects from this bucket that are {@link messif.objects.LocalAbstractObject#dataEquals data-equals} to
     * the specified object.
     * 
     * @param object the object to match against
     * @return the number of deleted objects
     * @throws BucketStorageException if the object cannot be deleted from the bucket
     */
    public int deleteObject(LocalAbstractObject object) throws BucketStorageException {
        return deleteObject(object, 0);
    }

    /**
     * Delete multiple objects with specified IDs.
     * 
     * This method can be overriden if there is more efficient implementation
     * available at the storage level.
     *
     * @param objectIDs List of object IDs to be deleted
     * @param removeDeletedIDs 
     * @return list of objects that were delete from this bucket
     * @throws BucketStorageException if the object cannot be deleted from the bucket
     */
    public AbstractObjectList<LocalAbstractObject> deleteObjects(Collection<? extends UniqueID> objectIDs, boolean removeDeletedIDs) throws BucketStorageException {
        // Prepare return list
        AbstractObjectList<LocalAbstractObject> rtv = new AbstractObjectList<LocalAbstractObject>(objectIDs.size());

        // Enumerate deleted objects and delete one by one
        Iterator<? extends UniqueID> iterator = objectIDs.iterator();
        while (iterator.hasNext()) {
            try {
                rtv.add(deleteObject(iterator.next()));
                if (removeDeletedIDs)
                    iterator.remove();
            } catch (NoSuchElementException ignore) {
                // Ignore the IDs that were not found
            }
        }
        
        return rtv;
    }

    /**
     * Delete all objects from this bucket.
     * 
     * This method can be overriden if there is more efficient implementation
     * available at the storage level.
     *
     * @return the number of deleted objects
     * @throws BucketStorageException if there was an object that cannot be deleted from the bucket
     */
    public abstract int deleteAllObjects() throws BucketStorageException;


    //****************** Splitting ******************//

    /**
     * Splits this bucket according to the specified policy.
     * Objects from this bucket are examined using the policy's matcher method.
     * If the method returned 0, the object is kept in this bucket.
     * Otherwise, object is moved from this bucket to the target bucket denoted by the matcher (e.g. if matcher returns 1 object is inserted into targetBuckets.get(0), etc.).
     * The number of target buckets must match the number of partitions of the policy minus one or the <code>bucketCreator</code>
     * must be able to create additional buckets.
     * 
     * @param policy the split policy used to split this bucket
     * @param targetBuckets the list of target buckets to split the objects to
     * @param bucketCreator the bucket dispatcher to use when creating target buckets; can be <tt>null</tt> if the <code>targetBuckets</code> has enough buckets
     * @param whoStays identification of a partition whose objects stay in this bucket.
     * @return the number of objects moved
     * @throws IllegalArgumentException if there are too few target buckets
     * @throws BucketStorageException if there was a storage error during objects
     *          between buckets (including capacity overflow or underflow) during object move;
     *          <b>warning:</b> the split is interrupted and should be reinitialized
     */
    public synchronized int split(SplitPolicy policy, List<Bucket> targetBuckets, BucketDispatcher bucketCreator, int whoStays) throws IllegalArgumentException, BucketStorageException {
        // Sanity checks
        if (targetBuckets == null)
            throw new IllegalArgumentException("Target buckets for split must be set");
        if (bucketCreator == null && (targetBuckets.size() < policy.getPartitionsCount() - 1))
            throw new IllegalArgumentException("Not enough buckets for split, policy " + policy + " requires at least " + (policy.getPartitionsCount() - 1) + " buckets");
        // Fill target buckets list with nulls to match the policy's partition count
        while (targetBuckets.size() < policy.getPartitionsCount() - 1)
            targetBuckets.add(null);
        
        // Get all objects and use policy's matcher to mark the moved ones
        int count = 0;
        AbstractObjectIterator<LocalAbstractObject> iterator = getAllObjects();
        while (iterator.hasNext()) {
            LocalAbstractObject object = iterator.next();
            int partId = policy.match(object);
            // If object is subject to move
            if (partId > 0) {
                try {
                    // Add object to target bucket
                    Bucket bucket = targetBuckets.get(partId - 1);
                    if (bucket == null) // The bucket was not initialized, use bucket creator to create a new one
                        targetBuckets.set(partId - 1, bucket = bucketCreator.createBucket());
                    bucket.addObject(object);
                } catch (IndexOutOfBoundsException e) {
                    throw new IllegalArgumentException("Wrong partition ID '" + partId + "' in policy " + policy);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Can't create bucket", e);
                }
                // Remove object from this bucket (so the move is complete)
                iterator.remove(); // WARNING, if this method throws OccupationLowException, the object will be in both buckets!
                count++;
            }
        }

        // Remove unused buckets
        Iterator<Bucket> bucketIter = targetBuckets.iterator();
        while (bucketIter.hasNext())
            if (bucketIter.next() == null)
                bucketIter.remove();

        return count;
    }


    //****************** Object access ******************//

    /**
     * Retrieves an object with the specified ID from this bucket.
     *
     * @param objectID the ID of the object to retrieve
     * @return object an object with the specified ID
     * @throws NoSuchElementException if there is no object with the specified ID in this bucket
     */
    public abstract LocalAbstractObject getObject(UniqueID objectID) throws NoSuchElementException;

    /**
     * Retrieve an object with the specified locator from this bucket.
     *
     * @param locator the locator URI of the object to retrieve
     * @return object with specified locator from this bucket
     * @throws NoSuchElementException This exception is thrown if there is no object with the specified locator in this bucket
     */
    public abstract LocalAbstractObject getObject(String locator) throws NoSuchElementException;

    /**
     * Retrieve an object with the specified key from this bucket.
     *
     * @param key the key of the object to retrieve
     * @return object with specified key from this bucket
     * @throws NoSuchElementException This exception is thrown if there is no object with the specified key in this bucket
     */
    public abstract LocalAbstractObject getObject(AbstractObjectKey key) throws NoSuchElementException;

    /**
     * Returns iterator over all objects from this bucket.
     * @return iterator over all objects from this bucket
     */
    public abstract AbstractObjectIterator<LocalAbstractObject> getAllObjects();

    /**
     * Process a query operation on objects from this bucket.
     * The query operation's answer is updated with objects from this bucket
     * that satisfy the query.
     * 
     * Default implementation calls the evaluation method of the query operation.
     * Override this method if the underlying storage structure supports effective
     * evaluation of query operations.
     *
     * @param query query operation that is to be processed on this bucket
     * @return the number of objects that were added to answer
     */
    public int processQuery(QueryOperation<?> query) {
        return query.evaluate(getAllObjects());
    }

    /**
     * The iterator for provided objects for ObjectProvider interface.
     * @return iterator for provided objects
     */
    public AbstractObjectIterator<LocalAbstractObject> provideObjects() {
        return getAllObjects();
    }

}
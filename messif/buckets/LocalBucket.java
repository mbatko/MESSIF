/*
 * Bucket.java
 *
 * Created on 4. kveten 2003, 13:53
 */

package messif.buckets;

import java.io.Serializable;
import java.util.NoSuchElementException;
import messif.objects.UniqueID;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.statistics.StatisticRefCounter;

/**
 * This class represents the <tt>Bucket</tt> that is maintained locally (i.e. on the current computer).
 *
 * The local bucket maintains statistics for number of reads, inserts and deletions.
 *
 * This class provides most of the implementation using a naive approach,
 * minimal implementation of the underlying class must have at least
 * methods getObjectCount, storeObject and iterator. Getting an object and removing one is done through
 * the methods of the iterator. More effective implementations can override other methods as well.
 * For deletion, you need to override method iterator:removeInternal().
 *
 * @see messif.netbucket.RemoteBucket
 *
 * @author  xbatko
 */
public abstract class LocalBucket extends Bucket implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;    

    /****************** Statistics ******************/

    /** Number of bucket reads statistic per bucket */
    protected static final StatisticRefCounter counterBucketRead = StatisticRefCounter.getStatistics("BucketRead");
    /** Number of object inserts statistic per bucket */
    protected static final StatisticRefCounter counterBucketAddObject = StatisticRefCounter.getStatistics("BucketAddObject");
    /** Number of object deletions statistic per bucket */
    protected static final StatisticRefCounter counterBucketDelObject = StatisticRefCounter.getStatistics("BucketDelObject");


    /****************** Bucket ID ******************/

    /** Unique identifier of this bucket */
    private int bucketID = BucketDispatcher.UNASSIGNED_BUCKET_ID;

    /**
     * Returns the unique ID of this bucket.
     * @return this bucket ID
     */
    public int getBucketID() {
        return bucketID;
    }

    /**
     * Returns whether this bucket is standalone bucket or if it is maintained by a bucket dispatcher.
     * @return <tt>true</tt>
     */
    public boolean isBucketStandalone() {
        return bucketID == BucketDispatcher.UNASSIGNED_BUCKET_ID;
    }

    /**
     * Set this bucket's ID.
     * This method should be used only from the {@link BucketDispatcher}.
     * @param bucketID the new bucket ID
     */
    void setBucketID(int bucketID) { // Only settable from BucketDispatcher
        this.bucketID = bucketID;
    }


    /****************** Constructors ******************/

    /**
     * Constructs a new LocalBucket instance without any limits (everything is unlimited).
     * Occupation is counted in bytes.
     */
    protected LocalBucket() {
        this(true);
    }

    /**
     * Constructs a new LocalBucket instance without any limits (everything is unlimited).
     * Occupation count may be in bytes or number of objects.
     *
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    protected LocalBucket(boolean occupationAsBytes) {
        this(Long.MAX_VALUE, Long.MAX_VALUE, 0, occupationAsBytes);
    }

    /**
     * Constructs a new LocalBucket instance and setups all bucket limits
     *
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    protected LocalBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes) {
        // Set limits
        this.capacity = capacity;
        this.softCapacity = softCapacity;
        this.lowOccupation = lowOccupation;
        
        // Set flag
        this.occupationAsBytes = occupationAsBytes;
    }

    /**
     * Clean up bucket internals before deletion.
     * This method is called by bucket dispatcher when this bucket is removed
     * or when the bucket is garbage collected.
     * 
     * The method removes statistics for this bucket.
     * 
     * @throws Throwable if there was an error during releasing resources
     */
    @Override
    public void finalize() throws Throwable {
        // Remove statistics
        counterBucketAddObject.remove(this);
        counterBucketDelObject.remove(this);
        counterBucketRead.remove(this);

        super.finalize();
    }


    /****************** Bucket limits ******************/

    /** The maximal (hard) capacity of this bucket */
    protected final long capacity;
    /** The soft capacity of this bucket */
    protected final long softCapacity;
    /** The minimal (hard) capacity of this bucket */
    protected final long lowOccupation;

    /**
     * Returns the maximal capacity of this bucket.
     * This limit cannot be exceeded.
     *
     * @return the maximal capacity of bucket
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * Returns the soft capacity of this bucket.
     * This limit can be exceeded, but insert operations will return BucketErrorCode.SOFTCAPACITY_EXCEEDED.
     *
     * @return the soft capacity of this bucket
     */
    public long getSoftCapacity() {
        return softCapacity;
    }

    /**
     * Returns the minimal occupation of this bucket. 
     * Whenever a deletion of an object is tried, that would result in undeflow,
     * an OccupationLowException exception is thrown.
     *
     * @return the minimal occupation of this bucket
     */
    public long getLowOccupation() {
        return lowOccupation;
    }


    /****************** Bucket occupation ******************/

    /** Flag if the occupation is stored as bytes or object count */
    protected final boolean occupationAsBytes;

    /** Actual bucket occupation in either bytes or object count (see occupationAsBytes flag) */
    protected long occupation = 0;

    /**
     * Returns the current occupation of this bucket.
     * @return the current occupation of this bucket
     */
    public long getOccupation() { return occupation; }

    /**
     * Returns an occupation ratio with respect to the bucket's soft capacity, i.e. the current occupation
     * divided by the soft capacity.
     *
     * @return the occupation ratio of this bucket
     */
    public double getOccupationRatio() {
        return (double)getOccupation() / (double)getSoftCapacity();
    }

    /**
     * Returns true if the soft-capacity of the bucket has been exceeded.
     * @return true if the soft-capacity of the bucket has been exceeded or false otherwise
     */
    public boolean isSoftCapacityExceeded() {
        return occupation > softCapacity;
    }


    /****************** Bucket methods overrides ******************/

    /**
     * Insert a new object into the bucket.
     * The object is passed through the {@link #storeObject} method to the lower layer
     *
     * @param object the new object to be inserted
     * @return BucketErrorCode.SOFTCAPACITY_EXCEEDED if the soft capacity has been exceeded.
     *         BucketErrorCode.OBJECT_INSERTED       upon successful insertion
     * @throws CapacityFullException if the hard capacity of the bucket is exceeded
     */
    public synchronized BucketErrorCode addObject(LocalAbstractObject object) throws CapacityFullException {
        // Get object size either in bytes or number of objects
        long size = occupationAsBytes?object.getSize():1;
        
        if (occupation + size > capacity)
            throw new CapacityFullException();
        
        // Pass the object to the lower layer;
        BucketErrorCode rtv = storeObject(object);
        if (!rtv.equals(BucketErrorCode.OBJECT_INSERTED))
            return rtv;
        
        // Update occupation
        occupation += size;
        
        // Increase statistics
        counterBucketAddObject.add(this);
        
        if (occupation > softCapacity)
            return BucketErrorCode.SOFTCAPACITY_EXCEEDED;
        
        return BucketErrorCode.OBJECT_INSERTED;
    }

    /**
     * Delete object with specified ID from this bucket.
     *
     * The <code>remove</code> method of the underlying <code>iterator</code> over all objects is used.
     * If a more efficient implementation is available for the specific storage
     * layer, this method should be reimplemented (however, do not forget to update statistics).
     *
     * @param objectID ID of the object to delete
     * @throws NoSuchElementException This exception is thrown if there is no object with the specified ID in this bucket
     * @throws OccupationLowException This exception is throws if the low occupation limit is reached when deleting object
     * @return The object deleted from this bucket
     */
    public synchronized LocalAbstractObject deleteObject(UniqueID objectID) throws NoSuchElementException, OccupationLowException {
        LocalBucketIterator<? extends LocalBucket> iterator = iterator();
        
        // Search for the object using iterator
        iterator.getObjectByID(objectID);
        
        // Call the implementation method
        return deleteObject(iterator);
    }

    /**
     * Delete all objects from this bucket that are {@link messif.objects.LocalAbstractObject#dataEquals data-equals} to
     * the specified object. If <code>deleteLimit</code> is greater than zero, only the first <code>deleteLimit</code> 
     * data-equal objects found are deleted.
     *
     * The <code>remove</code> method of the underlying <code>iterator</code> over all objects is used.
     * If a more efficient implementation is available for the specific storage
     * layer, this method should be reimplemented (however, do not forget to update statistics).
     *
     * @param object the object to match against
     * @param deleteLimit the maximal number of deleted objects (zero means unlimited)
     * @return the number of deleted objects
     * @throws OccupationLowException if the low occupation limit is reached when deleting object
     */
    public synchronized int deleteObject(LocalAbstractObject object, int deleteLimit) throws OccupationLowException {
        LocalBucketIterator<? extends LocalBucket> iterator = iterator();

        // Search for the object using iterator
        int count = 0;
        try {
            while (iterator.hasNext() && (deleteLimit <= 0 || count < deleteLimit)) {
                iterator.getObjectByData(object);
                deleteObject(iterator);
                count++;
            }
        } catch (NoSuchElementException e) {
        }
        
        // Call the implementation method
        return count;
    }

    /**
     * Delete object to which the iterator points currently.
     *
     * @param iterator iterator that points to the deleted object (must iterate over objects from this bucket!)
     * @throws NoSuchElementException This exception is thrown if there is no current object in the iterator
     * @throws OccupationLowException This exception is throws if the low occupation limit is reached when deleting object
     * @return The object deleted from this bucket
     */
    protected synchronized LocalAbstractObject deleteObject(LocalBucketIterator<? extends LocalBucket> iterator) throws NoSuchElementException, OccupationLowException {
        // Use the current object from iterator
        LocalAbstractObject rtv = iterator.getCurrentObject();
        
        // Get object size either in bytes or number of objects
        long size = occupationAsBytes?rtv.getSize():1;

        // Test occupation
        if (occupation - size < lowOccupation)
            throw new OccupationLowException();
        
        // Remove the object (Do not call iterator.remove() unless you can enter an infinite loop!!!!!)
        iterator.removeInternal();
        
        // Update occupation
        occupation -= size;

        // Update statistics
        counterBucketDelObject.add(this);

        return rtv;
    }

    /**
     * Get an object with specified ID from this bucket.
     *
     * The underlying <code>iterator</code> method is used and the object with the specified ID is returned.
     * If a more efficient implementation is available for the specific storage
     * layer, this method should be reimplemented (however, do not forget to update statistics).
     *
     * @param objectID ID of the object to retrieve
     * @return object with specified ID from this bucket
     * @throws NoSuchElementException This exception is thrown if there is no object with the specified ID in this bucket
     */
    public synchronized LocalAbstractObject getObject(UniqueID objectID) throws NoSuchElementException {
        // Get object using iterator - it will throw NoSuchElement exception if the object was not found
        LocalAbstractObject rtv = iterator().getObjectByID(objectID);

        // Update statistics
        counterBucketRead.add(this);

        return rtv;
    }

    /**
     * Retrieve an object with the specified locator from this bucket.
     *
     * The underlying <code>iterator</code> method is used and the object with the specified locator is returned.
     * If a more efficient implementation is available for the specific storage
     * layer, this method should be reimplemented (however, do not forget to update statistics).
     *
     * @param locator the locator URI of the object to retrieve
     * @return object with specified locator from this bucket
     * @throws NoSuchElementException This exception is thrown if there is no object with the specified locator in this bucket
     */
    public synchronized LocalAbstractObject getObject(String locator) throws NoSuchElementException {
        // Get object using iterator - it will throw NoSuchElement exception if the object was not found
        LocalAbstractObject rtv = iterator().getObjectByLocator(locator);

        // Update statistics
        counterBucketRead.add(this);

        return rtv;
    }

    /**
     * Returns iterator over all objects from this bucket.
     * @return iterator over all objects from this bucket
     */
    public synchronized AbstractObjectIterator<LocalAbstractObject> getAllObjects() {
        // Update statistics
        counterBucketRead.add(this);
        
        return iterator();
    }


    /****************** Bucket lower implementation overrides ******************/

    /**
     * Returns current number of objects stored in this bucket.
     * @return current number of objects stored in this bucket
     */
    public abstract int getObjectCount();

    /**
     * Stores an object in a physical storage.
     * It should return OBJECT_INSERTED value if the object was successfuly inserted.
     *
     * @param object the new object to be inserted
     * @return error code - for details, see documentation of {@link BucketErrorCode}
     */
    protected abstract BucketErrorCode storeObject(LocalAbstractObject object);

    /**
     * Returns iterator through all the objects in this bucket.
     * @return iterator through all the objects in this bucket
     */
    protected abstract LocalBucketIterator<? extends LocalBucket> iterator();

    /**
     * Internal class for bucket iterator implementation
     * @param <T> the type of the bucket this iterator operates on
     */
    protected static abstract class LocalBucketIterator<T extends LocalBucket> extends AbstractObjectIterator<LocalAbstractObject> {
        /** Reference to the bucket this iterator is working on */
        protected final T bucket;

        /**
         * Constructs a new LocalBucketIterator instance that iterates over objects in the specified bucket
         * @param bucket the bucket this iterator is working on
         */
        protected LocalBucketIterator(T bucket) {
           this.bucket = bucket;
        }

        /**
         * Removes from the underlying bucket the last element returned by this
         * iterator. This method can be called only once per call to <tt>next</tt>.
         */
        public final void remove() {
            try {
                bucket.deleteObject(this);
            } catch (OccupationLowException e) {
                throw new UnsupportedOperationException(e);
            } catch (FilterRejectException e) {
                throw new UnsupportedOperationException(e);
            }
        }
 
        /**
         * This method physically removes current element.
         * It is defined because all statistics and internal attributes associated with the bucket must be maintained.
         * It is done by calling deleteObject() in iterator's remove(). The bucket's method deleteObject() then calls 
         * directly this method.
         * 
         * As a result remove() should not be overriden in iterators of specialized bucket class. Instead, removeInternal() 
         * is the correct way.
         */
        protected abstract void removeInternal();
    }    


    /****************** String representation ******************/

    /**
     * Returns a string representation of this bucket.
     * @return a string representation of this bucket
     */
    @Override
    public String toString() {
        String sID = new java.text.DecimalFormat("0000").format(getBucketID()); // Number <10000 gets formated with leading zeros, number >=10000 are printed as is.
        return "LocalBucket:" + sID;
    }

}

/*
 * LocalBucket
 *
 */

package messif.buckets;

import java.io.Serializable;
import java.util.NoSuchElementException;
import messif.buckets.index.Index;
import messif.buckets.index.LocalAbstractObjectOrder;
import messif.buckets.index.ModifiableIndex;
import messif.buckets.index.ModifiableSearch;
import messif.buckets.index.Search;
import messif.objects.UniqueID;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.statistics.StatisticRefCounter;
import messif.utility.Convert;

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
    private static final long serialVersionUID = 2L;    

    //****************** Statistics ******************//

    /** Number of bucket reads statistic per bucket */
    protected static final StatisticRefCounter counterBucketRead = StatisticRefCounter.getStatistics("BucketRead");
    /** Number of object inserts statistic per bucket */
    protected static final StatisticRefCounter counterBucketAddObject = StatisticRefCounter.getStatistics("BucketAddObject");
    /** Number of object deletions statistic per bucket */
    protected static final StatisticRefCounter counterBucketDelObject = StatisticRefCounter.getStatistics("BucketDelObject");


    //****************** Attributes ******************//

    /** Unique identifier of this bucket */
    private int bucketID = BucketDispatcher.UNASSIGNED_BUCKET_ID;

    /** The maximal (hard) capacity of this bucket */
    private final long capacity;
    /** The soft capacity of this bucket */
    private final long softCapacity;
    /** The minimal (hard) capacity of this bucket */
    private final long lowOccupation;
    /** Flag if the occupation is stored as bytes or object count */
    private final boolean occupationAsBytes;
    /** Actual bucket occupation in either bytes or object count (see occupationAsBytes flag) */
    private long occupation = 0;


    //****************** Filter attributes ******************//

    /** List of registered "before add" filters */
    private BucketFilterBeforeAdd[] beforeAddFilters = null;
    /** List of registered "after add" filters */
    private BucketFilterAfterAdd[] afterAddFilters = null;
    /** List of registered "before remove" filters */
    private BucketFilterBeforeRemove[] beforeRemoveFilters = null;
    /** List of registered "after remove" filters */
    private BucketFilterAfterRemove[] afterRemoveFilters = null;


    //****************** Constructors ******************//

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


    //****************** Bucket ID methods ******************//

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


    //****************** Bucket limit methods ******************//

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


    //****************** Bucket occupation ******************//

    /**
     * Returns the current occupation of this bucket.
     * @return the current occupation of this bucket
     */
    public long getOccupation() {
        return occupation;
    }

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

    /**
     * Returns current number of objects stored in this bucket.
     * @return current number of objects stored in this bucket
     */
    public int getObjectCount() {
        return getModifiableIndex().size();
    }


    //****************** Index access ******************//

    /**
     * Returns the index (including storage) for this bucket.
     * The index provides the access to the underlying storage of objects in this bucket.
     * @return the index for this bucket
     */
    protected abstract ModifiableIndex<LocalAbstractObject> getModifiableIndex();

    /**
     * Returns the index defined on this bucket that can be used for searching.
     * @return the index for this bucket
     */
    public Index<LocalAbstractObject> getIndex() {
        // Update statistics
        counterBucketRead.add(this);
        
        return getModifiableIndex();
    }


    //****************** Filters ******************//

    /**
     * Append a new filter to the filter chain.
     * @param filter the new filter to append
     */
    public synchronized void registerFilter(BucketFilter filter) {
        if (filter instanceof BucketFilterBeforeAdd)
            beforeAddFilters = Convert.addToArray(beforeAddFilters, BucketFilterBeforeAdd.class, (BucketFilterBeforeAdd)filter);
        if (filter instanceof BucketFilterAfterAdd)
            afterAddFilters = Convert.addToArray(afterAddFilters, BucketFilterAfterAdd.class, (BucketFilterAfterAdd)filter);
        if (filter instanceof BucketFilterBeforeRemove)
            beforeRemoveFilters = Convert.addToArray(beforeRemoveFilters, BucketFilterBeforeRemove.class, (BucketFilterBeforeRemove)filter);
        if (filter instanceof BucketFilterAfterRemove)
            afterRemoveFilters = Convert.addToArray(afterRemoveFilters, BucketFilterAfterRemove.class, (BucketFilterAfterRemove)filter);
    }
    
    /**
     * Remove a filter from the filter chain
     * @param filter the filter to remove
     */
    public synchronized void deregisterFilter(BucketFilter filter) {
        if (filter instanceof BucketFilterBeforeAdd)
            beforeAddFilters = Convert.removeFromArray(beforeAddFilters, (BucketFilterBeforeAdd)filter);
        if (filter instanceof BucketFilterAfterAdd)
            afterAddFilters = Convert.removeFromArray(afterAddFilters, (BucketFilterAfterAdd)filter);
        if (filter instanceof BucketFilterBeforeRemove)
            beforeRemoveFilters = Convert.removeFromArray(beforeRemoveFilters, (BucketFilterBeforeRemove)filter);
        if (filter instanceof BucketFilterAfterRemove)
            afterRemoveFilters = Convert.removeFromArray(afterRemoveFilters, (BucketFilterAfterRemove)filter);
    }

    /**
     * Returns the first registered filter that has the specified class
     * @param <T> the class of the filter
     * @param filterClass filter class to search for
     * @throws NoSuchElementException if there was no filter with the specified class
     * @return the first registered filter that has the specified class
     */
    public synchronized <T extends BucketFilter> T getFilter(Class<T> filterClass) throws NoSuchElementException {
        for (BucketFilter filter : beforeAddFilters)
            if (filterClass.isInstance(filter))
                return filterClass.cast(filter);
        for (BucketFilter filter : afterAddFilters)
            if (filterClass.isInstance(filter))
                return filterClass.cast(filter);
        for (BucketFilter filter : beforeRemoveFilters)
            if (filterClass.isInstance(filter))
                return filterClass.cast(filter);
        for (BucketFilter filter : afterRemoveFilters)
            if (filterClass.isInstance(filter))
                return filterClass.cast(filter);
        
        throw new NoSuchElementException("Filter with specified class not found");
    }


    //****************** Internal state updating methods ******************//

    /**
     * Check if the object <code>object</code> can added to this bucket.
     * @param object the object to add
     * @param addible the {@link Addible} that actually stores the object
     * @throws BucketStorageException if there was an error adding the object
     */
    protected synchronized void addObject(LocalAbstractObject object, Addible<LocalAbstractObject> addible) throws BucketStorageException {
        // Execute before add filters
        if (beforeAddFilters != null)
            for (BucketFilterBeforeAdd filter : beforeAddFilters)
                filter.filterBeforeAdd(object, this);

        // Get object size either in bytes or number of objects
        long size = occupationAsBytes?object.getSize():1;
        
        if (occupation + size > capacity)
            throw new CapacityFullException();

        // Pass the object to the lower layer for inserting
        addible.add(object);
        
        // Update occupation
        occupation += size;

        // Increase statistics
        counterBucketAddObject.add(this);

        // Execute after add filters
        if (afterAddFilters != null)
            for (BucketFilterAfterAdd filter : afterAddFilters)
                filter.filterAfterAdd(object, this);
    }

    /**
     * Check if the <code>object</code> can be deleted from this bucket.
     * This includes the pre-checks of the filters and also the low-occupation check.
     * 
     * @param removable the object that is going to be removed
     * @throws BucketStorageException if the object cannot be removed (reason is stored in the exception)
     */
    protected synchronized void deleteObject(Removable<LocalAbstractObject> removable) throws BucketStorageException {
        // Execute before remove filters
        if (beforeRemoveFilters != null)
            for (BucketFilterBeforeRemove filter : beforeRemoveFilters)
                filter.filterBeforeRemove(removable.getCurrentObject(), this);

        // Get object size either in bytes or number of objects
        long size = occupationAsBytes?removable.getCurrentObject().getSize():1;

        // Test occupation
        if (occupation - size < lowOccupation)
            throw new OccupationLowException();

        // Call the lower layer removal
        removable.remove();

        // Update occupation
        occupation -= size;

        // Update statistics
        counterBucketDelObject.add(this);

        // Execute after add filters
        if (afterRemoveFilters != null)
            for (BucketFilterAfterRemove filter : afterRemoveFilters)
                filter.filterAfterRemove(removable.getCurrentObject(), this);
    }

    //****************** Bucket methods overrides ******************//

    public void addObject(LocalAbstractObject object) throws BucketStorageException {
        addObject(object, getModifiableIndex());
    }

    public synchronized LocalAbstractObject deleteObject(UniqueID objectID) throws NoSuchElementException, BucketStorageException {
        // Search for objects with the specified ID
        ModifiableSearch<UniqueID, LocalAbstractObject> search = getModifiableIndex().search(LocalAbstractObjectOrder.uniqueIDComparator, objectID, true);

        // If object is found, delete and return it
        if (!search.next())
            throw new NoSuchElementException("There is no object with ID: " + objectID);
        deleteObject(search);
        return search.getCurrentObject();
    }

   public synchronized int deleteObject(LocalAbstractObject object, int deleteLimit) throws BucketStorageException {
        // Search for object with the same data
        ModifiableSearch<LocalAbstractObject, LocalAbstractObject> search = getModifiableIndex().search(LocalAbstractObjectOrder.DATA, object, true);

        int count = 0;
        try {
            // If there is another object found by the search
            while ((deleteLimit <= 0 || count < deleteLimit) && search.next()) {
                // Delete it
                deleteObject(search);
                count++;
            }
        } catch (NoSuchElementException ignore) {
            // The iterator's getObjectByData has thrown NoSuchElementException to indicate end-of-search
        }
        
        // Call the implementation method
        return count;
    }

    public int deleteAllObjects() throws BucketStorageException {
        ModifiableSearch<?, LocalAbstractObject> search = getModifiableIndex().search();
        int count = 0;
        while (search.next()) {
            deleteObject(search);
            count++;
        }
        return count;
    }

    public synchronized LocalAbstractObject getObject(UniqueID objectID) throws NoSuchElementException {
        // Search for objects with the specified ID
        ModifiableSearch<UniqueID, LocalAbstractObject> search = getModifiableIndex().search(LocalAbstractObjectOrder.uniqueIDComparator, objectID, true);

        // If object is found, delete and return it
        if (!search.next())
            throw new NoSuchElementException("There is no object with ID: " + objectID);

        // Update statistics
        counterBucketRead.add(this);

        return search.getCurrentObject();
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
        // Search for objects with the specified ID
        Search<String, LocalAbstractObject> search = getModifiableIndex().search(LocalAbstractObjectOrder.locatorToLocalObjectComparator, locator, true);

        // If object is found, delete and return it
        if (!search.next())
            throw new NoSuchElementException("There is no object with locator: " + locator);

        // Update statistics
        counterBucketRead.add(this);

        return search.getCurrentObject();
    }

    public AbstractObjectIterator<LocalAbstractObject> getAllObjects() {
        // Update statistics
        counterBucketRead.add(this);

        final ModifiableSearch<?, LocalAbstractObject> search = getModifiableIndex().search();
        return new AbstractObjectIterator<LocalAbstractObject>() {
            private int hasNext = -1;

            @Override
            public LocalAbstractObject getCurrentObject() {
                return search.getCurrentObject();
            }

            public boolean hasNext() {
                // If the next was called, thus hasNext is not decided yet
                if (hasNext == -1)
                    hasNext = search.next()?1:0; // Perform search

                return hasNext == 1;
            }

            public LocalAbstractObject next() {
                if (!hasNext())
                    throw new NoSuchElementException("There are no more objects");
                hasNext = -1;
                return search.getCurrentObject();
            }

            public void remove() {
                try {
                    deleteObject(search);
                } catch (BucketStorageException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }


    //****************** String representation ******************//

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

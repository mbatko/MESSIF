/*
 * LocalFilteredlBucket.java
 *
 * Created on 4. kveten 2003, 13:53
 */

package messif.buckets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import messif.objects.LocalAbstractObject;


/**
 *  This class enriches the basic LocalBucket with filters for adding and deleting objects.
 *
 * @author  xbatko
 */
public abstract class LocalFilteredBucket extends LocalBucket implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;
   
    /****************** Constructors ******************/

    /**
     * Constructs a new LocalFilteredBucket instance
     *  Limits and the ID for the statistics can be provided
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    protected LocalFilteredBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
    }
    
    
    /****************** Filters ******************/
    
    /** List of registered filters */
    private final List<BucketFilterInterface> registeredFilters = Collections.synchronizedList(new ArrayList<BucketFilterInterface>());
    
    
    /**
     * Append a new filter to the filter chain
     * @param filter the new filter to append
     */
    public synchronized void registerFilter(BucketFilterInterface filter) {
        registeredFilters.add(filter); 
    }
    
    /**
     * Remove a filter from the filter chain
     * @param filter the filter to remove
     * @return <tt>true</tt> if the specified filter was registered
     */
    public synchronized boolean deregisterFilter(BucketFilterInterface filter) {
        return registeredFilters.remove(filter);
    }

    /**
     * Returns the number of currently registered filters
     * @return the number of currently registered filters
     */
    public int getFilterCount() {
        return registeredFilters.size();
    }
    
    /**
     * Returns the filter on the specified position
     * @param index the position of the filter to get (zero-based)
     * @throws IndexOutOfBoundsException if the index is out of range
     * @return the filter on the specified position
     */
    public BucketFilterInterface getFilter(int index) throws IndexOutOfBoundsException {
        return registeredFilters.get(index);
    }
    
    /**
     * Returns the first registered filter that has the specified class
     * @param filterClass filter class to search for
     * @throws NoSuchElementException if there was no filter with the specified class
     * @return the first registered filter that has the specified class
     */
    public <T extends BucketFilterInterface> T getFilter(Class<T> filterClass) throws NoSuchElementException {
        synchronized (registeredFilters) {
            for (BucketFilterInterface filter : registeredFilters) {
                if (filterClass.isInstance(filter))
                    return (T)filter; // This cast IS checked in the previous line
            }
        }
        
        throw new NoSuchElementException("Filter with specified class not found");
    }
    
    
    /****************** Overrides for filtering ******************/

    /**
     * Insert a new object into the bucket.
     * This method goes through all registered filters and use their filter method
     * with BEFORE_ADD and AFTER_ADD situations (see {@link BucketFilterInterface.FilterSituations}).
     * 
     * @param object the new object to be inserted
     * @return BucketErrorCode.SOFTCAPACITY_EXCEEDED if the soft capacity has been exceeded.
     *         BucketErrorCode.OBJECT_INSERTED       upon successful insertion
     * @throws CapacityFullException if the hard capacity of the bucket is exceeded
     * @throws FilterRejectException if any associated filter aborts the insertion by throwing the exception
     */
    public synchronized BucketErrorCode addObject(LocalAbstractObject object) throws CapacityFullException, FilterRejectException {
        filterAddObjectBefore(object);
        BucketErrorCode rtv = super.addObject(object);
        filterAddObjectAfter(object);
        
        return rtv;
    }

    /**
     * Processes all filters with BEFORE_ADD situation.
     * @param object the object to process
     */
    protected final void filterAddObjectBefore(LocalAbstractObject object) {
        synchronized (registeredFilters) {
            for (BucketFilterInterface filter : registeredFilters)
                filter.filterObject(object, BucketFilterInterface.FilterSituations.BEFORE_ADD, this);
        }
    }


    /**
     * Processes all filters with AFTER_ADD situation.
     * @param object the object to process
     */
    protected final void filterAddObjectAfter(LocalAbstractObject object) {
        synchronized (registeredFilters) {
            for (BucketFilterInterface filter : registeredFilters)
                filter.filterObject(object, BucketFilterInterface.FilterSituations.AFTER_ADD, this);
        }
    }

    /**
     * Delete object to which the iterator points currently.
     * This method goes through all registered filters and use their filter method
     * with BEFORE_DEL and AFTER_DEL situations (see {@link BucketFilterInterface.FilterSituations}).
     * 
     * 
     * @return The object deleted from this bucket
     * @param iterator iterator that points to the deleted object (must iterate over objects from this bucket!)
     * @throws NoSuchElementException This exception is thrown if there is no current object in the iterator
     * @throws OccupationLowException This exception is throws if the low occupation limit is reached when deleting object
     * @throws FilterRejectException if any associated filter aborts the deletion by throwing the exception
     */
    protected synchronized LocalAbstractObject deleteObject(LocalBucketIterator<? extends LocalBucket> iterator) throws NoSuchElementException, OccupationLowException, FilterRejectException {
        LocalAbstractObject object = iterator.getCurrentObject();
        
        filterDeleteObjectBefore(object);
        super.deleteObject(iterator);
        filterDeleteObjectAfter(object);

        return object;
    }

    /**
     * Processes all filters with BEFORE_DEL situation.
     * @param object the object to process
     */
    protected final void filterDeleteObjectBefore(LocalAbstractObject object) {
        synchronized (registeredFilters) {
            for (BucketFilterInterface filter : registeredFilters)
                filter.filterObject(object, BucketFilterInterface.FilterSituations.BEFORE_DEL, this);
        }
    }

    /**
     * Processes all filters with AFTER_DEL situation.
     * @param object the object to process
     */
    protected final void filterDeleteObjectAfter(LocalAbstractObject object) {
        synchronized (registeredFilters) {
            for (BucketFilterInterface filter : registeredFilters)
                filter.filterObject(object, BucketFilterInterface.FilterSituations.AFTER_DEL, this);
        }
    }

    
}

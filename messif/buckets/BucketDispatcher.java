/*
 * BucketDispatcher.java
 *
 * Created on 4. kveten 2003, 14:11
 */

package messif.buckets;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import messif.buckets.impl.MemoryStorageBucket;
import messif.pivotselection.AbstractPivotChooser;
import messif.utility.Convert;
import messif.utility.Logger;


/**
 * This class is a dispatcher for maintaining a set of local buckets.
 *
 * Kept buckets can be accessed using unique bucket identifiers (BIDs).
 *
 * New buckets can be created using the {@link #createBucket} method - the unique ID is assigned automatically.
 * To create a bucket, a specific bucket implementation class, capacity settings and additional class-specific parameters are needed.
 * They are either passed to the {@link #createBucket createBucket} method, or the dispatcher's default values are used.
 * Automatic pivot choosers can be created for new buckets - see {@link #setAutoPivotChooser}.
 *
 * To remove a bucket from the dispatcher, use {@link #removeBucket}. Note that
 * objects remain inside the bucket, just the bucket will be no longer maintained
 * by this dispatcher.
 *
 * @see LocalBucket
 *
 * @author  xbatko
 */
public class BucketDispatcher implements Serializable {
    /** class serial id for serialization */
    private static final long serialVersionUID = 2L;

    /** Logger for the bucket dispatcher */
    private static Logger log = Logger.getLoggerEx("messif.buckets");

    //****************** Bucket dispatcher data ******************//

    /** The buckets maintained by this dispatcher organized in hashtable with bucket IDs as keys */
    private Map<Integer,LocalBucket> buckets = new HashMap<Integer,LocalBucket>();

    /** Maximal number of buckets maintained by this dispatcher */
    private final int maxBuckets;

    /** Automatic bucket ID generator */
    private final AtomicInteger nextBucketID = new AtomicInteger(1);

    /** The ID of buckets that do not belong to a dispatcher */
    public static final int UNASSIGNED_BUCKET_ID = 0;

    /** Default bucket hard capacity for newly created buckets */
    protected final long bucketCapacity;

    /** Default bucket soft capacity for newly created buckets */
    protected final long bucketSoftCapacity;

    /** Default bucket hard low-occupation for newly created buckets */
    protected final long bucketLowOccupation;

    /** Default flag whether to store occupation & capacity in bytes (<tt>true</tt>) or number of objects (<tt>false</tt>) for newly created buckets */
    protected final boolean bucketOccupationAsBytes;

    /** Default class for newly created buckets */
    protected final Class<? extends LocalBucket> defaultBucketClass;

    /** Default parameters for newly created buckets with default bucket class */
    protected final Map<String, Object> defaultBucketClassParams;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of BucketDispatcher with full specification of default values.
     *
     * @param maxBuckets the maximal number of buckets maintained by this dispatcher
     * @param bucketCapacity the default bucket hard capacity for newly created buckets
     * @param bucketSoftCapacity the default bucket soft capacity for newly created buckets
     * @param bucketLowOccupation the default bucket hard low-occupation for newly created buckets
     * @param bucketOccupationAsBytes the default flag whether to store occupation & capacity in bytes (<tt>true</tt>) or number of objects (<tt>false</tt>) for newly create buckets
     * @param defaultBucketClass the default class for newly created buckets
     * @param defaultBucketClassParams the default parameters for newly created buckets with default bucket class
     */
    public BucketDispatcher(int maxBuckets, long bucketCapacity, long bucketSoftCapacity, long bucketLowOccupation, boolean bucketOccupationAsBytes, Class<? extends LocalBucket> defaultBucketClass, Map<String, Object> defaultBucketClassParams) {
        this.maxBuckets = maxBuckets;
        this.bucketCapacity = bucketCapacity;
        this.bucketSoftCapacity = bucketSoftCapacity;
        this.bucketLowOccupation = bucketLowOccupation;
        this.bucketOccupationAsBytes = bucketOccupationAsBytes;
        this.defaultBucketClass = defaultBucketClass;
        this.defaultBucketClassParams = defaultBucketClassParams;
    }

    /**
     * Creates a new instance of BucketDispatcher with full specification of default values.
     * No additional parameters for the default bucket class is specified.
     *
     * @param maxBuckets the maximal number of buckets maintained by this dispatcher
     * @param bucketCapacity the default bucket hard capacity for newly created buckets
     * @param bucketSoftCapacity the default bucket soft capacity for newly created buckets
     * @param bucketLowOccupation the default bucket hard low-occupation for newly created buckets
     * @param bucketOccupationAsBytes the default flag whether to store occupation & capacity in bytes (<tt>true</tt>) or number of objects (<tt>false</tt>) for newly create buckets
     * @param defaultBucketClass the default class for newly created buckets
     */
    public BucketDispatcher(int maxBuckets, long bucketCapacity, long bucketSoftCapacity, long bucketLowOccupation, boolean bucketOccupationAsBytes, Class<? extends LocalBucket> defaultBucketClass) {
        this(maxBuckets, bucketCapacity, bucketSoftCapacity, bucketLowOccupation, bucketOccupationAsBytes, defaultBucketClass, null);
    }

    /**
     * Creates a new instance of BucketDispatcher only with maximal capacity specification.
     * The soft capacity and low-occupation limits are not set. The occupation and capacity
     * is counted in bytes. The {@link MemoryStorageBucket} is used as default bucket class.
     *
     * @param maxBuckets the maximal number of buckets maintained by this dispatcher
     * @param bucketCapacity the default bucket hard capacity for newly created buckets
     */
    public BucketDispatcher(int maxBuckets, long bucketCapacity) {
        this(maxBuckets, bucketCapacity, bucketCapacity, 0, true, MemoryStorageBucket.class);
    }

    /**
     * Clean statistics from all buckets.
     * @throws java.lang.Throwable if there is an error durnig finalizing
     */
    @Override
    public void finalize() throws Throwable {
        for (LocalBucket bucket : getAllBuckets())
            bucket.finalize();
        super.finalize();
    }


    //****************** Automatic pivot choosers ******************//

    /** The class of pivot chooser that is automatically created for newly created buckets */
    protected Class<? extends AbstractPivotChooser> autoPivotChooserClass = null;

    /** The pivot chooser instance that chooses pivots for all the buckets in this dispatcher */
    protected AbstractPivotChooser autoPivotChooserInstance = null;

    /** The hash table of pivot choosers that are assigned to buckets of this dispatcher */
    protected final Map<LocalBucket, AbstractPivotChooser> createdPivotChoosers = Collections.synchronizedMap(new HashMap<LocalBucket, AbstractPivotChooser>());

    /**
     * Returns pivot chooser that was automatically created for a bucket of this dispatcher.
     *
     * @param bucketID the ID of the bucket for which to get the pivot chooser
     * @return pivot chooser that was automatically created for the specified bucket
     */
    public AbstractPivotChooser getAutoPivotChooser(int bucketID) {
        return createdPivotChoosers.get(getBucket(bucketID));
    }

    /**
     * Set the class of pivot chooser that will be created whenever a bucket is created by this dispatcher.
     * @param autoPivotChooserClass the class of the pivot chooser to create
     */
    public void setAutoPivotChooser(Class<? extends AbstractPivotChooser> autoPivotChooserClass) {
        synchronized (createdPivotChoosers) {
            this.autoPivotChooserClass = autoPivotChooserClass;
            this.autoPivotChooserInstance = null;
        }
    }

    /**
     * Set the pivot chooser instance that chooses pivots for all the buckets in this dispatcher.
     * @param autoPivotChooserInstance the pivot chooser instance
     */
    public void setAutoPivotChooser(AbstractPivotChooser autoPivotChooserInstance) {
        synchronized (createdPivotChoosers) {
            this.autoPivotChooserInstance = autoPivotChooserInstance;
            this.autoPivotChooserClass = null;
        }
    }

    /**
     * Returns the class of the pivot chooser that is currently used for buckets in this dispatcher.
     * @return the class of the pivot chooser that is currently used for buckets in this dispatcher
     */
    public Class<? extends AbstractPivotChooser> getAutoPivotChooserClass() {
        synchronized (createdPivotChoosers) {
            if (autoPivotChooserInstance != null)
                return autoPivotChooserInstance.getClass();
            return autoPivotChooserClass;
        }
    }

    /**
     * Creates a new pivot chooser for the provided bucket.
     * If the class for autoPivotChooser was specified, a new instance is created, otherwise
     * the pivot chooser instance for the whole dispatcher is used.
     * The specified bucket is registered as sample set provider for the pivot chooser
     * The bucket is also associated with the chooser, so that the {@link #getAutoPivotChooser} will
     * return it for the bucket.
     *
     * @param bucket the bucket for which to create pivot chooser
     * @return the newly created pivot chooser or the whole dispatcher's pivot chooser instance; <tt>null</tt> is returned
     *         if either the class/instance of autoPivotChooser is not specified in this dispatcher or there was an error creating
     *         a new pivot chooser
     */
    protected AbstractPivotChooser createAutoPivotChooser(LocalBucket bucket) {
        synchronized (createdPivotChoosers) {
            AbstractPivotChooser rtv;
            
            // Create new instance of auto pivot chooser class
            if (autoPivotChooserClass != null) {
                try {
                    try {
                        // Try the pivot chooser constructor with bucket argument
                        rtv = Convert.createInstanceWithInheritableArgs(autoPivotChooserClass, bucket);
                    } catch (NoSuchMethodException e) {
                        // Constructor with bucket parameter not found, try no-param constructor
                        rtv = autoPivotChooserClass.getConstructor().newInstance();
                    }
                } catch (Exception e) {
                    log.warning("Can't create automatic pivot chooser " + autoPivotChooserClass.toString() + ": " + e.toString());
                    return null;
                }
            } else if (autoPivotChooserInstance != null) {
                // Use existing instance of pivot chooser (one for all buckets in this dispatcher)
                rtv = autoPivotChooserInstance;
            } else return null;
            
            // Register the new bucket as sample provider for the pivot chooser
            rtv.registerSampleProvider(bucket);

            // Register the pivot chooser as filter if it implements BucketFilterInterface
            if (rtv instanceof BucketFilterInterface && bucket instanceof LocalFilteredBucket)
                ((LocalFilteredBucket)bucket).registerFilter((BucketFilterInterface)rtv);
            
            // Assiciate the pivot chooser with the bucket
            createdPivotChoosers.put(bucket, rtv);
            
            return rtv;   
        }
    }


    //****************** Automatic filter creation ******************//

    /** The class of filter that is automatically created for newly created buckets */
    protected Class<? extends BucketFilterInterface> autoFilterClass = null;

    /** Parameters for the automatic filter constructors */
    protected Object[] autoFilterParams = {};

    /**
     * Set a class of filter that will be instantiated whenever a bucket is created by this dispatcher.
     * 
     * @param autoFilterClass the class of the filter to create
     * @param autoFilterParams the parameters for the filter's constructor
     */
    public void setAutoFilterClass(Class<? extends BucketFilterInterface> autoFilterClass, Object... autoFilterParams) {
        this.autoFilterClass = autoFilterClass;
        this.autoFilterParams = (autoFilterParams == null)?new Object[0]:autoFilterParams;
    }

    /**
     * Returns the class of the filter that is automatically created for new buckets.
     * @return the class of the filter that is automatically created for new buckets
     */
    public Class<? extends BucketFilterInterface> getAutoFilterClass() {
        return autoFilterClass;
    }

    /**
     * Returns the parameter for the constructor of the filter that is automatically created for new buckets.
     * Modification of this array doesn't apply to current dispatcher's settings. Use {@link #setAutoFilterClass} method instead.
     *
     * @return the parameter for the constructor of the filter that is automatically created for new buckets
     */
    public Object[] getAutoFilterParams() {
        return autoFilterParams.clone();
    }

    /**
     * Creates a new filter for the provided bucket.
     * The filter is only created if autoFilterClass was specified.
     * Filter is then registered to the bucket and returned. No additional
     * handling is done by this dispatcher, filter removal (and so on) is
     * the responsibility of the bucket itself.
     *
     * @param bucket the bucket for which to create filter
     * @return the newly created filter; <tt>null</tt> is returned
     *         if either the class/instance of autoFilterClass is not specified in this dispatcher or there was an error creating
     *         a new filter
     */
    protected BucketFilterInterface createAutoFilter(LocalBucket bucket) {
        if (!(bucket instanceof LocalFilteredBucket) || autoFilterClass == null)
            return null;
        
        // Create new instance of auto filter class
        BucketFilterInterface rtv;
        try {
            rtv = Convert.createInstanceWithInheritableArgs(autoFilterClass, autoFilterParams);
        } catch (Exception e) {
            log.warning("Can't create automatic filter " + autoFilterClass.toString() + ": " + e.toString());
            return null;
        }

        // Register the filter with the bucket
        ((LocalFilteredBucket)bucket).registerFilter(rtv);

        return rtv;
    }


    //****************** Info Methods *******************//

    /**
     * Returns the default hard capactity limit for new buckets.
     * @return the default hard capactity limit for new buckets
     */
    public long getBucketCapacity() {
        return bucketCapacity;
    }

    /**
     * Returns the default soft capactity limit for new buckets.
     * @return the default soft capactity limit for new buckets
     */
    public long getBucketSoftCapacity() {
        return bucketSoftCapacity;
    }

    /**
     * Returns the default hard low-occupation capactity limit for new buckets.
     * @return the default hard low-occupation capactity limit for new buckets
     */
    public long getBucketLowOccupation() {
        return bucketLowOccupation;
    }

    /**
     * Returns the default flag whether to compute occupation & capacity in bytes (<tt>true</tt>)
     * or number of objects (<tt>false</tt>) for new buckets.
     * @return <tt>true</tt> if the default is to compute occupation & capacity in bytes or <tt>false</tt> if it is computed in number of objects
     */
    public boolean getBucketOccupationAsBytes() {
        return bucketOccupationAsBytes;
    }

    /**
     * Returns the default class for newly created buckets.
     * @return the default class for newly created buckets
     */
    public Class<? extends LocalBucket> getDefaultBucketClass() {
        return defaultBucketClass;
    }

    /**
     * Returns the default parameters for newly created buckets with default bucket class.
     * @return the default parameters for newly created buckets with default bucket class
     */
    public Map<String, Object> getDefaultBucketClassParams() {
        return Collections.unmodifiableMap(defaultBucketClassParams);
    }

    /**
     * Returns the actual number of buckets maintained by this dispatcher.
     * @return the dispatcher's bucket count
     */
    public int getBucketCount() {
        return buckets.size();
    }

    /**
     * Returns number of buckets that exceed their soft-capacities.
     * @return number of buckets that exceed their soft-capacities
     */
    public int getOverloadedBucketCount() {
        int cnt = 0;
        for (LocalBucket b : buckets.values()) {
            if (b.isSoftCapacityExceeded())
                ++cnt;
        }
        return cnt;
    }

    /**
     * Returns the sum of occupations of all buckets maintained by this dispatcher.
     * @return the sum of occupations of all buckets
     */
    public long getOccupation() {
        long retVal = 0;
        for (LocalBucket bucket:buckets.values())
            retVal += bucket.getOccupation();
        return retVal;
    }

    /**
     * Returns the sum of object counts stored in all buckets maintained by this dispatcher.
     * @return the sum of object counts stored in all buckets
     */
    public int getObjectCount() {
        int retVal = 0;
        for (LocalBucket bucket:buckets.values())
            retVal += bucket.getObjectCount();
        return retVal;
    }


    //****************** Bucket access ******************//

    /**
     * Returns the set of bucket IDs maintaned by this dispatcher.
     * @return the set of bucket IDs maintaned by this dispatcher
     */
    public synchronized Set<Integer> getAllBucketIDs() {      
        return Collections.unmodifiableSet(buckets.keySet());
    }

    /**
     * Returns the collection of all buckets maintained by this dispatcher.
     * @return the collection of all buckets maintained by this dispatcher
     */
    public synchronized Collection<LocalBucket> getAllBuckets() {
        return Collections.unmodifiableCollection(buckets.values());
    }

    /**
     * Returns the bucket with the specified ID.
     *
     * @param bucketID the ID of the bucket to return
     * @return the bucket with the specified ID
     * @throws NoSuchElementException if there is no bucket associated with the specified ID in this dispatcher
     */
    public LocalBucket getBucket(int bucketID) throws NoSuchElementException {
        LocalBucket rtv = buckets.get(bucketID); 
        if (rtv == null) throw new NoSuchElementException("Bucket ID " + bucketID + " doesn't exist.");
        
        return rtv;
    }


    //****************** Bucket creation/deletion ******************//

    /**
     * Create new local bucket with specified storage class and storage capacity (different from default values).
     *
     * @param storageClass the class that represents the bucket implementation to use
     * @param capacity the hard capacity of the new bucket
     * @param softCapacity the soft capacity of the new bucket (soft &lt;= hard must hold otherwise hard is set to soft)
     * @param lowOccupation the low-occupation limit for the new bucket
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param storageClassParams additional parameters for creating a new instance of the storageClass
     * @return a new instance of the specified bucket class
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     * @throws InstantiationException if (1) the provided storageClass is not a part of LocalBucket hierarchy;
     *                                   (2) the storageClass does not have a proper constructor (String,long,long);
     *                                   (3) the correct constructor of storageClass is not accesible; or
     *                                   (4) the constuctor of storageClass has failed.
     */
    public static LocalBucket createBucket(Class<? extends LocalBucket> storageClass, long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, Map<String, Object> storageClassParams) throws CapacityFullException, InstantiationException {
        // Update provided parameters to correct values
        if (softCapacity < 0) softCapacity = 0;
        if (capacity < softCapacity) capacity = softCapacity;
        
        // Create new bucket with specified capacity
        try {
            try {
                // Try bucket class internal factory first
                Method factoryMethod = storageClass.getDeclaredMethod("getBucket", long.class, long.class, long.class, boolean.class, Map.class);
                if (!Modifier.isStatic(factoryMethod.getModifiers()))
                    throw new InstantiationException("Factory method 'getBucket' in " + storageClass + " is not static");
                if (!storageClass.isAssignableFrom(factoryMethod.getReturnType()))
                    throw new InstantiationException("Factory method 'getBucket' in " + storageClass + " has wrong return type");
                return (LocalBucket)factoryMethod.invoke(null, capacity, softCapacity, lowOccupation, occupationAsBytes, storageClassParams);
            } catch (NoSuchMethodException ignore) {
                // Factory method doesn't exist, try class constructor
                return storageClass.getDeclaredConstructor(
                            long.class, long.class, long.class, boolean.class
                         ).newInstance(
                            capacity, softCapacity, lowOccupation, occupationAsBytes
                         );
            }
        } catch (NoSuchMethodException e) {
            throw new InstantiationException("Storage " + storageClass + " lacks proper constructor: " + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new InstantiationException("Storage " + storageClass + " constructor with capacity is unaccesible: " + e.getMessage());
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new InstantiationException("Storage " + storageClass + " constructor invocation failed: " + e.getCause());
        }
    }

    /**
     * Create new local bucket with specified storage class and storage capacity (different from default values).
     *
     * @param storageClass the class that represents the bucket implementation to use
     * @param storageClassParams additional parameters for creating a new instance of the storageClass
     * @param capacity the hard capacity of the new bucket
     * @param softCapacity the soft capacity of the new bucket (soft &lt;= hard must hold otherwise hard is set to soft)
     * @param lowOccupation the low-occupation limit for the new bucket
     *
     * @return a new instance of the specified bucket class
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     * @throws InstantiationException if (1) the provided storageClass is not a part of LocalBucket hierarchy;
     *                                   (2) the storageClass does not have a proper constructor (String,long,long);
     *                                   (3) the correct constructor of storageClass is not accesible; or
     *                                   (4) the constuctor of storageClass has failed.
     */
    public synchronized LocalBucket createBucket(Class<? extends LocalBucket> storageClass, Map<String, Object> storageClassParams, long capacity, long softCapacity, long lowOccupation) throws CapacityFullException, InstantiationException {
        // Create new bucket with specified capacity
        LocalBucket bucket = createBucket(storageClass, capacity, softCapacity, lowOccupation, getBucketOccupationAsBytes(), storageClassParams);        

        // Create automatic filter for the bucket
        createAutoFilter(bucket);

        // Add the bucket to the list of buckets
        return addBucket(bucket);
    }

    /**
     * Create new local bucket with the default storage class and default storage capacity.
     *
     * @return a new instance of the default bucket class
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     * @throws InstantiationException if <ul><li>the provided storageClass is not a part of LocalBucket hierarchy</li>
     *                                   <li>the storageClass does not have a proper constructor (String,long,long)</li>
     *                                   <li>the correct constructor of storageClass is not accesible</li>
     *                                   <li>the constuctor of storageClass has failed</li></ul>
     */
    public LocalBucket createBucket() throws CapacityFullException, InstantiationException {
        return createBucket(defaultBucketClass, defaultBucketClassParams, bucketCapacity, bucketSoftCapacity, bucketLowOccupation);
    }

    /**
     * Create new local bucket with specified storage class and default storage capacity.
     * No additional parameters are passed to the constructor of the storageClass.
     *
     * @param storageClass the class that represents the bucket implementation to use
     *
     * @return a new instance of the specified bucket class
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     * @throws InstantiationException if <ul><li>the provided storageClass is not a part of LocalBucket hierarchy</li>
     *                                   <li>the storageClass does not have a proper constructor (String,long,long)</li>
     *                                   <li>the correct constructor of storageClass is not accesible</li>
     *                                   <li>the constuctor of storageClass has failed</li></ul>
     */
    public LocalBucket createBucket(Class<? extends LocalBucket> storageClass) throws CapacityFullException, InstantiationException {
        return createBucket(storageClass, null, bucketCapacity, bucketSoftCapacity, bucketLowOccupation);
    }

    /**
     * Create new local bucket with specified storage class and default storage capacity.
     * Additional parameters cat be specified for the constructor of the storageClass.
     *
     * @param storageClass the class that represents the bucket implementation to use
     * @param storageClassParams additional parameters for creating a new instance of the storageClass
     *
     * @return a new instance of the specified bucket class
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     * @throws InstantiationException if <ul><li>the provided storageClass is not a part of LocalBucket hierarchy</li>
     *                                   <li>the storageClass does not have a proper constructor (String,long,long)</li>
     *                                   <li>the correct constructor of storageClass is not accesible</li>
     *                                   <li>the constuctor of storageClass has failed</li></ul>
     */
    public LocalBucket createBucket(Class<? extends LocalBucket> storageClass, Map<String, Object> storageClassParams) throws CapacityFullException, InstantiationException {
        return createBucket(storageClass, storageClassParams, bucketCapacity, bucketSoftCapacity, bucketLowOccupation);
    }

    /**
     * Create new local bucket with default storage class and specified storage capacity.
     *
     * @param capacity the hard capacity of the new bucket
     * @param softCapacity the soft capacity of the new bucket (soft <= hard must hold otherwise hard is set to soft)
     * @param lowOccupation the low-occupation limit for the new bucket
     *
     * @return a new instance of the specified bucket class
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     * @throws InstantiationException if <ul><li>the provided storageClass is not a part of LocalBucket hierarchy</li>
     *                                   <li>the storageClass does not have a proper constructor (String,long,long)</li>
     *                                   <li>the correct constructor of storageClass is not accesible</li>
     *                                   <li>the constuctor of storageClass has failed</li></ul>
     */
    public LocalBucket createBucket(long capacity, long softCapacity, long lowOccupation) throws CapacityFullException, InstantiationException {
        return createBucket(defaultBucketClass, defaultBucketClassParams, capacity, softCapacity, lowOccupation);
    }

    /**
     * Add an existing bucket to this dispatcher.
     * A new unique ID is assigned to the bucket.
     *
     * @param bucket the bucket to add to this dispatcher
     * @return the added bucket
     * @throws IllegalStateException if the bucket is already maintained by another one
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     */
    public synchronized LocalBucket addBucket(LocalBucket bucket) throws IllegalStateException, CapacityFullException {
        // Check capacity
        if (buckets.size() >= maxBuckets)
            throw new CapacityFullException();

        // Check if bucket has no ID
        if (!bucket.isBucketStandalone()) {
            if (bucket == getBucket(bucket.getBucketID()))
                return bucket; // Bucket is already present in this dispatcher, ignore addition silently
            else throw new IllegalStateException("Bucket " + bucket + " can't be added to bucket dispatcher, because it is already maintained by another one");
        }

        // Add the new bucket to collection
        bucket.setBucketID(nextBucketID.getAndIncrement());
        buckets.put(bucket.getBucketID(), bucket);
        
        // Create pivot chooser for the bucket
        createAutoPivotChooser(bucket);

        return bucket;
    }

    /**
     * Delete the bucket with specified ID from this dispatcher.
     * Note that objects are not deleted from the bucket, just the bucket will be no longer maintained by this dispatcher.
     * However, statistics for the bucket are destroyed.
     *
     * @param bucketID the ID of the bucket to delete
     * @return the bucket deleted
     * @throws NoSuchElementException if there is no bucket with the specified ID
     */
    public synchronized LocalBucket removeBucket(int bucketID) throws NoSuchElementException {
        LocalBucket bucket = buckets.remove(bucketID);
        if (bucket == null)
            throw new NoSuchElementException("Bucket ID " + bucketID + " doesn't exist.");

        // Remove auto-pivot chooser for the bucket
        createdPivotChoosers.remove(bucket);

        // Reset bucket ID and statistics
        bucket.setBucketID(UNASSIGNED_BUCKET_ID);
        try {
            bucket.finalize();
        } catch (Throwable e) {
            // Log the exception but continue cleanly
            log.log(Level.WARNING, "Error during bucket clean-up, continuing", e);
        }

        return bucket;
    }

    /**
     * Move the bucket with the specified ID to another dispatcher.
     * @param bucketID the ID of the bucket to move
     * @param targetDispatcher the target dispatcher to move the bucket to
     * @return the bucket moved
     * @throws NoSuchElementException if there is no bucket with the specified ID
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     */
    public LocalBucket moveBucket(int bucketID, BucketDispatcher targetDispatcher) throws NoSuchElementException, CapacityFullException {
        synchronized (targetDispatcher) {
            if (targetDispatcher.getBucketCount() >= targetDispatcher.maxBuckets)
                throw new CapacityFullException();
            LocalBucket bucket = removeBucket(bucketID);
            targetDispatcher.addBucket(bucket); // This will synchronize also on this dispatcher, thus a deadlock can occurr if two moves are cross-executed
            return bucket;
        }
    }


    //****************** Textual representation info ******************//

    /**
     * Returns information about storage maintained by this dispatcher.
     * @return information about storage maintained by this dispatcher
     */
    @Override
    public String toString() {
        StringBuffer rtv = new StringBuffer(getClass().getName()).append(": ");
        rtv.append(buckets.size()).append('/').append(maxBuckets).append(" buckets of ");
        rtv.append(bucketSoftCapacity).append('/').append(bucketCapacity).append(bucketOccupationAsBytes?" bytes":" objects").append(" capacity ");        
        
        return rtv.toString();
    }

}

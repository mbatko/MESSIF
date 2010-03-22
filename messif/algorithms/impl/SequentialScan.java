/*
 * SequantialScan.java
 *
 */
package messif.algorithms.impl;

import java.util.Map;
import java.util.NoSuchElementException;
import messif.algorithms.Algorithm;
import messif.buckets.BucketDispatcher;
import messif.buckets.BucketErrorCode;
import messif.buckets.BucketStorageException;
import messif.buckets.CapacityFullException;
import messif.buckets.LocalBucket;
import messif.buckets.impl.MemoryStorageBucket;
import messif.objects.util.AbstractObjectList;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.LocalAbstractObject;
import messif.objects.PrecomputedDistancesFixedArrayFilter;
import messif.operations.query.ApproxKNNQueryOperation;
import messif.operations.data.BulkInsertOperation;
import messif.operations.data.DeleteByLocatorOperation;
import messif.operations.data.DeleteOperation;
import messif.operations.query.IncrementalNNQueryOperation;
import messif.operations.data.InsertOperation;
import messif.operations.QueryOperation;
import messif.operations.query.RangeQueryOperation;
import messif.operations.query.KNNQueryOperation;

/**
 * Implementation of the naive sequential scan algorithm.
 *
 * It uses one bucket to store objects and performs operations on the bucket.
 * It also supports pivot-based filtering. The pivots can be specified in a constructor.
 *
 * @author Vlastislav Dohnal, dohnal@fi.muni.cz, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class SequentialScan extends Algorithm {
    
    /** class id for serialization */
    static final long serialVersionUID = 1L;
    
    /** One instance of bucket where all objects are stored */
    protected final LocalBucket bucket;
    
    /** A list of fixed pivots used for filtering */
    protected final AbstractObjectList<LocalAbstractObject> pivots;
    
    /** Flag controlling the usage of PrecomputedDistancesFixedArrayFilter -- whether distances are set or appended (see the constructor below for details) */
    protected final boolean pivotDistsValidIfGiven;
    
    /**
     * Creates a new instance of SequantialScan access structure with specific bucket class and filtering pivots.
     * Additional parameters for the bucket class constructor can be passed.
     *
     * @param bucketClass the class of the storage bucket
     * @param bucketClassParams additional parameters for the bucket class constructor in the name->value form
     * @param pivotIter the iterator from which the fixed pivots will be read
     * @param pivotCount the nubmer of pivots to read from the iterator
     * @param pivotDistsValidIfGiven the flag which controls whether the already associated distances to pivots with new objects are valid or not; if so, they are used without computing and storing them again
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     * @throws InstantiationException if <ul><li>the provided storageClass is not a part of LocalBucket hierarchy</li>
     *                                   <li>the storageClass does not have a proper constructor (String,long,long)</li>
     *                                   <li>the correct constructor of storageClass is not accesible</li>
     *                                   <li>the constuctor of storageClass has failed</li></ul>
     */
    @Algorithm.AlgorithmConstructor(description = "SequantialScan Access Structure", arguments = {"bucket class", "bucket class params", "pivots", "pivot count", "pivotDistsValidIfGiven"})
    public SequentialScan(Class<? extends LocalBucket> bucketClass, Map<String, Object> bucketClassParams, AbstractObjectIterator<LocalAbstractObject> pivotIter, int pivotCount, boolean pivotDistsValidIfGiven) throws CapacityFullException, InstantiationException {
        super("SequantialScan");
        
        // Create an empty bucket (using the provided bucket class and parameters)
        bucket = BucketDispatcher.createBucket(bucketClass, Long.MAX_VALUE, Long.MAX_VALUE, 0, true, bucketClassParams);
        
        // Get the fixed pivots
        if (pivotCount > 0 && pivotIter != null) {
            pivots = new AbstractObjectList<LocalAbstractObject>();
            for (int i = 0; i < pivotCount; i++)
                pivots.add(pivotIter.next());
        } else pivots = null;
        
        // Precomputed distances already associated with newly inserted objects are valid or not.
        // If there are no precomputed distances stored at new objects, they are computed, of course.
        this.pivotDistsValidIfGiven = pivotDistsValidIfGiven;
    }
    
    /**
     * Creates a new instance of SequantialScan access structure with specific bucket class and filtering pivots.
     *
     * @param bucketClass The class of the storage bucket
     * @param pivotIter   The iterator from which the fixed pivots will be read
     * @param pivotCount  The nubmer of pivots to read from the iterator
     * @param pivotDistsValidIfGiven The flag which controls whether the already associated distances to pivots with new objects are valid or not. If so, they are used without computing and storing them again.
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     * @throws InstantiationException if <ul><li>the provided storageClass is not a part of LocalBucket hierarchy</li>
     *                                   <li>the storageClass does not have a proper constructor (String,long,long)</li>
     *                                   <li>the correct constructor of storageClass is not accesible</li>
     *                                   <li>the constuctor of storageClass has failed</li></ul>
     */
    @Algorithm.AlgorithmConstructor(description = "SequantialScan Access Structure", arguments = {"bucket class", "pivots", "pivot count", "pivotDistsValidIfGiven"})
    public SequentialScan(Class<? extends LocalBucket> bucketClass, AbstractObjectIterator<LocalAbstractObject> pivotIter, int pivotCount, boolean pivotDistsValidIfGiven) throws CapacityFullException, InstantiationException {
        this(bucketClass, null, pivotIter, pivotCount, pivotDistsValidIfGiven);
    }
    
    /**
     * Creates a new instance of SequantialScan access structure with specific bucket class.
     * Additional parameters for the bucket class constructor can be passed.
     *
     * @param bucketClass The class of the storage bucket
     * @param bucketClassParams additional parameters for the bucket class constructor in the name->value form
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     * @throws InstantiationException if <ul><li>the provided storageClass is not a part of LocalBucket hierarchy</li>
     *                                   <li>the storageClass does not have a proper constructor (String,long,long)</li>
     *                                   <li>the correct constructor of storageClass is not accesible</li>
     *                                   <li>the constuctor of storageClass has failed</li></ul>
     */
    @Algorithm.AlgorithmConstructor(description = "SequantialScan Access Structure", arguments = {"bucket class", "bucket class params"})
    public SequentialScan(Class<? extends LocalBucket> bucketClass, Map<String, Object> bucketClassParams) throws CapacityFullException, InstantiationException {
        this(bucketClass, bucketClassParams, null, 0, false);
    }
    
    /**
     * Creates a new instance of SequantialScan access structure with specific bucket class.
     *
     * @param bucketClass The class of the storage bucket
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     * @throws InstantiationException if <ul><li>the provided storageClass is not a part of LocalBucket hierarchy</li>
     *                                   <li>the storageClass does not have a proper constructor (String,long,long)</li>
     *                                   <li>the correct constructor of storageClass is not accesible</li>
     *                                   <li>the constuctor of storageClass has failed</li></ul>
     */
    @Algorithm.AlgorithmConstructor(description = "SequantialScan Access Structure", arguments = {"bucket class"})
    public SequentialScan(Class<? extends LocalBucket> bucketClass) throws CapacityFullException, InstantiationException {
        this(bucketClass, null);
    }
    
    /**
     * Creates a new instance of SequantialScan access structure with the default MemoryStorageBucket class.
     *
     * @throws CapacityFullException if the maximal number of buckets is already allocated
     * @throws InstantiationException if <ul><li>the provided storageClass is not a part of LocalBucket hierarchy</li>
     *                                   <li>the storageClass does not have a proper constructor (String,long,long)</li>
     *                                   <li>the correct constructor of storageClass is not accesible</li>
     *                                   <li>the constuctor of storageClass has failed</li></ul>
     */
    @Algorithm.AlgorithmConstructor(description = "SequantialScan Access Structure", arguments = {})
    public SequentialScan() throws CapacityFullException, InstantiationException {
        this(MemoryStorageBucket.class);
    }
    
    /******* PIVOT OPERATIONS *************************************/
    
    /**
     * Add precomputed distances to a given object.
     * Distance to all pivots is measured and stored into {@link PrecomputedDistancesFixedArrayFilter}.
     *
     * @param object the object to add the distances to
     */
    protected void addPrecompDist(LocalAbstractObject object) {
        PrecomputedDistancesFixedArrayFilter precompDist = object.getDistanceFilter(PrecomputedDistancesFixedArrayFilter.class);
        if (precompDist == null || !pivotDistsValidIfGiven) {
            // No precomputed distance associated or we are requested to add the distances to pivot on our own.
            if (precompDist == null)
                precompDist = new PrecomputedDistancesFixedArrayFilter(object);
            precompDist.addPrecompDist(pivots, object);
        }
    }
    
    @Override
    public void finalize() throws Throwable {
        bucket.finalize();
        super.finalize();
    }

    @Override
    public void destroy() throws Throwable {
        bucket.destroy();
        // Do not call super.destroy(), since algorithm needs to differentiate between finalizing and destroying
    }

    
    /**************************************************************/
    /******* INSERT OPERATION *************************************/
    /**************************************************************/
    
    /**
     * Inserts a new object.
     * 
     * @param operation Operation of insert which carries the object to be inserted.
     * @throws CapacityFullException if the hard capacity of the bucket is exceeded
     */
    public void insert(InsertOperation operation) throws CapacityFullException {
        // If pivot-based filtering is required, store the distances from pivots.
        if (pivots != null)
            addPrecompDist(operation.getInsertedObject());

        // Add the new object
        operation.endOperation(bucket.addObjectErrCode(operation.getInsertedObject()));
    }
    
    /**
     * Bulk insertion. Inserts a list of new objects.
     * 
     * @param operation The operation of bulk insert which carries the objects to be inserted.
     * @throws BucketStorageException if the hard capacity of the bucket is exceeded
     */
    public void bulkInsert(BulkInsertOperation operation) throws BucketStorageException {
        // If pivot-based filtering is required, store the distances from pivots.
        if (pivots != null)
            for (LocalAbstractObject obj : operation.getInsertedObjects())
                addPrecompDist(obj);

        // Add the new objects
        bucket.addObjects(operation.getInsertedObjects());
        operation.endOperation();
    }
    
    /**************************************************************/
    /******* DELETE OPERATION *************************************/
    /**************************************************************/
    
    /**
     * Deletes an object.
     *
     * @param operation The operation which specifies the object to be deleted.
     * @throws BucketStorageException if the low occupation limit is reached when deleting object
     */
    public void delete(DeleteOperation operation) throws BucketStorageException {
        int deleted = bucket.deleteObject(operation.getDeletedObject(), operation.getDeleteLimit());
        if (deleted > 0)
            operation.endOperation();
        else
            operation.endOperation(BucketErrorCode.OBJECT_NOT_FOUND);
    }

    /**
     * Deletes objects by locators.
     *
     * @param operation the operation which specifies the locators of objects to be deleted
     * @throws BucketStorageException if the low occupation limit is reached when deleting object
     */
    public void delete(DeleteByLocatorOperation operation) throws BucketStorageException {
        int deleted = 0;
        AbstractObjectIterator<LocalAbstractObject> bucketIterator = bucket.getAllObjects();
        try {
            while (!operation.isLimitReached()) {
                LocalAbstractObject obj = bucketIterator.getObjectByAnyLocator(operation.getLocators(), false); // Throws exception that exits the cycle
                bucketIterator.remove();
                operation.addDeletedObject(obj);
                deleted++;
            }
        } catch (NoSuchElementException ignore) {
        }

        if (deleted > 0)
            operation.endOperation();
        else
            operation.endOperation(BucketErrorCode.OBJECT_NOT_FOUND);
    }
    
    /**************************************************************/
    /******* SEARCH ALGORITHMS ************************************/
    /**************************************************************/
    
    /**
     * Performs the range search operation with given RangeQueryOperation object.
     * The answer is held in the RangeQueryOperation object.
     *
     * @param operation The range query operation which carries the query object and radius as well as the response list.
     */
    public void rangeSearch(RangeQueryOperation operation) {
        // If pivot-based filtering is required, store the distances from pivots.
        if (pivots != null)
            addPrecompDist(operation.getQueryObject());
        bucket.processQuery(operation);
    }
    
    
    /**
     * Performs the k-nearest neighbor search operation with given KNNQueryOperation object.
     * The answer is held in the KNNQueryOperation object.
     *
     * @param operation The kNN query operation which carries the query object and k as well as the response list.
     */
    public void knnSearch(KNNQueryOperation operation) {
        // If pivot-based filtering is required, store the distances from pivots.
        if (pivots != null)
            addPrecompDist(operation.getQueryObject());
        bucket.processQuery(operation);
    }
    
    
    /**
     * Performs the incremental nearest neighbor search operation with given IncrementalNNQueryOperation object.
     * The answer is held in the IncrementalNNQueryOperation object.
     *
     * @param operation The incremental NN query operation which carries the query object as well as the response list.
     */
    public void incrementalNNSearch(IncrementalNNQueryOperation operation) {
        // If pivot-based filtering is required, store the distances from pivots.
        if (pivots != null)
            addPrecompDist(operation.getQueryObject());
        bucket.processQuery(operation);
    }
    
    
    /**
     * Performs the approximate k-nearest neighbor search operation with given ApproxKNNQueryOperation object.
     * The answer is held in the ApproxKNNQueryOperation object.
     *
     * @param operation The approximate kNN query operation which carries the query object and k as well as the response list.
     */
    public void approxKNNSearch(ApproxKNNQueryOperation operation) {
        // If pivot-based filtering is required, store the distances from pivots.
        if (pivots != null)
            addPrecompDist(operation.getQueryObject());
        bucket.processQuery(operation);
    }
    
    /**
     * Performs a generic query operation.
     * Note that this method cannot provide precomputed distances.
     * 
     * @param operation the query operation which is to be executed and which will received the result list.
     * @throws CloneNotSupportedException if the operation does not support clonning (and thus cannot be used in parallel)
     * @throws InterruptedException if the processing thread was interrupted during processing
     */
    public void search(QueryOperation<?> operation) throws CloneNotSupportedException, InterruptedException {
        bucket.processQuery(operation);
    }
    
    /**
     * Converts the object to a string representation
     * @return String representation of this algorithm
     */
    @Override
    public String toString() {
        StringBuffer rtv;
        String lineSeparator = System.getProperty("line.separator", "\n");
        
        rtv = new StringBuffer();
        rtv.append("Algorithm: ").append(getName()).append(lineSeparator);
        rtv.append("Bucket Class: ").append(bucket.getClass().getName()).append(lineSeparator);
        rtv.append("Bucket Occupation: ").append(bucket.getOccupation()).append(" bytes").append(lineSeparator);
        rtv.append("Bucket Occupation: ").append(bucket.getObjectCount()).append(" objects").append(lineSeparator);
        
        return rtv.toString();
    }
}

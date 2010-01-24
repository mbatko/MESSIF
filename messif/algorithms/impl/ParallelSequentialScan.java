/*
 * ParallelSequantialScan
 *
 */
package messif.algorithms.impl;

import java.util.Map;
import messif.algorithms.Algorithm;
import messif.buckets.BucketDispatcher;
import messif.buckets.BucketErrorCode;
import messif.buckets.BucketStorageException;
import messif.buckets.LocalBucket;
import messif.buckets.impl.MemoryStorageBucket;
import messif.objects.LocalAbstractObject;
import messif.operations.BulkInsertOperation;
import messif.operations.DeleteOperation;
import messif.operations.InsertOperation;
import messif.operations.QueryOperation;

/**
 * Parallel implementation of the naive sequential scan algorithm.
 * Several buckets are used to store data in a round-robin fashion
 * using the {@link InsertOperation}. Then, each {@link messif.operations.QueryOperation}
 * is executed on each of the buckets in parallel.
 *
 * @author xbatko
 */
public class ParallelSequentialScan extends Algorithm {
    /** class id for serialization */
    static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Instances of bucket where all the objects are stored */
    private final LocalBucket[] buckets;

    /** Index of the bucket that receives next inserted object */
    private int insertBucket;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of ParallelSequentialScan access structure with specific bucket class.
     * Additional parameters for the bucket class constructor can be passed.
     *
     * @param parallelization the number of paralllel buckets to create
     * @param bucketClass the class of the storage bucket
     * @param bucketClassParams additional parameters for the bucket class constructor in the name->value form
     * @throws IllegalArgumentException if <ul><li>the provided bucketClass is not a part of LocalBucket hierarchy</li>
     *                                         <li>the bucketClass does not have a proper constructor (String,long,long)</li>
     *                                         <li>the correct constructor of bucketClass is not accesible</li>
     *                                         <li>the constuctor of bucketClass has failed</li></ul>
     */
    @Algorithm.AlgorithmConstructor(description = "Parallel SequantialScan Access Structure", arguments = {"parallelization", "bucket class", "bucket class params"})
    public ParallelSequentialScan(int parallelization, Class<? extends LocalBucket> bucketClass, Map<String, Object> bucketClassParams) throws IllegalArgumentException {
        super("SequantialScan");

        // Check the parallelization parameter
        if (parallelization < 1)
            throw new IllegalArgumentException("Parallelization argument must be at least 1");

        // Create empty buckets (using the provided bucket class and parameters)
        buckets = new LocalBucket[parallelization];
        for (int i = 0; i < parallelization; i++)
            buckets[i] = BucketDispatcher.createBucket(bucketClass, Long.MAX_VALUE, Long.MAX_VALUE, 0, true, bucketClassParams);
        insertBucket = 0;
    }

    /**
     * Creates a new instance of ParallelSequentialScan access structure with specific bucket class.
     *
     * @param parallelization the number of paralllel buckets to create
     * @param bucketClass the class of the storage bucket
     * @throws IllegalArgumentException if <ul><li>the provided bucketClass is not a part of LocalBucket hierarchy</li>
     *                                         <li>the bucketClass does not have a proper constructor (String,long,long)</li>
     *                                         <li>the correct constructor of bucketClass is not accesible</li>
     *                                         <li>the constuctor of bucketClass has failed</li></ul>
     */
    @Algorithm.AlgorithmConstructor(description = "Parallel SequantialScan Access Structure", arguments = {"parallelization", "bucket class"})
    public ParallelSequentialScan(int parallelization, Class<? extends LocalBucket> bucketClass) throws IllegalArgumentException {
        this(parallelization, bucketClass, null);
    }

    /**
     * Creates a new instance of ParallelSequentialScan access structure with {@link MemoryStorageBucket} as the storage class.
     *
     * @param parallelization the number of paralllel buckets to create
     * @throws IllegalArgumentException if <ul><li>the provided bucketClass is not a part of LocalBucket hierarchy</li>
     *                                         <li>the bucketClass does not have a proper constructor (String,long,long)</li>
     *                                         <li>the correct constructor of bucketClass is not accesible</li>
     *                                         <li>the constuctor of bucketClass has failed</li></ul>
     */
    @Algorithm.AlgorithmConstructor(description = "Parallel SequantialScan Access Structure", arguments = {"parallelization"})
    public ParallelSequentialScan(int parallelization) throws IllegalArgumentException {
        this(parallelization, MemoryStorageBucket.class);
    }

    @Override
    public void finalize() throws Throwable {
        for (LocalBucket localBucket : buckets)
            localBucket.finalize();
        super.finalize();
    }


    //****************** Insert operation ******************//

    /**
     * Inserts a new object.
     * 
     * @param operation the insert operation which carries the object to be inserted.
     */
    public void insert(InsertOperation operation) {
        try {
            buckets[insertBucket].addObject(operation.getInsertedObject());
            operation.endOperation();
            insertBucket = (insertBucket + 1) % buckets.length;
        } catch (BucketStorageException e) {
            operation.endOperation(e.getErrorCode());
        }
    }

    /**
     * Inserts multiple new objects.
     * 
     * @param operation the bulk-insert operation which carries the objects to be inserted.
     */
    public void insert(BulkInsertOperation operation) {
        try {
            for (LocalAbstractObject object : operation.getInsertedObjects()) {
                buckets[insertBucket].addObject(object);
                insertBucket = (insertBucket + 1) % buckets.length;
            }
            operation.endOperation();
        } catch (BucketStorageException e) {
            operation.endOperation(e.getErrorCode());
        }
    }


    //****************** Delete operation ******************//

    /**
     * Deletes an object.
     * 
     * @param operation the delete operation which specifies the object to be deleted.
     * @throws BucketStorageException if the low occupation limit is reached when deleting object
     */
    public void delete(DeleteOperation operation) throws BucketStorageException {
        int deleted = 0;
        for (LocalBucket bucket : buckets) {
            deleted += bucket.deleteObject(operation.getDeletedObject(), operation.getDeleteLimit() - deleted);
            if (deleted >= operation.getDeleteLimit())
                break;
        }
        if (deleted > 0)
            operation.endOperation();
        else
            operation.endOperation(BucketErrorCode.OBJECT_NOT_FOUND);
    }


    //****************** Query operations ******************//
    
    /**
     * Performs a query operation.
     * @param operation the query operation which is to be executed and which will received the result list.
     * @throws CloneNotSupportedException if the operation does not support clonning (and thus cannot be used in parallel)
     * @throws InterruptedException if the processing thread was interrupted during processing
     */
    public void search(QueryOperation<?> operation) throws CloneNotSupportedException, InterruptedException {
        // Create a query clone for each bucket and execute it in a new thread
        final QueryOperation<?>[] operationClones = new QueryOperation[buckets.length];
        final Thread[] operationThreads = new Thread[buckets.length];
        for (int i = 0; i < buckets.length; i++) {
            final int j = i;
            operationClones[i] = (QueryOperation<?>)operation.clone();
            operationThreads[i] = new Thread("Query processing thread - bucket " + i) {
                @Override
                public void run() {
                    buckets[j].processQuery(operationClones[j]);
                }
            };
            operationThreads[i].start();
        }

        // Wait for the threads to finish
        for (int i = 0; i < operationThreads.length; i++) {
            operationThreads[i].join();
            operation.updateFrom(operationClones[i]);
        }

        operation.endOperation();
    }


    //****************** Query operations ******************//

    /**
     * Shows the information about this algorithm.
     * @return the information about this algorithm
     */
    @Override
    public String toString() {
        StringBuffer rtv;
        String lineSeparator = System.getProperty("line.separator", "\n");
        
        rtv = new StringBuffer();
        rtv.append("Algorithm: ").append(getName()).append(lineSeparator);
        rtv.append("Bucket Class: ").append(buckets[0].getClass().getName()).append(lineSeparator);
        long occupation = 0;
        int objectCount = 0;
        for (LocalBucket bucket : buckets) {
            occupation += bucket.getOccupation();
            objectCount += bucket.getObjectCount();
        }
        rtv.append("Number of buckets (threads): ").append(buckets.length).append(lineSeparator);
        rtv.append("Bucket Occupation: ").append(occupation).append(" bytes").append(lineSeparator);
        rtv.append("Bucket Occupation: ").append(objectCount).append(" objects").append(lineSeparator);
        
        return rtv.toString();
    }
}

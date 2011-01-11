/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.algorithms.impl;

import java.util.Map;
import java.util.NoSuchElementException;
import messif.algorithms.Algorithm;
import messif.buckets.BucketDispatcher;
import messif.buckets.BucketErrorCode;
import messif.buckets.BucketStorageException;
import messif.buckets.LocalBucket;
import messif.buckets.impl.MemoryStorageBucket;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectIterator;
import messif.operations.data.BulkInsertOperation;
import messif.operations.data.DeleteByLocatorOperation;
import messif.operations.data.DeleteOperation;
import messif.operations.data.InsertOperation;
import messif.operations.QueryOperation;

/**
 * Parallel implementation of the naive sequential scan algorithm.
 * Several buckets are used to store data in a round-robin fashion
 * using the {@link InsertOperation}. Then, each {@link messif.operations.QueryOperation}
 * is executed on each of the buckets in parallel.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
        super("ParallelSequentialScan");

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

    @Override
    public void destroy() throws Throwable {
        for (LocalBucket localBucket : buckets)
            localBucket.finalize();
        // Do not call super.destroy(), since algorithm needs to differentiate between finalizing and destroying
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
        int limit = operation.getDeleteLimit();
        for (LocalBucket bucket : buckets) {
            deleted += bucket.deleteObject(operation.getDeletedObject(), limit == 0 ? 0 : limit - deleted);
            if (limit > 0 && deleted >= limit)
                break;
        }
        if (deleted > 0)
            operation.endOperation();
        else
            operation.endOperation(BucketErrorCode.OBJECT_NOT_FOUND);
    }

    /**
     * Deletes objects by locators.
     *
     * @param operation the delete operation which specifies the locators of objects to be deleted
     * @throws BucketStorageException if the low occupation limit is reached when deleting object
     */
    public void delete(DeleteByLocatorOperation operation) throws BucketStorageException {
        int deleted = 0;
        for (LocalBucket bucket : buckets) {
            try {
                AbstractObjectIterator<LocalAbstractObject> bucketIterator = bucket.getAllObjects();
                while (!operation.isLimitReached()) {
                    LocalAbstractObject obj = bucketIterator.getObjectByAnyLocator(operation.getLocators(), false); // Throws exception that exits the cycle
                    bucketIterator.remove();
                    operation.addDeletedObject(obj);
                    deleted++;
                }
            } catch (NoSuchElementException ignore) {
            }
        }
        if (deleted > 0)
            operation.endOperation();
        else
            operation.endOperation(BucketErrorCode.OBJECT_NOT_FOUND);
    }


    //****************** Query processing thread implementation ******************//

    /**
     * Internal thread used for processing an operation on a given bucket.
     */
    private static class QueryProcessingThread extends Thread {
        /** Operation processed by this thread */
        private final QueryOperation<?> operation;
        /** Bucket on which the operation is processed by this thread */
        private final LocalBucket bucket;
        /** Exception thrown during the processing */
        private RuntimeException processingException;

        /**
         * Creates a new processing thread for given operation and bucket.
         * @param bucketName the identification of the bucket processed by this thread
         * @param operation the operation processed by this thread
         * @param bucket the bucket on which the operation is processed by this thread
         */
        public QueryProcessingThread(String bucketName, QueryOperation<?> operation, LocalBucket bucket) {
            super("Query processing thread - " + bucketName);
            this.operation = operation;
            this.bucket = bucket;
        }

        @Override
        public void run() {
            processingException = null;
            try {
                bucket.processQuery(operation);
            } catch (RuntimeException e) {
                processingException = e;
            }
        }

        /**
         * Finishes processing of this thread by updating the given operation
         * with answers from this operation. This method waits for this thread
         * to finish.
         * @param originalOperation the operation to update
         * @throws RuntimeException if there was an error during the processing
         * @throws InterruptedException if a waiting for this thread to finish was interrupted
         */
        public void finishUpdate(QueryOperation<?> originalOperation) throws RuntimeException, InterruptedException {
            join();
            if (processingException != null)
                throw processingException;
            originalOperation.updateFrom(operation);
        }
    }

    /**
     * Performs a query operation.
     * @param operation the query operation which is to be executed and which will received the result list.
     * @throws CloneNotSupportedException if the operation does not support clonning (and thus cannot be used in parallel)
         * @throws InterruptedException if a waiting for a processing thread was interrupted
     */
    public void search(QueryOperation<?> operation) throws CloneNotSupportedException, InterruptedException {
        // Create a processing thread for each bucket
        QueryProcessingThread[] operationThreads = new QueryProcessingThread[buckets.length];
        for (int i = 0; i < buckets.length; i++) {
            operationThreads[i] = new QueryProcessingThread("bucket " + i, (QueryOperation<?>)operation.clone(), buckets[i]);
            operationThreads[i].start();
        }

        // Wait for the threads to finish
        for (int i = 0; i < operationThreads.length; i++)
            operationThreads[i].finishUpdate(operation);

        operation.endOperation();
    }


    //****************** Information string ******************//

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

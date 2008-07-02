/*
 * AlgorithmStorageBucket.java
 *
 * Created on 21. unor 2005, 19:39
 */

package messif.buckets;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import messif.algorithms.Algorithm;
import messif.algorithms.AlgorithmMethodException;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.operations.DeleteOperation;
import messif.operations.GetAllObjectsQueryOperation;
import messif.operations.GetObjectQueryOperation;
import messif.operations.InsertOperation;
import messif.operations.QueryOperation;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import messif.objects.util.StreamGenericAbstractObjectIterator;
import messif.objects.UniqueID;
import messif.operations.GetObjectsByLocatorsOperation;
import messif.utility.Convert;

/**
 * This is a LocalBucket that allows to create buckets backed by an Algorithm.
 * The algorithm should be able to execute the following operations:
 *    InsertOperation
 *    DeleteOperation
 *    GetAllObjectsQueryOperation
 *    GetObjectQueryOperation
 * All other query operations are first tried on the encapsulated algorithm
 * and if they are not supported, they are evaluated by the standard mechanism.
 *
 * @author xbatko
 */
public class AlgorithmStorageBucket extends LocalFilteredBucket {

    /** Class serial id for serialization */
    private static final long serialVersionUID = -792888618241233159L;    

    /****************** Local data ******************/

    /** Encapsulated algorithm */
    protected final Algorithm algorithm;

    /** Stored object count */
    protected int objectCount = 0;


    /****************** Constructors ******************/
    
    /**
     * Creates a new instance of AlgorithmStorageBucket and setups all bucket limits.
     * Note that the algorithm should not contain objects.
     *
     * @param algorithm encapsulated algorithm that will actually do the job
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    public AlgorithmStorageBucket(Algorithm algorithm, long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
        this.algorithm = algorithm;
    }

    /**
     * Clean up bucket internals before deletion.
     * This method is called by bucket dispatcher when this bucket is removed
     * or when the bucket is garbage collected.
     * 
     * The method calls finalizer of the encapsulated algorithm.
     * 
     * @throws Throwable if there was an error during releasing resources
     */
    @Override
    public void finalize() throws Throwable {
        algorithm.finalize();
        super.finalize();
    }


    /***************** Factory method  *****************************************/
    
    /**
     * Creates a new algorithm bucket. The parameters for the algorithm constructor are specified in the parameters map.
     * 
     * Recognized parameters:
     *   class   - the algorithm class
     *   param.1 - the first argument for the algorithm constructor
     *   param.2 - etc.
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param parameters list of named parameters - this bucket supports "file" and "path" (see above)
     * @throws IllegalArgumentException if the parameters from the <code>parameters</code> map are invalid and the
     *         backing algorithm stub cannot be created
     * @return a new DiskBucket instance
     */
    protected static AlgorithmStorageBucket getBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, Map<String, Object> parameters) throws IllegalArgumentException {
        // Check the "algorithm" parameter
        try {
            Algorithm alg = (Algorithm)parameters.get("algorithm");
            if (alg != null)
                return new AlgorithmStorageBucket(alg, capacity, softCapacity, lowOccupation, occupationAsBytes);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("The parameter map contains key 'algorithm', but it is not an instance of Algorithm");
        }

        // Create the algorithm
        Algorithm alg;
        Object classParam = parameters.get("class");
        if (classParam == null)
            throw new IllegalArgumentException("The parameter map must contain key 'class' for the encapsulated algorithm");
        if (classParam instanceof String) {
            alg = createAlgorithmFromParams((String)classParam, parameters);
        } else if (classParam instanceof Class) {
            alg = createAlgorithmFromParams(Convert.genericCastToClass(classParam, Algorithm.class), parameters);
        } else throw new IllegalArgumentException("The 'class' parameter must be a valid class name");
                
        return new AlgorithmStorageBucket(alg, capacity, softCapacity, lowOccupation, occupationAsBytes);
    }    

    /**
     * Creates an algorithm of the specified class with map of parameters.
     * The map should have keys of the format "param.1", "param.2", etc.
     * @param algClass the class of the algorithm that is created
     * @param parameters the parameters for the algorithm constructor
     * @return a new instance of algorithm
     * @throws IllegalArgumentException if the parameter map contains invalid values or the instantiation of algorithm fails
     */
    protected static Algorithm createAlgorithmFromParams(Class<? extends Algorithm> algClass, Map<String, Object> parameters) throws IllegalArgumentException {
        // Transform mapped parameters into list
        List<Object> algParams = new ArrayList<Object>(parameters.size() - 1);
        Object param;
        while ((param = parameters.get("param."+(algParams.size()+1))) != null)
            algParams.add(param);

        // Create new instance of algorithm
        try {
            return Convert.createInstanceWithInheritableArgs(algClass, algParams.toArray());
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("The class " + algClass.getName() + " does not contain constructor for given parameters " + algParams + ": " + e.getMessage());
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Can't create algorithm '" + algClass.getName() + "' for AlgorithmStorageBucket", e.getCause());
        }
    }

    /**
     * Creates an algorithm of the specified class with map of parameters.
     * The map should have keys of the format "param.1", "param.2", etc.
     * @param algClassName the name of the class of the algorithm that is created
     * @param parameters the parameters for the algorithm constructor
     * @return a new instance of algorithm
     * @throws IllegalArgumentException if the parameter map contains invalid values or the instantiation of algorithm fails
     */
    protected static Algorithm createAlgorithmFromParams(String algClassName, Map<String, Object> parameters) throws IllegalArgumentException {
        // Check validity of the passed class argument
        Class<Algorithm> algClass;
        try {
            algClass = Convert.getClassForName(algClassName, Algorithm.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("The parameter map must contain key 'class' as subtype of Algorithm: " + e.getMessage());
        }

        // Transform mapped parameters into list
        List<String> algParams = new ArrayList<String>(parameters.size() - 1);
        try {
            Object param;
            while ((param = parameters.get("param."+(algParams.size()+1))) != null)
                algParams.add((String)param);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("The parameter map contained string class, but object parameters: " + e.getMessage());
        }

        // Create new instance of algorithm
        try {
            Map<String, StreamGenericAbstractObjectIterator> objectStreams = Convert.safeGenericCastMap(parameters.get("objectStreams"), String.class, StreamGenericAbstractObjectIterator.class); // This cast IS checked, because StreamGenericAbstractObjectIterator has LocalAbstractObject as default E
            return Convert.createInstanceWithStringArgs(
                    Algorithm.getAnnotatedConstructors(algClass),
                    algParams.toArray(new String[algParams.size()]),
                    objectStreams
            );
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Can't create algorithm '" + algClass.getName() + "' for AlgorithmStorageBucket", e.getCause());
        }
    }


    /***************** Implementations using algorithm instance *****************/

    /**
     * Stores the specified object in the encapsulated algorithm, i.e.
     * the InsertOperation is executed.
     *
     * @param object The new object to be inserted
     * @return error code - for details, see documentation of {@link BucketErrorCode}
     * @throws UnsupportedOperationException if the encapsulated algorithm does not support InsertOperation
     */
    protected BucketErrorCode storeObject(LocalAbstractObject object) throws UnsupportedOperationException {
        InsertOperation operation = new InsertOperation(object);
        try {
            algorithm.executeOperation(operation);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException(e.getMessage());
        } catch (AlgorithmMethodException e) {
            throw new UnsupportedOperationException(e.getCause());
        }

        // Update object counter
        if (operation.wasSuccessful())
            objectCount++;

        return (BucketErrorCode)operation.getErrorCode();
    }

    /**
     * Process a query operation on objects from this bucket.
     * The query operation's answer is updated with objects from this bucket
     * that satisfy the query.
     * 
     * The query is first executed directly on the algorithm and
     * if it is unsupported, the standard query evaluation is performed
     * (i.e. getAllObjects is called and then the sequential scan is used).
     *
     * @param query query operation that is to be processed on this bucket
     * @return the number of objects that were added to answer
     */
    public int processQuery(QueryOperation query) throws UnsupportedOperationException {
        int beforeCount = query.getAnswerCount();
        try {
            algorithm.executeOperation(query);
            return query.getAnswerCount() - beforeCount;
        } catch (NoSuchMethodException e) {
            return super.processQuery(query);
        } catch (AlgorithmMethodException e) {
            throw new UnsupportedOperationException(e.getCause());
        }
    }

    /**
     * Delete all objects from this bucket, that are {@link messif.objects.LocalAbstractObject#dataEquals data-equals} to
     * the specified object.
     * @param object the object to match against
     * @param deleteLimit the maximal number of deleted objects (zero means unlimited)
     * @throws OccupationLowException This exception is throws if the low occupation limit is reached when deleting object
     * @return the number of deleted objects
     */
    @Override
    public synchronized int deleteObject(LocalAbstractObject object, int deleteLimit) throws OccupationLowException {
        DeleteOperation operation = new DeleteOperation(object, deleteLimit);
        try {
            algorithm.executeOperation(operation);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException(e.getMessage());
        } catch (AlgorithmMethodException e) {
            throw new UnsupportedOperationException(e.getCause());
        }

        if (operation.getErrorCode().equals(BucketErrorCode.LOWOCCUPATION_EXCEEDED))
            throw new OccupationLowException();

        // Update occupation
        occupation -= operation.getTotalSizeDeleted();

        // Update object count
        objectCount -= operation.getObjectsDeleted();

        // Update statistics
        counterBucketDelObject.add(this, operation.getObjectsDeleted());
        
        return operation.getObjectsDeleted();
    }

    /**
     * Returns current number of objects stored in bucket.
     * @return current number of objects stored in bucket
     */
    public int getObjectCount() {
        return objectCount;
    }


    /****************** Splitting ******************/

    /**
     * Splits this bucket according to the specified policy.
     * If the encapsulated algorithm implements the {@link SplittableAlgorithm} interface and the bucketCreator is provided,
     * the split method of the algorithm will be called to perform the split. Otherwise, the standard Bucket's {@link Bucket#split}
     * is executed.
     * 
     * @param policy the split policy used to split this bucket
     * @param targetBuckets the list of target buckets that will receive the list of created buckets
     * @param bucketCreator the bucket dispatcher to use when creating target buckets
     * @param whoStays identification of a partition whose objects stay in this bucket.
     * @return the number of objects moved
     * @throws IllegalArgumentException if there are too few target buckets
     * @throws CapacityFullException if a target bucket overflows during object move; <b>warning:</b> the split is interrupted and you should reinitialize it
     * @throws OccupationLowException if a this bucket underflows during object move; <b>warning:</b> the split is interrupted and you should reinitialize it
     */
    public synchronized int split(SplitPolicy policy, final List<Bucket> targetBuckets, final BucketDispatcher bucketCreator, int whoStays) throws OccupationLowException, IllegalArgumentException, CapacityFullException {
        if (!(algorithm instanceof SplittableAlgorithm) || bucketCreator == null)
            return super.split(policy, targetBuckets, bucketCreator, whoStays);
        
        // Prepare the split result object
        final AtomicInteger count = new AtomicInteger(0);
        final AlgorithmStorageBucket thisBucket = this;
        SplittableAlgorithm.SplittableAlgorithmResult result = new SplittableAlgorithm.SplittableAlgorithmResult() {
            protected Map<Algorithm, AlgorithmStorageBucket> bucketMap = new HashMap<Algorithm, AlgorithmStorageBucket>();
            public void markMovedObjects(Algorithm alg, Collection<? extends LocalAbstractObject> objects) throws OccupationLowException, CapacityFullException, InstantiationException, FilterRejectException {
                // Call single addition for all objects
                for (LocalAbstractObject object : objects)
                    markMovedObject(alg, object);
            }
            public void markMovedObject(Algorithm alg, LocalAbstractObject object) throws OccupationLowException, CapacityFullException, InstantiationException, FilterRejectException {
                // Get the existing bucket for the specified algorithm
                AlgorithmStorageBucket bucket = bucketMap.get(alg);
                if (bucket == null) {
                    // Bucket for the specified algorithm doesn't exist yet, create a AlgorithmStorageBucket wrapper
                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put("algorithm", alg);
                    bucketMap.put(alg, bucket = (AlgorithmStorageBucket)bucketCreator.createBucket(AlgorithmStorageBucket.class, params));
                    if (targetBuckets != null)
                        targetBuckets.add(bucket);
                }
                
                // Update filters of the destination algorithm bucket
                thisBucket.filterDeleteObjectBefore(object);
                bucket.filterAddObjectBefore(object);
                
                // Check the destination bucket for overflow
                long bytesMoved = object.getSize();
                if (bucket.occupation + (bucket.occupationAsBytes?bytesMoved:1) > bucket.capacity)
                    throw new CapacityFullException("Encapsulating bucket capacity for algorithm '" + alg.getName() + "' was exceeded");
                // Check the current bucket for underflow
                if (occupation - (occupationAsBytes?bytesMoved:1) < lowOccupation)
                    throw new OccupationLowException("Encapsulating bucket capacity for algorithm '" + algorithm.getName() + "' was exceeded");
                // Update the occupation
                bucket.occupation += bucket.occupationAsBytes?bytesMoved:1;
                occupation -= occupationAsBytes?bytesMoved:1;
                // Update objects count
                bucket.objectCount++;
                objectCount--;
                count.incrementAndGet();
                
                // Update filters of this algorithm bucket
               thisBucket.filterDeleteObjectAfter(object);
               bucket.filterAddObjectAfter(object);
            }
        };
        
        // Execute the split
        ((SplittableAlgorithm)algorithm).split(policy, result, whoStays);

        return count.get();
    }


    /****************** Iterator object ******************/

    /**
     * Returns iterator through all the objects in this bucket.
     * @return iterator through all the objects in this bucket
     */
    protected LocalBucketIterator<? extends AlgorithmStorageBucket> iterator() {
        return new AlgorithmStorageBucketIterator<AlgorithmStorageBucket>(this);
    }

    /** Internal class for algorithm-backed iterator implementation */
    protected static class AlgorithmStorageBucketIterator<T extends AlgorithmStorageBucket> extends LocalBucket.LocalBucketIterator<T> {
        /** Currently executed iterator */
        protected Iterator<AbstractObject> iterator = null;
        /** Last returned object */
        protected LocalAbstractObject currentObject = null;

        /**
         * Creates a new instance of AlgorithmStorageBucketIterator with the AlgorithmStorageBucket.
         * This constructor is intended to be called only from AlgorithmStorageBucket class.
         *
         * @param bucket actual instance of AlgorithmStorageBucket on which this iterator should work
         */
        protected AlgorithmStorageBucketIterator(T bucket) {
           super(bucket);
        }

        /**
         * Get an object with specified ID from this bucket.
         * This method will execute GetObject operation on the encapsulated algorithm.
         *
         * @param objectID ID of the object to retrieve
         * @return object with specified ID from this bucket
         * @throws NoSuchElementException if there is no object with the specified ID in this bucket
         */
        public LocalAbstractObject getObjectByID(UniqueID objectID) throws NoSuchElementException {
            GetObjectQueryOperation operation = new GetObjectQueryOperation(objectID);
            try {
                bucket.algorithm.executeOperation(operation);
            } catch (NoSuchMethodException e) {
                // Algorithm doesn't support GetObjectQueryOperation query, try the fallback sequential search approach with getAllObjects
                return super.getObjectByID(objectID);
            } catch (AlgorithmMethodException e) {
                throw new NoSuchElementException("Object not found because of error: " + e.getCause());
            }
            if (operation.getAnswerCount() == 0)
                throw new NoSuchElementException("Object not found");

            return currentObject = operation.getAnswerObject().getLocalAbstractObject();
        }

        /**
         * Get an object with specified ID from this bucket.
         * This method will execute GetObject operation on the encapsulated algorithm.
         *
         * @param locatorURIs the set of locator URIs of the object to retrieve
         * @param removeFound if <tt>true</tt> the locators which were found are removed from the <tt>locatorURIs</tt> set, otherwise, <tt>locatorURIs</tt> is not touched
         * @return object with specified ID from this bucket
         * @throws NoSuchElementException if there is no object with the specified ID in this bucket
         */
        public LocalAbstractObject getObjectByAnyLocator(Set<String> locatorURIs, boolean removeFound) throws NoSuchElementException {
            QueryOperation operation = new GetObjectsByLocatorsOperation(locatorURIs);
            try {
                bucket.algorithm.executeOperation(operation);
            } catch (NoSuchMethodException e) {
                // Algorithm doesn't support GetObjectByLocatorOperation query, try the fallback sequential search approach with getAllObjects 
                return super.getObjectByAnyLocator(locatorURIs, removeFound);
            } catch (AlgorithmMethodException e) {
                throw new NoSuchElementException("Object not found because of error: " + e.getCause());
            }
            if (operation.getAnswerCount() == 0)
                throw new NoSuchElementException("Object not found");

            // Return first object found
            currentObject = operation.getAnswer().next().getLocalAbstractObject();
            locatorURIs.remove(currentObject.getLocatorURI());
            return currentObject;
        }

        /** 
         * Physically removes the last object returned by this iterator.
         * This method will execute DeleteOperation on the encapsulated algorithm.
         * 
         * @throws NoSuchElementException if next or getObjectByID was not called before
         * @throws UnsupportedOperationException if the encapsulated algorithm does not support DeleteOperation
         */
        protected void removeInternal() throws NoSuchElementException, UnsupportedOperationException {
            if (currentObject == null)
                throw new NoSuchElementException("Can't call remove before next was called");
            
            DeleteOperation operation = new DeleteOperation(currentObject);
            try {
                bucket.algorithm.executeOperation(operation);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOperationException(e.getMessage());
            } catch (AlgorithmMethodException e) {
                throw new NoSuchElementException("Object not found because of error: " + e.getCause());
            }

            // Update object count
            if (operation.wasSuccessful())
                bucket.objectCount -= operation.getObjectsDeleted();
        }

        /**
         * Internal method that either returns previously initialized iterator or
         * creates a new one by executing GetAllObjectsQueryOperation on the encapsulated algorithm.
         *
         * @return iterator over all objects stored in the encapsulated algorithm
         * @throws UnsupportedOperationException if the encapsulated algorithm does not support GetAllObjectsQueryOperation
         */
        protected Iterator<AbstractObject> getIterator() throws UnsupportedOperationException {
            if (iterator != null)
                return iterator;
            
            GetAllObjectsQueryOperation operation = new GetAllObjectsQueryOperation();
            try {
                bucket.algorithm.executeOperation(operation);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOperationException(e.getMessage());
            } catch (AlgorithmMethodException e) {
                throw new UnsupportedOperationException(e.getCause());
            }
            
            return iterator = operation.getAnswer();
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration.
         * @throws NoSuchElementException iteration has no more elements.
         */
        public LocalAbstractObject next() throws NoSuchElementException {
            return currentObject = getIterator().next().getLocalAbstractObject();
        }

        /**
         * Returns <tt>true</tt> if the iteration has more elements. (In other
         * words, returns <tt>true</tt> if <tt>next</tt> would return an element
         * rather than throwing an exception.)
         *
         * @return <tt>true</tt> if the iterator has more elements.
         */
        public boolean hasNext() {
            return getIterator().hasNext();
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
        
    }    
}

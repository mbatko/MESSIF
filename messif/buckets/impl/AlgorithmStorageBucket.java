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
package messif.buckets.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import messif.algorithms.Algorithm;
import messif.algorithms.AlgorithmMethodException;
import messif.buckets.BucketStorageException;
import messif.buckets.index.IndexComparator;
import messif.buckets.index.ModifiableSearch;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.operations.data.DeleteOperation;
import messif.operations.data.InsertOperation;
import messif.operations.QueryOperation;
import messif.buckets.Addible;
import messif.buckets.Bucket;
import messif.buckets.BucketDispatcher;
import messif.buckets.LocalBucket;
import messif.buckets.Removable;
import messif.buckets.StorageFailureException;
import messif.buckets.index.ModifiableIndex;
import messif.buckets.index.OperationIndexComparator;
import messif.buckets.index.impl.AbstractSearch;
import messif.buckets.split.SplitPolicy;
import messif.buckets.split.SplittableAlgorithm;
import messif.operations.AnswerType;
import messif.operations.data.BulkInsertOperation;
import messif.operations.data.DeleteByLocatorOperation;
import messif.operations.query.GetAllObjectsQueryOperation;
import messif.utility.Convert;
import messif.utility.reflection.ConstructorInstantiator;
import messif.utility.reflection.NoSuchInstantiatorException;

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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class AlgorithmStorageBucket extends LocalBucket implements ModifiableIndex<LocalAbstractObject> {

    /** Class serial id for serialization */
    private static final long serialVersionUID = -792888618241233159L;    

    //****************** Local data ******************//

    /** Encapsulated algorithm */
    private final Algorithm algorithm;

    /** Stored object count */
    private int objectCount = 0;


    //****************** Constructors ******************//

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

    @Override
    public void destroy() throws Throwable {
        algorithm.destroy();
        super.destroy();
    }


    //***************** Factory method *****************//

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
    public static AlgorithmStorageBucket getBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, Map<String, Object> parameters) throws IllegalArgumentException {
        // Check parameters
        if (parameters == null)
            throw new IllegalArgumentException("No parameters were specified");

        // Check the "algorithm" parameter
        try {
            Algorithm alg = (Algorithm)parameters.get("algorithm");
            if (alg != null)
                return new AlgorithmStorageBucket(alg, capacity, softCapacity, lowOccupation, occupationAsBytes);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("The parameter map contains key 'algorithm', but it is not an instance of Algorithm");
        }
                
        return new AlgorithmStorageBucket(createAlgorithmFromParams(parameters), capacity, softCapacity, lowOccupation, occupationAsBytes);
    }    

    /**
     * Creates an algorithm of the specified class with map of parameters.
     * The map should have keys of the format "param.1", "param.2", etc.
     * The map also must contain a "class" parameter with the algorithm class.
     * @param parameters the parameters for the algorithm constructor
     * @return a new instance of algorithm
     * @throws IllegalArgumentException if the parameter map contains invalid values or the instantiation of algorithm fails
     */
    protected static Algorithm createAlgorithmFromParams(Map<String, Object> parameters) throws IllegalArgumentException {
        // Get the algorithm class from parameters
        Class<? extends Algorithm> algClass = Convert.toGenericClass(parameters.get("class"), Algorithm.class);
        if (algClass == null)
            throw new IllegalArgumentException("The parameter map must contain key 'class' for the encapsulated algorithm");

        // Transform mapped parameters into list
        List<Object> algParams = new ArrayList<Object>(parameters.size() - 1);
        Object param;
        while ((param = parameters.get("param."+(algParams.size()+1))) != null)
            algParams.add(param);

        // Create new instance of algorithm
        try {
            Map<String, Object> namedInstances = Convert.safeGenericCastMap(parameters.get("namedInstances"), String.class, Object.class);
            return ConstructorInstantiator.createInstanceWithStringArgs(algClass, algParams.toArray(), 0, algParams.size() -1, namedInstances);
        } catch (NoSuchInstantiatorException e) {
            throw new IllegalArgumentException("The class " + algClass.getName() + " does not contain constructor for given parameters " + algParams + ": " + e.getMessage());
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Can't create algorithm '" + algClass.getName() + "' for AlgorithmStorageBucket", e.getCause());
        }
    }


    //***************** Index method overrides *****************//

    @Override
    protected ModifiableIndex<LocalAbstractObject> getModifiableIndex() {
        return this;
    }

    @Override
    public int size() {
        return objectCount;
    }

    /**
     * Return the instance of the algorithm encapsulated by this bucket
     * @return the instance of the algorithm encapsulated by this bucket
     */
    public Algorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * Stores the specified object in the encapsulated algorithm, i.e.
     * the InsertOperation is executed.
     *
     * @param object The new object to be inserted
     * @return error code - for details, see documentation of {@link messif.buckets.BucketErrorCode}
     * @throws BucketStorageException if there was an error storing the object to the encapsulated algorithm,
     *              e.g. the encapsulated algorithm does not support InsertOperation
     */
    @Override
    public boolean add(LocalAbstractObject object) throws BucketStorageException {
        InsertOperation operation = new InsertOperation(object);
        try {
            algorithm.executeOperation(operation);
        } catch (NoSuchMethodException e) {
            throw new StorageFailureException(e);
        } catch (AlgorithmMethodException e) {
            throw new StorageFailureException(e.getCause());
        }

        // Update object counter
        if (operation.wasSuccessful()) {
            objectCount++;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int addObjects(Iterator<? extends LocalAbstractObject> objects) throws BucketStorageException {

        BulkInsertOperation operation = new BulkInsertOperation(objects);
        try {
            algorithm.executeOperation(operation);
            objectCount += operation.getInsertedObjects().size();
        } catch (NoSuchMethodException e) {
            super.addObjects(operation.getInsertedObjects().iterator());
        } catch (AlgorithmMethodException e) {
            throw new StorageFailureException(e.getCause());
        }

        // Update object counter
        if (operation.wasSuccessful()) {
            return operation.getInsertedObjects().size();
        } else {
            return 0;
        }
    }

    /**
     * Removes the given object by calling {@link DeleteOperation} on the
     * encapsulated algorithm.
     *
     * @param object the object to delete
     * @throws BucketStorageException if the algorithm does not support delete operation or there was an error deleting the object
     */
    @Override
    public int deleteObject(LocalAbstractObject object, int deleteLimit) throws BucketStorageException {
        DeleteOperation operation;
        try {
            operation = algorithm.executeOperation(new DeleteOperation(object, deleteLimit));
        } catch (NoSuchMethodException e) {
            throw new StorageFailureException("Cannot delete object from algorithm, because DeleteOperation is not supported", e);
        } catch (AlgorithmMethodException e) {
            throw new StorageFailureException("DeleteOperation executed on " + algorithm.getName() + " failed", e.getCause());
        }

        // Update object count
        if (operation.wasSuccessful())
            objectCount -= operation.getObjects().size();
        return operation.getObjects().size();
    }

    /**
     * Removes the given object by calling {@link DeleteByLocatorOperation} on the
     * encapsulated algorithm.
     * @throws BucketStorageException if the algorithm does not support delete operation or there was an error deleting the object
     */
    @Override
    public synchronized int deleteObject(String locatorURI, int deleteLimit) throws BucketStorageException {
        DeleteByLocatorOperation operation;
        try {
            operation = algorithm.executeOperation(new DeleteByLocatorOperation(Collections.singleton(locatorURI), deleteLimit));
        } catch (NoSuchMethodException e) {
            throw new StorageFailureException("Cannot delete object from algorithm, because DeleteOperation is not supported", e);
        } catch (AlgorithmMethodException e) {
            throw new StorageFailureException("DeleteOperation executed on " + algorithm.getName() + " failed", e.getCause());
        }

        // Update object count
        if (operation.wasSuccessful())
            objectCount -= operation.getObjects().size();
        return operation.getObjects().size();
    }

    @Override
    public ModifiableSearch<LocalAbstractObject> search() throws IllegalStateException {
        return new AlgorithmStorageSearch<Object>(null, Collections.emptyList());
    }

    @Override
    public <C> ModifiableSearch<LocalAbstractObject> search(IndexComparator<? super C, ? super LocalAbstractObject> comparator, C key) throws IllegalStateException {
        return new AlgorithmStorageSearch<Object>(null, Collections.singletonList(key));
    }

    @Override
    public <C> ModifiableSearch<LocalAbstractObject> search(IndexComparator<? super C, ? super LocalAbstractObject> comparator, Collection<? extends C> keys) throws IllegalStateException {
        return new AlgorithmStorageSearch<C>(comparator, keys);
    }

    @Override
    public <C> ModifiableSearch<LocalAbstractObject> search(IndexComparator<? super C, ? super LocalAbstractObject> comparator, C from, C to) throws IllegalStateException {
        return new AlgorithmStorageSearch<C>(comparator, from, to);
    }

    /**
     * Internal class that provides the {@link ModifiableIndex} for the encapsulated algorithm.
     * @param <C> the type of keys of this search
     */
    private class AlgorithmStorageSearch<C> extends AbstractSearch<C, LocalAbstractObject> implements ModifiableSearch<LocalAbstractObject> {
        /** Iterator over objects returned as this search's answer */
        private final ListIterator<LocalAbstractObject> iterator;

        /**
         * Creates a new instance of AlgorithmStorageSearch.
         * During the constructor call, a search operation is executed on
         * the encapsulated algorithm.
         * @param comparator the comparator that is used to compare the keys
         * @param keys list of keys to search for
         * @throws IllegalStateException if there was a problem querying the encapsulated algorithm
         */
        private AlgorithmStorageSearch(IndexComparator<? super C, ? super LocalAbstractObject> comparator, Collection<? extends C> keys) throws IllegalStateException {
            super(comparator, keys);
            this.iterator = executeAlgorithmOperation(comparator, keys);
        }

        /**
         * Creates a new instance of AlgorithmStorageSearch for the specified search comparator and [from,to] bounds.
         * @param comparator the comparator that compares the <code>keys</code> with the stored objects
         * @param fromKey the lower bound on the searched keys
         * @param toKey the upper bound on the searched keys
         */
        @SuppressWarnings("unchecked")
        private AlgorithmStorageSearch(IndexComparator<? super C, ? super LocalAbstractObject> comparator, C fromKey, C toKey) {
            super(comparator, fromKey, toKey);
            this.iterator = executeAlgorithmOperation(comparator, Arrays.asList(fromKey, toKey));
        }

        /**
         * Creates an operation to execute on the encapsulated algorithm for the specified comparator and keys.
         * @param comparator the comparator to use for the query definition
         * @param keys the list of keys the operation searches for
         * @return a new instance of query operation for the given key
         */
        @SuppressWarnings("unchecked")
        protected final ListIterator<LocalAbstractObject> executeAlgorithmOperation(IndexComparator<? super C, ? super LocalAbstractObject> comparator, Collection<? extends C> keys) {
            // Prepare operation for the given comparator
            QueryOperation<?> operation;
            if (comparator != null && !keys.isEmpty() && comparator instanceof OperationIndexComparator)
                operation = ((OperationIndexComparator<C>)comparator).createIndexOperation(keys); // This cast IS checked because the OperationIndexComparator is always a subtype
            else
                operation = new GetAllObjectsQueryOperation(AnswerType.ORIGINAL_OBJECTS);

            // Execute operation to get objects from the algorithm
            operation = executeOperation(operation);

            // Read results into a list
            List<LocalAbstractObject> list = new ArrayList<LocalAbstractObject>(operation.getAnswerCount());
            Iterator<AbstractObject> answer = operation.getAnswerObjects();
            while (answer.hasNext()) {
                LocalAbstractObject object = (LocalAbstractObject)answer.next();
                list.add(object);
            }

            return list.listIterator();
        }

        /**
         * Executes a query operation on the encapsulated algorithm and wraps
         * the query answer into a list.
         * @param operation the operation to execute on the algorithm
         * @return the operation executed by the algorithm
         * @throws IllegalStateException if there was a problem executing the operation on the encapsulated algorithm
         */
        protected QueryOperation<?> executeOperation(QueryOperation<?> operation) throws IllegalStateException {
            // Execute operation
            try {
                return algorithm.executeOperation(operation);
            } catch (AlgorithmMethodException e) {
                throw new IllegalStateException("Cannot execute " + operation.getClass().getName() + " on " + algorithm.getName(), e.getCause());
            } catch (NoSuchMethodException e) { // Specified query is not supported, fall back to get-all-objects
                // If the operation already is the fallback, bail out
                if (operation instanceof GetAllObjectsQueryOperation)
                    throw new IllegalStateException("GetAllObjects operation must be supported by " + algorithm.getName() + " in order to be wrapped as algorithm-bucket");
                // Fall-back operation
                return executeOperation(new GetAllObjectsQueryOperation());
            }
        }

        @Override
        protected LocalAbstractObject readNext() throws BucketStorageException {
            return iterator.hasNext()?iterator.next():null;
        }

        @Override
        protected LocalAbstractObject readPrevious() throws BucketStorageException {
            return iterator.hasPrevious()?iterator.previous():null;
        }

        @Override
        public void remove() throws IllegalStateException, BucketStorageException {
            LocalAbstractObject object = getCurrentObject();
            if (object == null)
                throw new IllegalStateException("There is no object to delete yet");
            deleteObject(object);
        }

        @Override
        public void close() {
        }

    }


    //****************** Special bucket methods overrides ******************//

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
     * @throws UnsupportedOperationException if the specified query is not supported by the encapsulated algorithm
     */
    @Override
    public int processQuery(QueryOperation query) throws UnsupportedOperationException {
        int beforeCount = query.getAnswerCount();
        try {
            QueryOperation clonedQuery = query.clone(true);
            clonedQuery.clearSurplusData();
            algorithm.executeOperation(clonedQuery);
            return query.getAnswerCount() - beforeCount;
        } catch (NoSuchMethodException e) {
            return super.processQuery(query);
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException(e);
        } catch (AlgorithmMethodException e) {
            throw new UnsupportedOperationException(e.getCause());
        }
    }


    //****************** Splitting ******************//

    /** Internal class for capturing split results */
    private class SplitResult implements  SplittableAlgorithm.SplittableAlgorithmResult, Removable<LocalAbstractObject>, Addible<LocalAbstractObject> {
        /** Table of created AlgorithmStorageBuckets for encapsulated algorithms */
        private Map<Algorithm, AlgorithmStorageBucket> bucketMap = new HashMap<Algorithm, AlgorithmStorageBucket>();
        /** Bucket dispatcher that is used to create the encapsulating buckets */
        private final BucketDispatcher bucketCreator;
        /** Number of objects moved */
        private int objectsMoved = 0;

        /**
         * Creates a new instance of SplitResult.
         * @param bucketCreator the bucket dispatcher that is used to create the encapsulating buckets
         */
        private SplitResult(BucketDispatcher bucketCreator) {
            this.bucketCreator = bucketCreator;
        }

        @Override
        public void markMovedObjects(Algorithm alg, Collection<? extends LocalAbstractObject> objects) throws BucketStorageException, InstantiationException {
            // Call single addition for all objects
            for (LocalAbstractObject object : objects)
                markMovedObject(alg, object);
        }

        @Override
        public void markMovedObject(Algorithm alg, LocalAbstractObject object) throws BucketStorageException, InstantiationException {
            // Get the existing bucket for the specified algorithm
            AlgorithmStorageBucket bucket = bucketMap.get(alg);
            if (bucket == null) {
                // Bucket for the specified algorithm doesn't exist yet, create a AlgorithmStorageBucket wrapper
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("algorithm", alg);
                bucketMap.put(alg, bucket = (AlgorithmStorageBucket)bucketCreator.createBucket(AlgorithmStorageBucket.class, params));
            }

            currentMovedObject = object;
            AlgorithmStorageBucket.this.deleteObject(this);
            bucket.addObject(object, this);
            objectsMoved++;
        }

        /**
         * Returns the number of objects moved
         * @return the number of objects moved
         */
        public int getObjectsMoved() {
            return objectsMoved;
        }

        /**
         * Returns the encapsulating {@link AlgorithmStorageBucket}s created during the split.
         * @return the encapsulating {@link AlgorithmStorageBucket}s created during the split
         */
        public Collection<AlgorithmStorageBucket> getCreatedBuckets() {
            return bucketMap.values();
        }


        //****************** Empty implementation of Addible & Removable interface ******************//

        /** Currently moved object (set from {@link #markMovedObject}) */
        private LocalAbstractObject currentMovedObject;

        @Override
        public LocalAbstractObject getCurrentObject() throws NoSuchElementException {
            return currentMovedObject;
        }

        @Override
        public void remove() throws IllegalStateException, BucketStorageException {
        }

        @Override
        public boolean add(LocalAbstractObject object) throws BucketStorageException {
            return true;
        }
    }

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
     * @throws BucketStorageException if there was a storage error (capacity overflow/underflow or filter reject) during split
     */
    @Override
    public synchronized int split(SplitPolicy policy, final List<Bucket> targetBuckets, final BucketDispatcher bucketCreator, int whoStays) throws IllegalArgumentException, BucketStorageException {
        if (!(algorithm instanceof SplittableAlgorithm) || bucketCreator == null)
            return super.split(policy, targetBuckets, bucketCreator, whoStays);

        // Prepare the split result object
        SplitResult result = new SplitResult(bucketCreator);

        // Execute the split
        ((SplittableAlgorithm)algorithm).split(policy, result, whoStays);

        // Add created buckets
        if (targetBuckets != null)
            targetBuckets.addAll(result.getCreatedBuckets());

        return result.getObjectsMoved();
    }

}

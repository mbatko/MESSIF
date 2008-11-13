/*
 * InterfaceStorageBucket.java
 *
 * Created on 21. unor 2005, 19:39
 */

package messif.buckets.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import messif.buckets.BucketErrorCode;
import messif.buckets.LocalBucket;
import messif.buckets.LocalFilteredBucket;
import messif.objects.LocalAbstractObject;
import messif.objects.UniqueID;
import messif.objects.util.AbstractObjectIterator;
import messif.operations.QueryOperation;
import messif.utility.Convert;


/**
 * This is a LocalBucket storage implementation stub for anything
 * that implements BucketInterface.
 *
 * @see BucketInterface
 * @author xbatko
 */
public class InterfaceStorageBucket extends LocalFilteredBucket {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;    

    /****************** Local data ******************/

    /** Encapsulate interface that provides the functionality */
    protected final BucketInterface stub;


    /****************** Constructors ******************/

    /**
     * Creates a new instance of AlgorithmStorageBucket and setups all bucket limits
     *
     * @param stub encapsulated interface that will actually do the job
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     */
    public InterfaceStorageBucket(BucketInterface stub, long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes) {
        super(capacity, softCapacity, lowOccupation, occupationAsBytes);
        this.stub = stub;
    }

    
    /***************** Factory method  *****************************************/
    
    /**
     * Creates a new algorithm bucket. The parameters for the algorithm constructor are specified in the parameters map.
     * 
     * Recognized parameters:
     *   class   - the class implementing the BucketInterface, which is to be created
     *   param.1 - the first argument for the algorithm constructor
     *   param.2 - etc.
     * 
     * @param capacity maximal capacity of the bucket - cannot be exceeded
     * @param softCapacity maximal soft capacity of the bucket
     * @param lowOccupation a minimal occupation for deleting objects - cannot be lowered
     * @param occupationAsBytes flag whether the occupation (and thus all the limits) are in bytes or number of objects
     * @param parameters list of named parameters - this bucket supports "file" and "path" (see above)
     * @throws java.lang.IllegalArgumentException if the parameters from the <code>parameters</code> map are invalid and the interface stub cannot be created
     * @return a new DiskBucket instance
     */
    protected static InterfaceStorageBucket getBucket(long capacity, long softCapacity, long lowOccupation, boolean occupationAsBytes, Map<String, Object> parameters) throws IllegalArgumentException {
        // Get class parameter
        Class<? extends BucketInterface> algClass;
        try {
            algClass = Convert.genericCastToClass(parameters.get("class"), BucketInterface.class);
            if (algClass == null)
                throw new IllegalArgumentException("The parameters map must contain key 'class' implementing the BucketInterface");
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("The parameters map must contain key 'class' implementing the BucketInterface");
        }

        // Read parameters
        List<Object> algParams = new ArrayList<Object>(parameters.size() - 1);
        Object param;
        while ((param = parameters.get("param."+(algParams.size()+1))) != null)
            algParams.add(param);

        // Create instance
        try {
            BucketInterface alg = Convert.createInstanceWithInheritableArgs(algClass, algParams.toArray());
            return new InterfaceStorageBucket(alg, capacity, softCapacity, lowOccupation, occupationAsBytes);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("The class "+algClass+ " does not contain constructor for given parameters "+algParams);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Can't create bucket interface '" + algClass.getName() + "' for InterfaceStorageBucket", e.getCause());
        }        
    }
       

    /****************** Implementations using stub instance ******************/

    /**
     * Stores the specified object through the encapsulated interface
     *
     * @param object The new object to be inserted
     * @return error code - for details, see documentation of {@link BucketErrorCode}
     */
    protected BucketErrorCode storeObject(LocalAbstractObject object) {
        return stub.addObject(object);
    }

    /**
     * Process a query operation on the encapsulated bucket interface.
     * The query operation's answer is updated with objects that satisfy the query.
     *
     * @param query query operation that is to be processed on this bucket
     * @return the number of objects that were added to answer
     */
    @Override
    public int processQuery(QueryOperation query) {
        return stub.processQuery(query);
    }

    /**
     * Returns current number of objects stored in bucket.
     * @return current number of objects stored in bucket
     */
    public int getObjectCount() {
        return stub.getObjectCount();
    }

    /**
     * Returns iterator through all the objects in this bucket.
     * @return iterator through all the objects in this bucket
     */
    protected LocalBucketIterator<? extends InterfaceStorageBucket> iterator() {
        return new InterfaceStorageBucketIterator<InterfaceStorageBucket>(this);
    }

    /****************** Iterator object ******************/

    /**
     * Internal class for iterator implementation
     * @param <T> the type of the bucket this iterator operates on
     */
    protected static class InterfaceStorageBucketIterator<T extends InterfaceStorageBucket> extends LocalBucket.LocalBucketIterator<T> {
        /** Currently executed iterator */
        protected final AbstractObjectIterator<LocalAbstractObject> iterator;

        /**
         * Creates a new instance of InterfaceStorageBucketIterator with the InterfaceStorageBucket.
         * This constructor is intended to be called only from InterfaceStorageBucket class.
         *
         * @param bucket actual instance of InterfaceStorageBucket on which this iterator should work
         */
        protected InterfaceStorageBucketIterator(T bucket) {
           super(bucket);
           iterator = bucket.stub.getAllObjects();
        }

        /**
         * Get an object with specified ID from this bucket via the encapsulated interface.
         *
         * @param objectID ID of the object to retrieve
         * @return object with specified ID from this bucket
         * @throws NoSuchElementException if there is no object with the specified ID in this bucket
         */
        @Override
        public LocalAbstractObject getObjectByID(UniqueID objectID) throws NoSuchElementException {       
            return iterator.getObjectByID(objectID);
        }

        /** 
         * Physically removes the last object returned by this iterator via the encapsulated interface.
         * 
         * @throws NoSuchElementException if next or getObjectByID was not called before
         */
        protected void removeInternal() throws NoSuchElementException {
            iterator.remove();
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration.
         * @throws NoSuchElementException iteration has no more elements.
         */
        public LocalAbstractObject next() throws NoSuchElementException {
            return iterator.next();
        }

        /**
         * Returns <tt>true</tt> if the iteration has more elements. (In other
         * words, returns <tt>true</tt> if <tt>next</tt> would return an element
         * rather than throwing an exception.)
         *
         * @return <tt>true</tt> if the iterator has more elements.
         */
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /**
         * Returns the object returned by the last call to next().
         * @return the object returned by the last call to next()
         * @throws NoSuchElementException if next() has not been called yet
         */
        public LocalAbstractObject getCurrentObject() throws NoSuchElementException {
            return iterator.getCurrentObject();
        }
        
    }    
}

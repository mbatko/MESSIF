/*
 * BucketInterface.java
 *
 * Created on 21. unor 2005, 14:46
 */

package messif.buckets;

import messif.objects.GenericAbstractObjectIterator;
import messif.objects.LocalAbstractObject;
import messif.operations.QueryOperation;

/**
 * This interface allows any algorithm, structure, or storage to implement
 * the basic bucket operations.
 * Such a structure then can be used in <code>InterfaceLocalBucket</code>
 * stub to support internals of a bucket.
 *
 * @see InterfaceStorageBucket
 *
 * @author xbatko
 */
public interface BucketInterface {

    /**
     * Insert a new object into the bucket.
     * 
     * @return error code - for details, see documentation of {@link BucketErrorCode}
     * @param object A new object to be inserted
     */
    public BucketErrorCode addObject(LocalAbstractObject object);

    /**
     * Returns current number of objects stored in bucket.
     * @return current number of objects stored in bucket
     */
    public int getObjectCount();

    /**
     * Returns iterator over all objects from this bucket.
     * Method <tt>getObjectByID</tt> will be called when accessing object by ID.
     * Method <tt>remove</tt> will be called when deleting objects.
     *
     * @return iterator over all objects from this bucket
     */
    public GenericAbstractObjectIterator<LocalAbstractObject> getAllObjects();

    /**
     * Process a query operation on objects from this bucket.
     * The query operation's answer is updated with objects from this bucket
     * that satisfy the query.
     *
     * @param query query operation that is to be processed on this bucket
     * @return the number of objects that were added to answer
     */
    public int processQuery(QueryOperation query);

}

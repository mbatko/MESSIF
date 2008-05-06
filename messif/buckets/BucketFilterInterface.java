/*
 * BucketFilterInterface.java
 *
 * Created on 24. duben 2004, 12:09
 */

package messif.buckets;

import messif.objects.LocalAbstractObject;

/**
 *  The BucketFilter class allows filtering of add and delete operations in
 *  a generic local bucket. A new filter is appended to the LocalBucket
 *  internal chain of filters in the constructor. To remove a filter call its
 *  deregister method. 
 * 
 *  A filter without the association to the bucket may be created using the
 *  no-argument constructor. Such a filter is not linked in a bucket's chain
 *  of filters. The association can be created later by calling register method.
 *
 * @author  xbatko
 */
public interface BucketFilterInterface {

    /****************** Constants ******************/

    /** Situations for which can be installed filter */
    public static enum FilterSituations {
        /** Filter before object insertion, exception can be thrown to abort insertion */
        BEFORE_ADD,
        /** Filter after object insertion, object is already stored in the bucket */
        AFTER_ADD,
        /** Filter before object deletion, exception can be thrown to abort deletion */
        BEFORE_DEL,
        /** Filter after object deletion, object is already deleted from the bucket */
        AFTER_DEL
    }


    /****************** Filtered operations ******************/

    /**
     * Filter the bucket insertion/deletion operation
     *
     * @param object the inserted/deleted object
     * @param situation actual situation in which the method is called (see FilterSituations constants for detailed description)
     * @param inBucket bucket, where the object will be/was stored
     * @throws FilterRejectException if the current operation should be aborted (should be thrown only in BEFORE situations)
     */
    public abstract void filterObject(LocalAbstractObject object, FilterSituations situation, LocalFilteredBucket inBucket) throws FilterRejectException;

}

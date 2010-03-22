/*
 *  BucketFilterBeforeAdd
 * 
 */

package messif.buckets;

import messif.objects.LocalAbstractObject;

/**
 * Implements a filter used before an object is inserted into a bucket.
 * 
 * @author xbatko
 */
public interface BucketFilterBeforeAdd extends BucketFilter {
    /**
     * Filter object before its insertion into a bucket.
     *
     * @param object the inserted object
     * @param bucket bucket, where the object will be stored
     * @throws FilterRejectException if the object insertion is aborted
     */
    void filterBeforeAdd(LocalAbstractObject object, LocalBucket bucket) throws FilterRejectException;
}

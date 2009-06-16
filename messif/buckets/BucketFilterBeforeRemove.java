/*
 *  BucketFilterBeforeAdd
 * 
 */

package messif.buckets;

import messif.objects.LocalAbstractObject;

/**
 * Implements a filter used before an object is removed from a bucket.
 *
 * @author xbatko
 */
public interface BucketFilterBeforeRemove extends BucketFilter {
    /**
     * Filter object before its removal from a bucket.
     *
     * @param object the removed object
     * @param bucket bucket, where the object is stored
     * @throws FilterRejectException if the object removal is aborted
     */
    void filterBeforeRemove(LocalAbstractObject object, LocalBucket bucket) throws FilterRejectException;
}

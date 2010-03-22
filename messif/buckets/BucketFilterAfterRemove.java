/*
 *  BucketFilterBeforeAdd
 * 
 */

package messif.buckets;

import messif.objects.LocalAbstractObject;

/**
 * Implements a filter used after an object was removed from a bucket.
 *
 * @author xbatko
 */
public interface BucketFilterAfterRemove extends BucketFilter {
    /**
     * Filter object after its removal from a bucket.
     *
     * @param object the removed object
     * @param bucket bucket, where the object has been stored
     */
    void filterAfterRemove(LocalAbstractObject object, LocalBucket bucket);
}

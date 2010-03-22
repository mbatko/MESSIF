/*
 *  BucketFilterBeforeAdd
 * 
 */

package messif.buckets;

import messif.objects.LocalAbstractObject;

/**
 * Implements a filter used after an object was inserted into a bucket.
 *
 * @author xbatko
 */
public interface BucketFilterAfterAdd extends BucketFilter {
    /**
     * Filter object after its insertion into a bucket.
     *
     * @param object the inserted object
     * @param bucket bucket, where the object is stored
     */
    void filterAfterAdd(LocalAbstractObject object, LocalBucket bucket);
}

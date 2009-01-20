/*
 * BucketRemoveRequestMessage.java
 *
 * Created on 4. kveten 2003, 13:50
 */

package messif.netbucket;

import messif.buckets.BucketDispatcher;

/**
 *
 * @author  xbatko
 */
public class BucketRemoveRequestMessage extends BucketRequestMessage<BucketRemoveReplyMessage> {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constructor ******************//
    
    /**
     * Creates a new instance of BucketRemoveRequestMessage.
     * @param bucketID the ID of a remote bucket to remove
     */
    public BucketRemoveRequestMessage(int bucketID) {
        super(bucketID);
    }
    
    //****************** Executing the request ******************//

    @Override
    public BucketRemoveReplyMessage execute(BucketDispatcher bucketDispatcher) {
        return new BucketRemoveReplyMessage(this, bucketDispatcher.removeBucket(bucketID) != null);
    }

}

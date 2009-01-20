/*
 * MessageBucketCreateResponse.java
 *
 * Created on 4. kveten 2003, 13:50
 */

package messif.netbucket;

import messif.buckets.BucketDispatcher;
import messif.buckets.BucketStorageException;
import messif.buckets.LocalBucket;

/**
 * Message for requesting creation of a remote bucket.
 * @author xbatko
 */
public class BucketCreateRequestMessage extends BucketRequestMessage<BucketCreateReplyMessage> {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Constructor ******************//

    /**
     * Creates a new instance of BucketCreateRequestMessage.
     */
    public BucketCreateRequestMessage() {
        super(0);
    }

    //****************** Executing the request ******************//

    @Override
    public BucketCreateReplyMessage execute(BucketDispatcher bucketDispatcher) throws BucketStorageException {
        LocalBucket bucket = bucketDispatcher.createBucket();
        return new BucketCreateReplyMessage(this, bucket.getBucketID(), bucket.getCapacity());
    }

}

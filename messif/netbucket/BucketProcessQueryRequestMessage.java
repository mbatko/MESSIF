/*
 *  BucketProcessQueryRequestMessage
 * 
 */

package messif.netbucket;

import messif.buckets.BucketDispatcher;
import messif.buckets.BucketStorageException;
import messif.buckets.LocalBucket;
import messif.operations.QueryOperation;

/**
 * Message requesting to process a query on a remote bucket.
 * 
 * @author xbatko
 * @see NetworkBucketDispatcher
 */
public class BucketProcessQueryRequestMessage extends BucketRequestMessage<BucketProcessQueryReplyMessage> {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;

    //****************** Attributes ******************//

    /** Query operation to process on a remote bucket */
    private final QueryOperation query;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of BucketProcessQueryRequestMessage.
     * @param bucketID the ID of a remote bucket on which to process the request
     * @param query the query operation to process on a remote bucket
     */
    public BucketProcessQueryRequestMessage(int bucketID, QueryOperation query) {
        super(bucketID);
        this.query = query;
    }


    //****************** Executing the request ******************//

    @Override
    public BucketProcessQueryReplyMessage execute(BucketDispatcher bucketDispatcher) throws RuntimeException, BucketStorageException {
        bucketDispatcher.getBucket(bucketID).processQuery(query);
        return new BucketProcessQueryReplyMessage(this, query);
    }

}

/*
 * BucketRequestMessage.java
 *
 */


package messif.netbucket;

import messif.buckets.BucketDispatcher;
import messif.buckets.BucketStorageException;
import messif.network.Message;


/**
 * Generic message for requesting an object manipulation on a remote bucket.
 *
 * @param <T> the type of reply that is expected as a result for this request
 * @author xbatko
 */
public abstract class BucketRequestMessage<T extends BucketReplyMessage> extends Message {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 3L;

    //****************** Attributes ******************//

    /** ID of a remote bucket on which to process the request */
    protected final int bucketID;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of BucketRequestMessage.
     * @param bucketID the ID of a remote bucket on which to process the request
     */
    protected BucketRequestMessage(int bucketID) {
        this.bucketID = bucketID;
    }


    //****************** Executing the request ******************//

    /**
     * Executes this request on the specified bucket dispatcher.
     * This method is intended to be used on the destination peer where the bucket is kept.
     * @param bucketDispatcher the dispatcher that can provide the bucket of for the request
     * @return the reply message with the result of the processing
     * @throws RuntimeException if there was an error processing this request
     * @throws BucketStorageException if there was an error processing this request
     */
    public abstract T execute(BucketDispatcher bucketDispatcher) throws RuntimeException, BucketStorageException;

}

/*
 * BucketCreateReplyMessage.java
 *
 * Created on 4. kveten 2003, 13:50
 */

package messif.netbucket;

import messif.buckets.CapacityFullException;

/**
 * Message for returning results of a remote bucket creation.
 * @author  xbatko
 */
public class BucketCreateReplyMessage extends BucketReplyMessage {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 2L;

    //****************** Attributes ******************//

    /** ID of the bucket that was created on the remote network node */
    private final int bucketID;
    /** Capacity of the created bucket */
    private final long capacity;


    //****************** Constructor ******************//
    
    /**
     * Creates a new instance of BucketCreateReplyMessage for the supplied data.
     *
     * @param message the original message this message is response to
     * @param bucketID the ID of the bucket that was created on the remote network node
     * @param capacity the capacity of the created bucket
     */
    public BucketCreateReplyMessage(BucketCreateRequestMessage message, int bucketID, long capacity) {
        super(message);
        this.bucketID = bucketID;
        this.capacity = capacity;
    }


    //****************** Attribute access methods ******************//

    /**
     * Returns the remote bucket encapsulation for the newly created bucket.
     * @param netbucketDisp the network bucket dispatcher that will handle the remote bucket's processing
     * @return the remote bucket encapsulation for the newly created bucket
     * @throws CapacityFullException if the bucket was not created on the remote network node
     */
    public RemoteBucket getRemoteBucket(NetworkBucketDispatcher netbucketDisp) throws CapacityFullException {
        if (bucketID == 0)
            throw new CapacityFullException();
        return new RemoteBucket(netbucketDisp, bucketID, getSender(), capacity);
    }

}

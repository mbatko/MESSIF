/*
 * BucketCreateReplyMessage.java
 *
 * Created on 4. kveten 2003, 13:50
 */

package messif.netbucket;

import messif.network.Message;
import messif.network.NetworkNode;
import messif.network.ReplyMessage;

/**
 *
 * @author  xbatko
 */
public class BucketCreateReplyMessage extends ReplyMessage {
    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;   

    /****************** Message extensions ******************/
    protected final int bucketID;
    protected final NetworkNode remoteNetworkNode;
    protected final long capacity;

    public RemoteBucket getBucket(NetworkBucketDispatcher netbucketDisp) {
        return new RemoteBucket(netbucketDisp, bucketID, remoteNetworkNode, capacity);
    }
    
    public boolean isSuccess() {
        return remoteNetworkNode != null && bucketID != 0;
    }
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of MessageInitUse from supplied data */
    public BucketCreateReplyMessage(Message responseToMessage) {
        super(responseToMessage);
        this.bucketID = 0;
        this.remoteNetworkNode = null;
        this.capacity = 0;
    }
    
    /** Creates a new instance of MessageInitUse from supplied data */
    public BucketCreateReplyMessage(int bucketID, NetworkNode remoteNetworkNode, long capacity, Message responseToMessage) {
        super(responseToMessage);
        this.bucketID = bucketID;
        this.remoteNetworkNode = remoteNetworkNode;
        this.capacity = capacity;
    }
       
}

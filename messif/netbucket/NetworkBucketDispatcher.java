/*
 * BucketStorage.java
 *
 * Created on 1. kveten 2004, 18:47
 */

package messif.netbucket;

import java.io.IOException;
import java.util.Map;
import messif.utility.Logger;
import messif.buckets.BucketDispatcher;
import messif.buckets.CapacityFullException;
import messif.buckets.LocalBucket;
import messif.buckets.impl.MemoryStorageBucket;
import messif.network.MessageDispatcher;
import messif.network.NetworkNode;
import messif.network.ThreadInvokingReceiver;
import messif.statistics.Statistics;

/**
 *
 * @author  xbatko
 */
public class NetworkBucketDispatcher extends BucketDispatcher {
    
    /** Class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Logger */
    protected static Logger log = Logger.getLoggerEx("messif.netbucket");
    
    /****************** Internal variables ******************/
    protected transient MessageDispatcher messageDisp;
    protected transient ThreadInvokingReceiver receiver;
    
    public NetworkNode getCurrentNetworkNode() {
        return messageDisp.getNetworkNode();
    }
    
    
    /****************** Constructors ******************/
    
    /**
     * Creates a new instance of NetworkBucketDispatcher
     */
    public NetworkBucketDispatcher(MessageDispatcher messageDisp, int maxBuckets, long bucketCapacity) throws InstantiationException {
        this(messageDisp, maxBuckets, bucketCapacity, bucketCapacity, 0, true, MemoryStorageBucket.class);
    }

    /**
     * Creates a new instance of NetworkBucketDispatcher
     */
    public NetworkBucketDispatcher(MessageDispatcher messageDisp, int maxBuckets, long bucketCapacity, long bucketSoftCapacity, long bucketLowOccupation, boolean bucketOccupationAsBytes, Class<? extends LocalBucket> defaultBucketClass) throws InstantiationException {
        super(maxBuckets, bucketCapacity, bucketSoftCapacity, bucketLowOccupation, bucketOccupationAsBytes, defaultBucketClass);
        
        setMessageDispatcher(messageDisp);
    }

    /**
     * Creates a new instance of NetworkBucketDispatcher
     */
    public NetworkBucketDispatcher(MessageDispatcher messageDisp, int maxBuckets, long bucketCapacity, long bucketSoftCapacity, long bucketLowOccupation, boolean bucketOccupationAsBytes, Class<? extends LocalBucket> defaultBucketClass, Map<String, Object> defaultBucketClassParams) throws InstantiationException {
        super(maxBuckets, bucketCapacity, bucketSoftCapacity, bucketLowOccupation, bucketOccupationAsBytes, defaultBucketClass, defaultBucketClassParams);
        
        setMessageDispatcher(messageDisp);
    }

    /** Set the message dispatcher and create and register the receiver.
     *   This method is called from constructor or from the outside - after deserialization */
    public void setMessageDispatcher(MessageDispatcher messageDisp) throws InstantiationException {
        this.messageDisp = messageDisp;
        
        // Create thread receiver for messages and start receiving
        receiver = new ThreadInvokingReceiver(this, "receive");
        messageDisp.registerReceiver(receiver);
    }
    
    /****************** Bucket creation messageing ******************/
    
    public RemoteBucket createRemoteBucket(NetworkNode atNetworkNode) throws CapacityFullException {
        // Failed, current local storage exhausted, use remote server
        try {
            BucketCreateReplyMessage msg = (BucketCreateReplyMessage)messageDisp.sendMessageWaitReply(
                    new BucketCreateRequestMessage(), atNetworkNode
                    ).getFirstReply();
            
            if (msg.isSuccess())
                return msg.getBucket(this);
        } catch (IOException e) {
            throw new CapacityFullException("Can't create remote bucket: " + e.getMessage());
        } catch (ClassCastException e) {
            throw new CapacityFullException("Server response has wrong message type");
        }
        
        throw new CapacityFullException();
    }
    
    /** "Create bucket" message recieving */
    protected boolean receive(BucketCreateRequestMessage msg) throws IOException {
        try {
            // Create bucket and send positive response
            LocalBucket bucket = createBucket();
            messageDisp.replyMessage(new BucketCreateReplyMessage(bucket.getBucketID(), getCurrentNetworkNode(), bucket.getCapacity(), msg));
            log.info("Created bucket from " + msg.getSender());
        } catch (CapacityFullException e) {
            // Send negative response
            messageDisp.replyMessage(new BucketCreateReplyMessage(msg));
            log.info("Create bucket from " + msg.getSender() + " failed - Server full");
        } catch (InstantiationException e) {
            // Send negative response
            messageDisp.replyMessage(new BucketCreateReplyMessage(msg));
            log.severe(e);
        }
        
        return true; // Accept message
    }
    
    
    /****************** Bucket deletion messageing ******************/
    
    public boolean removeRemoteBucket(RemoteBucket remoteBucket) throws IOException {
        BucketDeleteReplyMessage msg = (BucketDeleteReplyMessage)messageDisp.sendMessageWaitReply(
                new BucketDeleteRequestMessage(remoteBucket.getBucketID()), remoteBucket.getRemoteNetworkNode()
                ).getFirstReply();
        
        return msg.isDeleted();
    }
    
    /** "Delete bucket" message recieving */
    protected boolean receive(BucketDeleteRequestMessage msg) throws IOException {
        LocalBucket bucket = removeBucket(msg.getBucketID());
        
        // Reply with
        messageDisp.replyMessage(new BucketDeleteReplyMessage(msg, bucket != null));
        log.info("Deleted bucket ID " + msg.bucketID + " from " + msg.getSender());
        
        return true; // Accept message
    }
    
    
    /****************** Remote object messaging ******************/
    
    /** "Object" message sending */
    protected BucketManipulationReplyMessage sendMessageWaitReply(BucketManipulationRequestMessage msg, NetworkNode remoteNetworkNode) throws IOException {
        return (BucketManipulationReplyMessage)messageDisp.sendMessageWaitReply(msg, remoteNetworkNode).getFirstReply();
    }
    
    /** "Object" message recieving */
    public void receive(BucketManipulationRequestMessage msg) throws IOException {
        Statistics navigElDistComp = null;
        try {
            // Setup statistics
            msg.registerBoundStat("DistanceComputations");

            // Process message and send reply
            messageDisp.replyMessage(msg.execute(this));
        } catch (InstantiationException e) {
            log.severe("There is no DistanceComputations statistics global counter yet!?!");
        } finally {
            if (navigElDistComp != null)
                navigElDistComp.unbind();
        }
    }
    
    
}

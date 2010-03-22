/*
 * BucketStorage.java
 *
 * Created on 1. kveten 2004, 18:47
 */

package messif.netbucket;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import messif.buckets.BucketStorageException;
import messif.buckets.BucketDispatcher;
import messif.buckets.LocalBucket;
import messif.buckets.impl.MemoryStorageBucket;
import messif.network.MessageDispatcher;
import messif.network.NetworkNode;
import messif.network.ThreadInvokingReceiver;

/**
 *
 * @author  xbatko
 */
public class NetworkBucketDispatcher extends BucketDispatcher {
    /** Class id for serialization */
    private static final long serialVersionUID = 1L;


    //****************** Attributes ******************//

    /** Message dispatcher associated with this network bucket dispatcher */
    private final MessageDispatcher messageDisp;
    /** Receiver for receiving bucket request messages */
    private transient ThreadInvokingReceiver receiver;


    //****************** Constructors ******************//

    /**
     * Creates a new instance of NetworkBucketDispatcher with full specification of default values.
     *
     * @param messageDisp the message dispatcher used by this network bucket dispatcher
     * @param maxBuckets the maximal number of buckets maintained by this dispatcher
     * @param bucketCapacity the default bucket hard capacity for newly created buckets
     * @param bucketSoftCapacity the default bucket soft capacity for newly created buckets
     * @param bucketLowOccupation the default bucket hard low-occupation for newly created buckets
     * @param bucketOccupationAsBytes the default flag whether to store occupation & capacity in bytes (<tt>true</tt>) or number of objects (<tt>false</tt>) for newly create buckets
     * @param defaultBucketClass the default class for newly created buckets
     * @param defaultBucketClassParams the default parameters for newly created buckets with default bucket class
     */
    public NetworkBucketDispatcher(MessageDispatcher messageDisp, int maxBuckets, long bucketCapacity, long bucketSoftCapacity, long bucketLowOccupation, boolean bucketOccupationAsBytes, Class<? extends LocalBucket> defaultBucketClass, Map<String, Object> defaultBucketClassParams) {
        super(maxBuckets, bucketCapacity, bucketSoftCapacity, bucketLowOccupation, bucketOccupationAsBytes, defaultBucketClass, defaultBucketClassParams);
        this.messageDisp = messageDisp;
        startReceiving();
    }

    /**
     * Creates a new instance of NetworkBucketDispatcher with full specification of default values.
     * No additional parameters for the default bucket class are specified.
     *
     * @param messageDisp the message dispatcher used by this network bucket dispatcher
     * @param maxBuckets the maximal number of buckets maintained by this dispatcher
     * @param bucketCapacity the default bucket hard capacity for newly created buckets
     * @param bucketSoftCapacity the default bucket soft capacity for newly created buckets
     * @param bucketLowOccupation the default bucket hard low-occupation for newly created buckets
     * @param bucketOccupationAsBytes the default flag whether to store occupation & capacity in bytes (<tt>true</tt>) or number of objects (<tt>false</tt>) for newly create buckets
     * @param defaultBucketClass the default class for newly created buckets
     */
    public NetworkBucketDispatcher(MessageDispatcher messageDisp, int maxBuckets, long bucketCapacity, long bucketSoftCapacity, long bucketLowOccupation, boolean bucketOccupationAsBytes, Class<? extends LocalBucket> defaultBucketClass) {
        super(maxBuckets, bucketCapacity, bucketSoftCapacity, bucketLowOccupation, bucketOccupationAsBytes, defaultBucketClass);
        this.messageDisp = messageDisp;
        startReceiving();
    }

    /**
     * Creates a new instance of NetworkBucketDispatcher only with a maximal capacity specification.
     * The soft capacity and low-occupation limits are not set. The occupation and capacity
     * is counted in bytes. The {@link MemoryStorageBucket} is used as default bucket class.
     *
     * @param messageDisp the message dispatcher used by this network bucket dispatcher
     * @param maxBuckets the maximal number of buckets maintained by this dispatcher
     * @param bucketCapacity the default bucket hard capacity for newly created buckets
     */
    public NetworkBucketDispatcher(MessageDispatcher messageDisp, int maxBuckets, long bucketCapacity) {
        super(maxBuckets, bucketCapacity);
        this.messageDisp = messageDisp;
        startReceiving();
    }


    //****************** Messaging inicialization methods ******************//

    /**
     * Creates a receiver for bucket messages and registers it in the message dispatcher.
     * @throws IllegalStateException if this network bucket dispatcher has a receiver associated already
     */
    protected synchronized void startReceiving() throws IllegalStateException {
        if (receiver != null)
            throw new IllegalStateException("Network bucket dispatcher already has a receiver associated");

        // Create thread receiver for messages and start receiving
        receiver = new ThreadInvokingReceiver(this, "receive");
        messageDisp.registerReceiver(receiver);
    }

    /**
     * Removes the associated receiver for bucket messages.
     * If this network bucket dispatcher has no receiver associated yet,
     * this method does nothing.
     */
    protected synchronized void stopReceiving() {
        if (receiver != null) {
            messageDisp.deregisterReceiver(receiver);
            receiver = null;
        }
    }

    /**
     * Returns the network node of this dispatcher.
     * @return the network node of this dispatcher
     */
    public NetworkNode getCurrentNetworkNode() {
        return messageDisp.getNetworkNode();
    }


    //****************** Messaging ******************//

    /**
     * Receiving method for bucket request messages.
     * The message's execute method is called on arrival and the returned
     * reply message is sent back.
     * @param msg the bucket request message to process
     * @throws IOException if there was an error processing the message
     */
    protected void receive(BucketRequestMessage msg) throws IOException {
        try {
            messageDisp.replyMessage(msg.execute(this));
        } catch (RuntimeException e) {
            messageDisp.replyMessage(new BucketExceptionReplyMessage(msg, e));
        } catch (BucketStorageException e) {
            messageDisp.replyMessage(new BucketExceptionReplyMessage(msg, e));
        }
    }

    /**
     * Sending method for bucket request messages.
     * The message is send to <code>networkNode</code> and the returned
     * reply message of class <code>T</code> is returned.
     * @param <T> the class of the reply message that is returned
     * @param msg the bucket request message to process
     * @param networkNode the destination node where the message is sent
     * @return the returned message
     * @throws IOException if there was an error processing the message
     * @throws RuntimeException if there was a {@link RuntimeException} when processing the bucket request
     * @throws BucketStorageException if there was a storage error when processing the bucket request
     */
    protected <T extends BucketReplyMessage> T send(BucketRequestMessage<T> msg, NetworkNode networkNode) throws IOException, RuntimeException, BucketStorageException {
        BucketReplyMessage reply = messageDisp.sendMessageWaitSingleReply(msg, BucketReplyMessage.class, networkNode);
        if (reply instanceof BucketExceptionReplyMessage)
            throw ((BucketExceptionReplyMessage)reply).getException();
        else
            return msg.replyMessageClass().cast(reply);
    }


    //****************** Bucket creation/removal ******************//

    /**
     * Creates a bucket on a remote network node.
     * @param atNetworkNode the network node at which to create a bucket
     * @return a remote bucket encapsulation on the new bucket
     * @throws BucketStorageException if the bucket cannot be created on the remote network node
     * @throws IOException if there was an error communicating with the remote node
     */
    public RemoteBucket createRemoteBucket(NetworkNode atNetworkNode) throws IOException, BucketStorageException {
        // Send message to "atNetworkNode" to create the bucket there
        return send(new BucketCreateRequestMessage(), atNetworkNode).getRemoteBucket(this);
    }

    /**
     * Removes a bucket on a remote network node.
     * @param remoteBucket the bucket to remove
     * @return <tt>true</tt> if the bucket was removed on the remote side
     * @throws IOException if there was an error communicating with the remote node
     * @throws NoSuchElementException if the bucket was not found on the remote network node
     */
    public boolean removeRemoteBucket(RemoteBucket remoteBucket) throws IOException, NoSuchElementException {
        try {
            return send(new BucketRemoveRequestMessage(remoteBucket.getBucketID()), remoteBucket.getRemoteNetworkNode()).getRemoved();
        } catch (BucketStorageException e) {
            throw new InternalError("This should never happen - bucket remove method never throws BucketStorageException");
        }
    }

}

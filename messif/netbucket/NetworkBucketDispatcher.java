/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.netbucket;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import messif.buckets.BucketDispatcher;
import messif.buckets.BucketErrorCode;
import messif.buckets.BucketStorageException;
import messif.buckets.LocalBucket;
import messif.buckets.impl.MemoryStorageBucket;
import messif.buckets.split.SplitPolicy;
import messif.buckets.split.SplitResult;
import messif.network.MessageDispatcher;
import messif.network.NetworkNode;
import messif.network.ThreadInvokingReceiver;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
     * @param defaultBucketClass the default class for newly created buckets
     */
    public NetworkBucketDispatcher(MessageDispatcher messageDisp, int maxBuckets, long bucketCapacity, Class<? extends LocalBucket> defaultBucketClass) {
        super(maxBuckets, bucketCapacity, defaultBucketClass);
        this.messageDisp = messageDisp;
        startReceiving();
    }

    /**
     * Deserialize and start receiving messages.
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        startReceiving();
    }
    
    @Override
    public void finalize() throws Throwable {
        stopReceiving();
        super.finalize();
    }

    
    //****************** Messaging inicialization methods ******************//

    /**
     * Creates a receiver for bucket messages and registers it in the message dispatcher.
     * @throws IllegalStateException if this network bucket dispatcher has a receiver associated already
     */
    private synchronized void startReceiving() throws IllegalStateException {
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
    private synchronized void stopReceiving() {
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
    protected void receive(BucketRequestMessage<?> msg) throws IOException {
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


    //****************** Bucket creation/removal/manipulation ******************//

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
     * Creates a bucket on a remote network node with default class and params is created 
     *  but with specified capacity and soft capacity.
     * @param atNetworkNode the network node at which to create a bucket
     * @param capacity capacity of the new bucket
     * @param softCapacity soft capacity of the new bucket
     * @return a remote bucket encapsulation on the new bucket
     * @throws BucketStorageException if the bucket cannot be created on the remote network node
     * @throws IOException if there was an error communicating with the remote node
     */
    public RemoteBucket createRemoteBucket(NetworkNode atNetworkNode, long capacity, long softCapacity) throws IOException, BucketStorageException {
        // Send message to "atNetworkNode" to create the bucket there
        return send(new BucketCreateRequestMessage(capacity, softCapacity), atNetworkNode).getRemoteBucket(this);
    }
    
    /**
     * Creates a bucket on a remote network node.
     * @param atNetworkNode the network node at which to create a bucket
     * @param storageClass storage class
     * @param storageClassParams parameters of the new storage
     * @param capacity capacity of the new bucket
     * @param softCapacity soft capacity of the new bucket
     * @return a remote bucket encapsulation on the new bucket
     * @throws BucketStorageException if the bucket cannot be created on the remote network node
     * @throws IOException if there was an error communicating with the remote node
     */
    public RemoteBucket createRemoteBucket(NetworkNode atNetworkNode, Class<? extends LocalBucket> storageClass, Map<String, Object> storageClassParams, long capacity, long softCapacity) throws IOException, BucketStorageException {
        // Send message to "atNetworkNode" to create the bucket there
        return send(new BucketCreateRequestMessage(storageClass, storageClassParams, capacity, softCapacity), atNetworkNode).getRemoteBucket(this);
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

    /**
     * Splits a given bucket on the remote node creating the new buckets in situ.
     * @param remoteBucket bucket to split
     * @param policy split policy according to which the bucket is to be split
     * @param whoStays Identification of a partition whose objects stay in the split bucket
     * @return the message that contains various output values
     * @throws BucketStorageException if the bucket cannot be created on the remote network node
     * @throws IOException if there was an error communicating with the remote node
     */
    public SplitResult splitBucket(RemoteBucket remoteBucket, SplitPolicy policy, int whoStays) throws BucketStorageException, IOException {
        return send(new BucketSplitRequestMessage(remoteBucket.getBucketID(), policy, whoStays), remoteBucket.getRemoteNetworkNode());
    }

    /**
     * Copies all objects from the {@code sourceRemoteBucket} to the {@code remoteBucket} on their network node.
     *  Nothing is done if the remote nodes of thee two buckets are not identical. 
     * @param remoteBucket specification of the destination bucket
     * @param sourceRemoteBucket specification of the source bucket 
     * @return False is returned if the remote nodes of thee two buckets are not identical or the insertion did no work for any reason
     * @throws BucketStorageException if the data could not have been copied on the remote node
     * @throws IOException if there was an error communicating with the remote node
     */
    public boolean copyAllObjects(RemoteBucket remoteBucket, RemoteBucket sourceRemoteBucket) throws IOException, BucketStorageException {
        if (! remoteBucket.getRemoteNetworkNode().equals(sourceRemoteBucket.getRemoteNetworkNode())) {
            return false;
        }
        return send(new BucketManipulationRequestMessage(remoteBucket.getBucketID(), sourceRemoteBucket.getBucketID()), remoteBucket.getRemoteNetworkNode()).getErrorCode() == BucketErrorCode.OBJECT_INSERTED;
    }
    
}

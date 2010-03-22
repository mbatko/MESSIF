/*
 * RemoteBucket.java
 *
 * Created on 4. kveten 2003, 22:19
 */

package messif.netbucket;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import messif.buckets.Bucket;
import messif.buckets.BucketStorageException;
import messif.buckets.LocalBucket;
import messif.network.NetworkNode;
import messif.objects.LocalAbstractObject;
import messif.objects.UniqueID;
import messif.objects.keys.AbstractObjectKey;
import messif.objects.util.AbstractObjectIterator;
import messif.operations.QueryOperation;


/**
 * This class represents the <tt>Bucket</tt> that is maintained on remote network node (i.e. another computer).
 * 
 * All bucket operations (insertions, deletions, etc.) are sent over network and execute on a remote
 * network node. There must be {@link messif.netbucket.NetworkBucketDispatcher} running on both sides that handles
 * the communication.
 * 
 * This class cannot be created directly, instead, it is obtained by a call to
 * {@link messif.netbucket.NetworkBucketDispatcher#createRemoteBucket createRemoteBucket} method of
 * {@link messif.netbucket.NetworkBucketDispatcher}.
 * 
 * 
 * @author xbatko
 * @see LocalBucket
 * @see NetworkBucketDispatcher
 */
public class RemoteBucket extends Bucket implements Serializable {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    //****************** Bucket info ******************//

    /** ID of the bucket on the remote node */
    protected final int bucketID;

    /** Remote node on which the bucket resides */
    protected NetworkNode remoteNetworkNode;

    /** Network storage (bucket) dispatcher to which this remote bucket is associated */
    protected final NetworkBucketDispatcher netbucketDisp;

    /** The maximal (hard) capacity of the remote bucket */
    protected final long capacity;


    //****************** Constructors ******************//
    
    /**
     * Creates a new instance of RemoteBucket from LocalBucket.
     * @param netbucketDisp Network storage (bucket) dispatcher to which this remote bucket is associated
     * @param bucket The local bucket from which this remote bucket is created
     */
    protected RemoteBucket(NetworkBucketDispatcher netbucketDisp, LocalBucket bucket) {
        this(netbucketDisp, bucket.getBucketID(), netbucketDisp.getCurrentNetworkNode(), bucket.getCapacity());
    }
    
    /**
     * Creates a new instance of RemoteBucket from parameters.
     * @param netbucketDisp the network storage (bucket) dispatcher with which this remote bucket is associated
     * @param bucketID the ID of the bucket on remote node
     * @param remoteNetworkNode the remote node on which the bucket resides
     * @param capacity the hard capacity of the bucket on remote node
     */
    protected RemoteBucket(NetworkBucketDispatcher netbucketDisp, int bucketID, NetworkNode remoteNetworkNode, long capacity) {
        this.netbucketDisp = netbucketDisp;
        this.bucketID = bucketID;
        this.remoteNetworkNode = remoteNetworkNode;
        this.capacity = capacity;
    }


    //****************** Access methods ******************//

    /**
     * Returns the ID of the bucket on remote node.
     * @return the ID of the bucket on remote node
     */
    public int getBucketID() {
        return bucketID;
    }

    /**
     * Returns the remote node on which the bucket resides.
     * @return the remote node on which the bucket resides
     */
    public NetworkNode getRemoteNetworkNode() {
        return remoteNetworkNode;
    }

    /**
     * Sets the remote node on which the bucket resides.
     * @param remoteNetworkNode the remote node on which the bucket resides
     */
    public void setRemoteNetworkNode(NetworkNode remoteNetworkNode) {
        this.remoteNetworkNode = remoteNetworkNode;
    }

    /**
     * Returns the maximal capacity of the remote bucket.
     * @return the maximal capacity of the remote bucket
     */
    public long getCapacity() {
        return capacity;
    }


    //****************** Local bucket access ******************//

    /**
     * Returns whether this bucket is local or remote
     * @return true if the bucket is on this node, the bucket is hosted on remote network node otherwise
     */
    public boolean isLocalBucket() {
        return netbucketDisp.getCurrentNetworkNode().equals(remoteNetworkNode); 
    }


    //****************** Object manipulators ******************//

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if there was an error communicating with the remote bucket dispatcher
     */
    public LocalAbstractObject getObject(UniqueID objectID) throws IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).getObject(objectID);
        
        // Otherwise, send message to remote netnode
        /* FIXME:
        try {
            return netbucketDisp.sendMessageWaitReply(new BucketManipulationRequestMessage(objectID, bucketID), remoteNetworkNode).getObject();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while getting object " + objectID + " from " + toString(), e);
        }
         */
        return null;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if there was an error communicating with the remote bucket dispatcher
     */
    public LocalAbstractObject getObject(String locator) throws IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).getObject(locator);
        
        // Otherwise, send message to remote netnode
        /* FIXME:
        try {
            return netbucketDisp.sendMessageWaitReply(new BucketManipulationRequestMessage(objectID, bucketID), remoteNetworkNode).getObject();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while getting object " + objectID + " from " + toString(), e);
        }
         */
        return null;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if there was an error communicating with the remote bucket dispatcher
     */
    public LocalAbstractObject getObject(AbstractObjectKey key) throws IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).getObject(key);
        
        // Otherwise, send message to remote netnode
        /* FIXME:
        try {
            return netbucketDisp.sendMessageWaitReply(new BucketManipulationRequestMessage(objectID, bucketID), remoteNetworkNode).getObject();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while getting object " + objectID + " from " + toString(), e);
        }
         */
        return null;
    }

    /**
     * Returns iterator over all objects from the remote bucket.
     * @return iterator over all objects from the remote bucket
     * @throws IllegalStateException if there was an error communicating with the remote bucket dispatcher
     */
    public AbstractObjectIterator<LocalAbstractObject> getAllObjects() throws IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).getAllObjects();
        
        // Otherwise, send message to remote netnode
        /* FIXME:
        try {
            return netbucketDisp.sendMessageWaitReply(new BucketManipulationRequestMessage(bucketID), remoteNetworkNode).getObjects().iterator();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while getting all objects from " + toString(), e);
        } catch (NoSuchElementException e) {
            throw new IllegalStateException(e.getMessage());
        }
         */
        return null;
    }

    @Override
    public void addObject(LocalAbstractObject object) throws BucketStorageException, IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket()) {
            netbucketDisp.getBucket(bucketID).addObject(object);
        } else {
            // Otherwise, send message to remote netnode
            /* FIXME:
            try {
                BucketManipulationReplyMessage msg = netbucketDisp.sendMessageWaitReply(new BucketManipulationRequestMessage(object, bucketID), remoteNetworkNode);
                if (msg.getErrorCode().equals(BucketErrorCode.HARDCAPACITY_EXCEEDED))
                    throw new CapacityFullException();
                return msg.getErrorCode();
            } catch (IOException e) {
                throw new IllegalStateException("Network error while adding " + object + " to " + toString(), e);
            } catch (NoSuchElementException e) {
                throw new IllegalStateException(e.getMessage());
            }
             */
        }
    }

    @Override
    public int addObjects(Iterator<? extends LocalAbstractObject> objects) throws BucketStorageException, IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).addObjects(objects);
        
        // Otherwise, send message to remote netnode
        /* FIXME:
        try {
            BucketManipulationReplyMessage msg = netbucketDisp.sendMessageWaitReply(new BucketManipulationRequestMessage(objects, bucketID), remoteNetworkNode);
            if (msg.getErrorCode().equals(BucketErrorCode.HARDCAPACITY_EXCEEDED))
                throw new CapacityFullException();
            return msg.getChangesCount();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while adding " + objects + " to " + toString(), e);
        } catch (NoSuchElementException e) {
            throw new IllegalStateException(e.getMessage());
        }            
         */
        return 0;
    }

    public LocalAbstractObject deleteObject(UniqueID objectID) throws NoSuchElementException, BucketStorageException, IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).deleteObject(objectID);
        
        // Otherwise, send message to remote netnode
        /* FIXME:
        try {
            return netbucketDisp.sendMessageWaitReply(new BucketManipulationRequestMessage(objectID, bucketID, true), remoteNetworkNode).getObject();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while deleting Object (" + objectID + ") from " + toString(), e);
        }
         */
        return null;
    }

    public int deleteObject(LocalAbstractObject object, int deleteLimit) throws BucketStorageException, IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).deleteObject(object, deleteLimit);
        
        // Otherwise, send message to remote netnode
        /* FIXME:
        try {
            return netbucketDisp.sendMessageWaitReply(new BucketManipulationRequestMessage(object, bucketID, true), remoteNetworkNode).getChangesCount();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while deleting Object (" + object + ") from " + toString(), e);
        }
         */
        return 0;
    }

    @Override
    public int deleteAllObjects() throws BucketStorageException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Process a query operation on objects from the remote bucket.
     * The query operation's answer is updated with objects from the bucket
     * that satisfy the query.
     * 
     * @param query query operation that is to be processed on the bucket
     * @return the number of objects that were added to answer
     * @throws IllegalStateException if there was an error communicating with the remote bucket dispatcher
     */      
    @Override
    public int processQuery(QueryOperation query) throws IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).processQuery(query);
        
        // Otherwise, send message to remote netnode
            /* FIXME:
        try {
            BucketManipulationReplyMessage msg = sendMessageWaitSingleReply(new BucketProcessQueryRequestMessage(bucketID, query), BucketCreateReplyMessage.class, remoteNetworkNode);
            query.updateFrom(msg.getQuery());
            return msg.getChangesCount();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while executing query (" + query + ") from " + toString(), e);
        }       
            */
        return 0;
    }


    //****************** Comparing ******************//

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param obj the reference object with which to compare
     * @return <code>true</code> if this object is the same as the obj
     *          argument; <code>false</code> otherwise
     */
    @Override
    public boolean equals(Object obj) {
	if (!(obj instanceof RemoteBucket)) return false;
	return (((RemoteBucket)obj).bucketID == bucketID) && ((RemoteBucket)obj).remoteNetworkNode.equals(remoteNetworkNode);
    }

    /**
     * Returns a hash code value for this bucket.
     * @return a hash code value for this bucket
     */
    @Override
    public int hashCode() {
	return bucketID;
    }
    
    
    //****************** String representation ******************//

    /**
     * Returns a string representation of this bucket.
     * @return a string representation of this bucket
     */
    @Override
    public String toString() {
        if (isLocalBucket()) return "LocalBucket (" + bucketID + ")";
        else return "RemoteBucket (" + bucketID + "@" + remoteNetworkNode + ")";
    }

}

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
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import messif.buckets.Bucket;
import messif.buckets.BucketStorageException;
import messif.buckets.LocalBucket;
import messif.network.NetworkNode;
import messif.objects.LocalAbstractObject;
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
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
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
    private NetworkNode remoteNetworkNode;

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
    @Override
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
    @Override
    public LocalAbstractObject getObject(String locator) throws IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).getObject(locator);
        
        // Otherwise, send message to remote netnode
        try {
            return netbucketDisp.send(new BucketManipulationRequestMessage(locator, bucketID), remoteNetworkNode).getObject();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while getting object with locator " + locator + " from " + toString(), e);
        } catch (BucketStorageException e) {
            throw new IllegalStateException("Network error while getting object with locator " + locator + " from " + toString(), e);
        }
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if there was an error communicating with the remote bucket dispatcher
     */
    @Override
    public LocalAbstractObject getObject(AbstractObjectKey key) throws IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).getObject(key);
        
        // Otherwise, send message to remote netnode
        try {
            return netbucketDisp.send(new BucketManipulationRequestMessage(key, bucketID), remoteNetworkNode).getObject();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while getting object with key " + key + " from " + toString(), e);
        } catch (BucketStorageException e) {
            throw new IllegalStateException("Network error while getting object with key " + key + " from " + toString(), e);
        }
    }

    /**
     * Returns iterator over all objects from the remote bucket.
     * @return iterator over all objects from the remote bucket
     * @throws IllegalStateException if there was an error communicating with the remote bucket dispatcher
     */
    @Override
    public AbstractObjectIterator<LocalAbstractObject> getAllObjects() throws IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).getAllObjects();
        
        // Otherwise, send message to remote netnode
        try {
            return netbucketDisp.send(new BucketManipulationRequestMessage(bucketID), remoteNetworkNode).getObjects().iterator();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while getting all objects from " + toString(), e);
        } catch (NoSuchElementException e) {
            throw new IllegalStateException(e.getMessage());
        } catch (BucketStorageException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void addObject(LocalAbstractObject object) throws BucketStorageException, IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket()) {
            netbucketDisp.getBucket(bucketID).addObject(object);
        } else {
            // Otherwise, send message to remote netnode
            try {
                netbucketDisp.send(new BucketManipulationRequestMessage(object, bucketID), remoteNetworkNode);
            } catch (IOException e) {
                throw new IllegalStateException("Network error while adding " + object + " to " + toString(), e);
            } catch (NoSuchElementException e) {
                throw new IllegalStateException(e.getMessage());
            }
        }
    }

    @Override
    public int addObjects(Iterator<? extends LocalAbstractObject> objects) throws BucketStorageException, IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).addObjects(objects);
        
        // Otherwise, send message to remote netnode
        try {
            BucketManipulationReplyMessage msg = netbucketDisp.send(new BucketManipulationRequestMessage(objects, bucketID), remoteNetworkNode);
            return msg.getChangesCount();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while adding " + objects + " to " + toString(), e);
        } catch (NoSuchElementException e) {
            throw new IllegalStateException(e.getMessage());
        }            
    }

    @Override
    public int deleteObject(LocalAbstractObject object, int deleteLimit) throws BucketStorageException, IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).deleteObject(object, deleteLimit);
        
        // Otherwise, send message to remote netnode
        try {
            return netbucketDisp.send(new BucketManipulationRequestMessage(object, bucketID, deleteLimit), remoteNetworkNode).getChangesCount();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while deleting Object (" + object + ") from " + toString(), e);
        }
    }

    @Override
    public int deleteObject(String locatorURI, int deleteLimit) throws BucketStorageException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).deleteObject(locatorURI, deleteLimit);
        
        // Otherwise, send message to remote netnode
        try {
            return netbucketDisp.send(new BucketManipulationRequestMessage(locatorURI, bucketID, deleteLimit), remoteNetworkNode).getChangesCount();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while deleting object with locator " + locatorURI + " from " + toString(), e);
        }
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
    public int processQuery(QueryOperation<?> query) throws IllegalStateException {
        // If this remote bucket points is current node, use local bucket
        if (isLocalBucket())
            return netbucketDisp.getBucket(bucketID).processQuery(query);
        
        // Otherwise, send message to remote netnode
        try {
            BucketProcessQueryReplyMessage msg = netbucketDisp.send(new BucketProcessQueryRequestMessage(bucketID, query), remoteNetworkNode);
            query.updateFrom(msg.getQuery());
            return msg.getCount();
        } catch (IOException e) {
            throw new IllegalStateException("Network error while executing query (" + query + ") from " + toString(), e);
        } catch (BucketStorageException e) {
            throw new IllegalStateException("This should never happen: " + e, e);
        }
    }


    //****************** Comparing ******************//

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param obj the reference object with which to compare
     * @return <code>true</code> if this object is the same as the obj
     *          argument; <code>false</code> otherwise
     */
    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
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

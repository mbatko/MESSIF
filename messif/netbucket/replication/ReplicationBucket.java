/*
 * ReplicationBucket.java
 *
 * Created on 13. unor 2007, 11:20
 *
 */

package messif.netbucket.replication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import messif.buckets.Bucket;
import messif.buckets.BucketErrorCode;
import messif.buckets.CapacityFullException;
import messif.buckets.LocalBucket;
import messif.buckets.OccupationLowException;
import messif.netbucket.RemoteBucket;
import messif.network.NetworkNode;
import messif.objects.AbstractObject;
import messif.objects.LocalAbstractObject;
import messif.objects.UniqueID;
import messif.objects.util.AbstractObjectIterator;
import messif.objects.util.AbstractObjectList;
import messif.operations.QueryOperation;
import messif.statistics.OperationStatistics;
import messif.statistics.StatisticCounter;

/**
 *
 * @author xbatko
 */
public class ReplicationBucket extends LocalBucket {
    /** class serial id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Statistics counter */
    protected static StatisticCounter distanceComputations = StatisticCounter.getStatistics("DistanceComputations");
    
    protected final ReplicationNetworkBucketDispatcher bucketDispatcher;
    protected final LocalBucket encapsulatedBucket;
    protected final List<RemoteBucket> replicas = new ArrayList<RemoteBucket>();
    protected final AtomicInteger nextReplicaForGet = new AtomicInteger(0); // zero means access local bucket
    protected final ReadWriteLock replicaManipulationLock = new ReentrantReadWriteLock(true);
    
    /** Creates a new instance of ReplicationBucket */
    protected ReplicationBucket(ReplicationNetworkBucketDispatcher bucketDispatcher, LocalBucket encapsulatedBucket) {
        this.encapsulatedBucket = encapsulatedBucket;
        this.bucketDispatcher = bucketDispatcher;
    }
    
    
    /****************** Overrides for all public methods of LocalBucket that simply call the stub ******************/
    
    public void createReplica(NetworkNode atNetworkNode) throws CapacityFullException {
        replicaManipulationLock.writeLock().lock();
        
        try {
            // Create new remote bucket at specified node
            RemoteBucket replica = bucketDispatcher.createRemoteBucket(atNetworkNode);
            
            // Replicate all currently stored objects
            replica.addObjects(encapsulatedBucket.getAllObjects());
            
            // Add new replica to the internal replicas list
            replicas.add(replica);
        } finally {
            replicaManipulationLock.writeLock().unlock();
        }
    }
    
    /** Remove replica of this bucket from given node.
     * @return false if the bucket is not replicated at the node */
    public boolean removeReplica(NetworkNode atNetworkNode) throws IOException {
        replicaManipulationLock.writeLock().lock();
        
        try {
            // Search for replicas at specified node
            Iterator<RemoteBucket> iterator = replicas.iterator();
            while (iterator.hasNext()) {
                RemoteBucket replica = iterator.next();
                if (atNetworkNode.equals(replica.getRemoteNetworkNode())) {
                    // Found the replica, delete it...
                    iterator.remove();
                    return bucketDispatcher.removeRemoteBucket(replica);
                }
            }
            return false;
        } finally {
            replicaManipulationLock.writeLock().unlock();
        }
    }
    
    /** Indicate that one of the replica was migrated */
    public void replicaMigrated(NetworkNode origNode, NetworkNode newNode) {
        replicaManipulationLock.writeLock().lock();
        try {
            for (RemoteBucket replica : replicas)
                if (replica.getRemoteNetworkNode().equals(origNode))
                    replica.setRemoteNetworkNode(newNode);
        } finally {
            replicaManipulationLock.writeLock().unlock();
        }
    }
    
    /** Return set of all network nodes where this bucket has replicas */
    public Set<NetworkNode> getAllReplicaNodes() {
        Set<NetworkNode> retVal = new HashSet<NetworkNode>();
        for (RemoteBucket replicaBucket : replicas)
            retVal.add(replicaBucket.getRemoteNetworkNode());
        return retVal;
    }
    
    /****************** Overrides for all public methods of LocalBucket that simply call the stub ******************/
    
    public int getBucketID() {
        return encapsulatedBucket.getBucketID();
    }
    
    public int getObjectCount() {
        return encapsulatedBucket.getObjectCount();
    }
    
    public long getCapacity() {
        return encapsulatedBucket.getCapacity();
    }
    
    public long getSoftCapacity() {
        return encapsulatedBucket.getSoftCapacity();
    }
    
    public long getLowOccupation() {
        return encapsulatedBucket.getLowOccupation();
    }
    
    public long getOccupation() {
        return encapsulatedBucket.getOccupation();
    }
    
    public double getOccupationRatio() {
        return encapsulatedBucket.getOccupationRatio();
    }
    
    public boolean isSoftCapacityExceeded() {
        return encapsulatedBucket.isSoftCapacityExceeded();
    }
    
    public String toString() {
        return encapsulatedBucket.toString();
    }
    
    
    /****************** Overrides for all manipulation methods of LocalBucket ******************/
    
    public int addObjects(Iterator<? extends AbstractObject> objects) throws CapacityFullException {
        replicaManipulationLock.readLock().lock();
        try {
            int ret = encapsulatedBucket.addObjects(objects);
            
            // Update all replicas
            for (RemoteBucket replica : replicas)
                replica.addObjects(objects);

            return ret;
        } finally {
            replicaManipulationLock.readLock().unlock();
        }
    }
    
    public int addObjects(List<? extends AbstractObject> objects) throws CapacityFullException {
        replicaManipulationLock.readLock().lock();
        try {
            int ret = encapsulatedBucket.addObjects(objects);
            
            // Update all replicas
            for (RemoteBucket replica : replicas)
                replica.addObjects(objects);

            return ret;
        } finally {
            replicaManipulationLock.readLock().unlock();
        }
    }
    
    public BucketErrorCode addObject(LocalAbstractObject object) throws CapacityFullException {
        replicaManipulationLock.readLock().lock();
        try {
            BucketErrorCode error = encapsulatedBucket.addObject(object);
            
            if (error.OBJECT_INSERTED.equals(error)) {
                // Update all replicas
                for (RemoteBucket replica : replicas)
                    replica.addObject(object);
            }
            
            return error;
        } finally {
            replicaManipulationLock.readLock().unlock();
        }
    }
    
    public LocalAbstractObject deleteObject(UniqueID objectID) throws NoSuchElementException, OccupationLowException {
        replicaManipulationLock.readLock().lock();
        try {
            LocalAbstractObject object = encapsulatedBucket.deleteObject(objectID);
            
            if (object != null) {
                // Update all replicas
                for (RemoteBucket replica : replicas)
                    replica.deleteObject(objectID);
            }
            
            return object;
        } finally {
            replicaManipulationLock.readLock().unlock();
        }
    }
    
    public AbstractObjectList<LocalAbstractObject> deleteObjects(List<UniqueID> objectIDs) throws NoSuchElementException, OccupationLowException {
        replicaManipulationLock.readLock().lock();
        try {
            AbstractObjectList<LocalAbstractObject> objects = encapsulatedBucket.deleteObjects(objectIDs);
            
            if (objects.size() > 0) {
                // Update all replicas
                for (RemoteBucket replica : replicas)
                    replica.deleteObjects(objectIDs);
            }
            
            return objects;
        } finally {
            replicaManipulationLock.readLock().unlock();
        }
    }
    
    
    /****************** Overrides for all getter methods of LocalBucket ******************/
    
    protected Bucket getOperatingBucket() {
        // Get next replica index (thread safe)
        int index;
        synchronized (nextReplicaForGet) {
            if (nextReplicaForGet.get() > replicas.size())
                nextReplicaForGet.set(0);
            index = nextReplicaForGet.getAndIncrement();
        }
        
        return (index == 0)?encapsulatedBucket:replicas.get(index - 1);
    }
    
    public int processQuery(QueryOperation query) {
        replicaManipulationLock.readLock().lock();
        try {
            StatisticCounter threadDistComp = OperationStatistics.getOpStatisticCounter("DistanceComputations");
            threadDistComp.bindTo(distanceComputations); // Try to bind to global distance computations (if it is not)
            long currentDistComp = threadDistComp.get();
            Bucket bucket = getOperatingBucket();
            try {
                return bucket.processQuery(query);
            } finally {
                if ((bucket == encapsulatedBucket) || ((RemoteBucket) bucket).isLocalBucket())
                    bucketDispatcher.bucketOperationDistcompCounter.add(query.getOperationID(), threadDistComp.get() - currentDistComp);
                threadDistComp.unbind();
            }
        } finally {
            replicaManipulationLock.readLock().unlock();
        }
    }
    
    public AbstractObjectIterator<LocalAbstractObject> getAllObjects() {
        replicaManipulationLock.readLock().lock();
        try {
            return encapsulatedBucket.getAllObjects();
        } finally {
            replicaManipulationLock.readLock().unlock();
        }
    }
    
    public LocalAbstractObject getObject(UniqueID objectID) throws NoSuchElementException {
        replicaManipulationLock.readLock().lock();
        try {
            return encapsulatedBucket.getObject(objectID);
        } finally {
            replicaManipulationLock.readLock().unlock();
        }
    }
    
    public AbstractObjectIterator<LocalAbstractObject> provideObjects() {
        replicaManipulationLock.readLock().lock();
        try {
            return encapsulatedBucket.provideObjects();
        } finally {
            replicaManipulationLock.readLock().unlock();
        }
    }
    
    
    /****************** LocalBucket internal method implementations ******************/
    
    protected BucketErrorCode storeObject(LocalAbstractObject object) {
        throw new UnsupportedOperationException("This method should not be called from anywhere. Please, override the method in ReplicationBucket");
    }
    
    protected LocalBucket.LocalBucketIterator<? extends LocalBucket> iterator() {
        throw new UnsupportedOperationException("This method should not be called from anywhere. Please, override the method in ReplicationBucket");
    }
    
}

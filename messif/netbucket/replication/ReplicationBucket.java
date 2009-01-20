/*
 * ReplicationBucket.java
 *
 * Created on 13. unor 2007, 11:20
 *
 */

package messif.netbucket.replication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import messif.buckets.Bucket;
import messif.buckets.BucketStorageException;
import messif.buckets.LocalBucket;
import messif.buckets.index.ModifiableIndex;
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
        super(Long.MAX_VALUE, Long.MAX_VALUE, 0, false);
        this.encapsulatedBucket = encapsulatedBucket;
        this.bucketDispatcher = bucketDispatcher;
    }
    
    
    /****************** Overrides for all public methods of LocalBucket that simply call the stub ******************/
    
    public void createReplica(NetworkNode atNetworkNode) throws BucketStorageException, IllegalStateException {
        replicaManipulationLock.writeLock().lock();
        
        try {
            // Create new remote bucket at specified node
            RemoteBucket replica = bucketDispatcher.createRemoteBucket(atNetworkNode);
            
            // Replicate all currently stored objects
            replica.addObjects(encapsulatedBucket.getAllObjects());
            
            // Add new replica to the internal replicas list
            replicas.add(replica);
        } catch (IOException e) {
            throw new IllegalStateException(e);
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
    
    @Override
    public int getBucketID() {
        return encapsulatedBucket.getBucketID();
    }
    
    @Override
    public int getObjectCount() {
        return encapsulatedBucket.getObjectCount();
    }
    
    @Override
    public long getCapacity() {
        return encapsulatedBucket.getCapacity();
    }
    
    @Override
    public long getSoftCapacity() {
        return encapsulatedBucket.getSoftCapacity();
    }
    
    @Override
    public long getLowOccupation() {
        return encapsulatedBucket.getLowOccupation();
    }
    
    @Override
    public long getOccupation() {
        return encapsulatedBucket.getOccupation();
    }
    
    @Override
    public double getOccupationRatio() {
        return encapsulatedBucket.getOccupationRatio();
    }
    
    @Override
    public boolean isSoftCapacityExceeded() {
        return encapsulatedBucket.isSoftCapacityExceeded();
    }
    
    @Override
    public String toString() {
        return encapsulatedBucket.toString();
    }
    
    
    /****************** Overrides for all manipulation methods of LocalBucket ******************/
    
    @Override
    public int addObjects(Iterator<? extends AbstractObject> objects) throws BucketStorageException {
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
    
    @Override
    public int addObjects(Collection<? extends AbstractObject> objects) throws BucketStorageException {
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
    
    @Override
    public void addObject(LocalAbstractObject object) throws BucketStorageException {
        replicaManipulationLock.readLock().lock();
        try {
            encapsulatedBucket.addObject(object);
            // Update all replicas
            for (RemoteBucket replica : replicas)
                replica.addObject(object);
        } finally {
            replicaManipulationLock.readLock().unlock();
        }
    }

    @Override
    public LocalAbstractObject deleteObject(UniqueID objectID) throws NoSuchElementException, BucketStorageException {
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

    @Override
    public AbstractObjectList<LocalAbstractObject> deleteObjects(Collection<? extends UniqueID> objectIDs, boolean removeDeletedIDs) throws NoSuchElementException, BucketStorageException {
        replicaManipulationLock.readLock().lock();
        try {
            AbstractObjectList<LocalAbstractObject> objects = encapsulatedBucket.deleteObjects(objectIDs, removeDeletedIDs);
            
            if (objects.size() > 0) {
                // Update all replicas
                for (RemoteBucket replica : replicas)
                    replica.deleteObjects(objectIDs, removeDeletedIDs);
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
    
    @Override
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
    
    @Override
    public AbstractObjectIterator<LocalAbstractObject> getAllObjects() {
        replicaManipulationLock.readLock().lock();
        try {
            return encapsulatedBucket.getAllObjects();
        } finally {
            replicaManipulationLock.readLock().unlock();
        }
    }
    
    @Override
    public LocalAbstractObject getObject(UniqueID objectID) throws NoSuchElementException {
        replicaManipulationLock.readLock().lock();
        try {
            return encapsulatedBucket.getObject(objectID);
        } finally {
            replicaManipulationLock.readLock().unlock();
        }
    }
    
    @Override
    public AbstractObjectIterator<LocalAbstractObject> provideObjects() {
        replicaManipulationLock.readLock().lock();
        try {
            return encapsulatedBucket.provideObjects();
        } finally {
            replicaManipulationLock.readLock().unlock();
        }
    }

    @Override
    protected ModifiableIndex<LocalAbstractObject> getModifiableIndex() {
        throw new UnsupportedOperationException("This method should not be called anywhere in the replication bucket, please fix the code");
    }

}

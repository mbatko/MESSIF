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
package messif.netbucket.replication;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import messif.buckets.Bucket;
import messif.buckets.BucketStorageException;
import messif.buckets.CapacityFullException;
import messif.buckets.LocalBucket;
import messif.netbucket.NetworkBucketDispatcher;
import messif.network.MessageDispatcher;
import messif.network.NetworkNode;
import messif.statistics.StatisticSimpleWeakrefCounter;

/**
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ReplicationNetworkBucketDispatcher extends NetworkBucketDispatcher {
    
    /** Class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Statistic for per query operation DC */
    protected StatisticSimpleWeakrefCounter bucketOperationDistcompCounter = StatisticSimpleWeakrefCounter.getStatistics("BucketOperationDistcompCounter." + this.hashCode());
    
    /** If true then automatically replicate all bucket at the same nodes */
    protected final boolean replicateBucketsEqually;
    
    /****************** Attributes access ******************/

    /**
     * Get this bucket dispatcher's statistic for per query operation DC 
     * @return bucket dispatcher's statistic for per query operation DC
     */
    public StatisticSimpleWeakrefCounter getBucketOperationDistcompCounter() {
        return bucketOperationDistcompCounter;
    }


    /****************** Constructors ******************/

    /**
     * Creates a new instance of ReplicationNetworkBucketDispatcher
     */
    public ReplicationNetworkBucketDispatcher(MessageDispatcher messageDisp, int maxBuckets, long bucketCapacity, boolean replicateBucketsEqually, Class<? extends LocalBucket> defaultBucketClass) throws InstantiationException {
        super(messageDisp, maxBuckets, bucketCapacity, defaultBucketClass);
        this.replicateBucketsEqually = replicateBucketsEqually;
    }
    
    /**
     * Creates a new instance of ReplicationNetworkBucketDispatcher
     */
    public ReplicationNetworkBucketDispatcher(MessageDispatcher messageDisp, int maxBuckets, long bucketCapacity, long bucketSoftCapacity,
            long bucketLowOccupation, boolean bucketOccupationAsBytes, Class<? extends LocalBucket> defaultBucketClass, boolean replicateBucketsEqually)
            throws InstantiationException {
        super(messageDisp, maxBuckets, bucketCapacity, bucketSoftCapacity, bucketLowOccupation, bucketOccupationAsBytes, defaultBucketClass);
        this.replicateBucketsEqually = replicateBucketsEqually;
    }


    /****************** Override for replica change ******************/

    /** Add new bucket with encapsulation into ReplicationBucket */
    public synchronized LocalBucket addBucket(LocalBucket bucket) throws BucketStorageException, IllegalStateException {
        // Create replica envelope and call super implementation
        ReplicationBucket newBucket = new ReplicationBucket(this, bucket);
        
        // if all buckets should be replicated on the same nodes then replicate the new bucket
        if (replicateBucketsEqually && (getBucketCount() > 0)) {
            try {
                for (NetworkNode replicaNode : getAllReplicaNodes())
                    newBucket.createReplica(replicaNode);
            } catch (CapacityFullException e) { 
                log.log(Level.SEVERE, e.getClass().toString(), e);
            }
        }

        return super.addBucket(newBucket);
    }


    /******************************    Methods to be executed on the replication buckets    ******************************/
    
    public void createReplica(NetworkNode atNetworkNode) throws BucketStorageException, IllegalStateException {
        for (Bucket bucket : getAllBuckets())
            ((ReplicationBucket) bucket).createReplica(atNetworkNode);
    }
    
    /** Remove replica of this bucket from all bucket in this dispatcher.
     * @return false if some of the buckets is not replicated at the node */
    public boolean removeReplica(NetworkNode atNetworkNode) throws IOException {
        boolean retVal = true;
        for (Bucket bucket : getAllBuckets())
            retVal = retVal && ((ReplicationBucket) bucket).removeReplica(atNetworkNode);
        return retVal;
    }
    
    /** One of the replicas (of potentially all buckets) has been migrated.
     *   Tell the buckets that it was migrated */
    public void replicaMigrated(NetworkNode origNode, NetworkNode newNode) {
        for (Bucket bucket : getAllBuckets())
            ((ReplicationBucket) bucket).replicaMigrated(origNode, newNode);
    }
    
    /** Return set of all network nodes where my buckets have some replicas */
    public Set<NetworkNode> getAllReplicaNodes() {
        // if all buckets are replicated equally then pick any of the buckets
        if (replicateBucketsEqually && (getBucketCount() > 0))
            return ((ReplicationBucket) getAllBuckets().iterator().next()).getAllReplicaNodes();
        // else merge info from all buckets
        Set<NetworkNode> retVal = new TreeSet<NetworkNode>();
        for (Bucket bucket : getAllBuckets())
            retVal.addAll(((ReplicationBucket) bucket).getAllReplicaNodes());
        return retVal;
    }

}

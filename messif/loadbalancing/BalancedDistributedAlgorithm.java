/*
 * BalancedDistributedAlgorithm.java
 *
 * Created on September 5, 2006, 13:21
 */

package messif.loadbalancing;

import messif.algorithms.AlgorithmMethodException;
import messif.algorithms.DistributedAlgorithm;
import messif.netbucket.replication.ReplicationNetworkBucketDispatcher;
import messif.network.MessageDispatcher;
import messif.network.NetworkNode;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import messif.algorithms.DistAlgRequestMessage;
import messif.buckets.LocalBucket;
import messif.loadbalancing.replication.MigrateNotifyOperation;
import messif.statistics.StatisticSlidingSimpleRefCounter;
import messif.statistics.Statistics;

/**
 *
 * @author xnovak8
 */
public abstract class BalancedDistributedAlgorithm extends DistributedAlgorithm {
    /** class id for serialization */
    private static final long serialVersionUID = 5L;
    
    /** Peer managing this "node". May be null (no peer used) */
    protected Host host;
    /** Peer getter */
    public Host getHost() { return host; }
    
    /** Returns the messageDispatcher to all classes in the package */
    protected MessageDispatcher getMessageDisp() { return messageDisp; }
    
    /******************** Statistics for the logical node ******************/
    
    /** The sliding window for the busy load */
    protected StatisticSlidingSimpleRefCounter busyLoad;
    /** Return the busy load of this node */
    public double getBusyLoad() {
        if (! busyLoad.checkUsedTime())
            return LBSettings.LOAD_DONT_KNOW;
        return busyLoad.getSum();
    }
    
    /** The sliding window for the single load */
    protected StatisticSlidingSimpleRefCounter singleLoad;
    /* Return the single load of this node */
    public double getSingleLoad() {
        if (singleLoad.getCnt() < singleLoad.getMaxNumberOfValues())
            return LBSettings.LOAD_DONT_KNOW;
        return singleLoad.getAvg();
    }
    
    /** Return the data storage size */
    public long getDataLoad() {
        return storageDispatcher.getObjectCount();
    }
    
    /** The initialization of the statistics is done in constructor and after deserialization.
     *   It expects that the storage dispatcher is already created. */
    private void initStatistics() {
        busyLoad = StatisticSlidingSimpleRefCounter.getStatistics("busyLoad_"+messageDisp.getNetworkNode());
        busyLoad.setWindowSizeMilis(LBSettings.BUSY_LOAD_WINDOW);
        singleLoad = StatisticSlidingSimpleRefCounter.getStatistics("singleLoad_"+messageDisp.getNetworkNode());
        singleLoad.setMaxNumberOfValues(LBSettings.SINGLE_LOAD_AVG);
        
        busyLoad.bindTo(storageDispatcher.getBucketOperationDistcompCounter());
        singleLoad.bindTo(storageDispatcher.getBucketOperationDistcompCounter());
    }
    /** Clear all statistics including the averages obtained by gossiping */
    public void resetStatistics() {
        busyLoad.reset();
        singleLoad.reset();
    }
    
    /** The storage of this node - realized as the replicated storage dispatcher */
    protected ReplicationNetworkBucketDispatcher storageDispatcher = null;
    
    /** Return the storage (bucket) dispatcher of this node */
    public ReplicationNetworkBucketDispatcher getStorageDispatcher() {
        return storageDispatcher;
    }
    
    /******************** Constructors ********************************/
    
    /** Constructor given an existing parent algorithm */
    public BalancedDistributedAlgorithm(String algorithmName, Host host, Class<? extends LocalBucket> defaultBucketClass) throws InstantiationException {
        super(algorithmName, host, Host.getUniqueID());
        this.host = host;
        
        storageDispatcher = new ReplicationNetworkBucketDispatcher(messageDisp, Integer.MAX_VALUE,
                Long.MAX_VALUE, Long.MAX_VALUE, 0l, false, defaultBucketClass, true);
        storageDispatcher.setMessageDispatcher(messageDisp);
        
        initStatistics();
        
        host.addNode(this);
    }
    
    /****************** Destructor ******************/
    
    /** Public destructor to stop the algorithm.
     *  This should be overriden in order to clean up.
     */
    public void finalize() throws Throwable {
        Statistics.removeStatistic(busyLoad.getName());
        Statistics.removeStatistic(singleLoad.getName());
        super.finalize();
    }
    
    /*******************************  (De)serialization *********************/
    
    /** store the statistics in a correct way */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        if (host != null)
            out.writeInt(getThisNode().getNodeID());
    }
    
    /** Deserialization method - create the message dispatcher from the restored values "port", "broadcastport"
     * (for the top-most node). Create the activation receiver to wait for "start" message.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (host != null)
            createMessageDispatcher(host, in.readInt());

        busyLoad.setWindowSizeMilis(LBSettings.BUSY_LOAD_WINDOW);
        singleLoad.setMaxNumberOfValues(LBSettings.SINGLE_LOAD_AVG);        
    }
    
    /******************************  Overriding methods ******************************/
    
    /** The message dispatcher can be created when deserializing from DISK not from the NETWORK */
    protected void readMessageDisp(ObjectInputStream in) throws IOException { }
    /** Store only information about the nodeID (and do this only when serializing to DISK */
    protected void writeMessageDisp(ObjectOutputStream out) throws IOException { }
    
    /** 
     * This method creates the message dispatcher given a parent dispatcher
     * and a new id of the new NetworkNode and sets the <code>meesageDisp</code> field. 
     * @return the created message dispatcher
      */
    protected MessageDispatcher createMessageDispatcher(DistributedAlgorithm parent, int newId) {
        messageDisp = new GossipMessageDispatcher((Host)parent, ((Host)parent).getMessageDisp(), newId);
        try {
            if (storageDispatcher != null)
                storageDispatcher.setMessageDispatcher(messageDisp);
        } catch (InstantiationException ex) {
            log.severe(ex);
        }
        return messageDisp;
    }
    
    /** Migrate replica from a node to another node */
    public void migrateReplica(MigrateNotifyOperation operation, DistAlgRequestMessage request) throws AlgorithmMethodException {
        storageDispatcher.replicaMigrated(operation.origNode, operation.newNode);
    }
    
    /** Notify replicas about migration of this node */
    public void migrateNotifyReplicas(NetworkNode origNode, NetworkNode newNode) throws AlgorithmMethodException {
        Collection<NetworkNode> replicas = storageDispatcher.getAllReplicaNodes();
        if (replicas.isEmpty())
            return;
        try {
            messageDisp.sendMessage(new DistAlgRequestMessage(new MigrateNotifyOperation(origNode, newNode)), replicas, true);
        } catch (IOException ex) {
            log.severe(ex);
            throw new AlgorithmMethodException(ex.getMessage());
        }
    }
    
    /******************************  Abstract load-balancing operations to be implemented ***************************/
    
    /** The load-balancing "Split" operation. Implementation of this method should create the CreateNodeOperation
     *   with information about the specific node's class adn about constructor to be used to create a new node
     * @return <b>null</b> if this node cannot be split
     */
    public abstract CreateNodeOperation splitNode();
    
    /** The load-balacing "Leave" operation. Correctly removes given node from the PDN. Either join the data
     * with a neighbour or re-insert them to the network. The DistributedAlgorithm can be disposed afterwards.
     * @return false if the node cannot leave the network properly.
     */
    public abstract boolean leave();
    
    /** When a node leaves the network, it moves its data to another node (merge).
     *  This method should return the node this node would to merge with. If there is no such node, return null.
     */
    public abstract NetworkNode getMergingNode();

    /** When a node leaves the network, it moves its data to another node (merge).
     *  This method should return a node that would be merged with this node (if any) or null.
     */
    public abstract NetworkNode getNodeToMerge();
    
    /** The load-balacing "Migrate" operation. Substitute "thisNode" for a specified node (at a different host). This operation is
     *   called after physical migration at the new host!
     */
    public abstract void migrate(NetworkNode origNode, NetworkNode newNode) throws AlgorithmMethodException;
    
    /** Returns a random node that I know in the system (for gossiping) */
    public abstract NetworkNode getRandomNode();
}

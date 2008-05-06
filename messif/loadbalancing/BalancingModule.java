/*
 * BalancingModule.java
 *
 * Created on October 19, 2006, 11:45
 */

package messif.loadbalancing;

import java.util.SortedMap;
import java.util.concurrent.locks.ReentrantLock;
import messif.algorithms.DistAlgReplyMessage;
import messif.loadbalancing.HostList.HostLoad;
import messif.loadbalancing.replication.Replica;
import messif.network.MessageDispatcher;
import messif.utility.Logger;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import messif.algorithms.DistAlgRequestMessage;
import messif.network.NetworkNode;

/**
 * This class encapsulates the very load-balancing functionality of the Host
 *
 * @author xnovak8
 */
public class BalancingModule implements Serializable {
    /** Class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Log */
    private static Logger log = Logger.getLoggerEx("Host");
    
    /** The corresponding host */
    private Host host;
    /** The gossip module */
    private GossipModule gossip;
    /** The messageDisp */
    protected transient MessageDispatcher messageDisp;
    
    /** Monitor for exclusive balancing access */
    protected transient ReentrantLock monitor = new ReentrantLock();
    
    /** Creates a new instance of BalancingModule */
    protected BalancingModule(Host host, GossipModule gossip, MessageDispatcher messageDisp) {
        this.gossip = gossip;
        this.host = host;
        this.messageDisp = messageDisp;
    }
    
    /** Deserialization method  */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        overloadCounter = 0;
        overloadType = NO_OVERLOAD;
        monitor = new ReentrantLock();
    }
    
    /** The counter how many times the peer was overloaded in a row */
    protected transient int overloadCounter = 0;
    protected transient int overloadType = NO_OVERLOAD;
    
    protected static final int NO_OVERLOAD = 0;
    protected static final int DATA_OVERLOAD = 1;
    protected static final int BUSY_OVERLOAD = 2;
    protected static final int SINGLE_OVERLOAD = 3;
    protected static final int BUSY_UNDERLOAD = 4;
    
    /** This method is called when the peer is overloaded. It increases the overloadCounter and
     *  @return true if the overload was enough times in a row to really do the load balancing.
     */
    private boolean overloadedDoBalancing(int overloadType) {
        if (this.overloadType != overloadType) {
            this.overloadType = overloadType;
            overloadCounter = 0;
        }
        return (++overloadCounter >= LBSettings.OVERLOAD_RECHECKS);
    }
    
    /** This method is called when the peer is NOT overloaded. It clears the overloadCounter  */
    private void notOverloaded() {
        overloadCounter = 0;
    }
    
    /** Given a hostLoad, check whether the peer is empty
     *   contact the peer in order to update the data value, to check its availability for balancing and
     *   make sure that no other peer would start balancing action on given peer.
     */
    public boolean isEmpty(HostLoad peerLoad) {
        if (peerLoad.storage > 0)
            return false;
        try {
            SuitableHostOperation op = (SuitableHostOperation) ((DistAlgReplyMessage) messageDisp.sendMessageWaitReply(
                    new DistAlgRequestMessage(new SuitableHostOperation()), peerLoad.host).getFirstReply()).getOperation();
            return op.wasSuccessful();
        } catch (IOException ex) {
            log.severe(ex);
            return false;
        }
    }
    
    /** Given a hostLoad, check whether the planned balancing action would cause the overload of the peer
     *   contact the peer in order to update the load values, to check its availability for balancing and
     *   make sure that no other peer would start balancing action on given peer.
     */
    public boolean isSafe(double myBusyLoad, HostLoad peerLoad, double avgBusy, double avgSingle, double addedBusyLoad, double addedSingleLoad) {
        if ((peerLoad.busyLoad == LBSettings.LOAD_DONT_KNOW) || (peerLoad.busyLoad + addedBusyLoad > 2 * avgBusy) ||
//                ((peerLoad.singleLoad != LBSettings.LOAD_DONT_KNOW) && (peerLoad.singleLoad + addedSingleLoad > 2 * avgSingle)) ||
                (myBusyLoad - addedBusyLoad < 0.5 * avgBusy) )
            return false;
        try {
            SuitableHostOperation op = (SuitableHostOperation) ((DistAlgReplyMessage) messageDisp.sendMessageWaitReply(
                    new DistAlgRequestMessage(new SuitableHostOperation(false, addedBusyLoad, addedSingleLoad)), peerLoad.host).getFirstReply()).getOperation();
            return op.wasSuccessful();
        } catch (IOException ex) {
            log.severe(ex);
            return false;
        }
    }
    
    /** Given a hostLoad, check whether the busy-load is under the average busy load. */
    public boolean isUnderAvg(HostLoad peerLoad, double avgBusy) {
        if ((peerLoad.busyLoad == LBSettings.LOAD_DONT_KNOW) || (peerLoad.busyLoad > avgBusy))
            return false;
        try {
            SuitableHostOperation op = (SuitableHostOperation) ((DistAlgReplyMessage) messageDisp.sendMessageWaitReply(
                    new DistAlgRequestMessage(new SuitableHostOperation(true, 0, 0)), peerLoad.host).getFirstReply()).getOperation();
            return op.wasSuccessful();
        } catch (IOException ex) {
            log.severe(ex);
            return false;
        }
    }
    
    /**
     * Simply returns the least loaded peer known at the moment. Checkts, whether the peer is available for balancing and
     *   whether it is not overloaded. The peer is notified that it will be used for a load-balancing action.
     * @return the least loaded available peer known. Null if none exists.
     */
    public HostLoad getLeastLoadedPeer() {
        try {
            for (HostLoad peerLoad : gossip.unloadedPeers.getCurrentPeers()) {
                SuitableHostOperation op = (SuitableHostOperation) ((DistAlgReplyMessage) messageDisp.sendMessageWaitReply(
                        new DistAlgRequestMessage(new SuitableHostOperation(true, 0, 0)), peerLoad.host).getFirstReply()).getOperation();
                if (op.wasSuccessful())
                    return peerLoad;
            }
            return null;
        } catch (IOException ex) {
            log.severe(ex);
            return null;
        }
    }
    
    /**
     * @return true if node n can be deleted from p without overloading the joining peer and under-loading this peer.
     */
    public boolean canDelete(double myBusyLoad, BalancedDistributedAlgorithm node, double avgBusy, double avgSingle) {
        if ((Replica.class.isAssignableFrom(node.getClass())) || (node.storageDispatcher.getAllReplicaNodes().size() > 0))
            return false;
        NetworkNode mergingNode = node.getMergingNode();
        if (mergingNode == null)
            return false;
        double nodeBusy = node.getBusyLoad();
        return (nodeBusy != 0) && isSafe(myBusyLoad, new HostLoad(new NetworkNode(mergingNode,false)), avgBusy, avgSingle, nodeBusy, node.getSingleLoad());
    }
    
    /** Decide whether to delete or migrate one of the nodes elsewhere (called only if more then 1 node at this peer).
     *  The policy is the following:
     *   if one of the nodes is a Replica, then migrate one of the replicas elsewhere
     *   else, try to find a node, that is NOT REPLICATED and:
     *     denote q the peer that data n would be moved to after Leave(n),
     *     If q.isSuitable(busy-load(n), single-load(n)) then  Leave(n)
     *   else Migrate(p.FindPeer) - rather migrate a replica than a regular node
     *  @return false if no balancing action done (no known peer is suibable)
     */
    public boolean deleteOrMigrate(boolean checkUnderAvg, double myBusyLoad, Collection<BalancedDistributedAlgorithm> nodes, double avgBusy, double avgSingle) {
        SortedMap<Double, BalancedDistributedAlgorithm> sortedNodes = new TreeMap<Double, BalancedDistributedAlgorithm>();
        for (BalancedDistributedAlgorithm node : nodes)
            sortedNodes.put(node.getBusyLoad(), node);
        
        // find the least loaded node and either delete it or migrate it
        for (Map.Entry<Double, BalancedDistributedAlgorithm> entry : sortedNodes.entrySet()) {
            BalancedDistributedAlgorithm node = entry.getValue();
            // if the node is a regular node and it is not replicated, try to delete it
            if (canDelete(myBusyLoad, node, avgBusy, avgSingle))
                return host.leave(node);
            
            // if it is a replica or it is replicated or it cannot be deleted then try to migrate it
            for (HostLoad peerLoad : gossip.unloadedPeers.getCurrentPeers()) {
                if ((checkUnderAvg && isUnderAvg(peerLoad, avgBusy)) || // if only check that the peer has busy-load under the average
                        (! checkUnderAvg && isSafe(myBusyLoad, peerLoad, avgBusy, avgSingle, entry.getKey(), node.getSingleLoad()))) {
                    return host.migrate(node, peerLoad.host);
                }
            }
        }
        
        // if no peer is suitable, then pick simply the least loaded known and migrate...
        /*HostLoad peerLoad = getLeastLoadedPeer();
        if (peerLoad != null)
            return host.migrate(sortedNodes.get(sortedNodes.firstKey()), peerLoad.host);*/
        return false;
    }
    
    /** The load balancing strategy. Return true if some balancing action was taken */
    protected boolean loadBalancing() {
        if (! monitor.tryLock())
            return false;
        try {
            double data = host.getDataLoad();
            double single = host.getSingleLoad();
            double load = host.getBusyLoad();
            double avgLoad = gossip.avgWaitLoadEst();
            double avgSingle = gossip.avgProcLoadEst();
            double avgData = gossip.avgDataLoadEst();
            int nodesNumber = host.nodes.size();
            // Do not take any action if this host is empty or an action was taken recently
            if ((data == LBSettings.LOAD_DONT_KNOW) || (nodesNumber == 0) || (avgData < 0)) {
                notOverloaded();
                return false;
            }
            
            // If the wait load is 0, then consider only the size of the data storage
            if ((load == 0) && (avgLoad < LBSettings.MIN_BUSY_LOAD)) {
                if ((avgData > LBSettings.MIN_SINGLE_LOAD) && (data >= 1.5 * avgData)) {
                    log.info("I'm overloaded because of data-load: "+data+" > 1.5 *"+avgData+" (avg_data-load). \n\tThis is the "+(overloadCounter +1) +". time");
                    if (overloadedDoBalancing(DATA_OVERLOAD)) {
                        if (host.nodes.size() == 0) {
                            log.warning("Balancing on data while being empty!");
                            return false;
                        }
                        for (HostLoad peerLoad : gossip.unloadedPeers.getCurrentPeers()) {
                            if (peerLoad.storage > 0)
                                break;
                            if (isEmpty(peerLoad)) {
                                if (host.nodes.size() > 1)
                                    return host.migrate(host.getNodes().next(), peerLoad.host);
                                else {
                                    BalancedDistributedAlgorithm node = host.getNodes().next();
                                    return host.split(node, peerLoad.host);
                                }
                            }
                        }
                    }
                    return false;
                }
            }
            
            if ((load == LBSettings.LOAD_DONT_KNOW) || (avgLoad < LBSettings.MIN_BUSY_LOAD)) {
                notOverloaded();
                return false;
            }
            
            ////////////////////////////////////    the busy load is over 0   ////////////////////////////////////
            
            // else do the balancing on busy and single load
            if (load > 2 * avgLoad) {
                log.info("I'm overloaded because of busy-load: "+load+" > 2 *"+ (int) avgLoad+" (avg_busy-load, weight: "+gossip.getEstimationWeight()+"). \n\tThis is the "+(overloadCounter + 1) +". time");
                if (overloadedDoBalancing(BUSY_OVERLOAD)) {
                    if (host.nodes.size() > 1) {
                        return deleteOrMigrate(true, load, host.nodes.values(), avgLoad, avgSingle);
                    } else {
                        BalancedDistributedAlgorithm node = host.getNodes().next();
                        // if the only node at this peer is a replica then do nothing - let the master node do the balancing (split or replicating)
                        if (Replica.class.isAssignableFrom(node.getClass()))
                            return false;
                        
                        if ((avgSingle > LBSettings.MIN_SINGLE_LOAD) && (single != LBSettings.LOAD_DONT_KNOW) && (single > 2 * avgSingle)) {
                            for (HostLoad peerLoad : gossip.unloadedPeers.getCurrentPeers())
                                if (isUnderAvg(peerLoad, avgLoad))
                                    return host.split(node,peerLoad.host);
                        } else {
                            //int replicationLevel = node.storageDispatcher.getAllReplicaNodes().size();
                            //double addedBusyLoad = node.getBusyLoad() * (replicationLevel+1) / (double) (replicationLevel+2);
                            //double addedSingleLoad = node.getSingleLoad() * (replicationLevel+1) / (double) (replicationLevel+2);
                            for (HostLoad peerLoad : gossip.unloadedPeers.getCurrentPeers())
                                if (isUnderAvg(peerLoad, avgLoad))
                                    return (host.replicate(node,peerLoad.host) != null);
                            
                            // if no peer is suitable, then pick simply the least loaded known and replicate
                            /*HostLoad peerLoad = getLeastLoadedPeer();
                            if (peerLoad != null)
                                return (host.replicate(node,peerLoad.host) != null);*/
                        }
                    }
                    log.info("Found no peer to be used for a load-balancing action: "+host);
                }
                return false;
            }
            
            if (load < 0.5 * avgLoad) {
                log.info("I'm underloaded because of busy-load: "+load+" < 1/2 *"+(int) avgLoad+" (avg_busy-load). \n\tThis is the "+(overloadCounter +1) +". time");
                if (overloadedDoBalancing(BUSY_UNDERLOAD)) {
                    try {
                        // if there exists a node that is replicated then remove one of its replicas
                        for (BalancedDistributedAlgorithm node : host.nodes.values()) {
                            if (node.storageDispatcher.getAllReplicaNodes().size() > 0) {
                                for (NetworkNode replica : node.storageDispatcher.getAllReplicaNodes()) {
                                    SuitableHostOperation op = (SuitableHostOperation) ((DistAlgReplyMessage) messageDisp.sendMessageWaitReply(
                                            new DistAlgRequestMessage(new SuitableHostOperation(replica)), new NetworkNode(replica, false)).getFirstReply()).getOperation();
                                    if (op.wasSuccessful())
                                        return host.unify(node, replica);
                                }
                            }
                        }
                        BalancingOfferOperation balancingOffer;
                        NetworkNode nodeToMerge;
                        // else try to merge some node with me
                        for (BalancedDistributedAlgorithm node : host.nodes.values()) {
                            if ((! Replica.class.isAssignableFrom(node.getClass())) && ((nodeToMerge = node.getNodeToMerge()) != null)) {
                                log.info("Sending balancing request to: "+nodeToMerge+" (merge)");
                                balancingOffer = new BalancingOfferOperation(nodeToMerge);
                                balancingOffer = (BalancingOfferOperation) ((DistAlgReplyMessage) messageDisp.sendMessageWaitReply(
                                        new DistAlgRequestMessage(balancingOffer), new NetworkNode(nodeToMerge, false)).getFirstReply()).getOperation();
                                if (balancingOffer.wasSuccessful())
                                    return true;
                            }
                        }
                        
                        // else iterate over the most loaded peers in the system
                        for (HostLoad peerLoad : gossip.loadedPeers.getCurrentPeers()) {
                            if ((peerLoad.busyLoad != LBSettings.LOAD_DONT_KNOW) && (peerLoad.busyLoad != 0)) {
                                log.info("Sending balancing request to: "+peerLoad.host+" (any balancing action)");
                                balancingOffer = new BalancingOfferOperation(new HostLoad(host));
                                balancingOffer = (BalancingOfferOperation) ((DistAlgReplyMessage) messageDisp.sendMessageWaitReply(
                                        new DistAlgRequestMessage(balancingOffer), peerLoad.host).getFirstReply()).getOperation();
                                if (balancingOffer.wasSuccessful())
                                    return true;
                            }
                        }
                        
                    } catch (IOException ex) {
                        log.severe(ex);
                        return false;
                    }
                }
                return false;
            }
            
            if ((avgSingle >= LBSettings.MIN_SINGLE_LOAD) && (single != LBSettings.LOAD_DONT_KNOW) && (single > 2 * avgSingle)) {
                log.info("I'm overloaded because of single-load: "+single+" > 2 *"+ (int) avgSingle+" (avg_single-load). \n\tThis is the "+(overloadCounter +1) +". time");
                if (overloadedDoBalancing(SINGLE_OVERLOAD)) {
                    if (host.nodes.size() > 1) {
                        return deleteOrMigrate(false, load, host.nodes.values(), avgLoad, avgSingle);
                    } else {
                        BalancedDistributedAlgorithm node = host.getNodes().next();
                        // if the only node at this peer is a replica then do nothing - let the master node do the balancing (split or replicating)
                        if (Replica.class.isAssignableFrom(node.getClass()))
                            return false;
                        
                        double addedBusyLoad = node.getBusyLoad() / 2;
                        double addedSingleLoad = node.getSingleLoad() / 2;
                        for (HostLoad peerLoad : gossip.unloadedPeers.getCurrentPeers())
                            if (isSafe(load, peerLoad, avgLoad, avgSingle, addedBusyLoad, addedSingleLoad))
                                return host.split(node,peerLoad.host);
                        
                        // if no peer is suitable, then pick simply the least loaded known and split
                        /*HostLoad peerLoad = getLeastLoadedPeer();
                        if (peerLoad != null)
                            return host.split(node,peerLoad.host);*/
                    }
                    log.info("Found no peer to be used for a load-balancing action: "+host);
                }
                return false;
            }
            notOverloaded();
            return false;
        } finally {
            monitor.unlock();
        }
    }
    
    protected boolean processBalancingOffer(BalancingOfferOperation operation, DistAlgRequestMessage request) throws IOException {
        if (! monitor.tryLock())
            return false;
        NetworkNode sender = request.getSender();
        try {
            double single = host.getSingleLoad();
            double load = host.getBusyLoad();
            double avgSingle = gossip.avgProcLoadEst();
            double avgLoad = gossip.avgWaitLoadEst();
            
            // if the sender offers deleting a node and merging it with the sender
            if (operation.nodeToDelete != null) {
                if (host.nodes.size() < 2)
                    return false;
                BalancedDistributedAlgorithm node = host.nodes.get(operation.nodeToDelete);
                // if the node is a regular node and it is not replicated, try to delete it
                if ((node != null) && (canDelete(load, node, avgLoad, avgSingle))) {
                    operation.endOperation();
                    messageDisp.replyMessage(new DistAlgReplyMessage(request));
                    return host.leave(node);
                }
                return false;
            }
            
            // HERE, check whether my busy load is over the average
            if ((load == LBSettings.LOAD_DONT_KNOW) || (avgLoad < LBSettings.MIN_BUSY_LOAD) || (load <= avgLoad))
                return false;
            
            // count number of nodes that process some queries
            // find the least loaded node of them
            int busyNodes = 0;
            BalancedDistributedAlgorithm leastLoadedNode = null;
            double smallestLoad = Double.MAX_VALUE;
            for (BalancedDistributedAlgorithm node : host.nodes.values()) {
                double nodeLoad = node.getBusyLoad();
                if ((nodeLoad != 0) && (nodeLoad != LBSettings.LOAD_DONT_KNOW)) {
                    busyNodes++;
                    if (nodeLoad < smallestLoad) {
                        smallestLoad = nodeLoad;
                        leastLoadedNode = node;
                    }
                }
            }
            
            // else do anything for load-balancing to given peer
            if (busyNodes > 1) {
                // and migrate it, if "canBalance"
                //if (canBalance(busy, operation.senderLoad, avgBusy, avgSingle, smallestBusyLoad, leastLoadedNode.getSingleLoad()))
                if (isUnderAvg(operation.senderLoad, avgLoad)) {
                    operation.endOperation();
                    messageDisp.replyMessage(new DistAlgReplyMessage(request));
                    return host.migrate(leastLoadedNode, sender);
                }
            } else if (busyNodes == 1) {
                // if the only node at this peer is a replica then do nothing - let the master node do the balancing (split or replicating)
                if (Replica.class.isAssignableFrom(leastLoadedNode.getClass()))
                    return false;
                // if better balance using "split" then using replication
                if ((single != LBSettings.LOAD_DONT_KNOW) && (avgSingle > LBSettings.MIN_SINGLE_LOAD) && (avgLoad > LBSettings.MIN_BUSY_LOAD) &&
                        ((single/avgSingle >= load/avgLoad) || (single > 2 * avgSingle))) {
                    //double addedBusyLoad = leastLoadedNode.getBusyLoad() / 2;
                    //double addedSingleLoad = leastLoadedNode.getSingleLoad() / 2;
                    //if (canBalance(busy, operation.senderLoad, avgBusy, avgSingle, addedBusyLoad, addedSingleLoad))
                    if (isUnderAvg(operation.senderLoad, avgLoad)) {
                        operation.endOperation();
                        messageDisp.replyMessage(new DistAlgReplyMessage(request));
                        return host.split(leastLoadedNode,sender);
                    }
                } else if (avgLoad > LBSettings.MIN_BUSY_LOAD) {
                    // else replicate the node
                    int replicationLevel = leastLoadedNode.storageDispatcher.getAllReplicaNodes().size();
                    //double addedBusyLoad = leastLoadedNode.getBusyLoad() * (replicationLevel+1) / (double) (replicationLevel+2);
                    //double addedSingleLoad = leastLoadedNode.getSingleLoad() * (replicationLevel+1) / (double) (replicationLevel+2);
                    //if (canBalance(busy, operation.senderLoad, avgBusy, avgSingle, addedBusyLoad, addedSingleLoad))
                    if (isUnderAvg(operation.senderLoad, avgLoad)) {
                        operation.endOperation();
                        messageDisp.replyMessage(new DistAlgReplyMessage(request));
                        return (host.replicate(leastLoadedNode, sender) != null);
                    }
                }
            }
            return false;
        } finally {
            monitor.unlock();
        }
    }
    
}
/*
 * Replica.java
 *
 * Created on November 9, 2006, 11:05
 */

package messif.loadbalancing.replication;

import messif.algorithms.AlgorithmMethodException;
import messif.loadbalancing.BalancedDistributedAlgorithm;
import messif.loadbalancing.CreateNodeOperation;
import messif.loadbalancing.Host;
import messif.netcreator.CantStartException;
import messif.network.NetworkNode;
import messif.network.ThreadInvokingReceiver;
import java.io.IOException;
import messif.algorithms.DistAlgRequestMessage;
import messif.buckets.LocalBucket;

/**
 * This is a node-envelope of the replication storage
 * @author xnovak8
 */
public class Replica extends BalancedDistributedAlgorithm {
    /** Class id for serialization */
    private static final long serialVersionUID = 3L;

    /** The original replicated node */
    protected NetworkNode replicatedNode;

    /** Getter for replicated node id */
    public NetworkNode getReplicatedNode() {
        return replicatedNode;
    }

    /** message receiver */
    protected transient ThreadInvokingReceiver receiver = null;
    
    /** Creates a new instance of Replica */
    public Replica(Host host, NetworkNode replicatedNode, Class<? extends LocalBucket> defaultBucketClass) throws InstantiationException {
        super("Replica of "+replicatedNode, host, defaultBucketClass);
        
        this.replicatedNode = replicatedNode;
        start();
    }
    
    /******************************  Message and operation handlers   ************************************/
    
    /** Migrate the original node elswhere */
    protected void migrateMainNode(MigrateNotifyOperation operation, DistAlgRequestMessage request) throws AlgorithmMethodException {
        if (replicatedNode.equals(operation.origNode))
            replicatedNode = operation.newNode;
        else log.warning("The "+operation.origNode+" is not the main node of this replica");
    }
    
    /** Handling of the message that contains an operation to be executed on this algorithm */
    public final void receive(DistAlgRequestMessage msg) {
        try {
            receiveRequest(msg);
        } catch (AlgorithmMethodException e) {
            log.severe(e);
        }
    }
    
    /******************************  Abstract load-balancing operations to be implemented ***************************/
    
    /** It starts receiving messages  */
    public void start() {
        receiver = new ThreadInvokingReceiver(this, "receive");
        messageDisp.registerReceiver(receiver);
    }
    
    /** Clean up everything */
    public void finalize() throws Throwable {
        messageDisp.deregisterReceiver(receiver);
    }
    
    /** The load-balancing "Split" operation. Implementation of this method should create the CreateNodeOperation
     *   with information about the specific node's class adn about constructor to be used to create a new node
     */
    public CreateNodeOperation splitNode() {
        throw new UnsupportedOperationException("replica-split");
    }
    
    /** The load-balacing "Leave" operation. Correctly removes given node from the PDN. Either join the data
     * with a neighbour or re-insert them to the network. The DistributedAlgorithm can be disposed afterwards.
     * @return false if the node cannot leave the network properly.
     */
    public boolean leave() {
        throw new UnsupportedOperationException("replica-leave");
    }
    
    /** The load-balacing "Migrate" operation. Substitute "thisNode" for a specified node (at a different host). This operation is
     *   called after physical migration at the new peer!
     */
    public void migrate(NetworkNode origNode, NetworkNode newNode) throws AlgorithmMethodException {
        try {
            messageDisp.sendMessage(new DistAlgRequestMessage(new MigrateNotifyOperation(origNode, newNode)), replicatedNode, true);
        } catch (IOException ex) {
            log.severe(ex); throw new AlgorithmMethodException(ex.getMessage());
        }
    }
    
    /** Returns a random node that I know in the system (for gossiping) */
    public NetworkNode getRandomNode() {
        return replicatedNode;
    }

    public NetworkNode getMergingNode() {
        return replicatedNode;
    }
    
    public NetworkNode getNodeToMerge() {
        return null;
    }

    /** toString() */
    public String toString() {
        return "Replica of node "+replicatedNode+"; storing: "+getDataLoad()+" objects";
    }

}

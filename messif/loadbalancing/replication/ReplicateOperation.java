/*
 * ReplicateOperation.java
 *
 * Created on November 9, 2006, 10:46
 */

package messif.loadbalancing.replication;

import java.util.Collection;
import messif.buckets.LocalBucket;
import messif.loadbalancing.LoadBalancingErrorCode;
import messif.network.NetworkNode;
import messif.operations.AbstractOperation;


/**
 *
 * @author xnovak8
 */
public class ReplicateOperation extends AbstractOperation {
    /** Class id for serialization */
    private static final long serialVersionUID = 3L;
    
    /** The replicated node */
    public final NetworkNode replicatedNode;
    
    /** If this flag is true, the destination peer does not check whether the sender asked for balancing */
    public final boolean silent;

    /** New node id (answer value) */
    public NetworkNode replicaId;
    
    /** Creates a new instance of ReplicateOperation */
    public ReplicateOperation(NetworkNode replicatedNode) {
        this(replicatedNode, false);
    }
    
    /** Creates a new instance of ReplicateOperation */
    public ReplicateOperation(NetworkNode replicatedNode, boolean silent) {
        this.replicatedNode = replicatedNode;
        this.silent = silent;
    }
    
    /****************** Override abstract methods ******************/
    
    /** End operation successfully */
    public void endOperation() {
        endOperation(LoadBalancingErrorCode.NODE_CREATED);
    }
    
    /** Get status of the operation */
    public boolean wasSuccessful() {
        return (LoadBalancingErrorCode.NODE_CREATED.equals(errValue));
    }

    @Override
    protected boolean dataEqualsImpl(AbstractOperation obj) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int dataHashCode() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}

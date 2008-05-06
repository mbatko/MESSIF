/*
 * UnifyOperation.java
 *
 * Created on 9. listopad 2006, 17:08
 */

package messif.loadbalancing.replication;

import messif.loadbalancing.LoadBalancingErrorCode;
import messif.network.NetworkNode;
import messif.operations.AbstractOperation;

/**
 *
 * @author xnovak8
 */
public class UnifyOperation extends AbstractOperation {
    /** Class id for serialization */
    private static final long serialVersionUID = 2L;
    
    /** New node id (answer value) */
    public final NetworkNode replicaId;
    
    /** If this flag is true, the destination peer does not check whether the sender asked for balancing */
    public final boolean silent;
    
    /** Creates a new instance of UnifyOperation */
    public UnifyOperation(NetworkNode replicaId) {
        this(replicaId, false);
    }

    /** Creates a new instance of UnifyOperation */
    public UnifyOperation(NetworkNode replicaId, boolean silent) {
        this.replicaId = replicaId;
        this.silent = silent;
    }
    
    /****************** Override abstract methods ******************/
    
    /** End operation successfully */
    public void endOperation() {
        endOperation(LoadBalancingErrorCode.REPLICA_REMOVED);
    }
    
    /** Get status of the operation */
    public boolean wasSuccessful() {
        return (LoadBalancingErrorCode.REPLICA_REMOVED.equals(errValue));
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

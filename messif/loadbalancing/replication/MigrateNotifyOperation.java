/*
 * MigrateNotifyOperation.java
 *
 * Created on November 10, 2006, 10:10
 */

package messif.loadbalancing.replication;

import messif.loadbalancing.LoadBalancingErrorCode;
import messif.network.NetworkNode;
import messif.operations.AbstractOperation;

/**
 * This operation is sent to the main node (replica) if a replica (main node) are migrated.
 *
 * @author xnovak8
 */
public class MigrateNotifyOperation extends AbstractOperation {
    /** Class id for serialization */
    private static final long serialVersionUID = 2L;
    
    /** Original node */
    public final NetworkNode origNode;
    
    /** New migrated node */
    public final NetworkNode newNode;
    
    /**
     * Creates a new instance of MigrateNotifyOperation
     */
    public MigrateNotifyOperation(NetworkNode origNode, NetworkNode newNode) {
        this.origNode = origNode;
        this.newNode = newNode;
    }
    
    /****************** Override abstract methods ******************/
    
    /** End operation successfully */
    public void endOperation() {
        endOperation(LoadBalancingErrorCode.MIGRATION_REGISTERED);
    }
    
    /** Get status of the operation */
    public boolean wasSuccessful() {
        return (LoadBalancingErrorCode.MIGRATION_REGISTERED.equals(errValue));
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

/*
 * MigrateOperation.java
 *
 * Created on November 3, 2006, 13:07
 */

package messif.loadbalancing;

import messif.network.NetworkNode;
import messif.operations.AbstractOperation;

/**
 * This operation is sent by a host to another to migrate a node.
 *
 * @author <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a> David Novak, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class MigrateOperation extends AbstractOperation {
    /** Class id for serialization */
    private static final long serialVersionUID = 2L;
    
    /** The migrated algorithm itself */
    protected BalancedDistributedAlgorithm node;
    
    /** Identification of the original node */
    protected NetworkNode origId;
    
    /** Identification of the new node (answer) */
    protected NetworkNode newId;
    
    /** Creates a new instance of MigrateOperation */
    public MigrateOperation(BalancedDistributedAlgorithm node, NetworkNode origId) {
        this.node = node;
        this.origId = origId;
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
    
    /**
     * Clear non-messif data stored in operation.
     * This method is intended to be called whenever the operation is
     * sent back to client in order to minimize problems with unknown
     * classes after deserialization.
     */
    public void clearSuplusData() {
        throw new UnsupportedOperationException("MigrateOperation was not intended to be exposed to client");
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

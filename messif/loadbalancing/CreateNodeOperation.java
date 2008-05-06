/*
 * MessageAddNode.java
 *
 * Created on October 6, 2006, 12:20
 */

package messif.loadbalancing;

import java.util.Collection;
import messif.network.NetworkNode;
import messif.operations.AbstractOperation;

/**
 * This operation is sent by a host that wants to split a node. The receiving host creates a new node by calling
 *   specified constructor of a specified class.
 *
 * @author <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a> David Novak, Faculty of Informatics, Masaryk University, Brno, Czech Republic
 */
public class CreateNodeOperation extends AbstractOperation {
    
    /** Class serial id for serialization. */
    private static final long serialVersionUID = 2L;
    
    /** Class of the node to be created */
    protected Class<?> nodeClass;
    
    /** List of parameter classes of the constructor to be called */
    protected Class[] constructorTypes;
    
    /** List of actual parameters for the constructor of the "nodeClass" */
    protected Object[] constructorParameters;

    /** List of peers to create replicas immediately after creating the node */
    protected Collection<NetworkNode> replicationPeers;
    
    /**
     * Creates a new instance of CreateNodeOperation
     */
    public <N extends BalancedDistributedAlgorithm> CreateNodeOperation(Class<N> nodeClass, Class[] constructorTypes, Object[] constructorParameters) {
        this.nodeClass = nodeClass;
        this.constructorTypes = constructorTypes;
        this.constructorParameters = constructorParameters;
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

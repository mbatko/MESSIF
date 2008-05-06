/*
 * BalancingOfferOperation.java
 *
 * Created on March 26 2007, 16:00
 */

package messif.loadbalancing;

import messif.loadbalancing.HostList.HostLoad;
import messif.network.NetworkNode;
import messif.operations.AbstractOperation;

/**
 *
 * @author David Novak, FI Masaryk University, Brno, Czech Republic; <a href="mailto:xnovak8@fi.muni.cz">xnovak8@fi.muni.cz</a>
 */
public class BalancingOfferOperation extends AbstractOperation {
    /** Class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** If this is request to delete a node and merge it with the sender, this is idenfication of the node to delete. */
    protected NetworkNode nodeToDelete = null;
    
    /** The busy load of the sending peer. */
    protected HostLoad senderLoad;
    
    /** Creates a new instance of BalancingOfferOperation */
    public BalancingOfferOperation(NetworkNode nodeToDelete) {
        this.nodeToDelete = nodeToDelete;
    }

    /** Creates a new instance of BalancingOfferOperation */
    public BalancingOfferOperation(HostLoad senderLoad) {
        this.senderLoad = senderLoad;
    }
    
    /****************** Override abstract methods ******************/
    
    /** End operation successfully */
    public void endOperation() {
        endOperation(LoadBalancingErrorCode.WILL_BALANCE);
    }
    
    /** Get status of the operation */
    public boolean wasSuccessful() {
        return (LoadBalancingErrorCode.WILL_BALANCE.equals(errValue));
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

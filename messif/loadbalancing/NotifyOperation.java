/*
 * NotifyOperation.java
 *
 * Created on November 4, 2006, 19:52
 */

package messif.loadbalancing;

import messif.loadbalancing.HostList.HostLoad;
import messif.operations.AbstractOperation;

/**
 * This operation is sent by a new host to an existing one.
 * The receiver answers by saying whether the balancing is on or off
 *
 * @author Administrator
 */
public class NotifyOperation extends AbstractOperation {
    /** Class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Notifying host */
    protected final HostLoad hostLoad;
    
    /** Response: info about balancing on/off */
    protected boolean loadBalancing;
    
    /** Creates a new instance of NotifyOperation */
    public NotifyOperation(HostLoad hostLoad) {
        this.hostLoad = hostLoad;
    }
    
    /****************** Override abstract methods ******************/
    
    /** End operation successfully */
    public void endOperation() {
        endOperation(LoadBalancingErrorCode.HOST_REGISTERED);
    }
    
    /** Get status of the operation */
    public boolean wasSuccessful() {
        return (LoadBalancingErrorCode.HOST_REGISTERED.equals(errValue));
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

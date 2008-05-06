/*
 * SuitableHostOperation.java
 *
 * Created on November 6, 2006, 12:40
 */

package messif.loadbalancing;

import messif.network.NetworkNode;
import messif.operations.AbstractOperation;

/**
 * This operation is sent to a host before it is used for some load-balancing action.
 * The receiver check outs last time it has been used for balancing and returns either
 * true or false saying - yes/no, you can/cannot use me.
 *
 * @author xnovak8
 */
public class SuitableHostOperation extends AbstractOperation {
    /** Class id for serialization */
    private static final long serialVersionUID = 5L;
    
    /** The estimated busy load to be added to the peer */
    protected double addedBusyLoad;
    
    /** The estimated single load to be added to the peer */
    protected double addedSingleLoad;
    
    /** Requested a fresh peer */
    protected boolean freshPeerRequested;

    /** Check only if the peers busy-load(q) < avg-busy-load */
    protected boolean checkUnderAvg;
    
    /** If this flag is true, then this is not claim for balancing but this message says
     *   that there will be no balancing action */
    protected boolean cancelOperation;
    
    /** If the sender is about to call "Unify" operation, this attribute caries ID of the replica to be deleted */
    protected NetworkNode replica = null;
    
    /** The fresh peer required */
    public SuitableHostOperation() {
        this(true, false, Double.MAX_VALUE, Double.MAX_VALUE, false);
    }
    /** Define the estimated busy and single load to be added to the peer */
    public SuitableHostOperation(boolean checkUnderAvg, double addedBusyLoad, double addedSingleLoad) {
        this(false, checkUnderAvg, addedBusyLoad, addedSingleLoad, false);
    }
    /** This is not request for load-balancing but canceling of the announced operation */
    public SuitableHostOperation(boolean cancelOperation) {
        this(false, false, Double.MAX_VALUE, Double.MAX_VALUE, cancelOperation);
    }
    
    /** This is a request for removing a replica (Unify) */
    public SuitableHostOperation(NetworkNode replica) {
        this.replica = replica;
    }
    
    /** Creates a new instance of SuitableHostOperation */
    public SuitableHostOperation(boolean freshPeerRequested, boolean checkUnderAvg, double addedBusyLoad, double addedSingleLoad, boolean cancelOperation) {
        this.freshPeerRequested = freshPeerRequested;
        this.checkUnderAvg = checkUnderAvg;
        this.addedBusyLoad = addedBusyLoad;
        this.addedSingleLoad = addedSingleLoad;
        this.cancelOperation = cancelOperation;
    }
    
    /****************** Override abstract methods ******************/
    
    /** End operation successfully */
    public void endOperation() {
        endOperation(LoadBalancingErrorCode.SUITABLE_HOST);
    }
    
    /** Get status of the operation */
    public boolean wasSuccessful() {
        return (LoadBalancingErrorCode.SUITABLE_HOST.equals(errValue));
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

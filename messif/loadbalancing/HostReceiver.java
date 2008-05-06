/*
 * HostReceiver.java
 *
 * Created on 15. srpen 2006, 10:52
 */

package messif.loadbalancing;

import messif.algorithms.DistAlgRequestMessage;
import messif.loadbalancing.replication.ReplicateOperation;
import messif.loadbalancing.replication.UnifyOperation;
import messif.network.Message;
import messif.network.ThreadInvokingReceiver;
import messif.operations.AbstractOperation;

/**
 * This is the receiver of the host - all messages go through this receiver. It has
 * two functions:
 *
 * 1) Acccept messages for the host: only the following messages:
 *   MessageStartStopBalancing
 *   DistAlgRequestMessage with operation:
 *           CreateNodeOperation
 *
 * 2) Check, whether the addressed logical node exists - if not, return
 *
 * @author xnovak8
 */
public class HostReceiver extends ThreadInvokingReceiver {
    
    /** Host */
    protected Host host;

    /** Creates a new instance of HostReceiver */
    public HostReceiver(Host methodInvoker, String methodsName) throws InstantiationException {
        super(methodInvoker, methodsName);
        host = methodInvoker;
    }
    
    /** Accept only the specified operations */
    // TODO: completely remake this method - check the target address first (don't check the operations)
    public boolean acceptMessage(Message msg, boolean allowSuperclass) {
        //if (DistAlgRequestMessage.class.isAssignableFrom(msg.getClass())) {
        if (msg.getClass().equals(DistAlgRequestMessage.class)) {
            AbstractOperation op = ((DistAlgRequestMessage) msg).getOperation();
            if (! ((op == null) || (op instanceof CreateNodeOperation) || (op instanceof MigrateOperation) || (op instanceof NotifyOperation) || (op instanceof SuitableHostOperation) || (op instanceof BalancingOfferOperation) || (op instanceof ReplicateOperation) || (op instanceof UnifyOperation)))
                return ! host.targetNodeExists(msg);
        }
        if (super.acceptMessage(msg, allowSuperclass))
            return true;
        return ! host.targetNodeExists(msg);
    }
}

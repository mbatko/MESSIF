/*
 * OperationRequestMessage.java
 *
 * Created on November 23, 2006, 15:59
 */

package messif.loadbalancing;

import messif.algorithms.DistAlgRequestMessage;
import messif.operations.AbstractOperation;

/**
 * This message with operation is sent only by an empty host to another host in order to process a operation at
 *   any node.
 * @author xnovak8
 */
public class OperationRequestMessage extends DistAlgRequestMessage {
    /** class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of OperationRequestMessage */
    public OperationRequestMessage(AbstractOperation operation) {
        super(operation);
    }
}

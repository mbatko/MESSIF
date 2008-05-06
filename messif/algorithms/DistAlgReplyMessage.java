/*
 * DistAlgReplyMessage.java
 *
 * Created on 15. srpen 2006, 18:06
 *
 */

package messif.algorithms;

import messif.network.ReplyMessage;
import messif.operations.AbstractOperation;


/**
 * Reply message for the distributed algorithm - contains the operation.
 *
 * @author xnovak8
 */
public class DistAlgReplyMessage extends ReplyMessage {
    
    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Message extensions ******************/
    
    /** Operation processed by the algorithm */
    protected final AbstractOperation operation;
    
    /** Returns the operation that this message holds */
    public AbstractOperation getOperation() {
        return operation;
    }
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of DistAlgReplyMessage */
    public DistAlgReplyMessage(DistAlgRequestMessage request) {
        super(request);
        
        this.operation = request.operation;
    }
    
    
    /****************** String representation ******************/
    
    @Override
    public String toString() {
        return super.toString() + ": " + operation;
    }
    
    
}

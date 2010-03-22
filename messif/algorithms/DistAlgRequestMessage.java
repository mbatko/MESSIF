/*
 * DistAlgRequestMessage.java
 *
 * Created on 6. kveten 2003, 10:56
 */

package messif.algorithms;

import messif.network.Message;
import messif.operations.AbstractOperation;

/**
 *
 * @author  xbatko
 */
public class DistAlgRequestMessage extends Message {

    /** Class version id for serialization */
    private static final long serialVersionUID = 1L;
    
    /****************** Message extension ******************/
    protected AbstractOperation operation;
    public AbstractOperation getOperation() { return operation; }
    
    
    /****************** Constructors ******************/
    
    /** Creates a new instance of DistAlgRequestMessage */
    public DistAlgRequestMessage(AbstractOperation operation) {
        super();
        
        this.operation = operation;
    }


    /****************** Clonning ******************/
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        DistAlgRequestMessage rtv = (DistAlgRequestMessage)super.clone();
        
        rtv.operation = operation.clone();
        
        return rtv;
    }
    
    
    /****************** String representation ******************/
    
    @Override
    public String toString() {
        return super.toString() + ": " + operation;
    }
    
}


/*
 * NodeDoesntExistMessage.java
 *
 * Created on October 10, 2006, 12:03
 */

package messif.loadbalancing;

import messif.network.Message;
import messif.network.NetworkNode;
import messif.network.ReplyMessage;

/**
 * This message is send to a originator of a message destination of which does not exist any more
 *
 * @author David Novak
 */
public class NodeDoesntExistMessage extends ReplyMessage {
    /** Class id for serialization */
    private static final long serialVersionUID = 1L;
    
    /** Node that does not exist */
    private final NetworkNode nonExistingNode;
    /** Getter */
    public NetworkNode getNonExistingNode() {
        return nonExistingNode;
    }
    
    /** Creates a new instance of NodeDoesntExistMessage */
    public NodeDoesntExistMessage(Message msg, NetworkNode nonExistingNode) {
        super(msg);
        this.nonExistingNode = nonExistingNode;
    }
}

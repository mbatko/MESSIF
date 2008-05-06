/*
 * MessageActivateResponse.java
 *
 * Created on 4. kveten 2003, 13:50
 */

package messif.netcreator;

import messif.network.NetworkNode;
import messif.network.ReplyMessage;

/**
 *
 * @author  xbatko
 */
public class MessageActivateResponse extends ReplyMessage {

    /** class id for serialization */
    private static final long serialVersionUID = 1L;

    /****************** Response constants ******************/
    public static final byte ACTIVATED = 0;
    public static final byte CANT_START = 1;
    public static final byte ALREADY_ACTIVE = 2;

    /****************** Message extensions ******************/
    protected byte response;
    public byte getResponse() {
        return response;
    }
    public boolean isSuccess() {
        return response == ACTIVATED;
    }

    protected final NetworkNode activatedNode;
    public NetworkNode getActivatedNode() {
        return activatedNode;
    }
    
    /****************** Constructors ******************/
        
    /** Creates a new instance of MessageActivateResponse from supplied data */
    public MessageActivateResponse(byte response, MessageActivate activationMessage) {
        super(activationMessage);
        
        this.response = response;
        this.activatedNode = null;
    }

    /** Creates a new instance of MessageActivateResponse from supplied data */
    public MessageActivateResponse(NetworkNode activatedNode, MessageActivate activationMessage) {
        super(activationMessage);
        
        this.response = ACTIVATED;
        this.activatedNode = activatedNode;
    }
    
}
